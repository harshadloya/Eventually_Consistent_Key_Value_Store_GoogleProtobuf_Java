import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.zip.DataFormatException;


public class Controller {

	public static String consistency_level = null;
	private static List<Branches> branchesList = new ArrayList<Branches>();
	private static Socket socket;
	private static String branchName = null;
	private static int key = 0;
	private static String stringValue = null;
	private static String consistentString = null;
	private static ArrayList<String> mapValue = new ArrayList<String>();
    public static Map<Integer,ArrayList<String>> consistentMap = new HashMap<Integer,ArrayList<String>>();

	
	public static void main(String args[]) {
		System.out.println("Client started...");

		if (args.length != 2) {
			System.out
					.println("Arguments missing: \n./branch <brancname> <portnumber>");
			return;
		}
		consistency_level = args[0];
		String fileName = args[1];
		
		String branchName = null;
		String ipAddress = null;
		String portNumber = null;

		try {
			File curDir = new File("");
			String currentFilePath = curDir.getAbsolutePath();
			File readFile = new File(fileName);
			FileProcessor fp = new FileProcessor(fileName);
			fp.openfile();
//			System.out.println(fileName);
			// BufferedReader reader = new BufferedReader(new
			// FileReader(readFile));

			String line = null;
//			System.out.println("Printing lines in branch text file");
			Bank.InitBranch.Builder initBranch = Bank.InitBranch.newBuilder();
			Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage
					.newBuilder();

			while ((line = fp.readLine()) != null) {

				// System.out.println(line);
				String[] lineArr = line.split(" ");
				// System.out.println(lineArr);
//				 System.out.println("1st string: " + lineArr[0]);
//				 System.out.println("2nd string: " + lineArr[1]);
//				 System.out.println("3nd string: " + lineArr[2]);

				branchName = lineArr[0];
				ipAddress = lineArr[1];
				portNumber = lineArr[2];
				
				Branches branches = new Branches();
				branches.setName(branchName);
				branches.setIp(ipAddress);
				branches.setPort(Integer.parseInt(portNumber));
				
				Bank.InitBranch.Branch.Builder br = Bank.InitBranch.Branch
						.newBuilder().setName(branchName).setIp(ipAddress)
						.setPort(Integer.parseInt(portNumber));
				Bank.InitBranch.Branch bb = br.build();

				branchesList.add(branches);
				initBranch.addAllBranches(bb);


			}//while
			messageBuilder.setInitBranch(initBranch);
			Bank.BranchMessage message = messageBuilder.build();
			initialize(message);
			
			put();//how and where to check for consistency level ?
			
		}
		catch(Exception e){e.printStackTrace();}
		
	}
	
	public static void put(){
		
		try {

		Branches getRandomBranch = getRandombranch();
		String toBranchName = getRandomBranch.getName();
		String toIpAddress = getRandomBranch.getIp();
		int toPort = getRandomBranch.getPort();
		System.out.println("Chosen Coordinator-" + toPort + " : "+ toBranchName);
		Timestamp sentTime = new Timestamp(new Date().getTime());
//	    Thread.sleep(1000);
		Timestamp timestamp2 = new Timestamp(new Date().getTime());
		
	    System.out.println(sentTime.toString());
	    
	    String sentDateString = sentTime.toString();
	    String endDate = timestamp2.toString();
	    System.out.println(sentTime.before(timestamp2));

	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
	    Date start = sdf.parse(sentDateString);
	    Date end = sdf.parse(endDate);
	    System.out.println(start.compareTo(end));
	    
System.out.println(start);
System.out.println(end);
	
if (start.compareTo(end) > 0) {
    System.out.println("start is after end");
} else if (start.compareTo(end) < 0) {
    System.out.println("start is before end");
} else if (start.compareTo(end) == 0) {
    System.out.println("start is equal to end");
} else {
    System.out.println("Something weird happened...");
}
	    
	    
	    
	    key = key+1;
		stringValue = "value"+key;
		/*
		mapValue.add(stringValue);
		consistentMap.put(key, mapValue);
		consistentString = stringValue;
		*/
		Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();
		Bank.Transfer.Builder transfer = Bank.Transfer.newBuilder()
				.setKey(key)
				.setValue(stringValue)
				.setTime(sentDateString);
		transfer.build();
		messageBuilder.setTransfer(transfer);
		Bank.BranchMessage message = messageBuilder.build();

			Socket socket = new Socket(toIpAddress,toPort);
			
			//sends message to Coordinator -> what if coordinator is down ?
			message.writeDelimitedTo(socket.getOutputStream());
			
			socket.close();

		}
		catch(Exception e){e.printStackTrace();}
		
	}
	
	public static Branches getRandombranch() {

		Random random = new Random();
		int randomIndex = random.nextInt(branchesList.size() - 0);
		Branches branchToTransfer = branchesList.get(randomIndex);
//		 System.out.println("Get random from branchList size : " +
//				 branchesList.size() + " <> " + branchesList.get(randomIndex).getIp() + " || " + branchesList.get(randomIndex).getPort());
				
		return branchToTransfer;
	}

	public static boolean isHostRunning(String serverIP, int serverPort) { 
	    try (Socket socket = new Socket( serverIP , serverPort)) {
	    	System.out.println("in check try");
	        return true;
	    } catch (IOException ex) {
	        /* ignore */
	    }
	    return false;
	}
	   public static boolean hostAvailabilityCheck(String SERVER_ADDRESS, int TCP_SERVER_PORT) throws UnknownHostException, IOException
	   { 
	       socket = new Socket(SERVER_ADDRESS, TCP_SERVER_PORT);
	       boolean available = true; 
	       try {               
	           if (socket.isConnected())
	           { socket.close();    
	           }               
	           } 
	       catch (UnknownHostException e) 
	           { // unknown host 
	           available = false;
	           socket = null;
	           } 
	       catch (IOException e) { // io exception, service probably not running 
	           available = false;
	           socket = null;
	           } 
	       catch (NullPointerException e) {
	           available = false;
	           socket=null;
	       }


	       return available;   
	   } 
	public static void initialize(Bank.BranchMessage message){
		
			for (Branches branch : branchesList) {
//				System.out.println(branch.name + " " + branch.ip + " "
//						+ branch.port + "\n");
					boolean isRunning = false;
					//isRunning = isHostRunning(branch.getIp(), branch.getPort());
					try {
					socket = new Socket(branch.getIp(), branch.getPort());
					if(socket.isConnected()){
						message.writeDelimitedTo(socket.getOutputStream());
					}
					else{
						System.out.println(" after check try " );
					}

			}
					catch(IOException e)
					{
						System.out.println("Prabhakar not running : "+branch.getIp() + ":"+branch.getPort());
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
//Problem -> ?
//1. cannot handle condition for inactive server -> should not generate exception and silently stand-by
