public class Branches
{
	private String name; 
	private String ip; 
	private int port; 

	public void setName(String name){
		this.name = name;
	}
	
	public void setIp(String ip){
		this.ip = ip;
	}
	
	public void setPort(int port){
		this.port = port;
	}

	public String getName(){
		return name;
	}
	
	public String getIp(){
		return ip;
	}
	
	public int getPort(){
		return port;
	}
}