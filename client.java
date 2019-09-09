import java.io.*; 
import java.net.*; 

class TCPClient { 

	public static void main(String argv[]) throws Exception 
	{
		String username = "";
		String serverIP = "";

      System.out.println("argv: "+ argv.length);
		if (argv.length == 1){
			username = argv[0];
			serverIP = "localhost";
      System.out.println("User: "+username);
		}
		else if (argv.length == 2){
			username = argv[0];
			// if (checkIP(argv[2])){System.out.println("Incorrect IP address"); return;}
			serverIP = argv[1];
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

			if (output == "ERROR 100 Malformed username\n\n"){
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

	inClient(Socket connectionSocket, String username, String serverIP){
		this.connectionSocket = connectionSocket;
		this.username = username;
		this.serverIP = serverIP;
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
        // Display Message
        System.out.println(sender_username + ": " + message);
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

	outClient(Socket connectionSocket, String username, String serverIP){
		this.connectionSocket = connectionSocket;
		this.username = username;
		this.serverIP = serverIP;
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
        data[0] = data[0].substring(1);

        String output = "SEND " + data[0] + "\n" + "Content-length: "+ data[1].length() +"\n\n" + data[1];

        outToServer.writeBytes(output); 

        String response = inFromServer.readLine();

        if (response.equals("SENT " + data[0] + "\n\n")){
          System.out.println("Message Sent");
        } 
        else if (response.equals("ERROR 102 Unable to send\n")){
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

