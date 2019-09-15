import java.io.*; 
import java.net.*; 
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

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


class ServerChat {
	// Global HashMap to keep track of Socket Objects
	static ConcurrentHashMap<String, UserData> userSocketMap =  new ConcurrentHashMap<>();	
	static int mode;


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

	// Create 2 threads for 2 TCP connections
	public static void main(String argv[]) throws Exception {
		System.out.println("=== Server Initializing ===");
		if (argv.length == 1){
      mode = Integer.parseInt(argv[0]);
		}
		InConnections inConnThreadGen = new InConnections();
		OutConnections outConnThreadGen = new OutConnections();
		Thread inThread = new Thread(inConnThreadGen);
		Thread outThread = new Thread(outConnThreadGen);
		inThread.start();
		outThread.start();
		System.out.println("=== Server Up and Running ===");
	} 
}

class InConnections implements Runnable{
	public void run(){
		try{
			ServerSocket inSocket = new ServerSocket(51234);		// RX Port
			while(true) { 
				Socket connectionSocket = inSocket.accept();			// Wait for Incoming Connection
				
				// System.out.println("In Client Conneted");	// Debug

				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));		// Input Stream
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream()); 		// Output Stream (Used for backend)

				InSocketThread socketThread = new InSocketThread(connectionSocket, inFromClient, outToClient);
				Thread thread = new Thread(socketThread);
				thread.start();  
			}
		}catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
}

class OutConnections implements Runnable{
	public void run(){
		try {
			ServerSocket outSocket = new ServerSocket(51235);		// TX Port
			while(true) { 
				Socket connectionSocket = outSocket.accept();			// Wait for Incoming Connection

				// System.out.println("Out Client Conneted");	// Debug

				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));		// Input Stream (Used for backend)
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream()); 		// Output Stream

				OutSocketThread socketThread = new OutSocketThread(connectionSocket, inFromClient, outToClient);
				Thread thread = new Thread(socketThread);
				thread.start();  
			}
		}catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
}


class InSocketThread implements Runnable {
	String inSentence; 
	Socket connectionSocket;
	BufferedReader inFromClient;
	DataOutputStream outToClient;
	String user;

	InSocketThread (Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient) {
		this.connectionSocket = connectionSocket;
		this.inFromClient = inFromClient;
		this.outToClient = outToClient;
		this.user = "";
	} 

	public void register() throws Exception{
		// Registration
		while (true){
			inSentence = inFromClient.readLine();
			try{
				if (inSentence.startsWith("REGISTER TOSEND ")){
					String username = inSentence.substring("REGISTER TOSEND ".length()).toLowerCase();
					char[] chk = username.toCharArray();

					boolean valid = true;
					if (chk.length == 0){
						valid = false;
					}
					for (int i=0; i<chk.length; i++){
						if ( !( ((int)chk[i] >= 97 && (int)chk[i] < 123) || ((int)chk[i] >= 48 && (int)chk[i] < 58) ) ){
							valid = false;
							break;
						}
					}
					if (!valid){
						outToClient.writeBytes("ERROR 100 Malformed username\n\n");
						continue;
					}
					// Add User to registered users list
					if (ServerChat.userSocketMap.get(username) == null){
						UserData newUser = new UserData(connectionSocket, null);
						ServerChat.userSocketMap.put(username, newUser);
					}else{
						ServerChat.userSocketMap.get(username).inSocket = connectionSocket;
					}
					this.user = username;
					// Registration Successful Acknowledgement Message
					outToClient.writeBytes("REGISTERED TOSEND " + username + "\n\n");
					// Throw away Extra \n
					inFromClient.read();
					break;
				}else{
					throw new Exception();
				}
			}catch (Exception e){
				outToClient.writeBytes("ERROR 101 No user registered\n\n");
			}
		}
	}

