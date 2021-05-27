package Parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

//import edu.stanford.ejalbert.BrowserLauncher;

import DataObjects.Cube;
import DataObjects.Query;

public class WikiGenerator { // c'est une classe utilitaire
	
	public static void entries(Cube c,Query q,String d){
		File f=new File(d+File.separator+c.getId()+".txt");
		try {
			FileWriter fw = new FileWriter(f, false);
			fw.write(" <- [[:start]]\n\n");
			fw.write("==== OLAP Cube ====\n\n");
			fw.write(c.toWikiSyntax()+"\n\n");
			fw.write("==== MDX Query ====\n\n");
			fw.write(q.toWikiSyntax()+"\n\n");
			fw.write("~~DISCUSSION~~\n\n");
			fw.close();			
//			try {
//				BrowserLauncher b;
//				b = new BrowserLauncher();
//			} catch (Exception e) {
//				System.out.println(e.getMessage());
//			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	public static boolean open(String id,String d){
		try {
			File f = new File(d+File.separator+id+".txt");
			if( f.exists()){
//				System.out.println("-------------------true");
//				BrowserLauncher b;
//				b = new BrowserLauncher();
				return true;
			}else{
				System.out.println("-------------------false");
				return false;
			}
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		System.out.println("-------------------false");
		return false;
	}
}
