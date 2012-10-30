import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 
 */

/**
 * @author Susanne Knoop Singleton-Klasse, die mehreren Prozessen (Agenten)
 *         erlaubt, in die Datei Simulation.csv zu schreiben, ohne dass es zu
 *         Race Conditions kommt
 */
public class CSVWriter {

	private static CSVWriter csvwriter;
	private static BufferedWriter out = null;
	private static String pfad = "../logs/Simulation.csv";

	private CSVWriter() {

		final File file = new File(pfad);
		// wenn die Datei noch nicht existiert, neu erstellen...
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (final IOException e) {

			}
		}
		// ...und sonst den Inhalt löschen
		else {
			file.delete();
		}

		// gepufferten Datenstrom in die Datei öffnen
		try {
			out = new BufferedWriter(new FileWriter(file));
		} catch (final IOException e) {
			System.err
					.println("Schreiben in csv-Datei wegen IO-Fehler nicht möglich!");
		}

		// Spaltenüberschriften in der csv-Datei
		try {
			out
					.write("AID;Performanz;Transportnr;günstigsterAgent;günstigster Preis;gewählter Preis;Vertrauenspreis;Erfolg;"
							+ "VertrauenVorher;VertrauenNachher\n");
			out.flush();
		} catch (final IOException e) {
			System.err
					.println("Schreiben in csv-Datei wegen IO-Fehler nicht möglich!");
		}

	}

	public synchronized static CSVWriter getInstance() {

		if (csvwriter == null) {
			csvwriter = new CSVWriter();
			return csvwriter;
		} else {
			return csvwriter;
		}
	}

	/**
	 * Public Methode, mit der Agenten in die cvg-Datei schreiben können
	 * synchronized, damit es nicht zu Race Conditions kommt, falls mehrere
	 * Agenten gleichzeitig schreiben wollen.
	 * 
	 * @param s
	 */
	public synchronized void writeCSV(String s) {

		try {
			out.write(s);
			out.flush();
		} catch (final IOException e) {
			System.err
					.println("Schreiben in Simulation.csv wegen IO-Fehler nicht möglich!");
		}
	}

}
