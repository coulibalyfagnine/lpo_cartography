/*
 * @author Ali HASSAN
 */
package useful_Document;

import java.io.File;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

/**
 * Cette classe contient des traitements g�n�riques de donn�es de type XML.
 * Ces traitements peuvent �tre utilis�s dans n'importe quelle application. Ils ne sont pas sp�cifiques � notre prototype.
 * @author Ali HASSAN
 */
public class XML {
	
	/**
	 * Cette fonction obtient le contenu d'un fichier XML dans un objet de type Document.
	 *
	 * @param f le fichier XML (la source de donn�es)
	 * @return le document r�sultant
	 */
	public static Document getDocument(File f){
		SAXBuilder sxb = new SAXBuilder();
		Document document = null;
		try{
			 document = sxb.build(f);  // on cr�e un document JDOM � partir du fichier XML
		}catch(Exception e){
			System.out.println(e.getMessage());
			return document;  //En cas d'erreur on retournera un document vide
		}
		return document;
	}

}
