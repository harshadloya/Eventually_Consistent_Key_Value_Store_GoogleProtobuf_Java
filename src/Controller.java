import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;



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
	public static int consistency = 0;

	public static void main(String args[]) 
	{
		System.out.println("Client started...");

		if (args.length < 3) {
			System.out.println("Minimum Arguments missing ");
			return;
		}
		consistency_level = args[0];
		String fileName = args[1];
		String operation = args[2];

		String branchName = null;
		String ipAddress = null;
		String portNumber = null;

		try 
		{
			//File curDir = new File("");
			//String currentFilePath = curDir.getAbsolutePath();
			//File readFile = new File(fileName);
			FileProcessor fp = new FileProcessor(fileName);
			fp.openfile();
			//			System.out.println(fileName);
			// BufferedReader reader = new BufferedReader(new
			// FileReader(readFile));

			String line = null;
			//			System.out.println("Printing lines in branch text file");
			Bank.InitBranch.Builder initBranch = Bank.InitBranch.newBuilder().setConsistencylevel(Integer.parseInt(consistency_level));
			Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();

			while ((line = fp.readLine()) != null) 
			{
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

			/*			int hasConsistency = checkConsistency();
			if( (hasConsistency < Integer.parseInt(consistency_level))){
				System.out.println(hasConsistency + " Exiting !! Servers are down !!" + consistency_level);
				System.exit(0);
			}
			 */		
			if(operation.equals("init"))
			{
				initialize(message);
			}

			if(operation.equals("put"))
			{
				//testing flow
				int putKey = Integer.parseInt(args[3]);
				String putValue = args[4];
				put(putKey,putValue);
			}

			if(operation.equals("update"))
			{
				int updateKey = Integer.parseInt(args[3]);
				update(updateKey,args[4]);
			}

			if(operation.equals("read"))
			{
				String readBranch = args[3];
				String readIp = args[4];
				int readPort = Integer.parseInt(args[5]);
				int readKey = Integer.parseInt(args[6]);
				read(readBranch,readIp,readPort,readKey);
			}

		}
		catch(Exception e){e.printStackTrace();}

	}
	/*	
	public static int checkConsistency(){
		int localConsistency = 0;
		System.out.println("Checking Consistency !! ");
		for (Branches branch : branchesList) {
				try {
					socket = new Socket(branch.getIp(), branch.getPort());
					localConsistency = consistency++;
					System.out.println("inside for loop checking consistency..."+consistency);

				}
				catch(IOException e)
				{
					System.out.println("==> Initialize Server not running : "+branch.getIp() + ":"+branch.getPort());
				}
				finally{
					try{					
						socket.close();

					}
					catch(Exception e){}
				}


	}

		System.out.println(localConsistency + " returning checking consistency..."+consistency);
		return consistency;


	}
	 */
	public static void update(int updateKey,String updatedValue)
	{	
		try
		{
			Branches getRandomBranch = getRandombranch();
			String toBranchName = getRandomBranch.getName();
			String toIpAddress = getRandomBranch.getIp();
			int toPort = getRandomBranch.getPort();
			System.out.println("==> Update Chosen Coordinator - - > " + toPort + " : "+ toBranchName);
			Timestamp sentTime = new Timestamp(new Date().getTime());

			//	    System.out.println(sentTime.toString());

			String sentDateString = sentTime.toString();

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
			Date start = sdf.parse(sentDateString);

			key = updateKey;
			stringValue = updatedValue;
			/*
			mapValue.add(stringValue);
			consistentMap.put(key, mapValue);
			consistentString = stringValue;
			 */
			Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();
			Bank.Transfer.Builder transfer = Bank.Transfer.newBuilder()
					.setKey(key)
					.setValue(stringValue)
					.setTime(sentDateString)
					.setFlag(1);
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

	public static void read(String branchName,String readIp,int readPort,int readKey)
	{
		try
		{
			Socket socket = new Socket(readIp, readPort);

			Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();
			Bank.Read.Builder read = Bank.Read.newBuilder()
					.setKey(readKey)
					.setReadflag(1);
			read.build();
			messageBuilder.setRead(read);
			Bank.BranchMessage message = messageBuilder.build();
			message.writeDelimitedTo(socket.getOutputStream());

			//			OutputStream os = socket.getOutputStream();
			//			OutputStreamWriter osw = new OutputStreamWriter(os);
			//			BufferedWriter bw = new BufferedWriter(osw);
			//
			//			String number = "retreivesnapshot 2";
			//
			//			String sendMessage = number + "\n";
			//			bw.write(sendMessage);
			//			bw.flush();


			InputStream is = socket.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String inmessage = br.readLine();
			System.out.println("==> Controller received Message from the server : "+ inmessage);
			
			socket.close();
		}
		catch(Exception e){}

	}

	public static void put(int putKey, String putValue)
	{
		try
		{
			Branches getRandomBranch = getRandombranch();
			String toBranchName = getRandomBranch.getName();
			String toIpAddress = getRandomBranch.getIp();
			int toPort = getRandomBranch.getPort();
			System.out.println("==> Put Chosen Coordinator - -> " + toPort + " : "+ toBranchName);
			Timestamp sentTime = new Timestamp(new Date().getTime());
			//	    Thread.sleep(1000);
			Timestamp timestamp2 = new Timestamp(new Date().getTime());

			//	    System.out.println(sentTime.toString());

			String sentDateString = sentTime.toString();
			String endDate = timestamp2.toString();
			//	    System.out.println(sentTime.before(timestamp2));

			//	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
			//	    Date start = sdf.parse(sentDateString);
			//	    Date end = sdf.parse(endDate);

			//	    System.out.println(start.compareTo(end));

			//System.out.println(start);
			//System.out.println(end);

			//if (sentTime.compareTo(timestamp2) > 0) {
			//    System.out.println("start is after end");
			//} else if (start.compareTo(end) < 0) {
			//    System.out.println("start is before end");
			//} else if (start.compareTo(end) == 0) {
			//    System.out.println("start is equal to end");
			//} else {
			//    System.out.println("Something weird happened...");
			//}



			//	    key = key+1;
			//		stringValue = "value"+key;
			/*
		mapValue.add(stringValue);
		consistentMap.put(key, mapValue);
		consistentString = stringValue;
			 */
			Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();
			Bank.Transfer.Builder transfer = Bank.Transfer.newBuilder()
					.setKey(putKey)
					.setValue(putValue)
					.setTime(sentDateString)
					.setFlag(1);
			transfer.build();
			messageBuilder.setTransfer(transfer);
			Bank.BranchMessage message = messageBuilder.build();

			Socket socket = new Socket(toIpAddress,toPort);

			//sends message to Coordinator -> what if coordinator is down ?
			message.writeDelimitedTo(socket.getOutputStream());

			socket.close();

		}
		catch(Exception e)
		{
			System.out.println("Servers are Down ! No Server to connect. Please try again later!");
		}

	}

	public static Branches getRandombranch() 
	{
		Random random = new Random();
		int randomIndex = random.nextInt(branchesList.size() - 0);
		Branches branchToTransfer = branchesList.get(randomIndex);

		return branchToTransfer;
	}

	public static void initialize(Bank.BranchMessage message)
	{
		for (Branches branch : branchesList)
		{
			//				System.out.println(branch.name + " " + branch.ip + " "
			//						+ branch.port + "\n");
			//boolean isRunning = false;
			//isRunning = isHostRunning(branch.getIp(), branch.getPort());
			try
			{
				socket = new Socket(branch.getIp(), branch.getPort());
				if(socket.isConnected())
				{
					message.writeDelimitedTo(socket.getOutputStream());
				}
			}
			catch(IOException e)
			{
				System.out.println("==> Initialize Server not running : "+branch.getIp() + ":"+branch.getPort());
			}
			finally
			{
				try
				{
					socket.close();
				}
				catch(Exception e){}
			}
		}
	}
}