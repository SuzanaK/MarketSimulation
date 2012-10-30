import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
 

/**
 * 
 * @author Susanne Knoop
 * 
 * Die Klasse BuyerAgent simuliert einen Verkaufsagenten, 
 *
 */

@SuppressWarnings("serial")
public class BuyerAgent extends Agent {

	// alle Verkaufsagenten, die dem BuyerAgent bekannt sind
	private final ArrayList<AID> knownSellerAgents = new ArrayList<AID>();
	// Abbildung von AID des Verkaufsagenten auf aktuelles Vertrauen
	private final HashMap<AID, Integer> trustSellerAgents = new HashMap<AID, Integer>();
	// Anfangswert des Vertrauens, fuer alle Verkaufsagenten gleich
	private int initialTrust;
	// Gesamtanzahl der Transporte, die gekauft werden sollen
	private int transports;
	// Anzahl der noch ausstehenden Transporte
	private int leftTransports;
	// allgemeiner Anstieg des Vertrauens nach jeder Runde
	private int allTrustRaise;
	// Anstieg des Vertrauens nach einem erfolgreich durchgefuehrten Transport
	private int sellerTrustRaise;
	// Anzahl der Verkaufsagenten
	private int noSellerAgents;
	// durchschnittliche Performance Rate aller Verkaufsagenten
	private int meanPerformanceRate;

	private Logger agentLogger = Logger.getLogger("BuyerAgent");
	private FileHandler fhAgentLogger;
	private CSVWriter writer;
	private Properties properties = new Properties();

	/**
	 * Die setup Methode wird beim Start des Agenten ausgeführt. 
	 * 
	 */
	@Override
	protected void setup() {

		// Logdateien einrichten
		try {
			fhAgentLogger = new FileHandler("../logs/BuyerAgent.txt");
			fhAgentLogger.setFormatter(new SimpleFormatter());
			agentLogger.addHandler(fhAgentLogger);
			agentLogger.setLevel(Level.ALL);

		} catch (final SecurityException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			System.err.println("Loggen wegen IO-Fehler nicht möglich!");
		}
		
		// aus Konfigurationsdatei auslesen
		try {
			properties.loadFromXML(new FileInputStream("../config/configuration.xml"));
		} catch (IOException e) {
			System.err.println("Laden der Properties nicht möglich.");
		}

		if (properties.getProperty("transports") != null) {
			transports = Integer.parseInt(properties.getProperty("transports"));
		} else {
			transports = 10;
		}

		if (properties.getProperty("initialTrust") != null) {
			initialTrust = Integer.parseInt(properties
					.getProperty("initialTrust"));
		} else {
			initialTrust = 50;
		}

		if (properties.getProperty("allTrustRaise") != null) {
			allTrustRaise = Integer.parseInt(properties
					.getProperty("allTrustRaise"));
		} else {
			allTrustRaise = 5;
		}

		if (properties.getProperty("sellerTrustRaise") != null) {
			sellerTrustRaise = Integer.parseInt(properties
					.getProperty("sellerTrustRaise"));
		} else {
			sellerTrustRaise = 20;
		}
		
		if (properties.getProperty("noSellerAgents") != null) {
			noSellerAgents = Integer.parseInt(properties
					.getProperty("noSellerAgents"));
		} else {
			noSellerAgents = 2;
		}


		leftTransports = transports;

		agentLogger
				.info("Die Parameter für diese Simulationsrunde sind: \n Anzahl der "
						+ "Verkaufsagenten: "
						+ noSellerAgents
						+ "\n Anzahl der Transporte: "
						+ transports
						+ "\n Anfangsvertrauen: "
						+ initialTrust
						+ "\n Vertrauenserhöhung allgemein: "
						+ allTrustRaise
						+ "\n Vertrauenserhöhung Verkäufer: "
						+ sellerTrustRaise + "\n");

		// nach Verkaufsagenten suchen

		do {
			// Schleife, damit die Verkaufsagenten Zeit haben, sich in die
			// Gelben Seiten (DFService) einzutragen
			final DFAgentDescription template = new DFAgentDescription();
			final ServiceDescription sd = new ServiceDescription();
			sd.setType("Seller");

			template.addServices(sd);
			try {
				final DFAgentDescription[] result = DFService.search(this,
						template);
				for (int i = 0; i < result.length; i++) {

					knownSellerAgents.add(result[i].getName());

					agentLogger
							.info("Verkaufsagent mit Namen"
									+ result[i].getName().getLocalName()
									+ " gefunden.");
				}

			} catch (final FIPAException fe) {
				fe.printStackTrace();
			}
		}
		// die Zahl der gefundenen Verkaufsagenten kann also höher sein
		// als noSellerAgents, aber nicht kleiner
		while (knownSellerAgents.size() < noSellerAgents);

		// Vertrauen beginnt bei initialTrust
		// ist anfangs für alle Agenten gleich
		for (AID aid : knownSellerAgents) {
			trustSellerAgents.put(aid, initialTrust);
		}

		agentLogger.info("Buyer Agent " + getAID().getName()
				+ " initialisiert.");
		agentLogger.info("Gefunden wurden " + knownSellerAgents.size()
				+ " Verkaufsagenten.");
		agentLogger.info("Agent kauft im Folgenden " + transports
				+ " Transporte.");

		writer = CSVWriter.getInstance();

		if (leftTransports > 0 && knownSellerAgents.size() > 0) {

			addBehaviour(new BuyTransportsManager());
		}
	}

