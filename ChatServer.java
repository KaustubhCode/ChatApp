import java.io.*; 
import java.net.*; 
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

class ServerChat {
	// Global HashMap to keep track of Socket Objects
	static ConcurrentHashMap<String, UserData> userSocketMap =  new ConcurrentHashMap<>();	
	
	// Create 2 threads for 2 TCP connections
	public static void main(String argv[]) throws Exception {
		InConnections inConnThreadGen = new InConnections();
		OutConnections outConnThreadGen = new OutConnections();
		Thread inThread = new Thread(inConnThreadGen);
		Thread outThread = new Thread(outConnThreadGen);
		inThread.start();
		outThread.start();
	} 
}

class InConnections implements Runnable{
	public void run(){
		try{
			ServerSocket inSocket = new ServerSocket(51234);		// RX Port
			while(true) { 
				Socket connectionSocket = inSocket.accept();			// Wait for Incoming Connection
				
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
					for (int i=0; i<chk.length; i++){
						if ( !( ((int)chk[i] >= 97 && (int)chk[i] < 123) || ((int)chk[i] >= 48 && (int)chk[i] < 58) ) ){
							valid = false;
							break;
						}
<<<<<<< HEAD
						if (!valid){
							outToClient.writeBytes("ERROR 100 Malformed username\n\n");
							continue;
						}
						// Add User to registered users list
						if (ServerChat.userSocketMap.get(username) == null){
							UserSockets newUser = new UserSockets(connectionSocket, null);
							ServerChat.userSocketMap.put(username, newUser);
						}else{
							ServerChat.userSocketMap.get(username).inSocket = connectionSocket;
						}
						// Registration Successful Acknowledgement Message
						outToClient.writeBytes("REGISTERED TOSEND " + username + "\n\n");
						break;
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
			register();

			// Get Public Key

			// Receive Chat Messages
			while(true) { 
				try{
					inSentence = inFromClient.readLine();
					String username;
					if (inSentence.startsWith("SEND ")){
						username = inSentence.substring("SEND ".length()).toLowerCase();
					}else{ 
						throw new Exception();
					}
					inSentence = inFromClient.readLine();
					int msgLength;
					if (inSentence.startsWith("Content-length: ")){
						msgLength = Integer.parseInt(inSentence.substring("Content-length: ".length() ) );
					}else{ 
						throw new Exception();
					}
					// outToClient.writeBytes(capitalizedSentence); 
				}catch(Exception e) {
					// If header is not complete
					outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
					// Close Socket and re-register
				}
			}
		}catch (Exception e){
			System.out.println(e.getMessage());
			// Close Socket in case of Error
			try{
				connectionSocket.close();
			}catch(Exception ee) { }
		}
	}
}

class OutSocketThread implements Runnable {
	String inSentence;
	Socket connectionSocket;
	BufferedReader inFromClient;
	DataOutputStream outToClient;

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
					// Registration Successful Acknowledgement Message
					outToClient.writeBytes("REGISTERED TORECV " + username + "\n\n");
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
			register();
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}

class UserData {
	Socket inSocket;
	Socket outSocket;

	UserData(Socket inSock, Socket outSock){
		this.inSocket = inSock;
		this.outSocket = outSock;
	}
}