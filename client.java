import java.io.*; 
import java.net.*; 

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

class TCPClient { 
  static int mode;
  static KeyPair keyCollection;
  // Encryption
  private static final String ALGORITHM = "RSA";

  public static byte[] encrypt(byte[] publicKey, byte[] inputData) throws Exception {
    PublicKey key = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKey));
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, key);
    byte[] encryptedBytes = cipher.doFinal(inputData);
    return encryptedBytes;
  }

  public static byte[] decrypt(byte[] privateKey, byte[] inputData) throws Exception {
    PrivateKey key = KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKey));

    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, key);

    byte[] decryptedBytes = cipher.doFinal(inputData);

    return decryptedBytes;
  }

  public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
    SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
    // 512 is keysize
    keyGen.initialize(512, random);
    KeyPair generateKeyPair = keyGen.generateKeyPair();
    return generateKeyPair;
  }

	public static void main(String argv[]) throws Exception 
	{
		String username = "";
		String serverIP = "";

      System.out.println("argv: "+ argv.length);
    if (argv.length == 1){
      mode = 1;
      System.out.println("Mode: " + mode);
      username = argv[0];
      serverIP = "localhost";
      System.out.println("User: "+username);
    }
		if (argv.length == 2){
      mode = Integer.parseInt(argv[0]);
      System.out.println("Mode: " + mode);
			username = argv[1];
			serverIP = "localhost";
      System.out.println("User: "+username);
		}
		else if (argv.length == 3){
      mode = Integer.parseInt(argv[0]);
      System.out.println("Mode: " + mode);
      username = argv[1];
			// if (checkIP(argv[2])){System.out.println("Incorrect IP address"); return;}
			serverIP = argv[2];
		}

    if (mode == 2 || mode == 3){
      keyCollection = generateKeyPair();
    }
		registerSend(username, serverIP);
		registerRcv(username, serverIP);
	}

	public static void registerSend(String username, String serverIP) throws Exception{
		Socket clientSocketOut = new Socket(serverIP, 51234);

		DataOutputStream outToServer = 
		new DataOutputStream(clientSocketOut.getOutputStream()); 


		BufferedReader inFromServer = 
		new BufferedReader(new
			InputStreamReader(clientSocketOut.getInputStream())); 

		String output = "";

		while(!output.equals("REGISTERED TOSEND " + username)) {

			outToServer.writeBytes("REGISTER TOSEND " + username + "\n\n"); 

			output = inFromServer.readLine();
			output += inFromServer.readLine();

			if (output.equals("ERROR 100 Malformed username")){
				System.out.println(output);
				System.exit(0);
			}

			System.out.println("FROM SERVER(send): " + output); 
		}

		outClient outConnThread = new outClient(clientSocketOut, username, serverIP);
		Thread outThread = new Thread(outConnThread);
		outThread.start();
	}
	
	public static void registerRcv(String username, String serverIP) throws Exception{
		Socket clientSocketIn = new Socket(serverIP, 51235);

		DataOutputStream outToServer = 
		new DataOutputStream(clientSocketIn.getOutputStream()); 


		BufferedReader inFromServer = 
		new BufferedReader(new
			InputStreamReader(clientSocketIn.getInputStream())); 

		String output = "";

		while(!output.equals("REGISTERED TORECV " + username)) {

			outToServer.writeBytes("REGISTER TORECV " + username + "\n\n"); 

			output = inFromServer.readLine(); 
			output += inFromServer.readLine();
			
			if (TCPClient.mode == 2 || TCPClient.mode == 3){
				if (output.equals("SEND KEY")){
					String keyToSend = java.util.Base64.getEncoder().encodeToString( TCPClient.keyCollection.getPublic().getEncoded() );
					outToServer.writeBytes("REGISTERKEY " + keyToSend + "\n\n");
					output = inFromServer.readLine(); 
					output += inFromServer.readLine();
        }
      }

			if (output.equals("ERROR 100 Malformed username")){
				System.out.println(output);
				System.exit(0);
			}

			System.out.println("FROM SERVER(rcv): " + output); 
		}

		inClient inConnThread = new inClient(clientSocketIn, username, serverIP);
		Thread inThread = new Thread(inConnThread);
		inThread.start();
	}


} 

class inClient implements Runnable{
	Socket connectionSocket;
	String username;
	String serverIP;
	byte[] privateKey;
	byte[] publicKey;
	int mode;

	inClient(Socket connectionSocket, String username, String serverIP){
		this.connectionSocket = connectionSocket;
		this.username = username;
		this.serverIP = serverIP;
		this.mode = TCPClient.mode;
		if (mode == 2 || mode == 3){
			privateKey = TCPClient.keyCollection.getPrivate().getEncoded();
			publicKey = TCPClient.keyCollection.getPublic().getEncoded();
		}
	}


