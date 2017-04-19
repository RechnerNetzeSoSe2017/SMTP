package mail_agent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLSocketFactory;
import static java.nio.charset.StandardCharsets.*;;

public class MailFile {
	private static PrintWriter out;
	private static BufferedReader in;
	private String fileToSendString;
	private String host;
	private String from;
	private String user;
	private String pw;
	private String body;
	private int port;
	private String filename;
	private boolean logOnConsole = true;	//Log auf Konsolo ein oder aus
	
public MailFile() {
	// Daten des Senders aus Konfigurationsdatei filtern
	// ________________________________________________________________________________

	// Porperties mit inputStream aus KonfDatei lesen
	// -------------------------------------------------------------------------------
	BufferedReader reader = null;
	List<String> properties = new ArrayList<String>();
	// try catch fuer den fall, dass die datei nicht gefunden wird
	try {
		InputStream konfigurationFile = MailFile.class.getResourceAsStream("UserKonf.txt"); 
		reader = new BufferedReader(new InputStreamReader(konfigurationFile));
		String line;
		// solange zeilen in der Datei gefunden werden, fuege diese dem
		// filebody hinzu
		while ((line = reader.readLine()) != null) {
			properties.add(line);
		}
	} catch (IOException e) {
		e.printStackTrace();
	}
	// -------------------------------------------------------------------------------

	//Properties in variablen binden weil mehrfach genutzt
	from = properties.get(0);
	user = properties.get(1);				//user und pw muessen laut RFC zusammen uebergeben werden
	pw = properties.get(2);
	host = properties.get(3);
	port = Integer.parseInt(properties.get(4));
	body = properties.get(5);

}

	public static void main(String[] args) {
		String toAddress = args[0];
		String path = args[1];
		path = path.replaceAll("\\\\", "\\\\\\\\");	//pfad zur anhaengedatei formatieren
		path = path.replaceAll("/", "\\\\\\\\");	//ja backslashs muessen so oft escaped werden
		MailFile mf = new MailFile();
		File fileToSend = new File(path);
		
		mf.filename = fileToSend.getName();
		Encoder base64Encoder = Base64.getEncoder();
		// die anzuhaengende file wird in ein bytearray verwandelt und dann in base64 encoded
		mf.fileToSendString = new String (base64Encoder.encode(mf.fileToByteArray(fileToSend))); 
		mf.checkMail(toAddress);
		mf.sendMail(toAddress, mf.body);
	}
	
	private byte[] fileToByteArray(File file) {
			byte[] fileAsByteArray = null;		
		try {
			fileAsByteArray = Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileAsByteArray;
	}


	public void send(String s) throws IOException {
		if (s != null) {
			out.println(s);
			log("\tC: " + s);
		}
		out.flush();
	}

	public void sendMail(String toAddress, String body) {

		// ________________________________________________________________________________
		// SEND MAIL		
		// Server Socket deklarieren
		Socket sslSocket = null;
		try {
			sslSocket = SSLSocketFactory.getDefault().createSocket(host, port);
			out = new PrintWriter(sslSocket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
			checkResponse(in.readLine());
			send("EHLO " + "P0rn0R4ll3"); //hier muss noch die IP des absenders rein
			checkResponse(in.readLine());
			if (auth()) {
				send("MAIL FROM: " + from);
				checkResponse(in.readLine());
				send("RCPT TO: " + "<" + toAddress + ">");
				checkResponse(in.readLine());
				send("DATA");						
				checkResponse(in.readLine());		// hier kommen die header informationen fuer die mail
				send("From: <"+ from + ">");		// absender
				send("To: <" + toAddress + ">");	// empfaenger
				send("Subject: RNP1 TestMail");	// betreff
				send("MIME-Version: 1.0");
				send("Content-Type: multipart/mixed; boundary=\"Filetransfer\""); // es wird file gesendet
				send("\n");		
				send("--Filetransfer");
				send("Content-Type: text/plain"); //es kommt wieder text
				send("\n");
				send(body);// eine Leerzeile trennt den header vom body
				send("--Filetransfer");
				send("Content-Type: application/octet-stream");
				send("Content-Disposition: attachment; filename=" + filename);
				send("Content-Transfer-Encoding: base64");
				send("\n");	
				send(fileToSendString);
				send("--Filetransfer--");				
				send(".");
				checkResponse(in.readLine());
				send("quit");
			}
			out.close();
			in.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {										// sslSocket muss auf jeden Fall geschlossen werden, deswegen finally
			try {										// das kann jedoch auch noch fehlschlagen deshalb noch ein try-catch
				sslSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	
	private boolean auth() throws IOException {
		send("AUTH PLAIN");
		checkResponse(in.readLine());
		Encoder base64Encoder = Base64.getEncoder();
		// user und passwort muessen in base64 encoded und zusammen gesendet werden in der Form \0user\0pw laut RFC
		String userAndPwEncoded = new String(base64Encoder.encode(("\0"+user+"\0"+pw).getBytes())); 
		send(userAndPwEncoded);
		String r = in.readLine();
		checkResponse(r); 
		if (r.startsWith("2")) {	//wenn der SMTP reply code mit 2 anfaengt ist alles gut gegangen
			return true;
		}
		return false;
	}

	private void checkResponse(String response) throws IOException {
		log("S: " + response);						// schreib in den log 
		if (response.startsWith("5")) {				// error codes starten mit 5 (smtp reply codes)
			throw new IOException();
		} else if (response.charAt(3) == '-') {		// wenn das vierte Zeichen ein '-' ist,
			checkResponse(in.readLine()); 			// schau ob noch mehr kommt
		}											// abfrage auf "2" ist ueberfluessig, da nur der abschluss der nachricht möglich ist
	}
	
	private void log(String log) {
		if(logOnConsole){
			System.out.println(log);
		}
			
	    FileWriter fw=null;
	    
	    try {
	    	fw = new FileWriter("log.txt",true);
	    	
	    	BufferedWriter bw = new BufferedWriter(fw);

	    	bw.append(log+"\n");

	    	
			bw.close();
			fw.close();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	}

	// ueberpruefe ob es sich um eine gueltige emailadresse handelt
	public Boolean checkMail(String toAdress) {
		// pruefung mittels regulserem ausdruck
		Pattern regexMail = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = regexMail.matcher(toAdress);
		return matcher.find();
	}

}
