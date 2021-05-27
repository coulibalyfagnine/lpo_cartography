package Main_App;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import DataObjects.Geometrie;
import DataObjects.Point;

public class Donnee_geo {

	 public static Geometrie get_donneesGeo (String table, List list_Level, String nomZone, Connection connexion) {
		try {
			Geometrie geom = new Geometrie() ; 
			Iterator i = list_Level.iterator();
			while(i.hasNext())
			{
			Element courant = (Element)i.next();
			String niveau = courant.getChildText("N");
			String Ngeom = courant.getChildText("GEOM");
			String Ngeomcentroid = courant.getChildText("CENT");
			String Nniveau = courant.getChildText("NIV");
			Statement state = connexion.createStatement();
//	        System.out.println(" nom spatial : "+ nomZone );
			ResultSet result = state.executeQuery("SELECT DISTINCT \""+niveau+"\", \""+Ngeom+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, "+ 
												  "ST_ASText(\""+Ngeom+"\") AS GEOM__, ST_ASText(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, "+ 
												  Nniveau+ " AS NIVEAUSPATIAL__ FROM \""+table+"\" WHERE \""+niveau+"\" = \'"+nomZone+"\'");
			
//			System.out.println(result.getString("NIVEAUSPATIAL__") + " <<-");
			if(result.next()) {
				geom.setType(result.getString("GeometryType__"));
				geom.setNom(result.getString(niveau));

				String resultat = result.getString("GEOM__"); 
				String resultatcentroid = result.getString("GEOMCENTROID__");
//				System.out.println(resultatcentroid.toString() + " <> ");
				
				String resultatniveau = result.getString("NIVEAUSPATIAL__");

				if("POLYGON".equals(geom.getType())) {
					geom.setListePolygon(obtenirPolygons(resultat));
					geom.setCentroid(ObtenirPoint(resultatcentroid));
					geom.setNiveau(resultatniveau);
				} else if ("MULTIPOLYGON".equals(geom.getType())){
					geom.setListePolygon(obetenirMultiPolygons(resultat));
					geom.setCentroid(ObtenirPoint(resultatcentroid));
					geom.setNiveau(resultatniveau);
				}
				else {
					geom.setListePoint(ObtenirPoints(resultat));
					geom.setNiveau(resultatniveau);
				}
				break;
			}
			}
			return geom ;
		}
			catch (Exception e) {
			System.out.println(e);
			return null ; 
			}
	}

	public static void get_donneesGeo_liste (String table, List list_Level, String nomListeZone, Connection connexion, HashMap<String, Geometrie> geometry) {
		if (nomListeZone != null) {
			try {
				Iterator i = list_Level.iterator();
				while(i.hasNext())
				{
					Element courant = (Element)i.next();
					String niveau = courant.getChildText("N");
					String Ngeom = courant.getChildText("GEOM");
					String Ngeomcentroid = courant.getChildText("CENT");			
					Statement state = connexion.createStatement();

					
//					System.out.println(" nom liste zone " + nomListeZone);
					
//					System.out.println("SELECT \""+niveau+"\", \""+Ngeom+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_ASText(\""+Ngeom+"\") AS GEOM__, ST_ASText(\""+Ngeomcentroid+"\") AS GEOMCENTROID__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					ResultSet result = state.executeQuery("SELECT \""+niveau+"\", \""+Ngeom+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_ASText(\""+Ngeom+"\") AS GEOM__, ST_ASText(\""+Ngeomcentroid+"\") AS GEOMCENTROID__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");

					while(result.next()) {
						Geometrie geom = new Geometrie() ; 
						
						geom.setType(result.getString("GeometryType__"));
						geom.setNom(result.getString(niveau));

						String resultat = result.getString("GEOM__"); 
						//System.out.println("resultat ::"+resultat);
						String resultatcentroid = result.getString("GEOMCENTROID__");

						if("POLYGON".equals(geom.getType())) {

							geom.setListePolygon(obtenirPolygons(resultat));
							geom.setCentroid(ObtenirPoint(resultatcentroid));
						} else if ("MULTIPOLYGON".equals(geom.getType())){

							geom.setListePolygon(obetenirMultiPolygons(resultat));

							geom.setCentroid(ObtenirPoint(resultatcentroid));
						}
						else {
							geom.setListePoint(ObtenirPoints(resultat));
						}
						geometry.put(result.getString(niveau), geom);
						//System.out.println("geom ::"+geom);
						//System.out.println("geometry ::"+geometry);
						//break;

					}
				}
			}
		catch (Exception e) {
			System.out.println(e);
		}
		}
	}

