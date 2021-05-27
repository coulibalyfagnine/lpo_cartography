package Main_App;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class Base_Connexion {
	static Document GetConfigBase(HttpServletRequest request, String export) {
		
		SAXBuilder sxb = new SAXBuilder();
		HttpSession session = request.getSession();
    	ServletContext sc = session.getServletContext();
    	String file = sc.getRealPath("configBase.xml");

		Document documentConfig = new Document(); 

		try
		{
			//On crée un nouveau document JDOM avec en argument le fichier XML
			//Le parsing est terminé ;)
			//documentConfig = sxb.build(new File("C:"+File.separator+"configBase.xml")); // ou fichier Resultat
			documentConfig = sxb.build(new File(file)); // ou fichier Resultat
		}
		catch(JDOMException e){
			System.out.println(e);
		}
		catch (IOException e) {
			System.out.println(e);
		}
		return documentConfig ; 
	}

	static Connection connexionBase(HttpServletRequest request, String export) throws ClassNotFoundException, SQLException{

		//File f = new File("C:"+File.separator+"config.xml");
		Document documentConfig = GetConfigBase(request, export);

		Element root = documentConfig.getRootElement() ; 

		Class.forName(root.getChild("Driver").getTextTrim());
		
		String url = "";

		if (export.contentEquals("VM")) {
			url = root.getChild("jdbc").getTextTrim()+"://"+root.getChild("URL").getTextTrim()+":"+root.getChild("Port").getTextTrim()+"/"+root.getChild("Base").getTextTrim();

		}
		if (export.contentEquals("VM_local")) {
			url = root.getChild("jdbc").getTextTrim()+"://"+root.getChild("URL").getTextTrim()+":"+root.getChild("Port").getTextTrim()+"/"+root.getChild("Base").getTextTrim();

		}
		if (export.contentEquals("local")) {
			url = root.getChild("jdbc").getTextTrim()+"://"+root.getChild("URL").getTextTrim()+":"+root.getChild("Port").getTextTrim()+"/"+root.getChild("Base").getTextTrim();

		}

//		String url = root.getChild("jdbc").getTextTrim()+"://"+root.getChild("URL").getTextTrim()+":"+root.getChild("Port").getTextTrim()+"/"+root.getChild("Base").getTextTrim();

		String user = root.getChild("User").getTextTrim();
		String passwd = root.getChild("Pass").getText();

		Connection connexion = DriverManager.getConnection(url, user, passwd);

		return connexion ;
	}
	

	static void deconnexion(){

	}

}
