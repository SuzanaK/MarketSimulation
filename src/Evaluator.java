import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * 
 * @author Susanne Knoop
 * Klasse, die Methoden zur Auswertung der Simulationsergebnisse anbietet
 * 
 */
public class Evaluator {

	// Logdatei mit den auszuwertenden Daten
	private File infile = new File("../logs/Simulation.csv");
	// Datei, in der die Ergebnisse des Evaluator geschrieben werden
	private File outfile = new File("../logs/Evaluation.csv");
	private BufferedWriter out;
	// Liste von Stringarrays, in die die Logdatei eingelesen wird
	private ArrayList<String[]> table = new ArrayList<String[]>();
	private StringBuffer s = new StringBuffer();
	private DecimalFormat df = new DecimalFormat( "0.00" );
	
/**
 * Methode zum evaluieren von Simulationsergebnissen
 * 
 */
	public void evaluate() {

		try {
			BufferedReader in = new BufferedReader(new FileReader(infile));
			int lineNo = 0;
			String line;

			while ((line = in.readLine()) != null) {

				table.add((line.split(";", 9)));
				lineNo++;
			}
			in.close();
		} catch (IOException e) {
			System.err
			.println("Lesen der Datei wegen IO-Fehler nicht möglich!");
			return;
		}

		for (String[] line : table) {
			for (String str : line) {
				// Whitespace weg
				str = str.trim();
			}
		}

		s.append(this.evPerformance());
		s.append(this.evPrice());

		try {
			// append auf true hängt an vorhandene Datei an
			out = new BufferedWriter(new FileWriter(outfile, true));
			out.append(s.toString());
			out.flush();
			out.close();

		} catch (IOException e) {
			System.err
			.println("Schreiben in Evaluations-Datei wegen IO-Fehler nicht möglich!");
		}
		//Programm beenden
		System.exit(1);
	}

	private String evPrice() {

		// bezahlter Durchschnittspreis
		double paidPrice = 0;
		int noTransports = 0;

		// erwarteter Durchschnittspreis
		double expPrice = 0;

		for (String[] line : table) {

			if (line[5] != null && !line[5].equals("null")) {

				try {
					paidPrice += Integer.parseInt(line[5]);
					// System.out.println("spalte 5: " + line[5]);
					noTransports++;
				} catch (NumberFormatException e) {

					//System.err.println("Zahl nicht erkannt!");
					continue;
				}

			}
			if (line[4] != null && !line[4].equals("null")) {

				try {
					expPrice += Integer.parseInt(line[4]);
					// System.out.println("spalte 4: " + line[4]);
				} catch (NumberFormatException e) {

					//System.err.println("Zahl nicht erkannt!");
					continue;
				}

			}
		}

		paidPrice = paidPrice / noTransports;
		expPrice = expPrice / noTransports;
		System.out.println("\ngezahlter Durchschnittspreis: " + paidPrice
				+ "\nDurchschnitt der günstigsten Preise ohne Vertrauen: "
				+ expPrice);

		return ";" + df.format(expPrice) + ";" + df.format(paidPrice) + ";\n";
	}

	private String evPerformance() {

		// erwartete Performance
		double expPerf = 0;
		int noAg = 0;

		// reale Performance
		double realPerf = 0;
		// Anzahl erfolgreicher Transporte
		double suc = 0;
		// Anzahl fehlgeschlagener Transporte
		double fail = 0;

		for (String[] line : table) {


			// in Index 1 schreibt jeder SellerAgent am Anfang seine
			// Performance Rate
			if (line[1] != null && !line[1].equals("null")) {

				try {
					expPerf += Integer.parseInt(line[1]);
				} catch (NumberFormatException e) {

					//System.err.println("Zahl nicht erkannt!");
					continue;
				}
				// System.out.println(expPerf);
				noAg++;
			}
			// Index 7 erhält die Information über den Erfolg des Transports
			if (line[7] != null && !line[7].equals("null")) {
				 
				if (line[7].equals("ja")) {
					suc++;
				} 
				else if (line[7].equals("nein")) {
					fail++;
				}
			}
		}

		expPerf = expPerf / noAg;
		
		if (fail != 0) {
			realPerf = (suc / (fail + suc)) * 100;
		} else {
			realPerf = 100;
		}

		System.out.println("Die zu erwartende Performance war: " + expPerf
				+ "Prozent \nDie reale Performance war: " + realPerf
				+ "Prozent\n, d. h.  " + (realPerf - expPerf)
				+ " Prozent höher als erwartet.");

		return df.format(expPerf) + ";" + df.format(realPerf) + ";";

	}

}