	/*
	 * innere Klasse BuyTransportManager
	 */

	@SuppressWarnings("serial")
	private class BuyTransportsManager extends Behaviour {

		@Override
		public void action() {

			final ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			cfp.setContent("");
			// jeden bekannten SellerAgent als Empfänger hinzufügen
			for (final AID aid : knownSellerAgents) {
				cfp.addReceiver(aid);
			}
			myAgent.send(cfp);

			// Behaviour reagiert nur auf Nachrichten vom Typ PROPOSE
			final MessageTemplate mtprop = MessageTemplate
					.MatchPerformative(ACLMessage.PROPOSE);

			int proposalsCount = 0;
			// PriorityQueue sortiert beim speichern, niedrigste Eintraege
			// kommen
			// zuerst
			PriorityQueue<Offer> offers = new PriorityQueue<Offer>();

			// solange noch nicht von allen Seller Agents Angebote eingegangen
			// sind,
			// Mailbox auslesen und auswerten
			while (proposalsCount < knownSellerAgents.size()) {
				final ACLMessage msg = myAgent.receive(mtprop);
				if (msg != null) {

					AID seller = msg.getSender();
					final double price = (double) Integer.parseInt(msg.getContent());
					final int trust = trustSellerAgents.get(seller);

					// Preis wird mit dem Grad des "Vertrauens" verrechnet
					// je groesser das Vertrauen, umso kleiner wird der Preis.
					double trustPrice = price;
					
					switch (trust / 10) {
					
					case 0: trustPrice = price * 3.0; break;
					case 1: trustPrice = price * 2.5; break;
					case 2: trustPrice = price * 2; break;
					case 3: trustPrice = price * 1.5; break;
					case 4: 
					case 5: trustPrice = price;	 	break;
					case 6: trustPrice = price / 1.5; break;
					case 7: trustPrice = price / 2.0 ; break;
					case 8: trustPrice = price  / 2.5; break;
					case 9: 
					case 10: trustPrice = price / 3.0; break;
					}

					// neues Angebot wird in die PriorityQueue eingereiht
					Offer nextOffer = new Offer(seller, trust, (int) price,
							(int) Math.round(trustPrice));
					offers.add(nextOffer);
					proposalsCount++;

				} 
				
			}
			
			// alle Preise erhalten... 
			//suche günstigsten Preis
			int cheapestPrice = offers.peek().getPrice();
		
			for (Offer o : offers) {
				if (o.getPrice() < cheapestPrice){
					cheapestPrice = o.getPrice();
				}
			}
			//waehle nun besten Preis aus
			//unter Berücksichtigung des Vertrauens
			Offer bestoffer = offers.poll();
			AID bestSeller = bestoffer.getAID();
			int bestSellerTrust = bestoffer.getTrust();
			int bestPrice = bestoffer.getPrice();
			int bestTrustprice = bestoffer.getTrustprice();

			ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
			order.addReceiver(bestSeller);
			order.setContent("");
			myAgent.send(order);

			agentLogger.info("Transport gekauft zum Preis von " + bestPrice
					+ " von Agent " + bestSeller.getLocalName());

			leftTransports--;

			// trust aktualisieren

			final ACLMessage msg = myAgent.blockingReceive();

			boolean success = true;

			if (msg.getPerformative() == ACLMessage.FAILURE) {
				success = false;
			} else if (msg.getPerformative() == ACLMessage.INFORM) {
				success = true;
			}
			
			String strSuccess = "";
			if (success) {
				 strSuccess = "ja";
			}
			else {
				 strSuccess = "nein";
			}
			// Methode zum Aktualisieren des Vertrauens
			// je nach Ausgang des Auftrags
			updateTrust(success, bestSeller);

			// Ausfuellen folgender Spalten der csv Datei:
			// Nr. des Transports ; Seller Agent ID ; Preis ; Vertrauenspreis; Erfolg ; Vertrauen
			// vorher ; Vertrauen nachher;
			writer.writeCSV("null;null;" + (transports - leftTransports) + ";"
					+ bestSeller.getLocalName() + ";" + cheapestPrice + ";" + bestPrice + ";" 
					+ bestTrustprice + ";" 
					+ strSuccess + ";" + bestSellerTrust + ";"
					+ trustSellerAgents.get(bestSeller) + "\n");
		}

