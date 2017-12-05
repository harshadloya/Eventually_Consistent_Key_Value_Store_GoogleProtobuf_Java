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



public class Controller 
{
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
	private static String coordinatorBranchName = "";
	private static String coordinatorIpAddress = "";
	private static int coordinatorPort = -1;

	public static void main(String args[]) 
	{
		System.out.println("Client started...");

		BufferedReader buff = new BufferedReader(new InputStreamReader(System.in));
		Bank.BranchMessage message = null;
		try 
		{
			System.out.println("Enter Consistency Level");
			consistency_level = buff.readLine();

			System.out.println("\nEnter Branch Description File Path with FileName");
			String fileName = buff.readLine();

			String branchName = null;
			String ipAddress = null;
			String portNumber = null;

			FileProcessor fp = new FileProcessor(fileName);
			fp.openfile();

			String line = null;
			Bank.InitBranch.Builder initBranch = Bank.InitBranch.newBuilder().setConsistencylevel(Integer.parseInt(consistency_level));
			Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();

			while ((line = fp.readLine()) != null)  
			{
				String[] lineArr = line.split(" ");

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
			message = messageBuilder.build();
			initialize(message);


			int counter = 0;
			while(true)
			{
				System.out.println("\nSelect the operation to be performed:\n 1. Put\n 2. Update\n 3. Read\n 4. Change Consistency Level\n 5. Exit");
				String choice = "";

				while(choice.equals("") || choice.matches("\\s+"))
				{
					choice = buff.readLine();
				}

				int operationNo = Integer.parseInt(choice);

				switch(operationNo)
				{
				/*case 1:
					if(0 == counter)
					{
						initialize(message);
						counter++;
					}
					else
					{
						System.out.println("Already Initialized, Try Some Other Operation.\n");
					}
					break;*/

				case 1:
					if(0 == counter)
					{
						selectCoordinator();
						counter++;
					}
					System.out.println("Enter Key");
					String key = "";

					while(key.equals("") || key.matches("\\s+"))
					{
						key = buff.readLine();
					}

					int putKey = Integer.parseInt(key);

					System.out.println("Enter Value");
					String putValue = buff.readLine();

					put(putKey, putValue);
					break;

				case 2:
					if(0 == counter)
					{
						selectCoordinator();
						counter++;
					}
					System.out.println("Enter Key To Update");
					key = "";

					while(key.equals("") || key.matches("\\s+"))
					{
						key = buff.readLine();
					}

					int updateKey = Integer.parseInt(key);

					System.out.println("Enter New Value");
					String updateValue = buff.readLine();

					update(updateKey, updateValue);
					break;


				case 3:
					if(0 == counter)
					{
						selectCoordinator();
						counter++;
					}
					System.out.println("Enter Branch Name to Read from");
					String readBranch = buff.readLine();

					System.out.println("Enter Key to Read");

					key = "";
					while(key.equals("") || key.matches("\\s+"))
					{
						key = buff.readLine();
					}

					int readKey = Integer.parseInt(key);

					read(readBranch, readKey);
					break;

				case 4:
					System.out.println("Enter New Consistency Level\n");
					consistency_level = buff.readLine();
					break;

				case 5:
					System.out.println("Client Stopping");
					System.exit(1);

				default:
					System.out.println("Invalid Selection, Please select a number from the given choices");
				}
			}

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
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
	public static void selectCoordinator()
	{
		Branches getRandomBranch = getRandombranch();
		coordinatorBranchName = getRandomBranch.getName();
		coordinatorIpAddress = getRandomBranch.getIp();
		coordinatorPort = getRandomBranch.getPort();
		System.out.println("Chosen Coordinator --> " + coordinatorBranchName);
	}

	public static void update(int updateKey,String updatedValue)
	{	
		try
		{
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

			Socket socket = new Socket(coordinatorIpAddress, coordinatorPort);

			//sends message to Coordinator -> what if coordinator is down ?
			message.writeDelimitedTo(socket.getOutputStream());

			socket.close();

		}
		catch(Exception e){e.printStackTrace();}

	}

	public static void read(String branchName, int readKey)
	{
		String readIp = "";
		int readPort = 0;
		try
		{
			for(Branches b : branchesList)
			{
				if(b.getName().equals(branchName))
				{
					readIp = b.getIp();
					readPort = b.getPort();
					break;
				}
			}

			if(!readIp.equals("") && readPort != 0)
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

				InputStream is = socket.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String inmessage = br.readLine();
				System.out.println("==> Message received from the server :\n"+ inmessage + "\n");

				socket.close();
			}
			else
			{
				System.err.println("No Such Branch exists with name: "+branchName + " , cannot complete read.");
			}
		}
		catch(Exception e){}
	}

	public static void put(int putKey, String putValue)
	{
		try
		{
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

			Socket socket = new Socket(coordinatorIpAddress, coordinatorPort);

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
		int randomIndex = random.nextInt(branchesList.size());
		Branches branchToTransfer = branchesList.get(randomIndex);

		return branchToTransfer;
	}

	public static void initialize(Bank.BranchMessage message)
	{
		for (Branches branch : branchesList)
		{
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