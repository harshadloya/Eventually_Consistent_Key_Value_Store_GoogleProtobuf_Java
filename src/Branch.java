import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class Branch {

	private static String replicaName = null;
	private static int portNumber = 0;
	private static InetAddress serveripAddress = null;
	private static String ipAddress = null;
	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;
	public static List<Branches> branchesList = new ArrayList<Branches>();
    public static Map<Integer,ArrayList<String>> consistentMap = new HashMap<Integer,ArrayList<String>>();
    public static Map<String,ArrayList<Hints>> hintsMap = new HashMap<String,ArrayList<Hints>>();
	
	public static void main(String args[]) {

		if (args.length != 2) {
			System.out
					.println("Arguments missing: \n./branch <brancname> <portnumber>");
			return;
		}
		replicaName = args[0];
		portNumber = Integer.parseInt(args[1]);

		try {

			//load all contents on reboot from 'write-ahead.txt'
			/*
			File curDir = new File("");
			String currentFilePath = curDir.getAbsolutePath();
			File writeFile = new File(currentFilePath + "/" + "write-ahead.txt");
			System.out.println(writeFile.toString());
			FileWriter aWriter = new FileWriter(writeFile, true);
	        aWriter.write("");
	        aWriter.flush();
	        aWriter.close();
*/
			
			serverSocket = new ServerSocket(portNumber);
			serveripAddress = InetAddress.getLocalHost();
			ipAddress = serveripAddress.toString();
			System.out.println("hostname : "
					+ InetAddress.getLocalHost().getHostName());

			System.out.println("Now using ip = "+ ipAddress + "port number=" + portNumber
					+ " with branch name : " + replicaName);

			InputStream inputStream;

			//1> call initMethod here-> method that runs to look for 'write-ahead.log' when it recovers ?
			rebootCaller();
			//2>and also pings other replicas to get hints from them
			getHints();//how to get branchList
			
			
			while (true) {

				//call initMethod here-> method that runs to look for 'write-ahead.log' when it recovers ?

				clientSocket = serverSocket.accept();
				inputStream = clientSocket.getInputStream();
				
				Bank.BranchMessage bm = Bank.BranchMessage.parseDelimitedFrom(inputStream);
				System.out.println(bm);


				//check for InitBranch
				if(bm.hasInitBranch()){
					for(Bank.InitBranch.Branch branch : bm.getInitBranch().getAllBranchesList()){
//						System.out.println(" branch name : "+ branch.getName());
						String branchName = branch.getName();
						String branchIp = branch.getIp();
						int branchPort = branch.getPort();
						
						Branches b = new Branches();
						b.setName(branchName);
						b.setIp(branchIp);
						b.setPort(branchPort);
						
						branchesList.add(b);
																	
					}
					
					
				}//if

				if(bm.hasTransfer()){
					System.out.println(bm.toString());
					System.out.println("Key :" + bm.getTransfer().getKey());
					System.out.println("Value :" + bm.getTransfer().getValue());
					int key = bm.getTransfer().getKey();
					String value = bm.getTransfer().getValue();
					String receivedTime = bm.getTransfer().getTime();
					int receivedFlag = bm.getTransfer().getFlag();
					ArrayList<String> consistentString = new ArrayList<String>();
					consistentString.add(value);
					consistentString.add(receivedTime);
					
					//store incoming values to 'write-ahead.log' in Persistent Storage
					//so that when replicas restores Map when recovers
					//it should be done for all replicas 
					if(receivedFlag == 1){
						sendMessage(key,value,receivedTime,receivedFlag);
					}
					saveMapInMemory(key,consistentString);
					//before storing to memory-storage map , store in memory somewhere
					//in all replicas
					consistentMap.put(key, consistentString);
					
					//send to all active replicas
					//putCurrent(bm);
				}
				
				/*
				 if(bm.hasHint){
				 	branchName = bm.getHint.getname -> branch name looking for hint
				 	
				 	hintsListToSend = hintsMap.get(branchName:branchIp:branchPort)
				 	
				 	then send hintsListToSend backto branchName
				 }
				 
				 */
				
				if(bm.hasRead()){
					int readKey = bm.getRead().getKey();
					
					System.out.println("now Read values corresponding to this key from all replicas :");
				}
				
				
			}//while
		}//try
		catch(Exception e){e.printStackTrace();}
	}
	
	public static void rebootCaller(){
		System.out.println("Before populating consistentMap : -> \n");
		for (Map.Entry<Integer, ArrayList<String>> entry : consistentMap.entrySet()) {
			System.out.println("Consistent Key : "+entry.getKey());
			System.out.println("\n Consistent Value : "+entry.getValue());
		}
		
		File curDir = new File("");
		String currentFilePath = curDir.getAbsolutePath();
		String fileName = "write-ahead.txt";
		File readFile = new File(fileName);
		FileProcessor fp = new FileProcessor(fileName);
		fp.openfile();
		String line = null;
		while ((line = fp.readLine()) != null) {

			String[] lineArr = line.split("\n");
			System.out.println("This is from file: \n"+lineArr[0]);
			String[] keyValuePair = lineArr[0].split("/");
			System.out.println("get key/value from : "+keyValuePair[0] + " :: " +keyValuePair[1] );
			String key = keyValuePair[0];
			String value = keyValuePair[1].substring(1,keyValuePair[1].length()-1);
			ArrayList<String> reconsistentString = new ArrayList<String>();
			reconsistentString.add(value);
			consistentMap.put(Integer.parseInt(key),reconsistentString);
			
			
		}
		
/*		
		System.out.println("After populating consistentMap : -> \n");

		
		for (Map.Entry<Integer, ArrayList<String>> entry : consistentMap.entrySet()) {
			System.out.println("Consistent Key : "+entry.getKey());
			System.out.println("\n Consistent Value : "+entry.getValue());
		}
	*/	
		
	}
	
	//all servers have to be active initially
	public static void getHints(){
		for(Branches branch: branchesList){
			if(branch.getName()!=replicaName){
				//ask for hints from all other replicas
				//probably use proto to send a flag to return 
				//branch1:ip:port or use hint proto
			}
			
		}
	}
	
	public static void putCurrent(Bank.BranchMessage bm){
		
		//check for active replicas first
		//if number of replicas > consisteny level - OK
		//else - throw/send Exception "Server Down Currently"
		
		//then send to all active replicas
		
		//else for inactive replicas -> store a hinted-handoff
		
	}
	
	public static void sendMessage(int key, String value,String receivedTime, int receivedFlag){
		
		Socket socket = null;
		for(Branches branch: branchesList){
			System.out.println("not send this MAP to me : " + portNumber + ":");
			if(branch.getPort() != portNumber){
				try{
					System.out.println("not comebackto :" + branch.getIp()+":"+ branch.getPort() + ":"+branch.getName());
					Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();
					Bank.Transfer.Builder transfer = Bank.Transfer.newBuilder()
							.setKey(key)
							.setValue(value)
							.setTime(receivedTime)
							.setFlag(0);
					transfer.build();
					messageBuilder.setTransfer(transfer);
					Bank.BranchMessage message = messageBuilder.build();

					socket = new Socket(branch.getIp(),branch.getPort());
				
					message.writeDelimitedTo(socket.getOutputStream());
				
			}
				catch(IOException e){
					System.out.println("Failed to send MAP message : " + branch.getIp()+":"+branch.getPort());
					//save 'hints' for this not active branch node
					String hintsKey = branch.getName()+":"+branch.getIp()+":"+branch.getPort();
					Hints hints = new Hints();
					hints.setKey(key);
					hints.setValue(value);
					hints.setTime(receivedTime);
					ArrayList<Hints> hintsList = new ArrayList<Hints>();
					hintsMap.put(hintsKey, hintsList);
					
				}
				finally{
					try{					
						socket.close();

					}
					catch(Exception e){}
				}

		}
		}
	}
	
	public static void saveMapInMemory(int key, ArrayList<String> consistentString){

		//appends previous contents from previous run !! 
		
		//call to all replicas and save to persistent storage
		try{
		File curDir = new File("");
		String currentFilePath = curDir.getAbsolutePath();
		File writeFile = new File(currentFilePath + "/" + "write-ahead.txt");
//		System.out.println(writeFile.toString());
		FileWriter aWriter = new FileWriter(writeFile, true);
        aWriter.write(key +"/" + consistentString + "\n");
        aWriter.flush();
        aWriter.close();

		}
		catch(Exception e){e.printStackTrace();}
	}

}
