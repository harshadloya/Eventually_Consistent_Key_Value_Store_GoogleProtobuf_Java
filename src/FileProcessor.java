import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileProcessor {

	public String inFile=null;
	private FileInputStream fileInput=null;
	private BufferedReader bufferedReader=null;
	private File file = null;
	
	public FileProcessor(String inFile){
			this.inFile = inFile;
	}
		
	public void openfile(){
		try {
			File file = new File(inFile);
			if(file.exists()){
			fileInput  = new FileInputStream(file);
			bufferedReader = new BufferedReader(new InputStreamReader(fileInput));}
			else{
				//System.out.println(file + " file does not exist");
			}
		}catch(Exception e){
			e.printStackTrace();
		}				
		finally{
			
		}
	}
	
	public String readLine(){
		
		String line = null;
		try{
			if(bufferedReader!=null)
			line = bufferedReader.readLine();			
		}catch(Exception e){
			e.printStackTrace();
			return line;		
		}		finally{
			
		}

		return line;
	}
	
	public void close(){
		try {
			bufferedReader.close();
		}catch (IOException e) {
			e.printStackTrace();
		}		finally{
			
		}

	}
	
}
