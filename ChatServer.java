import java.io.*; 
import java.net.*; 
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

class ServerChat {
	static ConcurrentHashMap<String, UserSockets> userSocketMap =  new ConcurrentHashMap<>();	
	
	public static void main(String argv[]) throws Exception {
		InConnections inConnThreadGen = new InConnections();
		OutConnections outConnThreadGen = new OutConnections();
		Thread inThread = new Thread(inConnThreadGen);
		Thread outThread = new Thread(outConnThreadGen);
		inThread.start();
		outThread.start();
		/*
		ServerSocket inSocket = new ServerSocket(51234); 
		ServerSocket outSocket = new ServerSocket(51235); 
		while(true) { 
			Socket connectionSocket = inSocket.accept(); 
			BufferedReader inFromClient = 
			new BufferedReader(new
			InputStreamReader(connectionSocket.getInputStream())); 

			DataOutputStream outToClient = 
			new DataOutputStream(connectionSocket.getOutputStream()); 

			SocketThread socketThread = new SocketThread(connectionSocket, inFromClient, outToClient);
			Thread thread = new Thread(socketThread);
			thread.start();  
		}
		*/
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
	String clientSentence; 
	String capitalizedSentence; 
	Socket connectionSocket;
	BufferedReader inFromClient;
	DataOutputStream outToClient;

	InSocketThread (Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient) {
		this.connectionSocket = connectionSocket;
		this.inFromClient = inFromClient;
		this.outToClient = outToClient;
	} 

	public void run() {
		try{
			// Registration
			while (true){
				clientSentence = inFromClient.readLine();
				try{
					String cmd = clientSentence;		// 15 = Length of "REGISTER TOSEND"
					if (cmd.startsWith("REGISTER TOSEND ")){
						String username = cmd.substring("REGISTER TOSEND ".length()).toLowerCase();
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
							UserSockets newUser = new UserSockets(connectionSocket, null);
							ServerChat.userSocketMap.put(username, newUser);
						}else{
							ServerChat.userSocketMap.get(username).inSocket = connectionSocket;
						}
						// Registration Successful Acknowledgement Message
						outToClient.writeBytes("REGISTERED TOSEND " + username + "\n\n");
						break;
					}else{
						throw new Exception();
					}
				}catch (Exception e){
					outToClient.writeBytes("ERROR 101 No user registered\n\n");
				}
			}

			// Receive Chat Messages
			while(true) { 
				try {

					clientSentence = inFromClient.readLine(); 

					System.out.println(clientSentence);

					capitalizedSentence = clientSentence.toUpperCase() + '\n'; 

					outToClient.writeBytes(capitalizedSentence); 
				} catch(Exception e) {
					try {
					connectionSocket.close();
					} catch(Exception ee) { }
					break;
				}
			}
		}catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
}

class OutSocketThread implements Runnable {
     String clientSentence; 
     String capitalizedSentence; 
     Socket connectionSocket;
     BufferedReader inFromClient;
     DataOutputStream outToClient;
   
     OutSocketThread (Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient) {
	this.connectionSocket = connectionSocket;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
     } 

     public void run() {
       while(true) { 
	   try {

	           clientSentence = inFromClient.readLine(); 

		   System.out.println(clientSentence);

  	         capitalizedSentence = clientSentence.toUpperCase() + '\n'; 

        	   outToClient.writeBytes(capitalizedSentence); 
	   } catch(Exception e) {
		try {
			connectionSocket.close();
		} catch(Exception ee) { }
		break;
	   }
        } 
    }
}

class UserSockets {
	Socket inSocket;
	Socket outSocket;

	UserSockets(Socket inSock, Socket outSock){
		this.inSocket = inSock;
		this.outSocket = outSock;
	}
}