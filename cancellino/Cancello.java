import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

public class Cancello {
	//String to broadcast (the gate name) (ignored by the tag, almost useless)
	final static String staticAdvertise = "MAIN";

	//Authorized keys (the string transmitted by the tag)
	final static String[] authKeys = {"PLUTONIUM0"};
	//Key names (the key name is sent on Telegram)
	final static String[] keyNames = {"Me"};
	//Ack string to be transmitted when the gate is opened (ignored by the tag)
	final static String staticAck = "URANIUM000";

	//Telegram-cli server port
	final static int TELEGRAM_PORT = 2392;
	//Telegram-cli server address
	final static String TELEGRAM_SERVER = "localhost";

	//The Telegram chat ID; messages will be sent to this chat
	final static String chatID = "user#idXXXXXXXX";


	private static boolean cancelloAperto = false;
	private static boolean flushRXbuffer = false;

	public static void main(String[] args) {
		sendTelegram("Cancellino - Automatic RFID gate opening system: started");
		init();
		try { Thread.sleep(200); }
		catch (InterruptedException e) { e.printStackTrace(); }
		nRF24L01.setRXMode();
		boolean end = false;
		long prevTransmit = System.currentTimeMillis();
		while (end != true) {
			if (flushRXbuffer) {
				flushRXbuffer = false;
				nRF24L01.flushBuffers();
			}

			long currentTime = System.currentTimeMillis();
			/*if (currentTime - prevTransmit > 1000) {
				sendAdvertise();
				nRF24L01.setRXMode();
				try {Thread.sleep(2);} catch (InterruptedException e) {e.printStackTrace();}
				prevTransmit = System.currentTimeMillis();
			}*/
			if (nRF24L01.packetAvail()) {
				//System.out.print("New data: ");
				String data = nRF24L01.getPacket();
				//System.out.println(data);
				parseData(data);
			}
			if (cancelloAperto) {
				cancelloAperto = false;
				prevTransmit = System.currentTimeMillis() + 60000;
				flushRXbuffer = true;
			}
			try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		}    
	}

	public static ReceiveData nRF24L01;

	public static void init() {
		nRF24L01 = new ReceiveData();
		nRF24L01.start();
	}

	public static boolean sendTelegram(String msgToSend) {
		try {
	  		String serverResponse;
	  		Socket clientSocket = new Socket(TELEGRAM_SERVER, TELEGRAM_PORT); //ssh tunnel to a telegram-cli server may be required
	  		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
	  		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	  		outToServer.writeBytes("msg " + chatID + " " + msgToSend + '\n');
	  		serverResponse = inFromServer.readLine();
	  		inFromServer.close();
	  		outToServer.close();
	  		clientSocket.close();
	  		if (serverResponse.trim().equals("ANSWER 8")) {
	  			return true;
	  		}
	  		else { //TODO: manage error
	  			return false;
	  		}
  		}
  		catch (IOException e) {
  			e.printStackTrace();
  		}
  		return false;
  	}

	public static void parseData(String messaggio) {
		//System.out.print(";");

		//if (nearOk() && authOk(messaggio)) { //alternative line that checks signal strength
		if (authOk(messaggio)) {
			if ((authNum(messaggio) < 0) || (authNum(messaggio) >= keyNames.length)) {
				sendTelegram("Gate NOT opened; unauthorized key:  " + messaggio);
			}
			else {
				cancelloAperto = true;

				//Not strictly required
				//sendAck();
				try {
				Clip beepClip = beepSound();
				apriCancello();
				while(beepClip.getMicrosecondLength() > beepClip.getMicrosecondPosition()) {
					try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
				}
				beepClip.close();
				Clip openClip = openSound();
				while(openClip.getMicrosecondLength() > openClip.getMicrosecondPosition()) {
					try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
				}
				openClip.close();
				}
				catch (UnsupportedAudioFileException e) {
					apriCancello();
					e.printStackTrace();
				}
				catch (LineUnavailableException e) {
					apriCancello();
					e.printStackTrace();
				}
				catch (IOException e) {
					apriCancello();
					e.printStackTrace();
				}
				sendTelegram("Gate opened; key: " + keyNames[authNum(messaggio)]);
				flushRXbuffer = true;
			}
			//sendTelegram("Gate opened; key " + keyNames[authNum(messaggio)]);
		}
		else if (authOk(messaggio)) 
			System.out.println("AUTH OK");
		else {
			//System.err.println(";");//System.err.println("Packet Syntax ERR");
			//sendTelegram("Gate opening attempt with incorrect code");
			try {Thread.sleep(5000);} catch (InterruptedException e) {e.printStackTrace();}
		}

	}

	public static void apriCancello() {
		try {
		Process runApriCancello = Runtime.getRuntime().exec("/home/pi/open_gate_raspi.sh");
		runApriCancello.waitFor();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static Clip beepSound() throws UnsupportedAudioFileException, LineUnavailableException, IOException {
		AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File("tone.wav"));
		Clip clip = AudioSystem.getClip();
		clip.open(audioIn);
		clip.start();
		return clip;
	}

	public static Clip openSound() throws UnsupportedAudioFileException, LineUnavailableException, IOException {
		AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File("openSound.wav"));
		Clip clip = AudioSystem.getClip();
		clip.open(audioIn);
		clip.start();
		return clip;
	}

	public static boolean nearOk() {
		if (nRF24L01.signalStrength() > 0) return true;
		return false;
	}

	public static boolean authOk(String messaggio) {
		//return stringCompare(messaggio, staticAuthKey, 8);
		if (authNum(messaggio) >= 0) return true;
		return false;
	}

	public static int authNum(String messaggio) {
		return strInArr(messaggio, authKeys);
	}

	public static int strInArr(String str, String[] arr) {
		for (int cnt = 0; cnt < arr.length; cnt++)
			if (str.contains(arr[cnt]))
				return cnt;
		return -1;
	}

	public static boolean stringCompare(String string1, String string2, int num) {
		for (int cnt = 0; cnt < num; cnt++) 
			if (string1.charAt(cnt) != string2.charAt(cnt))
				return false;
		return true;
	}

	public static void sendAdvertise() {
		sendPacket(staticAdvertise);
	}

	public static void sendAck() {
		sendPacket(staticAck);
	}

	public static void sendPacket(String packetContent) {
		nRF24L01.sendStringNRF(packetContent);
	}
}
