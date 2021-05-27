package Parser;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import DataObjects.Query;

public class ExecuteXMLA {

    public static Document getDocument(File f) {
        SAXBuilder sxb = new SAXBuilder();
        Document document = null;
        try {
            document = sxb.build(f);  // on crée un document JDOM à partir du fichier XML à parser
            // le passage en argument à cette méthode on le fait par un constructeur de fichiers
            // new File("chemin");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return document;  //on cas d'erreur on retournera un document vide
        }
        return document;
    }

    public static Query findInstanceOfRequete(Document d) {
        //r�cup�ration de l'�l�ment racine
        Element racine = d.getRootElement();

        //acc�s � l'�l�ment Statement
        Element e = racine.getChild("Body", Namespace.getNamespace("SOAP-ENV", "http://schemas.xmlsoap.org/soap/envelope/"));
        e = e.getChild("Execute", Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis"));
        e = e.getChild("Command", Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis"));
        e = e.getChild("Statement", Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis"));

        //r�cup�ration de la requete
        String Requete = e.getTextTrim();

        Query query = new Query(Requete);

        return query;
    }

    public static boolean generateRequeteWIKI(Query query, File log) {
        //r�cup�ration de l'�l�ment racine
        Document d = getDocument(log);
        Element racine = d.getRootElement();
        List<Element> logQueries = racine.getChildren();

        Iterator<Element> it = logQueries.iterator();
        Element e;
        boolean found = false;
        while (it.hasNext()) {
            e = it.next();
            if (query.getId() == Integer.parseInt(e.getAttributeValue("id"))) {
                found = true;
            }
        }
        if (found == true) {
            return true; //url to wiki
        } else {

            //Ajout  QueryLog
            Element q = new Element("Query");
            racine.addContent(q);
            Attribute id = new Attribute("id", Integer.toString(query.getId()));
            q.setAttribute(id);
            q.setText(query.getQuery().replaceAll("\\s", ""));

            try {
                //On utilise ici un affichage classique avec getPrettyFormat()
                XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
                //Remarquez qu'il suffit simplement de crer une instance de FileOutputStream
                //avec en argument le nom du fichier pour effectuer la srialisation.
                sortie.output(d, new FileOutputStream(log));
            } catch (java.io.IOException ex) {
                System.out.println(ex.getMessage());
            }


            //generate the wiki then return url to it
            return false;
        }
    }
}
