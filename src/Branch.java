import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Branch 
{
	private static String replicaName = null;
	private static int consistency_level = 0;
	private static int consistency_count = 0;
	private static int portNumber = 0;
	private static InetAddress serveripAddress = null;
	private static String ipAddress = null;
	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;
	public static List<Branches> branchesList = new ArrayList<Branches>();
	public static Map<Integer,ArrayList<String>> consistentMap = new HashMap<Integer,ArrayList<String>>();
	public static Map<Integer,ArrayList<String>> hintsMap = new HashMap<Integer,ArrayList<String>>();
	//public static Map<String,ArrayList<Hints>> hintsMap = new HashMap<String,ArrayList<Hints>>();
	public static Socket socket;
	public static Date maxDate;
	public static Date currDate;
	//    public static String fileName = "branche.txt";
	public static List<Branches> toBeRepairedNodes = new ArrayList<Branches>();

	public static void main(String args[]) 
	{
		if (args.length != 2) 
		{
			System.out.println("Arguments missing: \n./branch <branchname> <portnumber>");
			return;
		}
		
		replicaName = args[0];
		portNumber = Integer.parseInt(args[1]);

		try 
		{
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
			ipAddress = ipAddress.substring(ipAddress.indexOf("/")+1,ipAddress.length());
			System.out.println("Server Running On:");
			System.out.println("=> Hostname : "+ InetAddress.getLocalHost().getHostName());
			System.out.println("==> Ip = "+ ipAddress + "\nPort number=" + portNumber + "\nBranch name : " + replicaName);

			InputStream inputStream;

			//File curDir = new File("");
			//String currentFilePath = curDir.getAbsolutePath();
			String fileName = "write-ahead"+replicaName+":"+ipAddress+":"+portNumber+".txt";
			File readFile = new File(fileName);

			//1> Calling initMethod here-> method that runs to look for 'write-ahead.log' when it recovers.
			if(readFile.exists())
			{
				System.out.println("Replica Recovering!!!");
				rebootCaller();
				System.out.println("Replica Recovered!!!");
			}
			//2>and also pings other replicas to get hints from them
			//			getHints();//how to get branchList


			while (true) 
			{
				clientSocket = serverSocket.accept();
				inputStream = clientSocket.getInputStream();

				final Bank.BranchMessage bm = Bank.BranchMessage.parseDelimitedFrom(inputStream);

				if(bm != null)
				{
					//check for InitBranch
					if(bm.hasInitBranch() && !bm.hasTransfer())
					{
						consistency_level = bm.getInitBranch().getConsistencylevel(); 
						for(Bank.InitBranch.Branch branch : bm.getInitBranch().getAllBranchesList())
						{
							//System.out.println("==== Inside Init Branch branch name : "+ branch.getName()+" ==== ");
							String branchName = branch.getName();
							String branchIp = branch.getIp();
							int branchPort = branch.getPort();

							Branches b = new Branches();
							b.setName(branchName);
							b.setIp(branchIp);
							b.setPort(branchPort);

							branchesList.add(b);
						}
					}
					
					if(bm.hasDecision())
					{
						//System.out.println("===== Decision Called =====");
						int key = bm.getDecision().getKey();
						String value = bm.getDecision().getValue();
						String receivedTime = bm.getDecision().getTime();
						int receivedFlag = bm.getDecision().getFlag();
						ArrayList<String> consistentString = new ArrayList<String>();
						consistentString.add(value);
						consistentString.add(receivedTime);

						//System.out.println("Saving into memory "+ key + " :: "+ receivedFlag + " :: "+ value + " :: " + receivedTime );

						String decision = bm.getDecision().getDecide();
						
						//System.out.println("Getting Response back from coordinator " + decision);
						
						//receive commitMessage
						//if commitMessage == "commit" -> 
						//call saveMapInMemory(key,consistentString) & 	consistentMap.put(key, consistentString);
						//if commitMessage == "abort" -> do nothing 
						//get back messages from other replicas
						
						if(decision.equals("commit"))
						{
							//System.out.println("got into commitmessage");
							saveMapInMemory(key,consistentString);
							//before storing to memory-storage map , store in memory somewhere
							//in all replicas
							consistentMap.put(key, consistentString);
							//store incoming values to 'write-ahead.log' in Persistent Storage
							//so that when replicas restores Map when recovers
							//it should be done for all replicas 
						}
						
						if(decision.equals("abort"))
						{
							//System.out.println("Aborting Put");
						}
						
						if(decision.equals("readrepair"))
						{
							//System.out.println("got into readrepair | will update all replicas with Max Updated Value");
							
							ArrayList<String> consistentValue = consistentMap.get(key);	
							//System.out.println("Consistent value:" + consistentValue);
							String latestValue = consistentValue.get(consistentValue.size()-1);
							String[] split = latestValue.split(",");
							
							//System.out.println("latest value:"+latestValue);
							//System.out.println("split 0:"+split[0]);
							//System.out.println("split 1:"+split[1]);
							
							SimpleDateFormat rsdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
							Date newDate = null;
							if(split.length > 1)
							{
								newDate = rsdf.parse(split[1]);

							}
							else
							{
								newDate = rsdf.parse(split[0]);
							}
							
							Date newDate2 = rsdf.parse(receivedTime);

							if(newDate2.compareTo(newDate) > 0)
							{
								saveMapInMemory(key,consistentString);
								consistentMap.put(key, consistentString);
							}
						}

					}

					if(bm.hasTransfer())
					{
						//System.out.println("============ Transfer Called ================ "+branchesList.size());
						//					System.out.println("Key :" + bm.getTransfer().getKey());
						//					System.out.println("Value :" + bm.getTransfer().getValue());
						int key = bm.getTransfer().getKey();
						String value = bm.getTransfer().getValue();
						String receivedTime = bm.getTransfer().getTime();
						int receivedFlag = bm.getTransfer().getFlag();
						ArrayList<String> consistentString = new ArrayList<String>();
						consistentString.add(value);
						consistentString.add(receivedTime);

						//System.out.println("saving into memory "+ key + " :: "+ receivedFlag );

						if(receivedFlag == 0)
						{
							//send some response to coordinator to ensure message received
							//ensureResponse = "OK";
							//if(decision == null){
							//System.out.println("Sending ensureResponse --------- OK with decision :" );
							OutputStream os = clientSocket.getOutputStream();
							OutputStreamWriter osw = new OutputStreamWriter(os);
							BufferedWriter bw = new BufferedWriter(osw);
							String number = null;
							number = "OK";
							//						String sendMessage = number + "/"+numberList.toString() + "\n";
							String sendMessage = number + "\n";
							bw.write(sendMessage);
							bw.flush();
							//}
						}

						//-> checkthis => send message to all replicas including me(coordinator)
						if(receivedFlag == 1)
						{
							//System.out.println("From Controller -> now sending message to all !");
							sendMessage(key, value, receivedTime, receivedFlag, consistentString);
						}

						/*for (Map.Entry<Integer, ArrayList<String>> entry : consistentMap.entrySet()) 
						{
							System.out.println("== Consistent Key Read : "+entry.getKey());
							System.out.println("\n==Consistent Value Read : "+entry.getValue());
						}*/
					}

					/*
				 if(bm.hasHint){
				 	branchName = bm.getHint.getname -> branch name looking for hint

				 	hintsListToSend = hintsMap.get(branchName:branchIp:branchPort)

				 	then send hintsListToSend backto branchName
				 }

					 */

					if(bm.hasRead())
					{
						final int readKey = bm.getRead().getKey();
						final int readFlag = bm.getRead().getReadflag();


						if(bm.getRead().getReadmethod().equals("hints"))
						{
							//System.out.println("======> Inside reading Hints <======== ");
							if(readFlag==0)
							{
								String readIp = bm.getRead().getReadbranchip();
								String readBranchName = bm.getRead().getReadbranchname();
								int readPort = bm.getRead().getReadbranchport();

								String branchFileName = "hints_"+readBranchName+":"+readIp+":"+readPort;

								FileProcessor fp = new FileProcessor(branchFileName);
								fp.openfile();
								
								String line = null;
								OutputStream os = clientSocket.getOutputStream();
								OutputStreamWriter osw = new OutputStreamWriter(os);
								BufferedWriter bw = new BufferedWriter(osw);
								String number = null;
								number = "";
								
								while ((line = fp.readLine()) != null) 
								{
									String[] lineArr = line.split("\n");
									String[] keyValuePair = lineArr[0].split("/");
									String key = keyValuePair[0];
									String value = keyValuePair[1].substring(1, keyValuePair[1].length()-1);

									number = number+"/"+key +"#"+ value;
									//System.out.println("Incoming Read Key :: "+readKey+"-- == Sending to coordinator returningValue from "+replicaName+":"+ ipAddress+ ":"+portNumber +" || "+number + value);
								}
								String sendMessage = number + "\n";
								//System.out.println("Final Sent String :: "+readKey+"-- == Sending to coordinator returningValue from "+replicaName+":"+ ipAddress+ ":"+portNumber +" || "+sendMessage );
								bw.write(sendMessage);
								bw.flush();

								File f = new File(branchFileName);
								f.delete();
							}

						}

						if(readFlag==1)
						{
							//how to read hints and store them
							String[] hintsByRow = null;
							Map<Integer,ArrayList<String>> writeHintsMap = new HashMap<Integer,ArrayList<String>>();
							for(Branches branch: branchesList)
							{
								//System.out.println("Read Hints from all replicas : ");
								if(branch.getPort() != portNumber)
								{
									try 
									{
										socket = new Socket(branch.getIp(), branch.getPort());
										if(socket.isConnected())
										{
											Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();
											Bank.Read.Builder read = Bank.Read.newBuilder()
													.setKey(readKey)
													.setReadflag(0)
													.setReadmethod("hints")
													.setReadbranchname(replicaName)
													.setReadbranchip(ipAddress)
													.setReadbranchport(portNumber);
											read.build();
											messageBuilder.setRead(read);
											Bank.BranchMessage message = messageBuilder.build();

											//System.out.println("Read Hints from replica : " + branch.getName());
											message.writeDelimitedTo(socket.getOutputStream());

											//get back messages from other replicas
											InputStream is = socket.getInputStream();
											InputStreamReader isr = new InputStreamReader(is);
											BufferedReader br = new BufferedReader(isr);
											String inmessage = br.readLine();

											//System.out.println("incoming message from branch to branch coordinator : "+ inmessage);

											//compare all the timestamps received from values
											hintsByRow = inmessage.split("\\/");
											//System.out.println("******** Hints from For Loop *************");
											for(String hints: hintsByRow)
											{
												if(hints.contains("#"))
												{
													//System.out.println(hints);
													String[] hintsplit = hints.split("#");
													//System.out.println(hintsplit[0]);
													//System.out.println(hintsplit[1]);
													String[] valueSplit = hintsplit[1].split(",");
													String hintMapValue = valueSplit[0];
													String hintMapValueTime = valueSplit[1].substring(1);
													//System.out.println(hintMapValue);
													//System.out.println(hintMapValueTime);
													ArrayList<String> consistentString = new ArrayList<String>();
													consistentString.add(hintMapValue);
													consistentString.add(hintMapValueTime);

													writeHintsMap.put(Integer.parseInt(hintsplit[0]), consistentString);
												}
											}
											//System.out.println("\n-----Printing whole hints as an Array ");
											String returnValue= hintsByRow[0].substring(2);
											String valueTime =  hintsByRow[1];
											//System.out.println(returnValue+" :: Just Time of Value <======= "+ valueTime);
											//									SimpleDateFormat rsdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
											//									Date returnDate = rsdf.parse(valueTime);
											//									System.out.println(" To Time :: "+returnDate);
										}
									}
									catch(Exception e){

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
							}//for loop ends
							//System.out.println("********* Reading hints after For Loop ****** ");
							for (Map.Entry<Integer, ArrayList<String>> entry : writeHintsMap.entrySet()) 
							{
								//System.out.println("== Consistent Key Read : "+entry.getKey());
								//System.out.println("\n==Consistent Value Read : "+entry.getValue());
								int key = entry.getKey();
								ArrayList<String> consistentString = entry.getValue();
								try
								{
									File writeHintsCurDir = new File("");
									String writeHintsCurrentFilePath = writeHintsCurDir.getAbsolutePath();
									File writeFile = new File(writeHintsCurrentFilePath + "/" + "write-ahead"+replicaName+":"+ipAddress+":"+portNumber+".txt");
									//System.out.println("----writing----"+writeFile.toString());
									
									if(!writeFile.exists()){
										writeFile.createNewFile();
									}
									
									FileWriter aWriter = new FileWriter(writeFile, true);
									aWriter.write(key +"/" + consistentString + "\n");
									aWriter.flush();
									aWriter.close();
								}
								catch(Exception e){e.printStackTrace();}

							}

							//					for(String hints: hintsByRow){
							//						
							//						System.out.println(hints + " : key : "+ readKey);
							//						String[] hintSplit = hints.split("\\,");
							//						if(!hintSplit[0].equals(" ") && !hintSplit.equals("")){
							//						System.out.println(hintSplit[0]+" first value" );
							//System.out.println(hintSplit[1]+" second value" );

							//						String consistentMapValue = hintSplit[0];
							//						System.out.println(hintSplit[1]);
							//String consistentMapTime = hintSplit[1];
							/*						ArrayList<String> consistentString = new ArrayList<String>();
						consistentString.add(consistentMapValue);
						consistentString.add(consistentMapTime);
						consistentMap.put(readKey, consistentString);

						//save to write-ahead.txt

						try{
							File writeHintsCurDir = new File("");
							String writeHintsCurrentFilePath = curDir.getAbsolutePath();
							File writeFile = new File(writeHintsCurrentFilePath + "/" + "write-ahead"+replicaName+":"+ipAddress+":"+portNumber+".txt");
							System.out.println("----writing----"+writeFile.toString());
							if(!writeFile.exists()){
								writeFile.createNewFile();
							}
							FileWriter aWriter = new FileWriter(writeFile, true);
					        aWriter.write(readKey +"/" + consistentString + "\n");
					        aWriter.flush();
					        aWriter.close();

							}
							catch(Exception e){e.printStackTrace();}
							 */					

							//then save to consistentMap

							//					}
							//					}

						}


						/*
						 * Run all below in a thread to keep it running in background
						 * 
						 */
						//					new clientThread(readKey);
						new Thread(new Runnable() 
						{
							public void run() {
								// code goes here.
								//System.out.println(consistentMap.get(readKey) + " : <== Reading from coordinator with key : "+ readKey);
								//System.out.println("--> now Read values corresponding to this key from all replicas :");

								//put a flag for request coming from which replica -> coordinator/others
								//if(flag==0){send back consisentMap Value to the coordinator} 
								if(readFlag==0 && bm.getRead().getReadmethod().equals("readrepair"))
								{
									try{
										//test send back consistent value to controller
										OutputStream os = clientSocket.getOutputStream();
										OutputStreamWriter osw = new OutputStreamWriter(os);
										BufferedWriter bw = new BufferedWriter(osw);

										//check the consistentMap output ->  checkthis
										//String number = "returningValue ";
										//System.out.println("Incoming Read Key :: "+readKey+"-- == Sending to coordinator returningValue from "+replicaName+":"+ ipAddress+ ":"+portNumber +" || "+number+" | " + consistentMap.get(readKey));
										String sendMessage = "";
										if(consistentMap.containsKey(readKey))
										{
											sendMessage = consistentMap.get(readKey) + "\n";
										}
										else
										{
											sendMessage = "not found"+"\n";
										}
										bw.write(sendMessage);
										bw.flush();
									}
									catch(Exception e){}

								}
								if(readFlag==0 && bm.getRead().getReadmethod().equals("readupdate"))
								{
									//write the max date value to the write-ahead log file : TODO
									//and the update consistentMap
								}


								//if(flag==1) -> set by controller
								//send to all replicas only if flag is == 1 i.e request from controller
								//call all replicas 

								if(readFlag==1)
								{
									int readconsistency_count = 0;
									String returnFinalValue = null;
									String currValueTime = null;
									String valueTime = null;
									String maxTimeString = null;
									
									for(Branches branch: branchesList)
									{
										//System.out.println("not send this MAP to me : " + portNumber + ":");
										if(branch.getPort() != portNumber)
										{
											try 
											{
												socket = new Socket(branch.getIp(), branch.getPort());
												if(socket.isConnected())
												{
													Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();
													Bank.Read.Builder read = Bank.Read.newBuilder()
															.setKey(readKey)
															.setReadmethod("readrepair")
															.setReadflag(0);
													read.build();
													messageBuilder.setRead(read);
													Bank.BranchMessage message = messageBuilder.build();

													message.writeDelimitedTo(socket.getOutputStream());

													//get back messages from other replicas
													InputStream is = socket.getInputStream();
													InputStreamReader isr = new InputStreamReader(is);
													BufferedReader br = new BufferedReader(isr);
													String inmessage = br.readLine();
													readconsistency_count = readconsistency_count + 1;

													String currString = null;
													String consistentMapValue = null;
													boolean isCompared = false;
													String currReturnValue =null;

													Date currDate = null;
													//System.out.println("Branch Cordinator received Message from the server : " + branch.getPort()+"\n"+ inmessage);
													
													if(consistentMap.get(readKey) == null && !inmessage.equals("not found"))
													{
														//System.out.println("No such key value pair available with me !");

														String[] timeOfMessage = inmessage.split(",");
														String returnValue= timeOfMessage[0];
														valueTime =  timeOfMessage[1];
														//System.out.println(valueTime+" :: got this Return as Current value <=======> ");
														SimpleDateFormat rsdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
														currDate = rsdf.parse(valueTime);
														//System.out.println(" To Time :: "+ currDate);
													}
													else if(consistentMap.get(readKey) == null && inmessage.equals("not found"))
													{
														OutputStream os = clientSocket.getOutputStream();
														OutputStreamWriter osw = new OutputStreamWriter(os);
														BufferedWriter bw = new BufferedWriter(osw);

														String sendMessage = "No Such Key available in Map, Try another" + "\n";
														
														bw.write(sendMessage);
														bw.flush();
														return;
													}
													
													if(!isCompared && consistentMap.get(readKey) != null)
													{
														//System.out.println("Comparing with coordinator's timestamp :" + consistentMap.get(readKey));

														currString = consistentMap.get(readKey).toString();
														String[] currTimeOfMessage = currString.split(",");
														currReturnValue = currTimeOfMessage[0];
														currValueTime =  currTimeOfMessage[1];
														//System.out.println(currValueTime+" :: Just Time of Value <======= " + currString);
														SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
														currDate = sdf.parse(currValueTime);
														//System.out.println(" To Time :: "+currDate);
													}

													//compare all the timestamps received from values
													String[] timeOfMessage = inmessage.split(",");
													String returnValue= timeOfMessage[0];
													valueTime =  timeOfMessage[1];
													//System.out.println(valueTime+" :: Just Time of Value <======= ");
													SimpleDateFormat rsdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
													Date returnDate = rsdf.parse(valueTime);
													//System.out.println(" To Time :: "+returnDate);

													//check for consistentMap value -> checkthis
													//System.out.println("Now comparing dates :: curretn:"+currDate+"return:"+returnDate);
													if (currDate.compareTo(returnDate) > 0) 
													{
														isCompared = true;
														maxDate = currDate;
														maxTimeString = currValueTime;
														consistentMapValue = currString;
														returnFinalValue = currReturnValue;
														//System.out.println("currDate is after returnDate " + maxDate+ " send to " + socket);
														//toBeRepairedNodes.add(branch);
													}
													else
													{
														isCompared = true;
														maxDate = returnDate;
														maxTimeString = valueTime;
														consistentMapValue = inmessage;
														currDate = maxDate;
														returnFinalValue = returnValue;
														//System.out.println("returnDate is after currDate " + maxDate);
													}

													//System.out.println(" I have compared all values - now write Max Date value :: ");

													//													if(!toBeRepairedNodes.isEmpty())
													//													{
													//														for(Branches b : toBeRepairedNodes)
													//														{
													//															Socket sock = new Socket(b.getIp(), b.getPort());
													//																															
													//															System.out.println();
													//															Bank.BranchMessage.Builder repairMessageBuilder = Bank.BranchMessage.newBuilder();
													//															Bank.Decision.Builder decision = Bank.Decision.newBuilder()
													//																	.setKey(readKey)
													//																	.setValue(returnFinalValue)
													//																	.setTime(maxDate.toString())
													//																	.setFlag(0)
													//																	.setDecide("readrepair");
													//															decision.build();
													//															repairMessageBuilder.setDecision(decision);
													//															Bank.BranchMessage rMessage = messageBuilder.build();
													//															rMessage.writeDelimitedTo(socket.getOutputStream());
													//															
													//															sock.close();
													//
													//
													//														}
													//														toBeRepairedNodes.clear();
													//													}


													//now write this consistent value to LOG and MAP too !
													//updateWriteAhead();
													//updateConsistentMap();
													//													ArrayList<String> consistentString = new ArrayList<String>();
													//													consistentString.add(consistentMapValue);
													//													consistentMap.put(readKey, consistentString);



												}
												else
												{
													//System.out.println(" after check try " );
												}

											}
											catch(IOException e)
											{
												System.err.println("==> Branch Read not running : "+branch.getIp() + ":"+branch.getPort());
											} 
											catch (ParseException pe) 
											{
												pe.printStackTrace();
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

									}//for loop
									
									readconsistency_count = readconsistency_count+1;
									if(readconsistency_count >= consistency_level)
									{
										try
										{
											for(Branches branch: branchesList)
											{
												//System.out.println("not send this MAP to me : " + portNumber + ":");
												if(branch.getPort() != portNumber)
												{
													try 
													{
														Socket updatesocket = null;
														updatesocket = new Socket(branch.getIp(), branch.getPort());
														if(updatesocket.isConnected())
														{
															returnFinalValue = returnFinalValue.replace("[", "");
															Bank.BranchMessage.Builder repairMessageBuilder = Bank.BranchMessage.newBuilder();
															Bank.Decision.Builder decision = Bank.Decision.newBuilder()
																	.setKey(readKey)
																	.setValue(returnFinalValue)
																	.setTime(maxTimeString.replace("]", ""))
																	.setFlag(0)
																	.setDecide("readrepair");
															decision.build();
															repairMessageBuilder.setDecision(decision);
															Bank.BranchMessage rMessage = repairMessageBuilder.build();
															rMessage.writeDelimitedTo(updatesocket.getOutputStream());

															updatesocket.close();
														}
													}
													catch(Exception e)
													{
													}
												}
											}
											//send back most consistent value to controller
											//test send back consistent value to controller
											OutputStream os = clientSocket.getOutputStream();
											OutputStreamWriter osw = new OutputStreamWriter(os);
											BufferedWriter bw = new BufferedWriter(osw);

											//String number = "returningValue : ";
											//System.out.println("==> In Branch - returningValue + "+number + consistentMap.get(readKey));

											//String sendMessage = number + " :: "+ returnFinalValue +" : "+ maxDate + "\n";
											String sendMessage = returnFinalValue +" : "+ maxDate + "\n";
											
											bw.write(sendMessage);
											bw.flush();
										}
										catch(Exception e){

										}

									}
									else
									{
										//System.out.println("ConsistencyLevel not met !!");
										try
										{
											//send back most consistent value to controller
											//test send back consistent value to controller
											OutputStream os = clientSocket.getOutputStream();
											OutputStreamWriter osw = new OutputStreamWriter(os);
											BufferedWriter bw = new BufferedWriter(osw);

											String number = "Consistency Level not met !";

											String sendMessage = number + "\n";
											bw.write(sendMessage);
											bw.flush();
										}
										catch(Exception e)
										{
										}
									}

								}//if (readFlag)
							}
						}).start();

						//Then call ReadRepair   
						//send back the return consistent value
					}
				}
			}//while
		}//try
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	
	public static void rebootCaller()
	{
		/*System.out.println("Before populating consistentMap : -> \n");
		for (Map.Entry<Integer, ArrayList<String>> entry : consistentMap.entrySet()) {
			System.out.println("Consistent Key : "+entry.getKey());
			System.out.println("\n Consistent Value : "+entry.getValue());
		}*/
		String fileName = "branch.txt";
		//File curDir = new File("");
		//String currentFilePath = curDir.getAbsolutePath();
		//File readBranchFile = new File(fileName);
		FileProcessor branchfp = new FileProcessor(fileName);
		branchfp.openfile();
		// System.out.println(fileName);
		// BufferedReader reader = new BufferedReader(new
		// FileReader(readFile));

		String rBranchline = null;

		while ((rBranchline = branchfp.readLine()) != null) 
		{
			// System.out.println(branchline);
			String[] lineArr = rBranchline.split(" ");
			// System.out.println(lineArr);
			//			 System.out.println("1st string: " + lineArr[0]);
			//			 System.out.println("2nd string: " + lineArr[1]);
			//			 System.out.println("3nd string: " + lineArr[2]);

			Branches branches = new Branches();
			branches.setName(lineArr[0]);
			branches.setIp(lineArr[1]);
			branches.setPort(Integer.parseInt(lineArr[2]));
			
			branchesList.add(branches);
		}// while

		String branchFileName = "write-ahead"+replicaName+":"+ipAddress+":"+portNumber+".txt";
		//File readFile = new File(branchFileName);
		
		FileProcessor fp = new FileProcessor(branchFileName);
		fp.openfile();
		String line = null;
		
		while ((line = fp.readLine()) != null) 
		{
			String[] lineArr = line.split("\n");
			//			System.out.println("This is from file: \n"+lineArr[0]);
			String[] keyValuePair = lineArr[0].split("/");
			//			System.out.println("get key/value from : "+keyValuePair[0] + " :: " +keyValuePair[1] );
			String key = keyValuePair[0];
			String value = keyValuePair[1].substring(1,keyValuePair[1].length()-1);
			ArrayList<String> reconsistentString = new ArrayList<String>();
			reconsistentString.add(value);
			consistentMap.put(Integer.parseInt(key),reconsistentString);
		}
	}

	//all servers have to be active initially
/*	public static void getHints()
	{
		for(Branches branch: branchesList)
		{
			if(branch.getName()!=replicaName)
			{
				//ask for hints from all other replicas
				//branch1:ip:port or use hint proto
			}

		}
	}*/

/*	public static void putCurrent(Bank.BranchMessage bm){

		//check for active replicas first
		//if number of replicas > consisteny level - OK
		//else - throw/send Exception "Server Down Currently"

		//then send to all active replicas

		//else for inactive replicas -> store a hinted-handoff

	}*/

	public static void sendMessage(int key, String value, String receivedTime, int receivedFlag, ArrayList<String> consistentString)
	{
		Socket socket = null;
		//System.out.println("Sending Message --- " + branchesList.size());
		for(Branches branch: branchesList){
			//System.out.println("not send this MAP to me : " + portNumber + ":");
			//comment this -> checkthis
			if(branch.getPort() != portNumber)
			{
				try
				{
					//System.out.println("not comebackto :" + branch.getIp()+":"+ branch.getPort() + ":"+branch.getName());
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

					//get ensureResponse from replicas
					//then add to counter: 
					//if response got == "OK" -> ++counter
					//else -> nothing
					InputStream is = socket.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					String inmessage = br.readLine();
					//System.out.println("----------Got Response from Server"+inmessage+"----------");
					
					if(inmessage.equals("OK"))
					{
						consistency_count = consistency_count + 1;
					}
					else
					{
					}

					//if counter > consistency_level -> send commitMessage = "commit"
					//else -> send commitMessage = "abort"
					//System.out.println("!! ======== Should I wait for everyone to send OK message ======= !!"+consistency_count);
				}
				catch(IOException e)
				{
					try
					{
						//System.out.println("Failed to send MAP message : " + branch.getIp()+":"+branch.getPort());
						//save hints to a log -> in case of reboot
						//save file as : "branch1:ip:port.txt"

						//save 'hints' for this not active branch node
						String hintsKey = "hints_"+branch.getName()+":"+branch.getIp()+":"+branch.getPort();
						String hintsFileName = hintsKey;

						File curDir = new File("");
						String currentFilePath = curDir.getAbsolutePath();
						File writeHintFile = new File(currentFilePath + "/" + hintsFileName);
						//System.out.println("----writing----HINTS ");
						FileWriter aWriter = new FileWriter(writeHintFile, true);
						aWriter.write(key +"/" + consistentString + "\n");
						aWriter.flush();
						aWriter.close();


						int hintKey = key;
						String hintValue = value;
						String hintTime = receivedTime;
						//					ArrayList<String> consistentString = new ArrayList<String>();
						//					consistentString.add(value);
						//					consistentString.add(receivedTime);
						//					Hints hints = new Hints();
						//					hints.setKey(key);
						//					hints.setValue(value);
						//					hints.setTime(receivedTime);
						hintsMap.put(key, consistentString);
					}
					catch(Exception he){}
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
		consistency_count = consistency_count+1; 
		//System.out.println(" After Getting response from all active/inactive server -- "+ consistency_count);
		//here send "Commit" / "Abort" message to replicas
		if(consistency_count >= consistency_level)
		{
			Socket csocket = null;
			//System.out.println("Sending Commit --------- "+branchesList.size());
			for(Branches branch: branchesList)
			{
				//System.out.println("not send this MAP to me : " + portNumber + ":");
				//comment this -> checkthis
				if(branch.getPort() != portNumber)
				{
					try
					{
						//System.out.println("inside try now !");
						Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();
						Bank.Decision.Builder decision = Bank.Decision.newBuilder()
								.setKey(key)
								.setValue(value)
								.setTime(receivedTime)
								.setFlag(0)
								.setDecide("commit");
						decision.build();
						messageBuilder.setDecision(decision);
						Bank.BranchMessage message = messageBuilder.build();
						//System.out.println("inside try before csocket !");						
						csocket = new Socket(branch.getIp(),branch.getPort());
						//System.out.println("inside try now after csocket !");
						message.writeDelimitedTo(csocket.getOutputStream());
					}
					catch(Exception e){}
					finally
					{
						try
						{				
							csocket.close();
						}
						catch(Exception e){}
					}
				}
			}
			saveMapInMemory(key, consistentString);
			consistentMap.put(key, consistentString);
		}
		else
		{
			Socket csocket = null;
			//System.out.println("Sending Abort --------- ");
			for(Branches branch: branchesList)
			{
				//System.out.println("not send this MAP to me : " + portNumber + ":");
				//comment this -> checkthis
				if(branch.getPort() != portNumber)
				{
					try
					{
						Bank.BranchMessage.Builder messageBuilder = Bank.BranchMessage.newBuilder();
						Bank.Decision.Builder decision = Bank.Decision.newBuilder()
								.setKey(key)
								.setValue(value)
								.setTime(receivedTime)
								.setFlag(0)
								.setDecide("abort");
						decision.build();
						messageBuilder.setDecision(decision);
						Bank.BranchMessage message = messageBuilder.build();

						csocket = new Socket(branch.getIp(),branch.getPort());
						message.writeDelimitedTo(csocket.getOutputStream());
					}
					catch(Exception e){}
					finally
					{
						try
						{
							csocket.close();
						}
						catch(Exception e){}
					}
				}
			}
		}

	}

	public static void saveMapInMemory(int key, ArrayList<String> consistentString){

		//appends previous contents from previous run !! 

		//call to all replicas and save to persistent storage
		try
		{
			File curDir = new File("");
			String currentFilePath = curDir.getAbsolutePath();
			File writeFile = new File(currentFilePath + "/" + "write-ahead"+replicaName+":"+ipAddress+":"+portNumber+".txt");
			//System.out.println("----writing----"+writeFile.toString());
			if(!writeFile.exists()){
				writeFile.createNewFile();
			}
			FileWriter aWriter = new FileWriter(writeFile, true);
			aWriter.write(key +"/" + consistentString + "\n");
			aWriter.flush();
			aWriter.close();
		}
		catch(Exception e){e.printStackTrace();}
	}
}