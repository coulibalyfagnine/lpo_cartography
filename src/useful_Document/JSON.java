/*
 * @author Ali HASSAN
 */
package useful_Document;

import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import Main_App.MainApp;

/**
 * Cette classe contient des traitements génériques de données de type JSON.
 * Ces traitements peuvent être utilisés dans n'importe quelle application. Ils ne sont pas spécifiques à notre prototype.
 * @author Ali HASSAN
 */
public class JSON {
	
	/**
	 * Charge le contenu d'un fichier textul dans un objet de type JSONObject.
	 *
	 * @param file le fichier (la source de données)
	 * @return le JSON résultant
	 */
	public static JSONObject getJSONFromFile(File file) {
		JSONObject jsonObject = null;
		try {

			JSONParser jsonParser = new JSONParser();
    		///////////test
 /*   		ContainerFactory containerFactory = new ContainerFactory(){
    	        @Override
    	        public Map createObjectContainer() {
    	            return new LinkedHashMap();
    	        }

    	        @Override
    	        public List creatArrayContainer() {
    	            return null;
    	        }
    	    };*/
    		///////////////

			jsonObject = (JSONObject) jsonParser.parse(new FileReader(file));//, containerFactory);// on crée un JSONObject à partir du fichier

			return jsonObject;


		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return jsonObject; //En cas d'erreur on retournera un document vide

	}
	
	/**
	 * Charge le contenu d'une variable de type String dans un objet de type JSONObject.
	 *
	 * @param JSON_String la variable de type String (la source de données)
	 * @return le JSON résultant
	 */
	public static JSONObject getJSONFromText(String JSON_String) {
		JSONObject jsonObject = null;

		JSONParser jsonParser = new JSONParser();
		try {
			Object object = jsonParser.parse(JSON_String);
			return jsonObject = (JSONObject) object;
		} catch (Exception e) {
			Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, e);
			System.out.println(e);
			return jsonObject;  //on cas d'erreur on retournera un JSONObject vide

		}
	}
}
