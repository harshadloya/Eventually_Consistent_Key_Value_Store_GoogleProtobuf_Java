import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

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
			fileInput  = new FileInputStream(file);
			if(file.exists()){
			bufferedReader = new BufferedReader(new InputStreamReader(fileInput));}
			else{
				System.out.println("Files does not exist");
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
