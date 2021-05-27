package Parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import org.apache.commons.io.input.ReversedLinesFileReader;

public class LogParser {
        // Récupère le xml de la requête mdx
	public static boolean getQueryToExecute(File f,File queryFile){	    
		Scanner sc;
		int i=0,lastFoundAtIndex=-1;
		String test,tempXMLALine;
		
		try {
				sc = new Scanner(f);
				
				while(sc.hasNextLine()){
					i++;
					test = sc.nextLine();
					if(test.contains(new String("Query to Execute"))){
						lastFoundAtIndex = i;
						System.out.println("1111111111");
					}
				}	
				sc.close();
			} catch (Exception e) {
			System.out.println(" ----"+e.getMessage());
		}
		if(lastFoundAtIndex==-1){
			return false;
		}else{
			i=0;
			try {
				sc = new Scanner(f);
				while(i<lastFoundAtIndex){
					i++;
					sc.nextLine();
				}
				tempXMLALine = new String(sc.nextLine());
				tempXMLALine = tempXMLALine.substring(tempXMLALine.indexOf("<"));
				
				
				FileWriter out = new FileWriter(queryFile,false);
				out.write(tempXMLALine);
				out.close();
				sc.close();
				System.out.println(tempXMLALine);
			} catch (Exception e) {
				System.out.println(" --&&&--"+e.getMessage());
			}
			
			return true;
		}
	}
	
        // Appel : (geomdxISIMA.txt, XMLduTableau.xml) ;
        // Récupère le xml du cube résultat
	public static boolean getExecuteResponse(File f,File responseFile){
            //f : geomdxISIMA.txt
            //responseFile : XMLduTableau.xml

		Scanner sc;
		int i=0,lastFoundAtIndex=-1;
		String test,XMLALine;
		try {
				sc = new Scanner(f);
				while(sc.hasNextLine()){ // sur toutes les lignes de f
					i++;
					test = sc.nextLine();
					if(test.contains(new String("<cxmla:ExecuteResponse"))){
						lastFoundAtIndex = i;
					}// Si la ligne contient la chaine alors on prend l'index de la ligne
				}
				sc.close();
			    System.out.println(new java.util.Date());
			} catch (Exception e) {
			System.out.println(" ---!!!!!!-"+e.getMessage());
		}
		if(lastFoundAtIndex==-1){
			return false;
		}else{ // Si on a trouvé la chaine on a l'indice de la dernière apparition de la phrase
			i=0;
			try {
				sc = new Scanner(f);
				while(i<lastFoundAtIndex-1){
					i++;
					sc.nextLine();
				} // On se place sur la ligne précédant la ligne concernée ("cxmla:ExecuteResponse")
				
				test = sc.nextLine(); // On se place sur la ligne concern�e ("cxmla:ExecuteResponse")
				XMLALine = new String(test);
				XMLALine = XMLALine.substring(XMLALine.indexOf("<"));
                                // On prend la ligne a partir de "<"
				
                while(! test.contains(new String("</cxmla:ExecuteResponse>"))){
					test = sc.nextLine();
					XMLALine = XMLALine + test;
				} // On rajoute toutes les lignes jusqu'à la chaine "</SOAP-ENV:Envelope>"
                                
				FileWriter out = new FileWriter(responseFile,false);
				out.write(XMLALine); // On �crit la chaine dans le fichier de sortie
				out.close();
				sc.close();

				//System.out.println(XMLALine);  //write in file
			}catch (Exception e) {
				System.out.println(" --??????--"+e.getMessage());
			}
			return true;
		}
	}

	public static boolean getExecuteResponse_optimized(File f,File responseFile, File f_queryTempFile){

    ReversedLinesFileReader fr;
	try {
		fr = new ReversedLinesFileReader(f);
	
    String test = "", XMLALine = "",Query="";
    boolean found = false;
    do{
    	test = fr.readLine();
    	if (test.contains(new String("</cxmla:ExecuteResponse>"))) 
    		{found = true;}
    	
    	if (found)
    	{
    		//XMLALine = XMLALine.substring(XMLALine.indexOf("<"));
                            // On prend la ligne a partir de "<"  
    		XMLALine = test + XMLALine ;
    		//System.out.println("XMLALine is :" + XMLALine);

    	}
     }    while ((! test.contains(new String("<cxmla:ExecuteResponse"))) && (test != null ));
    
    found=false;
	//System.out.println("test is :" + test);
    do{
    	test = fr.readLine();
    	if (test.contains(new String("</Statement>"))) 
    		{found = true;}
    	
    	if (found)
    	{
    		//XMLALine = XMLALine.substring(XMLALine.indexOf("<"));
                            // On prend la ligne a partir de "<"  
    		Query = test + Query ;
    		//System.out.println("XMLALine is :" + XMLALine);

    	}
     }    while ((! test.contains(new String("<Statement>"))) && (test != null ));

    
    System.out.println(" ++++++++> " + responseFile);
	FileWriter out = new FileWriter(responseFile,false);
	FileWriter out1 = new FileWriter(f_queryTempFile,false);
	out1.write(Query);
	out.write(XMLALine); // On �crit la chaine dans le fichier de sortie
	out.close();
	out1.close();

    fr.close();
    
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}    

		return true;
}

}
