import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class MailFile {

	public static void main(String[] args) {
		// Zuerst die anmeldedaten auslesen

		// wenn OK, Socket mit server herstellen

		// DATA übertragen

		// quit
		MailFile mf = new MailFile("",null);
		
		mf.addRecipients("patrick.hoeling@haw-hamburg.de");
		mf.senden();
		mf.closeConnection();

	}

	private String recipient = "patrick.hoeling@haw-hamburg.de";
	private File zuSendendeDaten;

	private String absenderMailadresse;
	private String username;
	private String password;
	private String hostnameZumAbsenden;
	private int portNr = 465;
	
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	
	private boolean consoleLog = true;
	
	private ArrayList<String> recipientList = new ArrayList<>();
	private ArrayList<File> attachments = new ArrayList<File>();

	public MailFile(String email, File file) {

		// patternmatcher fuer emailadressen
		recipient = email;
		zuSendendeDaten = file;

	}

	private boolean readConfig() {
		// hier wird die config ausgelesen
		
		absenderMailadresse="rnsose2017@informatik.haw-hamburg.de";
		username="rnsose2017";
		password="Aufgabe1";
		hostnameZumAbsenden="mailgate.informatik.haw-hamburg.de";
		portNr = 465;

		return true;
		
	}

	public void senden() {
		
		if(!readConfig()){
			return;
		}

		

		try {

			if (portNr == 465) {

				socket = SSLSocketFactory.getDefault().createSocket(hostnameZumAbsenden, portNr);

			} else if (portNr == 25) {

				socket = new Socket(hostnameZumAbsenden, portNr);

			}
		} catch (UnknownHostException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		//-------------
		try {
			connectAndAuth();
		} catch (IOException e) {
			try {
				log("C:> Beende Socketverbindung!");
				socket.close();
			} catch (IOException e1) {
				
				e1.printStackTrace();
			}
			e.printStackTrace();
		}

	}
	
	private void connectAndAuth() throws IOException{
		in= new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out= new PrintWriter(socket.getOutputStream());
		
		//Server stellt sich vor und begrueßt
		String response = in.readLine();
		log("S:> "+response);
		errorCheck(response);
		
		//Hello nachrich AN den Server
		String tmp = "EHLO its.me.mario";
		sendToServer(tmp);
		checkResponse(in.readLine(), "2");
		
		//Die AUTH Methode wird gewählt un dem server mitgeteilt
		tmp = "AUTH PLAIN";
		sendToServer(tmp);
		
		String antwort = in.readLine();
		checkResponse(antwort, "3");
		
		//wenn das AUTH PLAIN erfolgreich war, kann die mail übertragen werden
		if(authentisierenBase64Plain()){
			tmp="MAIL FROM:<"+absenderMailadresse+">";
			sendToServer(tmp);
			antwort=in.readLine();
			checkResponse(antwort, "2");
			
			for(int i = 0;i < recipientList.size();i++){
				addRecipient(recipientList.get(i));
			}
			
					
			
			tmp= "DATA";
			sendToServer(tmp);
			antwort=in.readLine();
			checkResponse(antwort, "3");
			
			for(int i = 0;i < recipientList.size();i++){
				out.println("From:"+recipientList.get(i));
			}
			
//			out.println("From:<"+absenderMailadresse+">");
			out.println("To:<"+"alle aus unserer Praktikumsgruppe"+">");
			out.println("Subject: TestEmail");
			out.println("\n");
			out.println("Hi, ich wollt euch nur mal nerven! pt2, denn chris war ebennicht mit drinne. :D");
			out.println(".");
			out.flush();
			
			antwort=in.readLine();
			checkResponse(antwort, "2");
			sendToServer("QUIT");
			
		}
		
	}
	private void addRecipient(String adress) throws IOException{
		String tmp= "RCPT TO:<"+adress+">";
		sendToServer(tmp);
		String antwort=in.readLine();
		checkResponse(antwort, "2");
	}
	
	private void errorCheck(String response) throws IOException {
		if(response.startsWith("5")){
			log("S:> Fataler Fehler!");
			
			throw new IOException(response);
		}
		
	}
	private void checkResponse(String input,String expected) throws IOException{
		log("S:> "+input);
		if(!input.startsWith(expected)){
			try {
				errorCheck(input);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}else if(input.charAt(3) == '-'){
			checkResponse(in.readLine(), expected);
		}
		
	}
	private void sendToServer(String message){
		out.println(message);
		out.flush();
		log("\tC:> "+message);
	}

	private void log(String log){
		if(consoleLog){
			System.out.println(log);
		}
	}
	public void closeConnection(){
		try {
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private boolean authentisierenBase64Plain(){

		
		Encoder encoder = Base64.getEncoder();
		Decoder decoder = Base64.getDecoder();
		

		String erg = new String(encoder.encode(("\0"+username+"\0"+password).getBytes()));
		
//		log("\tC:> "+erg);
		sendToServer(erg);
		
		erg = "5";
		try {
			erg= in.readLine();
			checkResponse(erg, "2");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return erg.startsWith("2");
		
	}
	private void sendAttachment(File file){
		//mimetype machen..
		//bytearray der files geben lassen und die dann in base 64 codieren
	}
	/**
	 * fügt den übergebenen String zur Liste der Empfänger hinzu
	 * @param recipient
	 */
	public void addRecipients(String recipient){
		//noch testen ob der übergebene String auch eine emailadresse ist..
		recipientList.add("<"+recipient+">");
	}
	/**
	 * fügt das File einer liste hinzu die der mail angehängt wird
	 * @param file
	 */
	public void addAttachments(File file){
		if(file != null){
			attachments.add(file);
		}
	}

}
