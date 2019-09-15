// How to Run:
// java TCPClient Username
// java TCPClient Mode username
// java TCPClient mode username Server_IP
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

import java.security.MessageDigest;

class TCPClient { 
  static int mode;
  static KeyPair encryptionKey;
  static KeyPair signKey;
  // Encryption
  private static final String ALGORITHM = "RSA";

  public static byte[] encryptMessage(byte[] publicKey, byte[] inputData) throws Exception {
    PublicKey key = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKey));
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, key);
    byte[] encryptedBytes = cipher.doFinal(inputData);
    return encryptedBytes;
  }

  public static byte[] decryptMessage(byte[] privateKey, byte[] inputData) throws Exception {
    PrivateKey key = KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKey));
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, key);
    byte[] decryptedBytes = cipher.doFinal(inputData);
    return decryptedBytes;
  }

  public static byte[] encryptSign(byte[] privateKey, byte[] inputData) throws Exception {
    PrivateKey key = KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKey));
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, key);
    byte[] encryptedBytes = cipher.doFinal(inputData);
    return encryptedBytes;
  }

  public static byte[] decryptSign(byte[] publicKey, byte[] inputData) throws Exception {
    PublicKey key = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKey));
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

	public static void main(String argv[])
	{
		try{
			String username = "";
			String serverIP = "";

	    System.out.println("=== Client Initializing ===");	
	    if (argv.length == 1){
	      mode = 1;
	      System.out.println("Mode: " + mode);
	      username = argv[0];
	      serverIP = "localhost";
	      System.out.println("User Name: "+username);
	    }
			if (argv.length == 2){
	      mode = Integer.parseInt(argv[0]);
	      System.out.println("Mode: " + mode);
				username = argv[1];
				serverIP = "localhost";
	      System.out.println("User Name: "+username);
			}
			else if (argv.length == 3){
	      mode = Integer.parseInt(argv[0]);
	      System.out.println("Mode: " + mode);
	      username = argv[1];
				// if (checkIP(argv[2])){System.out.println("Incorrect IP address"); return;}
				serverIP = argv[2];
	      System.out.println("Server IP: " + serverIP);
			}

	    if (mode == 2 || mode == 3){
	      encryptionKey = generateKeyPair();
	      if (mode == 3){
	      	signKey = generateKeyPair();
	      }
	    }
			registerSend(username, serverIP);
			registerRcv(username, serverIP);
		}catch(Exception e){
			System.out.println("### Couldn't connect to Server. ###");
		}
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

			System.out.println("### Registered on Server(SEND) ###"); 
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
					String keyToSend = java.util.Base64.getEncoder().encodeToString( TCPClient.encryptionKey.getPublic().getEncoded() );
					outToServer.writeBytes("REGISTERKEY " + keyToSend + "\n\n");
					output = inFromServer.readLine(); 
					output += inFromServer.readLine();
        }
      }

			if (output.equals("ERROR 100 Malformed username")){
				System.out.println(output);
				System.exit(0);
			}

			System.out.println("### Registered on Server(RECV) ###");  
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
			privateKey = TCPClient.encryptionKey.getPrivate().getEncoded();
			publicKey = TCPClient.encryptionKey.getPublic().getEncoded();
		}
	}


	public void run(){
		try{ 
      String sentence;
			DataOutputStream outToServer = new DataOutputStream(connectionSocket.getOutputStream()); 
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 

			while(true) {
				sentence = inFromServer.readLine();
				if (sentence == null){
					System.out.println("### Server Unreachable. ###");
					System.exit(0);
				}
        String sender_username;
        if (sentence.startsWith("FORWARD ")){
          sender_username = sentence.substring("FORWARD ".length());
        }else{
          // ghapla
          outToServer.writeBytes("ERROR 103 Header incomplete\n\n");
          continue;
        }
        // Mode 3
      	String signKey = "", signHash = "";
        if (mode == 3){
        	sentence = inFromServer.readLine();
        	if (sentence.startsWith("SIGNKEY ")){
	          signKey = sentence.substring("SIGNKEY ".length());
	        }else{
	          outToServer.writeBytes("ERROR 103 Header incomplete\n\n");
	          continue;
	        }
        	sentence = inFromServer.readLine();
        	if (sentence.startsWith("SIGN ")){
	          signHash = sentence.substring("SIGN ".length());
	        }else{
	          outToServer.writeBytes("ERROR 103 Header incomplete\n\n");
	          continue;
	        }
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
        	// Mode 3 Message Integrity Check
        	if (mode == 3){
        		byte[] sKey = java.util.Base64.getDecoder().decode(signKey);
        		byte[] sHash = java.util.Base64.getDecoder().decode(signHash);
        		byte[] decryptedHash = TCPClient.decryptSign(sKey, sHash);
        		String decHash = java.util.Base64.getEncoder().encodeToString(decryptedHash);

	        	MessageDigest md = MessageDigest.getInstance("SHA-256");
						byte[] hash = md.digest(message.getBytes());
						String msgHash = java.util.Base64.getEncoder().encodeToString(hash);
						if (!decHash.equals(msgHash)){
							System.out.println(decHash);
							System.out.println(msgHash);
          		outToServer.writeBytes("ERROR 999 Signature Verification Failed.\n\n");
          		continue;
						}
        	}
        	// Mode 2 decrypt
        	byte[] decodedData = java.util.Base64.getDecoder().decode(message); // Convert string to byte data
        	byte[] decryptedData = TCPClient.decryptMessage(privateKey, decodedData);	// Decrypt data
        	message = new String(decryptedData);
        }

        // Display Message
        System.out.println("#" + sender_username + ": " + message);
        // Confirm Message Received
        outToServer.writeBytes("RECEIVED " + sender_username + "\n\n");

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
    if (data.length < 2){
    	valid = false;
    }else{
    	if (data[1].length() == 0){
    		valid = false;
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
        String requested_key;		// M2
        String signString = "";	// M3
        if (mode == 2 || mode == 3){
        	outToServer.writeBytes("FETCHKEY " + recp_username + "\n\n");
        	response = inFromServer.readLine();
        	response += inFromServer.readLine();
        	if (response.startsWith("RESPKEY ")){
        		requested_key = response.substring("RESPKEY ".length() );
        		byte[] key_encrypt = java.util.Base64.getDecoder().decode(requested_key);
        		byte[] msg_encode = TCPClient.encryptMessage(key_encrypt, outMessage.getBytes());
        		outMessage = java.util.Base64.getEncoder().encodeToString(msg_encode);
        	}else{
          	System.out.println("--- Unable to send message ---");
        		continue;
        	}
        	// Mode 3
        	signString = "";
	        if (mode == 3){
	        	MessageDigest md = MessageDigest.getInstance("SHA-256");
						byte[] shaBytes = md.digest(outMessage.getBytes());
	        	byte[] hash_encrypt = TCPClient.encryptSign(TCPClient.signKey.getPrivate().getEncoded(), shaBytes);
	        	String hash_encode = java.util.Base64.getEncoder().encodeToString(hash_encrypt);
	        	String key_to_send = java.util.Base64.getEncoder().encodeToString(TCPClient.signKey.getPublic().getEncoded());
	        	signString = "SIGNKEY " + key_to_send + "\nSIGN " + hash_encode + "\n";
	        }
        }

        

        String output = "SEND " + recp_username + "\n" + signString + "Content-length: "+ outMessage.length() +"\n\n" + outMessage;

        outToServer.writeBytes(output); 

        response = inFromServer.readLine();
        response += inFromServer.readLine();

        if (response.equals("SENT " + recp_username)){
          System.out.println("--- Message Sent ---");
        } 
        else if (response.equals("ERROR 102 Unable to send")){
          System.out.println("--- Unable to send message ---");
        }
        else if (response.equals("ERROR 102 Unable to send\n")){
          System.out.println("--- Unable to send message ---");
        }

			}
		}catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
}