	public void run() {
		try{
			// Register
			// System.out.println("### New Client ###");	// Debug
			register();
			System.out.println("### User Registered (In): " + this.user + " ###"); // Debug
			// Get Public Key

			// Receive Chat Messages
			while(true) { 
				try{
					inSentence = inFromClient.readLine();
					if (inSentence.equals("SIGNOUT")){
						try{
							connectionSocket.close();
							System.out.println("### User Signed-Out: " + this.user + " ###"); // Debug
							break;
						}catch(Exception ee) { }
					}
					String recv_username;
					if (ServerChat.mode == 2 || ServerChat.mode == 3){
						if (inSentence.startsWith("FETCHKEY ")){
							recv_username = inSentence.substring("FETCHKEY ".length()).toLowerCase();
						}else{
							outToClient.writeBytes("ERROR 102 Unable to send\n\n");
							continue;
						}
						inSentence = inFromClient.readLine(); // Throw away extra \n
						// Check if Key Exists
						if (ServerChat.userSocketMap.get(recv_username) == null){
							outToClient.writeBytes("ERROR 102 Unable to send\n\n");
							continue;
						}else{
							String key = ServerChat.userSocketMap.get(recv_username).key;
							outToClient.writeBytes("RESPKEY " + key + "\n\n");
						}
						inSentence = inFromClient.readLine();
					}
					if (inSentence.startsWith("SEND ")){
						recv_username = inSentence.substring("SEND ".length()).toLowerCase();
					}else{ 
						throw new Exception();
					}
					// For Mode 3
					String signString = "";
					if (ServerChat.mode == 3){
						String sKey;
						inSentence = inFromClient.readLine();
						if (inSentence.startsWith("SIGNKEY ")){
							sKey = inSentence;
						}else{ 
							throw new Exception();
						}
						String sHash;
						inSentence = inFromClient.readLine();
						if (inSentence.startsWith("SIGN ")){
							sHash = inSentence;
						}else{ 
							throw new Exception();
						}
						signString = sKey + "\n" + sHash + "\n";
					}

					inSentence = inFromClient.readLine();	// Read Content Length
					int msgLength;
					if (inSentence.startsWith("Content-length: ")){
						msgLength = Integer.parseInt(inSentence.substring("Content-length: ".length() ) );
					}else{ 
						throw new Exception();
					}
					inFromClient.readLine();	// Read extra \n
					char[] inp = new char[msgLength];
					inFromClient.read(inp,0,msgLength);
					// for (int i=0; i<msgLength; i++){
					// 	inp[i] = inFromClient.read();
					// }
					String message = new String(inp);
					System.out.println("-=- Recipient: " + recv_username + " Message: " + message); // Debug
					int status = forward_message(recv_username, signString, message, msgLength);	
					if (status == -1){
						outToClient.writeBytes("ERROR 102 Unable to send\n\n");
					}else if(status == 0){
						outToClient.writeBytes("SENT "+recv_username + "\n\n");
					}
					// outToClient.writeBytes(capitalizedSentence); 
				}catch(Exception e) {
					// If header is not complete
					outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
					// Close Socket and re-register
					connectionSocket.close();
					break;
				}
			}
		}catch (Exception e){
			// System.out.println(e.getMessage());
			// Close Socket in case of Error
			try{
				connectionSocket.close();
				System.out.println("### User Disconnected: " + this.user + " ###"); // Debug
			}catch(Exception ee) { }
		}
	}

	public int forward_message(String username, String signStr, String message, int msgLength){
		try{
			if (ServerChat.userSocketMap.get(username) == null){
				return -1;
			}
			if (ServerChat.userSocketMap.get(username).outSocket == null){
				return -1;
			}

			Socket outSocket = ServerChat.userSocketMap.get(username).outSocket;
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(outSocket.getInputStream()));		// Input Stream (Used for backend)
			DataOutputStream outToClient = new DataOutputStream(outSocket.getOutputStream()); 		// Output Stream

			outToClient.writeBytes("FORWARD " + this.user + "\n" + signStr);
			outToClient.writeBytes("Content-length: " + msgLength + "\n\n");
			outToClient.writeBytes(message);
			String inSentence = inFromClient.readLine();
			if (inSentence.equals("RECEIVED " + this.user)){
				inFromClient.readLine(); // throw away extra \n
				System.out.println("--- Success: Message Forward"); // Debug
				return 0;
			}else{
				System.out.println("Recv from fw client: " + inSentence); // Debug
				return -1;
			}
		}catch (Exception e){
			System.out.println("--- Failed: Message Forward ---");
			return -1;
		}
	}
}

class OutSocketThread implements Runnable {
	String inSentence;
	Socket connectionSocket;
	BufferedReader inFromClient;
	DataOutputStream outToClient;
	String user;

	OutSocketThread (Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient) {
	this.connectionSocket = connectionSocket;
	this.inFromClient = inFromClient;
	this.outToClient = outToClient;
	} 

	public void register() throws Exception{
		// Registration
		while (true){
			inSentence = inFromClient.readLine();
			try{
				if (inSentence.startsWith("REGISTER TORECV ")){
					String username = inSentence.substring("REGISTER TORECV ".length()).toLowerCase();
					char[] chk = username.toCharArray();
					boolean valid = true;
					if (chk.length == 0){
						valid = false;
					}
					for (int i=0; i<chk.length; i++){
						if ( !( ((int)chk[i] >= 97 && (int)chk[i] < 123) || ((int)chk[i] >= 48 && (int)chk[i] < 58) ) ){
							valid = false;
							break;
						}
					}
					if (!valid){
						outToClient.writeBytes("ERROR 100 Malformed username\n\n");
						continue;
					}
					// Add User to registered users list
					if (ServerChat.userSocketMap.get(username) == null){
						UserData newUser = new UserData(null, connectionSocket);
						ServerChat.userSocketMap.put(username, newUser);
					}else{
						ServerChat.userSocketMap.get(username).outSocket = connectionSocket;
					}

					// Throw away Extra \n
					inFromClient.read();

					// Get Key from User
					if (ServerChat.mode == 2 || ServerChat.mode == 3){
						outToClient.writeBytes("SEND KEY\n\n");
						inSentence = inFromClient.readLine();
						inSentence += inFromClient.readLine();
						if (inSentence.startsWith("REGISTERKEY ")){
							ServerChat.userSocketMap.get(username).key = inSentence.substring("REGISTERKEY ".length());
						}
					}

					this.user = username;
					// Registration Successful Acknowledgement Message
					outToClient.writeBytes("REGISTERED TORECV " + username + "\n\n");
					break;
				}else{
					throw new Exception();
				}
			}catch (Exception e){
				outToClient.writeBytes("ERROR 101 No user registered\n\n");
			}
		}
	}

	public void run() {
		try{
			// System.out.println("Client Out Thread Created"); // Debug
			register();
			System.out.println("### User Registered (Out): " + this.user + " ###"); // Debug
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}

class UserData {
	Socket inSocket;
	Socket outSocket;
	String key;
	String sign_key;

	UserData(Socket inSock, Socket outSock){
		this.inSocket = inSock;
		this.outSocket = outSock;
	}
}