	public void run(){
		try{ 
      String sentence;
			DataOutputStream outToServer = new DataOutputStream(connectionSocket.getOutputStream()); 
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 

			while(true) {
				sentence = inFromServer.readLine();
        String sender_username;
        if (sentence.startsWith("FORWARD ")){
          sender_username = sentence.substring("FORWARD ".length());
        }else{
          // ghapla
          outToServer.writeBytes("ERROR 103 Header incomplete\n\n");
          continue;
        }
        sentence = inFromServer.readLine(); // Read Content Length
        int msgLength;
        if (sentence.startsWith("Content-length: ")){
          msgLength = Integer.parseInt(sentence.substring("Content-length: ".length() ) );
        }else{ 
          // ghapla
          outToServer.writeBytes("ERROR 103 Header incomplete\n\n");
          continue;
        }
        inFromServer.readLine(); // Read extra \n
        char[] inp = new char[msgLength];
        inFromServer.read(inp,0,msgLength);
        String message = new String(inp);

        if (mode == 2 || mode == 3){
        	byte[] decodedData = java.util.Base64.getDecoder().decode(message); // Convert string to byte data
        	byte[] decryptedData = TCPClient.decrypt(privateKey, decodedData);	// Decrypt data
        	message = new String(decryptedData);
        }

        // Display Message
        System.out.println("#" + sender_username + ": " + message);
        // Confirm Message Received
        outToServer.writeBytes("RECEIVED " + sender_username + "\n\n");

        /*
				if (!parseValid(sentence)){
					System.out.println("Invalid syntax. Type again.");
					continue;
				}

				String[] data = str.split(" ", 2);
				data[0] = data[0].substring(1);

				String output = "SEND " + data[0] + "\n" + "Content-length: "+ data[1].length() +"\n\n" + data[1];

				outToServer.writeBytes(output); 

				String response = inFromServer.readLine();

				if (response == "SENT " + data[0] + "\n\n"){
					System.out.println("Message Sent");
				} 
				else if (response == "ERROR 102 Unable to send\n"){
					System.out.println("Unable to send message");
				}
				else if (response == "ERROR 102 Unable to send\n"){
					System.out.println("Unable to send message");
				}
        */
			}
		}catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
}

class outClient implements Runnable{

	Socket connectionSocket;
	String username;
	String serverIP;
	int mode;

	outClient(Socket connectionSocket, String username, String serverIP){
		this.connectionSocket = connectionSocket;
		this.username = username;
		this.serverIP = serverIP;
		this.mode = TCPClient.mode;
	}

  boolean parseValid(String sentence){
    if (!sentence.startsWith("@")){ return false; }
    String[] data = sentence.split(" ", 2);
    boolean valid = true;
    char[] chk = data[0].substring(1).toCharArray();
    if (chk.length == 0){
      valid = false;
    }
    for (int i=0; i<chk.length; i++){
      if ( !( ((int)chk[i] >= 97 && (int)chk[i] < 123) || ((int)chk[i] >= 48 && (int)chk[i] < 58) ) ){
        valid = false;
        break;
      }
    }
    if (!valid){ return false; }
    return true;
  }

	public void run(){
		try{
      String sentence;
      String response;
			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));  
			DataOutputStream outToServer = new DataOutputStream(connectionSocket.getOutputStream()); 
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 

			while(true) {
				sentence = inFromUser.readLine(); 
        if (!parseValid(sentence)){
          System.out.println("Invalid syntax. Type again.");
          continue;
        }

        String[] data = sentence.split(" ", 2);
        String recp_username = data[0].substring(1);
        String outMessage = data[1];
        String requested_key;
        if (mode == 2 || mode == 3){
        	outToServer.writeBytes("FETCHKEY " + recp_username + "\n\n");
        	response = inFromServer.readLine();
        	response += inFromServer.readLine();
        	if (response.startsWith("RESPKEY ")){
        		requested_key = response.substring("RESPKEY ".length() );
        		byte[] key_encrypt = java.util.Base64.getDecoder().decode(requested_key);
        		byte[] msg_encode = TCPClient.encrypt(key_encrypt, outMessage.getBytes());
        		outMessage = java.util.Base64.getEncoder().encodeToString(msg_encode);
        	}else{
          	System.out.println("Unable to send message: Key Fetch Failed.");
        		continue;
        	}
        }

        String output = "SEND " + recp_username + "\n" + "Content-length: "+ outMessage.length() +"\n\n" + outMessage;

        outToServer.writeBytes(output); 

        response = inFromServer.readLine();
        response += inFromServer.readLine();

        if (response.equals("SENT " + recp_username)){
          System.out.println("Message Sent");
        } 
        else if (response.equals("ERROR 102 Unable to send")){
          System.out.println("Unable to send message");
        }
        else if (response.equals("ERROR 102 Unable to send\n")){
          System.out.println("Unable to send message");
        }

			}
		}catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
}

