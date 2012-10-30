import jade.core.AID;

import java.util.Comparator;

/**
 * @author Susanne Knoop 
 * 
 * Einfache Datenstruktur zur Speicherung eines Angebotes
 */
public class Offer implements Comparable {

	// Agent ID des Agenten, der das Angebot gemacht hat
	private AID aid;
	// Vertrauen, dass der BuyerAgent aktuell in diesen Agenten setzt
	private int trust;
	// Preis, den der Agent angeboten hat 
	private int price;
	// Preis nach Verrechnung mit dem Vertrauen 
	private int trustprice;
	

	public Offer(AID aid, int trust, int price, int trustprice) {
		super();
		this.aid = aid;
		this.trust = trust;
		this.price = price;
		this.trustprice = trustprice;
	}
	public AID getAID() {
		return aid;
	}
	public void setAID(AID aid) {
		this.aid = aid;
	}
	public int getTrust() {
		return trust;
	}
	public void setTrust(int trust) {
		this.trust = trust;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	public int getTrustprice() {
		return trustprice;
	}
	public void setTrustprice(int trustprice) {
		this.trustprice = trustprice;
	}

	
	@Override
	/**
	 * Angebot mit dem günstigsten "Trustprice", d. h. Preis
	 * unter Berücksichtigung des Vertrauens, ist das "kleinste".  
	 */
	public int compareTo(Object o) {
	
		if(((Offer) o).getTrustprice() > this.trustprice) {
			return -1;
		}
		else if (((Offer) o).getTrustprice() < this.trustprice) {
			return 1;
		}
		else {
			return 0;
		}
	} 
	
	
}