		@Override
		public boolean done() {

			if (leftTransports < 1) {
				return true;
			} else {
				return false;
			}
		}
		
		/**
		 * nach Abschluss der Verkäufe Evaluation beginnen
		 */
		@Override
		public int onEnd() {

			Evaluator ev = new Evaluator();
			ev.evaluate();
			return 1;
		}

	}
	


	/**
	 * Aktualisiert nach jedem Kauf die Vertrauensfunktion aller Verkaufsagenten
	 * 
	 * @param success
	 *            Hat der Verkaufsagent bestSeller den Auftrag korrekt
	 *            ausgeführt?
	 * @param bestSeller
	 *            der Verkaufsagent des letzten Kaufs
	 */

	public void updateTrust(boolean success, AID bestSeller) {

		final Iterator<AID> it = trustSellerAgents.keySet().iterator();
		while (it.hasNext()) {
			final AID nextAID = it.next();
			final int nextTrust = trustSellerAgents.get(nextAID);
			// update des Verkäufers, je nach Erfolg
			if (nextAID.equals(bestSeller)) {
				if (success) {
					// Vertrauen kann nicht größer als 100 Prozent sein
					if (nextTrust + sellerTrustRaise > 100) {
						trustSellerAgents.put(bestSeller, 100);
					} 
					else {
						trustSellerAgents.put(bestSeller,
								(nextTrust + sellerTrustRaise));
					}

				} 
				
				else {
					// im Falle eines Misserfolgs wird das 
					// Vertrauen erst einmal sehr klein
					trustSellerAgents.put(bestSeller, 1);
				}
			}
			// alle anderen Verkäufer
			else {
				// Vertrauen kann nicht größer als 100 Prozent sein
				if (nextTrust + allTrustRaise > 100) {
					trustSellerAgents.put(nextAID, 100);
				} else {
					trustSellerAgents.put(nextAID, (nextTrust + allTrustRaise));
				}
			}
		}

	}
}
