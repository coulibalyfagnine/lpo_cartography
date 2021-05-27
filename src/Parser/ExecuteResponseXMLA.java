package Parser;

import java.io.File;
import java.io.FileOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import DataObjects.CellValue;
import DataObjects.Cube;
import DataObjects.Hierarchy;
import DataObjects.ValueTable;

public class ExecuteResponseXMLA {

	public static Document getDocument(File f){
		SAXBuilder sxb = new SAXBuilder();
		Document document = null;
		try{
			 document = sxb.build(f);  // on cr�e un document JDOM  partir du fichier XML  parser
			 						  // le passage en argument  cette mthode on le fait par un constructeur de fichiers
			 						 // new File("chemin");
		}catch(Exception e){
			System.out.println(e.getMessage());
			return document;  //on cas d'erreur on retournera un document vide
		}
		return document;
	}
	
        // Appel : (contenu de XMLduTableau .xml);
        // Crée un cube à partir du fichier XMLduTableau.xml
	
	public static Cube findAxes(Document d) throws NoSuchAlgorithmException{
		

		LinkedList<Hierarchy> Filters = new LinkedList<Hierarchy>();
		LinkedList<Hierarchy> Dimensions = new LinkedList<Hierarchy>();
		Hierarchy temporary;
                
                
		//récupration de l'élment racine
		Element racine = d.getRootElement();
	    

		
		
		//accès aux fils de Axes
		//Element e = racine.getChild("Body",Namespace.getNamespace("SOAP-ENV", "http://schemas.xmlsoap.org/soap/envelope/"));
		//Element e = racine.getChild("ExecuteResponse",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis"));
        Element e = racine.getChild("return",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis"));
		//Element e = racine.getChild("cxmla:ExecuteResponse");
		//e = e.getChild("root",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));

     
        //System.out.println(e);
        e = e.getChild("root",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));
                /* On est désormais dans la balise <root> qui est dans le chemin de balise : 
                 * <SOAP-ENV:Body>
                 *   <cxmla:ExecuteResponse xmlns:cxmla="urn:schemas-microsoft-com:xml-analysis">
                 *      <cxmla:return>
                 *         ...
                 */
                
                
		//On récupere les dimensions en colonne du cube d'abord--------------------------------------------------------------------------------------------------
		Element faits = e.getChild("OlapInfo",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));
		faits = faits.getChild("AxesInfo",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));
		
                // On est dans les fils de la balise <AxesInfo> qui est fille de <OlapInfo>
                
                // On met dans la liste les balises filles de <AxesInfo>, 
                // à savoir : <AxisInfo name="???">
		List<Element> listAxeInfo=faits.getChildren();
		Iterator<Element> it0 = listAxeInfo.iterator();
		Element testAxisInfo;
		String SlicerAxis=new String("");
		

		
		while(it0.hasNext()){ // On parcours les �l�ments de la liste
			testAxisInfo=it0.next();
			if( testAxisInfo.getAttributeValue("name").compareTo("SlicerAxis") == 0 ){
				faits = testAxisInfo;
			} // Si on trouve une balise nom�e "SlicerAxis" on la consid�re comme la L'�l�ment faits
		}
		
		listAxeInfo = faits.getChildren();
		it0 = listAxeInfo.iterator();
		while(it0.hasNext()){ // Pour toutes les balises filles de l'élément faits
			faits=it0.next();
			SlicerAxis=SlicerAxis+faits.getAttributeValue("name")+" ";
		}// On rajoute l'attribut "name" de la balise dans la chaine SlicerAxis
		
		//Fin-------------------------------------------------------------------------------------------------------------------------------------------------------
		
		
		//Acquisition des lignes et colonnes------------------------------------------------------------------------------------------------------------------------
		// e est le bout du fichier contenu dans la balise <root> 
                
                e = e.getChild("Axes",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));
		
                // e est le bout du fichier contenu dans la balise <Axes>
                
		List<Element> listAxes = e.getChildren(); //ces variables sont des variables intermediaires qui servent à récuperer les différentes inofrmations sur les axes
		// On récupère ici la liste des balises filles de la balise <Axes>
        Iterator<Element> it = listAxes.iterator();
		List<Element> listTuple;
		List<Element> listMember;
		Iterator<Element> it1;
		Iterator<Element> it2;
		Element e2;
		Element e1;
		String axis;
		int tuple=0;
		String testColonne;
		String result = new String("");
		while(it.hasNext()){ // Pour toutes les balises filles : c-a-d les balises <Axis>
			e1=it.next();
			axis = e1.getAttributeValue("name");
			//Accès aux noms des dimensions prise en compte dans la requète
			e1=e1.getChild("Tuples",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));
			// On se place dans la balise <Tuples>
            listTuple = e1.getChildren(); // On prend la liste des balise filles de <Tuples> c-a-d les balises <Tuple>
			it1 = listTuple.iterator();
			while(it1.hasNext()){ // Pour chaque balise <Tuple>
				tuple++;
				e=it1.next();
				listMember = e.getChildren(); // On prend la liste des balises filles des filles de <Tuples> c-a-d les balises <Member>
				it2=listMember.iterator();
					while(it2.hasNext()){ // Pour chaque balise <Member> On cr�e une hierarchie
						e=it2.next();
						testColonne = e.getAttributeValue("Hierarchy"); // On récupère la valeur de l'argument Hierarchy
// Si la chaine de caractère SlicerAxis crée précédement contient la valeur de l'attribut Hierarchy de la balise :
                                                if(SlicerAxis.contains(testColonne)){
// On crée une hierarchy de type Filter
							testColonne = new String("</Filter>");
							temporary = new Hierarchy("Filter", e.getAttributeValue("Hierarchy"), null, null);
							result = result + "\t<Filter=\""+e.getAttributeValue("Hierarchy")+"\">";
						}else{
// On crée une hierarchy de type Dimension
							testColonne = new String("</Dimension>");
							temporary = new Hierarchy("Dimension", e.getAttributeValue("Hierarchy"), null, null);
							result = result + "\t<Dimension=\""+e.getAttributeValue("Hierarchy")+"\">";
						}
// Positionement sur la balise LName et récupération du texte sans les espaces extérieurs
						e2=e.getChild("LName",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));
						result = result + "" + e2.getTextTrim()+"";
						temporary.setLevel(e2.getTextTrim());
						
// Positionnement sur la balse Caption et récupération du texte sans les espaces extérieurs
						e2=e.getChild("Caption",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));
						result = result +"."+e2.getTextTrim()+ testColonne +"\n";  //Incstance de la dimension
						temporary.setCaption(e2.getTextTrim());
						temporary.setAxis(axis);
						temporary.setTuple(tuple);

