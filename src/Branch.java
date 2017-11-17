import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
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

	
	public static void main(String args[]) {

		if (args.length != 2) {
			System.out
					.println("Arguments missing: \n./branch <brancname> <portnumber>");
			return;
		}
		replicaName = args[0];
		portNumber = Integer.parseInt(args[1]);

		try {

			serverSocket = new ServerSocket(portNumber);
			serveripAddress = InetAddress.getLocalHost();
			ipAddress = serveripAddress.toString();
			System.out.println("hostname : "
					+ InetAddress.getLocalHost().getHostName());

			System.out.println("Now using ip = "+ ipAddress + "port number=" + portNumber
					+ " with branch name : " + replicaName);

			InputStream inputStream;

			//call initMethod here-> method that runs to look for 'write-ahead.log' when it recovers ?
			
			
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
					String receiveTime = bm.getTransfer().getTime();
					ArrayList<String> consistentString = new ArrayList<String>();
					consistentString.add(value);
					consistentString.add(receiveTime);
					
					//store incoming values to 'write-ahead.log' in Persistent Storage
					//so that when replicas restores Map when recovers
					
					saveMapInMemory(bm);
					//before storing to memory-storage map , store in memory somewhere
					//in all replicas
					consistentMap.put(key, consistentString);
					
					//send to all active replicas
					putCurrent(bm);
				}
				
			}//while
		}//try
		catch(Exception e){e.printStackTrace();}
	}
	
	public static void putCurrent(Bank.BranchMessage bm){
		
		//check for active replicas first
		//if number of replicas > consisteny level - OK
		//else - throw/send Exception "Server Down Currently"
		
		//then send to all active replicas
		
		//else for inactive replicas -> store a hinted-handoff
		
	}
	public static void saveMapInMemory(Bank.BranchMessage bm){

		//call to all replicas and save to persistent storage
		try{
		File curDir = new File("");
		String currentFilePath = curDir.getAbsolutePath();
		File writeFile = new File(currentFilePath + "/" + "write-ahead.txt");
		System.out.println(writeFile.toString());
		FileWriter aWriter = new FileWriter(writeFile, true);
        
		Iterator it = consistentMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        aWriter.write(pair.getKey() +":" + pair.getValue() + "\n");
	        aWriter.flush();
	        aWriter.close();
	        it.remove(); // avoids a ConcurrentModificationException
	    }
		}
		catch(Exception e){e.printStackTrace();}
	}

}
