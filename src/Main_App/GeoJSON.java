/**
 * 
 */
package Main_App;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jdom2.Document;
import org.jdom2.Element;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import DataObjects.Geometrie;
import DataObjects.Indicateur;
import useful_Document.TEXT;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author hassan
 *
 */
public class GeoJSON {
	static int count;
	static int countElem;

	static Double shifting = new Double(0.0);
	static Double shh = new Double(0.0);

	static String[][] geo_ind = null;
	static List<String> label_ok = null;

	static Map<String, Double> max_ind_Values;
	static Map<String, Double> min_ind_Values;
	static Map<String, Double> count_mes_ind; // pour compter le nombre dâ€™indicateurs pour chaque mesure Ã  afficher
												// dans chaque zone spatiale
	static Map<String, Integer> count_map_by_type; // pour compter le nombre de map pour chaque type d'affichage
													// (cloropeth, circle, bars)

	static Map<String, Double> max_mes_Values;
	static Map<String, Double> min_mes_Values;

	static Map<String, Double> sum_mes_sp_Values;
	static Map<String, Double> sum_mes_sp_Values_curr;

	static List<String> dimension_value = new ArrayList<String>();

//	PENSER A CHANGER AVEC CODE DANS GeoJSON_avar.java

	public static void execute(HttpServletRequest request, List<String> liste_dimensions, List<Indicateur> liste_ind,
			List<Measure_Display> liste_mesure_display, String geoJSON_file, String boundary_file,
			String s_templatejson_File, String export, String hote, String disc,String geojsonpath,String templatepath, int date, String Rule_name)
			throws IOException, ParseException, SQLException {
		Double sh = new Double(0.0);
		shh = 0.0;
		// avant la création des fichiers necessaires pour l'affichage cartographique,
		// il faut vérifier le type de la requête afin d'avoir la bonne classe à
		// executer
		// ecrireDonneesGeoJSON
		// ecrireDonneesGeoJSONBar
		// ecrireDonneesGeoJSONBarRegion
		// ecrireDonneesGeoJSONBarMultiMeasure
		// ecrireDonneesGeoJSONBarMultiMeasureRegion

		// ecrireDonneesTemplates
		// ecrireDonneesTemplatesBar
		// ecrireDonneesTemplates2MultiBar
		// ecrireDonneesTemplatesBarNMeasure
		// ecrireDonneesTemplatesBarMultiNMeasure

		// CRITERE A TENIR COMPTE <NB MESURE> et <NB NIV SPATIAL>
		ArrayList<String> measure = new ArrayList<String>();
//	for (String ind : liste_dimensions) 
//
//	System.out.println(" Liste de nos dimensions : " + ind);

		// RECUPERATION DES MESURES UTILISEES
		for (Indicateur ind : liste_ind) {
			for (Measure_Display mes : liste_mesure_display) {
				if (ind.getMeasure().equals(mes.getMeasureName())) {
					if (!measure.contains(mes.getMeasureName())) {
						System.out.println(" nom mesure utilisée : " + mes.getMeasureName());
						measure.add(mes.getMeasureName());
						break;
					}
					break;
				}
			}
		}

		System.out.println(" nb mes : " + measure.size() + " VS mes disp : " + liste_mesure_display.size());

		System.out.println();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date dateReq = new Date();

		// RECUPERATION DE LA DATE
		String dt = dateFormat.format(dateReq).toString();
		dt = dt.replace(" ", "").replace("/", "").replace(":", "");


		/*
		 * -----------------------------------------------------------------------------
		 * -
		 */

		// RECUPERATION DES INFORMATIONS DANS LE FICHIER config.xml
		Document documentConfig = Base_Connexion.GetConfigBase(request, export);
		Element rootgeo = documentConfig.getRootElement();
		String table = rootgeo.getChildText("Table");
		Element levels = rootgeo.getChild("Levels");
		List list_Level = levels.getChildren("Level");
		Connection connexion = null;

		try {
			connexion = Base_Connexion.connexionBase(request, export);
		} catch (ClassNotFoundException e) {
			//
			e.printStackTrace();
		} catch (SQLException e) {
			//
			e.printStackTrace();
		}

		HashMap<String, Geometrie> geometry = new HashMap<String, Geometrie>();
		String zone_liste = null;

		// afin d'ajouter les mesures dans les mÃªme document JSON. Autrement dit, avoir
		// un attribut pour chaque mesure dans tous les 'features' au lieu d'ajouter
		// des nouveaux 'features' pour la deuxiÃ¨me mesure
		Map<String, Integer> features_id = new HashMap<String, Integer>();

		// String dis_type="";

		int ct = 0; // COMPTEUR POUR COMPTER LE NOMBRE D'ELEMENT DEJA ECRIT DANS LE FICHIER
		int ctEl = 0;
		for (Indicateur ind : liste_ind) {

			String ind_spatial = ind.getSpatial();

			// pour charger les gÃ©omÃ©tries des toutes les zones spatiales en une fois
			if (!geometry.containsKey(ind_spatial)) {
				if (zone_liste == null) {
					zone_liste = "'" + ind_spatial + "'";
				} else {
					zone_liste = zone_liste + ", '" + ind_spatial + "'";
				}
			}
			ctEl++;
		}

		ArrayList<String> liste_niveau = new ArrayList<String>();

		// RECUPERER LEs GEOMETRIES DES MEMBRES SPATIAUX - liste region/departement
		Donnee_geo.get_niveau(table, list_Level, zone_liste, connexion, liste_niveau);
		System.out.println(" liste_niveau : " + liste_niveau);
		System.out.println(" list_level : " + list_Level);
		System.out.println(" zone_liste : " + zone_liste);
		/*
		 * -----------------------------------------------------------------------------
		 * -
		 */

		Map<String, String> liste_fichier_mesure = new HashMap<String, String>();

		// ACTION A FAIRE SI LA REGLE RETOURNEE EST "Bars"
		if (Rule_name.equals("Bars")) {
			System.out.println(" here is bar " + geoJSON_file);

			int y = 0;

			String lab = "";
			int c = 0;
			List<String> dimension_val = new ArrayList<String>();
			Map<String, String> liste_legende = new HashMap<String, String>();
			// RECUPERATION DE LA LISTE DES LEGENDES DES INDICATEURS UTILISEES : pour
			// dimension conduite/date -> legende {biologique_2017; biologique_2018;
			// conventionnelle_2017; conventionnelle_2018}
			for (Indicateur ind : liste_ind) {

				List<String> dimesnions_membres = new ArrayList<String>();
				int j = 0;
				dimesnions_membres = ind.getAttributes();

				lab = ""; // variable qui va contenir le nom des légendes à créer
				int ct_dim = 0;
				for (String dimension : liste_dimensions) {
					// pour identifier le ID de feature on ajoute les valeurs de différents membres
					// de dimensions (PAS de mesures)
					// si les valeurs sont identiques donc c'est le même feature. alors, il faut
					// ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans
					// le même feature
					if (!dimension.toLowerCase().equals("Measures".toLowerCase())) {
						if (!dimension_val.contains(dimesnions_membres.get(j))) {
							dimension_val.add(dimesnions_membres.get(j));
						}
						if (ct_dim == 0) {
							lab = dimesnions_membres.get(j);
							ct_dim++;
						} else {
							// le nom de la légende va être la combinaison des indicateurs hors-mesure
							// utilisée
							lab = lab + "_" + dimesnions_membres.get(j);
						}

					}
					j++;
				}
				// ajout legende dans liste_legende
				if (!liste_legende.containsValue(lab)) {
					liste_legende.put("" + c + "", lab);
					c++;
				}

			}
			System.out.println(" NB LISTE LEGENDE : " + liste_legende.size());

			System.out.println();

			// VERIFICATION SI NB MES > 1
			if (measure.size() > 1) {
				// initialisation de la valeur du positionnement du bar quand on a plusieurs
				// mesures.
				shifting = -(measure.size() * 0.2);

				// CREER DES GeoJSON POUR CHAQUE MESURE
				for (String mes : measure) {

					System.out.println(" mesure utilisé : " + mes);

					// ajout du nom du fichier geojson avec le nom de la mesure
					liste_fichier_mesure.put(mes, geoJSON_file + "_Bar_" + mes.replace(":", "_").replace(" ", "_") + "_"
							+ liste_niveau.get(y));

					// ECRITURE GEOJSON MULTIBAR MULTILAYER MULTIMESURE
					ecrireDonneesGeoJSONBarMultiMeasure(request, liste_dimensions, liste_ind, liste_mesure_display,
							geoJSON_file + "_Bar_" + mes.replace(":", "_").replace(" ", "_"), export, sh, hote, disc,geojsonpath,templatepath,
							dt, date, liste_niveau.get(y), mes, liste_legende);

//				CODE PERMETTANT DE CREER UN VISUALISATION MULTI-ECHELLE
					if (liste_niveau.size() > 1) {
//					ecrireDonneesGeoJSONBarPositionMultiMeasure(request, liste_dimensions, liste_ind, liste_mesure_display, geoJSON_file+"_Bar_"+mes.replace(":", "_").replace(" ", "_"), export, dt, date,  liste_niveau.get(y), 0, liste_legende, mes);
					}

					System.out.println(" ---- shifting value ---- " + shifting);

				}

				// DANS LE CAS OU ON A UNE SEULE MESURE
			} else {
				// La creation des bars des niveaux spatiales plus haut se font dans la fonction
				// ecrireDonneesGeoJSONBar
				// ECRITURE GEOJSON BAR
				ecrireDonneesGeoJSONBar(request, liste_dimensions, liste_ind, liste_mesure_display,
						geoJSON_file + "_Bar", export, sh, hote, disc,geojsonpath,templatepath, dt, date, liste_niveau.get(y), liste_legende);

				// Perspective Pour les min devant et les max arrières ENCORE A VOIR
//			if(liste_niveau.size()>1) {
//				ecrireDonneesGeoJSONBarPosition(request, liste_dimensions, liste_ind, liste_mesure_display, geoJSON_file+"_Bar", export, dt, date,  liste_niveau.get(y), 0, liste_legende);
//			}

			}

			// RECUPERATION DES POLYGONES SPATIAUX
			while (y < liste_niveau.size()) {

				System.out.println(" print legend : ");
				for (int i = 0; i < liste_legende.size(); i++) {
					System.out.println(liste_legende.get("" + i));
				}
				System.out.println(" &&& ");
//			recuperationDataLegend(request, liste_dimensions, liste_ind, liste_mesure_display, geoJSON_file+"_Polygon", export, dt, date, liste_niveau.get(y), geo_ind, label_ok);
				System.out.println("BEFORE POLYGON DATA LEGEND ");
				ecrireDonneesGeoJSONPolygon(request, liste_dimensions, liste_ind, liste_mesure_display,
						geoJSON_file + "_Polygon", export, hote, disc,geojsonpath,templatepath, dt, date, liste_niveau.get(y), geo_ind, label_ok,
						measure);
				System.out.println("ecrire Donnees GeoJSON Polygon done " + liste_niveau.get(y));
				y++;
			}

			// GENERATION DU FICHIER DE CONFIGURATION

			if (liste_niveau.size() > 1) {

				String[] nom_fichier_polygon = new String[liste_niveau.size()];
				String[] nom_fichier_bar = new String[liste_niveau.size()];
				c = 0;
				for (String i : liste_niveau) {
					System.out.println(i);
					nom_fichier_polygon[c] = "Polygon_" + i;
					nom_fichier_bar[c] = "Bar_" + i;
					c++;
				}

				if (measure.size() > 1) {

					// CREATION TEMPLATE NMEASURE MUTLI LAYER BAR

					// créer une liste contenant mesure et niveau spatial
					// avec combinaison de liste_niveau et measure
					String[][] niv_mes = new String[measure.size() * liste_niveau.size()][2];
					int i = 0;
					for (String mes : measure) {
						for (String niv : liste_niveau) {
							niv_mes[i][0] = niv;
							niv_mes[i][1] = mes;
							i++;
						}
					}

					// les polygones doivent être associer au niveau spatial
					// les bar doivent être associer au mes et au niv
					// ECRITURE TEMPLATE MULTIBAR - MULTIMESURE
					ecrireDonneesTemplatesBarMultiNMeasure(Indicators.dim_mem_done, s_templatejson_File,
							geoJSON_file + "_Polygon_", geoJSON_file + "_Bar_", boundary_file, export, hote, disc,geojsonpath,templatepath, dt,
							date, liste_fichier_mesure, measure, Rule_name, niv_mes, liste_niveau, liste_legende);

					System.out.println(" FIN ECRITURE FICHIER template multi nmesure ");

				} else {

//        		ecrireDonneesTemplates2MultiBarAvAr(Indicators.dim_mem_done, s_templatejson_File, liste_niveau,
//        				geoJSON_file+"_"+nom_fichier_polygon[0], geoJSON_file+"_"+nom_fichier_polygon[1], 
//        				geoJSON_file+"_"+nom_fichier_bar[0], geoJSON_file+"_"+nom_fichier_bar[0]+"_devant", geoJSON_file+"_"+nom_fichier_bar[0]+"_derriere", 
//        				geoJSON_file+"_"+nom_fichier_bar[1], geoJSON_file+"_"+nom_fichier_bar[1]+"_devant", geoJSON_file+"_"+nom_fichier_bar[1]+"_derriere", 
//        				boundary_file, export, dt, date);

					// ECRITURE TEMPLATE AVEC MULTIBAR - MULTILAYER
					ecrireDonneesTemplates2MultiBar(Indicators.dim_mem_done, s_templatejson_File, liste_niveau,
							geoJSON_file + "_" + nom_fichier_polygon[0], geoJSON_file + "_" + nom_fichier_polygon[1],
							geoJSON_file + "_" + nom_fichier_bar[0], geoJSON_file + "_" + nom_fichier_bar[1],
							boundary_file, export, hote, disc,geojsonpath,templatepath, dt, date, liste_legende);

				}

			} else {
				if (measure.size() > 1) {

					// ECRITURE TEMPLATE AVEC BAR+MULTIMESURE
					ecrireDonneesTemplatesBarNMeasure(Indicators.dim_mem_done, s_templatejson_File,
							geoJSON_file + "_Polygon_" + liste_niveau.get(0),
							geoJSON_file + "_Bar_" + liste_niveau.get(0), boundary_file, export, hote, disc,geojsonpath,templatepath, dt, date,
							liste_fichier_mesure, measure, Rule_name, liste_legende);

				} else {
					// ECRITURE TEMPLATE AVEC LES BAR
					ecrireDonneesTemplatesBar(Indicators.dim_mem_done, s_templatejson_File,
							geoJSON_file + "_Polygon_" + liste_niveau.get(0),
							geoJSON_file + "_Bar_" + liste_niveau.get(0), boundary_file, export, hote, disc,geojsonpath,templatepath, dt, date,
							Rule_name, liste_niveau.get(0), liste_legende);

				}

			}

			System.out.println("ecrire Donnees Templates Bar done");
		} else {

			// ANALYSE AVEC LA DIMENSION SPATIALE ET UNE MESURE - GeoJSON + template

			ecrireDonneesGeoJSON(request, liste_dimensions, liste_ind, liste_mesure_display, geoJSON_file, export, hote,
					disc,geojsonpath,templatepath, dt, date);
			GeoJsonLayers(disc, geojsonpath, templatepath, liste_ind);
			System.out.println("ecrire Donnees GeoJSON done");
			ecrireDonneesTemplates(Indicators.dim_mem_done, s_templatejson_File, disc + "/" + geoJSON_file + "Data",
					disc + "/" + geoJSON_file + "EmptyData", boundary_file, export, hote, disc,geojsonpath,templatepath, dt, date, Rule_name);
			System.out.println("ecrire Donnees Templates done");

		}
	}

// classe active quand on a une mesure avec les dimensions spatiales

	@SuppressWarnings("unchecked")
	private static void ecrireDonneesGeoJSON(HttpServletRequest request, List<String> liste_dimensions,
			List<Indicateur> liste_ind, List<Measure_Display> liste_mesure_display, String geoJSON_file, String export,
			String hote, String disc,String geojsonpath,String templatepath, String dt, int date) throws IOException {

		FileWriter filename = null;
		if (export.contentEquals("VM")) {
			if (date == 0) {
				filename = new FileWriter(geojsonpath + "\\geoJSON.json");
			}
			if (date == 1) {
				filename = new FileWriter(geojsonpath + "\\geoJSON" + "_" + dt + ".json"); // CREATION D'UN
																									// NOUVEAU
				// FICHIER A CHAQUE FOIS
			}

		}

		HashMap<String, Geometrie> geometry = new HashMap<String, Geometrie>();
		// d'abord on prépare la strucutre de fichier JSON
		JSONObject geoJsonData = new JSONObject();
		JSONObject crs = new JSONObject();
		JSONObject crs_properties = new JSONObject();
		crs_properties.put("name", "urn:ogc:def:crs:OGC:1.3:CRS84");
		crs.put("type", "name");
		crs.put("properties", crs_properties);
		JSONArray features = new JSONArray();
		geoJsonData.put("type", "FeatureCollection");
		geoJsonData.put("name", "t");
		geoJsonData.put("crs", crs);
		geoJsonData.put("features", features);

		// charger les informations nécessaires pour trouver les géometries
		Document documentConfig = Base_Connexion.GetConfigBase(request, export);
		Element rootgeo = documentConfig.getRootElement();
		String table = rootgeo.getChildText("Table");
		Element levels = rootgeo.getChild("Levels");
		List list_Level = levels.getChildren("Level");
		Connection connexion = null;

		try {
			connexion = Base_Connexion.connexionBase(request, export);
		} catch (ClassNotFoundException e) {
			//
			e.printStackTrace();
		} catch (SQLException e) {
			//
			e.printStackTrace();
		}
		//////////////// Fin pour la charge des géometries

		// Pour charger les geométries et faire des statistiques sur les données
		// max,min, count, count par zone spatiale, sum par zone spatiale SI NECESSAIRE
		String zone_liste = null;

		// afin d'ajouter les mesures dans les même document JSON. Autrement dit, avoir
		// un attribut pour chaque mesure dans tous les 'features' au lieu d'ajouter
		// des nouveaux 'features' pour la deuxième mesure
		Map<String, Integer> features_id = new HashMap<String, Integer>();

		int ct = 0; // COMPTEUR POUR COMPTER LE NOMBRE D'ELEMENT DEJA ECRIT DANS LE FICHIER

		// RECUPERATION DE LA LISTE DES NOMS DES ZONES
		for (Indicateur ind : liste_ind) {
			String ind_spatial = ind.getSpatial();
			// pour charger les géométries des toutes les zones spatiales en une fois
			if (!geometry.containsKey(ind_spatial)) {
				if (zone_liste == null) {
					zone_liste = "'" + ind_spatial + "'";
				} else {
					zone_liste = zone_liste + ", '" + ind_spatial + "'";
				}
			}
		}

		// RECUPERATION DES GEOMETRIES DES ZONES SPATIAUX
		Donnee_geo.get_donneesGeo_liste_JSON2(table, list_Level, zone_liste, connexion, geometry, count, countElem);

		System.out.println(" table get element : " + table);

		// COMPTAGE DU NOMBRE D'ELEMENT
		int ctEl = Donnee_geo.get_nbElement(table, list_Level, zone_liste, connexion); // RECUPERATION DU NOMBRE
																						// D'ELEMENT RETOURNEE PAR LA
																						// REQUETE

		System.out.println("nb element :" + ctEl);

		// pour ajouter les données JSON
		int i = 0;
		double[] mes = null;

		for (Indicateur ind : liste_ind) {
			JSONObject jsondata = new JSONObject();
			jsondata.put("type", "Feature");

			JSONObject jsondata_properties = new JSONObject();
			jsondata_properties.put("ID", i + 1);

			// pour identifier le ID de feature
			String feature_identifiant = ind.getSpatial();

			// pour ajouter les membres des dimensions
			// On crée une List contenant tous les attribute "attribute(i)" de l'Element
			// racine
			List<String> dimesnions_membres = new ArrayList<String>();
			int j = 0;
			dimesnions_membres = ind.getAttributes();

			for (String dimension : liste_dimensions) {
				// pour identifier le ID de feature on ajouter les valeurs de différents membres
				// de dimensions (PAS de mesures)
				// si les valeurs sont identiques donc c'est le même feature. alors, il faut
				// ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans
				// le même feature
				if (!dimension.toLowerCase().equals("Measures".toLowerCase())) {
					feature_identifiant = feature_identifiant + "_" + dimesnions_membres.get(j);

					jsondata_properties.put(dimension, dimesnions_membres.get(j));
				}
				j++;
			}
			// fin d'ajout des memebres des dimensions

			jsondata_properties.put(ind.getMeasure(), ind.getValeur());

			jsondata_properties.put("_Location", ind.getSpatial());

			// r飵p鲡tion du centroid du polygone
			JSONObject jo = geometry.get(ind.getSpatial()).getCentroidJson();
			Iterator itr = jo.values().iterator();
			while (itr.hasNext()) {
				Object element = itr.next();

				if (element.toString().length() > 5) {

					String[] val = element.toString().split(",");

					float lon = Float.parseFloat(val[0].replace("[", ""));
					float lat = Float.parseFloat(val[1].replace("]", ""));

					jsondata_properties.put("long_centro", lon);
					jsondata_properties.put("lat_centro", lat);

				}
			}

			jsondata.put("properties", jsondata_properties);

			int test = geometry.get(ind.getSpatial()).getCount();

			// test verification pour le nombre de niveau spatial
			if (test > 1) {

				jsondata.put("geometry", geometry.get(ind.getSpatial()).getGeoJsonCollect());
			} else {

				jsondata.put("geometry", geometry.get(ind.getSpatial()).getGeoJson());

				// TEST SUR LA CREATION D'UN CERLCE
//				jsondata.put("geometry", geometry.get(ind.getSpatial()).getCentroidJson());

			}

			// pour identifier le ID de feature
			if (!features_id.containsKey(feature_identifiant)) {
				features_id.put(feature_identifiant, i);
//				System.out.println(" -> feature identifiant " + feature_identifiant + " " + i);
				features.add(features_id.get(feature_identifiant), jsondata);
				i++;
			} else {

				int id = features_id.get(feature_identifiant);

				JSONObject old_feature = new JSONObject();
				old_feature = (JSONObject) features.get(id);
				JSONObject properties = new JSONObject();

				// add les mesures déjà trouvées
				properties.putAll(((JSONObject) old_feature.get("properties")));

				// add la nouvelle mesure
				properties.putAll(((JSONObject) jsondata.get("properties")));

				jsondata.put("properties", properties);
				features.set(id, jsondata);

			}
			ct += 1;

			// ECRITURE LIGNE PAR LIGNE DU FICHIER JSON SUR LA MACHINE 51.
			if (export.contentEquals("VM")) {
				if (ctEl == 1) { // TEST SI IL N'Y A QU'UN ELEMENT DANS LA REPONSE DE LA REQUETE
					TEXT.savefile4(filename, geoJsonData.toString(), ct, ctEl);
					geoJsonData = new JSONObject();
				} else {
					if (ct == 1 && ctEl != 1) { // TEST SI C'EST LE PREMIER ELEMENT A ECRIRE ET QU'IL Y A PLUSIEURS
												// ELEMENT DE REPONSE
						TEXT.savefile4(filename, geoJsonData.toString().replace("}]}", "},\n"), ct, ctEl);
						geoJsonData = new JSONObject();
					} else if (ct == ctEl && ctEl != 1) { // TEST SI TOUS LES ELEMENTS ONT ETE ECRIT DANS LE FICHIER
						TEXT.savefile4(filename, jsondata.toString().replace("}}", "}}]}"), ct, ctEl);
					} else { // ECRIRE LES ELEMENTS DANS LE FICHIER
						TEXT.savefile4(filename, jsondata.toString().replace("}}", "}},\n"), ct, ctEl);

					}
				}
			}

		}

		// calcul range Quantile

		if (export.equals("local") || export.equals("VM_local")) {

//			  geoJsonData
			System.out.println(" ECRITURE FICHIER JSON ");

			System.out.println(" --- ");
			System.out.println(geoJsonData.toString().replace("}},", "}},\n"));
			System.out.println(" --- ");

			if (date == 0) {
				TEXT.savefile3(geoJSON_file + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																										// FICHIER
																										// UNIQUE
			}
			if (date == 1) {
				TEXT.savefile3(geoJSON_file + "_" + dt + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																													// FICHIER
																													// AVEC
																													// LA
																													// DATE
																													// DANS
																													// LE
																													// NOM
			}

			System.out.println(" FIN ECRITURE FICHIER JSON ");
		}

	}

// clasee qui permet de séparer geojson en deux  couches celle contenant les valeurs vides et celles non vides
	// Yassine
	private static void GeoJsonLayers(String disc,String geojsonpath,String templatepath, List<Indicateur> liste_ind) {
		// JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();
		String measure = "";
		String geojson3 = "";
		String valeurempty = "";
		String valeur = "";

		for (Indicateur ind : liste_ind) {
			measure = ind.getMeasure();
		}
		try (FileReader reader = new FileReader(geojsonpath + "\\geoJSON.json")) {

			// Read JSON file
			Object obj = jsonParser.parse(reader);
			JSONObject geojson = (JSONObject) obj;
			JSONArray array = (JSONArray) geojson.get("features");
			Iterator j = array.listIterator();
			while (j.hasNext()) {
				JSONObject geojson1 = (JSONObject) j.next();
				JSONObject geojson2 = (JSONObject) geojson1.get("properties");
				if (geojson2.get(measure).toString().contentEquals("99999.0")) {

					// System.out.println(geojson.toString().replaceFirst("\\[\\{.*\\W*.*\\}\\]",
					// "["+geojson1.toString()+"]"));
					valeurempty = valeurempty + geojson1.toString() + ",";

				} else {

					// System.out.println("3amr");
					valeur = valeur + geojson1.toString() + ",";

				}

			}
			StringBuilder geojson4 = new StringBuilder(valeurempty);
			StringBuilder geojson5 = new StringBuilder(valeur);
//			System.out.println("khawe " + geojson4);
//			System.out.println("khawe1 " + valeurempty);
			geojson5.setCharAt(valeur.lastIndexOf(","), ' ');
			if (!(geojson4.length() == 0))
				geojson4.setCharAt(valeurempty.lastIndexOf(","), ' ');
			TEXT.savefile3(geojsonpath + "\\geoJSONData.json",
					geojson.toString().replaceFirst("\\[\\{.*\\W*.*\\}\\]", "[" + geojson5.toString() + "]"));
			TEXT.savefile3(geojsonpath + "\\geoJSONEmptyData.json",
					geojson.toString().replaceFirst("\\[\\{.*\\W*.*\\}\\]", "[" + geojson4.toString() + "]"));
			if ((geojson4.length() == 0)) {
				PrintWriter writer = new PrintWriter(geojsonpath + "\\geoJSONEmptyData.json");
				writer.print("");
				writer.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
// classe qui permet la création des polygones

	@SuppressWarnings({ "unchecked", "unlikely-arg-type" })
	private static void ecrireDonneesGeoJSONPolygon(HttpServletRequest request, List<String> liste_dimensions,
			List<Indicateur> liste_ind, List<Measure_Display> liste_mesure_display, String geoJSON_file, String export,
			String hote, String disc,String geojsonpath,String templatepath, String dt, int date, String niveau, String[][] geo_ind, List<String> label_ok,
			ArrayList<String> measure) throws IOException, SQLException {

		// essai pour la création d'une nouvelle structuration
		if (measure.size() == 1) {

			// DANS LE CAS OU ON A QU'UNE MESURE DANS LA REQUETE, on récupère les valeurs
			// des indicateurs pour les attribués au polygones -> paris | ind_1 | ind_2 |
			// ind_3

			String label = "";
			label_ok = new ArrayList<String>();
			List<String> geo_ok0 = new ArrayList<String>();
			for (Indicateur ind : liste_ind) {

				int j = 0;
				int ct_dim = 0;

				List<String> dimesnions_membres = new ArrayList<String>();
				dimesnions_membres = ind.getAttributes();

				if (!geo_ok0.contains(ind.getSpatial())) {
					geo_ok0.add(ind.getSpatial());
				}

				for (String dimension : liste_dimensions) {
					// pour identifier le ID de feature on ajouter les valeurs de diffÃ©rents
					// membres de dimensions (PAS de mesures)
					// si les valeurs sont identiques donc c'est le mÃªme feature. alors, il faut
					// ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans
					// le mÃªme feature
					if (!dimension.toLowerCase().equals("Measures".toLowerCase())) {
						if (!dimension_value.contains(dimesnions_membres.get(j))) {
							dimension_value.add(dimesnions_membres.get(j));
						}
						if (ct_dim == 0) {
							label = dimesnions_membres.get(j);
							ct_dim++;
						} else {
							label = label + "_" + dimesnions_membres.get(j);
						}
					}
					j++;
				}
				if (!label_ok.contains(label) && label.length() > 0) {
					label_ok.add(label);
				}
			}

			geo_ind = new String[geo_ok0.size()][label_ok.size() + 1];

			int cp1 = 0;
			for (String geo : geo_ok0) {

				String[][] legend_value = new String[label_ok.size()][2];
				int cp = 0;

				geo_ind[cp1][0] = geo;

				for (Indicateur ind : liste_ind) {

					if (geo.equals(ind.getSpatial())) {

						int j = 0;
						int ct_dim = 0;
						List<String> dimesnions_membres = new ArrayList<String>();
						dimesnions_membres = ind.getAttributes();

						for (String dimension : liste_dimensions) {
							// pour identifier le ID de feature on ajouter les valeurs de diffÃ©rents
							// membres de dimensions (PAS de mesures)
							// si les valeurs sont identiques donc c'est le mÃªme feature. alors, il faut
							// ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans
							// le mÃªme feature

							if (!dimension.toLowerCase().equals("Measures".toLowerCase())) {

								if (!dimension_value.contains(dimesnions_membres.get(j))) {
									dimension_value.add(dimesnions_membres.get(j));
								}
								if (ct_dim == 0) {
									label = dimesnions_membres.get(j);
									ct_dim++;
								} else {
									label = label + "_" + dimesnions_membres.get(j);
								}
							}
							j++;
						}
						legend_value[cp][0] = label;
						legend_value[cp][1] = "" + ind.getValeur();
						cp++;
					}
				}

				for (int i = 0; i < label_ok.size(); i++) {
					for (int j = 0; j < legend_value.length; j++) {
						if (label_ok.get(i).equals(legend_value[j][0])) {
							geo_ind[cp1][i + 1] = legend_value[j][1];
							break;
						}

					}

				}
				cp1++;

			}

		}

//	String name = "E:\\Data Published\\geoJSON"+"_"+dt+".json";
//	geoJSON_file+"_"+dt+"_.json"
		FileWriter filename = null;
		if (export.contentEquals("VM")) {
			if (date == 0) {
				filename = new FileWriter(geojsonpath + "\\geoJSON_Polygon_" + niveau + ".json");
//			file = new FileWriter( geoJSON_file+"_Polygon_"+niveau+".json");	
			}
			if (date == 1) {
				filename = new FileWriter(geojsonpath + "\\geoJSON" + "_" + dt + ".json"); // CREATION D'UN
																									// NOUVEAU
				// FICHIER A CHAQUE FOIS
			}

		}

		if (export.contentEquals("VM_local")) {
			if (date == 0) {
				filename = new FileWriter(geoJSON_file + "_" + niveau + ".json");

			}
			if (date == 1) {
				filename = new FileWriter(geojsonpath + "\\geoJSON" + "_" + dt + ".json"); // CREATION D'UN
																									// NOUVEAU
				// FICHIER A CHAQUE FOIS
			}

		}

		HashMap<String, Geometrie> geometry = new HashMap<String, Geometrie>();
		// d'abord on prÃ©pare la strucutre de fichier JSON
		JSONObject geoJsonData = new JSONObject();
		JSONObject crs = new JSONObject();
		JSONObject crs_properties = new JSONObject();
		crs_properties.put("name", "urn:ogc:def:crs:OGC:1.3:CRS84");
		// System.out.println("crs_properties:" + crs_properties);
		crs.put("type", "name");
		crs.put("properties", crs_properties);
		JSONArray features = new JSONArray();
		geoJsonData.put("type", "FeatureCollection");
		geoJsonData.put("name", "t");
		geoJsonData.put("crs", crs);
		geoJsonData.put("features", features);

		// charger les informations nÃ©cessaires pour trouver les gÃ©ometries
		Document documentConfig = Base_Connexion.GetConfigBase(request, export);
		Element rootgeo = documentConfig.getRootElement();
		String table = rootgeo.getChildText("Table");
		Element levels = rootgeo.getChild("Levels");
		List list_Level = levels.getChildren("Level");
		Connection connexion = null;

		try {
			connexion = Base_Connexion.connexionBase(request, export);
		} catch (ClassNotFoundException e) {
			//
			e.printStackTrace();
		} catch (SQLException e) {
			//
			e.printStackTrace();
		}
		//////////////// Fin pour la charge des gÃ©ometries

		// Pour charger les geomÃ©tries et faire des statistiques sur les donnÃ©es
		// max,min, count, count par zone spatiale, sum par zone spatiale SI NECESSAIRE
		String zone_liste = null;

		// afin d'ajouter les mesures dans les mÃªme document JSON. Autrement dit, avoir
		// un attribut pour chaque mesure dans tous les 'features' au lieu d'ajouter
		// des nouveaux 'features' pour la deuxiÃ¨me mesure
		Map<String, Integer> features_id = new HashMap<String, Integer>();

		// String dis_type="";

		int ct = 0; // COMPTEUR POUR COMPTER LE NOMBRE D'ELEMENT DEJA ECRIT DANS LE FICHIER

		for (Indicateur ind : liste_ind) {

			String ind_spatial = ind.getSpatial();
			// measure = ind.getMeasure();

			// pour charger les gÃ©omÃ©tries des toutes les zones spatiales en une fois
			if (!geometry.containsKey(ind_spatial)) {
				if (zone_liste == null) {
					zone_liste = "'" + ind_spatial + "'";
				} else {
					zone_liste = zone_liste + ", '" + ind_spatial + "'";
				}
			}

		}

		Donnee_geo.get_donneesGeo_liste_JSON3(table, list_Level, zone_liste, connexion, geometry, count, countElem);

		// pour ajouter les donnÃ©es JSON
		int i = 0;
		double[] mes = null;

		ArrayList<String> geo_get = new ArrayList<String>();
		ArrayList<Indicateur> geo_ok = new ArrayList<Indicateur>();
		int ctEl = 0;
		for (Indicateur ind : liste_ind) {
			if (!geo_get.contains(ind.getSpatial()) && geometry.get(ind.getSpatial()).getNiveau().equals(niveau)) {
				geo_get.add(ind.getSpatial());
				geo_ok.add(ind);
				ctEl++;
				// System.out.println(ind.getSpatial());
			}

		}
		System.out.println("--------------------------");
		System.out.println("nb element :" + ctEl);
		String reg = "";
		int idct = 0;
		ct = 0;
		for (Indicateur ind : geo_ok) {

			if (geometry.get(ind.getSpatial()).getNiveau().equals(niveau)) {

				JSONObject jsondata = new JSONObject();
				jsondata.put("type", "Feature");

				JSONObject jsondata_properties = new JSONObject();
				jsondata_properties.put("ID", idct);
				idct++;

				jsondata_properties.put("val_", 1);

				// pour identifier le ID de feature
				String feature_identifiant = ind.getSpatial();

				jsondata_properties.put("_Location", ind.getSpatial());

				// INSERTION DES VALEURS DES INDICATEURS ENREGISTREES

				if (measure.size() == 1) {
//					System.out.println(" DEBUT POLYGON TEST SI OK OU PAS");
					for (int ii = 0; ii < geo_ind.length; ii++) {

						if (geo_ind[ii][0].equals(ind.getSpatial())) {

//								System.out.println("nom polygon " + geo_ind[ii][0]);
							for (int j = 0; j < label_ok.size(); j++) {
//									System.out.println(label_ok.get(j) + " = " + geo_ind[ii][j+1]);
								jsondata_properties.put(label_ok.get(j), geo_ind[ii][j + 1]);
							}
//								System.out.println();
							break;
						}
					}
//					System.out.println(" FIN TEST");
				}

				// AJOUTER L'ATTRIBUT REGION POUR LES DEPARTEMENTS, RECUPERER SON HIERARCHIE
				// PERE
				if (niveau.equals("departement")) {
					jsondata_properties.put("_reg", Donnee_geo.get_Region(connexion,
							Base_Connexion.GetConfigBase(request, export), ind.getSpatial()));

				}

				jsondata.put("properties", jsondata_properties);

				int test = geometry.get(ind.getSpatial()).getCount();

				if (test > 1) {

					jsondata.put("geometry", geometry.get(ind.getSpatial()).getGeoJsonCollect());
				} else {
					// TEST SUR LA CREATION D'UN CERLCE
					jsondata.put("geometry", geometry.get(ind.getSpatial()).getGeoJson());

				}

				// pour identifier le ID de feature
				if (!features_id.containsKey(feature_identifiant)) {
					features_id.put(feature_identifiant, i);
					features.add(features_id.get(feature_identifiant), jsondata);

				} else {

					int id = features_id.get(feature_identifiant);

					JSONObject old_feature = new JSONObject();
					old_feature = (JSONObject) features.get(id);
					JSONObject properties = new JSONObject();

					// add les mesures dÃ©jÃ  trouvÃ©es
					properties.putAll(((JSONObject) old_feature.get("properties")));

					// add la nouvelle mesure
					properties.putAll(((JSONObject) jsondata.get("properties")));

					jsondata.put("properties", properties);
					features.set(id, jsondata);

				}
				ct += 1;

				if (export.contentEquals("VM")) {
					if (ctEl == 1) { // TEST SI IL N'Y A QU'UN ELEMENT DANS LA REPONSE DE LA REQUETE
						TEXT.savefile4(filename, geoJsonData.toString(), ct, ctEl);
						geoJsonData = new JSONObject();

					} else {
						if (ct == 1 && ctEl != 1) { // TEST SI C'EST LE PREMIER ELEMENT A ECRIRE ET QU'IL Y A PLUSIEURS
													// ELEMENT DE REPONSE

							TEXT.savefile4(filename, geoJsonData.toString().replace("}]}", "},\n"), ct, ctEl);
							geoJsonData = new JSONObject();

						} else if (ct == ctEl && ctEl != 1) { // TEST SI TOUS LES ELEMENTS ONT ETE ECRIT DANS LE FICHIER

							TEXT.savefile4(filename, jsondata.toString().replace("}}", "}}]}"), ct, ctEl);

						} else { // ECRIRE LES ELEMENTS DANS LE FICHIER
							TEXT.savefile4(filename, jsondata.toString().replace("}}", "}},\n"), ct, ctEl);
						}
					}
				}
				if (export.contentEquals("VM_local")) {
					if (ctEl == 1) { // TEST SI IL N'Y A QU'UN ELEMENT DANS LA REPONSE DE LA REQUETE
						TEXT.savefile4(filename, geoJsonData.toString(), ct, ctEl);
						geoJsonData = new JSONObject();
					} else {
						if (ct == 1 && ctEl != 1) { // TEST SI C'EST LE PREMIER ELEMENT A ECRIRE ET QU'IL Y A PLUSIEURS
													// ELEMENT DE REPONSE
							System.out.println(" begin ");
							TEXT.savefile4(filename, geoJsonData.toString().replace("}]}", "},\n"), ct, ctEl);
							geoJsonData = new JSONObject();
						} else if (ct == ctEl && ctEl != 1) { // TEST SI TOUS LES ELEMENTS ONT ETE ECRIT DANS LE FICHIER
							System.out.println(" end ");
							TEXT.savefile4(filename, jsondata.toString().replace("}}", "}}]}"), ct, ctEl);

						} else { // ECRIRE LES ELEMENTS DANS LE FICHIER
							TEXT.savefile4(filename, jsondata.toString().replace("}}", "}},\n"), ct, ctEl);

						}
					}
				}
			}
		}

		if (export.equals("local")) {
			System.out.println(" ECRITURE FICHIER JSON ");
			if (date == 0) {
				TEXT.savefile3(geoJSON_file + "_" + niveau + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																														// FICHIER
																														// UNIQUE
			}
			if (date == 1) {
				TEXT.savefile3(geoJSON_file + "_" + dt + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																													// FICHIER
																													// AVEC
																													// LA
																													// DATE
																													// DANS
																													// LE
																													// NOM
			}

			System.out.println(" FIN ECRITURE FICHIER JSON ");
		}

	}

//classe qui permet la création des bars 
	public static double separateBar(Indicateur ind, List<Indicateur> liste_ind, Double sh) {
		int n = ind.getAttributes().size();
		if (liste_ind.indexOf(ind) != 0) {

			for (int h = n - 1; h > 1; h--) {

				if (!ind.getAttributes().get(n - h)
						.equals(liste_ind.get(liste_ind.indexOf(ind) - 1).getAttributes().get(n - h))) {
					sh = sh + 0.08;
					break;
				}

			}
			shh += sh;
		}
		return shh;

	}

	@SuppressWarnings("unchecked")
	private static void ecrireDonneesGeoJSONBar(HttpServletRequest request, List<String> liste_dimensions,
			List<Indicateur> liste_ind, List<Measure_Display> liste_mesure_display, String geoJSON_file, String export,
			Double sh, String hote, String disc,String geojsonpath,String templatepath, String dt, int date, String niveau, Map<String, String> liste_legende)
			throws IOException, ParseException, SQLException {
		FileWriter file = null;

		if (export.contentEquals("VM")) {
			if (date == 0) {
				file = new FileWriter(geojsonpath + "\\geoJSON_Bar_" + niveau + ".json");
//			file = new FileWriter( geoJSON_file+"_"+niveau+".json");

			}
			if (date == 1) {
				file = new FileWriter(geojsonpath + "\\geoJSON" + "_" + dt + ".json"); // CREATION D'UN NOUVEAU
																								// FICHIER A CHAQUE FOIS
			}

		}

		if (export.contentEquals("VM_local")) {
			if (date == 0) {
//			file = new FileWriter( "E:\\Data Published\\geoJSON_Bar_"+niveau+".json");	
				file = new FileWriter(geoJSON_file + "_" + niveau + ".json");

			}
			if (date == 1) {
				file = new FileWriter(geojsonpath + "\\geoJSON" + "_" + dt + ".json"); // CREATION D'UN NOUVEAU
																								// FICHIER A CHAQUE FOIS
			}

		}

//	---
		Integer cpt = 0;
		shifting = 0.0;

		// cette variable est utilisée pour indiquer si il y az une mesure n'utilise pas
		// un type d'affichage multicarte
		boolean map0existe = false;

		List<Element> liste_root_map = new ArrayList<Element>();

		max_ind_Values = new HashMap<String, Double>();
		min_ind_Values = new HashMap<String, Double>();
		count_mes_ind = new HashMap<String, Double>();
		count_map_by_type = new HashMap<String, Integer>();

		max_mes_Values = new HashMap<String, Double>();
		min_mes_Values = new HashMap<String, Double>();

		sum_mes_sp_Values = new HashMap<String, Double>();
		sum_mes_sp_Values_curr = new HashMap<String, Double>();

		// pour creer les bars et déplacer les bars avec chaque indiquateur
		//////////////////////////////////////
		Double bar_width = 0.0;
		Double bar_min_high = 0.0;
		Double bar_max_high = 0.0;

		List<String> list_ind_done = new ArrayList<String>();
		List<String> list_measure_done = new ArrayList<String>();

//	---

		HashMap<String, Geometrie> geometry = new HashMap<String, Geometrie>();
		// d'abord on prépare la strucutre de fichier JSON
		JSONObject geoJsonData = new JSONObject();
		JSONObject crs = new JSONObject();
		JSONObject crs_properties = new JSONObject();
		crs_properties.put("name", "urn:ogc:def:crs:OGC:1.3:CRS84");
		// System.out.println("crs_properties:" + crs_properties);
		crs.put("type", "name");
		crs.put("properties", crs_properties);
		JSONArray features = new JSONArray();
		geoJsonData.put("type", "FeatureCollection");
		geoJsonData.put("name", "t");
		geoJsonData.put("crs", crs);
		geoJsonData.put("features", features);

		// charger les informations nécessaires pour trouver les géometries
		Document documentConfig = Base_Connexion.GetConfigBase(request, export);
		Element rootgeo = documentConfig.getRootElement();
		String table = rootgeo.getChildText("Table");
		Element levels = rootgeo.getChild("Levels");
		List list_Level = levels.getChildren("Level");
		Connection connexion = null;

		try {
			connexion = Base_Connexion.connexionBase(request, export);
		} catch (ClassNotFoundException e) {
			//
			e.printStackTrace();
		} catch (SQLException e) {
			//
			e.printStackTrace();
		}
		//////////////// Fin pour la charge des géometries

//	---
		String zone_liste = null;
		String dis_type = "";
		int ctEl = 0;
		String label = "";

		// RECUPERATION DES VALEURS MIN/MAX DES INDICATEURS POUR GERER LA TAILLE DES
		// BARS
		for (Indicateur ind : liste_ind) {

			String measure = "";

			String ind_name = ind.getNom();
			double ind_value = ind.getValeur();
			String ind_spatial = ind.getSpatial();
			measure = ind.getMeasure();

			// pour charger les géométries des toutes les zones spatiales en une fois
			if (!geometry.containsKey(ind_spatial)) {
				if (zone_liste == null) {
					zone_liste = "'" + ind_spatial + "'";
				} else {
					zone_liste = zone_liste + ", '" + ind_spatial + "'";
				}
			}
			/////////////////////////////////////////////

			// VERIFICATION SI LA MESURE A DEJA ETE PRISE;
			if (!list_measure_done.contains(measure)) {
				// on initiale (0) le nombre d'indiquateurs pour chaque mesure à afficher dans
				// chaque zone spatial
				count_mes_ind.put(measure, 0.0);

				// on initiale dis_type parce que il est possible que la mesure ne soit pas
				// décrite dans le fichier display_conf
				// pour qu'il ne garde pas l'affichage de la dernière mesure trouvée
				System.out.println(" measure equals :: " + measure);
				dis_type = "";
				for (Measure_Display m_d : liste_mesure_display) {

					if (m_d.getMeasureName().equals(measure)) {
						dis_type = m_d.getDisplayType();

						break;
					}
				}
				list_measure_done.add(measure);
			}

			/*
			 * Recuperation des valeurs min et max de chaque indicateur et count
			 * d'indicateur pour chaque mesure Pour créer des intervalles égaux et
			 * convenables dans le sdl
			 */
			if (min_ind_Values.containsKey(ind_name)) {
				if (ind_value < min_ind_Values.get(ind_name)) {
					min_ind_Values.put(ind_name, ind_value);
				}
				if (ind_value > max_ind_Values.get(ind_name)) {
					max_ind_Values.put(ind_name, ind_value);
				}
			} else {
				min_ind_Values.put(ind_name, ind_value);
				max_ind_Values.put(ind_name, ind_value);
				count_mes_ind.put(measure, count_mes_ind.get(measure) + 1);
			}

			/*
			 * Recuperation des valeurs mix et max de chaque mesure Pour creer des
			 * intervales egaux (tailles des barres) et convenables dans le gml pour
			 * l'affichage Bars
			 */

			if (min_mes_Values.containsKey(measure)) {
				if (ind_value < min_mes_Values.get(measure)) {
					min_mes_Values.put(measure, ind_value);
				}
				if (ind_value > max_mes_Values.get(measure) && ind_value != 99999) {

					max_mes_Values.put(measure, ind_value);

				}
			} else if (ind_value != 99999) {
				min_mes_Values.put(measure, ind_value);
				max_mes_Values.put(measure, ind_value);

			}
			ctEl++;
		}

		// Pour charger les geométries et faire des statistiques sur les données
		// max,min, count, count par zone spatiale, sum par zone spatiale SI NECESSAIRE

		// afin d'ajouter les mesures dans les même document JSON. Autrement dit, avoir
		// un attribut pour chaque mesure dans tous les 'features' au lieu d'ajouter
		// des nouveaux 'features' pour la deuxième mesure
		Map<String, Integer> features_id = new HashMap<String, Integer>();
		Map<String, Integer> features_measure = new HashMap<String, Integer>();

		int ct = 0; // COMPTEUR POUR COMPTER LE NOMBRE D'ELEMENT DEJA ECRIT DANS LE FICHIER

		Donnee_geo.get_donneesGeo_liste_JSON3(table, list_Level, zone_liste, connexion, geometry, count, countElem);

		ArrayList<String> liste_niveau = new ArrayList<String>();

		Donnee_geo.get_niveau(table, list_Level, zone_liste, connexion, liste_niveau);

		int l = 0;
		while (l < liste_niveau.size()) {
			l++;
		}

		list_ind_done = new ArrayList<String>();
		list_measure_done = new ArrayList<String>();

		// il faut vider la liste de mesures traitées
		list_measure_done.clear();
		list_ind_done.clear();

		// pour ajouter les données JSON
		int i = 0;
		int testNiveau = 0;
		double[] mes = null;
		String barGeoJson = "";
		String barGeoJsonRegion = "";
		JSONObject jsondata = null;

		/* A RECUPERER LA VALEUR POUR CREER UNE FONCTION */
		Map<String, String> list_region = new HashMap<String, String>();
		Map<String, Geometrie> list_dep_geometry = new HashMap<String, Geometrie>();
		Map<String, String> list_dep_ind = new HashMap<String, String>();
		int dep_ct = 0;

		for (Indicateur ind : liste_ind) {

			String str = ind.getNom();
			if (!list_ind_done.contains(str)) {
				String measure = "";
				measure = ind.getMeasure();
				if (!list_measure_done.contains(measure)) {
					// on initiale dis_type parce que il est possible que la mesure ne soit pas
					// décrite dans le fichier display_conf
					// pour qu'il ne garde pas l'affichage de la dernière mesure trouvée
					dis_type = "";

					for (Measure_Display m_d : liste_mesure_display) {
						if (m_d.getMeasureName().equals(measure)) {
							dis_type = m_d.getDisplayType();
							if (dis_type.toUpperCase().equals(("Bars").toUpperCase())) {
								bar_width = m_d.getBar_width();
								bar_min_high = m_d.getSize_min();
								bar_max_high = m_d.getSize_max();
								System.out.println("bar_width: " + bar_width + " bar_min_high: " + bar_min_high
										+ " bar_max_high: " + bar_max_high);
								// -bar_width parcque on va faire un shefting pour le premier indiquateur avant
								// de commencer
								if (shifting != 0.0) // pour ne pas faire un shefting pour la première mesure
									shifting = shifting + bar_width; // ce changement de la valeur de shifting a pour
																		// but de séparer les barres des différentes
																		// mesures.
							}
							break;
						}
					}
					list_measure_done.add(measure);
				}
			}

			String nomSpatial = ind.getSpatial();

			Geometrie geom1;

			// Si la geometrie a deja etait recuperee
			if (geometry.containsKey(nomSpatial)) {
				geom1 = geometry.get(nomSpatial);
			} else {
				geom1 = Donnee_geo.get_donneesGeo(table, list_Level, nomSpatial, connexion);
			}
			Iterator i1 = list_Level.iterator();
			while (i1.hasNext()) {
				Element courant = (Element) i1.next();
			}
			testNiveau = geometry.get(ind.getSpatial()).getCount(); // Vérification du nombre de niveau spatial

			if (geom1.getCentroid() != null) {
				String measure = "";

				measure = ind.getMeasure();

				// un type d'affichage qui n'est pas MultiMap est utilisé
				map0existe = true;

				String ind_name = ind.getNom();
				if (!list_ind_done.contains(ind_name)) {

					shifting = shifting + bar_width;
					list_ind_done.add(ind_name);
				}

				Indicateur idt = null;

				if (testNiveau > 1) {
					String reg = "";
					if (geometry.get(ind.getSpatial()).getNiveau().equals("departement")) {
						reg = Donnee_geo.get_Region(connexion, Base_Connexion.GetConfigBase(request, export),
								ind.getSpatial());
						Iterator<Indicateur> iterator = liste_ind.iterator();

						while (iterator.hasNext()) {
							idt = iterator.next();
							if (idt.getSpatial().equals(reg)) {

								break;

							}

						}
						list_region.put("" + dep_ct + "", reg);
						list_dep_geometry.put("" + dep_ct + "", geom1);
						list_dep_ind.put("" + dep_ct + "", ind_name);
						dep_ct++;

						barGeoJson = GML_SLD.getMultiBarCoordinate(geom1, ind, shifting, bar_width, bar_min_high,
								bar_max_high, max_mes_Values, 0, "");

						/*---------------------------------------------------------------------------------------------------*/

						jsondata = new JSONObject();
						jsondata.put("type", "Feature");

						JSONObject jsondata_properties = new JSONObject();
						jsondata_properties.put("ID", i + 1);

						// pour identifier le ID de feature
						String feature_identifiant = ind.getSpatial();

						// pour ajouter les membres des dimensions
						// On crée une List contenant tous les attribute "attribute(i)" de l'Element
						// racine
						List<String> dimesnions_membres = new ArrayList<String>();
						int j = 0;
						dimesnions_membres = ind.getAttributes();

						label = "";
						int ct_dim = 0;
						for (String dimension : liste_dimensions) {
							// pour identifier le ID de feature on ajouter les valeurs de différents membres
							// de dimensions (PAS de mesures)
							// si les valeurs sont identiques donc c'est le même feature. alors, il faut
							// ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans
							// le même feature
							if (!dimension.toLowerCase().equals("Measures".toLowerCase())) {
								feature_identifiant = feature_identifiant + "_" + dimesnions_membres.get(j);
								if (!dimension_value.contains(dimesnions_membres.get(j))) {
									dimension_value.add(dimesnions_membres.get(j));
								}
								if (ct_dim == 0) {
									label = dimesnions_membres.get(j);

									ct_dim++;
								} else {
									label = label + " + " + dimesnions_membres.get(j);

								}

								jsondata_properties.put(dimension, dimesnions_membres.get(j));
							}
							j++;
						}
						// fin d'ajout des memebres des dimensions
						int u = 0;
						while (u < liste_legende.size()) {

							if (liste_legende.get("" + u).equals(label)) {
								jsondata_properties.put("id_Legend", u);
								break;
							}

							u++;
						}

						if (label.length() == 0) {
							jsondata_properties.put("_Legend", ind.getMeasure());

						} else {
							jsondata_properties.put("_Legend", label);

						}

						jsondata_properties.put(ind.getMeasure(), ind.getValeur());
						jsondata_properties.put("_Location", ind.getSpatial());
						jsondata_properties.put("_niveau", geometry.get(ind.getSpatial()).getNiveau());
						jsondata_properties.put("_reg", reg);
						jsondata_properties.put("_ref", reg + "_" + ind.getSpatial());

						JSONObject jo = geometry.get(ind.getSpatial()).getCentroidJson();

						Iterator itr = jo.values().iterator();

						while (itr.hasNext()) {
							Object element = itr.next();

							if (element.toString().length() > 5) {

								String[] val = element.toString().split(",");

								float lon = Float.parseFloat(val[0].replace("[", ""));
								float lat = Float.parseFloat(val[1].replace("]", ""));

								jsondata_properties.put("long_centro", lon);
								jsondata_properties.put("lat_centro", lat);

							}
						}

						jsondata.put("properties", jsondata_properties);

						if (testNiveau > 1) {

							JSONParser jsonParser = new JSONParser();
							jsondata.put("geometry", jsonParser.parse(barGeoJson));
						} else {

							JSONParser jsonParser = new JSONParser();
							jsondata.put("geometry", jsonParser.parse(barGeoJson));
						}

						// pour identifier le ID de feature.
						// features_measure

						if (!features_id.containsKey(feature_identifiant)) {

							features_id.put(feature_identifiant, i);
							features.add(features_id.get(feature_identifiant), jsondata);

							i++;

						} else {
							int id = features_id.get(feature_identifiant);

							JSONObject old_feature = new JSONObject();
							old_feature = (JSONObject) features.get(id);
							JSONObject properties = new JSONObject();

							// add les mesures déjà trouvées
							properties.putAll(((JSONObject) old_feature.get("properties")));

							// add la nouvelle mesure
							properties.putAll(((JSONObject) jsondata.get("properties")));

							jsondata.put("properties", properties);
							features.set(id, jsondata);

						}

						/*---------------------------------------------------------------------------------------------------*/

					}

				} else {

					/* FOR NIV SPATIAL = 1 */
//					System.out.println("niveau spatial 1 valeur : " + i);
					double shth = separateBar(ind, liste_ind, sh);

					barGeoJson = GML_SLD.getBarCoordinate(geom1, ind, shifting, shth, bar_width, bar_min_high,
							bar_max_high, max_mes_Values);

					jsondata = new JSONObject();
					jsondata.put("type", "Feature");

					JSONObject jsondata_properties = new JSONObject();
					jsondata_properties.put("ID", i + 1);

					// pour identifier le ID de feature
					String feature_identifiant = ind.getSpatial();

					// pour ajouter les membres des dimensions
					// On crée une List contenant tous les attribute "attribute(i)" de l'Element
					// racine
					List<String> dimesnions_membres = new ArrayList<String>();
					int j = 0;
					dimesnions_membres = ind.getAttributes();
//					System.out.println("Liste Members " + dimesnions_membres);
//					System.out.println("Liste dimensions " + liste_dimensions);

					label = "";
					int ct_dim = 0;
					for (String dimension : liste_dimensions) {
						// pour identifier le ID de feature on ajouter les valeurs de différents membres
						// de dimensions (PAS de mesures)
						// si les valeurs sont identiques donc c'est le même feature. alors, il faut
						// ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans
						// le même feature
						if (!dimension.toLowerCase().equals("Measures".toLowerCase())) {
							feature_identifiant = feature_identifiant + "_" + dimesnions_membres.get(j);
							if (!dimension_value.contains(dimesnions_membres.get(j))) {
								dimension_value.add(dimesnions_membres.get(j));
							}

							if (j == 1)
								label = dimesnions_membres.get(j);
							else
								label = label + " + " + dimesnions_membres.get(j);
							ct_dim++;

//						else if (j != (liste_dimensions.size()-1)){
//							label = label+"_"+dimesnions_membres.get(j+1);
//							System.out.println(j +" " + dimesnions_membres.get(j+1) + " hak");
//
//						}
//						else {
//							System.out.println(j +" " + dimesnions_membres.get(1) + " hak000");
//							label = label+"_"+dimesnions_membres.get(1);
//
//						}
							jsondata_properties.put(dimension, dimesnions_membres.get(j));
						}
						j++;
					}

					// fin d'ajout des memebres des dimensions
					if (label.length() == 0) {
						jsondata_properties.put("_Legend", ind.getMeasure());

					} else {
						jsondata_properties.put("_Legend", label);

					}

					jsondata_properties.put(ind.getMeasure(), ind.getValeur());
					jsondata_properties.put("_Location", ind.getSpatial());
					jsondata_properties.put("_niveau", geometry.get(ind.getSpatial()).getNiveau());

					JSONObject jo = geometry.get(ind.getSpatial()).getCentroidJson();

					Iterator itr = jo.values().iterator();

					while (itr.hasNext()) {
						Object element = itr.next();

						if (element.toString().length() > 5) {

							String[] val = element.toString().split(",");

							float lon = Float.parseFloat(val[0].replace("[", ""));
							float lat = Float.parseFloat(val[1].replace("]", ""));

							jsondata_properties.put("long_centro", lon);
							jsondata_properties.put("lat_centro", lat);

						}
					}

					jsondata.put("properties", jsondata_properties);

					if (testNiveau > 1) {

						JSONParser jsonParser = new JSONParser();
						jsondata.put("geometry", jsonParser.parse(barGeoJson));

					} else {

						JSONParser jsonParser = new JSONParser();
						jsondata.put("geometry", jsonParser.parse(barGeoJson));
					}

					// pour identifier le ID de feature
					if (!features_measure.containsKey(feature_identifiant + "_" + ind.getMeasure())) {

						features_measure.put(feature_identifiant + "_" + ind.getMeasure(), i);
						features.add(features_measure.get(feature_identifiant + "_" + ind.getMeasure()), jsondata);
						i++;
					} else {

						int id = features_measure.get(feature_identifiant + "_" + ind.getMeasure());

						JSONObject old_feature = new JSONObject();
						old_feature = (JSONObject) features.get(id);
						JSONObject properties = new JSONObject();

						// add les mesures déjà trouvées
						properties.putAll(((JSONObject) old_feature.get("properties")));

						// add la nouvelle mesure
						properties.putAll(((JSONObject) jsondata.get("properties")));

						jsondata.put("properties", properties);
						features.set(id, jsondata);

					}

				}

			}

			ct += 1;

//		System.out.println(" INDICATEUR NAME : " + ind.getNom());
//		System.out.println(" INDICATEUR ATTRIBUTE0 : " + ind.getAttributes().get(0));
//		System.out.println(" INDICATEUR ATTRIBUTE1 : " + ind.getAttributes().get(1));
//		System.out.println(" INDICATEUR VALUE : " + ind.getValeur());
//		System.out.println(" INDICATEUR SPATIAL : " + ind.getSpatial());

		}

		// calcul range Equal initerval
		double[][] equInt = equalInterval(mes);

//		  geoJsonData
		System.out.println(" ECRITURE FICHIER BAR JSON " + niveau);
		if (date == 0) {

			if (export.contentEquals("VM")) {
				TEXT.savefile3(geojsonpath + "\\geoJSON_Bar_" + niveau + ".json",
						geoJsonData.toString().replace("}},", "}},\n")); // GENERATION FICHIER UNIQUE
			}
		}
		if (export.contentEquals("VM_local") || export.contentEquals("local")) {
			String filee = geoJSON_file + "_" + niveau + ".json";

			TEXT.savefile3(geoJSON_file + "_" + niveau + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
			// FICHIER
			// UNIQUE
		}

//			  TEXT.savefile3(geoJSON_file+"_"+niveau+".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION FICHIER UNIQUE

		if (date == 1) {
			TEXT.savefile3(geoJSON_file + "_" + dt + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																												// FICHIER
																												// AVEC
																												// LA
																												// DATE
																												// DANS
																												// LE
																												// NOM
		}

		System.out.println(" FIN ECRITURE FICHIER BAR JSON ");

		if (testNiveau > 1) {
			ecrireDonneesGeoJSONBarRegion(connexion, liste_ind, dis_type, list_region, list_dep_geometry, list_dep_ind,
					list_ind_done, list_measure_done, liste_mesure_display, export, hote, disc,geojsonpath,templatepath, liste_dimensions,
					features_id, date, geoJSON_file, dt, "region", liste_legende);
		}

	}

	@SuppressWarnings({ "unchecked", "unused" })
	private static void ecrireDonneesGeoJSONBarRegion(Connection connexion, List<Indicateur> liste_ind, String dis_type,
			Map<String, String> list_region, Map<String, Geometrie> list_dep_geometry, Map<String, String> list_dep_ind,
			List<String> list_ind_done, List<String> list_measure_done, List<Measure_Display> liste_mesure_display,
			String export, String hote, String disc,String geojsonpath,String templatepath, List<String> liste_dimensions, Map<String, Integer> features_id,
			int date, String geoJSON_file, String dt, String niveau, Map<String, String> liste_legende)
			throws ParseException, IOException {

		System.out.println(" Début bar region ");

		FileWriter file = null;
		if (export.contentEquals("VM")) {
			if (date == 0) {
//			file = new FileWriter( "E:\\Data Published\\geoJSON_Bar_region.json");	
//			file = new FileWriter( geoJSON_file+"_Bar.json");
			}
			if (date == 1) {
				file = new FileWriter(geojsonpath+"\\geoJSON" + "_" + dt + ".json"); // CREATION D'UN NOUVEAU
																								// FICHIER A CHAQUE FOIS
			}

		}

		JSONObject geoJsonData = new JSONObject();
		JSONObject crs = new JSONObject();
		JSONObject crs_properties = new JSONObject();
		crs_properties.put("name", "urn:ogc:def:crs:OGC:1.3:CRS84");
		// System.out.println("crs_properties:" + crs_properties);
		crs.put("type", "name");
		crs.put("properties", crs_properties);
		JSONArray features = new JSONArray();
		geoJsonData.put("type", "FeatureCollection");
		geoJsonData.put("name", "t");
		geoJsonData.put("crs", crs);
		geoJsonData.put("features", features);

		list_measure_done.clear();
		list_ind_done.clear();

		int ctEl = list_region.size();
		int ct = 0;
		int i = 0;
		String barGeoJson = "";
		String label = "";
		JSONObject jsondata = null;

		Double bar_width = 0.0;
		Double bar_min_high = 0.0;
		Double bar_max_high = 0.0;
		shifting = new Double(0.0);
		int shift_control = 0;

		// System.out.println("shifting : " + shifting);

		int y = 0;
		while (y < list_region.size()) {
			/* RECUPERER LA PREMIERE REGION AVEC SON INDICATEUR */
			System.out.println(" region : " + list_region.get("" + y) + " avec son departement "
					+ list_dep_geometry.get("" + y).getNom() + " avec indicateur " + list_dep_ind.get("" + y));
			Indicateur ind = null;
			for (Indicateur indt : liste_ind) {

				if (indt.getNom().equals(list_dep_ind.get("" + y))
						&& indt.getSpatial().equals(list_region.get("" + y))) {
					ind = indt;
//				  y++;
					break;
				}

			}
			/* METTRE TOUT LES TRAITEMENTS NECESSAIRE JUSQU'A LA CREATION DES BARS */

			System.out.println(" INDICATEUR VALUE : " + ind.getNom());
			System.out.println(" DEBUT SHIFT VALUE : " + shifting);

			String str = ind.getNom();
			if (!list_ind_done.contains(str)) {
//				System.out.println(" début recherche si nom est dans list_ind_done");
				String measure = "";
				// measure = get_measure(str, CreateIndicators.separator, mesure_position);
				measure = ind.getMeasure();
//				System.out.println(" mesure nom " + measure + " " + dis_type);
				if (!list_measure_done.contains(measure)) {
					// on initiale dis_type parce que il est possible que la mesure ne soit pas
					// décrite dans le fichier display_conf
					// pour qu'il ne garde pas l'affichage de la dernière mesure trouvée
					dis_type = "";

					for (Measure_Display m_d : liste_mesure_display) {
						if (m_d.getMeasureName().equals(measure)) {
							dis_type = m_d.getDisplayType();
							if (dis_type.toUpperCase().equals(("Bars").toUpperCase())) {
								bar_width = m_d.getBar_width();
								bar_min_high = m_d.getSize_min();
								bar_max_high = m_d.getSize_max();
//								shifting = shifting -bar_width;
								// -bar_width parcque on va faire un shefting pour le premier indiquateur avant
								// de commencer
//								System.out.println(" check for shifting " + bar_width);
//								System.out.println(" check shift value " + shifting);
								if (shifting != new Double(0.0) && shift_control != 0) { // pour ne pas faire un
																							// shefting pour la première
																							// mesure
									System.out.println(" control shift ");
									shifting = shifting + bar_width;
								} // ce changement de la valeur de shifting a pour but de séparer les barres des
									// différentes mesures.
							}

							break;
						}
					}

					list_measure_done.add(measure);
				}
			}

//			System.out.println(" --> Shifting : " + shifting);
			String nomSpatial = ind.getSpatial();

			Geometrie geom1 = list_dep_geometry.get("" + y);

			// Si la geometrie a deja etait recuperee

			if (geom1.getCentroid() != null) {
				String measure = "";
				// measure = get_measure(str, CreateIndicators.separator, mesure_position);
				measure = ind.getMeasure();

				String ind_name = ind.getNom();
				if (!list_ind_done.contains(ind_name)) {
//					System.out.println(" j entre tout le temps là ");
					shifting = shifting + bar_width;
					shift_control++;
					list_ind_done.add(ind_name);
				}

//				System.out.println(" --> Shifting direct : " + shifting);

				System.out.println(ind.getSpatial() + " niveau spatial centroid " + geom1.getCentroidJson().toString());
				Indicateur idt = null;

//				System.out.println(" Spatial value : " + );

				barGeoJson = GML_SLD.getMultiBarCoordinate(geom1, ind, shifting, bar_width, bar_min_high, bar_max_high,
						max_mes_Values, 0, "");

			}

//			---
			/* RAJOUTER UNE FONCTION POUR JSONDATA */

			jsondata = new JSONObject();
			jsondata.put("type", "Feature");

			JSONObject jsondata_properties = new JSONObject();
			jsondata_properties.put("ID", y);

			// pour identifier le ID de feature
			String feature_identifiant = ind.getSpatial();

			// pour ajouter les membres des dimensions
			// On crée une List contenant tous les attribute "attribute(i)" de l'Element
			// racine
			List<String> dimesnions_membres = new ArrayList<String>();
			int j = 0;
			dimesnions_membres = ind.getAttributes();

			label = "";
			int ct_dim = 0;
			ArrayList<String> nb_label = new ArrayList<String>();
			for (String dimension : liste_dimensions) {
				// pour identifier le ID de feature on ajouter les valeurs de différents membres
				// de dimensions (PAS de mesures)
				// si les valeurs sont identiques donc c'est le même feature. alors, il faut
				// ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans
				// le même feature
				if (!dimension.toLowerCase().equals("Measures".toLowerCase())) {
					feature_identifiant = feature_identifiant + "_" + dimesnions_membres.get(j);
					if (!dimension_value.contains(dimesnions_membres.get(j))) {
						dimension_value.add(dimesnions_membres.get(j));
					}
					if (ct_dim == 0) {
						label = dimesnions_membres.get(j);
						ct_dim++;
					} else {
						label = label + "_" + dimesnions_membres.get(j);
					}

					jsondata_properties.put(dimension, dimesnions_membres.get(j));
				}
				if (!nb_label.contains(label)) {
					nb_label.add(label);
				}
				j++;
			}
//			count_label = nb_label.size();
			// fin d'ajout des memebres des dimensions

			int u = 0;
			while (u < liste_legende.size()) {
//				  System.out.println(" legende : " + liste_legende.get(""+u));

				if (liste_legende.get("" + u).equals(label)) {
					jsondata_properties.put("id_Legend", u);
					break;
				}

				u++;
			}

			// jsondata_properties.put("Nom", ind.getNom());
			if (label.length() == 0) {
				jsondata_properties.put("_Legend", ind.getMeasure());
			} else {
				jsondata_properties.put("_Legend", label);
			}

//				jsondata_properties.put("_Legend", label);

//			jsondata_properties.put("_Legend", label);
			jsondata_properties.put(ind.getMeasure(), ind.getValeur());
			jsondata_properties.put("_Location", ind.getSpatial());
			jsondata_properties.put("_niveau", "region");
			jsondata_properties.put("_dep", geom1.getNom());
			jsondata_properties.put("_ref", ind.getSpatial() + "_" + geom1.getNom());

			JSONObject jo = geom1.getCentroidJson();

			Iterator itr = jo.values().iterator();

			while (itr.hasNext()) {
				Object element = itr.next();

				if (element.toString().length() > 5) {

					String[] val = element.toString().split(",");

					float lon = Float.parseFloat(val[0].replace("[", ""));
					float lat = Float.parseFloat(val[1].replace("]", ""));

					jsondata_properties.put("long_centro", lon);
					jsondata_properties.put("lat_centro", lat);

				}
			}

			jsondata.put("properties", jsondata_properties);

			JSONParser jsonParser = new JSONParser();
			jsondata.put("geometry", jsonParser.parse(barGeoJson));

			features_id.put(feature_identifiant, y);
			features.add(features_id.get(feature_identifiant), jsondata);
			// pour identifier le ID de feature
			if (!features_id.containsKey(feature_identifiant)) {
				features_id.put(feature_identifiant, i);
				features.add(features_id.get(feature_identifiant), jsondata);
				i++;
			} else {
				int id = features_id.get(feature_identifiant);
				JSONObject old_feature = new JSONObject();
				old_feature = (JSONObject) features.get(id);
				JSONObject properties = new JSONObject();

				// add les mesures déjà trouvées
				properties.putAll(((JSONObject) old_feature.get("properties")));

				// add la nouvelle mesure
				properties.putAll(((JSONObject) jsondata.get("properties")));

				jsondata.put("properties", properties);
				features.set(id, jsondata);

			}

			/* FIN RECUPERATION DU JSON DATA */

			ct += 1;

//			if (export.contentEquals("VM")) {
//				if (ctEl == 1) { // TEST SI IL N'Y A QU'UN ELEMENT DANS LA REPONSE DE LA REQUETE
//					TEXT.savefile4(file, geoJsonData.toString(), ct, ctEl);
//					geoJsonData = new JSONObject();
//				}
//				else {
//					if(ct == 1 && ctEl != 1) { // TEST SI C'EST LE PREMIER ELEMENT A ECRIRE ET QU'IL Y A PLUSIEURS ELEMENT DE REPONSE
//						TEXT.savefile4(file, geoJsonData.toString().replace("}]}", "},\n"), ct, ctEl);
//						geoJsonData = new JSONObject();
//					}
//					else if (ct == ctEl && ctEl != 1){ // TEST SI TOUS LES ELEMENTS ONT ETE ECRIT DANS LE FICHIER
//						TEXT.savefile4(file, jsondata.toString().replace("}}", "}}]}"), ct, ctEl);
//					}
//					else { // ECRIRE LES ELEMENTS DANS LE FICHIER
//						TEXT.savefile4(file, jsondata.toString().replace("}}", "}},\n"), ct, ctEl);
//			//			break;
//					}
//				}
//			}	
			System.out.println(" ---- ");

			/* FIN TOUT LES TRAITEMENTS NECESSAIRE JUSQU'A LA CREATION DES BARS */

			y++;
		}

		if (date == 0) {

			if (export.contentEquals("VM")) {
				TEXT.savefile3(geojsonpath + "\\geoJSON_Bar_" + niveau + ".json",
						geoJsonData.toString().replace("}},", "}},\n")); // GENERATION FICHIER UNIQUE
			}
			if (export.contentEquals("VM_local") || export.contentEquals("local")) {
				TEXT.savefile3(geoJSON_file + "_" + niveau + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																														// FICHIER
																														// UNIQUE
			}

//		  

		}
		if (date == 1) {
			TEXT.savefile3(geoJSON_file + "_" + dt + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																												// FICHIER
																												// AVEC
																												// LA
																												// DATE
																												// DANS
																												// LE
																												// NOM
		}

		System.out.println(" nb in list : " + list_region.size());
//	  System.out.println(" key list region" + list_region.get);
		System.out.println("region list : " + list_region.get("0"));

		System.out.println(
				" region : " + list_region.get("0") + " avec son departement " + list_dep_geometry.get("0").getNom());

	}

	@SuppressWarnings("unchecked")
	private static void ecrireDonneesGeoJSONBarMultiMeasure(HttpServletRequest request, List<String> liste_dimensions,
			List<Indicateur> liste_ind, List<Measure_Display> liste_mesure_display, String geoJSON_file, String export,
			Double sh, String hote, String disc,String geojsonpath,String templatepath, String dt, int date, String niveau, String measure_used,
			Map<String, String> liste_legende) throws IOException, ParseException, SQLException {

		FileWriter file = null;
		if (export.contentEquals("VM")) {
			if (date == 0) {
//			file = new FileWriter( "E:\\Data Published\\geoJSON_Bar_"+niveau+".json");
				file = new FileWriter(geojsonpath + "\\geoJSON_Bar_"
						+ measure_used.replace(":", "_").replace(" ", "_") + "_" + niveau + ".json");
//			file = new FileWriter( geoJSON_file+"_"+niveau+".json");

			}
			if (date == 1) {
				file = new FileWriter(geojsonpath + "\\geoJSON" + "_" + dt + ".json"); // CREATION D'UN NOUVEAU
																								// FICHIER A CHAQUE FOIS
			}

		}

		if (export.contentEquals("VM_local")) {
			if (date == 0) {
//			file = new FileWriter( "E:\\Data Published\\geoJSON_Bar_"+niveau+".json");	
				file = new FileWriter(geoJSON_file + "_" + niveau + ".json");

			}
			if (date == 1) {
				file = new FileWriter(geojsonpath + "\\geoJSON" + "_" + dt + ".json"); // CREATION D'UN NOUVEAU
																								// FICHIER A CHAQUE FOIS
			}

		}

//	---
		Integer cpt = 0;
//	shifting = 0.0;

		Double shift_val = new Double(0.0);

		// cette variable est utilisÃ©e pour indiquer si il y az une mesure n'utilise
		// pas un type d'affichage multicarte
		boolean map0existe = false;

		List<Element> liste_root_map = new ArrayList<Element>();

		max_ind_Values = new HashMap<String, Double>();
		min_ind_Values = new HashMap<String, Double>();
		count_mes_ind = new HashMap<String, Double>();
		count_map_by_type = new HashMap<String, Integer>();

		max_mes_Values = new HashMap<String, Double>();
		min_mes_Values = new HashMap<String, Double>();

		sum_mes_sp_Values = new HashMap<String, Double>();
		sum_mes_sp_Values_curr = new HashMap<String, Double>();

		// pour creer les bars et dÃ©placer les bars avec chaque indiquateur
		//////////////////////////////////////
		Double bar_width = 0.0;
		Double bar_min_high = 0.0;
		Double bar_max_high = 0.0;

		List<String> list_ind_done = new ArrayList<String>();
		List<String> list_measure_done = new ArrayList<String>();

//	---

		HashMap<String, Geometrie> geometry = new HashMap<String, Geometrie>();
		// d'abord on prÃ©pare la strucutre de fichier JSON
		JSONObject geoJsonData = new JSONObject();
		JSONObject crs = new JSONObject();
		JSONObject crs_properties = new JSONObject();
		crs_properties.put("name", "urn:ogc:def:crs:OGC:1.3:CRS84");
		// System.out.println("crs_properties:" + crs_properties);
		crs.put("type", "name");
		crs.put("properties", crs_properties);
		JSONArray features = new JSONArray();
		geoJsonData.put("type", "FeatureCollection");
		geoJsonData.put("name", "t");
		geoJsonData.put("crs", crs);
		geoJsonData.put("features", features);

		// charger les informations nÃ©cessaires pour trouver les gÃ©ometries
		Document documentConfig = Base_Connexion.GetConfigBase(request, export);
		Element rootgeo = documentConfig.getRootElement();
		String table = rootgeo.getChildText("Table");
		Element levels = rootgeo.getChild("Levels");
		List list_Level = levels.getChildren("Level");
		Connection connexion = null;

		try {
			connexion = Base_Connexion.connexionBase(request, export);
		} catch (ClassNotFoundException e) {
			//
			e.printStackTrace();
		} catch (SQLException e) {
			//
			e.printStackTrace();
		}
		//////////////// Fin pour la charge des gÃ©ometries

//	---
		String zone_liste = null;
		String dis_type = "";
		int ctEl = 0;
		String label = "";

//	MultiMap<String, String> map = new MultiValueMap<>();
		for (Indicateur ind : liste_ind) {

			String measure = "";

			String ind_name = ind.getNom();
			double ind_value = ind.getValeur();
			String ind_spatial = ind.getSpatial();
			measure = ind.getMeasure();

			// pour charger les gÃ©omÃ©tries des toutes les zones spatiales en une fois
			if (!geometry.containsKey(ind_spatial)) {
				if (zone_liste == null) {
					zone_liste = "'" + ind_spatial + "'";
				} else {
					zone_liste = zone_liste + ", '" + ind_spatial + "'";
				}
			}
			/////////////////////////////////////////////

			if (!list_measure_done.contains(measure)) {
				// on initiale (0) le nombre d'indiquateurs pour chaque mesure Ã  afficher dans
				// chaque zone spatial
				count_mes_ind.put(measure, 0.0);

				// on initiale dis_type parce que il est possible que la mesure ne soit pas
				// dÃ©crite dans le fichier display_conf
				// pour qu'il ne garde pas l'affichage de la derniÃ¨re mesure trouvÃ©e
				System.out.println(" measure equals :: " + measure);
				dis_type = "";
				for (Measure_Display m_d : liste_mesure_display) {
//				System.out.println(" find for mesure display => " + m_d.getMeasureName() + " affichage " + m_d.getDisplayType());
					if (m_d.getMeasureName().equals(measure)) {
						dis_type = m_d.getDisplayType();
//					System.out.println(" ps : " + m_d.getDisplayType());
						break;
					}
				}
				list_measure_done.add(measure);
			}

			/*
			 * Recuperation des valeurs min et max de chaque indicateur et count
			 * d'indicateur pour chaque mesure Pour crÃ©er des intervalles Ã©gaux et
			 * convenables dans le sdl
			 */
			if (min_ind_Values.containsKey(ind_name)) {
				if (ind_value < min_ind_Values.get(ind_name)) {
					min_ind_Values.put(ind_name, ind_value);
				}
				if (ind_value > max_ind_Values.get(ind_name)) {
					max_ind_Values.put(ind_name, ind_value);
				}
			} else {
				min_ind_Values.put(ind_name, ind_value);
				max_ind_Values.put(ind_name, ind_value);
				count_mes_ind.put(measure, count_mes_ind.get(measure) + 1);
			}
//		System.out.println(measure + ":"+count_mes_ind.get(measure)); 

			/*
			 * Recuperation des valeurs mix et max de chaque mesure Pour creer des
			 * intervales egaux (tailles des barres) et convenables dans le gml pour
			 * l'affichage Bars
			 */

			if (min_mes_Values.containsKey(measure)) {
				if (ind_value < min_mes_Values.get(measure)) {
					min_mes_Values.put(measure, ind_value);
				}
				if (ind_value > max_mes_Values.get(measure) && ind_value != 99999) {
					max_mes_Values.put(measure, ind_value);
				}
			} else if (ind_value != 99999) {
				min_mes_Values.put(measure, ind_value);
				max_mes_Values.put(measure, ind_value);
			}
			ctEl++;
		}

//	---

		// Pour charger les geomÃ©tries et faire des statistiques sur les donnÃ©es
		// max,min, count, count par zone spatiale, sum par zone spatiale SI NECESSAIRE
//	String zone_liste = null;

		// afin d'ajouter les mesures dans les mÃªme document JSON. Autrement dit, avoir
		// un attribut pour chaque mesure dans tous les 'features' au lieu d'ajouter
		// des nouveaux 'features' pour la deuxiÃ¨me mesure
		Map<String, Integer> features_id = new HashMap<String, Integer>();
		Map<String, Integer> features_measure = new HashMap<String, Integer>();

		// String dis_type="";

		int ct = 0; // COMPTEUR POUR COMPTER LE NOMBRE D'ELEMENT DEJA ECRIT DANS LE FICHIER

		Donnee_geo.get_donneesGeo_liste_JSON3(table, list_Level, zone_liste, connexion, geometry, count, countElem);

		ArrayList<String> liste_niveau = new ArrayList<String>();

		Donnee_geo.get_niveau(table, list_Level, zone_liste, connexion, liste_niveau);

		int l = 0;
		while (l < liste_niveau.size()) {
			System.out.println(liste_niveau.get(l));
			l++;
		}

		list_ind_done = new ArrayList<String>();
		list_measure_done = new ArrayList<String>();

		// il faut vider la liste de mesures traitÃ©es
		list_measure_done.clear();
		list_ind_done.clear();

//	int ctEl = 0;

//	if (export.contentEquals("VM")) {
//		ctEl = zone_liste.length(); // RECUPERATION DU NOMBRE D'ELEMENT RETOURNEE PAR LA REQUETE
//	}
//	if (export.contentEquals("local")) {
//		ctEl = Donnee_geo.get_nbElement(table, list_Level, zone_liste, connexion, ct, ct); // RECUPERATION DU NOMBRE D'ELEMENT RETOURNEE PAR LA REQUETE
//	}

//	ctEl = zone_liste.length();

//	System.out.println("nb element :"+ctEl);

		// pour ajouter les donnÃ©es JSON
		int i = 0;
//	int t = 0;
		int testNiveau = 0;
		double[] mes = null;
//	List<String>dimension_value = new ArrayList<String>();
		String barGeoJson = "";
		String barGeoJsonRegion = "";
		JSONObject jsondata = null;

		/* A RECUPERER LA VALEUR POUR CREER UNE FONCTION */
		Map<String, String> list_region = new HashMap<String, String>();
		Map<String, Geometrie> list_dep_geometry = new HashMap<String, Geometrie>();
		Map<String, String> list_dep_ind = new HashMap<String, String>();
		int dep_ct = 0;
		for (Indicateur ind : liste_ind) {

			if (ind.getMeasure().equals(measure_used)) {
				String str = ind.getNom();
				if (!list_ind_done.contains(str)) {

					String measure = "";
					measure = ind.getMeasure();
					if (!list_measure_done.contains(measure)) {
						// on initiale dis_type parce que il est possible que la mesure ne soit pas
						// dÃ©crite dans le fichier display_conf
						// pour qu'il ne garde pas l'affichage de la derniÃ¨re mesure trouvÃ©e
						dis_type = "";

						for (Measure_Display m_d : liste_mesure_display) {

							if (m_d.getMeasureName().equals(measure)) {
								dis_type = m_d.getDisplayType();
								if (dis_type.toUpperCase().equals(("Bars").toUpperCase())) {
									bar_width = m_d.getBar_width();
									bar_min_high = m_d.getSize_min();
									bar_max_high = m_d.getSize_max();

									if (shifting != 0.0) // pour ne pas faire un shefting pour la premiÃ¨re mesure

										shifting = shifting + bar_width; // ce changement de la valeur de shifting a
																			// pour but de sÃ©parer les barres des
																			// diffÃ©rentes mesures.
									shift_val = shifting;
								}

								break;
							}
						}

						list_measure_done.add(measure);
					}
				}

				String nomSpatial = ind.getSpatial();

				Geometrie geom1;

				// Si la geometrie a deja etait recuperee
				if (geometry.containsKey(nomSpatial)) {
					geom1 = geometry.get(nomSpatial);
				} else {
					geom1 = Donnee_geo.get_donneesGeo(table, list_Level, nomSpatial, connexion);

				}

				testNiveau = geometry.get(ind.getSpatial()).getCount(); // VÃ©rification du nombre de niveau spatial

				if (geom1.getCentroid() != null) {
					String measure = "";

					measure = ind.getMeasure();

					// un type d'affichage qui n'est pas MultiMap est utilisÃ©
					map0existe = true;

					String ind_name = ind.getNom();
					if (!list_ind_done.contains(ind_name)) {
//					System.out.println(" ici je teste ");
						shifting = shifting + bar_width;
						list_ind_done.add(ind_name);
					}

					Indicateur idt = null;

					if (testNiveau > 1) {
						String reg = "";
						if (geometry.get(ind.getSpatial()).getNiveau().equals("departement")) {
							reg = Donnee_geo.get_Region(connexion, Base_Connexion.GetConfigBase(request, export),
									ind.getSpatial());

							Iterator<Indicateur> iterator = liste_ind.iterator();

							while (iterator.hasNext()) {
								idt = iterator.next();
								if (idt.getSpatial().equals(reg)) {

									break;

								}

							}
							list_region.put("" + dep_ct + "", reg);
							list_dep_geometry.put("" + dep_ct + "", geom1);
							list_dep_ind.put("" + dep_ct + "", ind_name);
							dep_ct++;

							barGeoJson = GML_SLD.getMultiBarCoordinate(geom1, ind, shifting, bar_width, bar_min_high,
									bar_max_high, max_mes_Values, 0, "");

							/*---------------------------------------------------------------------------------------------------*/

							jsondata = new JSONObject();
							jsondata.put("type", "Feature");

							JSONObject jsondata_properties = new JSONObject();
							jsondata_properties.put("ID", i + 1);

							// pour identifier le ID de feature
							String feature_identifiant = ind.getSpatial();

							// pour ajouter les membres des dimensions
							// On crÃ©e une List contenant tous les attribute "attribute(i)" de l'Element
							// racine
							List<String> dimesnions_membres = new ArrayList<String>();
							int j = 0;
							dimesnions_membres = ind.getAttributes();

							label = "";
							int ct_dim = 0;
							for (String dimension : liste_dimensions) {
								// pour identifier le ID de feature on ajouter les valeurs de diffÃ©rents
								// membres de dimensions (PAS de mesures)
								// si les valeurs sont identiques donc c'est le mÃªme feature. alors, il faut
								// ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans
								// le mÃªme feature
								if (!dimension.toLowerCase().equals("Measures".toLowerCase())) {
									feature_identifiant = feature_identifiant + "_" + dimesnions_membres.get(j);
									if (!dimension_value.contains(dimesnions_membres.get(j))) {
										dimension_value.add(dimesnions_membres.get(j));
									}
									if (ct_dim == 0) {
										label = dimesnions_membres.get(j);
										ct_dim++;
									} else {
										label = label + "+" + dimesnions_membres.get(j);
									}

									jsondata_properties.put(dimension, dimesnions_membres.get(j));
								}
								j++;
							}
							// fin d'ajout des memebres des dimensions
							// jsondata_properties.put("Nom", ind.getNom());

							System.out.println(" BRRRRRR " + label.length() + " OR " + ind.getMeasure());

							if (label.length() == 0) {
								jsondata_properties.put("_Legend", ind.getMeasure());
							} else {
								jsondata_properties.put("_Legend", label);
							}

//						jsondata_properties.put("_Legend", label);
							jsondata_properties.put(ind.getMeasure(), ind.getValeur());
							jsondata_properties.put("_Location", ind.getSpatial());
							jsondata_properties.put("_niveau", geometry.get(ind.getSpatial()).getNiveau());
							jsondata_properties.put("_reg", reg);
							jsondata_properties.put("_ref", reg + "_" + ind.getSpatial());

							JSONObject jo = geometry.get(ind.getSpatial()).getCentroidJson();

							Iterator itr = jo.values().iterator();

							while (itr.hasNext()) {
								Object element = itr.next();

								if (element.toString().length() > 5) {

									String[] val = element.toString().split(",");

									float lon = Float.parseFloat(val[0].replace("[", ""));
									float lat = Float.parseFloat(val[1].replace("]", ""));

									jsondata_properties.put("long_centro", lon);
									jsondata_properties.put("lat_centro", lat);

								}
							}

							jsondata.put("properties", jsondata_properties);

							if (testNiveau > 1) {

								JSONParser jsonParser = new JSONParser();
								jsondata.put("geometry", jsonParser.parse(barGeoJson));
							} else {

								JSONParser jsonParser = new JSONParser();
								jsondata.put("geometry", jsonParser.parse(barGeoJson));
							}

							// pour identifier le ID de feature.
							// features_measure

//						if (! features_id.containsKey(feature_identifiant)) {
							if (!features_measure.containsKey(feature_identifiant + "_" + ind.getMeasure())) {

//							features_id.put(feature_identifiant, i);
//							features.add(features_id.get(feature_identifiant), jsondata);

								features_measure.put(feature_identifiant + "_" + ind.getMeasure(), i);
								features.add(features_measure.get(feature_identifiant + "_" + ind.getMeasure()),
										jsondata);
								i++;

							} else {
//							int id = features_id.get(feature_identifiant);

								int id = features_measure.get(feature_identifiant + "_" + ind.getMeasure());

								JSONObject old_feature = new JSONObject();
								old_feature = (JSONObject) features.get(id);
								JSONObject properties = new JSONObject();

								// add les mesures déj  trouvÃ©es
								properties.putAll(((JSONObject) old_feature.get("properties")));

								// add la nouvelle mesure
								properties.putAll(((JSONObject) jsondata.get("properties")));

								jsondata.put("properties", properties);
								features.set(id, jsondata);

							}

							/*---------------------------------------------------------------------------------------------------*/

						}

					} else {
						double shth = separateBar(ind, liste_ind, sh);

						/* FOR NIV SPATIAL = 1 */
//						System.out.println("niveau spatial 1 valeur : " + i);
						barGeoJson = GML_SLD.getBarCoordinate(geom1, ind, shifting, shth, bar_width, bar_min_high,
								bar_max_high, max_mes_Values);

						jsondata = new JSONObject();
						jsondata.put("type", "Feature");

						JSONObject jsondata_properties = new JSONObject();
						jsondata_properties.put("ID", i + 1);

						// pour identifier le ID de feature
						String feature_identifiant = ind.getSpatial();

						// pour ajouter les membres des dimensions
						// On crÃ©e une List contenant tous les attribute "attribute(i)" de l'Element
						// racine
						List<String> dimesnions_membres = new ArrayList<String>();
						int j = 0;
						dimesnions_membres = ind.getAttributes();

						label = "";
						int ct_dim = 0;
						for (String dimension : liste_dimensions) {
							// pour identifier le ID de feature on ajouter les valeurs de diffÃ©rents
							// membres de dimensions (PAS de mesures)
							// si les valeurs sont identiques donc c'est le mÃªme feature. alors, il faut
							// ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans
							// le mÃªme feature
							if (!dimension.toLowerCase().equals("Measures".toLowerCase())) {
								feature_identifiant = feature_identifiant + "_" + dimesnions_membres.get(j);
								if (!dimension_value.contains(dimesnions_membres.get(j))) {
									dimension_value.add(dimesnions_membres.get(j));
								}
								if (ct_dim == 0) {
									label = dimesnions_membres.get(j);
									ct_dim++;
								} else {
									label = label + "+" + dimesnions_membres.get(j);
								}

								jsondata_properties.put(dimension, dimesnions_membres.get(j));
							}
							j++;
						}
						// fin d'ajout des memebres des dimensions
						// jsondata_properties.put("Nom", ind.getNom());

						if (label.length() == 0) {
							jsondata_properties.put("_Legend", ind.getMeasure());
						} else {
							jsondata_properties.put("_Legend", label);
						}

//					jsondata_properties.put("_Legend", label);
//					jsondata_properties.put("_Legend", label);
						jsondata_properties.put(ind.getMeasure(), ind.getValeur());
						jsondata_properties.put("_Location", ind.getSpatial());
						jsondata_properties.put("_niveau", geometry.get(ind.getSpatial()).getNiveau());
//					jsondata_properties.put("_reg", reg);

						JSONObject jo = geometry.get(ind.getSpatial()).getCentroidJson();

						Iterator itr = jo.values().iterator();

						while (itr.hasNext()) {
							Object element = itr.next();

							if (element.toString().length() > 5) {

								String[] val = element.toString().split(",");

								float lon = Float.parseFloat(val[0].replace("[", ""));
								float lat = Float.parseFloat(val[1].replace("]", ""));

								jsondata_properties.put("long_centro", lon);
								jsondata_properties.put("lat_centro", lat);

							}
						}

						jsondata.put("properties", jsondata_properties);

						if (testNiveau > 1) {

							JSONParser jsonParser = new JSONParser();
							jsondata.put("geometry", jsonParser.parse(barGeoJson));

						} else {

							JSONParser jsonParser = new JSONParser();
							jsondata.put("geometry", jsonParser.parse(barGeoJson));
						}

						// pour identifier le ID de feature
//					if (! features_id.containsKey(feature_identifiant)) {
						if (!features_measure.containsKey(feature_identifiant + "_" + ind.getMeasure())) {

//						features_id.put(feature_identifiant, i);
//						features.add(features_id.get(feature_identifiant), jsondata);

							features_measure.put(feature_identifiant + "_" + ind.getMeasure(), i);
							features.add(features_measure.get(feature_identifiant + "_" + ind.getMeasure()), jsondata);
							i++;
						} else {
//						int id = features_id.get(feature_identifiant);

							int id = features_measure.get(feature_identifiant + "_" + ind.getMeasure());

							JSONObject old_feature = new JSONObject();
							old_feature = (JSONObject) features.get(id);
							JSONObject properties = new JSONObject();

							// add les mesures dÃ©jÃ  trouvÃ©es
							properties.putAll(((JSONObject) old_feature.get("properties")));

							// add la nouvelle mesure
							properties.putAll(((JSONObject) jsondata.get("properties")));

							jsondata.put("properties", properties);
							features.set(id, jsondata);

						}

					}

//				System.out.println(" by getBarCoordinate = " + barGeoJson);
				}

				ct += 1;

//			if (export.contentEquals("VM")) {
//				if (ctEl == 1) { // TEST SI IL N'Y A QU'UN ELEMENT DANS LA REPONSE DE LA REQUETE
//					TEXT.savefile4(file, geoJsonData.toString(), ct, ctEl);
//					geoJsonData = new JSONObject();
//				}
//				else {
//					if(ct == 1 && ctEl != 1) { // TEST SI C'EST LE PREMIER ELEMENT A ECRIRE ET QU'IL Y A PLUSIEURS ELEMENT DE REPONSE
//						TEXT.savefile4(file, geoJsonData.toString().replace("}]}", "},\n"), ct, ctEl);
//						geoJsonData = new JSONObject();
//					}
//					else if (ct == ctEl && ctEl != 1){ // TEST SI TOUS LES ELEMENTS ONT ETE ECRIT DANS LE FICHIER
//						TEXT.savefile4(file, jsondata.toString().replace("}}", "}}]}"), ct, ctEl);
//					}
//					else { // ECRIRE LES ELEMENTS DANS LE FICHIER
//						TEXT.savefile4(file, jsondata.toString().replace("}}", "}},\n"), ct, ctEl);
//			//			break;
//					}
//				}
//			}	
//			System.out.println(" ---- ");

//			System.out.println(" INDICATEUR NAME : " + ind.getNom());
//			System.out.println(" INDICATEUR ATTRIBUTE0 : " + ind.getAttributes().get(0));
//			System.out.println(" INDICATEUR ATTRIBUTE1 : " + ind.getAttributes().get(1));
//			System.out.println(" INDICATEUR VALUE : " + ind.getValeur());
//			System.out.println(" INDICATEUR SPATIAL : " + ind.getSpatial());

//			System.out.println();
			}

		}

		// calcul range Equal initerval
		double[][] equInt = equalInterval(mes);

//		  geoJsonData
//			 System.out.println(" ECRITURE FICHIER BAR JSON " + niveau);

		if (date == 0) {

			if (export.contentEquals("VM")) {
//				  file = new FileWriter( "E:\\Data Published\\geoJSON_Bar_"+measure_used.replace(":", "_").replace(" ", "_")+"_"+niveau+".json");	
//				  TEXT.savefile3("E:\\Data Published\\geoJSON_Bar_"+niveau+".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION FICHIER UNIQUE
				TEXT.savefile3(geojsonpath + "\\geoJSON_Bar_"
						+ measure_used.replace(":", "_").replace(" ", "_") + "_" + niveau + ".json",
						geoJsonData.toString().replace("}},", "}},\n")); // GENERATION FICHIER
																			// UNIQUE
			}
		}
		if (export.contentEquals("VM_local") || export.contentEquals("local")) {
			TEXT.savefile3(geoJSON_file + "_" + niveau + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																													// FICHIER
																													// UNIQUE
		}

//			  TEXT.savefile3(geoJSON_file+"_"+niveau+".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION FICHIER UNIQUE

		if (date == 1) {
			TEXT.savefile3(geoJSON_file + "_" + dt + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																												// FICHIER
																												// AVEC
																												// LA
																												// DATE
																												// DANS
																												// LE
																												// NOM
		}

//		  System.out.println(" FIN ECRITURE FICHIER BAR JSON ");

//	  System.out.println(" nb in list region : " + list_region.size());
//	  System.out.println(" nb in list dep geometry : " + list_dep_geometry.size());
//	  System.out.println(" nb in list indicateur : " + list_dep_ind.size());
//	  System.out.println(" key list region" + list_region.get);
//	  System.out.println("region list : " + list_region.get("0"));

//	  int y = 0;
//	  while (y < list_region.size()) {
//		  System.out.println(" region : " + list_region.get(""+y) + " avec son departement " + list_dep_geometry.get(""+y).getNom() + " avec indicateur "+list_dep_ind.get(""+y));
//		  y++;
//	  }
//	  System.out.println(" last y value " + (y-1));

		if (testNiveau > 1) {
			ecrireDonneesGeoJSONBarMultiMeasureRegion(connexion, liste_ind, dis_type, list_region, list_dep_geometry,
					list_dep_ind, list_ind_done, list_measure_done, liste_mesure_display, export, hote, disc,geojsonpath,templatepath,
					liste_dimensions, features_id, date, geoJSON_file, dt, "region", shift_val, measure_used);
		}

	}

	@SuppressWarnings({ "unchecked", "unused" })
	private static void ecrireDonneesGeoJSONBarMultiMeasureRegion(Connection connexion, List<Indicateur> liste_ind,
			String dis_type, Map<String, String> list_region, Map<String, Geometrie> list_dep_geometry,
			Map<String, String> list_dep_ind, List<String> list_ind_done, List<String> list_measure_done,
			List<Measure_Display> liste_mesure_display, String export, String hote, String disc,String geojsonpath,String templatepath,
			List<String> liste_dimensions, Map<String, Integer> features_id, int date, String geoJSON_file, String dt,
			String niveau, double shift_val, String measure_used) throws ParseException, IOException {

		System.out.println(" DÃ©but bar region ");

		FileWriter file = null;
		if (export.contentEquals("VM")) {
			if (date == 0) {
//			file = new FileWriter( "E:\\Data Published\\geoJSON_Bar_region.json");	
//			file = new FileWriter( geoJSON_file+"_Bar.json");
			}
			if (date == 1) {
				file = new FileWriter(geojsonpath + "\\geoJSON" + "_" + dt + ".json"); // CREATION D'UN NOUVEAU
																								// FICHIER A CHAQUE FOIS
			}

		}

		JSONObject geoJsonData = new JSONObject();
		JSONObject crs = new JSONObject();
		JSONObject crs_properties = new JSONObject();
		crs_properties.put("name", "urn:ogc:def:crs:OGC:1.3:CRS84");
		// System.out.println("crs_properties:" + crs_properties);
		crs.put("type", "name");
		crs.put("properties", crs_properties);
		JSONArray features = new JSONArray();
		geoJsonData.put("type", "FeatureCollection");
		geoJsonData.put("name", "t");
		geoJsonData.put("crs", crs);
		geoJsonData.put("features", features);

		list_measure_done.clear();
		list_ind_done.clear();

		int ctEl = list_region.size();
		int ct = 0;
		int i = 0;
		String barGeoJson = "";
		String label = "";
		JSONObject jsondata = null;

		Double bar_width = 0.0;
		Double bar_min_high = 0.0;
		Double bar_max_high = 0.0;
		Double shift = shift_val;
		int shift_control = 0;

		System.out.println("shift : " + shift);

		int y = 0;
		while (y < list_region.size()) {
			/* RECUPERER LA PREMIERE REGION AVEC SON INDICATEUR */
			System.out.println(" region : " + list_region.get("" + y) + " avec son departement "
					+ list_dep_geometry.get("" + y).getNom() + " avec indicateur " + list_dep_ind.get("" + y));
			Indicateur ind = null;
			for (Indicateur indt : liste_ind) {

				if (indt.getNom().equals(list_dep_ind.get("" + y))
						&& indt.getSpatial().equals(list_region.get("" + y))) {
					ind = indt;
//				  y++;
					break;
				}

			}
			/* METTRE TOUT LES TRAITEMENTS NECESSAIRE JUSQU'A LA CREATION DES BARS */

			System.out.println(" INDICATEUR VALUE : " + ind.getNom());
			System.out.println(" DEBUT SHIFT VALUE : " + shift);

			String str = ind.getNom();
			if (!list_ind_done.contains(str)) {
//				System.out.println(" dÃ©but recherche si nom est dans list_ind_done");
				String measure = "";
				// measure = get_measure(str, CreateIndicators.separator, mesure_position);
				measure = ind.getMeasure();
//				System.out.println(" mesure nom " + measure + " " + dis_type);
				if (!list_measure_done.contains(measure)) {
					// on initiale dis_type parce que il est possible que la mesure ne soit pas
					// dÃ©crite dans le fichier display_conf
					// pour qu'il ne garde pas l'affichage de la derniÃ¨re mesure trouvÃ©e
					dis_type = "";

					for (Measure_Display m_d : liste_mesure_display) {
						if (m_d.getMeasureName().equals(measure)) {
							dis_type = m_d.getDisplayType();
							if (dis_type.toUpperCase().equals(("Bars").toUpperCase())) {
								bar_width = m_d.getBar_width();
								bar_min_high = m_d.getSize_min();
								bar_max_high = m_d.getSize_max();
//								shifting = shifting -bar_width;
								// -bar_width parcque on va faire un shefting pour le premier indiquateur avant
								// de commencer
//								System.out.println(" check for shifting " + bar_width);
//								System.out.println(" check shift value " + shifting);
								if (shift != new Double(0.0) && shift_control != 0) { // pour ne pas faire un shefting
																						// pour la premiÃ¨re mesure
									System.out.println(" control shift ");
									shift = shift + bar_width;
								} // ce changement de la valeur de shifting a pour but de sÃ©parer les barres des
									// diffÃ©rentes mesures.
							}

							break;
						}
					}

					list_measure_done.add(measure);
				}
			}

//			System.out.println(" --> Shifting : " + shifting);
			String nomSpatial = ind.getSpatial();

			Geometrie geom1 = list_dep_geometry.get("" + y);

			// Si la geometrie a deja etait recuperee

			if (geom1.getCentroid() != null) {
				String measure = "";
				// measure = get_measure(str, CreateIndicators.separator, mesure_position);
				measure = ind.getMeasure();

				String ind_name = ind.getNom();
				if (!list_ind_done.contains(ind_name)) {
//					System.out.println(" j entre tout le temps lÃ  ");
					shift = shift + bar_width;
					shift_control++;
					list_ind_done.add(ind_name);
				}

//				System.out.println(" --> Shifting direct : " + shifting);

				System.out.println(ind.getSpatial() + " niveau spatial centroid " + geom1.getCentroidJson().toString());
				Indicateur idt = null;

//				System.out.println(" Spatial value : " + );

				barGeoJson = GML_SLD.getMultiBarCoordinate(geom1, ind, shift, bar_width, bar_min_high, bar_max_high,
						max_mes_Values, 0, "");

			}

//			---
			/* RAJOUTER UNE FONCTION POUR JSONDATA */

			jsondata = new JSONObject();
			jsondata.put("type", "Feature");

			JSONObject jsondata_properties = new JSONObject();
			jsondata_properties.put("ID", y);

			// pour identifier le ID de feature
			String feature_identifiant = ind.getSpatial();

			// pour ajouter les membres des dimensions
			// On crÃ©e une List contenant tous les attribute "attribute(i)" de l'Element
			// racine
			List<String> dimesnions_membres = new ArrayList<String>();
			int j = 0;
			dimesnions_membres = ind.getAttributes();

			label = "";
			int ct_dim = 0;
			ArrayList<String> nb_label = new ArrayList<String>();
			for (String dimension : liste_dimensions) {
				// pour identifier le ID de feature on ajouter les valeurs de diffÃ©rents
				// membres de dimensions (PAS de mesures)
				// si les valeurs sont identiques donc c'est le mÃªme feature. alors, il faut
				// ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans
				// le mÃªme feature
				if (!dimension.toLowerCase().equals("Measures".toLowerCase())) {
					feature_identifiant = feature_identifiant + "_" + dimesnions_membres.get(j);
					if (!dimension_value.contains(dimesnions_membres.get(j))) {
						dimension_value.add(dimesnions_membres.get(j));
					}
					if (ct_dim == 0) {
						label = dimesnions_membres.get(j);
						ct_dim++;
					} else {
						label = label + "_" + dimesnions_membres.get(j);
					}

					jsondata_properties.put(dimension, dimesnions_membres.get(j));
				}
				if (!nb_label.contains(label)) {
					nb_label.add(label);
				}
				j++;
			}
//			count_label = nb_label.size();
			// fin d'ajout des memebres des dimensions
			// jsondata_properties.put("Nom", ind.getNom());
			if (label.length() == 0) {
				jsondata_properties.put("_Legend", ind.getMeasure());
			} else {
				jsondata_properties.put("_Legend", label);
			}
//			jsondata_properties.put("_Legend", label);
			jsondata_properties.put(ind.getMeasure(), ind.getValeur());
			jsondata_properties.put("_Location", ind.getSpatial());
			jsondata_properties.put("_niveau", "region");
			jsondata_properties.put("_dep", geom1.getNom());
			jsondata_properties.put("_ref", ind.getSpatial() + "_" + geom1.getNom());

			JSONObject jo = geom1.getCentroidJson();

			Iterator itr = jo.values().iterator();

			while (itr.hasNext()) {
				Object element = itr.next();

				if (element.toString().length() > 5) {

					String[] val = element.toString().split(",");

					float lon = Float.parseFloat(val[0].replace("[", ""));
					float lat = Float.parseFloat(val[1].replace("]", ""));

					jsondata_properties.put("long_centro", lon);
					jsondata_properties.put("lat_centro", lat);

				}
			}

			jsondata.put("properties", jsondata_properties);

			JSONParser jsonParser = new JSONParser();
			jsondata.put("geometry", jsonParser.parse(barGeoJson));

			features_id.put(feature_identifiant, y);
			features.add(features_id.get(feature_identifiant), jsondata);
			// pour identifier le ID de feature
			if (!features_id.containsKey(feature_identifiant)) {
				features_id.put(feature_identifiant, i);
				features.add(features_id.get(feature_identifiant), jsondata);
				i++;
			} else {
				int id = features_id.get(feature_identifiant);
				JSONObject old_feature = new JSONObject();
				old_feature = (JSONObject) features.get(id);
				JSONObject properties = new JSONObject();

				// add les mesures dÃ©jÃ  trouvÃ©es
				properties.putAll(((JSONObject) old_feature.get("properties")));

				// add la nouvelle mesure
				properties.putAll(((JSONObject) jsondata.get("properties")));

				jsondata.put("properties", properties);
				features.set(id, jsondata);

			}

			/* FIN RECUPERATION DU JSON DATA */

			ct += 1;

//			if (export.contentEquals("VM")) {
//				if (ctEl == 1) { // TEST SI IL N'Y A QU'UN ELEMENT DANS LA REPONSE DE LA REQUETE
//					TEXT.savefile4(file, geoJsonData.toString(), ct, ctEl);
//					geoJsonData = new JSONObject();
//				}
//				else {
//					if(ct == 1 && ctEl != 1) { // TEST SI C'EST LE PREMIER ELEMENT A ECRIRE ET QU'IL Y A PLUSIEURS ELEMENT DE REPONSE
//						TEXT.savefile4(file, geoJsonData.toString().replace("}]}", "},\n"), ct, ctEl);
//						geoJsonData = new JSONObject();
//					}
//					else if (ct == ctEl && ctEl != 1){ // TEST SI TOUS LES ELEMENTS ONT ETE ECRIT DANS LE FICHIER
//						TEXT.savefile4(file, jsondata.toString().replace("}}", "}}]}"), ct, ctEl);
//					}
//					else { // ECRIRE LES ELEMENTS DANS LE FICHIER
//						TEXT.savefile4(file, jsondata.toString().replace("}}", "}},\n"), ct, ctEl);
//			//			break;
//					}
//				}
//			}	
			System.out.println(" ---- ");

			/* FIN TOUT LES TRAITEMENTS NECESSAIRE JUSQU'A LA CREATION DES BARS */

			y++;
		}

		if (date == 0) {

			if (export.contentEquals("VM")) {
//			  TEXT.savefile3("E:\\Data Published\\geoJSON_Bar_"+niveau+".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION FICHIER UNIQUE
				TEXT.savefile3(geojsonpath + "\\geoJSON_Bar_"
						+ measure_used.replace(":", "_").replace(" ", "_") + "_" + niveau + ".json",
						geoJsonData.toString().replace("}},", "}},\n")); // GENERATION FICHIER
																			// UNIQUE
			}
			if (export.contentEquals("VM_local") || export.contentEquals("local")) {
				TEXT.savefile3(geoJSON_file + "_" + niveau + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																														// FICHIER
																														// UNIQUE
			}

//		  

		}
		if (date == 1) {
			TEXT.savefile3(geoJSON_file + "_" + dt + ".json", geoJsonData.toString().replace("}},", "}},\n")); // GENERATION
																												// FICHIER
																												// AVEC
																												// LA
																												// DATE
																												// DANS
																												// LE
																												// NOM
		}

		System.out.println(" nb in list : " + list_region.size());
//	  System.out.println(" key list region" + list_region.get);
		System.out.println("region list : " + list_region.get("0"));

		System.out.println(
				" region : " + list_region.get("0") + " avec son departement " + list_dep_geometry.get("0").getNom());

	}

//#7f7f7f  #e6f2f0

	@SuppressWarnings("unchecked")
	private static double[][] equalInterval(double[] mes) {

		double[][] range = null;

		return range;
	}

// --------------------------------- CREATION DES TEMPLATES EN FONCTION DE LA REQUETE - <choropleth - Bar> - MultiIndicateur - NMeasure + MultiLayer

	@SuppressWarnings("unchecked")
	private static void ecrireDonneesTemplates(Map<String, List<String>> dim_mem_done, String s_templatejson_File,
			String geoJSON_Data, String geoJSON_Empty, String boundary_file, String export, String hote, String disc,String geojsonpath,String templatepath,
			String dt, int date, String rules) throws IOException {
		// d'abord on prÃ©pare la strucutre de fichier JSON
		JSONObject templateJsonData = new JSONObject();
		templateJsonData.put("version", 2);
		templateJsonData.put("incidentClusterDistance", 50);
		templateJsonData.put("hotspotDistance", 300);
		templateJsonData.put("hotspotMapMaxValue", 16);
		templateJsonData.put("hotspotVisible", false);
		templateJsonData.put("hotspotOpacity", 100);
		templateJsonData.put("incidentsVisible", true);
		templateJsonData.put("boundariesVisible", false);
		templateJsonData.put("tooltipsVisible", false);

//	JSONObject mapCenter = new JSONObject();
//	mapCenter.put("lat", 44.16250418310723);
//	mapCenter.put("lng", 0.22521972656250003);
//	templateJsonData.put("mapCenter",mapCenter);
		JSONObject mapCenter = new JSONObject();
		mapCenter.put("lat", 47.45780853075031);
		mapCenter.put("lng", 1.0546875000000002);
		templateJsonData.put("mapCenter", mapCenter);
		templateJsonData.put("mapZoom", 7);
		templateJsonData.put("titleBarEnabled", true);

		if (export.equals("local")) {
			templateJsonData.put("tileServer", "");
			templateJsonData.put("enterpriseBaseMapId", "204b67da-6927-4fa2-a549-a346a4e5a15c");
		}

		if (export.equals("VM")) {
			templateJsonData.put("tileServer",
					"https://{s}.base.maps.api.here.com/maptile/2.1/xbasetile/newest/pedestrian.day/{z}/{x}/{y}/256/png8?app_id=t2USz3mzaHBkI1917H45&app_code=NcNYh0n_yCVwOMzHbh-93A&style=default");
			templateJsonData.put("enterpriseBaseMapId", "");
		}

		/* config en local */
//
//	templateJsonData.put("tileServer","");	
//	templateJsonData.put("enterpriseBaseMapId","204b67da-6927-4fa2-a549-a346a4e5a15c");
//	templateJsonData.put("enterpriseBaseMapId","0f3f8b1f-fa2f-4e60-b5c4-f6c5ac0a703c");

		/* fin config en local */

		/* config pour la vm */

//	templateJsonData.put("tileServer","https://{s}.base.maps.api.here.com/maptile/2.1/xbasetile/newest/pedestrian.day/{z}/{x}/{y}/256/png8?app_id=t2USz3mzaHBkI1917H45&app_code=NcNYh0n_yCVwOMzHbh-93A&style=default");	
//	templateJsonData.put("enterpriseBaseMapId","");

		/* fin config pour la vm */

		templateJsonData.put("preventWorldWrap", false);

		JSONArray wmsLayers = new JSONArray();
		templateJsonData.put("wmsLayers", wmsLayers);
		templateJsonData.put("hideApplicationLogo", true);
		templateJsonData.put("hideFeatureCount", true);
		templateJsonData.put("hideHelp", true);

		JSONArray staticPointLayers = new JSONArray();
		templateJsonData.put("staticPointLayers", staticPointLayers);
		templateJsonData.put("customHelpUrl", "");
		templateJsonData.put("incidentRenderer", "WebGL");
		templateJsonData.put("disableExport", false);
		templateJsonData.put("beta", null);
		templateJsonData.put("timeOfDayChartDisabled", false);
		templateJsonData.put("dayOfWeekChartDisabled", false);
		templateJsonData.put("dateChartDisabled", false);
		templateJsonData.put("heatMapChartDisabled", false);
		templateJsonData.put("timeOfDayChartVisible", false);
		templateJsonData.put("dayOfWeekChartVisible", false);
		templateJsonData.put("dateChartVisible", false);
		templateJsonData.put("heatMapChartVisible", false);

		JSONObject visualTheme = new JSONObject();
		visualTheme.put("name", "Light");
		visualTheme.put("font", "'Roboto Condensed', sans-serif");
		visualTheme.put("titleBarFontColor", "#111111");
		visualTheme.put("titleBarFontSize", "3vmin");
		visualTheme.put("titleBarBackgroundColor", "#ffffff");
		visualTheme.put("chartFontColor", "#111111");
		visualTheme.put("chartBackgroundColor", "#ffffff");
		visualTheme.put("chartTitleBarBackgroundColor", "#f9f9f9");
		visualTheme.put("chartTitleBarFontColor", "#111111");
		visualTheme.put("chartTitleBarFontSize", "1.9vmin");

		templateJsonData.put("visualTheme", visualTheme);

		JSONObject markerColorSequence = new JSONObject();
		markerColorSequence.put("fromColor", "#7fff7f");
		markerColorSequence.put("toColor", "#ff7f7f");
		markerColorSequence.put("slices", 10);
		markerColorSequence.put("path", "clockwise");

		templateJsonData.put("markerColorSequence", markerColorSequence);

		JSONObject markerRingColorSequence = new JSONObject();
		markerRingColorSequence.put("fromColor", "#007f00");
		markerRingColorSequence.put("toColor", "#7f0000");
		markerRingColorSequence.put("slices", 10);
		markerRingColorSequence.put("path", "clockwise");

		templateJsonData.put("markerRingColorSequence", markerRingColorSequence);
		templateJsonData.put("markerTextColor", "#000000");

		JSONObject clusterStyle = new JSONObject();
		clusterStyle.put("fromSize", 40);
		clusterStyle.put("toSize", 40);
		clusterStyle.put("fromFontSize", 12);
		clusterStyle.put("toFontSize", 12);
		clusterStyle.put("style", "traditional");
		clusterStyle.put("thematicSize", 10);

		templateJsonData.put("clusterStyle", clusterStyle);
		templateJsonData.put("hideIncidentCount", false);

		//////////////// pour carte title
		// pour chaque mesure afficher dans la carte

		String contenuMaps_TitleString = new String();

		// Pour ajouter la liste des mesures affichers
		List<String> mesures = dim_mem_done.get("Measures");
		int b = 0;

		for (String measure_name : mesures) {
			if (b == 0) {
				contenuMaps_TitleString += "[";
			}

			contenuMaps_TitleString += measure_name;

			if (b != mesures.size() - 1) {
				contenuMaps_TitleString += " - ";
			}

			if (b == mesures.size() - 1) {
				contenuMaps_TitleString += "]";
			}

			b++;
		}

		// Pour ajouter la liste des dimensions avec des membres unique sans doubler les
		// mesures
		Set<String> dimensions_names = dim_mem_done.keySet();
		boolean first = true;

		for (String dimension_name : dimensions_names) {
			List<String> dim_members = dim_mem_done.get(dimension_name);

			if ((dim_members.size() == 1) && !(dimension_name.contains("Measures"))) {
				if (first) {
					contenuMaps_TitleString += " : " + dimension_name + " " + dim_members;
					first = false;
				} else {
					contenuMaps_TitleString += ", " + dimension_name + " " + dim_members;
				}
			}
		}
		//////////////// Fin pour carte title

		templateJsonData.put("incidentBranding", contenuMaps_TitleString); // TODO peut Ãªtre c'est le titre de la carte
		templateJsonData.put("logoImage1", "");
		templateJsonData.put("logoImage2", "http://www.geosystems.fr/images/logonoir222.png");

		JSONObject chartConfiguration = new JSONObject();

		chartConfiguration.put("version", 2);
		chartConfiguration.put("recordIndex", 1);
		chartConfiguration.put("compressed", true);

//	System.out.println("+++++++++++++ > " + geoJSON_file);
//	System.out.println(" =============== > " + s_templatejson_File);

		/* APPEL DES FICHIERS GEOJSON */
		File f_data = null;
		File f_empty = null;
		if (date == 0) {
			f_data = new File(geoJSON_Data + ".json");
			f_empty = new File(geoJSON_Empty + ".json");
		}
		if (date == 1) {
			f_data = new File(geoJSON_Data + "_" + dt + ".json");
			f_empty = new File(geoJSON_Empty + ".json");
		}

		String file_name_d = f_data.getName();
		String file_name_e = f_empty.getName();

		// file_name = (new File(f.getParent()).getName()) + "/" + file_name ;
		System.out.println(" -------------- > " + f_empty);
		System.out.println(" -------------- > " + f_empty.getPath());
		System.out.println(" -------------- > " + f_empty.getAbsolutePath());

		if (export.equals("VM")) {
			chartConfiguration.put("featureData", "http://" + hote + "/MappeChest/" + file_name_d); // TODO changer le
																									// chemin de fichier
																									// de donnÃ©es
		}

		if (export.equals("local")) {
			chartConfiguration.put("featureData",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_d); // TODO changer le chemin
																								// de
																								// fichier de donnÃ©es
		}

		chartConfiguration.put("pauseLiveStreaming", false);
		chartConfiguration.put("themeFieldName", null);

		JSONObject defaultThemeClassification = new JSONObject();
		defaultThemeClassification.put("method", "equalInterval");
		defaultThemeClassification.put("groups", 5);
		defaultThemeClassification.put("precision", 0);
		defaultThemeClassification.put("min", null);
		defaultThemeClassification.put("max", null);

		chartConfiguration.put("defaultThemeClassification", defaultThemeClassification);
		chartConfiguration.put("dateTimeFieldName", null);
		chartConfiguration.put("dateTimeFormat", "");
		chartConfiguration.put("geoprocessingTemplate", false);
		chartConfiguration.put("themeToMarkerIconMap", null);
		chartConfiguration.put("dayOfWeekChartId", "dc-dayweek-chart");
		chartConfiguration.put("timeOfDayChartId", "dc-time-chart");
		chartConfiguration.put("themeChartId", "dc-priority-chart");
		chartConfiguration.put("heatMapChartId", "dc-heatmap-chart");
		chartConfiguration.put("dateLineChartId", "dc-dateline-chart");

		JSONArray customChartConfiguration = new JSONArray(); // TODO, ICI on charge les mini-cartes Ã  afficher

		int d = 0;

		// Pour ajouter les chartes pour les mesures
		customChartConfiguration.add(d++, addMesuresThemeCharts(mesures, d));
		/*
		 * if (mesures.size()>1) { // charte combo pour choisir les mesure //
		 * customChartConfiguration.add(d++, addMesureComboChart(d)); // }
		 * 
		 * // charte thÃ¨me pour chaque mesure for (String mesure : mesures) {
		 * customChartConfiguration.add(d++,addMesureThemeChart(mesure, d)); } }
		 */

		customChartConfiguration.add(d++, addDimensionDropDownList("Localisation", "_Location", "false", 31, rules));
		if (f_empty.length() != 0)
			customChartConfiguration.add(d++,
					addDimensionDropDownList("Localisation  Empty Value", "_Location", "true", 48, rules));
		String mesure_nom = "";

		// pour ajouter les chartes pour chaque dimension
		for (String dimension : dim_mem_done.keySet()) {
//		System.out.println("-> dimension membre count " + dimension + " : " + dim_mem_done.get(dimension).size());
//		System.out.println("==> " + dim_mem_done.get(dimension).toString().replace("[", "").replace("]", ""));
//		System.out.println(" <> " + dimension);
			/*
			 * !!!!!!!!!!!! A gÃ©rer dans le cas oÃ¹ on a plusieurs mesures
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!
			 */
			if (dimension.equals("Measures")) {
//			System.out.println(" <=> " + dimension);
				mesure_nom = dim_mem_done.get(dimension).toString().replace("[", "").replace("]", "");
//			System.out.println(" <===> " + mesure_nom);
			}

			if (dimension.equals("species")) {
				customChartConfiguration.add(d++, addDimensionRowChart(dimension, d, mesure_nom));
			}

			/*
			 * if (!dimension.toUpperCase().contentEquals("Measures".toUpperCase())) { if
			 * (dim_mem_done.get(dimension).size() > 5) { customChartConfiguration.add(d++,
			 * addDimensionRowChart(dimension, d, mesure_nom)); }else if
			 * (dim_mem_done.get(dimension).size() > 1) { customChartConfiguration.add(d++,
			 * addDimensionBarChart(dimension, d)); } }
			 */
		}
		// fin d'ajout des chartes des dimensions

		chartConfiguration.put("customChartConfiguration", customChartConfiguration);
		// Yassine
		JSONArray customFeatureConfiguration = new JSONArray();

		/*
		 * ------------------- DÃ©but Creation custom feature for multiStage
		 * ---------------------
		 */

		JSONObject get_custom_polygon = addCustomFeatureConfigurationFeaturesEmpty("FeaturesEmpty", mesure_nom);
		if (f_empty.length() != 0)
			customFeatureConfiguration.add(get_custom_polygon);

		JSONObject get_custom_feature = addCustomFeatureConfigurationFeatures("Features", mesure_nom);
		customFeatureConfiguration.add(get_custom_feature);

//	JSONObject get_custom_polygon_boundary = addCustomFeatureConfigurationPolygonBoundary(file_name_polygon, mesure_nom, export);
//	customFeatureConfiguration.add(get_custom_feature);

//	customFeatureConfiguration.add(get_custom_html);

		chartConfiguration.put("customFeatureConfiguration", customFeatureConfiguration);

//options.put("url", "https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name );
//"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 

		JSONArray customAttributeConfiguration = new JSONArray();

		chartConfiguration.put("customAttributeConfiguration", customAttributeConfiguration);

		JSONArray customStageModelLinks = new JSONArray();
		JSONObject custStageModLinks = new JSONObject();
		custStageModLinks.put("stageModelAKey", "_Location");
		custStageModLinks.put("stageModelB", "polygon_geo");
		custStageModLinks.put("stageModelBKey", "_Location");
		custStageModLinks.put("stageModelA", "Features");

		customStageModelLinks.add(custStageModLinks);
		if (f_empty.length() != 0)
			chartConfiguration.put("customStageModelLinks", customStageModelLinks);

		JSONArray customStageModelConfiguration = new JSONArray();
		JSONObject custStageModDataset = new JSONObject();

		if (export.equals("VM") || export.equals("VM_local")) {
			custStageModDataset.put("dataset", "http://" + hote + "/MappeChest/" + file_name_d); // TODO changer le
																									// chemin de fichier
																									// de donnÃ©es
		}

		if (export.equals("local")) {
			custStageModDataset.put("dataset",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_d); // TODO changer le
																								// chemin de fichier
																								// de
																								// donnÃ©es
		}

		customStageModelConfiguration.add(custStageModDataset);

		JSONObject custStageModStage = new JSONObject();
		custStageModStage.put("stageId", "polygon_geo");
		if (export.equals("VM")) {
			custStageModStage.put("dataset", "http://" + hote + "/MappeChest/" + file_name_e); // TODO changer le
																								// chemin de
																								// fichier de
																								// donnÃ©es
		}

		if (export.equals("local")) {
			custStageModStage.put("dataset", "https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_e); // TODO
																														// changer
																														// le
																														// chemin
																														// de
																														// fichier
																														// de
																														// donnÃ©es
		}

		customStageModelConfiguration.add(custStageModStage);
		if (f_empty.length() != 0)
			chartConfiguration.put("customStageModelConfiguration", customStageModelConfiguration);

		/*
		 * ------------------- Fin Creation custom feature for multiStage
		 * ---------------------
		 */
//Yassine**
//	chartConfiguration.put("customFeatureConfiguration", customFeatureConfiguration);

		chartConfiguration.put("themeChartScaling", 0.95);
		chartConfiguration.put("latitudeFieldName", "lat_centro");
		chartConfiguration.put("longitudeFieldName", "long_centro");

		JSONArray tooltipConfiguration = new JSONArray();

		chartConfiguration.put("tooltipConfiguration", tooltipConfiguration);
		chartConfiguration.put("boundaryData", null);
		chartConfiguration.put("boundaryDataIndexFieldName", null);

		JSONObject boundaryColorSequence = new JSONObject();

		chartConfiguration.put("boundaryColorSequence", boundaryColorSequence);

		JSONObject webGLPointStyle = new JSONObject();
		webGLPointStyle.put("displayEmphasis", true);
		webGLPointStyle.put("opacity", 80);
		webGLPointStyle.put("pointSize", "3");
		webGLPointStyle.put("isMeters", false);

		chartConfiguration.put("webGLPointStyle", webGLPointStyle);

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		lineStyle.put("opacity", 90);
		lineStyle.put("isMeters", false);

		chartConfiguration.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
		polygonStyle.put("fillOpacity", 0.9);
		polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 1);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.8);
		polygonStyle.put("autosetOpacity", true);

		chartConfiguration.put("polygonStyle", polygonStyle);

		JSONObject heatMapColorSequence = new JSONObject();
		heatMapColorSequence.put("fromColor", "#FFEBE5");
		heatMapColorSequence.put("toColor", "#FF0000");
		heatMapColorSequence.put("slices", 15);
		heatMapColorSequence.put("path", "linear");

		chartConfiguration.put("heatMapColorSequence", heatMapColorSequence);
		JSONObject chartColorConfiguration = new JSONObject();

		chartConfiguration.put("chartColorConfiguration", chartColorConfiguration);

		JSONObject dayOfWeekColorSequence = new JSONObject();
		dayOfWeekColorSequence.put("fromColor", "#7f7f00");
		dayOfWeekColorSequence.put("toColor", "#0000FF");
		dayOfWeekColorSequence.put("path", "linear");
		dayOfWeekColorSequence.put("d3", "category10");

		chartConfiguration.put("dayOfWeekColorSequence", dayOfWeekColorSequence);
//Yassine

		templateJsonData.put("chartConfiguration", chartConfiguration);
		JSONObject mapLayerConfiguration = new JSONObject();
		JSONObject New_Feature_Layer = new JSONObject();
		New_Feature_Layer.put("visible", true);
		mapLayerConfiguration.put("FeaturesEmpty", New_Feature_Layer);
		JSONObject Feature = new JSONObject();
		Feature.put("visible", true);
		mapLayerConfiguration.put("Features", New_Feature_Layer);

		templateJsonData.put("mapLayerConfiguration", mapLayerConfiguration);

		templateJsonData.put("loading", false);

		JSONObject filterConfiguration = new JSONObject();
//	JSONArray dow= new JSONArray();
//	filterConfiguration.put("dow", dow);
//	JSONArray tod = new JSONArray();
//	filterConfiguration.put("tod", tod);
//	JSONArray theme = new JSONArray();
//	filterConfiguration.put("theme", theme);
//	JSONArray heatmap = new JSONArray();
//	filterConfiguration.put("heatmap", heatmap);
//	JSONArray dateline = new JSONArray();
//	filterConfiguration.put("dateline", dateline);

		templateJsonData.put("filterConfiguration", filterConfiguration);

		JSONObject geometry = new JSONObject();
		geometry.put("type", "FeatureCollection");
		JSONArray features = new JSONArray();
		geometry.put("features", features);
		JSONObject spatialFilterConfiguration = new JSONObject();
		spatialFilterConfiguration.put("geometry", geometry);
		JSONArray filter = new JSONArray();
		spatialFilterConfiguration.put("filter", filter);

		templateJsonData.put("spatialFilterConfiguration", spatialFilterConfiguration);

		JSONArray bounds = new JSONArray();
		JSONArray bounds_0 = new JSONArray();
		bounds_0.add(0, 99999999);
		bounds_0.add(1, 99999999);
		JSONArray bounds_1 = new JSONArray();
		bounds_1.add(0, -99999999);
		bounds_1.add(1, -99999999);
		bounds.add(0, bounds_0);
		bounds.add(1, bounds_1);

		templateJsonData.put("bounds", bounds);

		if (export.equals("local")) {
			TEXT.savefile(s_templatejson_File, templateJsonData.toString().replace("},", "},\n"));
		}

		if (export.equals("VM")) {
			TEXT.savefile(templatepath,
					templateJsonData.toString().replace("},", "},\n"));
		}

	}

	@SuppressWarnings("unchecked")
	private static void ecrireDonneesTemplatesBar(Map<String, List<String>> dim_mem_done, String s_templatejson_File,
			String geoJSONPolygon_file, String geoJSONBar_file, String boundary_file, String export, String hote,
			String disc,String geojsonpath,String templatepath, String dt, int date, String rules, String niveau, Map<String, String> liste_legende)
			throws IOException {

		String html = write_html.getHtml(dimension_value);

		System.out.println(" <<<<<<<<<<<<< --- POLYGON BAR 1 NIVAU ---- >>>>>>>>>>>>>");

		// d'abord on prÃ©pare la strucutre de fichier JSON
		JSONObject templateJsonData = new JSONObject();
		templateJsonData.put("version", 2);
		templateJsonData.put("incidentClusterDistance", 50);
		templateJsonData.put("hotspotDistance", 300);
		templateJsonData.put("hotspotMapMaxValue", 16);
		templateJsonData.put("hotspotVisible", false);
		templateJsonData.put("hotspotOpacity", 100);
		templateJsonData.put("incidentsVisible", true);
		templateJsonData.put("boundariesVisible", false);
		templateJsonData.put("tooltipsVisible", false);

		JSONObject mapCenter = new JSONObject();
		mapCenter.put("lat", 47.45780853075031);
		mapCenter.put("lng", 1.0546875000000002);
		templateJsonData.put("mapCenter", mapCenter);

		templateJsonData.put("mapZoom", 7);
		templateJsonData.put("titleBarEnabled", true);

		if (export.equals("local")) {
			templateJsonData.put("tileServer", "");
			templateJsonData.put("enterpriseBaseMapId", "0f3f8b1f-fa2f-4e60-b5c4-f6c5ac0a703c");
		}

		if (export.equals("VM") || export.equals("VM_local")) {
			templateJsonData.put("tileServer",
					"https://{s}.base.maps.api.here.com/maptile/2.1/xbasetile/newest/pedestrian.day/{z}/{x}/{y}/256/png8?app_id=t2USz3mzaHBkI1917H45&app_code=NcNYh0n_yCVwOMzHbh-93A&style=default");
			templateJsonData.put("enterpriseBaseMapId", "");
		}

		templateJsonData.put("preventWorldWrap", false);

		JSONArray wmsLayers = new JSONArray();
		templateJsonData.put("wmsLayers", wmsLayers);
		templateJsonData.put("hideApplicationLogo", true);
		templateJsonData.put("hideFeatureCount", true);
		templateJsonData.put("hideHelp", true);

		JSONArray staticPointLayers = new JSONArray();
		templateJsonData.put("staticPointLayers", staticPointLayers);
		templateJsonData.put("customHelpUrl", "");
		templateJsonData.put("incidentRenderer", "Cluster");
		templateJsonData.put("disableExport", false);
		templateJsonData.put("beta", null);
		templateJsonData.put("timeOfDayChartDisabled", false);
		templateJsonData.put("dayOfWeekChartDisabled", false);
		templateJsonData.put("dateChartDisabled", false);
		templateJsonData.put("heatMapChartDisabled", false);
		templateJsonData.put("timeOfDayChartVisible", false);
		templateJsonData.put("dayOfWeekChartVisible", false);
		templateJsonData.put("dateChartVisible", false);
		templateJsonData.put("heatMapChartVisible", false);

		JSONObject visualTheme = new JSONObject();
		visualTheme.put("name", "Light");
		visualTheme.put("font", "'Roboto Condensed', sans-serif");
		visualTheme.put("titleBarFontColor", "#111111");
		visualTheme.put("titleBarFontSize", "3vmin");
		visualTheme.put("titleBarBackgroundColor", "#ffffff");
		visualTheme.put("chartFontColor", "#111111");
		visualTheme.put("chartBackgroundColor", "#ffffff");
		visualTheme.put("chartTitleBarBackgroundColor", "#f9f9f9");
		visualTheme.put("chartTitleBarFontColor", "#111111");
		visualTheme.put("chartTitleBarFontSize", "1.9vmin");

		templateJsonData.put("visualTheme", visualTheme);

		JSONObject markerColorSequence = new JSONObject();
		markerColorSequence.put("fromColor", "#7fff7f");
		markerColorSequence.put("toColor", "#ff7f7f");
		markerColorSequence.put("slices", 10);
		markerColorSequence.put("path", "clockwise");

		templateJsonData.put("markerColorSequence", markerColorSequence);

		JSONObject markerRingColorSequence = new JSONObject();
		markerRingColorSequence.put("fromColor", "#007f00");
		markerRingColorSequence.put("toColor", "#7f0000");
		markerRingColorSequence.put("slices", 10);
		markerRingColorSequence.put("path", "clockwise");

		templateJsonData.put("markerRingColorSequence", markerRingColorSequence);
		templateJsonData.put("markerTextColor", "#000000");

		JSONObject clusterStyle = new JSONObject();
		clusterStyle.put("fromSize", 40);
		clusterStyle.put("toSize", 40);
		clusterStyle.put("fromFontSize", 12);
		clusterStyle.put("toFontSize", 12);
		clusterStyle.put("style", "traditional");
		clusterStyle.put("thematicSize", 10);

		templateJsonData.put("clusterStyle", clusterStyle);
		templateJsonData.put("hideIncidentCount", false);

		//////////////// pour carte title
		// pour chaque mesure afficher dans la carte

		String contenuMaps_TitleString = new String();

		// Pour ajouter la liste des mesures affichers
		List<String> mesures = dim_mem_done.get("Measures");
		int b = 0;

		for (String measure_name : mesures) {
			if (b == 0) {
				contenuMaps_TitleString += "[";
			}

			contenuMaps_TitleString += measure_name;

			if (b != mesures.size() - 1) {
				contenuMaps_TitleString += " - ";
			}

			if (b == mesures.size() - 1) {
				contenuMaps_TitleString += "]";
			}

			b++;
		}

		// Pour ajouter la liste des dimensions avec des membres unique sans doubler les
		// mesures
		Set<String> dimensions_names = dim_mem_done.keySet();
		boolean first = true;

		for (String dimension_name : dimensions_names) {
			List<String> dim_members = dim_mem_done.get(dimension_name);

			if ((dim_members.size() == 1) && !(dimension_name.contains("Measures"))) {
				if (first) {
					contenuMaps_TitleString += " : " + dimension_name + " " + dim_members;
					first = false;
				} else {
					contenuMaps_TitleString += ", " + dimension_name + " " + dim_members;
				}
			}
		}
		//////////////// Fin pour carte title

		templateJsonData.put("incidentBranding", "Features"); // TODO peut Ãªtre c'est le titre de la carte
		templateJsonData.put("logoImage1", "");
		templateJsonData.put("logoImage2", "http://www.geosystems.fr/images/logonoir222.png");

		JSONObject chartConfiguration = new JSONObject();

		chartConfiguration.put("version", 2);
		chartConfiguration.put("recordIndex", 1);
		chartConfiguration.put("compressed", true);

		/* APPEL DES FICHIERS GEOJSON */
		File f_polygon = null;
		File f_bar = null;
		if (date == 0) {
			f_polygon = new File(geoJSONPolygon_file + ".json");
			f_bar = new File(geoJSONBar_file + ".json");
		}
		if (date == 1) {
			f_polygon = new File(geoJSONPolygon_file + "_" + dt + ".json");
			f_bar = new File(geoJSONBar_file + ".json");
		}

		String file_name_polygon = f_polygon.getName();
		String file_name_bar = f_bar.getName();
		// file_name = (new File(f.getParent()).getName()) + "/" + file_name ;

		System.out.println(" -------------- > " + f_bar.getName());

		if (export.equals("VM") || export.equals("VM_local")) {
			chartConfiguration.put("featureData", "http://" + hote + "/MappeChest/" + file_name_bar); // TODO changer
																										// le chemin de
																										// fichier de
																										// donnÃ©es
		}

		if (export.equals("local")) {
			chartConfiguration.put("featureData",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_bar); // TODO changer le
																									// chemin de fichier
																									// de
																									// donnÃ©es
		}

		chartConfiguration.put("pauseLiveStreaming", false);
		chartConfiguration.put("themeFieldName", null);

		chartConfiguration.put("defaultThemeClassification", null);
		chartConfiguration.put("dateTimeFieldName", null);
		chartConfiguration.put("dateTimeFormat", "");
		chartConfiguration.put("geoprocessingTemplate", false);
		chartConfiguration.put("themeToMarkerIconMap", null);
		chartConfiguration.put("dayOfWeekChartId", "dc-dayweek-chart");
		chartConfiguration.put("timeOfDayChartId", "dc-time-chart");
		chartConfiguration.put("themeChartId", "dc-priority-chart");
		chartConfiguration.put("heatMapChartId", "dc-heatmap-chart");
		chartConfiguration.put("dateLineChartId", "dc-dateline-chart");

		JSONArray customChartConfiguration = new JSONArray(); // TODO, ICI on charge les mini-cartes Ã  afficher

		int d = 0;
		double pos = 5;

		// Pour ajouter les chartes pour les mesures

		customChartConfiguration.add(d++, addDimensionDropDownList("Localisation", "_Location", "true", pos, rules));

		String mesure_nom = "";
		int j = 0;
		for (String dimension : dim_mem_done.keySet()) {
			if (dimension.equals("Measures")) {
				mesure_nom = dim_mem_done.get(dimension).toString().replace("[", "").replace("]", "");
				customChartConfiguration.add(d++, addIndicatorThemeChart(mesure_nom, d));

			} else {
				pos += 17;
				customChartConfiguration.add(d++, addDimensionDropDownList(dimension, dimension, "false", pos, rules));
			}
			j++;
		}

		// pour ajouter les chartes pour chaque dimension

		System.out.println(" <mesure> " + j);
		customChartConfiguration.add(d++, addDimensionBarChart("_Legend", mesure_nom, j));

		// RAJOUT DU ROW POUR LA LEGENDE
		// customChartConfiguration.add(d++,addDimensionRowChart("_Legend", 0,
		// mesure_nom));

		// RAJOUT DU DATA TABLE
		// customChartConfiguration.add(d++,addPolygonDataTable("_Location", mesure_nom,
		// 0, niveau, liste_legende));

		// customChartConfiguration.add(d++, addDimensionDropDownList(location, d,
		// rules));

		// fin d'ajout des chartes des dimensions

		chartConfiguration.put("customChartConfiguration", customChartConfiguration);

		JSONArray customFeatureConfiguration = new JSONArray();

//	"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b |||||||||||||||  file_name_polygon

		/*
		 * ------------------- DÃ©but Creation custom feature for multiStage
		 * ---------------------
		 */

		JSONObject get_custom_polygon = addCustomFeatureConfigurationPolygon("Polygon Stage", mesure_nom);
		customFeatureConfiguration.add(get_custom_polygon);

		JSONObject get_custom_feature = addCustomFeatureConfigurationFeatures("Features", mesure_nom);
		customFeatureConfiguration.add(get_custom_feature);

//	JSONObject get_custom_polygon_boundary = addCustomFeatureConfigurationPolygonBoundary(file_name_polygon, mesure_nom, export);
//	customFeatureConfiguration.add(get_custom_feature);

//	customFeatureConfiguration.add(get_custom_html);

		chartConfiguration.put("customFeatureConfiguration", customFeatureConfiguration);

//options.put("url", "https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name );
//"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 

		JSONArray customAttributeConfiguration = new JSONArray();

		chartConfiguration.put("customAttributeConfiguration", customAttributeConfiguration);

		JSONArray customStageModelLinks = new JSONArray();
		JSONObject custStageModLinks = new JSONObject();
		custStageModLinks.put("stageModelAKey", "_Location");
		custStageModLinks.put("stageModelB", "polygon_geo");
		custStageModLinks.put("stageModelBKey", "_Location");
		customStageModelLinks.add(custStageModLinks);
		chartConfiguration.put("customStageModelLinks", customStageModelLinks);

		JSONArray customStageModelConfiguration = new JSONArray();
		JSONObject custStageModDataset = new JSONObject();

		if (export.equals("VM") || export.equals("VM_local")) {
			custStageModDataset.put("dataset", "http://" + hote + "/MappeChest/" + file_name_bar); // TODO changer le
																									// chemin de fichier
																									// de donnÃ©es
		}

		if (export.equals("local")) {
			custStageModDataset.put("dataset",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_bar); // TODO changer le
																									// chemin de fichier
																									// de
																									// donnÃ©es
		}

		customStageModelConfiguration.add(custStageModDataset);

		JSONObject custStageModStage = new JSONObject();
		custStageModStage.put("stageId", "polygon_geo");
		if (export.equals("VM") || export.equals("VM_local")) {
			custStageModStage.put("dataset", "http://" + hote + "/MappeChest/" + file_name_polygon); // TODO changer le
																										// chemin de
																										// fichier de
																										// donnÃ©es
		}

		if (export.equals("local")) {
			custStageModStage.put("dataset",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_polygon); // TODO changer le
																										// chemin de
																										// fichier
																										// de donnÃ©es
		}

		customStageModelConfiguration.add(custStageModStage);

		chartConfiguration.put("customStageModelConfiguration", customStageModelConfiguration);

		/*
		 * ------------------- Fin Creation custom feature for multiStage
		 * ---------------------
		 */

//	chartConfiguration.put("customFeatureConfiguration", customFeatureConfiguration);

		chartConfiguration.put("themeChartScaling", 0.95);
		chartConfiguration.put("latitudeFieldName", "");
		chartConfiguration.put("longitudeFieldName", "");

		JSONArray tooltipConfiguration = new JSONArray();

		chartConfiguration.put("tooltipConfiguration", tooltipConfiguration);
		chartConfiguration.put("boundaryData", null);
		chartConfiguration.put("boundaryDataIndexFieldName", null);

		JSONObject boundaryColorSequence = new JSONObject();

		chartConfiguration.put("boundaryColorSequence", boundaryColorSequence);

		JSONObject webGLPointStyle = new JSONObject();
		webGLPointStyle.put("displayEmphasis", true);
		webGLPointStyle.put("opacity", 100);
		webGLPointStyle.put("pointSize", "3");
		webGLPointStyle.put("isMeters", false);

		chartConfiguration.put("webGLPointStyle", webGLPointStyle);

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		lineStyle.put("opacity", 90);
		lineStyle.put("isMeters", false);

		chartConfiguration.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
		polygonStyle.put("fillOpacity", 0.9);
		polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 1);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.8);
		polygonStyle.put("autosetOpacity", true);

		chartConfiguration.put("polygonStyle", polygonStyle);

		JSONObject heatMapColorSequence = new JSONObject();
		heatMapColorSequence.put("fromColor", "#FFEBE5");
		heatMapColorSequence.put("toColor", "#FF0000");
		heatMapColorSequence.put("slices", 15);
		heatMapColorSequence.put("path", "linear");

		chartConfiguration.put("heatMapColorSequence", heatMapColorSequence);
		JSONObject chartColorConfiguration = new JSONObject();

		chartConfiguration.put("chartColorConfiguration", chartColorConfiguration);

		JSONObject dayOfWeekColorSequence = new JSONObject();
		dayOfWeekColorSequence.put("fromColor", "#7f7f00");
		dayOfWeekColorSequence.put("toColor", "#0000FF");
		dayOfWeekColorSequence.put("path", "linear");
		dayOfWeekColorSequence.put("d3", "category10");

		chartConfiguration.put("dayOfWeekColorSequence", dayOfWeekColorSequence);

		templateJsonData.put("chartConfiguration", chartConfiguration);

		JSONObject mapLayerConfiguration = new JSONObject();
		JSONObject New_Feature_Layer = new JSONObject();
		New_Feature_Layer.put("visible", true);
		mapLayerConfiguration.put("Polygon Stage", New_Feature_Layer);
		JSONObject Feature = new JSONObject();
		Feature.put("visible", true);
		mapLayerConfiguration.put("Features", New_Feature_Layer);

		templateJsonData.put("mapLayerConfiguration", mapLayerConfiguration);
		templateJsonData.put("loading", false);

		JSONObject filterConfiguration = new JSONObject();

		templateJsonData.put("filterConfiguration", filterConfiguration);

		JSONObject spatialFilterConfiguration = new JSONObject();
		JSONObject geometry = new JSONObject();
		geometry.put("type", "FeatureCollection");
		JSONArray features = new JSONArray();
		geometry.put("features", features);

		spatialFilterConfiguration.put("geometry", geometry);
		JSONArray filter = new JSONArray();
		spatialFilterConfiguration.put("filter", filter);
		spatialFilterConfiguration.put("circles", new JSONArray());

		templateJsonData.put("spatialFilterConfiguration", spatialFilterConfiguration);

		if (export.equals("local") || export.equals("VM_local")) {
			TEXT.savefile(s_templatejson_File, templateJsonData.toString().replace("},", "},\n"));
		}
		System.out.println("eeeeeeeeeeeeeeeeeeeXXXXXXXXXXXXXXXXXeeeeeeeeeeeeeeeeeeee");
		System.out.println("eeeeeeeeeeeeeeeeeeeXXXXXXXXXXXXXXXXXeeeeeeeeeeeeeeeeeeee");
		System.out.println(templatepath);
		System.out.println("eeeeeeeeeeeeeeeeeeeXXXXXXXXXXXXXXXXXeeeeeeeeeeeeeeeeeeee");
		System.out.println("eeeeeeeeeeeeeeeeeeeXXXXXXXXXXXXXXXXXeeeeeeeeeeeeeeeeeeee");
		System.out.println(templateJsonData.toString().replace("},", "},\n"));
		System.out.println("eeeeeeeeeeeeeeeeeeeXXXXXXXXXXXXXXXXXeeeeeeeeeeeeeeeeeeee");
		if (export.equals("VM")) {
			TEXT.savefile(templatepath,
					templateJsonData.toString().replace("},", "},\n"));
		}

	}

	@SuppressWarnings("unchecked")
	private static void ecrireDonneesTemplatesBarNMeasure(Map<String, List<String>> dim_mem_done,
			String s_templatejson_File, String geoJSONPolygon_file, String geoJSONBar_file, String boundary_file,
			String export, String hote, String disc,String geojsonpath,String templatepath, String dt, int date, Map<String, String> liste_fichier_mesure,
			ArrayList<String> measure, String rules, Map<String, String> liste_legende) throws IOException {

		System.out.println(" DEBUT ECRITURE TEMPLATE N MEASURE ");
//	for (String mes : measure) {
//		System.out.println(mes);
//		System.out.println(liste_fichier_mesure.get(mes));
//		
//	}

//	System.out.println(list_couleur[0][0] + " to " + list_couleur[0][1]);
//	System.out.println();
		System.out.println(liste_fichier_mesure.get(measure.get(0)));

//	for(String val : dimension_value) {
//		System.out.println(" here " + val);
//	}
//	System.out.println("  ----  ");

		String html = write_html.getHtml(dimension_value);

		// d'abord on prÃ©pare la strucutre de fichier JSON
		JSONObject templateJsonData = new JSONObject();
		templateJsonData.put("version", 2);
		templateJsonData.put("incidentClusterDistance", 50);
		templateJsonData.put("hotspotDistance", 300);
		templateJsonData.put("hotspotMapMaxValue", 16);
		templateJsonData.put("hotspotVisible", false);
		templateJsonData.put("hotspotOpacity", 100);
		templateJsonData.put("incidentsVisible", true);
		templateJsonData.put("boundariesVisible", false);
		templateJsonData.put("tooltipsVisible", false);

//	JSONObject mapCenter = new JSONObject();
//	mapCenter.put("lat",  44.84029065139799);
//	mapCenter.put("lng", 0.48339843750000006);
//	templateJsonData.put("mapCenter",mapCenter);

		JSONObject mapCenter = new JSONObject();
		mapCenter.put("lat", 47.45780853075031);
		mapCenter.put("lng", 1.0546875000000002);
		templateJsonData.put("mapCenter", mapCenter);

		templateJsonData.put("mapZoom", 7);
		templateJsonData.put("titleBarEnabled", true);

		if (export.equals("local")) {
			templateJsonData.put("tileServer", "");
			templateJsonData.put("enterpriseBaseMapId", "0f3f8b1f-fa2f-4e60-b5c4-f6c5ac0a703c");
		}

		if (export.equals("VM") || export.equals("VM_local")) {
			templateJsonData.put("tileServer",
					"https://{s}.base.maps.api.here.com/maptile/2.1/xbasetile/newest/pedestrian.day/{z}/{x}/{y}/256/png8?app_id=t2USz3mzaHBkI1917H45&app_code=NcNYh0n_yCVwOMzHbh-93A&style=default");
			templateJsonData.put("enterpriseBaseMapId", "");
		}

		/* config en local */
//
//	templateJsonData.put("tileServer","");	
//	templateJsonData.put("enterpriseBaseMapId","204b67da-6927-4fa2-a549-a346a4e5a15c");
//	templateJsonData.put("enterpriseBaseMapId","0f3f8b1f-fa2f-4e60-b5c4-f6c5ac0a703c");

		/* fin config en local */

		/* config pour la vm */

//	templateJsonData.put("tileServer","https://{s}.base.maps.api.here.com/maptile/2.1/xbasetile/newest/pedestrian.day/{z}/{x}/{y}/256/png8?app_id=t2USz3mzaHBkI1917H45&app_code=NcNYh0n_yCVwOMzHbh-93A&style=default");	
//	templateJsonData.put("enterpriseBaseMapId","");

		/* fin config pour la vm */

		templateJsonData.put("preventWorldWrap", false);

		JSONArray wmsLayers = new JSONArray();
		templateJsonData.put("wmsLayers", wmsLayers);

		templateJsonData.put("hideApplicationLogo", true);
		templateJsonData.put("hideFeatureCount", true);
		templateJsonData.put("hideHelp", true);

		JSONArray staticPointLayers = new JSONArray();
		templateJsonData.put("staticPointLayers", staticPointLayers);

		templateJsonData.put("customHelpUrl", "");
		templateJsonData.put("incidentRenderer", "Cluster");
		templateJsonData.put("disableExport", false);
		templateJsonData.put("beta", null);
		templateJsonData.put("timeOfDayChartDisabled", false);
		templateJsonData.put("dayOfWeekChartDisabled", false);
		templateJsonData.put("dateChartDisabled", false);
		templateJsonData.put("heatMapChartDisabled", false);
		templateJsonData.put("timeOfDayChartVisible", false);
		templateJsonData.put("dayOfWeekChartVisible", false);
		templateJsonData.put("dateChartVisible", false);
		templateJsonData.put("heatMapChartVisible", false);

		JSONObject visualTheme = new JSONObject();
		visualTheme.put("name", "Light");
		visualTheme.put("font", "'Roboto Condensed', sans-serif");
		visualTheme.put("titleBarFontColor", "#111111");
		visualTheme.put("titleBarFontSize", "3vmin");
		visualTheme.put("titleBarBackgroundColor", "#ffffff");
		visualTheme.put("chartFontColor", "#111111");
		visualTheme.put("chartBackgroundColor", "#ffffff");
		visualTheme.put("chartTitleBarBackgroundColor", "#f9f9f9");
		visualTheme.put("chartTitleBarFontColor", "#111111");
		visualTheme.put("chartTitleBarFontSize", "1.9vmin");

		templateJsonData.put("visualTheme", visualTheme);

		JSONObject markerColorSequence = new JSONObject();
		markerColorSequence.put("fromColor", "#7fff7f");
		markerColorSequence.put("toColor", "#ff7f7f");
		markerColorSequence.put("slices", 10);
		markerColorSequence.put("path", "clockwise");

		templateJsonData.put("markerColorSequence", markerColorSequence);

		JSONObject markerRingColorSequence = new JSONObject();
		markerRingColorSequence.put("fromColor", "#007f00");
		markerRingColorSequence.put("toColor", "#7f0000");
		markerRingColorSequence.put("slices", 10);
		markerRingColorSequence.put("path", "clockwise");

		templateJsonData.put("markerRingColorSequence", markerRingColorSequence);
		templateJsonData.put("markerTextColor", "#000000");

		JSONObject clusterStyle = new JSONObject();
		clusterStyle.put("fromSize", 40);
		clusterStyle.put("toSize", 40);
		clusterStyle.put("fromFontSize", 12);
		clusterStyle.put("toFontSize", 12);
		clusterStyle.put("style", "traditional");
		clusterStyle.put("thematicSize", 10);

		templateJsonData.put("clusterStyle", clusterStyle);
		templateJsonData.put("hideIncidentCount", false);

		//////////////// pour carte title
		// pour chaque mesure afficher dans la carte

		String contenuMaps_TitleString = new String();

		// Pour ajouter la liste des mesures affichers
		List<String> mesures = dim_mem_done.get("Measures");
		int b = 0;

		for (String measure_name : mesures) {
			if (b == 0) {
				contenuMaps_TitleString += "[";
			}

			contenuMaps_TitleString += measure_name;

			if (b != mesures.size() - 1) {
				contenuMaps_TitleString += " - ";
			}

			if (b == mesures.size() - 1) {
				contenuMaps_TitleString += "]";
			}

			b++;
		}

		// Pour ajouter la liste des dimensions avec des membres unique sans doubler les
		// mesures
		Set<String> dimensions_names = dim_mem_done.keySet();
		boolean first = true;

		for (String dimension_name : dimensions_names) {
			List<String> dim_members = dim_mem_done.get(dimension_name);

			if ((dim_members.size() == 1) && !(dimension_name.contains("Measures"))) {
				if (first) {
					contenuMaps_TitleString += " : " + dimension_name + " " + dim_members;
					first = false;
				} else {
					contenuMaps_TitleString += ", " + dimension_name + " " + dim_members;
				}
			}
		}
		//////////////// Fin pour carte title

		templateJsonData.put("incidentBranding", "Features"); // TODO peut Ãªtre c'est le titre de la carte
		templateJsonData.put("logoImage1", "");
		templateJsonData.put("logoImage2", "http://www.geosystems.fr/images/logonoir222.png");

		JSONObject chartConfiguration = new JSONObject();

		chartConfiguration.put("version", 2);
		chartConfiguration.put("recordIndex", 1);
		chartConfiguration.put("compressed", true);

//	System.out.println("+++++++++++++ > " + geoJSON_file);
//	System.out.println(" =============== > " + s_templatejson_File);

		/* APPEL DES FICHIERS GEOJSON */
		File f_polygon = null;
		File f_bar = null;
		if (date == 0) {
			f_polygon = new File(geoJSONPolygon_file + ".json");
//		f_bar = new File(geoJSONBar_file+".json");
			f_bar = new File(liste_fichier_mesure.get(measure.get(0)) + ".json");

		}
		if (date == 1) {
			f_polygon = new File(geoJSONPolygon_file + "_" + dt + ".json");
			f_bar = new File(geoJSONBar_file + ".json");
		}
//	File f = new File(geoJSON_file+"_"+dt+".json"); 
//	File f = new File(geoJSON_file);

		String file_name_polygon = f_polygon.getName();
		String file_name_bar = f_bar.getName();
		// file_name = (new File(f.getParent()).getName()) + "/" + file_name ;

		System.out.println(" -------------- > " + f_bar.getName());

//	list_couleur
//	liste_fichier_mesure.get(measure.get(0))

		if (export.equals("VM") || export.equals("VM_local")) {
			chartConfiguration.put("featureData", "http://" + hote + "/MappeChest/" + file_name_bar); // TODO changer
																										// le chemin de
																										// fichier de
																										// donnÃ©es
//		chartConfiguration.put("featureData","http://51.38.196.163/MappeChest/" + liste_fichier_mesure.get(measure.get(0))+".json");
		}

		if (export.equals("local")) {
			chartConfiguration.put("featureData",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_bar); // TODO changer le
																									// chemin de fichier
																									// de
																									// donnÃ©es
		}

		chartConfiguration.put("pauseLiveStreaming", false);
		chartConfiguration.put("themeFieldName", null);

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",5);
//	defaultThemeClassification.put("precision",0);
//	defaultThemeClassification.put("min",null);
//	defaultThemeClassification.put("max",null);

		chartConfiguration.put("defaultThemeClassification", null);
		chartConfiguration.put("dateTimeFieldName", null);
		chartConfiguration.put("dateTimeFormat", "");
		chartConfiguration.put("geoprocessingTemplate", false);
		chartConfiguration.put("themeToMarkerIconMap", null);
		chartConfiguration.put("dayOfWeekChartId", "dc-dayweek-chart");
		chartConfiguration.put("timeOfDayChartId", "dc-time-chart");
		chartConfiguration.put("themeChartId", "dc-priority-chart");
		chartConfiguration.put("heatMapChartId", "dc-heatmap-chart");
		chartConfiguration.put("dateLineChartId", "dc-dateline-chart");

		JSONArray customChartConfiguration = new JSONArray(); // TODO, ICI on charge les mini-cartes Ã  afficher

//		int d = 0;

		// Pour ajouter les chartes pour les mesures
//	customChartConfiguration.add(d++,addMesuresThemeCharts(mesures, d));
		/*
		 * if (mesures.size()>1) { // charte combo pour choisir les mesure //
		 * customChartConfiguration.add(d++, addMesureComboChart(d)); // }
		 * 
		 * // charte thÃ¨me pour chaque mesure for (String mesure : mesures) {
		 * customChartConfiguration.add(d++,addMesureThemeChart(mesure, d)); } }
		 */
//		String location = "";
//
//		String mesure_nom = "";
//		int j = 0;
//		for (String dimension : dim_mem_done.keySet()) {
//			if (dimension.equals("Measures")) {
//				mesure_nom = dim_mem_done.get(dimension).toString().replace("[", "").replace("]", "");
//			}
//			j++;
//		}
		int d = 0;
		double pos = 5;

		// Pour ajouter les chartes pour les mesures

		customChartConfiguration.add(d++, addDimensionDropDownList("Localisation", "_Location", "true", pos, rules));

		String mesure_nom = "";
		int j = 0;
		for (String dimension : dim_mem_done.keySet()) {
			if (dimension.equals("Measures")) {
				mesure_nom = dim_mem_done.get(dimension).toString().replace("[", "").replace("]", "");
				customChartConfiguration.add(d++, addIndicatorThemeChart(mesure_nom, d));

			} else {
				pos += 17;
				customChartConfiguration.add(d++, addDimensionDropDownList(dimension, dimension, "false", pos, rules));
			}
			j++;
		}
		// pour ajouter les chartes pour chaque dimension
//	JSONObject get_custom_html = null;
//	for (String dimension : dim_mem_done.keySet()){
//		System.out.println("-> dimension membre count " + dimension + " : " + dim_mem_done.get(dimension).size());
//		System.out.println("==> " + dim_mem_done.get(dimension).toString().replace("[", "").replace("]", ""));

		/*
		 * !!!!!!!!!!!! A gÃ©rer dans le cas oÃ¹ on a plusieurs mesures
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!
		 */

//		if (!dimension.equals("Measures")) {
//			System.out.println(" <dimension> " + dimension);
//			customChartConfiguration.add(d++,addIndicatorThemeChart(dimension, d));
//			System.out.println(" <mesure> " + mesure_nom);
//			customChartConfiguration.add(d++,addDimensionBarChart(dimension, mesure_nom, d));

		// RAJOUTER HTML DISPLAY
//			get_custom_html = addCustomFeatureConfigurationHtml(html, dimension);
//		}

//	}

		// creation des thÃ¨mes pour chaque mesure
//	customChartConfiguration.add(d++,addIndicatorThemeChart("_Legend", d));

		// rÃ©cupÃ©ration des stages
		int stg = 0;
		String[][] stage_bar = new String[measure.size() - 1][2];
		for (String mes : measure) {
			if (stg != 0) {
				stage_bar[stg - 1][0] = mes.replace(" ", "_").replace(":", "_").toLowerCase();
				stage_bar[stg - 1][1] = "stage_bar_" + mes.replace(" ", "_").replace(":", "_").toLowerCase();
				System.out.println(stage_bar[stg - 1][1]);
			}

			stg++;

		}

		int com = 0;

//	creation des stages en mÃªme temps
//	String[] stage_bar = new String[measure.size()-1];
		for (String mes : measure) {
//		System.out.println(mes);
//		System.out.println(liste_fichier_mesure.get(mes));

			if (com != 0) {
//			stage_bar[com-1] = "stage_bar_"+mes.replace(" ", "_").replace(":", "_").toLowerCase();
//			System.out.println(stage_bar[com-1] );
				customChartConfiguration.add(d++,
						addIndicatorThemeChartNMeasure("_Legend", mes, com, stage_bar[com - 1][1], liste_legende));
				customChartConfiguration.add(d++,
						addDimensionBarChartNMeasure("_Legend", mes, com, stage_bar[com - 1][1]));
			} else {
				customChartConfiguration.add(d++,
						addIndicatorThemeChartNMeasure("_Legend", mes, com, "", liste_legende));
				customChartConfiguration.add(d++, addDimensionBarChartNMeasure("_Legend", mes, com, ""));
			}

			com++;

		}

		System.out.println(" <mesure> " + mesure_nom);

		// rajout de la lÃ©gende
//	customChartConfiguration.add(d++,addDimensionBarChart("_Legend", mesure_nom, j));

		// rajout du dropdownlist des locations
//		customChartConfiguration.add(d++, addDimensionDropDownList(location,25, rules));
		// fin d'ajout des chartes des dimensions

		chartConfiguration.put("customChartConfiguration", customChartConfiguration);

		JSONArray customFeatureConfiguration = new JSONArray();

//	"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b |||||||||||||||  file_name_polygon

		/*
		 * ------------------- DÃ©but Creation custom feature for multiStage
		 * ---------------------
		 */

		JSONObject get_custom_polygon = addCustomFeatureConfigurationPolygon("Polygon Stage", mesure_nom);
		customFeatureConfiguration.add(get_custom_polygon);

		// rajout des features Ã  part le feature par defaut
		int ct_mes0 = 1;
		while (ct_mes0 < measure.size()) {
			System.out.println(" -> here feature configuration <- ");
			JSONObject get_custom_feature0 = addCustomFeatureConfigurationFeaturesNMeasure(file_name_polygon,
					stage_bar[ct_mes0 - 1][0], stage_bar[ct_mes0 - 1][1]);

			customFeatureConfiguration.add(get_custom_feature0);
			ct_mes0++;
		}

		JSONObject get_custom_feature = addCustomFeatureConfigurationFeatures("Features", mesure_nom);
		customFeatureConfiguration.add(get_custom_feature);

//	JSONObject get_custom_polygon_boundary = addCustomFeatureConfigurationPolygonBoundary(file_name_polygon, mesure_nom, export);
//	customFeatureConfiguration.add(get_custom_feature);

//	customFeatureConfiguration.add(get_custom_html);

		chartConfiguration.put("customFeatureConfiguration", customFeatureConfiguration);

//	JSONObject get_custom_features = addCustomFeatureConfigurationFeaturesCluster(file_name_b);
//	customFeatureConfiguration.add(get_custom_features);

//	JSONObject get_custom_point = addCustomFeatureConfigurationFeaturesPoint(file_name_b, mesure_nom);
//	customFeatureConfiguration.add(get_custom_point);

//	if (export.equals("VM")) {
//	options.put("url","http://51.38.196.163/MappeChest/" + file_name); // TODO changer le chemin de fichier de donnÃ©es
//  }
//  
//  if (export.equals("local")) {
//	  options.put("url","https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name); // TODO changer le chemin de fichier de donnÃ©es
//  }

//options.put("url", "https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name );
//"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 

		JSONArray customAttributeConfiguration = new JSONArray();

		chartConfiguration.put("customAttributeConfiguration", customAttributeConfiguration);

		JSONArray customStageModelLinks = new JSONArray();
		JSONObject custStageModLinks = new JSONObject();
		custStageModLinks.put("stageModelAKey", "_Location");
		custStageModLinks.put("stageModelB", "polygon_geo");
		custStageModLinks.put("stageModelBKey", "_Location");
		customStageModelLinks.add(custStageModLinks);

		// rajout des liens en fonction des mesures utilisÃ©es
		System.out.println();
		String stage = "";
		for (String[] m : stage_bar) {
			JSONObject custStageModLinksMes = new JSONObject();
			stage = m[1];
			System.out.println(" -> creation link stage " + stage);
			custStageModLinksMes.put("stageModelA", stage);
			custStageModLinksMes.put("stageModelAKey", "_Location");
			custStageModLinksMes.put("stageModelB", "polygon_geo");
			custStageModLinksMes.put("stageModelBKey", "_Location");
			customStageModelLinks.add(custStageModLinksMes);

		}

		JSONObject custStageModLinksdim = new JSONObject();

		custStageModLinksdim.put("stageModelAKey", "_Legend");
		custStageModLinksdim.put("stageModelB", stage);
		custStageModLinksdim.put("stageModelBKey", "_Legend");
		customStageModelLinks.add(custStageModLinksdim);

		chartConfiguration.put("customStageModelLinks", customStageModelLinks);

		JSONArray customStageModelConfiguration = new JSONArray();
		JSONObject custStageModDataset = new JSONObject();

		if (export.equals("VM") || export.equals("VM_local")) {
			custStageModDataset.put("dataset", "http://" + hote + "/MappeChest/" + file_name_bar); // TODO changer le
																									// chemin de fichier
																									// de donnÃ©es
		}

		if (export.equals("local")) {
			custStageModDataset.put("dataset",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_bar); // TODO changer le
																									// chemin de fichier
																									// de
																									// donnÃ©es
		}

		customStageModelConfiguration.add(custStageModDataset);

		JSONObject custStageModStage = new JSONObject();
		custStageModStage.put("stageId", "polygon_geo");
		if (export.equals("VM") || export.equals("VM_local")) {
			custStageModStage.put("dataset", "http://" + hote + "/MappeChest/" + file_name_polygon); // TODO changer le
																										// chemin de
																										// fichier de
																										// donnÃ©es
			customStageModelConfiguration.add(custStageModStage);

			// rejout des fichiers avec les stagees de chaque mesure
//	  liste_fichier_mesure.get(measure.get(0))
			int ct_mes = 1;
			while (ct_mes < measure.size()) {
				System.out.println(" -> here <- ");
				JSONObject custStageModStage1 = new JSONObject();
				custStageModStage1.put("stageId", stage_bar[ct_mes - 1][1]);
				File f_bar_ = new File(liste_fichier_mesure.get(measure.get(ct_mes)) + ".json");
				custStageModStage1.put("dataset", "http://" + hote + "/MappeChest/" + f_bar_.getName());
				customStageModelConfiguration.add(custStageModStage1);
				ct_mes++;
			}

		}

		if (export.equals("local")) {
			custStageModStage.put("dataset",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_polygon); // TODO changer le
																										// chemin de
																										// fichier
																										// de donnÃ©es
		}

//  customStageModelConfiguration.add(custStageModStage);

		chartConfiguration.put("customStageModelConfiguration", customStageModelConfiguration);

		/*
		 * ------------------- Fin Creation custom feature for multiStage
		 * ---------------------
		 */

//	chartConfiguration.put("customFeatureConfiguration", customFeatureConfiguration);

		chartConfiguration.put("themeChartScaling", 0.95);
		chartConfiguration.put("latitudeFieldName", "");
		chartConfiguration.put("longitudeFieldName", "");

		JSONArray tooltipConfiguration = new JSONArray();

		chartConfiguration.put("tooltipConfiguration", tooltipConfiguration);
		chartConfiguration.put("boundaryData", null);
		chartConfiguration.put("boundaryDataIndexFieldName", null);

		JSONObject boundaryColorSequence = new JSONObject();

		chartConfiguration.put("boundaryColorSequence", boundaryColorSequence);

		JSONObject webGLPointStyle = new JSONObject();
		webGLPointStyle.put("displayEmphasis", true);
		webGLPointStyle.put("opacity", 100);
		webGLPointStyle.put("pointSize", "3");
		webGLPointStyle.put("isMeters", false);

		chartConfiguration.put("webGLPointStyle", webGLPointStyle);

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		lineStyle.put("opacity", 90);
		lineStyle.put("isMeters", false);

		chartConfiguration.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
		polygonStyle.put("fillOpacity", 0.9);
		polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 1);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.8);
		polygonStyle.put("autosetOpacity", true);

		chartConfiguration.put("polygonStyle", polygonStyle);

		JSONObject heatMapColorSequence = new JSONObject();
		heatMapColorSequence.put("fromColor", "#FFEBE5");
		heatMapColorSequence.put("toColor", "#FF0000");
		heatMapColorSequence.put("slices", 15);
		heatMapColorSequence.put("path", "linear");

		chartConfiguration.put("heatMapColorSequence", heatMapColorSequence);
		JSONObject chartColorConfiguration = new JSONObject();

		chartConfiguration.put("chartColorConfiguration", chartColorConfiguration);

		JSONObject dayOfWeekColorSequence = new JSONObject();
		dayOfWeekColorSequence.put("fromColor", "#7f7f00");
		dayOfWeekColorSequence.put("toColor", "#0000FF");
		dayOfWeekColorSequence.put("path", "linear");
		dayOfWeekColorSequence.put("d3", "category10");

		chartConfiguration.put("dayOfWeekColorSequence", dayOfWeekColorSequence);

		templateJsonData.put("chartConfiguration", chartConfiguration);

		JSONObject mapLayerConfiguration = new JSONObject();
		JSONObject New_Feature_Layer = new JSONObject();
		New_Feature_Layer.put("visible", true);
		mapLayerConfiguration.put("Polygon Stage", New_Feature_Layer);

		// mettre Ã  visible true les stages bar des autres mesures
		int ct_mes1 = 1;
		while (ct_mes1 < measure.size()) {

			mapLayerConfiguration.put("Bar " + stage_bar[ct_mes1 - 1][0], New_Feature_Layer);

			ct_mes1++;
		}

		JSONObject Features = new JSONObject();
		Features.put("visible", true);
		mapLayerConfiguration.put("Features", New_Feature_Layer);

		templateJsonData.put("mapLayerConfiguration", mapLayerConfiguration);
		templateJsonData.put("loading", false);

		JSONObject filterConfiguration = new JSONObject();
//	JSONArray dow= new JSONArray();
//	filterConfiguration.put("dow", dow);
//	JSONArray tod = new JSONArray();
//	filterConfiguration.put("tod", tod);
//	JSONArray theme = new JSONArray();
//	filterConfiguration.put("theme", theme);
//	JSONArray heatmap = new JSONArray();
//	filterConfiguration.put("heatmap", heatmap);
//	JSONArray dateline = new JSONArray();
//	filterConfiguration.put("dateline", dateline);

		templateJsonData.put("filterConfiguration", filterConfiguration);

		JSONObject spatialFilterConfiguration = new JSONObject();
		JSONObject geometry = new JSONObject();
		geometry.put("type", "FeatureCollection");
		JSONArray features = new JSONArray();
		geometry.put("features", features);

		spatialFilterConfiguration.put("geometry", geometry);
		JSONArray filter = new JSONArray();
		spatialFilterConfiguration.put("filter", filter);
		spatialFilterConfiguration.put("circles", new JSONArray());

		templateJsonData.put("spatialFilterConfiguration", spatialFilterConfiguration);

		if (export.equals("local") || export.equals("VM_local")) {
			TEXT.savefile(s_templatejson_File, templateJsonData.toString().replace("},", "},\n"));
		}

		System.out.println("eeeeeeeeeeeeeeeeeeeXXXXXXXXXXXXXXXXXeeeeeeeeeeeeeeeeeeee");
		System.out.println("eeeeeeeeeeeeeeeeeeeXXXXXXXXXXXXXXXXXeeeeeeeeeeeeeeeeeeee");
		System.out.println(templatepath);
		System.out.println("eeeeeeeeeeeeeeeeeeeXXXXXXXXXXXXXXXXXeeeeeeeeeeeeeeeeeeee");
		System.out.println("eeeeeeeeeeeeeeeeeeeXXXXXXXXXXXXXXXXXeeeeeeeeeeeeeeeeeeee");
		System.out.println(templateJsonData.toString().replace("},", "},\n"));
		System.out.println("eeeeeeeeeeeeeeeeeeeXXXXXXXXXXXXXXXXXeeeeeeeeeeeeeeeeeeee");
		if (export.equals("VM")) {
			TEXT.savefile(templatepath,
					templateJsonData.toString().replace("},", "},\n"));
		}

	}

	@SuppressWarnings("unchecked")
	private static void ecrireDonneesTemplatesBarMultiNMeasure(Map<String, List<String>> dim_mem_done,
			String s_templatejson_File, String geoJSONPolygon_file, String geoJSONBar_file, String boundary_file,
			String export, String hote, String disc,String geojsonpath,String templatepath, String dt, int date, Map<String, String> liste_fichier_mesure,
			ArrayList<String> measure, String rules, String[][] niv_mes, ArrayList<String> liste_niveau,
			Map<String, String> liste_legende) throws IOException {

		System.out.println(" DEBUT ECRITURE TEMPLATE N MEASURE MULTI LAYER ");

		System.out.println(liste_fichier_mesure.get(measure.get(0)));

		String html = write_html.getHtml(dimension_value);

		// d'abord on prÃ©pare la strucutre de fichier JSON
		JSONObject templateJsonData = new JSONObject();
		templateJsonData.put("version", 2);
		templateJsonData.put("incidentClusterDistance", 50);
		templateJsonData.put("hotspotDistance", 300);
		templateJsonData.put("hotspotMapMaxValue", 16);
		templateJsonData.put("hotspotVisible", false);
		templateJsonData.put("hotspotOpacity", 100);
		templateJsonData.put("incidentsVisible", true);
		templateJsonData.put("boundariesVisible", false);
		templateJsonData.put("tooltipsVisible", false);

//	JSONObject mapCenter = new JSONObject();
//	mapCenter.put("lat",  48.17341248658084);
//	mapCenter.put("lng", 6.987304687500001);
//	templateJsonData.put("mapCenter",mapCenter);

		JSONObject mapCenter = new JSONObject();
		mapCenter.put("lat", 47.45780853075031);
		mapCenter.put("lng", 1.0546875000000002);
		templateJsonData.put("mapCenter", mapCenter);

		templateJsonData.put("mapZoom", 6);
		templateJsonData.put("titleBarEnabled", true);

		if (export.equals("local")) {
			templateJsonData.put("tileServer", "");
			templateJsonData.put("enterpriseBaseMapId", "0f3f8b1f-fa2f-4e60-b5c4-f6c5ac0a703c");
		}

		if (export.equals("VM") || export.equals("VM_local")) {
			templateJsonData.put("tileServer",
					"https://{s}.base.maps.api.here.com/maptile/2.1/xbasetile/newest/pedestrian.day/{z}/{x}/{y}/256/png8?app_id=t2USz3mzaHBkI1917H45&app_code=NcNYh0n_yCVwOMzHbh-93A&style=default");
			templateJsonData.put("enterpriseBaseMapId", "");
		}

		/* fin config pour la vm */

		templateJsonData.put("preventWorldWrap", false);

		JSONArray wmsLayers = new JSONArray();
		templateJsonData.put("wmsLayers", wmsLayers);

		templateJsonData.put("hideApplicationLogo", true);
		templateJsonData.put("hideFeatureCount", true);
		templateJsonData.put("hideHelp", true);

		JSONArray staticPointLayers = new JSONArray();
		templateJsonData.put("staticPointLayers", staticPointLayers);

		templateJsonData.put("customHelpUrl", "");
		templateJsonData.put("incidentRenderer", "Cluster");
		templateJsonData.put("disableExport", false);
		templateJsonData.put("beta", null);
		templateJsonData.put("timeOfDayChartDisabled", false);
		templateJsonData.put("dayOfWeekChartDisabled", false);
		templateJsonData.put("dateChartDisabled", false);
		templateJsonData.put("heatMapChartDisabled", false);
		templateJsonData.put("timeOfDayChartVisible", false);
		templateJsonData.put("dayOfWeekChartVisible", false);
		templateJsonData.put("dateChartVisible", false);
		templateJsonData.put("heatMapChartVisible", false);

		JSONObject visualTheme = new JSONObject();
		visualTheme.put("name", "Light");
		visualTheme.put("font", "'Roboto Condensed', sans-serif");
		visualTheme.put("titleBarFontColor", "#111111");
		visualTheme.put("titleBarFontSize", "3vmin");
		visualTheme.put("titleBarBackgroundColor", "#ffffff");
		visualTheme.put("chartFontColor", "#111111");
		visualTheme.put("chartBackgroundColor", "#ffffff");
		visualTheme.put("chartTitleBarBackgroundColor", "#f9f9f9");
		visualTheme.put("chartTitleBarFontColor", "#111111");
		visualTheme.put("chartTitleBarFontSize", "1.9vmin");

		templateJsonData.put("visualTheme", visualTheme);

		JSONObject markerColorSequence = new JSONObject();
		markerColorSequence.put("fromColor", "#7fff7f");
		markerColorSequence.put("toColor", "#ff7f7f");
		markerColorSequence.put("slices", 10);
		markerColorSequence.put("path", "clockwise");

		templateJsonData.put("markerColorSequence", markerColorSequence);

		JSONObject markerRingColorSequence = new JSONObject();
		markerRingColorSequence.put("fromColor", "#007f00");
		markerRingColorSequence.put("toColor", "#7f0000");
		markerRingColorSequence.put("slices", 10);
		markerRingColorSequence.put("path", "clockwise");

		templateJsonData.put("markerRingColorSequence", markerRingColorSequence);
		templateJsonData.put("markerTextColor", "#000000");

		JSONObject clusterStyle = new JSONObject();
		clusterStyle.put("fromSize", 40);
		clusterStyle.put("toSize", 40);
		clusterStyle.put("fromFontSize", 12);
		clusterStyle.put("toFontSize", 12);
		clusterStyle.put("style", "traditional");
		clusterStyle.put("thematicSize", 10);

		templateJsonData.put("clusterStyle", clusterStyle);
		templateJsonData.put("hideIncidentCount", false);

		//////////////// pour carte title
		// pour chaque mesure afficher dans la carte

		String contenuMaps_TitleString = new String();

		// Pour ajouter la liste des mesures affichers
		List<String> mesures = dim_mem_done.get("Measures");
		int b = 0;

		for (String measure_name : mesures) {
			if (b == 0) {
				contenuMaps_TitleString += "[";
			}

			contenuMaps_TitleString += measure_name;

			if (b != mesures.size() - 1) {
				contenuMaps_TitleString += " - ";
			}

			if (b == mesures.size() - 1) {
				contenuMaps_TitleString += "]";
			}

			b++;
		}

		// Pour ajouter la liste des dimensions avec des membres unique sans doubler les
		// mesures
		Set<String> dimensions_names = dim_mem_done.keySet();
		boolean first = true;

		for (String dimension_name : dimensions_names) {
			List<String> dim_members = dim_mem_done.get(dimension_name);

			if ((dim_members.size() == 1) && !(dimension_name.contains("Measures"))) {
				if (first) {
					contenuMaps_TitleString += " : " + dimension_name + " " + dim_members;
					first = false;
				} else {
					contenuMaps_TitleString += ", " + dimension_name + " " + dim_members;
				}
			}
		}
		//////////////// Fin pour carte title

		// OBJECTIF : RECUPERER LES NOMS DES NIVEAUX-MESURES

		System.out.println(" NOM DU FEATURE => Bar " + niv_mes[0][0] + " "
				+ niv_mes[0][1].replace(" ", "_").replace(":", "_").toLowerCase());
		templateJsonData.put("incidentBranding",
				"Bar " + niv_mes[0][1].replace(" ", "_").replace(":", "_").toLowerCase() + " " + niv_mes[0][0]); // NOM
																													// DU
																													// FEATURE
																													// DE
																													// BASE

		templateJsonData.put("logoImage1", "");
		templateJsonData.put("logoImage2", "http://www.geosystems.fr/images/logonoir222.png");

		JSONObject chartConfiguration = new JSONObject();

		chartConfiguration.put("version", 2);
		chartConfiguration.put("recordIndex", 1);
		chartConfiguration.put("compressed", true);

		System.out.println(" TEST NOM FICHIER POLYGONE");

		for (String niv : liste_niveau) {
			System.out.println(geoJSONPolygon_file + niv);
		}

		System.out.println(" FIN TEST NOM FICHIER POLYGONE ");

		/* APPEL DES FICHIERS GEOJSON */
//	File f_polygon = null;
//	File f_bar = null;
//	if (date == 0) {
//		f_polygon = new File(geoJSONPolygon_file+".json");
//		f_bar = new File(liste_fichier_mesure.get(measure.get(0))+".json");
//		
//
//		
//	}
//	if (date == 1) {
//		f_polygon = new File(geoJSONPolygon_file+"_"+dt+".json"); 
//		f_bar = new File(geoJSONBar_file+".json");
//	}
//
//	
//	String file_name_polygon = f_polygon.getName();
//	String file_name_bar = f_bar.getName();
//	
//	System.out.println(" -------------- > " + f_bar.getName() );
//	
//
//	
		// récupération du fichier en feature data => le premier dans le tableau niv_mes
		// normalement
		String file_name_bar = new File(
				geoJSONBar_file + niv_mes[0][1].replace(" ", "_").replace(":", "_") + "_" + niv_mes[0][0] + ".json")
						.getName();
		if (export.equals("VM") || export.equals("VM_local")) {
			chartConfiguration.put("featureData", "http://" + hote + "/MappeChest/" + file_name_bar); // TODO changer
																										// le chemin de
																										// fichier de
																										// donnÃ©es
//		chartConfiguration.put("featureData","http://51.38.196.163/MappeChest/" + liste_fichier_mesure.get(measure.get(0))+".json");
		}

		if (export.equals("local")) {
			chartConfiguration.put("featureData",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_bar); // TODO changer le
																									// chemin de fichier
																									// de
																									// donnÃ©es
		}

		chartConfiguration.put("pauseLiveStreaming", false);
		chartConfiguration.put("themeFieldName", null);

		chartConfiguration.put("defaultThemeClassification", null);
		chartConfiguration.put("dateTimeFieldName", null);
		chartConfiguration.put("dateTimeFormat", "");
		chartConfiguration.put("geoprocessingTemplate", false);
		chartConfiguration.put("themeToMarkerIconMap", null);
		chartConfiguration.put("dayOfWeekChartId", "dc-dayweek-chart");
		chartConfiguration.put("timeOfDayChartId", "dc-time-chart");
		chartConfiguration.put("themeChartId", "dc-priority-chart");
		chartConfiguration.put("heatMapChartId", "dc-heatmap-chart");
		chartConfiguration.put("dateLineChartId", "dc-dateline-chart");

		JSONArray customChartConfiguration = new JSONArray(); // TODO, ICI on charge les mini-cartes Ã  afficher

		int d = 0;

		String location = "";

		String mesure_nom = "";
		int j = 0;
		for (String dimension : dim_mem_done.keySet()) {
			if (dimension.equals("Measures")) {
				mesure_nom = dim_mem_done.get(dimension).toString().replace("[", "").replace("]", "");
			}
			j++;
		}

//    System.out.println(" c quoi ce bordel " + mesure_nom);

		// rÃ©cupÃ©ration des stages
//	int stg = 0;
//	String[][] stage_bar = new String[measure.size()-1][2];
//	for (String mes : measure) {
//		if (stg != 0) {
//			stage_bar[stg-1][0] = mes.replace(" ", "_").replace(":", "_").toLowerCase();
//			stage_bar[stg-1][1] = "stage_bar_"+mes.replace(" ", "_").replace(":", "_").toLowerCase();
//			System.out.println(stage_bar[stg-1][1] );
//		}
//		
//		stg++;
//
//	}

		int stg = 0;

//	for (String mes : measure) {
//		if (stg != 0) {
//			stage_bar[stg-1][0] = mes.replace(" ", "_").replace(":", "_").toLowerCase();
//			stage_bar[stg-1][1] = "stage_bar_"+mes.replace(" ", "_").replace(":", "_").toLowerCase();
//			System.out.println(stage_bar[stg-1][1] );
//		}
//		
//		stg++;
//
//	}

		String[][] stage_bar = new String[niv_mes.length - 1][2];
		String[][] default_bar = new String[1][2];
		for (int i = 0; i < niv_mes.length; i++) {
			if (stg != 0) {
				stage_bar[stg - 1][0] = niv_mes[i][1].replace(" ", "_").replace(":", "_").toLowerCase();
				stage_bar[stg - 1][1] = "stage_bar_" + niv_mes[i][1].replace(" ", "_").replace(":", "_").toLowerCase()
						+ "_" + niv_mes[i][0];
//		System.out.println(stage_bar[stg-1][1] );
			} else {
				default_bar[stg][0] = niv_mes[i][1].replace(" ", "_").replace(":", "_").toLowerCase();
				default_bar[stg][1] = "stage_bar_" + niv_mes[i][1].replace(" ", "_").replace(":", "_").toLowerCase()
						+ "_" + niv_mes[i][0];
			}

			stg++;

		}
//	String[][] default_bar = {};
		System.out.println();

		for (int i = 0; i < stage_bar.length; i++) {
			System.out.println(" stage " + i + " : " + stage_bar[i][1]);
		}

		System.out.println(" arrivée ");
		System.out.println();
		for (int i = 0; i < niv_mes.length; i++) {

			System.out.println(niv_mes[i][0] + " " + niv_mes[i][1]);

		}

//	 for (int i=0; i< niv_mes.length; i++) {
//		 System.out.println(niv_mes[i][0]+ " " + niv_mes[i][1]);
//	 }

		int com = 0; // pour récupérer la bonne coloration
		int cc = 0; // compteur pour vérifier si le stage est le feature par defaut
		for (int i = 0; i < niv_mes.length; i++) {
			if (cc != 0) {

				String tmp[] = stage_bar[cc - 1][1].split("_");
				if (stage_bar[cc - 1][1].split("_")[tmp.length - 1].equals("departement")) {

					com++;

				}

				System.out.println(" indicateur " + i + " theme chart mutli Nmeasure : " + niv_mes[i][1] + " " + com
						+ " " + cc + " ST " + stage_bar[cc - 1][0]);
				System.out.println();

				customChartConfiguration.add(d++, addIndicatorThemeChartMultiNMeasure("_Legend", niv_mes[i][1], com, cc,
						stage_bar[cc - 1][1], niv_mes[i][0], liste_legende));

				System.out.println(" indicateur " + i + "  bar multi chart Nmeasure : " + niv_mes[i][1] + " " + com
						+ " " + cc + " ST " + stage_bar[cc - 1][1] + " " + niv_mes[i][0]);
				System.out.println();

//				PASSER NIVEAUX BAE LEGEND
//				customChartConfiguration.add(d++,addDimensionBarChartNMeasure("_Legend", mes, com, stage_bar[com-1][1]));
				customChartConfiguration.add(d++, addDimensionBarChartMultiNMeasure("_Legend", niv_mes[i][1], com, cc,
						stage_bar[cc - 1][1], niv_mes[i][0]));

				// addDimensionBarChartMultiNMeasure(String dimension_name, String mesure_nom,
				// int com, int cc, String stage, String niveau)
//				addIndicatorThemeChartMultiNMeasure(String indicator_name, String mesure, int com, int cc, String stage, String niveau)
//				String[][] list_couleur_dep = { {"#e5f2ff", "#8f86ff"}, {"#fce5ff", "#e300ff"}, {"#c6fee0", "#07ff00"}, {"#fef4c6", "#d1a200"}, {"#ecf6c7", "#f0fa00"} };
//				String[][] list_couleur_reg = { {"#b1c6db", "#1300ff"}, {"#c8afcb", "#a600ba"}, {"#9cc8b1", "#04a100"}, {"#cec494", "#9d7a00"}, {"#b8bf9e", "#d4dd00"} };

			} else {

				System.out.println(" indicateur  " + i + "  theme chart mutliNmeasure : " + niv_mes[i][1] + " " + com
						+ " " + cc + " ST " + niv_mes[i][0]);
				System.out.println();
				customChartConfiguration.add(d++, addIndicatorThemeChartMultiNMeasure("_Legend", niv_mes[i][1], com, cc,
						"", niv_mes[i][0], liste_legende));
//				customChartConfiguration.add(d++,addDimensionBarChartNMeasure("_Legend", mes, com, ""));

//				 System.out.println(" indicateur bar multi chart Nmeasure : " + niv_mes[i][1] + " "+ com + " "+  cc + " " +stage_bar[cc-1][1] +" "+ stage_bar[cc-1][0]);
				System.out.println();

				customChartConfiguration.add(d++,
						addDimensionBarChartMultiNMeasure("_Legend", niv_mes[i][1], com, cc, "", niv_mes[i][0]));

			}

			cc++;

		}

//	
//	int com = 0;
//	
//	
////	creation des themes/bar en mÃªme temps
//	for (String mes : measure) {
//
//		
//		if (com != 0) {
//			customChartConfiguration.add(d++,addIndicatorThemeChartNMeasure("_Legend", mes, com, stage_bar[com-1][1]));
//			customChartConfiguration.add(d++,addDimensionBarChartNMeasure("_Legend", mes, com, stage_bar[com-1][1]));
//		}
//		else {
//			customChartConfiguration.add(d++,addIndicatorThemeChartNMeasure("_Legend", mes, com, ""));
//			customChartConfiguration.add(d++,addDimensionBarChartNMeasure("_Legend", mes, com, ""));
//		}
//		
//		com++;
//
//	}
//	
		System.out.println(" <mesure> " + mesure_nom);
//	
//	// rajout de la lÃ©gende

//	
//	for (String niv : liste_niveau) {
//		customChartConfiguration.add(d++,addDimensionBarChartMulti("_Legend", mesure_nom, j, niv));
//	}

//	
//	// rajout du dropdownlist des locations
//	customChartConfiguration.add(d++, addDimensionDropDownList(location, d, rules));

		for (String niv : liste_niveau) {
			customChartConfiguration.add(d++, addDimensionDropDownListMulti(location, d, niv));
		}

		System.out.println(" dropdown OK ");

//	// fin d'ajout des chartes des dimensions
//
//	
		chartConfiguration.put("customChartConfiguration", customChartConfiguration);

//	

		JSONArray customFeatureConfiguration = new JSONArray();

//	
//	
//
//	/* ------------------- DÃ©but Creation custom feature for multiStage ---------------------*/
//	

//	for (String niv : liste_niveau) {
//	System.out.println( geoJSONPolygon_file+niv );
//	}

//	addCustomFeatureConfigurationPolygonMulti(String file_name, String mesure_nom, String export, String niveau)

//	File f_polygon = null;
//	File f_bar = null;
//	if (date == 0) {
//		f_polygon = new File(geoJSONPolygon_file+".json");
////		f_bar = new File(geoJSONBar_file+".json");
//		f_bar = new File(liste_fichier_mesure.get(measure.get(0))+".json");
//		
//
//		
//	}
//	if (date == 1) {
//		f_polygon = new File(geoJSONPolygon_file+"_"+dt+".json"); 
//		f_bar = new File(geoJSONBar_file+".json");
//	}
////	File f = new File(geoJSON_file+"_"+dt+".json"); 
////	File f = new File(geoJSON_file);
//	
//	String file_name_polygon = f_polygon.getName();
//	String file_name_bar = f_bar.getName();

//	JSONObject get_custom_polygon = addCustomFeatureConfigurationPolygon(file_name_polygon, mesure_nom, export);
//	customFeatureConfiguration.add(get_custom_polygon);
		for (String niv : liste_niveau) {
			File f_polygon = new File(geoJSONPolygon_file + niv + ".json");
			String file_name_polygon = f_polygon.getName();
			JSONObject get_custom_polygon = addCustomFeatureConfigurationPolygonMulti(file_name_polygon, mesure_nom,
					export, niv);
			customFeatureConfiguration.add(get_custom_polygon);
		}
		System.out.println(" fin ajout geojson polygone ");

//	// rajout des features Ã  part le feature par defaut

		// int ct_mes0 = 1;
//	  while(ct_mes0<measure.size()) {
//		  System.out.println(" -> here feature configuration <- ");
//		  JSONObject get_custom_feature0 = addCustomFeatureConfigurationFeaturesNMeasure(file_name_polygon, stage_bar[ct_mes0-1][0], stage_bar[ct_mes0-1][1]);
//		  
//		  customFeatureConfiguration.add(get_custom_feature0);
//		  ct_mes0++;
//	  }

//	JSONObject get_custom_feature = addCustomFeatureConfigurationFeatures(file_name_polygon, mesure_nom);
//	customFeatureConfiguration.add(get_custom_feature);
//	chartConfiguration.put("customFeatureConfiguration", customFeatureConfiguration);
//	
//

		int ct_mes0 = niv_mes.length - 1;

//	 while(ct_mes0<niv_mes.length) {

		while (ct_mes0 != 0) {

			System.out.println(" stage nb : " + stage_bar.length);
			System.out.println(niv_mes[ct_mes0][0] + " " + niv_mes[ct_mes0][1]);
			System.out.println(" stage bar : " + stage_bar[ct_mes0 - 1][0] + " " + stage_bar[ct_mes0 - 1][1]);

//		 addCustomFeatureConfigurationFeaturesMultiNMeasure(String mesure_nom, String stage_mesure, String niveau, int def)

			JSONObject get_custom_feature = addCustomFeatureConfigurationFeaturesMultiNMeasure(
					stage_bar[ct_mes0 - 1][0], stage_bar[ct_mes0 - 1][1], niv_mes[ct_mes0][0], 1);
//		  
			customFeatureConfiguration.add(get_custom_feature);
			ct_mes0--;
		}

		System.out.println(" 1 ---> " + stage_bar[0][0] + " " + stage_bar[0][1] + " " + niv_mes[0][0]);
		System.out.println(" 2 ---> " + stage_bar[0][0] + " " + stage_bar[0][1] + " " + niv_mes[0][1]);

		JSONObject get_custom_feature0 = addCustomFeatureConfigurationFeaturesMultiNMeasure(stage_bar[0][0],
				stage_bar[0][1], niv_mes[0][0], 0);
		customFeatureConfiguration.add(get_custom_feature0);
//	  customFeatureConfiguration.add(get_custom_feature0);

//	  while(ct_mes0<measure.size()) {
//		  System.out.println(" -> here feature configuration <- ");
//		  JSONObject get_custom_feature0 = addCustomFeatureConfigurationFeaturesNMeasure(file_name_polygon, stage_bar[ct_mes0-1][0], stage_bar[ct_mes0-1][1]);
//		  
//		  customFeatureConfiguration.add(get_custom_feature0);
//		  ct_mes0++;
//	  }
//

//	JSONObject get_custom_feature = addCustomFeatureConfigurationFeatures(file_name_polygon, mesure_nom);
//	customFeatureConfiguration.add(get_custom_feature);

		chartConfiguration.put("customFeatureConfiguration", customFeatureConfiguration);

		JSONArray customAttributeConfiguration = new JSONArray();

		chartConfiguration.put("customAttributeConfiguration", customAttributeConfiguration);

		JSONArray customStageModelLinks = new JSONArray();

		JSONObject custStageModLinksPolygon = new JSONObject();
		custStageModLinksPolygon.put("stageModelA", "stage_polygon_departement");
		custStageModLinksPolygon.put("stageModelAKey", "_reg");
		custStageModLinksPolygon.put("stageModelB", "stage_polygon_region");
		custStageModLinksPolygon.put("stageModelBKey", "_Location");
		customStageModelLinks.add(custStageModLinksPolygon);

//	JSONObject custStageModLinks = new JSONObject();
//	custStageModLinks.put("stageModelAKey", "_Location");
//	custStageModLinks.put("stageModelB", "polygon_geo");
//	custStageModLinks.put("stageModelBKey", "_Location");
//	customStageModelLinks.add(custStageModLinks);

//	// rajout des liens en fonction des mesures utilisÃ©es
		System.out.println();
//	for (String[] m:stage_bar) {
//		JSONObject custStageModLinksMes = new JSONObject();
//		System.out.println(" -> creation link stage " + m[1]);
//		custStageModLinksMes.put("stageModelA", m[1]);
//		custStageModLinksMes.put("stageModelAKey", "_Location");
//		custStageModLinksMes.put("stageModelB", "polygon_geo");
//		custStageModLinksMes.put("stageModelBKey", "_Location");
//		customStageModelLinks.add(custStageModLinksMes);
//	}

		for (String niv : liste_niveau) {
			JSONObject custStageModLinksMes = new JSONObject();
			System.out.println(" -> creation link stage default");
			custStageModLinksMes.put("stageModelA", "stage_polygon_" + niv);
			custStageModLinksMes.put("stageModelAKey", "_Location");

			if (niv.equals("region")) {
				custStageModLinksMes.put("stageModelBKey", "_reg");
			} else {
				custStageModLinksMes.put("stageModelBKey", "_Location");
			}

//			custStageModLinksMes.put("stageModelBKey", "_Location");
			customStageModelLinks.add(custStageModLinksMes);
		}

		System.out.println();
		for (String niv : liste_niveau) {

//		for 

			for (String[] m : stage_bar) {
				JSONObject custStageModLinksMes = new JSONObject();
				System.out.println(niv + " -> creation link stage " + m[0] + " " + m[1]);
				custStageModLinksMes.put("stageModelA", "stage_polygon_" + niv);
				custStageModLinksMes.put("stageModelAKey", "_Location");

				String[] tmp = m[1].split("_");

				if (niv.equals("departement") && tmp[tmp.length - 1].equals("region")) {
					custStageModLinksMes.put("stageModelB", m[1]);
					custStageModLinksMes.put("stageModelBKey", "_dep");
				}

				if (niv.equals("region") && tmp[tmp.length - 1].equals("departement")) {
					custStageModLinksMes.put("stageModelB", m[1]);
					custStageModLinksMes.put("stageModelBKey", "_reg");
				}

				if (niv.equals("departement") && tmp[tmp.length - 1].equals("departement")) {
					custStageModLinksMes.put("stageModelB", m[1]);
					custStageModLinksMes.put("stageModelBKey", "_Location");
				}

				if (niv.equals("region") && tmp[tmp.length - 1].equals("region")) {
					custStageModLinksMes.put("stageModelB", m[1]);
					custStageModLinksMes.put("stageModelBKey", "_Location");
				}

				customStageModelLinks.add(custStageModLinksMes);
			}
		}

		chartConfiguration.put("customStageModelLinks", customStageModelLinks);

		JSONArray customStageModelConfiguration = new JSONArray();
		JSONObject custStageModDataset = new JSONObject();

		System.out.println(" -> file name bar : " + file_name_bar);

		if (export.equals("VM") || export.equals("VM_local")) {
			custStageModDataset.put("dataset", "http://" + hote + "/MappeChest/" + file_name_bar); // TODO changer le
																									// chemin de fichier
																									// de donnÃ©es
		}

		if (export.equals("local")) {
			custStageModDataset.put("dataset",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_bar); // TODO changer le
																									// chemin de fichier
																									// de
																									// donnÃ©es
		}

		customStageModelConfiguration.add(custStageModDataset);

//  JSONObject custStageModStage = new JSONObject();
//  custStageModStage.put("stageId", "polygon_geo");
//  if (export.equals("VM") || export.equals("VM_local")) {
//	  custStageModStage.put("dataset","http://51.38.196.163/MappeChest/" + file_name_polygon); // TODO changer le chemin de fichier de donnÃ©es
//	  customStageModelConfiguration.add(custStageModStage);

		for (String niv : liste_niveau) {

			File f_polygon = new File(geoJSONPolygon_file + niv + ".json");
			String file_name_polygon = f_polygon.getName();

			JSONObject custStageModStage = new JSONObject();

			custStageModStage.put("stageId", "stage_polygon_" + niv);

			if (export.equals("VM") || export.equals("VM_local")) {
				custStageModStage.put("dataset", "http://" + hote + "/MappeChest/" + file_name_polygon); // TODO
																											// changer
																											// le chemin
																											// de
																											// fichier
																											// de
																											// donnÃ©es
				customStageModelConfiguration.add(custStageModStage);

			}
		}

		// rejout des fichiers avec les stagees de chaque mesure
		int ct_mes = 1;
		while (ct_mes < niv_mes.length) {
//		  System.out.println(" -> here <- " + liste_fichier_mesure.get(measure.get(ct_mes-1)) );
			JSONObject custStageModStage1 = new JSONObject();
			custStageModStage1.put("stageId", stage_bar[ct_mes - 1][1]);

			System.out.println(geoJSONBar_file + " <- " + niv_mes[ct_mes][0] + " "
					+ niv_mes[ct_mes][1].replace(" ", "_").replace(":", "_").toLowerCase());

//		  File	f_bar_ = new File(liste_fichier_mesure.get(measure.get(ct_mes))+".json");
			File f_bar_ = new File(
					geoJSONBar_file + niv_mes[ct_mes][1].replace(" ", "_").replace(":", "_").toLowerCase() + "_"
							+ niv_mes[ct_mes][0] + ".json");
			custStageModStage1.put("dataset", "http://" + hote + "/MappeChest/" + f_bar_.getName());
			customStageModelConfiguration.add(custStageModStage1);
			ct_mes++;

		}

//
//  if (export.equals("local")) {
//	custStageModStage.put("dataset","https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_polygon); // TODO changer le chemin de fichier de donnÃ©es
//  }

		chartConfiguration.put("customStageModelConfiguration", customStageModelConfiguration);

//	/* ------------------- Fin Creation custom feature for multiStage ---------------------*/

		chartConfiguration.put("themeChartScaling", 0.95);
		chartConfiguration.put("latitudeFieldName", "");
		chartConfiguration.put("longitudeFieldName", "");

		JSONArray tooltipConfiguration = new JSONArray();

		chartConfiguration.put("tooltipConfiguration", tooltipConfiguration);
		chartConfiguration.put("boundaryData", null);
		chartConfiguration.put("boundaryDataIndexFieldName", null);

		JSONObject boundaryColorSequence = new JSONObject();

		chartConfiguration.put("boundaryColorSequence", boundaryColorSequence);

		JSONObject webGLPointStyle = new JSONObject();
		webGLPointStyle.put("displayEmphasis", true);
//	webGLPointStyle.put("opacity",100);
		webGLPointStyle.put("opacity", 49);
		webGLPointStyle.put("pointSize", "3");
		webGLPointStyle.put("isMeters", false);

		chartConfiguration.put("webGLPointStyle", webGLPointStyle);

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
//	lineStyle.put("opacity",90);
		lineStyle.put("opacity", 49);
		lineStyle.put("isMeters", false);

		chartConfiguration.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
//	polygonStyle.put("fillOpacity",0.9);
		polygonStyle.put("fillOpacity", 0.49);
		polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 0.69);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.39);
		polygonStyle.put("autosetOpacity", true);

		chartConfiguration.put("polygonStyle", polygonStyle);

		JSONObject heatMapColorSequence = new JSONObject();
		heatMapColorSequence.put("fromColor", "#FFEBE5");
		heatMapColorSequence.put("toColor", "#FF0000");
		heatMapColorSequence.put("slices", 15);
		heatMapColorSequence.put("path", "linear");

		chartConfiguration.put("heatMapColorSequence", heatMapColorSequence);
		JSONObject chartColorConfiguration = new JSONObject();

		chartConfiguration.put("chartColorConfiguration", chartColorConfiguration);

		JSONObject dayOfWeekColorSequence = new JSONObject();
		dayOfWeekColorSequence.put("fromColor", "#7f7f00");
		dayOfWeekColorSequence.put("toColor", "#0000FF");
		dayOfWeekColorSequence.put("path", "linear");
		dayOfWeekColorSequence.put("d3", "category10");

		chartConfiguration.put("dayOfWeekColorSequence", dayOfWeekColorSequence);

		templateJsonData.put("chartConfiguration", chartConfiguration);

		JSONObject mapLayerConfiguration = new JSONObject();
		int index = 2;
		for (String niv : liste_niveau) {
			JSONObject New_Feature_Layer = new JSONObject();
			New_Feature_Layer.put("visible", true);
			New_Feature_Layer.put("zIndex", index);
			mapLayerConfiguration.put("Polygon " + niv, New_Feature_Layer);
			index++;
		}
		System.out.println(" zIndex value after polygon : " + index);

//	mapLayerConfiguration.put("Polygon Stage", New_Feature_Layer);

		// mettre Ã  visible true les stages bar des autres mesures System.out.println("
		// NOM DU FEATURE => Bar " + niv_mes[0][0] + " " + niv_mes[0][1].replace(" ",
		// "_").replace(":", "_").toLowerCase());
//	 int ct_mes1 = 1;
//	  while(ct_mes1<niv_mes.length) {

		int ct_mes1 = niv_mes.length - 1;
		while (ct_mes1 != 0) {
			System.out.println(" mettre à jour les indes : " + index);
			JSONObject New_Feature_Layer = new JSONObject();
			New_Feature_Layer.put("visible", true);
			New_Feature_Layer.put("zIndex", index);

			mapLayerConfiguration.put("Bar " + niv_mes[ct_mes1][1].replace(" ", "_").replace(":", "_").toLowerCase()
					+ " " + niv_mes[ct_mes1][0], New_Feature_Layer);

			ct_mes1--;
			index++;

		}

		JSONObject New_Feature_Layer = new JSONObject();
		New_Feature_Layer.put("visible", true);
		New_Feature_Layer.put("zIndex", index);
		mapLayerConfiguration.put(
				"Bar " + niv_mes[0][1].replace(" ", "_").replace(":", "_").toLowerCase() + " " + niv_mes[0][0],
				New_Feature_Layer);

//	JSONObject Features = new JSONObject();
//	Features.put("visible", true);
//	mapLayerConfiguration.put("Features", New_Feature_Layer);
//	

		templateJsonData.put("mapLayerConfiguration", mapLayerConfiguration);
		templateJsonData.put("loading", false);

		JSONObject filterConfiguration = new JSONObject();

		templateJsonData.put("filterConfiguration", filterConfiguration);

		JSONObject spatialFilterConfiguration = new JSONObject();
		JSONObject geometry = new JSONObject();
		geometry.put("type", "FeatureCollection");
		JSONArray features = new JSONArray();
		geometry.put("features", features);

		spatialFilterConfiguration.put("geometry", geometry);
		JSONArray filter = new JSONArray();
		spatialFilterConfiguration.put("filter", filter);
		spatialFilterConfiguration.put("circles", new JSONArray());

		templateJsonData.put("spatialFilterConfiguration", spatialFilterConfiguration);

		if (export.equals("local") || export.equals("VM_local")) {
			TEXT.savefile(s_templatejson_File, templateJsonData.toString().replace("},", "},\n"));
		}

		if (export.equals("VM")) {
			TEXT.savefile(templatepath,
					templateJsonData.toString().replace("},", "},\n"));
		}

	}

//#7f7f7f  #e6f2f0

// Sans la fonctionnalité Avant et Arrière

	@SuppressWarnings("unchecked")
	private static void ecrireDonneesTemplates2MultiBar(Map<String, List<String>> dim_mem_done,
			String s_templatejson_File, ArrayList<String> liste_niveau, String geoJSONPolygon_departement,
			String geoJSONPolygon_region, String geoJSONBar_departement, String geoJSONBar_region, String boundary_file,
			String export, String hote, String disc,String geojsonpath,String templatepath, String dt, int date, Map<String, String> liste_legende)
			throws IOException {

//	for(String val : dimension_value) {
//		System.out.println(" here " + val);
//	}
//	System.out.println("  ----  ");

		System.out.println(" template multi bar ");

		String html = write_html.getHtml(dimension_value);

//	System.out.println(html);
//	System.out.println("  ----  ");
		System.out.println(" <<<<<<<<<<<<< --- POLYGON BAR 2 NIVAU ---- >>>>>>>>>>>>>");

		// d'abord on prÃ©pare la strucutre de fichier JSON
		JSONObject templateJsonData = new JSONObject();
		templateJsonData.put("version", 2);
		templateJsonData.put("incidentClusterDistance", 50);
		templateJsonData.put("hotspotDistance", 300);
		templateJsonData.put("hotspotMapMaxValue", 16);
		templateJsonData.put("hotspotVisible", false);
		templateJsonData.put("hotspotOpacity", 100);
		templateJsonData.put("incidentsVisible", true);
		templateJsonData.put("boundariesVisible", false);
		templateJsonData.put("tooltipsVisible", false);

		JSONObject mapCenter = new JSONObject();
		mapCenter.put("lat", 47.45780853075031);
		mapCenter.put("lng", 1.0546875000000002);
		templateJsonData.put("mapCenter", mapCenter);
		templateJsonData.put("mapZoom", 7);
		templateJsonData.put("titleBarEnabled", true);

		if (export.equals("local")) {
			templateJsonData.put("tileServer", "");
			templateJsonData.put("enterpriseBaseMapId", "0f3f8b1f-fa2f-4e60-b5c4-f6c5ac0a703c");
		}

		if (export.equals("VM") || export.equals("VM_local")) {
			templateJsonData.put("tileServer",
					"https://{s}.base.maps.api.here.com/maptile/2.1/xbasetile/newest/pedestrian.day/{z}/{x}/{y}/256/png8?app_id=t2USz3mzaHBkI1917H45&app_code=NcNYh0n_yCVwOMzHbh-93A&style=default");
			templateJsonData.put("enterpriseBaseMapId", "");
		}

		/* config en local */
//
//	templateJsonData.put("tileServer","");	
//	templateJsonData.put("enterpriseBaseMapId","204b67da-6927-4fa2-a549-a346a4e5a15c");
//	templateJsonData.put("enterpriseBaseMapId","0f3f8b1f-fa2f-4e60-b5c4-f6c5ac0a703c");

		/* fin config en local */

		/* config pour la vm */

//	templateJsonData.put("tileServer","https://{s}.base.maps.api.here.com/maptile/2.1/xbasetile/newest/pedestrian.day/{z}/{x}/{y}/256/png8?app_id=t2USz3mzaHBkI1917H45&app_code=NcNYh0n_yCVwOMzHbh-93A&style=default");	
//	templateJsonData.put("enterpriseBaseMapId","");

		/* fin config pour la vm */

		templateJsonData.put("preventWorldWrap", false);

		JSONArray wmsLayers = new JSONArray();
		templateJsonData.put("wmsLayers", wmsLayers);
		templateJsonData.put("hideApplicationLogo", true);
		templateJsonData.put("hideFeatureCount", true);
		templateJsonData.put("hideHelp", true);

		JSONArray staticPointLayers = new JSONArray();
		templateJsonData.put("staticPointLayers", staticPointLayers);
		templateJsonData.put("customHelpUrl", "");
		templateJsonData.put("incidentRenderer", "Cluster");
		templateJsonData.put("disableExport", false);
		templateJsonData.put("beta", null);
		templateJsonData.put("timeOfDayChartDisabled", false);
		templateJsonData.put("dayOfWeekChartDisabled", false);
		templateJsonData.put("dateChartDisabled", false);
		templateJsonData.put("heatMapChartDisabled", false);
		templateJsonData.put("timeOfDayChartVisible", false);
		templateJsonData.put("dayOfWeekChartVisible", false);
		templateJsonData.put("dateChartVisible", false);
		templateJsonData.put("heatMapChartVisible", false);

		JSONObject visualTheme = new JSONObject();
		visualTheme.put("name", "Light");
		visualTheme.put("font", "'Roboto Condensed', sans-serif");
		visualTheme.put("titleBarFontColor", "#111111");
		visualTheme.put("titleBarFontSize", "3vmin");
		visualTheme.put("titleBarBackgroundColor", "#ffffff");
		visualTheme.put("chartFontColor", "#111111");
		visualTheme.put("chartBackgroundColor", "#ffffff");
		visualTheme.put("chartTitleBarBackgroundColor", "#f9f9f9");
		visualTheme.put("chartTitleBarFontColor", "#111111");
		visualTheme.put("chartTitleBarFontSize", "1.9vmin");

		templateJsonData.put("visualTheme", visualTheme);

		JSONObject markerColorSequence = new JSONObject();
		markerColorSequence.put("fromColor", "#7fff7f");
		markerColorSequence.put("toColor", "#ff7f7f");
		markerColorSequence.put("slices", 10);
		markerColorSequence.put("path", "clockwise");

		templateJsonData.put("markerColorSequence", markerColorSequence);

		JSONObject markerRingColorSequence = new JSONObject();
		markerRingColorSequence.put("fromColor", "#007f00");
		markerRingColorSequence.put("toColor", "#7f0000");
		markerRingColorSequence.put("slices", 10);
		markerRingColorSequence.put("path", "clockwise");

		templateJsonData.put("markerRingColorSequence", markerRingColorSequence);
		templateJsonData.put("markerTextColor", "#000000");

		JSONObject clusterStyle = new JSONObject();
		clusterStyle.put("fromSize", 40);
		clusterStyle.put("toSize", 40);
		clusterStyle.put("fromFontSize", 12);
		clusterStyle.put("toFontSize", 12);
		clusterStyle.put("style", "traditional");
		clusterStyle.put("thematicSize", 10);

		templateJsonData.put("clusterStyle", clusterStyle);
		templateJsonData.put("hideIncidentCount", false);

		//////////////// pour carte title
		// pour chaque mesure afficher dans la carte

		String contenuMaps_TitleString = new String();

		// Pour ajouter la liste des mesures affichers
		List<String> mesures = dim_mem_done.get("Measures");
		int b = 0;

		for (String measure_name : mesures) {
			if (b == 0) {
				contenuMaps_TitleString += "[";
			}

			contenuMaps_TitleString += measure_name;

			if (b != mesures.size() - 1) {
				contenuMaps_TitleString += " - ";
			}

			if (b == mesures.size() - 1) {
				contenuMaps_TitleString += "]";
			}

			b++;
		}

		// Pour ajouter la liste des dimensions avec des membres unique sans doubler les
		// mesures
		Set<String> dimensions_names = dim_mem_done.keySet();
		boolean first = true;

		for (String dimension_name : dimensions_names) {
			List<String> dim_members = dim_mem_done.get(dimension_name);

			if ((dim_members.size() == 1) && !(dimension_name.contains("Measures"))) {
				if (first) {
					contenuMaps_TitleString += " : " + dimension_name + " " + dim_members;
					first = false;
				} else {
					contenuMaps_TitleString += ", " + dimension_name + " " + dim_members;
				}
			}
		}
		//////////////// Fin pour carte title

		templateJsonData.put("incidentBranding", "Features"); // TODO peut Ãªtre c'est le titre de la carte
		templateJsonData.put("logoImage1", "");
		templateJsonData.put("logoImage2", "http://www.geosystems.fr/images/logonoir222.png");

		JSONObject chartConfiguration = new JSONObject();

		chartConfiguration.put("version", 2);
		chartConfiguration.put("recordIndex", 1);
		chartConfiguration.put("compressed", true);

//	System.out.println("+++++++++++++ > " + geoJSON_file);
//	System.out.println(" =============== > " + s_templatejson_File);

		/* APPEL DES FICHIERS GEOJSON */
		File f_polygon_departement = null;
		File f_polygon_region = null;
		File f_bar_departement = null;
		File f_bar_region = null;
		if (date == 0) {
			f_polygon_departement = new File(geoJSONPolygon_departement + ".json");
			f_polygon_region = new File(geoJSONPolygon_region + ".json");
			f_bar_departement = new File(geoJSONBar_departement + ".json");
			f_bar_region = new File(geoJSONBar_region + ".json");
		}
		if (date == 1) {
			f_polygon_departement = new File(geoJSONPolygon_departement + "_" + dt + ".json");
			f_polygon_region = new File(geoJSONPolygon_region + "_" + dt + ".json");
			f_bar_departement = new File(geoJSONBar_departement + ".json");
			f_bar_region = new File(geoJSONBar_region + ".json");
		}
//	File f = new File(geoJSON_file+"_"+dt+".json"); 
//	File f = new File(geoJSON_file);

		String file_name_polygon_departement = f_polygon_departement.getName();
		String file_name_polygon_region = f_polygon_region.getName();
		String file_name_bar_departement = f_bar_departement.getName();
		String file_name_bar_region = f_bar_region.getName();
		// file_name = (new File(f.getParent()).getName()) + "/" + file_name ;

//	System.out.println(" -------------- > " + f_bar.getName() );

		if (export.equals("VM") || export.equals("VM_local")) {
			chartConfiguration.put("featureData", "http://" + hote + "/MappeChest/" + file_name_bar_departement); // TODO
																													// changer
																													// le
																													// chemin
																													// de
																													// fichier
																													// de
																													// donnÃ©es
		}

		if (export.equals("local")) {
			chartConfiguration.put("featureData",
					"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name_bar_departement); // TODO
																												// changer
																												// le
																												// chemin
																												// de
																												// fichier
																												// de
																												// donnÃ©es
		}

		chartConfiguration.put("pauseLiveStreaming", false);
		chartConfiguration.put("themeFieldName", null);

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",5);
//	defaultThemeClassification.put("precision",0);
//	defaultThemeClassification.put("min",null);
//	defaultThemeClassification.put("max",null);

		chartConfiguration.put("defaultThemeClassification", null);
		chartConfiguration.put("dateTimeFieldName", null);
		chartConfiguration.put("dateTimeFormat", "");
		chartConfiguration.put("geoprocessingTemplate", false);
		chartConfiguration.put("themeToMarkerIconMap", null);
		chartConfiguration.put("dayOfWeekChartId", "dc-dayweek-chart");
		chartConfiguration.put("timeOfDayChartId", "dc-time-chart");
		chartConfiguration.put("themeChartId", "dc-priority-chart");
		chartConfiguration.put("heatMapChartId", "dc-heatmap-chart");
		chartConfiguration.put("dateLineChartId", "dc-dateline-chart");

		JSONArray customChartConfiguration = new JSONArray(); // TODO, ICI on charge les mini-cartes Ã  afficher

		int d = 0;

		// Pour ajouter les chartes pour les mesures
//	customChartConfiguration.add(d++,addMesuresThemeCharts(mesures, d));
		/*
		 * if (mesures.size()>1) { // charte combo pour choisir les mesure //
		 * customChartConfiguration.add(d++, addMesureComboChart(d)); // }
		 * 
		 * // charte thÃ¨me pour chaque mesure for (String mesure : mesures) {
		 * customChartConfiguration.add(d++,addMesureThemeChart(mesure, d)); } }
		 */
		String location = "";

		String mesure_nom = "";
		int j = 0;
		for (String dimension : dim_mem_done.keySet()) {
			if (dimension.equals("Measures")) {
				mesure_nom = dim_mem_done.get(dimension).toString().replace("[", "").replace("]", "");
			}
			j++;
		}

		// pour ajouter les chartes pour chaque dimension
//	JSONObject get_custom_html = null;
//	for (String dimension : dim_mem_done.keySet()){
//		System.out.println("-> dimension membre count " + dimension + " : " + dim_mem_done.get(dimension).size());
//		System.out.println("==> " + dim_mem_done.get(dimension).toString().replace("[", "").replace("]", ""));

		/*
		 * !!!!!!!!!!!! A gÃ©rer dans le cas oÃ¹ on a plusieurs mesures
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!
		 */

//		if (!dimension.equals("Measures")) {
//			System.out.println(" <dimension> " + dimension);
//			customChartConfiguration.add(d++,addIndicatorThemeChart(dimension, d));
//			System.out.println(" <mesure> " + mesure_nom);
//			customChartConfiguration.add(d++,addDimensionBarChart(dimension, mesure_nom, d));

		// RAJOUTER HTML DISPLAY
//			get_custom_html = addCustomFeatureConfigurationHtml(html, dimension);
//		}

//	}

		System.out.println(" creation theme indicator");

//	for (String)

//	customChartConfiguration.add(d++,addIndicatorThemeChart("_Legend", d));
		customChartConfiguration.add(d++, addIndicatorThemeChartDepartement("_Legend", d));
		customChartConfiguration.add(d++, addIndicatorThemeChartRegion("_Legend", d));

		System.out.println(" fin creation theme indicator");

		System.out.println(" <mesure> " + j);

		System.out.println(" creation bar legend ");

		for (String niv : liste_niveau) {

			customChartConfiguration.add(d++, addDimensionRowChartMulti("_Legend", 0, mesure_nom, niv));
			customChartConfiguration.add(d++, addPolygonDataTableMulti("_Location", mesure_nom, 0, niv, liste_legende));
			customChartConfiguration.add(d++, addDimensionBarChartMulti("_Legend", mesure_nom, j, niv));
		}

//	customChartConfiguration.add(d++,addDimensionBarChart("_Legend", mesure_nom, j));
//	customChartConfiguration.add(d++,addDimensionBarChartDepartement("_Legend", mesure_nom, j));
//	customChartConfiguration.add(d++,addDimensionBarChartRegion("_Legend", mesure_nom, j));

		System.out.println(" fin creation bar legend ");

		System.out.println(" creation dropdown liste ");

		for (String niv : liste_niveau) {
			customChartConfiguration.add(d++, addDimensionDropDownListMulti(location, d, niv));
		}
//	customChartConfiguration.add(d++, addDimensionDropDownList(location, d));
//	customChartConfiguration.add(d++, addDimensionDropDownListDepartement(location, d));
//	customChartConfiguration.add(d++, addDimensionDropDownListRegion(location, d));

		System.out.println(" fin creation dropdown liste ");

		// fin d'ajout des chartes des dimensions

		chartConfiguration.put("customChartConfiguration", customChartConfiguration);

		JSONArray customFeatureConfiguration = new JSONArray();

//	"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b |||||||||||||||  file_name_polygon

		/*
		 * ------------------- DÃ©but Creation custom feature for multiStage
		 * ---------------------
		 */

//	
//	String file_name_polygon_departement = f_polygon_departement.getName();
//	String file_name_polygon_region = f_polygon_region.getName();
//	String file_name_bar_departement = f_bar_departement.getName();
//	String file_name_bar_region = f_bar_region.getName();

//	for (String niv : liste_niveau) {
//		JSONObject get_custom_feature = addCustomFeatureConfigurationFeaturesMulti(file_name_bar_departement, mesure_nom, niv);
//		customFeatureConfiguration.add(get_custom_feature);
//		JSONObject get_custom_polygon_dep = addCustomFeatureConfigurationPolygon(file_name_polygon_departement, mesure_nom, export);
//		customFeatureConfiguration.add(get_custom_polygon_dep);
//	}

		JSONObject get_custom_polygon_dep = addCustomFeatureConfigurationPolygonMulti(file_name_polygon_departement,
				mesure_nom, export, "departement");
		customFeatureConfiguration.add(get_custom_polygon_dep);

		JSONObject get_custom_polygon_reg = addCustomFeatureConfigurationPolygonMulti(file_name_polygon_region,
				mesure_nom, export, "region");
		customFeatureConfiguration.add(get_custom_polygon_reg);

		JSONObject get_custom_feature_reg = addCustomFeatureConfigurationFeaturesMulti(file_name_bar_region, mesure_nom,
				"region");
		customFeatureConfiguration.add(get_custom_feature_reg);

		JSONObject get_custom_feature_dep = addCustomFeatureConfigurationFeaturesMulti(file_name_bar_departement,
				mesure_nom, "departement");
		customFeatureConfiguration.add(get_custom_feature_dep);

//	JSONObject get_custom_polygon_boundary = addCustomFeatureConfigurationPolygonBoundary(file_name_polygon, mesure_nom, export);
//	customFeatureConfiguration.add(get_custom_feature);

//	customFeatureConfiguration.add(get_custom_html);

		chartConfiguration.put("customFeatureConfiguration", customFeatureConfiguration);

		JSONArray customAttributeConfiguration = new JSONArray();

		chartConfiguration.put("customAttributeConfiguration", customAttributeConfiguration);

//	"customStageModelLinks":
//		[
//		{"stageModelAKey":"_Location","stageModelB":"stage_polygon_departement","stageModelBKey":"_Location"},
//		{"stageModelA":"stage_polygon_departement","stageModelAKey":"_reg","stageModelB":"stage_polygon_region","stageModelBKey":"_Location"},
//		{"stageModelA":"stage_bar_region","stageModelAKey":"_ref","stageModelBKey":"_ref"}
//		],

		JSONArray customStageModelLinks = new JSONArray();
		JSONObject custStageModLinks1 = new JSONObject();
		custStageModLinks1.put("stageModelAKey", "_Location");
		custStageModLinks1.put("stageModelB", "stage_polygon_departement");
		custStageModLinks1.put("stageModelBKey", "_Location");
		customStageModelLinks.add(custStageModLinks1);

		JSONObject custStageModLinks2 = new JSONObject();
		custStageModLinks2.put("stageModelA", "stage_polygon_departement");
		custStageModLinks2.put("stageModelAKey", "_reg");
		custStageModLinks2.put("stageModelB", "stage_polygon_region");
		custStageModLinks2.put("stageModelBKey", "_Location");
		customStageModelLinks.add(custStageModLinks2);

		JSONObject custStageModLinks3 = new JSONObject();
		custStageModLinks3.put("stageModelA", "stage_bar_region");
		custStageModLinks3.put("stageModelAKey", "_ref");
		custStageModLinks3.put("stageModelBKey", "_ref");
		customStageModelLinks.add(custStageModLinks3);

		JSONObject custStageModLinks4 = new JSONObject();
		custStageModLinks4.put("stageModelA", "stage_polygon_departement");
		custStageModLinks4.put("stageModelAKey", "_Location");
		custStageModLinks4.put("stageModelB", "stage_bar_region");
		custStageModLinks4.put("stageModelBKey", "_dep");
		customStageModelLinks.add(custStageModLinks4);

		JSONObject custStageModLinks5 = new JSONObject();
		custStageModLinks5.put("stageModelA", "stage_polygon_region");
		custStageModLinks5.put("stageModelAKey", "_Location");
		custStageModLinks5.put("stageModelB", "stage_bar_region");
		custStageModLinks5.put("stageModelBKey", "_Location");
		customStageModelLinks.add(custStageModLinks5);

		JSONObject custStageModLinks6 = new JSONObject();
		custStageModLinks6.put("stageModelA", "stage_polygon_region");
		custStageModLinks6.put("stageModelAKey", "_Location");
//	custStageModLinks6.put("stageModelB", "stage_bar_departement");
		custStageModLinks6.put("stageModelBKey", "_reg");
		customStageModelLinks.add(custStageModLinks6);

		chartConfiguration.put("customStageModelLinks", customStageModelLinks);

		JSONArray customStageModelConfiguration = new JSONArray();
		JSONObject custStageModDataset = new JSONObject();

//	"customStageModelConfiguration":
//		[
//		{"dataset":"http://51.38.196.163/MappeChest/geoJSON_Bar_departement.json"},
//		{"stageId":"stage_bar_region","dataset":"http://51.38.196.163/MappeChest/geoJSON_Bar_region.json"},
//		{"stageId":"stage_polygon_region","dataset":"http://51.38.196.163/MappeChest/geoJSON_Polygon_region.json"},
//		{"stageId":"stage_polygon_departement","dataset":"http://51.38.196.163/MappeChest/geoJSON_Polygon_departement.json"}
//		],
//	String file_name_polygon_departement = f_polygon_departement.getName();
//	String file_name_polygon_region = f_polygon_region.getName();
//	String file_name_bar_departement = f_bar_departement.getName();
//	String file_name_bar_region = f_bar_region.getName();

		if (export.equals("VM") || export.equals("VM_local")) {
			custStageModDataset.put("dataset", "http://" + hote + "/MappeChest/" + file_name_bar_departement);
//		custStageModDataset.put("dataset","http://51.38.196.163/MappeChest/" + file_name_bar_region);
//		custStageModDataset.put("dataset","http://51.38.196.163/MappeChest/" + file_name_polygon_departement);
//		custStageModDataset.put("dataset","http://51.38.196.163/MappeChest/" + file_name_polygon_region); // TODO changer le chemin de fichier de donnÃ©es
		}

//  if (export.equals("local")) {
//	  custStageModDataset.put("dataset","https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_bar); // TODO changer le chemin de fichier de donnÃ©es
//  }

		customStageModelConfiguration.add(custStageModDataset);

		if (export.equals("VM") || export.equals("VM_local")) {
			JSONObject custStageModStage1 = new JSONObject();
			custStageModStage1.put("stageId", "stage_bar_region");
			custStageModStage1.put("dataset", "http://" + hote + "/MappeChest/" + file_name_bar_region); // TODO
																											// changer
																											// le chemin
																											// de
																											// fichier
																											// de
																											// donnÃ©es
			customStageModelConfiguration.add(custStageModStage1);

			JSONObject custStageModStage2 = new JSONObject();
			custStageModStage2.put("stageId", "stage_polygon_region");
			custStageModStage2.put("dataset", "http://" + hote + "/MappeChest/" + file_name_polygon_region); // TODO
																												// changer
																												// le
																												// chemin
																												// de
																												// fichier
																												// de
																												// donnÃ©es
			customStageModelConfiguration.add(custStageModStage2);

			JSONObject custStageModStage3 = new JSONObject();
			custStageModStage3.put("stageId", "stage_polygon_departement");
			custStageModStage3.put("dataset", "http://" + hote + "/MappeChest/" + file_name_polygon_departement); // TODO
																													// changer
																													// le
																													// chemin
																													// de
																													// fichier
																													// de
																													// donnÃ©es
			customStageModelConfiguration.add(custStageModStage3);

		}

//  if (export.equals("local")) {
//	custStageModStage.put("dataset","https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_polygon); // TODO changer le chemin de fichier de donnÃ©es
//  }
//
//  customStageModelConfiguration.add(custStageModStage);

		chartConfiguration.put("customStageModelConfiguration", customStageModelConfiguration);

		/*
		 * ------------------- Fin Creation custom feature for multiStage
		 * ---------------------
		 */

//	chartConfiguration.put("customFeatureConfiguration", customFeatureConfiguration);

		chartConfiguration.put("themeChartScaling", 0.95);
		chartConfiguration.put("latitudeFieldName", "");
		chartConfiguration.put("longitudeFieldName", "");

		JSONArray tooltipConfiguration = new JSONArray();

		chartConfiguration.put("tooltipConfiguration", tooltipConfiguration);
		chartConfiguration.put("boundaryData", null);
		chartConfiguration.put("boundaryDataIndexFieldName", null);

		JSONObject boundaryColorSequence = new JSONObject();

		chartConfiguration.put("boundaryColorSequence", boundaryColorSequence);

		JSONObject webGLPointStyle = new JSONObject();
		webGLPointStyle.put("displayEmphasis", true);
		webGLPointStyle.put("opacity", 50);
		webGLPointStyle.put("pointSize", "3");
		webGLPointStyle.put("isMeters", false);

		chartConfiguration.put("webGLPointStyle", webGLPointStyle);

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		lineStyle.put("opacity", 50);
		lineStyle.put("isMeters", false);

		chartConfiguration.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
		polygonStyle.put("fillOpacity", 0.5);
		polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 1);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.8);
		polygonStyle.put("autosetOpacity", true);

		chartConfiguration.put("polygonStyle", polygonStyle);

		JSONObject heatMapColorSequence = new JSONObject();
		heatMapColorSequence.put("fromColor", "#FFEBE5");
		heatMapColorSequence.put("toColor", "#FF0000");
		heatMapColorSequence.put("slices", 15);
		heatMapColorSequence.put("path", "linear");

		chartConfiguration.put("heatMapColorSequence", heatMapColorSequence);
		JSONObject chartColorConfiguration = new JSONObject();

		chartConfiguration.put("chartColorConfiguration", chartColorConfiguration);

		JSONObject dayOfWeekColorSequence = new JSONObject();
		dayOfWeekColorSequence.put("fromColor", "#7f7f00");
		dayOfWeekColorSequence.put("toColor", "#0000FF");
		dayOfWeekColorSequence.put("path", "linear");
		dayOfWeekColorSequence.put("d3", "category10");

		chartConfiguration.put("dayOfWeekColorSequence", dayOfWeekColorSequence);

		templateJsonData.put("chartConfiguration", chartConfiguration);

//	"mapLayerConfiguration":
//	{"Bar Region":{"visible":true},
//	"Polygon Region":{"visible":true},
//	"Polygon Departement":{"visible":true},
//	"Features":{"visible":true}},

		JSONObject mapLayerConfiguration = new JSONObject();

		JSONObject Bar_region = new JSONObject();
		Bar_region.put("visible", true);
		mapLayerConfiguration.put("Bar Region", Bar_region);

		JSONObject Polygon_region = new JSONObject();
		Polygon_region.put("visible", true);
		mapLayerConfiguration.put("Polygon region", Polygon_region);

		JSONObject Polygon_departement = new JSONObject();
		Polygon_departement.put("visible", true);
		mapLayerConfiguration.put("Polygon departement", Polygon_departement);

		JSONObject Feature = new JSONObject();
		Feature.put("visible", true);
		mapLayerConfiguration.put("Features", Feature);

		templateJsonData.put("mapLayerConfiguration", mapLayerConfiguration);
		templateJsonData.put("loading", false);

		JSONObject filterConfiguration = new JSONObject();
//	JSONArray dow= new JSONArray();
//	filterConfiguration.put("dow", dow);
//	JSONArray tod = new JSONArray();
//	filterConfiguration.put("tod", tod);
//	JSONArray theme = new JSONArray();
//	filterConfiguration.put("theme", theme);
//	JSONArray heatmap = new JSONArray();
//	filterConfiguration.put("heatmap", heatmap);
//	JSONArray dateline = new JSONArray();
//	filterConfiguration.put("dateline", dateline);

		templateJsonData.put("filterConfiguration", filterConfiguration);

		JSONObject spatialFilterConfiguration = new JSONObject();
		JSONObject geometry = new JSONObject();
		geometry.put("type", "FeatureCollection");
		JSONArray features = new JSONArray();
		geometry.put("features", features);

		spatialFilterConfiguration.put("geometry", geometry);
		JSONArray filter = new JSONArray();
		spatialFilterConfiguration.put("filter", filter);
		spatialFilterConfiguration.put("circles", new JSONArray());

		templateJsonData.put("spatialFilterConfiguration", spatialFilterConfiguration);

		if (export.equals("local") || export.equals("VM_local")) {
			TEXT.savefile(s_templatejson_File, templateJsonData.toString().replace("},", "},\n"));
		}

		if (export.equals("VM")) {
			TEXT.savefile(templatepath,
					templateJsonData.toString().replace("},", "},\n"));

//		TEXT.savefile(s_templatejson_File, templateJsonData.toString().replace("},", "},\n"));
		}

	}

//--------------------------------- FIN CREATION DES TEMPLATES EN FONCTION DE LA REQUETE - <choropleth - Bar> - MultiIndicateur - NMeasure + MultiLayer

// ----------- CustomFeatureConfiguration ----------------------------
//ecrireDonneesTemplates2MultiBar

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationFeaturesPoint(String file_name_b, String mesure_nom) {
		// TODO Auto-generated method stub
		JSONObject customFeatureConfigFeature = new JSONObject();

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",3);
		customFeatureConfigFeature.put("version", 4);
		customFeatureConfigFeature.put("type", "primaryFeatureLayer");
		customFeatureConfigFeature.put("dock", "leaflet");
		customFeatureConfigFeature.put("title", "Features");
		customFeatureConfigFeature.put("color", new JSONObject());
		customFeatureConfigFeature.put("temporary", true);

		JSONObject options = new JSONObject();
		options.put("latitudeFieldName", "lat_centro");
//	"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 
		options.put("longitudeFieldName", "long_centro");

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		lineStyle.put("opacity", 80);
		lineStyle.put("isMeters", false);

		options.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
		polygonStyle.put("fillOpacity", 0.8);
		polygonStyle.put("outlineColor", null);
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 0.44999999999999996);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.24999999999999997);
		polygonStyle.put("autosetOpacity", true);

		options.put("polygonStyle", polygonStyle);

		JSONObject pointStyle = new JSONObject();
		pointStyle.put("displayEmphasis", true);
		pointStyle.put("opacity", 80);
		pointStyle.put("pointSize", "3");
		pointStyle.put("isMeters", false);

		options.put("pointStyle", pointStyle);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);
		options.put("additionalFields", new JSONObject());
		options.put("tooltipsEnabled", false);

		customFeatureConfigFeature.put("options", options);
		customFeatureConfigFeature.put("visible", true);
		customFeatureConfigFeature.put("_filters", new JSONArray());
		customFeatureConfigFeature.put("opacity", 100);
		customFeatureConfigFeature.put("groupKey", "_group-29551");
		customFeatureConfigFeature.put("top", 15);
		customFeatureConfigFeature.put("left", 15);
		customFeatureConfigFeature.put("width", 20);
		customFeatureConfigFeature.put("height", 15);
		customFeatureConfigFeature.put("x", new JSONObject());

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", new JSONArray());
		customFeatureConfigFeature.put("y", y);

		return customFeatureConfigFeature;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationFeatures(String titlefeature, String mesure_nom) {
		// TODO Auto-generated method stub
		JSONObject customFeatureConfigFeature = new JSONObject();

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",3);
		customFeatureConfigFeature.put("version", 4);
		customFeatureConfigFeature.put("type", "primaryFeatureLayer");
		customFeatureConfigFeature.put("dock", "leaflet");
		customFeatureConfigFeature.put("title", titlefeature);
		customFeatureConfigFeature.put("color", new JSONObject());
		customFeatureConfigFeature.put("temporary", true);

		JSONObject options = new JSONObject();
		options.put("latitudeFieldName", "");
//	"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 
		options.put("longitudeFieldName", "");

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		lineStyle.put("opacity", 100);
		lineStyle.put("isMeters", false);

		options.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
		polygonStyle.put("fillOpacity", 1);
		polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 1);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.9);
		polygonStyle.put("autosetOpacity", true);

		options.put("polygonStyle", polygonStyle);

		JSONObject pointStyle = new JSONObject();
		pointStyle.put("displayEmphasis", true);
		pointStyle.put("opacity", 100);
		pointStyle.put("pointSize", "7");
		pointStyle.put("isMeters", false);

		options.put("pointStyle", pointStyle);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);
		options.put("additionalFields", new JSONObject());

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("tooltipsEnabled", false);

		customFeatureConfigFeature.put("options", options);
		customFeatureConfigFeature.put("visible", true);
		customFeatureConfigFeature.put("_filters", new JSONArray());
		customFeatureConfigFeature.put("opacity", 100);
		customFeatureConfigFeature.put("groupKey", "_group-29551");
		customFeatureConfigFeature.put("top", 15);
		customFeatureConfigFeature.put("left", 15);
		customFeatureConfigFeature.put("width", 20);
		customFeatureConfigFeature.put("height", 15);
		customFeatureConfigFeature.put("x", new JSONObject());

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", new JSONArray());
		customFeatureConfigFeature.put("y", y);

		return customFeatureConfigFeature;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationFeaturesNMeasure(String file_name_b, String mesure_nom,
			String stage_mesure) {
		// TODO Auto-generated method stub
		JSONObject customFeatureConfigFeature = new JSONObject();

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",3);
		customFeatureConfigFeature.put("version", 4);
		customFeatureConfigFeature.put("type", "primaryFeatureLayer");
		customFeatureConfigFeature.put("dock", "leaflet");
		customFeatureConfigFeature.put("title", "Bar " + mesure_nom);
		customFeatureConfigFeature.put("color", new JSONObject());
		customFeatureConfigFeature.put("stageId", stage_mesure);
		customFeatureConfigFeature.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("latitudeFieldName", "");
//	"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 
		options.put("longitudeFieldName", "");

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		lineStyle.put("opacity", 100);
		lineStyle.put("isMeters", false);

		options.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
		polygonStyle.put("fillOpacity", 1);
		polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 1);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.9);
		polygonStyle.put("autosetOpacity", true);

		options.put("polygonStyle", polygonStyle);

		JSONObject pointStyle = new JSONObject();
		pointStyle.put("displayEmphasis", true);
		pointStyle.put("opacity", 100);
		pointStyle.put("pointSize", "7");
		pointStyle.put("isMeters", false);

		options.put("pointStyle", pointStyle);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);
		options.put("additionalFields", new JSONObject());

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("tooltipsEnabled", false);

		customFeatureConfigFeature.put("options", options);
		customFeatureConfigFeature.put("visible", true);
		customFeatureConfigFeature.put("_filters", new JSONArray());
		customFeatureConfigFeature.put("opacity", 100);

		Random r = new Random();
		int grp = r.nextInt((99999 - 4444) + 1) + 4444;
		customFeatureConfigFeature.put("groupKey", "_group-" + grp);

//	customFeatureConfigFeature.put("groupKey", "_group-29551");
		customFeatureConfigFeature.put("top", 15);
		customFeatureConfigFeature.put("left", 15);
		customFeatureConfigFeature.put("width", 20);
		customFeatureConfigFeature.put("height", 15);
		customFeatureConfigFeature.put("x", new JSONObject());

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", new JSONArray());
		customFeatureConfigFeature.put("y", y);

		return customFeatureConfigFeature;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationFeaturesMultiNMeasure(String mesure_nom, String stage_mesure,
			String niveau, int def) {
		// TODO Auto-generated method stub
		JSONObject customFeatureConfigFeature = new JSONObject();

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",3);
		customFeatureConfigFeature.put("version", 4);
		customFeatureConfigFeature.put("type", "primaryFeatureLayer");
		customFeatureConfigFeature.put("dock", "leaflet");
		customFeatureConfigFeature.put("title", "Bar " + mesure_nom + " " + niveau);
		customFeatureConfigFeature.put("color", new JSONObject());

		if (def != 0) {
			customFeatureConfigFeature.put("stageId", stage_mesure);
		}

//	customFeatureConfigFeature.put("stageId", stage_mesure);

		if (def != 0) {
			customFeatureConfigFeature.put("temporary", false);
		} else {
			customFeatureConfigFeature.put("temporary", true);
		}
//	customFeatureConfigFeature.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("latitudeFieldName", "");
//	"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 
		options.put("longitudeFieldName", "");

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		if (niveau.equals("region")) {
			lineStyle.put("opacity", 100);
		} else {
			lineStyle.put("opacity", 49);
		}

		lineStyle.put("isMeters", false);

		options.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
		polygonStyle.put("fillOpacity", 1);

		if (niveau.equals("region")) {
			polygonStyle.put("outlineColor", "#0000FF");

		} else {
			polygonStyle.put("outlineColor", "#000000");

		}
//	
//	polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 1);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.9);
		polygonStyle.put("autosetOpacity", true);

		options.put("polygonStyle", polygonStyle);

		JSONObject pointStyle = new JSONObject();
		pointStyle.put("displayEmphasis", true);

		if (def != 0) {

			if (niveau.equals("region")) {
				pointStyle.put("opacity", 100);

			} else {
				pointStyle.put("opacity", 49);

			}

		} else {
			pointStyle.put("opacity", 49);
		}

//	pointStyle.put("opacity", 100);
		pointStyle.put("pointSize", "7");
		pointStyle.put("isMeters", false);

		options.put("pointStyle", pointStyle);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);
		options.put("additionalFields", new JSONObject());

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("tooltipsEnabled", false);

		customFeatureConfigFeature.put("options", options);
		customFeatureConfigFeature.put("visible", true);
		customFeatureConfigFeature.put("_filters", new JSONArray());

		if (niveau.equals("region")) {
			customFeatureConfigFeature.put("opacity", 100);
		} else {
			customFeatureConfigFeature.put("opacity", 49);
		}

//	customFeatureConfigFeature.put("opacity", 100);

		Random r = new Random();
		int grp = r.nextInt((99999 - 4444) + 1) + 4444;
		customFeatureConfigFeature.put("groupKey", "_group-" + grp);

//	customFeatureConfigFeature.put("groupKey", "_group-29551");
		customFeatureConfigFeature.put("top", 15);
		customFeatureConfigFeature.put("left", 15);
		customFeatureConfigFeature.put("width", 20);
		customFeatureConfigFeature.put("height", 15);
		customFeatureConfigFeature.put("x", new JSONObject());

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", new JSONArray());
		customFeatureConfigFeature.put("y", y);

		return customFeatureConfigFeature;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationFeaturesMulti(String file_name_b, String mesure_nom,
			String niveau) {
		// TODO Auto-generated method stub
		JSONObject customFeatureConfigFeature = new JSONObject();

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",3);
		customFeatureConfigFeature.put("version", 4);
		customFeatureConfigFeature.put("type", "primaryFeatureLayer");
		customFeatureConfigFeature.put("dock", "leaflet");

		if (niveau.equals("region")) {
			customFeatureConfigFeature.put("title", "Bar Region");
		} else if (niveau.equals("region derriere")) {
			customFeatureConfigFeature.put("title", "Bar region derriere");
		} else if (niveau.equals("region devant")) {
			customFeatureConfigFeature.put("title", "Bar region devant");
		} else if (niveau.equals("departement derriere")) {
			customFeatureConfigFeature.put("title", "Bar departement derriere");
		} else if (niveau.equals("departement devant")) {
			customFeatureConfigFeature.put("title", "Bar departement devant");
		} else {
			customFeatureConfigFeature.put("title", "Bar Departement");
		}

		JSONObject color = new JSONObject();

		if (niveau.equals("region")) {
			color.put("fromColor", "#0086A6");
			color.put("toColor", "#B8B803");
			color.put("slices", 10);
			color.put("path", "linear");

			customFeatureConfigFeature.put("color", color);
		} else if (niveau.equals("region derriere")) {
			color.put("fromColor", "#0086A6");
			color.put("toColor", "#B8B803");
			color.put("slices", 10);
			color.put("path", "linear");

			customFeatureConfigFeature.put("color", color);
		} else if (niveau.equals("region devant")) {
			color.put("fromColor", "#0086A6");
			color.put("toColor", "#B8B803");
			color.put("slices", 10);
			color.put("path", "linear");

			customFeatureConfigFeature.put("color", color);
		} else if (niveau.equals("departement derriere")) {

			color.put("fromColor", "#79f7ee");
			color.put("toColor", "#f0f200");
			color.put("slices", 10);
			color.put("path", "linear");
			customFeatureConfigFeature.put("color", color);
		} else if (niveau.equals("departement devant")) {

			color.put("fromColor", "#79f7ee");
			color.put("toColor", "#f0f200");
			color.put("slices", 10);
			color.put("path", "linear");
			customFeatureConfigFeature.put("color", color);
		} else {
			customFeatureConfigFeature.put("color", color);
		}

		if (niveau.equals("region")) {
			customFeatureConfigFeature.put("stageId", "stage_bar_region");
			customFeatureConfigFeature.put("temporary", false);
		} else if (niveau.equals("region derriere")) {
			customFeatureConfigFeature.put("stageId", "stage_bar_region_der");
			customFeatureConfigFeature.put("temporary", false);
		} else if (niveau.equals("region devant")) {
			customFeatureConfigFeature.put("stageId", "stage_bar_region_dev");
			customFeatureConfigFeature.put("temporary", false);
		} else if (niveau.equals("departement derriere")) {
			customFeatureConfigFeature.put("stageId", "stage_bar_departement_der");
			customFeatureConfigFeature.put("temporary", false);
		} else if (niveau.equals("departement devant")) {
			customFeatureConfigFeature.put("stageId", "stage_bar_departement_dev");
			customFeatureConfigFeature.put("temporary", false);
		} else {
			customFeatureConfigFeature.put("temporary", true);

		}

//	customFeatureConfigFeature.put("temporary", true);

		JSONObject options = new JSONObject();
		options.put("latitudeFieldName", "");
//	"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 
		options.put("longitudeFieldName", "");

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);

//	if (niveau.equals("region")) {
//		lineStyle.put("opacity", 100);
//	}else if (niveau.equals("region derriere")){
//		lineStyle.put("opacity", 100);
//	}else if (niveau.equals("region devant")){
//		lineStyle.put("opacity", 100);
//	}else if (niveau.equals("departement derriere")){
//		lineStyle.put("opacity", 100);
//	}else if (niveau.equals("departement devant")){
//		lineStyle.put("opacity", 100);
//	}else {
//		lineStyle.put("opacity", 50);
//	}

		lineStyle.put("opacity", 100);
		lineStyle.put("isMeters", false);

		options.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");

//	if (niveau.equals("region")) {
//		polygonStyle.put("fillOpacity", 0.95);
//	}else {
//		polygonStyle.put("fillOpacity", 0.5);
//	}

		polygonStyle.put("fillOpacity", 1);

		if (niveau.contains("region")) {
			polygonStyle.put("outlineColor", "#0000FF");
		} else {
			polygonStyle.put("outlineColor", "#000000");
		}

//	polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);

		if (niveau.contains("region")) {
			polygonStyle.put("focusOpacity", 0.44999999999999996);
		} else {
			polygonStyle.put("focusOpacity", 0.44999999999999996);
		}

//	polygonStyle.put("focusOpacity", 1);
		polygonStyle.put("clearColor", "#e6f2f0");

		if (niveau.contains("region")) {
			polygonStyle.put("clearOpacity", 0.24999999999999997);
		} else {
			polygonStyle.put("clearOpacity", 0.24999999999999997);
		}

//	polygonStyle.put("clearOpacity", 0.9);
		polygonStyle.put("autosetOpacity", true);

		options.put("polygonStyle", polygonStyle);

		JSONObject pointStyle = new JSONObject();
		pointStyle.put("displayEmphasis", true);

//	if (niveau.equals("region")) {
//		pointStyle.put("opacity", 95);
//	}else {
//		pointStyle.put("opacity", 50);
//	}
//	pointStyle.put("opacity", 50);
		pointStyle.put("opacity", 100);
		pointStyle.put("pointSize", "7");
		pointStyle.put("isMeters", false);

		options.put("pointStyle", pointStyle);

		if (niveau.contains("region") || niveau.equals("departement derriere") || niveau.equals("departement devant")) {
//		"clusterDistance":100,
//		"style":{"colorSequence":{"fromColor":"#7fff7f","toColor":"#ff7f7f","slices":10,"path":"clockwise"},
//		"ringColorSequence":{"fromColor":"#007f00","toColor":"#7f0000","slices":10,"path":"clockwise"},
//		"font":"'Roboto Condensed', sans-serif",
//		"clusterStyle":{"fromSize":40,"toSize":40,"fromFontSize":12,"toFontSize":12,"style":"","thematicSize":10}},
			options.put("clusterDistance", 100);
			JSONObject style = new JSONObject();
			JSONObject colorSequence = new JSONObject();
			colorSequence.put("fromColor", "#7fff7f");
			colorSequence.put("toColor", "#ff7f7f");
			colorSequence.put("slices", 10);
			colorSequence.put("path", "clockwise");
			style.put("colorSequence", colorSequence);

			JSONObject ringColorSequence = new JSONObject();
			ringColorSequence.put("fromColor", "#007f00");
			ringColorSequence.put("toColor", "#7f0000");
			ringColorSequence.put("slices", 10);
			ringColorSequence.put("path", "clockwise");
			style.put("ringColorSequence", ringColorSequence);

			style.put("font", "'Roboto Condensed', sans-serif");

			JSONObject clusterStyle = new JSONObject();
			clusterStyle.put("fromSize", 40);
			clusterStyle.put("toSize", 40);
			clusterStyle.put("fromFontSize", 12);
			clusterStyle.put("toFontSize", 12);
			clusterStyle.put("style", "traditional");
			clusterStyle.put("thematicSize", 10);
			style.put("style", clusterStyle);

			options.put("style", style);

		}

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);
		options.put("additionalFields", new JSONObject());

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("tooltipsEnabled", false);

		JSONObject externalAttributeConfiguration = new JSONObject();

		if (niveau.equals("region")) {
			externalAttributeConfiguration.put("stageId", "stage_bar_region");
			externalAttributeConfiguration.put("fieldName", "id_Legend");
		} else if (niveau.equals("region derriere")) {
			externalAttributeConfiguration.put("stageId", "stage_bar_region_der");
			externalAttributeConfiguration.put("fieldName", "id_Legend");
		} else if (niveau.equals("region devant")) {
			externalAttributeConfiguration.put("stageId", "stage_bar_region_dev");
			externalAttributeConfiguration.put("fieldName", "id_Legend");
		} else if (niveau.equals("departement derriere")) {
			externalAttributeConfiguration.put("stageId", "stage_bar_departement_der");
			externalAttributeConfiguration.put("fieldName", "id_Legend");
		} else if (niveau.equals("departement devant")) {
			externalAttributeConfiguration.put("stageId", "stage_bar_departement_dev");
			externalAttributeConfiguration.put("fieldName", "id_Legend");
		} else {

		}

		options.put("externalAttributeConfiguration", externalAttributeConfiguration);

		customFeatureConfigFeature.put("options", options);
		customFeatureConfigFeature.put("visible", true);
		customFeatureConfigFeature.put("_filters", new JSONArray());

		if (niveau.equals("region")) {
			customFeatureConfigFeature.put("opacity", 100);
			customFeatureConfigFeature.put("groupKey", "_group-73889");
		} else if (niveau.equals("region derriere")) {
			customFeatureConfigFeature.put("opacity", 100);
			customFeatureConfigFeature.put("groupKey", "_group-16997");
		} else if (niveau.equals("region devant")) {
			customFeatureConfigFeature.put("opacity", 100);
			customFeatureConfigFeature.put("groupKey", "_group-1804");
		} else if (niveau.equals("departement derriere")) {
			customFeatureConfigFeature.put("opacity", 100);
			customFeatureConfigFeature.put("groupKey", "_group-65099");
		} else if (niveau.equals("departement devant")) {
			customFeatureConfigFeature.put("opacity", 100);
			customFeatureConfigFeature.put("groupKey", "_group-73281");
		} else {
			customFeatureConfigFeature.put("opacity", 100);
			customFeatureConfigFeature.put("groupKey", "_group-11461");

		}

//	if (niveau.contains("region")) {
//		customFeatureConfigFeature.put("opacity", 100);
//		customFeatureConfigFeature.put("groupKey", "_group-72400");
//	}else {
//		customFeatureConfigFeature.put("opacity", 100);
//		customFeatureConfigFeature.put("groupKey", "_group-71828");
//	}

//	customFeatureConfigFeature.put("opacity", 100);
//	customFeatureConfigFeature.put("groupKey", "_group-29551");
		customFeatureConfigFeature.put("top", 15);
		customFeatureConfigFeature.put("left", 15);
		customFeatureConfigFeature.put("width", 20);
		customFeatureConfigFeature.put("height", 15);
		customFeatureConfigFeature.put("x", new JSONObject());

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", new JSONArray());
		customFeatureConfigFeature.put("y", y);

		return customFeatureConfigFeature;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationFeaturesCluster(String file_name_b) {
		// TODO Auto-generated method stub
		JSONObject customFeatureConfigFeature = new JSONObject();

		customFeatureConfigFeature.put("version", 4);
		customFeatureConfigFeature.put("type", "clusterLayer");
		customFeatureConfigFeature.put("dock", "leaflet");
		customFeatureConfigFeature.put("title", "Features");
//	customFeatureConfig.put("version", 4);

		customFeatureConfigFeature.put("color", new JSONObject());
		customFeatureConfigFeature.put("temporary", true);

		JSONObject options = new JSONObject();
		options.put("latitudeFieldName", "lat_centro");
//	"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 
		options.put("longitudeFieldName", "long_centro");
		options.put("clusterDistance", 100);

		JSONObject style = new JSONObject();

		JSONObject colorSequence = new JSONObject();
		colorSequence.put("fromColor", "#7fff7f");
		colorSequence.put("toColor", "#ff7f7f");
		colorSequence.put("slices", 10);
		colorSequence.put("path", "clockwise");

		style.put("colorSequence", colorSequence);

		JSONObject ringColorSequence = new JSONObject();
		colorSequence.put("fromColor", "#007f00");
		colorSequence.put("toColor", "#7f0000");
		colorSequence.put("slices", 10);
		colorSequence.put("path", "clockwise");

		style.put("ringColorSequence", ringColorSequence);

		style.put("font", "'Roboto Condensed', sans-serif");

		JSONObject clusterStyle = new JSONObject();
		clusterStyle.put("fromSize", 40);
		clusterStyle.put("toSize", 40);
		clusterStyle.put("fromFontSize", 12);
		clusterStyle.put("toFontSize", 12);
		clusterStyle.put("traditional", "traditional");
		clusterStyle.put("thematicSize", 10);

		style.put("clusterStyle", clusterStyle);

		options.put("style", style);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);
		options.put("additionalFields", new JSONObject());
		options.put("tooltipsEnabled", false);

		customFeatureConfigFeature.put("options", options);
		customFeatureConfigFeature.put("visible", true);
		customFeatureConfigFeature.put("_filters", new JSONArray());
		customFeatureConfigFeature.put("groupKey", "_group-98499");
		customFeatureConfigFeature.put("top", 15);
		customFeatureConfigFeature.put("left", 15);
		customFeatureConfigFeature.put("width", 20);
		customFeatureConfigFeature.put("height", 15);
		customFeatureConfigFeature.put("x", new JSONObject());

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", new JSONArray());
		customFeatureConfigFeature.put("y", y);

		return customFeatureConfigFeature;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationBoundaries(String file_name, String mesure_nom) {
		// TODO Auto-generated method stub
		JSONObject customFeatureConfigBoundaries = new JSONObject();

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",3);
		customFeatureConfigBoundaries.put("version", 4);
		customFeatureConfigBoundaries.put("type", "featureLayer");
		customFeatureConfigBoundaries.put("dock", "leaflet");
		customFeatureConfigBoundaries.put("title", "Boundaries");
		customFeatureConfigBoundaries.put("fieldName", "ID");
//	customFeatureConfig.put("version", 4);

		JSONObject color = new JSONObject();
		color.put("fromColor", "#FFEBE5");
		color.put("toColor", "#FF0000");
		color.put("slices", 15);
		color.put("path", "linear");
		color.put("opacity", "70");

		customFeatureConfigBoundaries.put("color", color);
		customFeatureConfigBoundaries.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("geometryKey", "ID");
		options.put("url", "https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name);
//	"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 
		options.put("tooltipsEnabled", false);

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("fieldName", mesure_nom);
		fieldStats.put("mode", "average");
		options.put("fieldStats", fieldStats);

		options.put("tooltipConfiguration", new JSONArray());
		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);
		options.put("additionalFields", new JSONObject());

		customFeatureConfigBoundaries.put("options", options);
		customFeatureConfigBoundaries.put("_filters", new JSONArray());
		customFeatureConfigBoundaries.put("opacity", 0.7);
		customFeatureConfigBoundaries.put("groupKey", "_group-82716");
		customFeatureConfigBoundaries.put("stockLayer", "boundary");
		customFeatureConfigBoundaries.put("top", 15);
		customFeatureConfigBoundaries.put("left", 15);
		customFeatureConfigBoundaries.put("width", 20);
		customFeatureConfigBoundaries.put("height", 15);
		customFeatureConfigBoundaries.put("x", new JSONObject());

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", new JSONArray());

		customFeatureConfigBoundaries.put("y", y);

		JSONObject connector = new JSONObject();
		connector.put("version", 1);
		connector.put("mode", "StandardConnector");
		connector.put("url", "https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Temp/" + file_name);
		connector.put("loaderPromise", new JSONObject());

		customFeatureConfigBoundaries.put("connector", connector);

		return customFeatureConfigBoundaries;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationPolygon(String title_feature, String mesure_nom) {
		// TODO Auto-generated method stub
		JSONObject customFeatureConfigPolygon = new JSONObject();

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",3);
		customFeatureConfigPolygon.put("version", 4);
		customFeatureConfigPolygon.put("type", "primaryFeatureLayer");
		customFeatureConfigPolygon.put("dock", "leaflet");
		customFeatureConfigPolygon.put("title", title_feature);
		JSONObject color = new JSONObject();
		customFeatureConfigPolygon.put("color", color);

		customFeatureConfigPolygon.put("stageId", "polygon_geo");
		customFeatureConfigPolygon.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("latitudeFieldName", "");
		options.put("longitudeFieldName", "");

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		lineStyle.put("opacity", 15);
		lineStyle.put("isMeter", false);
		options.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
		polygonStyle.put("fillOpacity", 0.15);
		polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 0.9);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.7000000000000001);
		polygonStyle.put("autosetOpacity", true);
		options.put("polygonStyle", polygonStyle);

		JSONObject pointStyle = new JSONObject();
		pointStyle.put("displayEmphasis", true);
		pointStyle.put("opacity", 15);
		pointStyle.put("pointSize", 11);
		pointStyle.put("isMeters", false);
		options.put("pointStyle", pointStyle);

		options.put("clusterDistance", 100);

		JSONObject style = new JSONObject();
		JSONObject colorSequence = new JSONObject();
		colorSequence.put("fromColor", "#7fff7f");
		colorSequence.put("toColor", "#ff7f7f");
		colorSequence.put("slices", 10);
		colorSequence.put("path", "clockwise");
		style.put("colorSequence", colorSequence);

		JSONObject ringColorSequence = new JSONObject();
		ringColorSequence.put("fromColor", "#007f00");
		ringColorSequence.put("toColor", "#7f0000");
		ringColorSequence.put("slices", 10);
		ringColorSequence.put("path", "clockwise");
		style.put("ringColorSequence", ringColorSequence);

		style.put("font", "'Roboto Condensed', sans-serif");

		JSONObject clusterStyle = new JSONObject();
		clusterStyle.put("fromSize", 40);
		clusterStyle.put("toSize", 40);
		clusterStyle.put("fromFontSize", 12);
		clusterStyle.put("toFontSize", 12);
		clusterStyle.put("style", "traditional");
		clusterStyle.put("thematicSize", 10);

		style.put("clusterStyle", clusterStyle);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);
		options.put("additionalFields", new JSONObject());

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("tooltipsEnabled", false);

		customFeatureConfigPolygon.put("options", options);

		customFeatureConfigPolygon.put("_filters", new JSONArray());
		customFeatureConfigPolygon.put("opacity", 15);
		customFeatureConfigPolygon.put("groupKey", "_group-82716");
		customFeatureConfigPolygon.put("top", 15);
		customFeatureConfigPolygon.put("left", 15);
		customFeatureConfigPolygon.put("width", 20);
		customFeatureConfigPolygon.put("height", 15);
		customFeatureConfigPolygon.put("x", new JSONObject());

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", new JSONArray());

		customFeatureConfigPolygon.put("y", y);

		return customFeatureConfigPolygon;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationPolygonMulti(String file_name, String mesure_nom,
			String export, String niveau) {
		// TODO Auto-generated method stub
		JSONObject customFeatureConfigPolygon = new JSONObject();

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",3);
		customFeatureConfigPolygon.put("version", 4);
		customFeatureConfigPolygon.put("type", "primaryFeatureLayer");
		customFeatureConfigPolygon.put("dock", "leaflet");

		if (niveau.equals("region")) {
			customFeatureConfigPolygon.put("title", "Polygon region");
		} else {
			customFeatureConfigPolygon.put("title", "Polygon departement");
		}

//	customFeatureConfigPolygon.put("title", "Polygone Stage");
		JSONObject color = new JSONObject();
		customFeatureConfigPolygon.put("color", color);

		if (niveau.equals("region")) {
			customFeatureConfigPolygon.put("stageId", "stage_polygon_region");
		} else {
			customFeatureConfigPolygon.put("stageId", "stage_polygon_departement");
		}

//	customFeatureConfigPolygon.put("stageId", "polygon_geo");
		customFeatureConfigPolygon.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("latitudeFieldName", "");
		options.put("longitudeFieldName", "");

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		lineStyle.put("opacity", 35);
		lineStyle.put("isMeter", false);
		options.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#1f77b4");
		polygonStyle.put("fillOpacity", 0.35);
		polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 0.9);
		polygonStyle.put("clearColor", "#e6f2f0");
		polygonStyle.put("clearOpacity", 0.7000000000000001);
		polygonStyle.put("autosetOpacity", true);
		options.put("polygonStyle", polygonStyle);

		JSONObject pointStyle = new JSONObject();
		pointStyle.put("displayEmphasis", true);
		pointStyle.put("opacity", 35);
		pointStyle.put("pointSize", 11);
		pointStyle.put("isMeters", false);
		options.put("pointStyle", pointStyle);

		options.put("clusterDistance", 100);

		JSONObject style = new JSONObject();
		JSONObject colorSequence = new JSONObject();
		colorSequence.put("fromColor", "#7fff7f");
		colorSequence.put("toColor", "#ff7f7f");
		colorSequence.put("slices", 10);
		colorSequence.put("path", "clockwise");
		style.put("colorSequence", colorSequence);

		JSONObject ringColorSequence = new JSONObject();
		ringColorSequence.put("fromColor", "#007f00");
		ringColorSequence.put("toColor", "#7f0000");
		ringColorSequence.put("slices", 10);
		ringColorSequence.put("path", "clockwise");
		style.put("ringColorSequence", ringColorSequence);

		style.put("font", "'Roboto Condensed', sans-serif");

		JSONObject clusterStyle = new JSONObject();
		clusterStyle.put("fromSize", 40);
		clusterStyle.put("toSize", 40);
		clusterStyle.put("fromFontSize", 12);
		clusterStyle.put("toFontSize", 12);
		clusterStyle.put("style", "traditional");
		clusterStyle.put("thematicSize", 10);

		style.put("clusterStyle", clusterStyle);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);
		options.put("additionalFields", new JSONObject());

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("tooltipsEnabled", false);

		customFeatureConfigPolygon.put("options", options);

		customFeatureConfigPolygon.put("_filters", new JSONArray());
		customFeatureConfigPolygon.put("opacity", 35);

		if (niveau.equals("region")) {
			customFeatureConfigPolygon.put("groupKey", "_group-35107");
		} else {
			customFeatureConfigPolygon.put("groupKey", "_group-84679");
		}

//	customFeatureConfigPolygon.put("groupKey", "_group-82716");
		customFeatureConfigPolygon.put("top", 15);
		customFeatureConfigPolygon.put("left", 15);
		customFeatureConfigPolygon.put("width", 20);
		customFeatureConfigPolygon.put("height", 15);
		customFeatureConfigPolygon.put("x", new JSONObject());

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", new JSONArray());

		customFeatureConfigPolygon.put("y", y);

		return customFeatureConfigPolygon;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationFeaturesEmpty(String titlefeature, String mesure_nom) { // TODO
																													// Auto-generated
																													// method
																													// stub
		JSONObject customFeatureConfigFeature = new JSONObject();

//		JSONObject defaultThemeClassification = new JSONObject();
//		defaultThemeClassification.put("method","equalInterval");
//		defaultThemeClassification.put("groups",3);
		customFeatureConfigFeature.put("version", 4);
		customFeatureConfigFeature.put("type", "primaryFeatureLayer");
		customFeatureConfigFeature.put("dock", "leaflet");
		customFeatureConfigFeature.put("title", titlefeature);
		customFeatureConfigFeature.put("color", new JSONObject());
		customFeatureConfigFeature.put("stageId", "polygon_geo");

		customFeatureConfigFeature.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("latitudeFieldName", "");
//		"https://mapps.geosystems.fr/MappeChest/Vectors/VGI4Bio/" + file_name_b 
		options.put("longitudeFieldName", "");

		JSONObject lineStyle = new JSONObject();
		lineStyle.put("width", 1);
		lineStyle.put("opacity", 100);
		lineStyle.put("isMeters", false);

		options.put("lineStyle", lineStyle);

		JSONObject polygonStyle = new JSONObject();
		polygonStyle.put("fillColor", "#A9A9A9");
		polygonStyle.put("fillOpacity", 1);
		polygonStyle.put("outlineColor", "#000000");
		polygonStyle.put("outlineOpacity", 1);
		polygonStyle.put("focusColor", null);
		polygonStyle.put("focusOpacity", 1);
		polygonStyle.put("clearColor", "#A9A9A9");
		polygonStyle.put("clearOpacity", 0.9);
		polygonStyle.put("autosetOpacity", true);

		options.put("polygonStyle", polygonStyle);

		JSONObject pointStyle = new JSONObject();
		pointStyle.put("displayEmphasis", true);
		pointStyle.put("opacity", 100);
		pointStyle.put("pointSize", "7");
		pointStyle.put("isMeters", false);

		options.put("pointStyle", pointStyle);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);
		options.put("additionalFields", new JSONObject());

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("tooltipsEnabled", false);

		customFeatureConfigFeature.put("options", options);
		customFeatureConfigFeature.put("visible", true);
		customFeatureConfigFeature.put("_filters", new JSONArray());
		customFeatureConfigFeature.put("opacity", 100);
		customFeatureConfigFeature.put("groupKey", "_group-2951");
		customFeatureConfigFeature.put("top", 15);
		customFeatureConfigFeature.put("left", 15);
		customFeatureConfigFeature.put("width", 20);
		customFeatureConfigFeature.put("height", 15);
		customFeatureConfigFeature.put("x", new JSONObject());

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", new JSONArray());
		customFeatureConfigFeature.put("y", y);

		return customFeatureConfigFeature;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addCustomFeatureConfigurationHtml(String html, String dimension) {
		// TODO Auto-generated method stub
		JSONObject customFeatureConfigHtml = new JSONObject();

//	JSONObject defaultThemeClassification = new JSONObject();
//	defaultThemeClassification.put("method","equalInterval");
//	defaultThemeClassification.put("groups",3);
		customFeatureConfigHtml.put("version", 4);
		customFeatureConfigHtml.put("type", "html");
		customFeatureConfigHtml.put("dock", "float");
		customFeatureConfigHtml.put("title", dimension);
		customFeatureConfigHtml.put("fieldName", "");

		JSONObject color = new JSONObject();

		color.put("fromColor", "#0097ba");
		color.put("toColor", "#ba9700");
		color.put("path", "linear");
		color.put("d3", "category10");
		color.put("slices", 10);

		customFeatureConfigHtml.put("color", color);

		customFeatureConfigHtml.put("label", "");
		customFeatureConfigHtml.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		options.put("disableBinning", 5);
		JSONObject additionalFields = new JSONObject();
		additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);
		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);
		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);
		options.put("tableDisplayLocate", false);
		options.put("dateRounding", "");
		options.put("elasticX", false);
		options.put("precision", "");

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "advanced");
		fieldStats.put("fieldName", "");
		options.put("fieldStats", fieldStats);

		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);
		options.put("lastFieldName", "");

		JSONObject html_obj = new JSONObject();

		String html_one = "<div style=\'display:table;width:100%;text-align: left; font-size:13px; margin-left: 10px\'>\n   "
				+ " <div style=\'display:table-row\'>\n        " + "<div style='display: table-cell'>\n        "
				+ "    <strong>Filtered</strong>:\n        " + "</div>\n       "
				+ " <div style='display: table-cell'>\n         " + "   %filteredCount\n     " + "   </div>\n  "
				+ "  </div>\n  " + "  <div style='display:table-row'>\n       "
				+ " <div style='display: table-cell'>\n        " + "    <strong>Total</strong>:\n    "
				+ "    </div>\n     " + "   <div style='display: table-cell'>\n       " + "     %totalCount\n     "
				+ "   </div>\n   " + " </div>\n  " + "  <div style='display:table-row'>\n    "
				+ "    <div style='display: table-cell'>\n   "
				+ "         <strong>Records with Numeric Values</strong>:\n    " + "    </div>\n    "
				+ "    <div style='display: table-cell'>\n     " + "       %recordsWithValues\n   " + "     </div>\n  "
				+ "  </div>\n  " + "  <div style='display:table-row'>\n    "
				+ "    <div style='display: table-cell'>\n      " + "      <strong>Min</strong>:\n    "
				+ "    </div>\n    " + "    <div style='display: table-cell'>\n    " + "        %min\n    "
				+ "    </div>\n " + "   </div>\n  " + "  <div style='display:table-row'>\n    "
				+ "    <div style='display: table-cell'><strong>Max</strong>: </div>\n    "
				+ "    <div style='display: table-cell'>\n     " + "       %max\n     " + "   </div>\n  "
				+ "  </div>\n  " + "  <div style='display:table-row'>\n     "
				+ "   <div style='display: table-cell'>\n       " + "     <strong>Standard Deviation</strong>:\n     "
				+ "   </div>\n     " + "   <div style='display: table-cell'>\n      " + "      %stddev\n     "
				+ "   </div>\n  " + "  </div>\n  " + "  <div style='display:table-row'>\n    "
				+ "    <div style='display: table-cell'>\n      " + "      <strong>Variance</strong>:\n    "
				+ "    </div>\n     " + "   <div style='display: table-cell'>\n     " + "       %variance\n    "
				+ "    </div>\n  " + "  </div>\n  " + "  <div style='display:table-row'>\n     "
				+ "   <div style='display: table-cell'>\n        " + "    <strong>Mean</strong>:\n     "
				+ "   </div>\n      " + "  <div style='display: table-cell'>\n       " + "     %mean\n     "
				+ "   </div>\n  " + "  </div>\n  " + "  <div style='display:table-row'>\n     "
				+ "   <div style='display: table-cell'>\n     " + "       <strong>Median</strong>:\n    "
				+ "    </div>\n     " + "   <div style='display: table-cell'>\n    " + "        %median\n  "
				+ "      </div>\n   " + " </div>\n  " + "  <div style='display:table-row'>\n    "
				+ "    <div style='display: table-cell'>\n     " + "       <strong>Sum</strong>:\n    "
				+ "    </div>\n      " + "  <div style='display: table-cell'>\n      " + "      %sum\n    "
				+ "    </div>\n   " + " </div>\n   " + " <div style='display:table-row'>\n     "
				+ "   <div style='display: table-cell'>\n      " + "      <strong>Range</strong>:\n    "
				+ "    </div>\n     " + "   <div style='display: table-cell'>\n        " + "    %range\n     "
				+ "   </div>\n  " + "  </div>\n</div>";
//	"some":

		html_obj.put("one", html_one);

		String html_some = "<!-- \nThis example illustrates how to create a custom legend.\n"
				+ "This is useful in situations where you need to describe how colors are used in a map layer or in a chart.\n"
				+ "This example contains code to:\n- create a table with 2 columns and 3 rows\n"
				+ "\t- populate first column with colored boxes (green, red, and blue) \n"
				+ "\t- populate second column with text descriptions (Class A, Class B, and Class C)\n" + "-->\n" + "\n"
				+ "<style type=\"text/css\">\n\t\n\t.MyLegend-Table {\n\t\tdisplay: table;\n\t\twidth:100%;\n\t\ttext-align: left;\n\t\tfont-size:12px;\n\t\tmargin-left:10px;\n\t}\n\t\n\t.MyLegend-TableRow {\n\t\tdisplay: table-row;\n\t}\n\t\n\t.MyLegend-ColorCell {\n\t\tdisplay:table-cell;\n\t\twidth: 20px;\n\t}\n\t\n\t.MyLegend-ColorCell div {\n\t\twidth:12px;\n\t\theight:12px;\n\t}\n\t\n\t.MyLegend-LabelCell {\n\t\tdisplay: table-cell;\n\t}\n\t\n</style>\n\n"
				+ "<div class=\"MyLegend-Table\">\n " + html + " </div>\n";
//	,"none":"No Features to Display";

		html_obj.put("some", html_some);
		html_obj.put("none", "No Features to Display");

		options.put("html", html_obj);
		options.put("lastDock", "left");
		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");

		customFeatureConfigHtml.put("options", options);
		customFeatureConfigHtml.put("visible", true);
		customFeatureConfigHtml.put("_filters", new JSONArray());
		customFeatureConfigHtml.put("size", "25");
		customFeatureConfigHtml.put("groupKey", "_group-87063");
		customFeatureConfigHtml.put("top", 56.06611570247934);
		customFeatureConfigHtml.put("left", 0);
		customFeatureConfigHtml.put("width", 19.8);
		customFeatureConfigHtml.put("height", 8.991735537190083);
		customFeatureConfigHtml.put("x", new JSONObject());

		JSONObject x = new JSONObject();
		x.put("type", "linear");
		JSONArray domain = new JSONArray();
		domain.add("null");
		domain.add("null");
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");
		customFeatureConfigHtml.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", domain);
		y.put("min", "");
		y.put("max", "");
		y.put("rightY", false);
//	DropDownList.put("y", y);
		customFeatureConfigHtml.put("y", y);

		return customFeatureConfigHtml;
	}

//----------- Fin CustomFeatureConfiguration ----------------------------

	@SuppressWarnings("unchecked")
	private static JSONObject addDimensionDropDownList(String title, String fieldName, String stageid, double pos,
			String rules) {
		// TODO Auto-generated method stub
		JSONObject DropDownList = new JSONObject();

		DropDownList.put("version", 4);
		DropDownList.put("type", "combo");
		DropDownList.put("dock", "float");
		DropDownList.put("title", title);
		DropDownList.put("fieldName", fieldName);

		JSONObject color = new JSONObject();
		color.put("fromColor", "#0097ba");
		color.put("toColor", "#ba9700");
		color.put("path", "linear");
		color.put("d3", "category10");
		color.put("slices", 10);

		DropDownList.put("color", color);

		DropDownList.put("label", "");
		if (stageid.equals("true")) {
			DropDownList.put("stageId", "polygon_geo");
		}

		DropDownList.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		options.put("disableBinning", true);

		JSONObject additionalFields = new JSONObject();
		// additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);
		options.put("tableDisplayLocate", false);
		// options.put("dateRounding", "");
		options.put("elasticX", false);

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "none"); // featureCount
		fieldStats.put("fieldName", "ID");
		options.put("fieldStats", fieldStats);
		options.put("precision", 4);
		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);
		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");
		options.put("lastDock", "left");

		DropDownList.put("options", options);

		DropDownList.put("visible", true);
		DropDownList.put("_filters", new JSONArray());
		DropDownList.put("size", "25");
		// DropDownList.put("groupKey", "_group-61473");
		DropDownList.put("top", pos);
		DropDownList.put("left", 0);
		DropDownList.put("width", 18);
		DropDownList.put("height", 17);

		JSONObject x = new JSONObject();
		x.put("type", "linear");
		JSONArray domain = new JSONArray();
		domain.add(null);
		domain.add(null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");
		DropDownList.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", domain);
		y.put("min", "");
		y.put("max", "");
		// x.put("rightY", false);
		DropDownList.put("y", y);

		JSONObject moving = new JSONObject();
		moving.put("o_t", 14.85699481865285);
		moving.put("o_l", 15);
		moving.put("o_x", 458);
		moving.put("o_y", 127);
		DropDownList.put("moving", moving);

		return DropDownList;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addDimensionDropDownListMulti(String location, int d, String niveau) {
		// TODO Auto-generated method stub
		JSONObject DropDownList = new JSONObject();

		DropDownList.put("version", 4);
		DropDownList.put("type", "combo");
		DropDownList.put("dock", "left");
		if (niveau.equals("region")) {
			DropDownList.put("title", "Region Location");
		} else {
			DropDownList.put("title", "Departement Location");
		}
//	DropDownList.put("title", "Location");
		DropDownList.put("fieldName", "_Location");

		JSONObject color = new JSONObject();
		color.put("fromColor", "#0097ba");
		color.put("toColor", "#ba9700");
		color.put("path", "linear");
		color.put("d3", "category10");
		color.put("slices", 10);

		DropDownList.put("color", color);

		DropDownList.put("label", "");

		if (niveau.equals("region")) {
			DropDownList.put("stageId", "stage_polygon_region");
		} else {

			DropDownList.put("stageId", "stage_polygon_departement");
		}

		DropDownList.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		options.put("disableBinning", true);

		JSONObject additionalFields = new JSONObject();
		additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);
		options.put("tableDisplayLocate", false);
		options.put("dateRounding", "");
		options.put("elasticX", false);
		options.put("precision", "");

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "featureCount");
		fieldStats.put("fieldName", "ID");
		options.put("fieldStats", fieldStats);

		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);
		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");

		DropDownList.put("options", options);

		DropDownList.put("visible", true);
		DropDownList.put("_filters", new JSONArray());
		DropDownList.put("size", "10");

//	if (niveau.equals("region")) {
//		DropDownList.put("groupKey","_group-47744");
//	}else {
//		DropDownList.put("groupKey","_group-19544");
//	}
//	
		Random r = new Random();
		int grp = r.nextInt((99999 - 4444) + 1) + 4444;
		DropDownList.put("groupKey", "_group-" + grp);

//	DropDownList.put("groupKey", "_group-61473");
		DropDownList.put("top", 15);
		DropDownList.put("left", 15);
		DropDownList.put("width", 20);
		DropDownList.put("height", 20);

		JSONObject x = new JSONObject();
		x.put("type", "linear");
		JSONArray domain = new JSONArray();
		domain.add("null");
		domain.add("null");
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");
		DropDownList.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("rightY", false);
		DropDownList.put("y", y);

		/*
		 * JSONObject moving = new JSONObject(); moving.put("o_t", 15);
		 * moving.put("o_l", 15); moving.put("o_x", 331); moving.put("o_y", 66);
		 * DropDownList.put("moving", moving);
		 */

		return DropDownList;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addDimensionRowChart(String dimension_name, int chartNumber, String measure_nom) {
		JSONObject rowChart = new JSONObject();
		rowChart.put("version", 4);
		rowChart.put("type", "row");

		// choix de la position
//	if(chartNumber > 7) {
//		rowChart.put("dock","float");
//	}else if (chartNumber > 3) {
//		rowChart.put("dock","bottom");
//	} else {
//		rowChart.put("dock","left");	
//	}

		rowChart.put("dock", "left");

		rowChart.put("title", dimension_name.toUpperCase() + " INDICATEUR");
		rowChart.put("fieldName", dimension_name);

		JSONObject color = new JSONObject();
		color.put("fromColor", "#0097ba");
		color.put("toColor", "#f7ff00");
		color.put("path", "linear");
		color.put("d3", "custom");
		color.put("slices", 10);

		rowChart.put("color", color);
		rowChart.put("label", "");

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		JSONObject additionalFields = new JSONObject();
//	additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);
		options.put("tableDisplayLocate", false);
		options.put("elasticX", false);
		options.put("sortEnabled", true);
		JSONObject fieldStats = new JSONObject();
//	fieldStats.put("mode","average");
		fieldStats.put("mode", "none");
		fieldStats.put("fieldName", measure_nom);
		options.put("fieldStats", fieldStats);
		options.put("precision", 4);
		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);

		JSONObject sortParams = new JSONObject();
		sortParams.put("method", "label");
		sortParams.put("ascending", true);
		options.put("sortParams", sortParams);

		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");

		rowChart.put("options", options);
		rowChart.put("visible", false);

		JSONArray filters = new JSONArray();
		rowChart.put("filters", filters);
		rowChart.put("size", "25");

		Random r = new Random();
		int grp = r.nextInt((99999 - 4444) + 1) + 4444;
		rowChart.put("groupKey", "_group-" + grp);

//	rowChart.put("groupKey","_group-86829");
		rowChart.put("top", 15);
		rowChart.put("left", 15);
		rowChart.put("width", 20);
		rowChart.put("height", 20);

		JSONObject x = new JSONObject();
		x.put("type", "linear");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");

		rowChart.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", domain);
		y.put("min", "");
		y.put("max", "");

		rowChart.put("y", y);

		return rowChart;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addDimensionRowChartMulti(String dimension_name, int chartNumber, String measure_nom,
			String niveau) {
		JSONObject rowChart = new JSONObject();
		rowChart.put("version", 4);
		rowChart.put("type", "row");

		// choix de la position
//	if(chartNumber > 7) {
//		rowChart.put("dock","float");
//	}else if (chartNumber > 3) {
//		rowChart.put("dock","bottom");
//	} else {
//		rowChart.put("dock","left");	
//	}

		rowChart.put("dock", "left");

		rowChart.put("title", dimension_name.toUpperCase() + " INDICATEUR");
		rowChart.put("fieldName", dimension_name);

		JSONObject color = new JSONObject();

		if (niveau.equals("region")) {
			color.put("fromColor", "#0086a6");
			color.put("toColor", "#b8b803");
		} else {
			color.put("fromColor", "#23d6ff");
			color.put("toColor", "#feff70");
		}

		color.put("path", "linear");
		color.put("d3", "custom");
		color.put("slices", 10);

		rowChart.put("color", color);
		rowChart.put("label", "");

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		JSONObject additionalFields = new JSONObject();
//	additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);
		options.put("tableDisplayLocate", false);
		options.put("elasticX", false);
		options.put("sortEnabled", true);
		JSONObject fieldStats = new JSONObject();
//	fieldStats.put("mode","average");
		fieldStats.put("mode", "none");
		fieldStats.put("fieldName", measure_nom);
		options.put("fieldStats", fieldStats);
		options.put("precision", 4);
		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);

		JSONObject sortParams = new JSONObject();
		sortParams.put("method", "label");
		sortParams.put("ascending", true);
		options.put("sortParams", sortParams);

		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");

		rowChart.put("options", options);
		rowChart.put("visible", false);

		JSONArray filters = new JSONArray();
		rowChart.put("filters", filters);
		rowChart.put("size", "25");

		Random r = new Random();
		int grp = r.nextInt((99999 - 4444) + 1) + 4444;
		rowChart.put("groupKey", "_group-" + grp);

//	rowChart.put("groupKey","_group-86829");
		rowChart.put("top", 15);
		rowChart.put("left", 15);
		rowChart.put("width", 20);
		rowChart.put("height", 20);

		JSONObject x = new JSONObject();
		x.put("type", "linear");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");

		rowChart.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		y.put("domain", domain);
		y.put("min", "");
		y.put("max", "");

		rowChart.put("y", y);

		return rowChart;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addDimensionBarChart(String dimension_name, String mesure_nom, int chartNumber) {
		JSONObject barChart = new JSONObject();
		barChart.put("version", 4);
		barChart.put("type", "row");

		// choix de la position
//	if(chartNumber > 7) {
//		barChart.put("dock","float");
//	}else if (chartNumber > 3) {
//		barChart.put("dock","bottom");
//	} else {
//		barChart.put("dock","left");	
//	}

		barChart.put("dock", "float");

		barChart.put("title", dimension_name.toUpperCase());
		barChart.put("fieldName", dimension_name);

		JSONObject color = new JSONObject();
		color.put("fromColor", "#0097ba");
		color.put("toColor", "#ba9700");
		color.put("path", "linear");
		color.put("d3", "inherit");
		color.put("slices", 10);
		color.put("domain", "#ffffff");

		barChart.put("color", color);
		barChart.put("label", "");
		barChart.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		options.put("disableBinning", true);
		JSONObject additionalFields = new JSONObject();
		additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("tableDisplayLocate", false);
		options.put("dateRounding", "");
		options.put("elasticX", false);
		options.put("precision", "");

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "none");
		fieldStats.put("fieldName", mesure_nom);
		options.put("fieldStats", fieldStats);
//	options.put("precision",4);
		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);
		options.put("stack", "_Legend");
		options.put("showLegend", true);
		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");

		barChart.put("options", options);
		barChart.put("visible", true);

		JSONArray filters = new JSONArray();
		barChart.put("_filters", filters);
		barChart.put("size", "25");
		barChart.put("groupKey", "_group-27675");
		barChart.put("top", 17);
		barChart.put("left", 75);
		barChart.put("width", 25.0);
		barChart.put("height", 35.365325077399376);

		JSONObject x = new JSONObject();
		x.put("type", "ordinal");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");

		barChart.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		JSONArray ydomain = new JSONArray();
		ydomain.add(0, null);
		ydomain.add(1, null);
		y.put("domain", ydomain);
		y.put("min", "");
		y.put("max", "");
		y.put("rigthY", false);
		y.put("rigthY", false);

		barChart.put("y", y);

		JSONObject moving = new JSONObject();
		moving.put("o_t", 15);
		moving.put("o_l", 15);
		moving.put("o_x", 421);
		moving.put("o_y", 150);
		barChart.put("moving", moving);

		JSONObject resizing = new JSONObject();
		resizing.put("o_w", 36.9);
		resizing.put("o_h", 48.14860681114551);
		resizing.put("o_s", 25);
		resizing.put("o_x", 983);
		resizing.put("o_y", 600);
		barChart.put("resizing", resizing);

		return barChart;

	}

	@SuppressWarnings("unchecked")
	private static JSONObject addDimensionBarChartNMeasure(String dimension_name, String mesure_nom, int nb,
			String stage) {
		JSONObject barChart = new JSONObject();
		barChart.put("version", 4);
		barChart.put("type", "row");

		// choix de la position
//	if(chartNumber > 7) {
//		barChart.put("dock","float");
//	}else if (chartNumber > 3) {
//		barChart.put("dock","bottom");
//	} else {
//		barChart.put("dock","left");	
//	}

		barChart.put("dock", "float");

		barChart.put("title", dimension_name.toUpperCase() + " " + mesure_nom);
		barChart.put("fieldName", dimension_name);

		JSONObject color = new JSONObject();
		color.put("fromColor", "#0097ba");
		color.put("toColor", "#ba9700");
		color.put("path", "linear");
		color.put("d3", "inherit");
		color.put("slices", 10);

		barChart.put("color", color);
		barChart.put("label", "");

		if (nb != 0) {
			barChart.put("stageId", stage);
		}

		barChart.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		options.put("disableBinning", true);
		JSONObject additionalFields = new JSONObject();
		additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("tableDisplayLocate", false);
		options.put("dateRounding", "");
		options.put("elasticX", false);
		options.put("precision", "");

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "none");
		fieldStats.put("fieldName", mesure_nom);
		options.put("fieldStats", fieldStats);
//	options.put("precision",4);
		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);
		options.put("stack", "__theme");
		options.put("showLegend", true);
		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");
		options.put("lastDock", "left");

		barChart.put("options", options);
		barChart.put("visible", true);

		JSONArray filters = new JSONArray();
		barChart.put("_filters", filters);
		barChart.put("size", "25");

		// generation nb random
		Random r = new Random();
		int grp = r.nextInt((99999 - 4444) + 1) + 4444;
		barChart.put("groupKey", "_group-" + grp);

		if (nb != 0) {

			barChart.put("top", 61.35652173913044);
			barChart.put("left", 0);
			barChart.put("width", 37.199999999999996);
			barChart.put("height", 28.8);

		} else {
			barChart.put("top", 30.67826086956522);
			barChart.put("left", 0);
			barChart.put("width", 37.199999999999996);
			barChart.put("height", 30.67826086956522);
		}

		JSONObject x = new JSONObject();
		x.put("type", "ordinal");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");

		barChart.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		JSONArray ydomain = new JSONArray();
		ydomain.add(0, null);
		ydomain.add(1, null);
		y.put("domain", ydomain);
		y.put("min", "");
		y.put("max", "");
		y.put("rigthY", false);
		y.put("rigthY", false);

		barChart.put("y", y);

		if (nb != 0) {
			JSONObject moving = new JSONObject();
			moving.put("o_t", 71.37391304347827);
			moving.put("o_l", 4.2);
			moving.put("o_x", 234);
			moving.put("o_y", 670);
			barChart.put("moving", moving);

			JSONObject resizing = new JSONObject();
			resizing.put("o_w", 20);
			resizing.put("o_h", 20);
			resizing.put("o_s", 25);
			resizing.put("o_x", 372);
			resizing.put("o_y", 739);
			barChart.put("resizing", resizing);
		} else {
			JSONObject moving = new JSONObject();
			moving.put("o_t", 39.44347826086957);
			moving.put("o_l", 1.5);
			moving.put("o_x", 189);
			moving.put("o_y", 374);
			barChart.put("moving", moving);

			JSONObject resizing = new JSONObject();
			resizing.put("o_w", 20);
			resizing.put("o_h", 20);
			resizing.put("o_s", 25);
			resizing.put("o_x", 374);
			resizing.put("o_y", 459);
			barChart.put("resizing", resizing);
		}

//	JSONObject moving = new JSONObject();
//	moving.put("o_t", 15);
//	moving.put("o_l", 15);
//	moving.put("o_x", 421);
//	moving.put("o_y", 150);
//	barChart.put("moving", moving);
//	
//	JSONObject resizing = new JSONObject();
//	resizing.put("o_w", 36.9);
//	resizing.put("o_h", 48.14860681114551);
//	resizing.put("o_s", 25);
//	resizing.put("o_x", 983);
//	resizing.put("o_y", 600);
//	barChart.put("resizing", resizing);

		return barChart;

	}

//addDimensionBarChartMultiNMeasure(String dimension_name, String mesure_nom, int nb, String stage)
//addDimensionBarChartMultiNMeasure(String dimension_name, String mesure_nom, int com, int cc, String stage, String niveau)

	@SuppressWarnings("unchecked")
	private static JSONObject addDimensionBarChartMultiNMeasure(String dimension_name, String mesure_nom, int com,
			int cc, String stage, String niveau) {

		String[][] list_couleur_dep = { { "#e5f2ff", "#8f86ff" }, { "#fce5ff", "#e300ff" }, { "#c6fee0", "#07ff00" },
				{ "#fef4c6", "#d1a200" }, { "#ecf6c7", "#f0fa00" } };
		String[][] list_couleur_reg = { { "#b1c6db", "#1300ff" }, { "#c8afcb", "#a600ba" }, { "#9cc8b1", "#04a100" },
				{ "#cec494", "#9d7a00" }, { "#b8bf9e", "#d4dd00" } };

		JSONObject barChart = new JSONObject();
		barChart.put("version", 4);
		barChart.put("type", "bar");

		// choix de la position
//	if(chartNumber > 7) {
//		barChart.put("dock","float");
//	}else if (chartNumber > 3) {
//		barChart.put("dock","bottom");
//	} else {
//		barChart.put("dock","left");	
//	}

		barChart.put("dock", "float");

		barChart.put("title", dimension_name.toUpperCase() + " " + mesure_nom.toLowerCase() + " " + niveau);
		barChart.put("fieldName", dimension_name);

		JSONObject color = new JSONObject();
		color.put("fromColor", "#0097ba");
		color.put("toColor", "#ba9700");

		if (niveau.equals("region")) {
			color.put("fromColor", list_couleur_reg[com][0]);
			color.put("toColor", list_couleur_reg[com][1]);
//		color.put("opacity","100");
		} else {
			color.put("fromColor", list_couleur_dep[com][0]);
			color.put("toColor", list_couleur_dep[com][1]);
//		color.put("opacity","49");
		}

		color.put("path", "linear");
		color.put("d3", "custom");
		color.put("slices", 10);

		barChart.put("color", color);
		barChart.put("label", "");

		if (cc != 0) {
			barChart.put("stageId", stage);
		}

		barChart.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		options.put("disableBinning", true);
		JSONObject additionalFields = new JSONObject();
		additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("tableDisplayLocate", false);
		options.put("dateRounding", "");
		options.put("elasticX", false);
		options.put("precision", "");

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "average");
		fieldStats.put("fieldName", mesure_nom);
		options.put("fieldStats", fieldStats);
//	options.put("precision",4);
		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);
		options.put("stack", "__theme");
		options.put("showLegend", true);
		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");
		options.put("lastDock", "left");

		barChart.put("options", options);
		barChart.put("visible", true);

		JSONArray filters = new JSONArray();
		barChart.put("_filters", filters);
		barChart.put("size", "25");

		// generation nb random
		Random r = new Random();
		int grp = r.nextInt((99999 - 4444) + 1) + 4444;
		barChart.put("groupKey", "_group-" + grp);

		if (cc != 0) {

			barChart.put("top", 61.35652173913044);
			barChart.put("left", 0);
			barChart.put("width", 37.199999999999996);
			barChart.put("height", 28.8);

		} else {
			barChart.put("top", 30.67826086956522);
			barChart.put("left", 0);
			barChart.put("width", 37.199999999999996);
			barChart.put("height", 30.67826086956522);
		}

		JSONObject x = new JSONObject();
		x.put("type", "ordinal");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");

		barChart.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		JSONArray ydomain = new JSONArray();
		ydomain.add(0, null);
		ydomain.add(1, null);
		y.put("domain", ydomain);
		y.put("min", "");
		y.put("max", "");
		y.put("rigthY", false);
		y.put("rigthY", false);

		barChart.put("y", y);

		if (cc != 0) {
			JSONObject moving = new JSONObject();
			moving.put("o_t", 71.37391304347827);
			moving.put("o_l", 4.2);
			moving.put("o_x", 234);
			moving.put("o_y", 670);
			barChart.put("moving", moving);

			JSONObject resizing = new JSONObject();
			resizing.put("o_w", 20);
			resizing.put("o_h", 20);
			resizing.put("o_s", 25);
			resizing.put("o_x", 372);
			resizing.put("o_y", 739);
			barChart.put("resizing", resizing);
		} else {
			JSONObject moving = new JSONObject();
			moving.put("o_t", 39.44347826086957);
			moving.put("o_l", 1.5);
			moving.put("o_x", 189);
			moving.put("o_y", 374);
			barChart.put("moving", moving);

			JSONObject resizing = new JSONObject();
			resizing.put("o_w", 20);
			resizing.put("o_h", 20);
			resizing.put("o_s", 25);
			resizing.put("o_x", 374);
			resizing.put("o_y", 459);
			barChart.put("resizing", resizing);
		}

//	JSONObject moving = new JSONObject();
//	moving.put("o_t", 15);
//	moving.put("o_l", 15);
//	moving.put("o_x", 421);
//	moving.put("o_y", 150);
//	barChart.put("moving", moving);
//	
//	JSONObject resizing = new JSONObject();
//	resizing.put("o_w", 36.9);
//	resizing.put("o_h", 48.14860681114551);
//	resizing.put("o_s", 25);
//	resizing.put("o_x", 983);
//	resizing.put("o_y", 600);
//	barChart.put("resizing", resizing);

		return barChart;

	}

	@SuppressWarnings("unchecked")
	private static JSONObject addDimensionBarChartMulti(String dimension_name, String mesure_nom, int chartNumber,
			String niveau) {
		JSONObject barChart = new JSONObject();
		barChart.put("version", 4);
		barChart.put("type", "bar");

		barChart.put("dock", "float");

		barChart.put("title", niveau.toUpperCase() + " LEGEND ");
		barChart.put("fieldName", dimension_name);

		JSONObject color = new JSONObject();

		if (niveau.equals("region")) {
			color.put("fromColor", "#0086a6");
			color.put("toColor", "#b8b803");
			color.put("path", "linear");
			color.put("d3", "custom");
			color.put("slices", 10);

			barChart.put("color", color);
		} else {
			color.put("fromColor", "#79f7ee");
			color.put("toColor", "#f0f200");
			color.put("path", "linear");
			color.put("d3", "custom");
			color.put("slices", 10);

			barChart.put("color", color);
		}

		barChart.put("label", "");

		if (niveau.equals("region")) {
			barChart.put("stageId", "stage_bar_region");
		}

		barChart.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		options.put("disableBinning", true);
		JSONObject additionalFields = new JSONObject();
		additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("tableDisplayLocate", false);
		options.put("dateRounding", "");
		options.put("elasticX", false);
		options.put("precision", "");

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "average");
		fieldStats.put("fieldName", mesure_nom);
		options.put("fieldStats", fieldStats);
//	options.put("precision",4);
		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);
		options.put("stack", dimension_name);
		options.put("showLegend", true);
		options.put("lastDock", "left");
		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");

		barChart.put("options", options);
		barChart.put("visible", true);

		JSONArray filters = new JSONArray();
		barChart.put("_filters", filters);
		barChart.put("size", "25");

		if (niveau.equals("region")) {
//		barChart.put("groupKey","_group-63826");
			barChart.put("groupKey", "_group-10236");
			barChart.put("top", 31.504643962848295);
			barChart.put("left", 63);
		} else {
//		barChart.put("groupKey","_group-21248");
			barChart.put("groupKey", "_group-75987");
			barChart.put("top", 57.610047846889955);
			barChart.put("left", 0);
		}

		barChart.put("width", 36.6);
		barChart.put("height", 33.81459330143541);

		JSONObject x = new JSONObject();
		x.put("type", "ordinal");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");

		barChart.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		JSONArray ydomain = new JSONArray();
		ydomain.add(0, null);
		ydomain.add(1, null);
		y.put("domain", ydomain);
		y.put("min", "");
		y.put("max", "");
//	y.put("rigthY",false);
//	y.put("rigthY",false);

		barChart.put("y", y);

		if (niveau.equals("region")) {
			JSONObject moving = new JSONObject();
			moving.put("o_t", 56.35765550239235);
			moving.put("o_l", 34.5);
			moving.put("o_x", 984);
			moving.put("o_y", 482);
			barChart.put("moving", moving);

			JSONObject resizing = new JSONObject();
			resizing.put("o_w", 33.3);
			resizing.put("o_h", 32.5622009569378);
			resizing.put("o_s", 25);
			resizing.put("o_x", 1174);
			resizing.put("o_y", 730);
			barChart.put("resizing", resizing);
		} else {
			JSONObject moving = new JSONObject();
			moving.put("o_t", 56.35765550239235);
			moving.put("o_l", 0);
			moving.put("o_x", 333);
			moving.put("o_y", 479);
			barChart.put("moving", moving);

			JSONObject resizing = new JSONObject();
			resizing.put("o_w", 28.5);
			resizing.put("o_h", 28.835164835164836);
			resizing.put("o_s", 26);
			resizing.put("o_x", 486);
			resizing.put("o_y", 702);
			barChart.put("resizing", resizing);
		}
//	JSONObject moving = new JSONObject();
//	moving.put("o_t", 15);
//	moving.put("o_l", 15);
//	moving.put("o_x", 421);
//	moving.put("o_y", 150);
//	barChart.put("moving", moving);
//	
//	JSONObject resizing = new JSONObject();
//	resizing.put("o_w", 36.9);
//	resizing.put("o_h", 48.14860681114551);
//	resizing.put("o_s", 25);
//	resizing.put("o_x", 983);
//	resizing.put("o_y", 600);
//	barChart.put("resizing", resizing);

		return barChart;

	}

	@SuppressWarnings("unchecked")
	private static JSONObject addDimensionBarChartMultiOld(String dimension_name, String mesure_nom, int chartNumber,
			String niveau) {
		JSONObject barChart = new JSONObject();
		barChart.put("version", 4);
		barChart.put("type", "bar");

		barChart.put("dock", "float");

		barChart.put("title", niveau.toUpperCase() + " LEGEND ");
		barChart.put("fieldName", dimension_name);

		JSONObject color = new JSONObject();
		color.put("fromColor", "#0097ba");
		color.put("toColor", "#ba9700");
		color.put("path", "linear");
		color.put("d3", "category10");
		color.put("slices", 10);

		barChart.put("color", color);
		barChart.put("label", "");

		if (niveau.equals("region")) {
			barChart.put("stageId", "stage_bar_region");
		}

		barChart.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		options.put("disableBinning", true);
		JSONObject additionalFields = new JSONObject();
		additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("tableDisplayLocate", false);
		options.put("dateRounding", "");
		options.put("elasticX", false);
		options.put("precision", "");

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "average");
		fieldStats.put("fieldName", mesure_nom);
		options.put("fieldStats", fieldStats);
//	options.put("precision",4);
		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);
		options.put("stack", "__theme");
		options.put("showLegend", true);
		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");

		barChart.put("options", options);
		barChart.put("visible", true);

		JSONArray filters = new JSONArray();
		barChart.put("_filters", filters);
		barChart.put("size", "25");

		if (niveau.equals("region")) {
			barChart.put("groupKey", "_group-63826");
			barChart.put("top", 31.504643962848295);
			barChart.put("left", 63);
		} else {
			barChart.put("groupKey", "_group-21248");
			barChart.put("top", 57.610047846889955);
			barChart.put("left", 0);
		}

		barChart.put("width", 36.6);
		barChart.put("height", 33.81459330143541);

		JSONObject x = new JSONObject();
		x.put("type", "ordinal");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");

		barChart.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		JSONArray ydomain = new JSONArray();
		ydomain.add(0, null);
		ydomain.add(1, null);
		y.put("domain", ydomain);
		y.put("min", "");
		y.put("max", "");
//	y.put("rigthY",false);
//	y.put("rigthY",false);

		barChart.put("y", y);

		if (niveau.equals("region")) {
			JSONObject moving = new JSONObject();
			moving.put("o_t", 56.35765550239235);
			moving.put("o_l", 34.5);
			moving.put("o_x", 984);
			moving.put("o_y", 482);
			barChart.put("moving", moving);

			JSONObject resizing = new JSONObject();
			resizing.put("o_w", 33.3);
			resizing.put("o_h", 32.5622009569378);
			resizing.put("o_s", 25);
			resizing.put("o_x", 1174);
			resizing.put("o_y", 730);
			barChart.put("resizing", resizing);
		} else {
			JSONObject moving = new JSONObject();
			moving.put("o_t", 56.35765550239235);
			moving.put("o_l", 0);
			moving.put("o_x", 333);
			moving.put("o_y", 479);
			barChart.put("moving", moving);

			JSONObject resizing = new JSONObject();
			resizing.put("o_w", 28.5);
			resizing.put("o_h", 28.835164835164836);
			resizing.put("o_s", 26);
			resizing.put("o_x", 486);
			resizing.put("o_y", 702);
			barChart.put("resizing", resizing);
		}
//	JSONObject moving = new JSONObject();
//	moving.put("o_t", 15);
//	moving.put("o_l", 15);
//	moving.put("o_x", 421);
//	moving.put("o_y", 150);
//	barChart.put("moving", moving);
//	
//	JSONObject resizing = new JSONObject();
//	resizing.put("o_w", 36.9);
//	resizing.put("o_h", 48.14860681114551);
//	resizing.put("o_s", 25);
//	resizing.put("o_x", 983);
//	resizing.put("o_y", 600);
//	barChart.put("resizing", resizing);

		return barChart;

	}

	@SuppressWarnings("unchecked")
	private static JSONObject addPolygonDataTable(String dimension_name, String mesure_nom, int chartNumber,
			String niveau, Map<String, String> liste_legende) {

		JSONObject dataTable = new JSONObject();
		dataTable.put("version", 4);
		dataTable.put("type", "table");

		dataTable.put("dock", "float");

		dataTable.put("title", " VALEUR INDICATEUR " + niveau.toUpperCase());
		dataTable.put("fieldName", dimension_name);

		JSONObject color = new JSONObject();

		if (niveau.equals("region")) {
			color.put("fromColor", "#0086a6");
			color.put("toColor", "#b8b803");
			color.put("path", "linear");
			color.put("d3", "custom");
			color.put("slices", 10);

			dataTable.put("color", color);
		} else {
			color.put("fromColor", "#79f7ee");
			color.put("toColor", "#f0f200");
			color.put("path", "linear");
			color.put("d3", "custom");
			color.put("slices", 10);

			dataTable.put("color", color);
		}

		dataTable.put("label", "");

//	if (niveau.equals("region")) {
//		dataTable.put("stageId", "stage_bar_region");
//	}

//	dataTable.put("stageId", "stage_polygon_"+niveau);
		dataTable.put("stageId", "polygon_geo");

		dataTable.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		options.put("disableBinning", true);
		JSONObject additionalFields = new JSONObject();
//	additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("tableDisplayLocate", true);
		options.put("dateRounding", "");
		options.put("elasticX", false);
		options.put("sortEnabled", false);

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "featureCount");
		fieldStats.put("fieldName", "ID");
		options.put("fieldStats", fieldStats);
		options.put("precision", 4);
		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);
//	options.put("stack",dimension_name);
//	options.put("showLegend",true);
		options.put("lastDock", "bottom");

		JSONArray dataTableFieldNames = new JSONArray();

		int u = 0;

		JSONObject obj = null;
		while (u < liste_legende.size()) {

			obj = new JSONObject();

			obj.put("fieldName", liste_legende.get("" + u));
			obj.put("label", liste_legende.get("" + u));
			dataTableFieldNames.add(obj);
			u++;

		}
		options.put("dataTableFieldNames", dataTableFieldNames);
		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");

		dataTable.put("options", options);

		// ne pas oublier à remettre en true

		dataTable.put("visible", false);

		JSONArray filters = new JSONArray();
		dataTable.put("_filters", filters);
		dataTable.put("size", "25");

		/// CREATION DES GROUPS KEY RANDOM

		Random r = new Random();
		int grp = r.nextInt((99999 - 4444) + 1) + 4444;
		dataTable.put("groupKey", "_group-" + grp);

//	if (niveau.equals("region")) {
//		barChart.put("groupKey","_group-63826");
//		dataTable.put("groupKey","_group-10236");
//		dataTable.put("top",31.504643962848295);
//		dataTable.put("left",63);
//	}else {
//		barChart.put("groupKey","_group-21248");
//		dataTable.put("groupKey","_group-75987");
//		dataTable.put("top",57.610047846889955);
//		dataTable.put("left",0);
//	}

		dataTable.put("top", 72.52012383900929);
		dataTable.put("left", 0);

		dataTable.put("width", 95.1);
		dataTable.put("height", 17.238390092879257);

		JSONObject x = new JSONObject();
		x.put("type", "linear");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");

		dataTable.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		JSONArray ydomain = new JSONArray();
		ydomain.add(0, null);
		ydomain.add(1, null);
		y.put("domain", ydomain);
		y.put("min", "");
		y.put("max", "");
		y.put("rigthY", false);
//	y.put("rigthY",false);

		dataTable.put("y", y);

//	if (niveau.equals("region")) {
//		JSONObject moving = new JSONObject();
//		moving.put("o_t", 56.35765550239235);
//		moving.put("o_l", 34.5);
//		moving.put("o_x", 984);
//		moving.put("o_y", 482);
//		dataTable.put("moving", moving);
//		
//		JSONObject resizing = new JSONObject();
//		resizing.put("o_w", 33.3);
//		resizing.put("o_h", 32.5622009569378);
//		resizing.put("o_s", 25);
//		resizing.put("o_x", 1174);
//		resizing.put("o_y", 730);
//		dataTable.put("resizing", resizing);
//	}else {
//		JSONObject moving = new JSONObject();
//		moving.put("o_t", 56.35765550239235);
//		moving.put("o_l", 0);
//		moving.put("o_x", 333);
//		moving.put("o_y", 479);
//		dataTable.put("moving", moving);
//		
//		JSONObject resizing = new JSONObject();
//		resizing.put("o_w", 28.5);
//		resizing.put("o_h", 28.835164835164836);
//		resizing.put("o_s", 26);
//		resizing.put("o_x", 486);
//		resizing.put("o_y", 702);
//		dataTable.put("resizing", resizing);
//	}

		JSONObject moving = new JSONObject();
		moving.put("o_t", 56.470588235294116);
		moving.put("o_l", 6.6);
		moving.put("o_x", 1220);
		moving.put("o_y", 555);
		dataTable.put("moving", moving);
//	
//	JSONObject resizing = new JSONObject();
//	resizing.put("o_w", 36.9);
//	resizing.put("o_h", 48.14860681114551);
//	resizing.put("o_s", 25);
//	resizing.put("o_x", 983);
//	resizing.put("o_y", 600);
//	barChart.put("resizing", resizing);

		return dataTable;

	}

	@SuppressWarnings("unchecked")
	private static JSONObject addPolygonDataTableMulti(String dimension_name, String mesure_nom, int chartNumber,
			String niveau, Map<String, String> liste_legende) {

		JSONObject dataTable = new JSONObject();
		dataTable.put("version", 4);
		dataTable.put("type", "table");

		dataTable.put("dock", "float");

		dataTable.put("title", " VALEUR INDICATEUR " + niveau.toUpperCase());
		dataTable.put("fieldName", dimension_name);

		JSONObject color = new JSONObject();

		if (niveau.equals("region")) {
			color.put("fromColor", "#0086a6");
			color.put("toColor", "#b8b803");
			color.put("path", "linear");
			color.put("d3", "custom");
			color.put("slices", 10);

			dataTable.put("color", color);
		} else {
			color.put("fromColor", "#79f7ee");
			color.put("toColor", "#f0f200");
			color.put("path", "linear");
			color.put("d3", "custom");
			color.put("slices", 10);

			dataTable.put("color", color);
		}

		dataTable.put("label", "");

//	if (niveau.equals("region")) {
//		dataTable.put("stageId", "stage_bar_region");
//	}

		dataTable.put("stageId", "stage_polygon_" + niveau);
//	dataTable.put("stageId", "polygon_geo");

		dataTable.put("temporary", false);

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		options.put("disableBinning", true);
		JSONObject additionalFields = new JSONObject();
//	additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("tableDisplayLocate", true);
		options.put("dateRounding", "");
		options.put("elasticX", false);
		options.put("sortEnabled", false);

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "featureCount");
		fieldStats.put("fieldName", "ID");
		options.put("fieldStats", fieldStats);
		options.put("precision", 4);
		options.put("includeValueInLabel", false);
		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);
//	options.put("stack",dimension_name);
//	options.put("showLegend",true);
		options.put("lastDock", "bottom");

		JSONArray dataTableFieldNames = new JSONArray();

		int u = 0;

		JSONObject obj = null;
		while (u < liste_legende.size()) {

			obj = new JSONObject();

			obj.put("fieldName", liste_legende.get("" + u));
			obj.put("label", liste_legende.get("" + u));
			dataTableFieldNames.add(obj);
			u++;

		}
		options.put("dataTableFieldNames", dataTableFieldNames);
		options.put("tooltipsEnabled", false);
		options.put("compositeContainer", "");

		dataTable.put("options", options);
		dataTable.put("visible", false);

		JSONArray filters = new JSONArray();
		dataTable.put("_filters", filters);
		dataTable.put("size", "25");

		/// CREATION DES GROUPS KEY RANDOM

		Random r = new Random();
		int grp = r.nextInt((99999 - 4444) + 1) + 4444;
		dataTable.put("groupKey", "_group-" + grp);

//	if (niveau.equals("region")) {
//		barChart.put("groupKey","_group-63826");
//		dataTable.put("groupKey","_group-10236");
//		dataTable.put("top",31.504643962848295);
//		dataTable.put("left",63);
//	}else {
//		barChart.put("groupKey","_group-21248");
//		dataTable.put("groupKey","_group-75987");
//		dataTable.put("top",57.610047846889955);
//		dataTable.put("left",0);
//	}

		dataTable.put("top", 72.52012383900929);
		dataTable.put("left", 0);

		dataTable.put("width", 95.1);
		dataTable.put("height", 17.238390092879257);

		JSONObject x = new JSONObject();
		x.put("type", "linear");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");

		dataTable.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		JSONArray ydomain = new JSONArray();
		ydomain.add(0, null);
		ydomain.add(1, null);
		y.put("domain", ydomain);
		y.put("min", "");
		y.put("max", "");
		y.put("rigthY", false);
//	y.put("rigthY",false);

		dataTable.put("y", y);

//	if (niveau.equals("region")) {
//		JSONObject moving = new JSONObject();
//		moving.put("o_t", 56.35765550239235);
//		moving.put("o_l", 34.5);
//		moving.put("o_x", 984);
//		moving.put("o_y", 482);
//		dataTable.put("moving", moving);
//		
//		JSONObject resizing = new JSONObject();
//		resizing.put("o_w", 33.3);
//		resizing.put("o_h", 32.5622009569378);
//		resizing.put("o_s", 25);
//		resizing.put("o_x", 1174);
//		resizing.put("o_y", 730);
//		dataTable.put("resizing", resizing);
//	}else {
//		JSONObject moving = new JSONObject();
//		moving.put("o_t", 56.35765550239235);
//		moving.put("o_l", 0);
//		moving.put("o_x", 333);
//		moving.put("o_y", 479);
//		dataTable.put("moving", moving);
//		
//		JSONObject resizing = new JSONObject();
//		resizing.put("o_w", 28.5);
//		resizing.put("o_h", 28.835164835164836);
//		resizing.put("o_s", 26);
//		resizing.put("o_x", 486);
//		resizing.put("o_y", 702);
//		dataTable.put("resizing", resizing);
//	}

		JSONObject moving = new JSONObject();
		moving.put("o_t", 56.470588235294116);
		moving.put("o_l", 6.6);
		moving.put("o_x", 1220);
		moving.put("o_y", 555);
		dataTable.put("moving", moving);
//	
//	JSONObject resizing = new JSONObject();
//	resizing.put("o_w", 36.9);
//	resizing.put("o_h", 48.14860681114551);
//	resizing.put("o_s", 25);
//	resizing.put("o_x", 983);
//	resizing.put("o_y", 600);
//	barChart.put("resizing", resizing);

		return dataTable;

	}

	@SuppressWarnings("unchecked")
	private static JSONObject addMesureComboChart(int chartNumber) {

		JSONObject comboChart = new JSONObject();
		comboChart.put("version", 3);
		comboChart.put("type", "combo");

		// choix de la position
		if (chartNumber > 7) {
			comboChart.put("dock", "float");
		} else if (chartNumber > 3) {
			comboChart.put("dock", "bottom");
		} else {
			comboChart.put("dock", "left");
		}

		comboChart.put("title", ("Measures").toUpperCase());
		comboChart.put("fieldName", "Measures");

		JSONObject color = new JSONObject();
		color.put("fromColor", "#0097ba");
		color.put("toColor", "#ba9700");
		color.put("path", "linear");
		color.put("d3", "category10");
		color.put("slices", 10);

		comboChart.put("color", color);
		comboChart.put("label", "");

		JSONObject options = new JSONObject();
		options.put("mode", "Average");
		options.put("ticks", 5);
		options.put("multiplier", 1);
		options.put("scaling", 0.95);
		JSONObject additionalFields = new JSONObject();
		options.put("additionalFields", additionalFields);
		options.put("dateTimeFormat", "");
		options.put("dateTimeSubstring", null);
		options.put("commaSeperatedData", false);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);
		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "featureCount");
		fieldStats.put("fieldName", "ID");
		options.put("fieldStats", fieldStats);
		options.put("precision", 4);
		options.put("includeValueInLabel", true);
		options.put("includePercentInLabel", true);

		comboChart.put("options", options);
		comboChart.put("visible", true);

		JSONArray filters = new JSONArray();
		comboChart.put("filters", filters);
		comboChart.put("size", "25");
		comboChart.put("groupKey", "_group-498");
		comboChart.put("top", 15);
		comboChart.put("left", 15);
		comboChart.put("width", 20);
		comboChart.put("height", 20);

		JSONObject x = new JSONObject();
		x.put("type", "linear");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("bins", "");
		x.put("interpolate", "linear");

		comboChart.put("x", x);
		return comboChart;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addMesureThemeChart(String mesure_name, int chartNumber) {
		JSONObject themeChart = new JSONObject();
		themeChart.put("version", 3);
		themeChart.put("type", "theme");

		// choix de la position
		if (chartNumber > 7) {
			themeChart.put("dock", "float");
		} else if (chartNumber > 3) {
			themeChart.put("dock", "bottom");
		} else {
			themeChart.put("dock", "left");
		}

		themeChart.put("title", mesure_name.toUpperCase());
		themeChart.put("fieldName", mesure_name);

		JSONObject color = new JSONObject();
		color.put("fromColor", "#f6ddd6");
		color.put("toColor", "#ed5f30");
		color.put("path", "linear");
		color.put("d3", "custom");
		color.put("slices", 10);

		themeChart.put("color", color);
		themeChart.put("label", "");

		JSONObject options = new JSONObject();
		options.put("scaling", 0.95);
		JSONObject additionalFields = new JSONObject();
		options.put("additionalFields", additionalFields);
		options.put("dateTimeSubstring", null);
		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "featureCount");
		fieldStats.put("fieldName", "ID");
		options.put("fieldStats", fieldStats);
		options.put("precision", 4);
		options.put("includeValueInLabel", true);
		options.put("includePercentInLabel", true);
		options.put("stockType", "theme");
		JSONArray alternateThemes = new JSONArray();
		options.put("alternateThemes", alternateThemes);
		options.put("activeTheme", "");
		options.put("groupFilter", null);
		options.put("activeScaling", 0.95);

		themeChart.put("options", options);
		themeChart.put("visible", true);

		JSONArray filters = new JSONArray();
		themeChart.put("filters", filters);
		themeChart.put("size", "25");
		themeChart.put("groupKey", "_group-18600");
		themeChart.put("top", 15);
		themeChart.put("left", 15);
		themeChart.put("width", 20);
		themeChart.put("height", 15);

		JSONObject x = new JSONObject();
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");

		themeChart.put("x", x);

		JSONObject groupConfiguration = new JSONObject();
		groupConfiguration.put("groups", 5);
		groupConfiguration.put("precision", 0);
		groupConfiguration.put("method", "equalInterval");
		groupConfiguration.put("min", null);
		groupConfiguration.put("max", null);

		themeChart.put("groupConfiguration", groupConfiguration);
		themeChart.put("triggerUpdate", false);

		return themeChart;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addIndicatorThemeChart(String indicator_name, int chartNumber) {
		JSONObject themeChart = new JSONObject();
		themeChart.put("version", 4);
		themeChart.put("type", "theme");
		themeChart.put("dock", "float");
		// choix de la position
//		if (chartNumber > 7) {
//			themeChart.put("dock", "float");
//		} else if (chartNumber > 3) {
//			themeChart.put("dock", "bottom");
//		} else {
//			themeChart.put("dock", "left");
//		}

		themeChart.put("title", indicator_name.toUpperCase());
		themeChart.put("fieldName", "_Legend");

		JSONObject color = new JSONObject();
		color.put("d3", "custom");
		color.put("fromColor", "#0097ba");
		color.put("toColor", "#ba9700");
		color.put("path", "linear");
		color.put("d3", "category10");
		color.put("slices", 10);
		color.put("domain", "#ffffff");

		themeChart.put("color", color);
		themeChart.put("label", "");
		themeChart.put("temporary", false);

		JSONObject options = new JSONObject();

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "sum");
		fieldStats.put("fieldName", indicator_name);
		options.put("fieldStats", fieldStats);

		options.put("includeValueInLabel", true);
		options.put("precision", 4);

		options.put("stockType", "theme");

		options.put("scaling", 0.95);

		JSONArray alternateThemes = new JSONArray();
		options.put("alternateThemes", alternateThemes);

		JSONObject additionalFields = new JSONObject();
		options.put("additionalFields", additionalFields);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("activeTheme", "");
		options.put("tooltipsEnabled", false);
		options.put("activeScaling", 0.95);

		options.put("includePercentInLabel", true);

		themeChart.put("options", options);
		themeChart.put("visible", false);

		JSONArray filters = new JSONArray();
		themeChart.put("_filters", filters);
		themeChart.put("size", "25");
		themeChart.put("groupKey", "_group-18600");
		themeChart.put("top", 15);
		themeChart.put("left", 15);
		themeChart.put("width", 20);
		themeChart.put("height", 15);

		JSONObject x = new JSONObject();
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");

		themeChart.put("x", x);

		JSONObject y = new JSONObject();
		JSONArray domainy = new JSONArray();
		domainy.add(0, null);
		domainy.add(1, null);
		y.put("domain", domain);
		y.put("min", "");
		y.put("max", "");

		themeChart.put("y", y);

		return themeChart;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addIndicatorThemeChartNMeasure(String indicator_name, String mesure, int nb, String stage,
			Map<String, String> liste_legende) {

		String[][] list_couleur = { { "#e5f2ff", "#1300ff" }, { "#fce5ff", "#e300ff" }, { "#c6fee0", "#07ff00" },
				{ "#fef4c6", "#d1a200" }, { "#ecf6c7", "#f0fa00" } };
//	String[][] list_couleur = { {"#e5f2ff", "#8f86ff"}, {"#fce5ff", "#e300ff"}, {"#c6fee0", "#07ff00"}, {"#fef4c6", "#d1a200"}, {"#ecf6c7", "#f0fa00"} };

//	#b1c6db, #1300ff

		JSONObject themeChart = new JSONObject();
		themeChart.put("version", 4);
		themeChart.put("type", "theme");

		// choix de la position
//	if(chartNumber > 7) {
//		themeChart.put("dock","float");
//	}else if (chartNumber > 3) {
//		themeChart.put("dock","bottom");
//	} else {
//		themeChart.put("dock","left");	
//	}

		themeChart.put("dock", "left");

		themeChart.put("title", indicator_name.toUpperCase());
		themeChart.put("fieldName", indicator_name);

		JSONObject color = new JSONObject();

		if (liste_legende.size() == 1) {

			color.put("d3", "manual");
		} else {
			color.put("d3", "custom");
		}

//	color.put("d3","custom");

		color.put("fromColor", list_couleur[nb][0]);
		color.put("toColor", list_couleur[nb][1]);
		color.put("path", "linear");
		color.put("opacity", "100");
		color.put("domaine", "#ffffff");

		if (liste_legende.size() == 1) {
			color.put("domain", list_couleur[nb][1]);
		}

		themeChart.put("color", color);
		themeChart.put("label", "");

		if (nb != 0) {
			themeChart.put("stageId", stage);
		}

		themeChart.put("temporary", false);

		JSONObject options = new JSONObject();

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "average");
		fieldStats.put("fieldName", mesure);
		options.put("fieldStats", fieldStats);

		options.put("includeValueInLabel", true);
		options.put("precision", 4);

		options.put("stockType", "theme");

		options.put("scaling", 0.95);

		JSONArray alternateThemes = new JSONArray();
		options.put("alternateThemes", alternateThemes);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);

		JSONObject additionalFields = new JSONObject();
		additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);
		options.put("includePercentInLabel", true);
		options.put("hideLabels", false);
		options.put("activeTheme", "");
		options.put("tableDisplayLocate", false);
		options.put("mode", "");
		options.put("dateRounding", "");
		options.put("commaSeperatedData", false);
		options.put("suppressThemeGrouping", false);

//	JSONObject groupFilter = new JSONObject();
//	groupFilter.put("filterNull", false);
//	groupFilter.put("includeCount", 0);

		options.put("tooltipsEnabled", false);
		options.put("groupFilter", null);
		options.put("activeScaling", 0.95);

		themeChart.put("options", options);

		themeChart.put("visible", false);

		JSONArray filters = new JSONArray();
		themeChart.put("_filters", filters);
		themeChart.put("size", "25");

		// generation nb random
		Random r = new Random();
		int grp = r.nextInt((99999 - 4444) + 1) + 4444;

		themeChart.put("groupKey", "_group-" + grp);
		themeChart.put("top", 15);
		themeChart.put("left", 15);
		themeChart.put("width", 20);
		themeChart.put("height", 15);

		JSONObject x = new JSONObject();
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");

		themeChart.put("x", x);

		JSONObject y = new JSONObject();
		JSONArray domainy = new JSONArray();
		domainy.add(0, null);
		domainy.add(1, null);
		y.put("domain", domain);
		y.put("min", "");
		y.put("max", "");

		themeChart.put("y", y);

		return themeChart;

	}

// mesure : nom de la mesure || nb : choisi la colonne de la couleur approprié, stage : le stage a gérer.

	@SuppressWarnings("unchecked")
	private static JSONObject addIndicatorThemeChartMultiNMeasure(String indicator_name, String mesure, int com, int cc,
			String stage, String niveau, Map<String, String> liste_legende) {

//	String[][] list_couleur = { {"#e5f2ff", "#1300ff"}, {"#fce5ff", "#e300ff"}, {"#c6fee0", "#07ff00"}, {"#fef4c6", "#d1a200"}, {"#ecf6c7", "#f0fa00"} };
		String[][] list_couleur_dep = { { "#e5f2ff", "#8f86ff" }, { "#fce5ff", "#e300ff" }, { "#c6fee0", "#07ff00" },
				{ "#fef4c6", "#d1a200" }, { "#ecf6c7", "#f0fa00" } };
		String[][] list_couleur_reg = { { "#b1c6db", "#1300ff" }, { "#c8afcb", "#a600ba" }, { "#9cc8b1", "#04a100" },
				{ "#cec494", "#9d7a00" }, { "#b8bf9e", "#d4dd00" } };

//	#b1c6db, #1300ff

		JSONObject themeChart = new JSONObject();
		themeChart.put("version", 4);
		themeChart.put("type", "theme");

		// choix de la position
//	if(chartNumber > 7) {
//		themeChart.put("dock","float");d
//	}else if (chartNumber > 3) {
//		themeChart.put("dock","bottom");
//	} else {
//		themeChart.put("dock","left");	
//	}

		themeChart.put("dock", "left");

//	themeChart.put("title", indicator_name.toUpperCase());	
		themeChart.put("title", " THEME " + mesure.toUpperCase() + " " + niveau);
		themeChart.put("fieldName", indicator_name);

		JSONObject color = new JSONObject();

		if (liste_legende.size() == 1) {

			color.put("d3", "manual");
		} else {
			color.put("d3", "custom");
		}

//	color.put("d3","custom");

		// METTRE UN TEST SUR LA DIMENSION SPATIALE

		if (niveau.equals("region")) {
			color.put("fromColor", list_couleur_reg[com][0]);
			color.put("toColor", list_couleur_reg[com][1]);
			color.put("opacity", "100");
		} else {
			color.put("fromColor", list_couleur_dep[com][0]);
			color.put("toColor", list_couleur_dep[com][1]);
			color.put("opacity", "49");
		}
//	color.put("fromColor",list_couleur_dep[com][0]);
//	color.put("toColor",list_couleur_dep[com][1]);
		color.put("path", "linear");

		color.put("domaine", "#ffffff");

		if (liste_legende.size() == 1) {

			if (niveau.equals("region")) {
				color.put("domain", list_couleur_reg[com][1]);
			} else {
				color.put("domain", list_couleur_dep[com][1]);
			}

		}

		themeChart.put("color", color);
		themeChart.put("label", "");

		if (cc != 0) {
			themeChart.put("stageId", stage);
		}

		themeChart.put("temporary", false);

		JSONObject options = new JSONObject();

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "average");
		fieldStats.put("fieldName", mesure);
		options.put("fieldStats", fieldStats);

		options.put("includeValueInLabel", true);
		options.put("precision", 4);

		options.put("stockType", "theme");

		options.put("scaling", 0.95);

		JSONArray alternateThemes = new JSONArray();
		options.put("alternateThemes", alternateThemes);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);

		JSONObject additionalFields = new JSONObject();
//	additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);
		options.put("includePercentInLabel", true);
		options.put("hideLabels", false);
		options.put("activeTheme", "");
		options.put("tableDisplayLocate", false);
		options.put("mode", "");
		options.put("dateRounding", "");
		options.put("commaSeperatedData", false);
		options.put("suppressThemeGrouping", false);

		JSONObject groupFilter = new JSONObject();
		groupFilter.put("filterNull", false);
		groupFilter.put("includeCount", 0);
		options.put("groupFilter", groupFilter);

		options.put("tooltipsEnabled", false);
//	options.put("groupFilter",null);
		options.put("activeScaling", 0.95);

		themeChart.put("options", options);

		themeChart.put("visible", false);

		JSONArray filters = new JSONArray();
		themeChart.put("_filters", filters);
		themeChart.put("size", "25");

		// generation nb random
		Random r = new Random();
		int grp = r.nextInt((79999 - 4444) + 1) + 4444;

		themeChart.put("groupKey", "_group-" + grp);
		themeChart.put("top", 15);
		themeChart.put("left", 15);
		themeChart.put("width", 20);
		themeChart.put("height", 15);

		JSONObject x = new JSONObject();
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");

		themeChart.put("x", x);

		JSONObject y = new JSONObject();
		JSONArray domainy = new JSONArray();
		domainy.add(0, null);
		domainy.add(1, null);
		y.put("domain", domain);
		y.put("min", "");
		y.put("max", "");

		themeChart.put("y", y);

		return themeChart;

	}

	@SuppressWarnings("unchecked")
	private static JSONObject addIndicatorThemeChartDepartement(String indicator_name, int chartNumber) {
		JSONObject themeChart = new JSONObject();
		themeChart.put("version", 4);
		themeChart.put("type", "theme");

		// choix de la position
		if (chartNumber > 7) {
			themeChart.put("dock", "float");
		} else if (chartNumber > 3) {
			themeChart.put("dock", "bottom");
		} else {
			themeChart.put("dock", "left");
		}

		themeChart.put("title", "DEPARTEMENT THEME ");
		themeChart.put("fieldName", indicator_name);

		JSONObject color = new JSONObject();
		color.put("d3", "custom");

		color.put("fromColor", "#23d6ff");
		color.put("toColor", "#feff70");
		color.put("path", "linear");
		color.put("opacity", "50");

		themeChart.put("color", color);
		themeChart.put("label", "");
		themeChart.put("temporary", false);

		JSONObject options = new JSONObject();

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "featureCount");
		fieldStats.put("fieldName", "ID");
		options.put("fieldStats", fieldStats);

		options.put("includeValueInLabel", true);
		options.put("precision", 4);

		options.put("stockType", "theme");

		options.put("scaling", 0.95);

		JSONArray alternateThemes = new JSONArray();
		options.put("alternateThemes", alternateThemes);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);

		JSONObject additionalFields = new JSONObject();
		options.put("additionalFields", additionalFields);

//	JSONObject groupFilter = new JSONObject();
//	groupFilter.put("filterNull", false);
//	groupFilter.put("includeCount", 0);
		options.put("groupFilter", null);

		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("activeTheme", "");
		options.put("tooltipsEnabled", false);
		options.put("activeScaling", 0.95);

		options.put("includePercentInLabel", false);

		themeChart.put("options", options);
		themeChart.put("visible", false);

		JSONArray filters = new JSONArray();
		themeChart.put("_filters", filters);
		themeChart.put("size", "25");
		themeChart.put("groupKey", "_group-86792");
		themeChart.put("top", 15);
		themeChart.put("left", 15);
		themeChart.put("width", 20);
		themeChart.put("height", 15);

		JSONObject x = new JSONObject();
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");

		themeChart.put("x", x);

		JSONObject y = new JSONObject();
		JSONArray domainy = new JSONArray();
		domainy.add(0, null);
		domainy.add(1, null);
		y.put("domain", domain);
		y.put("min", "");
		y.put("max", "");

		themeChart.put("y", y);

		return themeChart;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addIndicatorThemeChartRegion(String indicator_name, int chartNumber) {
		JSONObject themeChart = new JSONObject();
		themeChart.put("version", 4);
		themeChart.put("type", "theme");

		// choix de la position
//	if(chartNumber > 7) {
//		themeChart.put("dock","float");
//	}else if (chartNumber > 3) {
//		themeChart.put("dock","bottom");
//	} else {
//		themeChart.put("dock","left");	
//	}
		themeChart.put("dock", "float");

		themeChart.put("title", "REGION THEME ");
		themeChart.put("fieldName", indicator_name);

		JSONObject color = new JSONObject();
		color.put("d3", "custom");

		color.put("fromColor", "#0086a6");
		color.put("toColor", "#b8b803");
		color.put("path", "linear");
		color.put("slice", 10);

		themeChart.put("color", color);
		themeChart.put("label", "");
		themeChart.put("stageId", "stage_bar_region");
		themeChart.put("temporary", false);

		JSONObject options = new JSONObject();

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "featureCount");
		fieldStats.put("fieldName", "ID");
		options.put("fieldStats", fieldStats);

		options.put("includeValueInLabel", true);
		options.put("precision", 4);

		options.put("stockType", "theme");

		options.put("scaling", 0.95);

		JSONArray alternateThemes = new JSONArray();
		options.put("alternateThemes", alternateThemes);

		options.put("dateTimeSubstring", null);
		options.put("compositeContainer", "");
		options.put("disableBinning", false);
		options.put("elasticX", false);

		JSONObject additionalFields = new JSONObject();
		additionalFields.put("y", "");
		options.put("additionalFields", additionalFields);

//	JSONObject groupFilter = new JSONObject();
//	groupFilter.put("filterNull", false);
//	groupFilter.put("includeCount", 0);
		options.put("groupFilter", null);

		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("activeTheme", "");
		options.put("tooltipsEnabled", false);
		options.put("activeScaling", 0.95);
		options.put("tableDisplayLocate", true);
		options.put("mode", "");
		options.put("dateRounding", "");
		options.put("commaSeperatedData", false);

		options.put("includePercentInLabel", false);
		options.put("suppressThemeGrouping", false);

		themeChart.put("options", options);
		themeChart.put("visible", false);

		JSONArray filters = new JSONArray();
		themeChart.put("_filters", filters);
		themeChart.put("size", "25");
		themeChart.put("groupKey", "_group-40997");
		themeChart.put("top", 5.626373626373627);
		themeChart.put("left", 76.8);
		themeChart.put("width", 21.3);
		themeChart.put("height", 26.12526997840173);

		JSONObject x = new JSONObject();
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);
		x.put("min", "");
		x.put("max", "");
		x.put("interpolate", "");
		x.put("type", "");

		themeChart.put("x", x);

		JSONObject y = new JSONObject();
		JSONArray domainy = new JSONArray();
		domainy.add(0, null);
		domainy.add(1, null);
		y.put("domain", domain);
		y.put("min", "");
		y.put("max", "");
		y.put("type", "auto");

		themeChart.put("y", y);

		return themeChart;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject addMesuresThemeCharts(List<String> mesures, int chartNumber) {

		// il faut d'abord ajouter la thÃ¨me de la premiÃ¨re mesure ensuite on ajouter
		// les autres mesure si on en a comme des "alternateThemes"
		String mesure_name = mesures.get(0);
		JSONObject themeChart = new JSONObject();
		themeChart.put("version", 4);
		themeChart.put("type", "theme");

		// choix de la position
//	if(chartNumber > 7) {
//		themeChart.put("dock","float");
//	}else if (chartNumber > 3) {
//		themeChart.put("dock","bottom");
//	} else {
//		themeChart.put("dock","left");	
//	}
//	
		themeChart.put("dock", "left");
//	themeChart.put("title",mesure_name.toUpperCase());
		themeChart.put("title", "PALETTE COULEUR");
		themeChart.put("fieldName", mesure_name);

		JSONObject color = new JSONObject();
//	{"d3":"manual","fromColor":"#0097ba","toColor":"#FF0000","path":"linear","opacity":"90","domain":"#ffefd8,#ffdca8,#fec267,#ffad30,#f19100"}

		color.put("d3", "manual");
		color.put("fromColor", "#0097ba");
		color.put("toColor", "#FF0000");
		color.put("path", "linear");
		color.put("opacity", "90");
		color.put("domain", "#ffefd8,#ffdca8,#fec267,#ffad30,#f19100");
//	color.put("slices",10);

		themeChart.put("color", color);
		themeChart.put("label", "");

		JSONObject options = new JSONObject();

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "featureCount");
		fieldStats.put("fieldName", "ID");
		options.put("fieldStats", fieldStats);

		options.put("includeValueInLabel", true);
		options.put("precision", 4);

		options.put("stockType", "theme");
		options.put("scaling", 0.95);

		JSONArray alternateThemes = new JSONArray();

		// il faut ajouter les chartes pour les autres mesures sans ajouter la premiÃ¨re
		// mesure une deuxiÃ¨me fois
		if (mesures.size() > 1) {
			int alter_thm_ind = 0;
			for (String mesure : mesures) {
				if (alter_thm_ind != 0) {
					alternateThemes.add(alter_thm_ind - 1, addMesureAlternateThemeChart(mesure, alter_thm_ind - 1));
				}
				alter_thm_ind++;
			}
		}
		options.put("alternateThemes", alternateThemes);
		options.put("dateTimeSubstring", null);

		JSONObject additionalFields = new JSONObject();
		options.put("additionalFields", additionalFields);

		options.put("groupFilter", null);

		options.put("tableFeaturesPerPage", 10);
		options.put("tableGroupFeatures", true);

		options.put("includePercentInLabel", true);
		options.put("hideLabels", false);
		options.put("activeTheme", "");
		options.put("tooltipsEnabled", false);
		options.put("activeScaling", 0.95);

		themeChart.put("options", options);
		themeChart.put("visible", true);

		JSONArray filters = new JSONArray();
		themeChart.put("_filters", filters);
		themeChart.put("size", "25");
		themeChart.put("groupKey", "_group-23843");
		themeChart.put("top", 15);
		themeChart.put("left", 15);
		themeChart.put("width", 20);
		themeChart.put("height", 15);

		JSONObject x = new JSONObject();
		x.put("min", "");
		x.put("max", "");
		JSONArray domain = new JSONArray();
		domain.add(0, null);
		domain.add(1, null);
		x.put("domain", domain);

		themeChart.put("x", x);

		JSONObject y = new JSONObject();
		y.put("type", "auto");
		JSONArray domainy = new JSONArray();
		domainy.add(0, null);
		domainy.add(1, null);
		y.put("domain", domainy);

		themeChart.put("y", y);

		JSONObject groupConfiguration = new JSONObject();
		groupConfiguration.put("groups", 5);
		groupConfiguration.put("precision", 0);
		groupConfiguration.put("method", "equalInterval");
		groupConfiguration.put("min", null);
		groupConfiguration.put("max", null);

		themeChart.put("groupConfiguration", groupConfiguration);

//	JSONObject moving = new JSONObject();
//	moving.put("o_t",15);
//	moving.put("o_l",15);
//	moving.put("o_x",0);
//	moving.put("o_y",0);
//
//	themeChart.put("moving", moving);

		themeChart.put("triggerUpdate", false);

		return themeChart;
	}

	/*
	 * @SuppressWarnings("unchecked") private static JSONObject
	 * addMesuresThemeCharts(List<String> mesures, int chartNumber) {
	 * 
	 * // il faut d'abord ajouter la thÃ¨me de la premiÃ¨re mesure ensuite on
	 * ajouter les autres mesure si on en a comme des "alternateThemes" String
	 * mesure_name = mesures.get(0); JSONObject themeChart = new JSONObject();
	 * themeChart.put("version", 3); themeChart.put("type","theme");
	 * 
	 * //choix de la position if(chartNumber > 7) { themeChart.put("dock","float");
	 * }else if (chartNumber > 3) { themeChart.put("dock","bottom"); } else {
	 * themeChart.put("dock","left"); }
	 * 
	 * themeChart.put("title",mesure_name.toUpperCase());
	 * themeChart.put("fieldName",mesure_name);
	 * 
	 * JSONObject color = new JSONObject(); color.put("fromColor","#fa9898");
	 * color.put("toColor","#ff0000"); color.put("path","linear");
	 * color.put("d3","custom"); color.put("slices",10);
	 * 
	 * 
	 * themeChart.put("color",color); themeChart.put("label","");
	 * 
	 * JSONObject options = new JSONObject(); options.put("scaling",0.95);
	 * 
	 * JSONObject additionalFields = new JSONObject();
	 * options.put("additionalFields",additionalFields);
	 * options.put("dateTimeSubstring",null);
	 * options.put("tableFeaturesPerPage",10);
	 * options.put("tableGroupFeatures",true);
	 * 
	 * JSONObject fieldStats = new JSONObject();
	 * fieldStats.put("mode","featureCount"); fieldStats.put("fieldName","ID");
	 * options.put("fieldStats", fieldStats); options.put("precision",4);
	 * options.put("includeValueInLabel",true);
	 * options.put("includePercentInLabel",true); options.put("stockType","theme");
	 * options.put("activeTheme",""); options.put("groupFilter",null);
	 * options.put("activeScaling",0.95);
	 * 
	 * JSONArray alternateThemes = new JSONArray();
	 * 
	 * // il faut ajouter les chartes pour les autres mesures sans ajouter la
	 * premiÃ¨re mesure une deuxiÃ¨me fois if (mesures.size()>1) { int alter_thm_ind
	 * = 0; for (String mesure : mesures) { if (alter_thm_ind != 0) {
	 * alternateThemes.add(alter_thm_ind-1, addMesureAlternateThemeChart(mesure,
	 * alter_thm_ind-1)); } alter_thm_ind++; } }
	 * 
	 * options.put("alternateThemes",alternateThemes);
	 * 
	 * themeChart.put("options", options); themeChart.put("visible",true);
	 * 
	 * JSONArray filters = new JSONArray(); themeChart.put("filters",filters);
	 * themeChart.put("size","25"); themeChart.put("groupKey","_group-23843");
	 * themeChart.put("top",15); themeChart.put("left",15);
	 * themeChart.put("width",20); themeChart.put("height",15);
	 * 
	 * JSONObject x = new JSONObject(); JSONArray domain = new JSONArray();
	 * domain.add(0, null); domain.add(1, null); x.put("domain",domain);
	 * x.put("min",""); x.put("max","");
	 * 
	 * themeChart.put("x", x);
	 * 
	 * JSONObject groupConfiguration = new JSONObject();
	 * groupConfiguration.put("groups",5); groupConfiguration.put("precision",0);
	 * groupConfiguration.put("method","equalInterval");
	 * groupConfiguration.put("min",null); groupConfiguration.put("max",null);
	 * 
	 * themeChart.put("groupConfiguration", groupConfiguration);
	 * 
	 * JSONObject moving = new JSONObject(); moving.put("o_t",15);
	 * moving.put("o_l",15); moving.put("o_x",0); moving.put("o_y",0);
	 * 
	 * themeChart.put("moving", moving);
	 * 
	 * themeChart.put("triggerUpdate", false);
	 * 
	 * return themeChart; }
	 */

	@SuppressWarnings("unchecked")
	private static JSONObject addMesureAlternateThemeChart(String mesure_name, int chartNumber) {

		JSONObject alternateTheme = new JSONObject();
		alternateTheme.put("version", 2);
		alternateTheme.put("type", "pie");

		alternateTheme.put("dock", "left");

		alternateTheme.put("title", mesure_name.toUpperCase());
		alternateTheme.put("fieldName", mesure_name);

		JSONObject color = new JSONObject();
		color.put("fromColor", "#fa9898");
		color.put("toColor", "#ff0000");
		color.put("path", "linear");
		color.put("d3", "custom");
		color.put("domain", "");
		color.put("slices", 10);

		alternateTheme.put("color", color);
		alternateTheme.put("label", "");

		JSONObject options = new JSONObject();
		options.put("scaling", 0.95);
		JSONObject additionalFields = new JSONObject();
		options.put("additionalFields", additionalFields);
		options.put("dateTimeSubstring", null);

		JSONObject fieldStats = new JSONObject();
		fieldStats.put("mode", "featureCount");
		fieldStats.put("fieldName", "ID");
		options.put("fieldStats", fieldStats);
		options.put("precision", 4);
		options.put("includeValueInLabel", true);
		options.put("includePercentInLabel", true);
		// options.put("activeTheme","");

		// JSONObject groupFilter = new JSONObject();
		// groupFilter.put("filterNull",true);
		// groupFilter.put("includeCount",0);
		// options.put("groupFilter",groupFilter);

		alternateTheme.put("options", options);
		// alternateTheme.put("visible",true);

		JSONArray filters = new JSONArray();
		alternateTheme.put("filters", filters);
		alternateTheme.put("groupKey", "_group-30682");
		alternateTheme.put("top", 15);
		alternateTheme.put("left", 15);
		alternateTheme.put("width", 20);
		alternateTheme.put("height", 15);

		JSONObject x = new JSONObject();
		alternateTheme.put("x", x);
		alternateTheme.put("legendItemPrefix", "");

		JSONObject groupConfiguration = new JSONObject();
		groupConfiguration.put("groups", 5);
		groupConfiguration.put("precision", 0);
		groupConfiguration.put("method", "equalInterval");
		groupConfiguration.put("min", null);
		groupConfiguration.put("max", null);

		alternateTheme.put("groupConfiguration", groupConfiguration);

		return alternateTheme;
	}

}
