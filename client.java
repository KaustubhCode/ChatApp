import java.io.*; 
import java.net.*; 

class TCPClient { 

	public static void main(String argv[]) throws Exception 
	{
		String username = "";
		String serverIP = "";

		if (argv.length == 2){
			username = argv[1];
			serverIP = "localhost";
		}
		else if (argv.length == 3){
			username = argv[1];
			// if (checkIP(argv[2])){System.out.println("Incorrect IP address"); return;}
			serverIP = argv[2];
		}

		registerSend(username, serverIP);
		registerRcv(username, serverIP);
	}

	public static void registerSend(String username, String serverIP){
		Socket clientSocketIn = new Socket(serverIP, 51234);

		DataOutputStream outToServer = 
		new DataOutputStream(clientSocketIn.getOutputStream()); 


		BufferedReader inFromServer = 
		new BufferedReader(new
			InputStreamReader(clientSocketIn.getInputStream())); 

		String output = "";

		while(output != "REGISTER TOSEND " + username + "\n\n") {

			outToServer.writeBytes("REGISTER TOSEND " + username + "\n\n"); 

			output = inFromServer.readLine(); 

			if (output == "ERROR 100 Malformed username\n\n"){
				System.out.println(output);
				System.exit();
			}

			System.out.println("FROM SERVER: " + output); 
		}

		inClient inConnThread = new inClient(username, serverIP);
		Thread inThread = new Thread(inConnThread);
		inThread.start();
	}
	
	public static void registerRcv(String username, String serverIP){
		Socket clientSocket = new Socket(serverIP, 51235);

		DataOutputStream outToServer = 
		new DataOutputStream(clientSocketOut.getOutputStream()); 


		BufferedReader inFromServer = 
		new BufferedReader(new
			InputStreamReader(clientSocketOut.getInputStream())); 

		String output = "";

		while(output != "REGISTER TORECV " + username + "\n\n") {

			outToServer.writeBytes("REGISTER TORECV " + username + "\n\n"); 

			output = inFromServer.readLine(); 

			if (output == "ERROR 100 Malformed username\n\n"){
				System.out.println(output);
				System.exit();
			}

			System.out.println("FROM SERVER: " + output); 
		}

		outClient outConnThread = new outClient(username, serverIP);
		Thread outThread = new Thread(outConnThread);
		outThread.start();
	}


} 

class inClient implements Runnable{
	Socket connectionSocket;
	String username;
	String serverIP;

	inClient(Socket connectionSocket, String username, String serverIP){
		this.connectionSocket = connectionSocket;
		this.username = username;
		this.serverIP = serverIP;
	}


	public void run(){
		try{ 
			// while (true){;}

			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); 
			DataOutputStream outToServer = new DataOutputStream(connectionSocket.getOutputStream()); 

			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 

			while(true) {

				sentence = inFromUser.readLine();
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
			}
		}catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
}

class outCient implements Runnable{

	Socket connectionSocket;
	String username;
	String serverIP;

	outClient(Socket connectionSocket, String username, String serverIP){
		this.connectionSocket = connectionSocket;
		this.username = username;
		this.serverIP = serverIP;
	}

	public void run(){
		try{
			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));  
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream()); 


			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 

			while(true) {

				sentence = inFromUser.readLine(); 

				outToServer.writeBytes(sentence + '\n'); 

				modifiedSentence = inFromServer.readLine(); 

				System.out.println("FROM SERVER: " + modifiedSentence); 

			}
		}catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
}

