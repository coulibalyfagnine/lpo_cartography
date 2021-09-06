package useful_Document.IOService;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class IOServiceWithBufferedTest {
	
	IOServiceWithBuffered iOServiceWithBuffered;
	String outputForRead;
	String outputForWrite;
	String outputForRewrite;
	@Before
	public void setUp() throws Exception {
		iOServiceWithBuffered = new IOServiceWithBuffered(new BufferedFactoryImpl());
		outputForRead = "./test/outputForRead.txt";
		outputForWrite = "./test/outputForWrite.txt";
		outputForRewrite = "./test/outputForRewrite.txt";
	}

	@Test
	public void testRead() {
		String message = "We have just started";
		iOServiceWithBuffered.rewrite(outputForRead, message);
		String result = iOServiceWithBuffered.read(outputForRead);
		System.out.println(result);
		System.out.println(message);
		assertTrue(result.equals(message));
	}

	@Test
	public void testWrite() {
		iOServiceWithBuffered.rewrite(outputForWrite, "");
		String message = "We have just started";
		String result;
		result = iOServiceWithBuffered.write(outputForWrite, message);
		assertTrue(result.equals(message));
		
		message = "Another one";
		
		result = iOServiceWithBuffered.write(outputForWrite, message);
		assertTrue(result.equals(message));
	}

	@Test
	public void testRewrite() {
		String message = "We have just started";
		String result;
		result = iOServiceWithBuffered.rewrite(outputForRewrite, message);
		assertTrue(result.equals(message));
		
		message = "Another one";
		
		result = iOServiceWithBuffered.rewrite(outputForRewrite, message);
		assertTrue(result.equals(message));
	}

}
