/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Main_App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class Measure_Display {
    private String name ;
    private String display;
    private String color;
	private double bar_width;
	private double size_min;
	private double size_max;
	private int count_levels;
		
	//static Map<String, Double> NB_dimensions;
	static String color_Background;

    public Measure_Display() {
    }

    public Measure_Display(String nom) {
        this.name = nom;
    }
    
    public Measure_Display(String nom, String display_type) {
        this.name = nom;
        this.display = display_type;
    }

    public String getMeasureName() {
        return name;
    }

    public void setMeasureName(String nom) {
        this.name = nom;
    }

    public String getDisplayType() {
        return display;
    }
    
    public void setDisplay(String display_type) {
        this.display = display_type;
    }
    
    public double getBar_width() {
        return bar_width;
    }

    public void setBar_width(double bar_width) {
        this.bar_width = bar_width;
    }
    
    public double getSize_min() {
        return size_min;
    }

    public void setSize_min(double bar_min_high) {
        this.size_min = bar_min_high;
    }

    public double getSize_max() {
        return size_max;
    }

    public void setSize_max(double bar_max_high) {
        this.size_max = bar_max_high;
    }
    
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getCount_levels() {
        return count_levels;
    }

    public void setCount_levels(int count_levels) {
        this.count_levels = count_levels;
    }

   public static void findRule(String s_display_conf, Map<String, Double> dim_member_count, int NB_total, String Rule_name) {
    	SAXBuilder sxb = new SAXBuilder();
    	Document doc1;
    	Element Actions = new Element() {};
    	try    	
    	{
    		//On crée un nouveau document JDOM avec en argument le fichier XML
    		doc1 = sxb.build(new File(s_display_conf));
    		Element rac = doc1.getRootElement();    		
//    		System.out.println(rac.getName());
    		Element map_active = rac.getChild("MAP_Active");
//    		System.out.println(map_active.getName());
    		map_active.removeContent();
//			System.out.println("yes test 00000");

    		
    		Element rules = rac.getChild("Display_rule");
    		
    		List ListeRule = rules.getChildren("Rule");

    		Iterator it = ListeRule.iterator();

//    		String Rule_name="";
    		int pref=-1;
    		boolean pass;	
    		while(it.hasNext())
    		{
    			Element courant = (Element)it.next();
    			Element nom = courant.getChild("name") ;
    			Element preference = courant.getChild("preference");
    			Element Conditions = courant.getChild("Conditions");
    			Element NB = Conditions.getChild("NB");
    			Element NB_min = NB.getChild("NB_min");
    			Element NB_max = NB.getChild("NB_max");
				System.out.println(nom.getValue());
    			if (! NB_min.getValue().equals("*"))  
        			{
    				if (NB_total < Integer.parseInt(NB_min.getValue()))
    					continue;//si la règle n'est pas applicable on saute la règle
    				else if	(! NB_max.getValue().equals("*") && NB_total > Integer.parseInt(NB_max.getValue()))    				
    					continue;//si la règle n'est pas applicable on saute la règle
        			}    				    			    			
    			pass=true;
    			Element dims = Conditions.getChild("Dimensions");
    			List dimensions = dims.getChildren("Dimension");
    			Iterator di = dimensions.iterator();
    			
    			while(pass && di.hasNext()){
    				Element dim = (Element)di.next();
    				Element ND_nom = dim.getChild("name");
    				Element ND_min = dim.getChild("NB_min");
    				Element ND_max = dim.getChild("NB_max");
    				double NB_D;

    				//Si la dimensions dans la règle n'intervient pas dans la requète on considère qu'elle un memebre
    				if (! dim_member_count.containsKey(ND_nom.getValue()))
    					NB_D =1;
    				else
    					NB_D = dim_member_count.get(ND_nom.getValue());

        			if (! ND_min.getValue().equals("*"))    				
        				if ( NB_D < Integer.parseInt(ND_min.getValue()))
        					pass = false;//si la règle n'est pas applicable on saute la règle
        				else if	(! ND_max.getValue().equals("*") && NB_D > Integer.parseInt(ND_max.getValue()))    				
        					pass = false;//si la règle n'est pas applicable on saute la règle        			
    			}
    			
    			if (pass) // si la règle est applicable on verifie si elle a la préférence minimal
    			{
    				int pref1 = Integer.parseInt(preference.getValue());
    				if (pref == -1 || pref1 < pref)
    				{
    					pref = pref1;
    					Rule_name = nom.getValue();
    					Actions = courant.getChild("Actions");
    		    		

//    		    		rac.removeChild("MAP_Active");
    		    		//on supprime l'affichage des measures à utiliser 
    		    		//{on va mettre dans la balise 'Measure' dans xml l'affichage qui correspont à la règle choisie}
    					
//    					rac.addContent(courant.getChild("MAP_Active").clone());
    				}
    			}    				
    		}
    		Element maps = rac.getChild("Maps");
    		
    		List ListeAction = Actions.getChildren("Measure");
    		Iterator ac = ListeAction.iterator();
    		while (ac.hasNext()) {
    			Element measure_courant = (Element)ac.next();
    			Element measure = new Element("Measure");
    			//ajouter le nom de la measure
    			measure.addContent(measure_courant.getChild("name").clone());

    			String map_ID = measure_courant.getChildText("Map");
    			Element map = maps.getChild(map_ID);//truover le Map correspondant à la measure de l'action courante
    			List map_detail = map.getChildren();
    			Iterator det_it = map_detail.iterator();
    			while (det_it.hasNext()){
    				Element map_det_cour = (Element)det_it.next();
        			//ajouter les détails d'affichage
    				measure.addContent(map_det_cour.clone());
    			}
    			
    			//ajouter la couleur
    			measure.addContent(measure_courant.getChild("color").clone());
    			map_active.addContent(measure);
			}
    		System.out.println("Rule found : "+Rule_name);
    		    		
    		XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
            sortie.output(rac, new FileOutputStream(s_display_conf));

    	}
    	catch(JDOMException e){
    		System.out.println(e);
    	}
    	catch(IOException e){
    		System.out.println(e);
    	}

    }
   
   public static String getfindRule(String s_display_conf, Map<String, Double> dim_member_count, int NB_total) {
   	SAXBuilder sxb = new SAXBuilder();
   	Document doc1;
   	Element Actions = new Element() {};
   	String Rule_name="";
   	try    	
   	{
   		//On crée un nouveau document JDOM avec en argument le fichier XML
   		doc1 = sxb.build(new File(s_display_conf));
   		Element rac = doc1.getRootElement();    		
//   		System.out.println(rac.getName());
   		Element map_active = rac.getChild("MAP_Active");
//   		System.out.println(map_active.getName());
   		map_active.removeContent();
//			System.out.println("yes test 00000");

   		
   		Element rules = rac.getChild("Display_rule");
   		
   		List ListeRule = rules.getChildren("Rule");

   		Iterator it = ListeRule.iterator();

   		
   		int pref=-1;
   		boolean pass;	
   		while(it.hasNext())
   		{
   			Element courant = (Element)it.next();
   			Element nom = courant.getChild("name") ;
   			Element preference = courant.getChild("preference");
   			Element Conditions = courant.getChild("Conditions");
   			Element NB = Conditions.getChild("NB");
   			Element NB_min = NB.getChild("NB_min");
   			Element NB_max = NB.getChild("NB_max");
				System.out.println(nom.getValue());
   			if (! NB_min.getValue().equals("*"))  
       			{
   				if (NB_total < Integer.parseInt(NB_min.getValue()))
   					continue;//si la règle n'est pas applicable on saute la règle
   				else if	(! NB_max.getValue().equals("*") && NB_total > Integer.parseInt(NB_max.getValue()))    				
   					continue;//si la règle n'est pas applicable on saute la règle
       			}    				    			    			
   			pass=true;
   			Element dims = Conditions.getChild("Dimensions");
   			List dimensions = dims.getChildren("Dimension");
   			Iterator di = dimensions.iterator();
   			
   			while(pass && di.hasNext()){
   				Element dim = (Element)di.next();
   				Element ND_nom = dim.getChild("name");
   				Element ND_min = dim.getChild("NB_min");
   				Element ND_max = dim.getChild("NB_max");
   				double NB_D;

   				//Si la dimensions dans la règle n'intervient pas dans la requète on considère qu'elle un memebre
   				if (! dim_member_count.containsKey(ND_nom.getValue()))
   					NB_D =1;
   				else
   					NB_D = dim_member_count.get(ND_nom.getValue());

       			if (! ND_min.getValue().equals("*"))    				
       				if ( NB_D < Integer.parseInt(ND_min.getValue()))
       					pass = false;//si la règle n'est pas applicable on saute la règle
       				else if	(! ND_max.getValue().equals("*") && NB_D > Integer.parseInt(ND_max.getValue()))    				
       					pass = false;//si la règle n'est pas applicable on saute la règle        			
   			}
   			
   			if (pass) // si la règle est applicable on verifie si elle a la préférence minimal
   			{
   				int pref1 = Integer.parseInt(preference.getValue());
   				if (pref == -1 || pref1 < pref)
   				{
   					pref = pref1;
   					Rule_name = nom.getValue();
   					Actions = courant.getChild("Actions");
   		    		

//   		    		rac.removeChild("MAP_Active");
   		    		//on supprime l'affichage des measures à utiliser 
   		    		//{on va mettre dans la balise 'Measure' dans xml l'affichage qui correspont à la règle choisie}
   					
//   					rac.addContent(courant.getChild("MAP_Active").clone());
   				}
   			}    				
   		}
   		Element maps = rac.getChild("Maps");
   		
   		List ListeAction = Actions.getChildren("Measure");
   		Iterator ac = ListeAction.iterator();
   		while (ac.hasNext()) {
   			Element measure_courant = (Element)ac.next();
   			Element measure = new Element("Measure");
   			//ajouter le nom de la measure
   			measure.addContent(measure_courant.getChild("name").clone());

   			String map_ID = measure_courant.getChildText("Map");
   			Element map = maps.getChild(map_ID);//truover le Map correspondant à la measure de l'action courante
   			List map_detail = map.getChildren();
   			Iterator det_it = map_detail.iterator();
   			while (det_it.hasNext()){
   				Element map_det_cour = (Element)det_it.next();
       			//ajouter les détails d'affichage
   				measure.addContent(map_det_cour.clone());
   			}
   			
   			//ajouter la couleur
   			measure.addContent(measure_courant.getChild("color").clone());
   			map_active.addContent(measure);
			}
   		System.out.println("Rule found : "+Rule_name);
   		    		
   		XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
           sortie.output(rac, new FileOutputStream(s_display_conf));

   	}
   	catch(JDOMException e){
   		System.out.println(e);
   	}
   	catch(IOException e){
   		System.out.println(e);
   	}
   	return Rule_name;

   }
    
    public static void RecupererMesureDisplay(String s_display_conf, List<Measure_Display> liste_mm) {
    	SAXBuilder sxb = new SAXBuilder();
    	Document doc1;
    	try
    	{
    		//On crée un nouveau document JDOM avec en argument le fichier XML
    		doc1 = sxb.build(new File(s_display_conf));
    		Element rac = doc1.getRootElement();
    		
    		Element col_Background = rac.getChild("Background");
    		color_Background =col_Background.getChildText("color");

			//System.out.println("color_Background = "+ color_Background);

    		Element map_active = rac.getChild("MAP_Active");

    		List ListeMeasure = map_active.getChildren("Measure");

    		Iterator it = ListeMeasure.iterator();

    		String nom;
    		String dis_type;
    		Measure_Display mm = new Measure_Display();
    		while(it.hasNext())
    		{
    			Element courant = (Element)it.next();
    			nom = courant.getChildText("name") ;
    			dis_type = courant.getChildText("display");
    			mm = new Measure_Display(nom);
    			mm.setDisplay(dis_type);
    			String color = courant.getChildText("color");
				mm.setColor(color);
    			if (dis_type.toUpperCase().equals(("Bars").toUpperCase()))
    			{
    				double bar_width = Double.parseDouble(courant.getChildText("bar_width"));
    				mm.setBar_width(bar_width);
    				double bar_min_high = Double.parseDouble(courant.getChildText("bar_min_high"));
    				mm.setSize_min(bar_min_high);
    				double bar_max_high = Double.parseDouble(courant.getChildText("bar_max_high"));
    				mm.setSize_max(bar_max_high);
    			}
    			else if(dis_type.toUpperCase().equals(("Circles").toUpperCase())){
    				double size_min = Double.parseDouble(courant.getChildText("size_min"));
    				mm.setSize_min(size_min);
    				double size_max = Double.parseDouble(courant.getChildText("size_max"));
    				mm.setSize_max(size_max);
    				int count_levels = Integer.parseInt(courant.getChildText("count_levels"));
    				mm.setCount_levels(count_levels);
    			}
    			else if((dis_type.toUpperCase().equals(("Cloropeth").toUpperCase()))||(dis_type.toUpperCase().equals(("MultiCloropeth").toUpperCase()))){
    				int count_levels = Integer.parseInt(courant.getChildText("count_levels"));
    				mm.setCount_levels(count_levels);
    			}
    			

    			liste_mm.add(mm);
    		}	
    	}
    	catch(JDOMException e){
    		System.out.println(e);
    	}
    	catch(IOException e){
    		System.out.println(e);
    	}

    }

/*	static void CountMemberCube(String CubeLog)
	// Cette procedure est utilisée pour compter le nombre de membres de chaque dimanension [nDi & nb]
	// a fin de verifier dans la suite s'il y a une règle d'affichage qui correspond au résultat de l'analyse courant
    {
		//NB_dimensions = new HashMap<String, Double>();
		dim_member_count = new HashMap<String, Double>();
		SAXBuilder sxb = new SAXBuilder();
		List<String> list_dim_done = new ArrayList<String>();
		List<String> list_dim_mem_done = new ArrayList<String>();

		 try
	      {
	         //On crée un nouveau document JDOM avec en argument le fichier XML
			 Document document = sxb.build(new File(CubeLog));
	         //On initialise un nouvel élément racine avec l'élément racine du document.
			 Element racine = document.getRootElement();
	         
	         //On crée une List contenant tous les noeuds "Cube" fils de l'Element racine
	         List ListeCube = racine.getChildren("Cube");
	         
	         
	         //On crée un Iterator sur notre liste
	         Iterator i = ListeCube.iterator();
	         while(i.hasNext())
	         { // Pour toutes les balises "Cube" listées :
	             Element courant = (Element)i.next();
	             	             
	             {// Traitement des axes
	                 // On crée une liste des balises "Axis" fils de la balise courante
	                 List listeAxes = courant.getChildren("Axis");
	                 Iterator i2 = listeAxes.iterator(); // On crée un itérateur sur notre liste
	                 while (i2.hasNext())
	                 {// Pour toutes les balises "Axis" listées :
	                     Element cour = (Element)i2.next(); 
	                     
	                     if ("Axis0".equals(cour.getAttributeValue("name")))
	                     {// Si la balise a  pour attribut "name" la valeur "Axis0"

	                         // On récuprèe la liste des balises "Tuple" fils de la balise courante
	                         List listeTuple = cour.getChildren("Tuple");
	                         Iterator i3 = listeTuple.iterator(); // On crée un itérateur sur notre liste
	                         while(i3.hasNext())
	                         {// Pour toutes les balises "Tuple" listées :
	                             Element curent = (Element)i3.next(); 
	                             
	                             // On récupère la valeur de l'attribut "Dimension" de la balise courante
	                             String dim = curent.getAttributeValue("Dimension") ;
	                             
	                             int x=dim.indexOf(".");
	                             if (x!=-1)
	                            	 dim =(dim.subSequence(0, x)).toString();				                    		

	                             if ( ! list_dim_done.contains(dim))
	                             {
	                            	 dim_member_count.put(dim, 1.0);
	                            	 //System.out.println(dim);
	                            	 list_dim_done.add(dim);
	                            	 
	                            	 //on ajoute le member traité pour ne pas le compter plusieurs fois
	                            	 list_dim_mem_done.add(curent.getText());
	                             }
	                             else
	                             {
	                            	 // si le memebre n'est pas déjà compté
	                            	 if ( ! list_dim_mem_done.contains(curent.getText()))
	                            	 {
	                            		 dim_member_count.put(dim, dim_member_count.get(dim)+1);
	                            		//on ajoute le member traité pour ne pas le compter plusieurs fois
	                            		 list_dim_mem_done.add(curent.getText());
	                            	 }	                            		 
	                             }
	                             //System.out.println(dim +":"+ dim_member_count.get(dim));
	                             //System.out.println(curent.getText());
	                         }
	                         
	                     }
	                     else if ("Axis1".equals(cour.getAttributeValue("name")))
	                     {// Si la balise à  pour attribut "name" la valeur "Axis0"
	                       
	                         // On récupère la liste des balises "Tuple" fils de la balise courante
	                         List listeTuple = cour.getChildren("Tuple");
	                         Iterator i3 = listeTuple.iterator(); // On crée un itérateur sur notre liste
	                         while(i3.hasNext())
	                         {// Pour toutes les balises "Tuple" listées :
	                             Element curent = (Element)i3.next(); 
	                             
	                             // On réupère la valeur de l'attribut "Dimension" de la balise courante
	                             // On récupère la valeur de l'attribut "Dimension" de la balise courante
	                             String dim = curent.getAttributeValue("Dimension") ; 
	                             int x=dim.indexOf(".");
	                             if (x!=-1)
	                            	 dim =(dim.subSequence(0, x)).toString();				                    		

	                             if ( ! list_dim_done.contains(dim))
	                             {
	                            	 dim_member_count.put(dim, 1.0);
	                            	 //System.out.println(dim);
	                            	 list_dim_done.add(dim);
	                            	 
	                            	 //on ajoute le member traité pour ne pas le compter plusieurs fois
	                            	 list_dim_mem_done.add(curent.getText());
	                             }
	                             else
	                             {
	                            	 // si le memebre n'est pas déjà compté
	                            	 if ( ! list_dim_mem_done.contains(curent.getText()))
	                            	 {
	                            		 dim_member_count.put(dim, dim_member_count.get(dim)+1);
	                            		//on ajoute le member traité pour ne pas le compter plusieurs fois
	                            		 list_dim_mem_done.add(curent.getText());
	                            	 }	                            		 
	                             }
	                             //System.out.println(dim +":"+ dim_member_count.get(dim));
	                             //System.out.println(curent.getText());
	                         }
	                     }
	                 }
	             }	        	 
	         }
	         
	         boolean measureFound = false;
	         NB_total=1;
	         int Nb_Di=1;
	         for (String  dime : list_dim_done) {
	        	 if (dime.toUpperCase().equals(("Measures").toUpperCase()))
	        		 measureFound = true;
	        	 if (!measureFound)
	        		 mesure_position = mesure_position + dime.length() + CreateIndicators.separator.length();
				
	        	 System.out.println(dime + " " + dim_member_count.get(dime));
	        	 
	        	 Nb_Di= dim_member_count.get(dime).intValue();
	        	 NB_total= NB_total * Nb_Di;
	         }	         
	         
	         // Comme il faut pas prendre en compte la dimension spatiale pour définir le nombre des membres à afficher 
	         // et en considerant que la dernière dimension est la dimension spatiale,
	         // on divise NB_total par le dernier Nb_Di (autrement dit le Nb_Dspatiale)	         
	         NB_total = NB_total / Nb_Di;
	         
	         System.out.println("NB_total : " + NB_total);
	         //System.out.println("mesure_position : " + mesure_position);
	      }
	      catch(JDOMException e){
	          System.out.println(e);
	      }
	      catch(IOException e){
	        System.out.println(e);
	      }
    }
*/	
}