// Si la hierarchie que l'on vient de créer est un filtre on l'ajoute aux filtres, sinon on l'ajoute aux dimensions.
						if(temporary.getType().contains("Filter") ){Filters.add(temporary);}
						else{Dimensions.add(temporary);}
						temporary = null;						
					}
				}tuple=0;
		}
// Une fois toutes les balises <Axis> traités : 
// On cré le cube avec les filtres et les dimensions trouvées. 
		Cube cube= new Cube(Filters,Dimensions);
		return cube;
	}

        // appel : (cube, CubeLog.xml) ; 
        /* Utilité de la fonction : 
         * Si le cube n'est pas déjà dans le fichier CubeLog.xml
         *      on le rajoute et on retourne false.
         */
	public static boolean generateCubeWIKI(Cube cube,File log, Document doc, boolean transposed) throws NoSuchAlgorithmException{
		
		//récupération de l'élément racine
		boolean found = false;
		Document d=getDocument(log);
		Element racine = d.getRootElement();
		racine.removeContent();
		System.out.println("On commence tout juste la fonction");
		
                // On récupère la liste des cubes présents dans le fichier CubeLog.xml
		LinkedList<Cube> WikiCubes = getWikiCubes(log);
		Iterator<Cube> cubesIt = WikiCubes.iterator();
		while(cubesIt.hasNext()){
			Cube c = cubesIt.next();
			if(cube.equality(c)){
                            found=true;
			}
		}
		System.out.println("On a fini la recherche du cube...");
		if(found==true){ // Si le cube sur lequel on travail est déjà dans le cube, on retourne l'adresse
			return true; //url to wiki
		}else{ // Sinon on rajoute le cube de travail dans le fichier CubeLog.xml
			Element c = new Element("Cube");
			Attribute id = new Attribute("id",cube.getId());
			c.setAttribute(id);
			Iterator<Hierarchy> its = cube.getFilterByFilter();
			Hierarchy result;
			Element dim,axis,tuple ;
            Element mesure, valeur;
			Attribute dimAtt;
			Attribute name;
            Attribute ax0, ax1;
			while(its.hasNext()){
				result = its.next();
				dim = new Element("Slicer");
				c.addContent(dim);
				name = new Attribute("name",result.getDimension());
				dim.setAttribute(name);
				dim.setText(result.getLevel()+"."+result.getCaption());
			}
                        
			Iterator<Hierarchy> it0 = cube.getAxis0().iterator();
			axis = new Element("Axis");
			c.addContent(axis);
			name = new Attribute("name","Axis0");
			axis.setAttribute(name);
			while(it0.hasNext()){
				result = it0.next();
				tuple = new Element("Tuple");
				name = new Attribute("number",Integer.toString(result.getTuple()));
				dimAtt = new Attribute("Dimension", result.getDimension());
				tuple.setAttribute(name);
				tuple.setAttribute(dimAtt);
				tuple.setText(result.getLevel()+"."+result.getCaption());
				axis.addContent(tuple);
			}
			
			
			Iterator<Hierarchy> it1 = cube.getAxis1().iterator();
			axis = new Element("Axis");
			c.addContent(axis);
			name = new Attribute("name","Axis1");
			axis.setAttribute(name);
			while(it1.hasNext()){
				result = it1.next();
				
				tuple = new Element("Tuple");
				name = new Attribute("number",Integer.toString(result.getTuple()));
				dimAtt = new Attribute("Dimension", result.getDimension());
				tuple.setAttribute(name);
				tuple.setAttribute(dimAtt);
				tuple.setText(result.getLevel()+"."+result.getCaption());
				axis.addContent(tuple);
			}
            
            ValueTable valueTab[] = cube.ValuesEtTuples(doc, false);
            
            int countprime = 0 ;
            int count = valueTab.length ; 
            
            while(countprime<count){
				Iterator<Hierarchy> axis0tuple = valueTab[countprime].getAxis0tuple().iterator();
				Iterator<Hierarchy> axis1tuple = valueTab[countprime].getAxis1tuple().iterator();
				CellValue cell = valueTab[countprime].getC();
				
                
//                if (!"n/o".equals(cell.getValue()))
//                {
                    mesure = new Element("Measure");
                    
                    ax0 = new Attribute("Ax0", Integer.toString(valueTab[countprime].getAxe0Val()));
                    ax1 = new Attribute("Ax1", Integer.toString(valueTab[countprime].getAxe1Val()));
                    mesure.setAttribute(ax0);
                    mesure.setAttribute(ax1);
                    
                    valeur = new Element("Value");
                    valeur.setText(cell.getValue());
                    mesure.addContent(valeur);
                    
                    c.addContent(mesure);
//                }
                
				countprime++;
			}
            
            
			
			racine.addContent(c);
			
			try
			{
			//On utilise ici un affichage classique avec getPrettyFormat()
			XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
			//Remarquez qu'il suffit simplement de créer une instance de FileOutputStream
			//avec en argument le nom du fichier pour effectuer la sérialisation.
			sortie.output(d, new FileOutputStream(log));
			}
			catch (java.io.IOException ex){
				System.out.println(ex.getMessage());
			}
			
			return false;
		}
	}
	
	
	public static LinkedList<Cube> getWikiCubes(File log) throws NoSuchAlgorithmException{
		LinkedList<Cube> resultList = new LinkedList<Cube>();
		//r�cup�ration de l'�l�ment racine
		Document d=getDocument(log);
		Element racine = d.getRootElement();
		List<Element> logCubes= racine.getChildren();
		
		Iterator<Element> cubesIt= logCubes.iterator();
		Iterator<Element> dimsIt;
		Element cube,dim,tuple;
		while(cubesIt.hasNext()){
			cube=cubesIt.next();
			List<Element> cubeDims = cube.getChildren();
			dimsIt = cubeDims.iterator();
			LinkedList<Hierarchy> Filters = new LinkedList<Hierarchy>();
			LinkedList<Hierarchy> Dimensions = new LinkedList<Hierarchy>();
			while(dimsIt.hasNext()){
				dim = dimsIt.next();
				if(!dim.getName().contains("Axis")){
					if(dim.getName()=="Slicer"){
						Hierarchy temp = new Hierarchy("Filter",dim.getAttributeValue("name"),dim.getTextTrim().substring(0, dim.getTextTrim().lastIndexOf(".")),dim.getTextTrim().substring(dim.getTextTrim().lastIndexOf(".")+1, dim.getTextTrim().length()));
						Filters.add(temp);
					}
				}else{
					Iterator<Element> tuples = dim.getChildren().iterator();
					while(tuples.hasNext()){
						tuple=tuples.next();
						Hierarchy temp = new Hierarchy("Dimension",tuple.getAttributeValue("Dimension"),tuple.getTextTrim().substring(0, tuple.getTextTrim().lastIndexOf(".")),tuple.getTextTrim().substring(tuple.getTextTrim().lastIndexOf(".")+1, tuple.getTextTrim().length()));
						temp.setAxis(dim.getAttributeValue("name"));
						temp.setTuple(Integer.parseInt(tuple.getAttributeValue("number")));
						Dimensions.add(temp);
						
					}
				}
			}
			Cube element = new Cube(Filters,Dimensions,cube.getAttributeValue("id"));
			resultList.add(element);			
		}
		
		return resultList;
	}
	
	public static CellValue[] getValues(Document d){
		LinkedList<CellValue> result = new LinkedList<CellValue>();
		Element racine = d.getRootElement();
	    
		//Element e = racine.getChild("Body",Namespace.getNamespace("SOAP-ENV", "http://schemas.xmlsoap.org/soap/envelope/"));
		//Element e = racine.getChild("ExecuteResponse",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis"));
        Element e = racine.getChild("return",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis"));
		//Element e = racine.getChild("cxmla:ExecuteResponse");
		//e = e.getChild("root",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));
		e = e.getChild("root",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));
		e = e.getChild("CellData",Namespace.getNamespace("urn:schemas-microsoft-com:xml-analysis:mddataset"));
		
		List<Element> cellList=e.getChildren();
		Iterator<Element> itCellList = cellList.iterator();
		List<Element> cellInfo;
		Iterator<Element> itCellInfo;
		Element cellElement;
		CellValue cell;
		int ordinal;
		String value = new String("");
		String s_value;
		while(itCellList.hasNext()){
			cellElement = itCellList.next();
			ordinal = Integer.parseInt(cellElement.getAttributeValue("CellOrdinal"));
			cellInfo = cellElement.getChildren();
			itCellInfo = cellInfo.iterator();
			cellElement = itCellInfo.next();
			cellElement = itCellInfo.next();
		//	System.out.println("hahozzwa: "+ cellElement.getName());

			s_value = new String(cellElement.getText());

			if(s_value.length()==0){value = new String("99999");}else{value = new String(s_value);}
			cell = new CellValue(ordinal,value);
//			System.out.println("+++++++++++++++++++++++="+cell.toString());
			result.add(cell);
			
		}
		int length = result.getLast().getOrdianal();
		CellValue c[] = new CellValue[length+1];
		CellValue tempCell = null;
		Iterator<CellValue> itCell = result.iterator();
		int count=0;
		while(count<=length){
			if(count==0){tempCell = itCell.next();}
//			if(itCell.hasNext()){tempCell = itCell.next();System.out.println("hah----"+tempCell.getOrdianal());}else{tempCell = new CellValue(-1, "n/o");}
			if(count==tempCell.getOrdianal()){
				c[count]=tempCell;
				if(itCell.hasNext()){tempCell = itCell.next();}
				count++;
			}else{
				c[count] = new CellValue(count,"99999");
				count++;
			}
//			System.out.println(c[count-1]);
		}
		return c;
		
	}
}
