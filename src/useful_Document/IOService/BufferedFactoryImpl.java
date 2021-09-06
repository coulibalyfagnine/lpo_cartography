package useful_Document.IOService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BufferedFactoryImpl implements BufferedFactory {

	@Override
	public BufferedReader getBufferedReader(String fileNane) {
		
		try {
			return new BufferedReader(new FileReader(fileNane));
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public BufferedWriter getBufferedWriter(String fileNane, boolean doYouWantToRewriteTheFile) {
		
		try {
			return new BufferedWriter(new FileWriter(fileNane, !doYouWantToRewriteTheFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
