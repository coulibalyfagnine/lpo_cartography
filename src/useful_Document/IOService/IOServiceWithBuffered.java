package useful_Document.IOService;

import java.io.*;


public class IOServiceWithBuffered implements IOService{
	
	private final BufferedFactory bufferedFactory;
	
	
	
	public IOServiceWithBuffered(BufferedFactory bufferedFactory) {
		super();
		this.bufferedFactory = bufferedFactory;
	}

	@Override
	public String read(String fileName) {
		String result="";
		String line="";
		BufferedReader bufferedReader = bufferedFactory.getBufferedReader(fileName);
		
		try {
			while ((line = bufferedReader.readLine()) != null) {
				result +=line;
			}
			bufferedReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String write(String fileName, String message) {
		BufferedWriter bw = bufferedFactory.getBufferedWriter(fileName, false);
		try {
			bw.write(message + "\n");
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return message;
	}

	@Override
	public String rewrite(String fileName, String message) {
		BufferedWriter bw = bufferedFactory.getBufferedWriter(fileName, true);
		try {
			bw.write(message);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return message;
	}

}
