import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
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
import java.util.logging.*;
import java.util.Properties;
import java.util.Random;

/**
 * @author Susanne Knoop
 * SellerAgent implementiert einen Verkaufsagenten, der dem BuyerAgent Verkaufsangebote macht
 * und diese mit einer festen Erfolgsrate durchführt. 
 * 
 */

@SuppressWarnings("serial")
public class SellerAgent extends Agent {

	// eigene Erfolgsrate in Prozent, mit der der Agent seine Aufträge durchführt
	private int performanceRate;
	// durchschn. Erfolgsrate aller SellerAgent-Objekte der Simulation
	private int meanPerformanceRate;
	// Standardabweichung der durchschnittlichen Erfolgsrate
	private double perfStandardDev;
	
	private Properties properties = new Properties();
	private Logger agentLogger;
    //private static Logger agentLogger = Logger.getLogger("SellerAgent");
    private static FileHandler fhAgentLogger;
    
    /**
     * Die setup Methode wird beim Start des Agenten ausgeführt. 
     * 
     */
    @Override
	protected void setup(){

    	// Logdatei einrichten 
		try {
			agentLogger = Logger.getLogger("SellerAgent"+this.getAID().getLocalName());
			fhAgentLogger = new FileHandler("../logs/SellerAgent"+ this.getAID().getLocalName() +".txt");
			// es wird im reinen Textformat geloggt
			fhAgentLogger.setFormatter(new SimpleFormatter());
	        agentLogger.addHandler(fhAgentLogger);
	        agentLogger.setLevel(Level.ALL);
	        
	        
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			agentLogger.warning("Loggen der Agententätigkeit nicht möglich.");
		}
		
		//Parameter des Agenten aus Konfigurationsdatei auslesen
		//Falls das nicht möglich ist, werden Standardwerte eingesetzt 
		try {
		  properties.loadFromXML(new FileInputStream("../config/configuration.xml"));
		} catch (IOException e) {
		  agentLogger.warning("Laden der Properties nicht möglich.");
		}
		
		if (properties.getProperty("perfStandardDev") != null) {
			perfStandardDev = Integer.parseInt(properties.getProperty("perfStandardDev"));
		}
		else {
			perfStandardDev = 5;
			agentLogger.warning("Laden der Properties nicht möglich.");
		}
		
		if (properties.getProperty("meanPerformanceRate") != null) {
			meanPerformanceRate = Integer.parseInt(properties.getProperty("meanPerformanceRate"));
		}
		else {
			meanPerformanceRate = 65;
			agentLogger.warning("Laden der Properties nicht möglich.");
		}
		
		Random rp = new Random();
		// erzeugt normalverteilte Zufallszahl mit Standardabweichung von perfStandardDev
		// und Mittelwert von meanPerformanceRate
		performanceRate = (int) Math.round(rp.nextGaussian() * perfStandardDev + meanPerformanceRate);
		// Performance kann höchstens 100 sein
		if(performanceRate > 100){
			performanceRate = 100; 
		}
		agentLogger.info("Die Performanz des Agenten " + this.getLocalName() + 
				" liegt bei " + performanceRate + "%!");
		
		// Name des Agenten und Perf. Rate in die csv-Datei schreiben zur späteren Auswertung 
		CSVWriter writer = CSVWriter.getInstance();
		writer.writeCSV(this.getLocalName() + ";" + performanceRate + ";null;null;null;null;null;null;null;null\n");

		// Agenten in den Gelben Seiten registrieren
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Seller");
		sd.setName("Seller");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
			agentLogger.info("Seller Agent " + this.getLocalName() + 
					" hat sich erfolgreich in den Gelben Seiten registriert!");
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		agentLogger.info("Seller Agent " + this.getLocalName() + " initialisiert!");
		
		// Haupt-Behaviour des Agenten hinzufügen 
		addBehaviour(new SellTransportManager());
	}
	/**
	 * Innere Klasse, die das Verhalten des SellerAgent beschreibt
	 * Das Verhalten besteht darin, auf Nachrichten des BuyerAgent zu reagieren. 
	 * Da immer wieder neue Nachrichten eintreffen können, handelt es sich um ein CyclicBehaviour, 
	 * das immer wieder von vorn ausgeführt wird. 
	 * @author Susanne Knoop
	 *
	 */
	@SuppressWarnings("serial")
	private class SellTransportManager extends CyclicBehaviour {

		@Override
		public void action() {
			
			// Behaviour reagiert nur auf Nachrichten vom Typ CFP (Call for Proposals) 
			MessageTemplate mtcfp = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			// ...oder vom Typ ACCEPT PROPOSAL 
			MessageTemplate mtacc = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			
			// falls die Nachricht eine CFP-Nachricht ist: 
			ACLMessage msg = myAgent.receive(mtcfp);
			if (msg != null) {
				
				agentLogger.info("Verkaufsagent " + myAgent.getLocalName() + 
						" hat Call for Proposal erhalten!");
				ACLMessage reply = msg.createReply();
				
				Random r = new Random();
				//normalverteilte Zufallszahl mit festem Mittelwert und Standardabweichung erzeugen
				int nextprice = (int) Math.round((r.nextGaussian() * 10) + 50); 
				// Angebot zurückschicken 
				reply.setPerformative(ACLMessage.PROPOSE);
				reply.setContent(String.valueOf(nextprice));
				
				myAgent.send(reply);
				agentLogger.info("Verkaufsagent " + myAgent.getLocalName() + 
						" hat Angebot zum Preis von " + nextprice + " abgegeben!");
			}
			
			// Wenn der Buyer Agent das Angebot angenommen hat
			msg = myAgent.receive(mtacc);
			if (msg != null) {
				
				agentLogger.info("Agent " + myAgent.getLocalName() + " hat einen Auftrag erhalten!");
				
				boolean success = true;
				Random r = new Random();
				// ausrechnen, ob der Auftrag erfolgreich war 
				int outcome = (int) Math.round(r.nextDouble() * 100);
				if (outcome < (100 - performanceRate)){
					success = false;
				}
				// ...und den Ausgang als Antwort an den Käufer zurückschicken
				ACLMessage reply = msg.createReply();
			
				if (success) {
					agentLogger.info("Agent " + myAgent.getLocalName() + 
							" hat den Auftrag erfolgreich ausgeführt.");
					reply.setContent("");
					reply.setPerformative(ACLMessage.INFORM);
				}
				else {
					agentLogger.info("Agent " + myAgent.getLocalName() + 
							" hat den Auftrag nicht erfolgreich ausgeführt.");
					reply.setContent("");
					reply.setPerformative(ACLMessage.FAILURE);
				}
				
				myAgent.send(reply);
				
			}
			
		}

		
	}
}
