package useful_Document;
import useful_Document.TEXT;
import static org.junit.Assert.*;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class TEXTTest {
	
	
	@Test
	public void testSavefile() {
		TEXT.savefile("test/junitTest.txt", "test the 'TEXT.savefile' function");
		
		
		
	
//		fail("Not yet implemented");
	}

	
	@Test
	public void test2Savefile()throws IOException, ParseException, ParserConfigurationException, SAXException {
		String file = "WebContent\\VM_Local.xml";
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		final DocumentBuilder builder = factory.newDocumentBuilder();
		final Document document = builder.parse(file);
		String export = document.getDocumentElement().getElementsByTagName("export").item(0).getTextContent();
		String protocole =document.getDocumentElement().getElementsByTagName("protocole").item(0).getTextContent();	
		String hote =document.getDocumentElement().getElementsByTagName("hote").item(0).getTextContent();		
		String disc =document.getDocumentElement().getElementsByTagName("disc").item(0).getTextContent();
		String geojsonpath =document.getDocumentElement().getElementsByTagName("geojsonpath").item(0).getTextContent();
		String templatepath =document.getDocumentElement().getElementsByTagName("templatepath").item(0).getTextContent();
		
		
		TEXT.savefile(templatepath, "yyy");
	} 
}
