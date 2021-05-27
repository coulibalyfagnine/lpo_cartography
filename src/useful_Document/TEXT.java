/*
 * @author Ali HASSAN
 *         Yassine 
 */
package useful_Document;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Cette classe contient des traitements génériques de données de textuel.
 * Ces traitements peuvent être utilisés dans n'importe quelle application. Ils ne sont pas spécifiques à notre prototype.
 * @author Ali HASSAN
 */
public class TEXT {
	
	/**
	 * Cette fonction erregistre le contenu d'une variable de type String dans un fichier.
	 *
	 * @param fileName le nom de fichier cible
	 * @param sourcesStr la variable de type String (la source de données)
	 */
	public static void savefile(String fileName, String sourcesStr){
		File file = new File(fileName);
		//System.out.println("file :" + file.getAbsolutePath());
		FileOutputStream fos = null;

		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			fos = new FileOutputStream(file);

			OutputStreamWriter osw = null;
			osw = new OutputStreamWriter(fos,"UTF-8");
			osw.write(sourcesStr.toString());
			osw.flush();
			osw.close();
			fos.flush();
			fos.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	public static void savefile2(String fileName, String sourcesStr){
		try (FileWriter file = new FileWriter(fileName)) {
			 
            file.write(sourcesStr);
            file.flush();
            file.close();
 
        } catch (IOException e) {
            e.printStackTrace();
        }
	
	}
	
	public static void savefile3(String fileName, String sourcesStr){
		BufferedWriter bw = null;
		FileWriter fw = null;

		try {
			fw = new FileWriter(fileName);
			bw = new BufferedWriter(fw);
			bw.write(sourcesStr);
			bw.flush();
			bw.close();
			System.out.println("Done");

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}

		}
	
	}

	public static void savefile4(FileWriter file, String sourcesStr, int count, int countElem){
		try {
//		FileWriter	file = new FileWriter(fileName);
			 
            file.write(sourcesStr);
            
            if (count == countElem) {
                file.flush();
                file.close();
            }
            else {
            	
            }
            
 
        } catch (IOException e) {
        	System.out.println(" erreur ecriture ");
            e.printStackTrace();
        }
	
	}

	public static void copyFile(File source, File dest) throws IOException {
		   InputStream is = null;
		    OutputStream os = null;
		    try {
		        is = new FileInputStream(source);
		        os = new FileOutputStream(dest);
		        byte[] buffer = new byte[1024];
		        int length;
		        while ((length = is.read(buffer)) > 0) {
		            os.write(buffer, 0, length);
		        }
		    } finally {
		        is.close();
		        os.close();
		    }}

}
