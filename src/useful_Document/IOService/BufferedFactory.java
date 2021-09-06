package useful_Document.IOService;

import java.io.BufferedReader;
import java.io.BufferedWriter;

public interface BufferedFactory {
	
	public BufferedReader getBufferedReader(String fileNane);
	public BufferedWriter getBufferedWriter(String fileNane, boolean doYouWantToRewriteTheFile);
}
