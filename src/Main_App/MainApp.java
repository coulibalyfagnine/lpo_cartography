/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Main_App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.json.simple.parser.ParseException;

import DataObjects.Cube;
import DataObjects.Indicateur;
import Parser.ExecuteResponseXMLA;
import Parser.ExecuteXMLA;
import Parser.LogParser;

/**
 *
 * @author Cl√©ment
 */
public class MainApp {
	static String Rule_name = "";

  /**
   * @param hote 
 * @param disc 
 * @param args the command line arguments
 * @throws IOException 
 * @throws ParseException 
 * @throws SQLException 
   */
  public static String main(HttpServletRequest request, String export, String hote, String disc,String geojsonpath,String templatepath, int date, String protocole) throws IOException, ParseException, SQLException {
    try { 	
    	
    	HttpSession session = request.getSession();
    	ServletContext sc = session.getServletContext();
    	String file = sc.getRealPath("config.xml");
    	
    	System.out.println("" + sc.getRealPath(""));

    	System.out.println(file);
    	
    	File f = new File(file);
        Document config = ExecuteResponseXMLA.getDocument(f);			
        Element root = config.getRootElement();
        Element olapLogFile = root.getChild("olapLogFile");             //Fao_Mondrian.log
        Element responseLogFile = root.getChild("responseLogFile");     //CubeLog.xml
        Element responseTempFile = root.getChild("responseTempFile");   //XMLduTableau.xml
        Element queryTempFile = root.getChild("queryTempFile");         //XMLduSelect.xml
        Element indicatorFile = root.getChild("indicateur") ;
        Element display_confFile = root.getChild("display_conf");

        
        // MACHINE EN LOCAL DANS LOGS DU TOMCAT 8
        File f_olapLogFile = new File(System.getProperty("catalina.base")+File.separator+"logs"+File.separator+ olapLogFile.getText());           //mdx.txt--geomdxISIMA.txt
        
        // MACHINE TOMCAT 6 LOGS C:\apache-tomcat-6.0.35\logs
//        File f_olapLogFile = new File("C:\\apache-tomcat-6.0.35\\logs\\"+ olapLogFile.getText());
        
        
        File f_responseTempFile = new File(sc.getRealPath(responseTempFile.getText())); //XMLduTableau.xml
        File f_queryTempFile = new File(sc.getRealPath(queryTempFile.getText())); //XMLduSelect.xml
        
        String s_queryTempFile = sc.getRealPath(queryTempFile.getText()); 
        String s_CubeLog = sc.getRealPath(responseLogFile.getText()); 
        String s_IndicateurFile = sc.getRealPath(indicatorFile.getText());
        String s_display_conf = sc.getRealPath(display_confFile.getText());

        flusher(f_responseTempFile); 
        XMLflusher(new File(s_CubeLog)); 
        
                
        if(LogParser.getExecuteResponse_optimized(f_olapLogFile,f_responseTempFile,f_queryTempFile)==false){
            System.out.println("No query to parse");
        }else {
        	System.out.println("getExecuteResponse done");
        }
        
       
      System.out.println("On a parsÈ le rÈsultat");
                /* On r√©cup√®re la requ√™te mdx pr√©sente dans le fichier geomdxISIMA et on l'√©crit
                 * avec le reste de la ligne dans le fichier XMLduSelect.xml
                 */
                File f_responseLogFile = new File(sc.getRealPath(responseLogFile.getText())); // CubeLog.xml
       
        //File f_responseLogFile = new File("CubeLog.xml"); // CubeLog.xml
        System.out.println(" ====> " + f_responseTempFile);
        Document d_responseTempFile = ExecuteXMLA.getDocument(f_responseTempFile);
                // On r√©cup√®re avec JSON le contenu du fichier XMLduTableau.xml
                // que l'on a remplit un peu plus t√¥t.

 
        
        // On cr√©e un cube √† partir des donn√©es du fichier XMLduSelect.xml
        Cube cubefound = ExecuteResponseXMLA.findAxes(d_responseTempFile);
        
		//System.out.println(cubefound.toString());
        if(ExecuteResponseXMLA.generateCubeWIKI(cubefound, f_responseLogFile, d_responseTempFile,false)){
            System.out.println("found");
        }else{
            System.out.println("not found");
            //WikiGenerator.entries(cubefound, found,f_wikiDirectory.toString());
        }
 

      // pour crÈer le fichier indicateur.xml
        System.out.println(" s_cubeLog : " + s_CubeLog + " s_indicateur : " + s_IndicateurFile);
      Indicators.execute(s_CubeLog, s_IndicateurFile);
      
      
      List<Indicateur> liste_ind = new ArrayList<Indicateur>();
      List<String> liste_dimensions = new ArrayList<String>();
      List<Measure_Display> liste_mesure_display = new ArrayList<Measure_Display>();

      
//      /*
      //pour charger les donnÈes des indicateurs dans une liste 'liste_ind'
      Indicators.RecupererDonnesIndicateur(s_IndicateurFile,s_queryTempFile, liste_ind, liste_dimensions);
      System.out.println("RecupererDonnesIndicateur done");

      //pour compter les members de chaque dimensions ‡ partire de 'liste_ind' (la liste des indicateurs) afin dans la suite trouver la rËgle d'affichage qui correspond ‡ la requÍte OLAP
      //Indicators.CountMemberIndicateurs(liste_ind, liste_dimensions);
      System.out.println("CountMemberIndicateurs done");

      // Trouver la rËgle d'affichage
      
      
      Rule_name = Measure_Display.getfindRule(s_display_conf, Indicators.dim_member_count, Indicators.NB_total);
      System.out.println("get findRule => " + Rule_name);

      //charger les types d'affichage (selon la rËgle trouvÈe dans l'Ètape prÈcÈdentes) pour les diffÈrentes mesure
      Measure_Display.RecupererMesureDisplay(s_display_conf, liste_mesure_display);
      System.out.println("RecupererMesureDisplay done");


      Element type_carte = root.getChild("type_carte");
      Element geoJSONFile = null;
      Element boundaryFile = null;
      Element templatejsonFile = null;
      Element gmlFile = null, sldFile = null, legende = null, maps_title = null;
      String s_templatejson_File, s_geoJSON_file, s_boundary_file, s_GML_file = null, s_SLD_file = null, s_legende_file = null, s_maps_title_file = null;

      if (type_carte.getText().toUpperCase().contains(("geoJSON").toUpperCase())) 
      {
          System.out.println("type_carte geoJSON");
          
    	  geoJSONFile = root.getChild("geojson");
    	  s_geoJSON_file = geoJSONFile.getText();
    	  boundaryFile = root.getChild("boundary");
    	  s_boundary_file = sc.getRealPath(boundaryFile.getText());

    	  templatejsonFile = root.getChild("templatejson");
    	  s_templatejson_File = sc.getRealPath(templatejsonFile.getText());
//
    	  GeoJSON.execute(request, liste_dimensions, liste_ind, liste_mesure_display, s_geoJSON_file, s_boundary_file, s_templatejson_File, export,hote,disc,geojsonpath,templatepath, date, Rule_name, protocole);

//    	  GeoJSON.execute(request, liste_dimensions, liste_ind, liste_mesure_display, s_geoJSON_file, s_boundary_file, s_templatejson_File, export, date);
    	  return("geoJSON");

      }
      else if (type_carte.getText().toUpperCase().contains(("GML_SLD").toUpperCase()))
      {
          System.out.println("type_carte GML_SLD");

    	  gmlFile = root.getChild("gml") ; 
    	  sldFile = root.getChild("sld") ;
    	  legende = root.getChild("legende");
    	  maps_title = root.getChild("maps_title");
    	  s_GML_file = sc.getRealPath(gmlFile.getText());
    	  s_SLD_file = sc.getRealPath(sldFile.getText());
    	  s_legende_file = sc.getRealPath(legende.getText());
    	  s_maps_title_file = sc.getRealPath(maps_title.getText());
          GML_SLD.execute(request, liste_ind, liste_mesure_display, s_GML_file, s_SLD_file, s_legende_file, s_maps_title_file, export);
          return("GML_SLD");
      }
      else {
    	  System.out.println("type_carte not found");
    	  return("None");
      }
      

//      */
      
      //flusher(f_olapLogFile); 
      
      //ouvrir la page de la carte (des cartes)
//      Element connexion = root.getChild("connexion");  
      
      //java.awt.Desktop.getDesktop().browse(java.net.URI.create("http://localhost:8080/SOLAOP_Tool/map/sld.html?N="+CreateGML.map_number));
      //java.awt.Desktop.getDesktop().browse(java.net.URI.create(connexion.getChildText("tomcat_host")+":"+connexion.getChildText("tomcat_port")+"/"+connexion.getChildText("cartography")+"/map/sld.html?N="+CreateGML.map_number));

      //connexion.getChildText("tomcat_host")+":"+connexion.getChildText("tomcat_port")+"/"+connexion.getChildText("project")+ connexion.getChildText("client_olap"));
    } catch (NoSuchAlgorithmException ex) {
      Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
    } /*catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}*/
	return("None");
  }
  
  public static void flusher(File inFile) 
  {
    FileWriter flush;
    try {
        flush = new FileWriter(inFile, false);
        flush.write("");
        flush.close();
    } catch (Exception e) {
        System.out.println("Unable to flush the file - "+e.getMessage());
    }
  }
  
  public static void XMLflusher(File inFile) 
  {
    SAXBuilder sxb = new SAXBuilder();
    org.jdom2.Document document;
    Element racine;
      try
      {
         //On cr√©e un nouveau document JDOM avec en argument le fichier XML
         //Le parsing est termin√© ;)
         document = sxb.build(inFile);
         racine = document.getRootElement();
         
         List<Element> liste = new ArrayList<Element>() ;
         
         liste = racine.getChildren(); 
         
         Iterator i = liste.iterator();
         while (i.hasNext())
         {
           Element cour = (Element) i.next(); 
           cour.detach() ; 
         }
         XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
         FileOutputStream fos = new FileOutputStream(inFile) ; 
         sortie.output(document, fos);
      }
      catch(JDOMException e){
          System.out.println(e);
      }
      catch(IOException e){
        System.out.println(e);
      }
  }


  
}