	public static void get_donneesGeo_liste_JSON (String table, List list_Level, String nomListeZone, Connection connexion, HashMap<String, Geometrie> geometry, int count) {
		System.out.println(" NOM des niveaux spatiales ");
		if (nomListeZone != null) {
			try {
				Iterator i = list_Level.iterator();
				int a = 0;
				while(i.hasNext())
				{
					Element courant = (Element)i.next();
					String niveau = courant.getChildText("N");
					String Ngeom = courant.getChildText("GEOM");
					String Ngeomcentroid = courant.getChildText("CENT");
					String Nniveau = courant.getChildText("NIV");
					String Ndiff = courant.getChildText("DIFF");
					Statement state = connexion.createStatement();

//					System.out.println("SELECT \""+niveau+"\", \""+Ngeom+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_ASText(\""+Ngeom+"\") AS GEOM__, ST_ASText(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, "+ Nniveau+ " AS NIVEAUSPATIAL__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");

//					System.out.println("SELECT \""+niveau+"\", \""+Ngeom+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, GeometryType(\""+Ndiff+"\") AS CollectType__, "+ Ndiff+ " AS COLLECTPOINT__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");

					
//					ResultSet result = state.executeQuery("SELECT \""+niveau+"\", \""+Ngeom+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");

//					ResultSet result = state.executeQuery("SELECT \""+niveau+"\", \""+Ngeom+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, "+ Nniveau+ " AS NIVEAUSPATIAL__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					
//					System.out.println(" nom liste => " + nomListeZone);
					
					ResultSet resultcount = state.executeQuery("SELECT count(distinct "+ Nniveau +") as countNiveau FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					
					String countNiv = "";
					while(resultcount.next()) {
						countNiv = resultcount.getString("countNiveau");
					}
					
					/* Vérifier le nombre de niveau spatial dans la requête */ 
					count = Integer.parseInt(countNiv);
					
					
					
//					System.out.println(" nb niveau spatial =  " + countNiv);
					
					ResultSet result = state.executeQuery("SELECT \""+niveau+"\", \""+Ngeom+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, "+ Nniveau+ " AS NIVEAUSPATIAL__, GeometryType(\""+Ndiff+"\") AS DiffType__, ST_AsGeoJSON(\""+ Ndiff+"\") AS DIFF__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					
//					ResultSet result = state.executeQuery("SELECT \""+niveau+"\", \""+Ngeom+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, GeometryType(\""+Ndiff+"\") AS DiffType__, ST_AsGeoJSON(\""+ Ndiff+"\") AS DIFF__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					while(result.next()) {
						Geometrie geom = new Geometrie() ; 
						geom.setCount(count);
//						System.out.println(" debut get geom type <-");
						geom.setType(result.getString("GeometryType__"));

//						System.out.println(" debut get collect type <-");
						geom.setTypeCollect("DiffType__");
						

//						System.out.println(" debut get niveau <-");
						geom.setNom(result.getString(niveau));
						geom.setNiveau(result.getString("NIVEAUSPATIAL__"));
//						
////						System.out.println(" niveau " + result.getString("NIVEAUSPATIAL__") );

//						System.out.println(" debut get geom geom resultat <-");
						String resultat = result.getString("GEOM__"); 
//						//System.out.println("resultat ::"+resultat);

//						System.out.println(" debut get geom geom centroid <-");
						String resultatcentroid = result.getString("GEOMCENTROID__");
						
//						System.out.println(" debut get geom geom diff <-");
						String resultatcollection = result.getString("DIFF__");
						
						
						//System.out.println("resultat ::"+resultatcentroid + " ||| niveau :" + result.getString(niveau) + " && " + result.getString("NIVEAUSPATIAL__") + " *** " + a++);

//						System.out.println("resultat ::"+resultatcentroid + " ||| niveau :" + result.getString(niveau) + " &&  *** " + a++);
						
						//Ajouter les données geoJSON
						JSONParser jsonParser = new JSONParser();
//						System.out.println(" début jsonParser geojson ");
//						System.out.println(" resultat " + resultat);
						

//						System.out.println(" debut get geom geom resultat geojson <-");
						geom.setGeoJson((JSONObject) jsonParser.parse(resultat));

//						System.out.println(" debut get geom geom resultat geojsondiff <-");
//						System.out.println(" ----------------- ");
//						System.out.println((JSONObject) jsonParser.parse(resultatcollection));
						geom.setGeoJsonCollect((JSONObject) jsonParser.parse(resultatcollection)); 

						
//						System.out.println(" value centroid " + resultatcentroid);
						
						/* DEBUT CREATION DESSIN SUR LA CARTE */ 
						String[] val1 = resultatcentroid.split(":");
//						System.out.println(val1[0] + " --- " + val1[2]);
//						String[] val2 = val1[1].split(":");
//						System.out.println(val2[0] + " --- " + val2[1]);
						String[] val3 = val1[2].split(",");
//						System.out.println(val3[0] + " --- " + val3[1]);
						Float lg = Float.parseFloat(val3[0].replace("[", ""));
						Float lt = Float.parseFloat(val3[1].replace("]", "").replace("}", ""));
						
						
						Float pas = (float) 0.1;
						Float f1_lg = (float) (lg - pas);
						Float f1_lt = (float) (lt + pas);
						
						String c1 = "["+f1_lg.toString()+","+f1_lt.toString()+"]";
						
						
						Float f2_lg = (float) (lg + pas);
						Float f2_lt = (float) (lt + pas);
						String c2 = "["+f2_lg.toString()+","+f2_lt.toString()+"]";
						
						Float f3_lg = (float) (lg + pas);
						Float f3_lt = (float) (lt - pas);
						String c3 = "["+f3_lg.toString()+","+f3_lt.toString()+"]";
						
						Float f4_lg = (float) (lg - pas);
						Float f4_lt = (float) (lt - pas);
						String c4 = "["+f4_lg.toString()+","+f4_lt.toString()+"]";
						
						Float f5_lg = f1_lg;
						Float f5_lt = f1_lt;
						String c5 = "["+f5_lg.toString()+" "+f5_lt.toString()+"]";
						
						String coord = "[["+c1+","+c2+","+c3+","+c4+"]]";
						String carre = "{\"type\":\"Polygon\",\"coordinates\":"+coord+"}";
						String carre2 = "{\"type\":\"Polygon\",\"coordinates\":[1.48741970559244,43.7685455116662]}";
						
//						System.out.println(coord);
//						
//						System.out.println(" value 1 teste " + (JSONObject) jsonParser.parse(carre)) ;
//						System.out.println(" début jsonParser centroid " + (JSONObject) jsonParser.parse(resultatcentroid));
//						System.out.println("\n");
						geom.setCentroidJson((JSONObject) jsonParser.parse(resultatcentroid));
												
						geom.setCarre((JSONObject) jsonParser.parse(carre));
						/* FIN CREATION DESSIN SUR LA CARTE */ 
						
						
						//geom.setCentroid(ObtenirPoint(resultatcentroid));

/*						if("POLYGON".equals(geom.getType())) {

							geom.setListePolygon(obtenirPolygons(resultat));
							geom.setCentroid(ObtenirPoint(resultatcentroid));
						} else if ("MULTIPOLYGON".equals(geom.getType())){

							geom.setListePolygon(obetenirMultiPolygons(resultat));

							geom.setCentroid(ObtenirPoint(resultatcentroid));
						}
						else {
							geom.setListePoint(ObtenirPoints(resultat));
						}
*/						
						geometry.put(result.getString(niveau), geom);
					}
				}
			}
		catch (Exception e) {
			System.out.println(e);
			System.out.println(" brrrrr ");
		}
		}
	}

	public static int get_nbElement(String table, List list_Level, String nomListeZone, Connection connexion) {
		
		int ctElem = 0;
//		System.out.println(" NOMBRE D'ELEMENT SPATIAL ");
		if (nomListeZone != null) {
			try {
				Iterator i = list_Level.iterator();
				int a = 0;
				while(i.hasNext())
				{
					Element courant = (Element)i.next();
					String niveau = courant.getChildText("N");
					String Nniveau = courant.getChildText("NIV");
					String Ngeom = courant.getChildText("GEOM");
					String Ngeomcentroid = courant.getChildText("CENT");
					String Nard = courant.getChildText("ARD");
					String Nar = courant.getChildText("AR");
					String Nad = courant.getChildText("AD");
					String Nrd = courant.getChildText("RD");
					
					Statement state = connexion.createStatement();

					
//					System.out.println(" nom liste => " + nomListeZone);
					
					// COMPTAGE DU NOMBRE DE NIVEAU DANS LA REQUETE
					ResultSet resultcount = state.executeQuery("SELECT count("+ Nniveau +") as countNiveau FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					
					String countNiv = "";
					while(resultcount.next()) {
						countNiv = resultcount.getString("countNiveau");
//						System.out.println(" NB NIVEAU " + countNiv);
					}
					
					// COMPTAGE DU NOMBRE D'ELEMENT DANS LA REQUETE
					ResultSet resultcountElement = state.executeQuery("SELECT count(distinct "+ niveau +") as countElement FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					
					String countElement = "";
					while(resultcountElement.next()) {
						countElement = resultcountElement.getString("countElement");
//						System.out.println(" NB ELEMENT " + countElement);
					}
					
					
					
					/* Vérifier le nombre d'element spatial dans la requête */ 
					ctElem = Integer.parseInt(countElement);
					
										
					

				}
			}
		catch (Exception e) {
			System.out.println(e);
			System.out.println(" brrrrr get element ");
		}
		}
		
		return ctElem; 
	}
	
	public static void get_niveau(String table, List list_Level, String nomListeZone, Connection connexion, ArrayList<String>list_niveau){
		
		if (nomListeZone != null) {
			try {
				Iterator i = list_Level.iterator();
				int a = 0;
				while(i.hasNext())
				{
					Element courant = (Element)i.next();
					String niveau = courant.getChildText("N");
					String Nniveau = courant.getChildText("NIV");
					
					
					Statement state = connexion.createStatement();
					
//					System.out.println(" nom liste => " + nomListeZone);
					
					ResultSet resultNiveau = state.executeQuery("SELECT distinct "+ Nniveau +" as TypeNiveau FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");

					while(resultNiveau.next()) {
						list_niveau.add(resultNiveau.getString("TypeNiveau"));	

					}
					

				}
			}
		catch (Exception e) {
			System.out.println(e);
			System.out.println(" brrrrr erreur ici liste geojson ");
		}
		}
		
		
//		return list_niveau;
	}
	
	public static void get_donneesGeo_liste_JSON2 (String table, List list_Level, String nomListeZone, Connection connexion, HashMap<String, Geometrie> geometry, int count, int countElem) {
//		System.out.println(" NOM des niveaux spatiales ");
		if (nomListeZone != null) {
			try {
				Iterator i = list_Level.iterator();
				int a = 0;
				while(i.hasNext())
				{
					Element courant = (Element)i.next();
					String niveau = courant.getChildText("N");
					String Nniveau = courant.getChildText("NIV");
					String Ngeom = courant.getChildText("GEOM");
					String Ngeomcentroid = courant.getChildText("CENT");
					String Nard = courant.getChildText("ARD");
					String Nar = courant.getChildText("AR");
					String Nad = courant.getChildText("AD");
					String Nrd = courant.getChildText("RD");
					
					Statement state = connexion.createStatement();

					
//					System.out.println(" nom liste => " + nomListeZone);

					// COMPTAGE DU NOMBRE DE NIVEAU DANS LA REQUETE
					ResultSet resultcount = state.executeQuery("SELECT count(distinct "+ Nniveau +") as countNiveau FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");

					String countNiv = "";
					while(resultcount.next()) {
						countNiv = resultcount.getString("countNiveau");
//						System.out.println(" NB NIVEAU " + countNiv);
					}

					// COMPTAGE DU NOMBRE D'ELEMENT DANS LA REQUETE
					ResultSet resultcountElement = state.executeQuery("SELECT count(*) as countElement FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");

					String countElement = "";
					while(resultcountElement.next()) {
						countElement = resultcountElement.getString("countElement");
//						System.out.println(" NB ELEMENT " + countElement);
					}

					
					ResultSet resultNiveau = state.executeQuery("SELECT distinct "+ Nniveau +" as TypeNiveau FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					

					String[] sp = {"","",""};
					while(resultNiveau.next()) {
						System.out.println(" TYPE NIVEAU " + resultNiveau.getString("TypeNiveau"));
						if (resultNiveau.getString("TypeNiveau").equals("All")) {
							sp[0] = "all";
						}
						if (resultNiveau.getString("TypeNiveau").equals("region")) {
							sp[1] = "region";
						}
						if (resultNiveau.getString("TypeNiveau").equals("departement")) {
							sp[2] = "departement";
						}
					
					}
//					System.out.println(sp);
					
					String geom_col = "";
					int ct = 0;
					for (int j = 0; j < sp.length; j++) {
						if (j == 0) {
							if (sp[j].length() > 1){
								geom_col = sp[j];
								System.out.println(" colonne utilisé1 " + geom_col);
								ct = 0;
							}
							else {
								ct = 1;
							}
						}
						else if (j == sp.length -1 ) {
							if (sp[j].length() > 1) {
								geom_col = geom_col + "_" + sp[j];
								System.out.println(" colonne utilisé2 " + geom_col);
							}
						}
						else {
							if (sp[j].length() > 1) {
								if (ct == 1) {
									geom_col = sp[j];
									System.out.println(" colonne utilisé3 " + geom_col);
								}
								else {
									geom_col = geom_col + "_" + sp[j];
									System.out.println(" colonne utilisé4 " + geom_col);
								}	
							}
						}
						
					}
//					System.out.println(" colonne utilisé " + geom_col);
					

					/* Vérifier le nombre de niveau spatial dans la requête */ 
					count = Integer.parseInt(countNiv);
					/* Vérifier le nombre d'element spatial dans la requête */ 
					countElem = Integer.parseInt(countElement);
					ResultSet result = null;
					if (count == 1) {
						System.out.println("bo3o : " + nomListeZone);

						result = state.executeQuery("SELECT \""+niveau+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, "+ Nniveau+ " AS NIVEAUSPATIAL__, GeometryType(\""+Ngeom+"\") AS DiffType__, ST_AsGeoJSON(\""+ Ngeom+"\") AS DIFF__, ST_ASText(\""+Ngeomcentroid+"\") AS GEOMCENTROIDTEXT__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					}
					else {
						System.out.println("bo3o1 : " + geom_col);

						result = state.executeQuery("SELECT \""+niveau+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, "+ Nniveau+ " AS NIVEAUSPATIAL__, GeometryType(\""+geom_col+"\") AS DiffType__, ST_AsGeoJSON(\""+ geom_col+"\") AS DIFF__, ST_ASText(\""+Ngeomcentroid+"\") AS GEOMCENTROIDTEXT__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");						
					}
					
					while(result.next()) {
						Geometrie geom = new Geometrie() ; 
						geom.setCount(count);
						geom.setCountElem(countElem);

						geom.setType(result.getString("GeometryType__"));

//						System.out.println(" debut get collect type <-");
						geom.setTypeCollect("DiffType__");
						

						geom.setNom(result.getString(niveau));
						geom.setNiveau(result.getString("NIVEAUSPATIAL__"));

						String resultat = result.getString("GEOM__"); 

						String resultatcentroid = result.getString("GEOMCENTROID__");
//						System.out.println(" centroid " + resultatcentroid);
						String resultatcentroidtext = result.getString("GEOMCENTROIDTEXT__");
//						System.out.println(" centroid texte " + resultatcentroidtext);
						
						String resultatcollection = result.getString("DIFF__");
						
						//Ajouter les données geoJSON
				
						JSONParser jsonParser = new JSONParser();

						geom.setGeoJson((JSONObject) jsonParser.parse(resultat));

						geom.setGeoJsonCollect((JSONObject) jsonParser.parse(resultatcollection)); 
						
						/* DEBUT CREATION DESSIN SUR LA CARTE */ 
						String[] val1 = resultatcentroid.split(":");

						String[] val3 = val1[2].split(",");

						Float lg = Float.parseFloat(val3[0].replace("[", ""));
						Float lt = Float.parseFloat(val3[1].replace("]", "").replace("}", ""));
						
						
						Float pas = (float) 0.1;
						Float f1_lg = (float) (lg - pas);
						Float f1_lt = (float) (lt + pas);
						
						String c1 = "["+f1_lg.toString()+","+f1_lt.toString()+"]";
						
						
						Float f2_lg = (float) (lg + pas);
						Float f2_lt = (float) (lt + pas);
						String c2 = "["+f2_lg.toString()+","+f2_lt.toString()+"]";
						
						Float f3_lg = (float) (lg + pas);
						Float f3_lt = (float) (lt - pas);
						String c3 = "["+f3_lg.toString()+","+f3_lt.toString()+"]";
						
						Float f4_lg = (float) (lg - pas);
						Float f4_lt = (float) (lt - pas);
						String c4 = "["+f4_lg.toString()+","+f4_lt.toString()+"]";
						
						Float f5_lg = f1_lg;
						Float f5_lt = f1_lt;
						String c5 = "["+f5_lg.toString()+" "+f5_lt.toString()+"]";
						
						String coord = "[["+c1+","+c2+","+c3+","+c4+"]]";
						String carre = "{\"type\":\"Polygon\",\"coordinates\":"+coord+"}";
						String carre2 = "{\"type\":\"Polygon\",\"coordinates\":[1.48741970559244,43.7685455116662]}";
						

						geom.setCentroidJson((JSONObject) jsonParser.parse(resultatcentroid));
						geom.setCentroid(ObtenirPoint(resultatcentroidtext));
												
						geom.setCarre((JSONObject) jsonParser.parse(carre));
						/* FIN CREATION DESSIN SUR LA CARTE */ 
						
						geometry.put(result.getString(niveau), geom);
					}
					
					
					

				}
			}
		catch (Exception e) {
			System.out.println(e);
			System.out.println(" brrrrr erreur ici liste geojson ");
		}
		}
	}
	
	public static void get_donneesGeo_liste_JSON3 (String table, List list_Level, String nomListeZone, Connection connexion, HashMap<String, Geometrie> geometry, int count, int countElem) {
//		System.out.println(" NOM des niveaux spatiales ");
		if (nomListeZone != null) {
			try {
				Iterator i = list_Level.iterator();
				int a = 0;
				while(i.hasNext())
				{
					Element courant = (Element)i.next();
					String niveau = courant.getChildText("N");
					String Nniveau = courant.getChildText("NIV");
					String Ngeom = courant.getChildText("GEOM");
					String Ngeomcentroid = courant.getChildText("CENT");
					String Nard = courant.getChildText("ARD");
					String Nar = courant.getChildText("AR");
					String Nad = courant.getChildText("AD");
					String Nrd = courant.getChildText("RD");
					String Nbrd = courant.getChildText("BRD");
					
					Statement state = connexion.createStatement();

					
//					System.out.println(" nom liste => " + nomListeZone);
					
					// COMPTAGE DU NOMBRE DE NIVEAU DANS LA REQUETE
					ResultSet resultcount = state.executeQuery("SELECT count(distinct "+ Nniveau +") as countNiveau FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					
					String countNiv = "";
					while(resultcount.next()) {
						countNiv = resultcount.getString("countNiveau");
//						System.out.println(" NB NIVEAU " + countNiv);
					}
					
					// COMPTAGE DU NOMBRE D'ELEMENT DANS LA REQUETE
					ResultSet resultcountElement = state.executeQuery("SELECT count(*) as countElement FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					
					String countElement = "";
					while(resultcountElement.next()) {
						countElement = resultcountElement.getString("countElement");
//						System.out.println(" NB ELEMENT " + countElement);
					}
					
					
					ResultSet resultNiveau = state.executeQuery("SELECT distinct "+ Nniveau +" as TypeNiveau FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					

					String[] sp = {"","",""};
					while(resultNiveau.next()) {
//						System.out.println(" TYPE NIVEAU " + resultNiveau.getString("TypeNiveau"));
						if (resultNiveau.getString("TypeNiveau").equals("All")) {
							sp[0] = "all";
						}
						if (resultNiveau.getString("TypeNiveau").equals("region")) {
							sp[1] = "region";
						}
						if (resultNiveau.getString("TypeNiveau").equals("departement")) {
							sp[2] = "departement";
						}
					}
//					System.out.println(sp);
					
					String geom_col = "";
					int ct = 0;
					for (int j = 0; j < sp.length; j++) {
						if (j == 0) {
							if (sp[j].length() > 1){
								geom_col = sp[j];
								ct = 0;
							}
							else {
								ct = 1;
							}
						}
						else if (j == sp.length -1 ) {
							if (sp[j].length() > 1) {
								geom_col = geom_col + "_" + sp[j];
							}
						}
						else {
							if (sp[j].length() > 1) {
								if (ct == 1) {
									geom_col = sp[j];
								}
								else {
									geom_col = geom_col + "_" + sp[j];
								}	
							}
						}
						
					}
					
					geom_col = Nbrd;
//					System.out.println(" colonne utilisé " + Nbrd);
					
					
					/* Vérifier le nombre de niveau spatial dans la requête */ 
					count = Integer.parseInt(countNiv);
					/* Vérifier le nombre d'element spatial dans la requête */ 
					countElem = Integer.parseInt(countElement);
					ResultSet result = null;
					if (count == 1) {
						result = state.executeQuery("SELECT \""+niveau+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, "+ Nniveau+ " AS NIVEAUSPATIAL__, GeometryType(\""+Ngeom+"\") AS DiffType__, ST_AsGeoJSON(\""+ Ngeom+"\") AS DIFF__, ST_ASText(\""+Ngeomcentroid+"\") AS GEOMCENTROIDTEXT__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					}
					else {
						result = state.executeQuery("SELECT \""+niveau+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, "+ Nniveau+ " AS NIVEAUSPATIAL__, GeometryType(\""+geom_col+"\") AS DiffType__, ST_AsGeoJSON(\""+ geom_col+"\") AS DIFF__, ST_ASText(\""+Ngeomcentroid+"\") AS GEOMCENTROIDTEXT__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");						
					}
					
					while(result.next()) {
						Geometrie geom = new Geometrie() ; 
						geom.setCount(count);
						geom.setCountElem(countElem);

						geom.setType(result.getString("GeometryType__"));

//						System.out.println(" debut get collect type <-");
						geom.setTypeCollect("DiffType__");
						

						geom.setNom(result.getString(niveau));
						geom.setNiveau(result.getString("NIVEAUSPATIAL__"));

						String resultat = result.getString("GEOM__"); 

						String resultatcentroid = result.getString("GEOMCENTROID__");
//						System.out.println(" centroid " + resultatcentroid);
						String resultatcentroidtext = result.getString("GEOMCENTROIDTEXT__");
//						System.out.println(" centroid texte " + resultatcentroidtext);
						
						String resultatcollection = result.getString("DIFF__");
						
						//Ajouter les données geoJSON
						JSONParser jsonParser = new JSONParser();

						geom.setGeoJson((JSONObject) jsonParser.parse(resultat));

						geom.setGeoJsonCollect((JSONObject) jsonParser.parse(resultatcollection)); 
						
						

						geom.setCentroidJson((JSONObject) jsonParser.parse(resultatcentroid));
						geom.setCentroid(ObtenirPoint(resultatcentroidtext));
												
						
						geometry.put(result.getString(niveau), geom);
					}
					
					
					

				}
			}
		catch (Exception e) {
			System.out.println(e);
			System.out.println(" brrrrr erreur ici liste geojson ");
		}
		}
	}
	
	
	public static void get_donneesGeo_Histogramme (String table, List list_Level, String nomListeZone, Connection connexion, HashMap<String, Geometrie> geometry, int count) {
		System.out.println(" NOM des niveaux spatiales ");
		if (nomListeZone != null) {
			try {
				Iterator i = list_Level.iterator();
				int a = 0;
				while(i.hasNext())
				{
					Element courant = (Element)i.next();
					String niveau = courant.getChildText("N");
					String Nniveau = courant.getChildText("NIV");
					String Ngeom = courant.getChildText("GEOM");
					String Ngeomcentroid = courant.getChildText("CENT");
					String Nard = courant.getChildText("ARD");
					String Nar = courant.getChildText("AR");
					String Nad = courant.getChildText("AD");
					String Nrd = courant.getChildText("RD");
					
					Statement state = connexion.createStatement();

					
//					System.out.println(" nom liste => " + nomListeZone);
					
					ResultSet resultcount = state.executeQuery("SELECT count(distinct "+ Nniveau +") as countNiveau FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					
					String countNiv = "";
					while(resultcount.next()) {
						countNiv = resultcount.getString("countNiveau");
						System.out.println(" NB NIVEAU " + countNiv);
					}
					
					
					ResultSet resultNiveau = state.executeQuery("SELECT distinct "+ Nniveau +" as TypeNiveau FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					

					String[] sp = {"","",""};
					while(resultNiveau.next()) {
						System.out.println(" TYPE NIVEAU " + resultNiveau.getString("TypeNiveau"));
						if (resultNiveau.getString("TypeNiveau").equals("All")) {
							sp[0] = "all";
						}
						if (resultNiveau.getString("TypeNiveau").equals("region")) {
							sp[1] = "region";
						}
						if (resultNiveau.getString("TypeNiveau").equals("departement")) {
							sp[2] = "departement";
						}
					}
					System.out.println(sp);
					
					String geom_col = "";
					int ct = 0;
					for (int j = 0; j < sp.length; j++) {
						if (j == 0) {
							if (sp[j].length() > 1){
								geom_col = sp[j];
								ct = 0;
							}
							else {
								ct = 1;
							}
						}
						else if (j == sp.length -1 ) {
							if (sp[j].length() > 1) {
								geom_col = geom_col + "_" + sp[j];
							}
						}
						else {
							if (sp[j].length() > 1) {
								if (ct == 1) {
									geom_col = sp[j];
								}
								else {
									geom_col = geom_col + "_" + sp[j];
								}	
							}
						}
						
					}
					System.out.println(" colonne utilisé " + geom_col);
					
					
					/* Vérifier le nombre de niveau spatial dans la requête */ 
					count = Integer.parseInt(countNiv);
					ResultSet result = null;
					if (count == 1) {
						result = state.executeQuery("SELECT \""+niveau+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, "+ Nniveau+ " AS NIVEAUSPATIAL__, GeometryType(\""+Ngeom+"\") AS DiffType__, ST_AsGeoJSON(\""+ Ngeom+"\") AS DIFF__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");
					}
					else {
						result = state.executeQuery("SELECT \""+niveau+"\", GeometryType(\""+Ngeom+"\") AS GeometryType__, ST_AsGeoJSON(\""+Ngeom+"\") AS GEOM__, ST_AsGeoJSON(\""+Ngeomcentroid+"\") AS GEOMCENTROID__, "+ Nniveau+ " AS NIVEAUSPATIAL__, GeometryType(\""+geom_col+"\") AS DiffType__, ST_AsGeoJSON(\""+ geom_col+"\") AS DIFF__ FROM \""+table+"\" WHERE \""+niveau+"\" in ("+nomListeZone+")");						
					}
					
					while(result.next()) {
						Geometrie geom = new Geometrie() ; 
						geom.setCount(count);

						geom.setType(result.getString("GeometryType__"));

//						System.out.println(" debut get collect type <-");
						geom.setTypeCollect("DiffType__");
						

						geom.setNom(result.getString(niveau));
						geom.setNiveau(result.getString("NIVEAUSPATIAL__"));

						String resultat = result.getString("GEOM__"); 

						String resultatcentroid = result.getString("GEOMCENTROID__");
						
						String resultatcollection = result.getString("DIFF__");
						
						//Ajouter les données geoJSON
						JSONParser jsonParser = new JSONParser();

						geom.setGeoJson((JSONObject) jsonParser.parse(resultat));

						geom.setGeoJsonCollect((JSONObject) jsonParser.parse(resultatcollection)); 
						
						/* DEBUT CREATION DESSIN SUR LA CARTE */ 
						String[] val1 = resultatcentroid.split(":");

						String[] val3 = val1[2].split(",");

						Float lg = Float.parseFloat(val3[0].replace("[", ""));
						Float lt = Float.parseFloat(val3[1].replace("]", "").replace("}", ""));
						
						
						Float pas = (float) 0.1;
						Float f1_lg = (float) (lg - pas);
						Float f1_lt = (float) (lt + pas);
						
						String c1 = "["+f1_lg.toString()+","+f1_lt.toString()+"]";
						
						
						Float f2_lg = (float) (lg + pas);
						Float f2_lt = (float) (lt + pas);
						String c2 = "["+f2_lg.toString()+","+f2_lt.toString()+"]";
						
						Float f3_lg = (float) (lg + pas);
						Float f3_lt = (float) (lt - pas);
						String c3 = "["+f3_lg.toString()+","+f3_lt.toString()+"]";
						
						Float f4_lg = (float) (lg - pas);
						Float f4_lt = (float) (lt - pas);
						String c4 = "["+f4_lg.toString()+","+f4_lt.toString()+"]";
						
						Float f5_lg = f1_lg;
						Float f5_lt = f1_lt;
						String c5 = "["+f5_lg.toString()+" "+f5_lt.toString()+"]";
						
						String coord = "[["+c1+","+c2+","+c3+","+c4+"]]";
						String carre = "{\"type\":\"Polygon\",\"coordinates\":"+coord+"}";
						String carre2 = "{\"type\":\"Polygon\",\"coordinates\":[1.48741970559244,43.7685455116662]}";
						

						geom.setCentroidJson((JSONObject) jsonParser.parse(resultatcentroid));
												
						geom.setCarre((JSONObject) jsonParser.parse(carre));
						/* FIN CREATION DESSIN SUR LA CARTE */ 
											
						geometry.put(result.getString(niveau), geom);
					}
					
					
					

				}
			}
		catch (Exception e) {
			System.out.println(e);
			System.out.println(" brrrrr ");
		}
		}
	}
	
	public static String get_Region(Connection connexion, Document documentConfig, String depart) throws SQLException {
		
		String region = "";
//		System.out.println(" RECUPERER NOM REGION ");
		
		Element rootgeo = documentConfig.getRootElement(); 
		String table = rootgeo.getChildText("Table");
		Element levels = rootgeo.getChild("Levels");
		List list_Level = levels.getChildren("Level");
		
		
		Iterator i = list_Level.iterator();
		int a = 0;
		while(i.hasNext())
		{
			Element courant = (Element)i.next();
			

			String TLOC = courant.getChildText("TLOC");
			String TDEP = courant.getChildText("TDEP");
			String TREG = courant.getChildText("TREG");
			
			Statement state = connexion.createStatement();
			ResultSet resultcountElement = state.executeQuery("SELECT distinct "+TREG+" as region FROM "+TLOC+" WHERE "+TDEP+" = '"+depart+"'");
			
	//		String countElement = "";
			while(resultcountElement.next()) {
				region = resultcountElement.getString("region");
	//			System.out.println(" REGION de " + depart + " est : " + region);
			}

		}
		
		
		
		
		
		return region; 
	}

	
	public static String get_RegionOld(Connection connexion, List list_Level, String depart) throws SQLException {
		
		String region = "";
//		System.out.println(" RECUPERER NOM REGION ");
		
		
		
		Statement state = connexion.createStatement();
		ResultSet resultcountElement = state.executeQuery("SELECT distinct region_name as region FROM parcelle WHERE dep_name = '"+depart+"'");
		
//		String countElement = "";
		while(resultcountElement.next()) {
			region = resultcountElement.getString("region");
//			System.out.println(" REGION de " + depart + " est : " + region);
		}
		
		
		
		return region; 
	}
	

	
	// ajout anael
	private static Point ObtenirPoint(String s1)    {
		Point point = new Point(2.0,2.0);

		if(s1 != null) {

			int open = s1.lastIndexOf('(');
			int close = s1.indexOf(')');

			//System.out.println(open + "  " + close);

			String sortie = (String) s1.subSequence(open+1, close);
			// ajout anael
			//System.out.println(sortie);



			String p[] = sortie.split(" ");
			double d1 = Double.parseDouble(p[0]) ; 
			double d2 = Double.parseDouble(p[1]) ; 

			point = new Point(d1, d2);

			point.Ecrire();
		}
		return point; 
	}

	private static List<Point> ObtenirPoints(String s1)    {
		List<Point> liste = new ArrayList<Point> () ;

		if(s1 != null) {

			int open = s1.lastIndexOf('(');
			int close = s1.indexOf(')');

			//System.out.println(open + "  " + close);

			String sortie = (String) s1.subSequence(open+1, close);
			// ajout anael
			//System.out.println(sortie);

			Point point ; 

			String l[] = sortie.split(",");

			for (String s : l)
			{
				String p[] = s.split(" ");
				double d1 = Double.parseDouble(p[0]) ; 
				double d2 = Double.parseDouble(p[1]) ; 

				point = new Point(d1, d2);

				point.Ecrire();
				liste.add(point);
			}
		}
		return liste ; 
	}

	// ajout anael
	private static List<List<Point> > obtenirPolygons(String s1) {
		List<List<Point> > liste = new ArrayList<List<Point> > () ;

		int open = s1.indexOf('(');
		int close = s1.lastIndexOf(')');

		//System.out.println(open + "  " + close);

		String sortie = (String) s1.subSequence(open+1, close);
		// ajout anael
		//System.out.println(sortie);

		//System.out.println(sortie.indexOf("),"));

		String l[] = sortie.split("[)]{1}[,]{1}");
		for (String s : l) {
			liste.add(ObtenirPoints(s.concat(")")));
			//System.out.println(s);
		}
		return liste ; 
	}

	private static List<List<Point> > obetenirMultiPolygons(String s1) {

		int open = s1.indexOf('(');
		int close = s1.lastIndexOf(')');

		//System.out.println(open + "  " + close);

		String sortie = (String) s1.subSequence(open+1, close);
		// ajout anael
		//System.out.println(sortie);

		//System.out.println(sortie.indexOf("),"));

		return obtenirPolygons(sortie); 
	}

	
	@SuppressWarnings("unused")
	public static JSONObject draw_circle (Connection connexion, String table, JSONObject centroidgeoJSON, String nomZone) throws ParseException, SQLException {
		
		JSONObject circle = new JSONObject();
		
		Statement state = connexion.createStatement();
		ResultSet resultCircle = state.executeQuery("SELECT ST_AsGeoJSON("
																		+ " ST_Buffer("
																		+ " ST_GeomFromGeoJSON('"+centroidgeoJSON+"')::geography"
																		+ "  1, 'quad_segs=16')::geometry) as GEOMCIRCLE_ ");
		
		/*SELECT ST_AsGeoJSON(ST_Buffer(
			 (select centroid
			from oab_geometry_multilayer
			where location_nom = 'ILE-DE-FRANCE'),
			 10, 'quad_segs=8')) as GEOMCIRCLE_; 
			 
			 ST_Transform(ST_Buffer(ST_Transform(geo:geometry, _ST_BestSRID(geo)), buffer_in_meters, 8), 4326)
			 
			 
			 SELECT 
  ST_Buffer(
    ST_GeomFromGeoJSON('{"type":"Point","coordinates":[11.26,44.42]}')::geography,
1000,'quad_segs=16')::geometry;
			 
			 */
		
		
//		String countElement = "";
		while(resultCircle.next()) {
			JSONParser jsonParser = new JSONParser();
			circle =  (JSONObject)jsonParser.parse(resultCircle.getString("GEOMCIRCLE_"));
//			System.out.println(" REGION de " + depart + " est : " + region);
		}
		
		
		
		
		
		return circle;
		
		
	}

}
