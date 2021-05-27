/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Main_App;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import DataObjects.Geometrie;
import DataObjects.Indicateur;
import DataObjects.Point;

public class GML_SLD {

	public static int map_number = 0; // pour compter le nombe de cartes à afficher
	static Double shifting = new Double(0.0);

	static Map<String, Double> max_ind_Values;
	static Map<String, Double> min_ind_Values;
	static Map<String, Double> count_mes_ind; // pour compter le nombre d’indicateurs pour chaque mesure à afficher dans
												// chaque zone spatiale
	static Map<String, Integer> count_map_by_type; // pour compter le nombre de map pour chaque type d'affichage
													// (cloropeth, circle, bars)

	static Map<String, Double> max_mes_Values;
	static Map<String, Double> min_mes_Values;

	static Map<String, Double> sum_mes_sp_Values;
	static Map<String, Double> sum_mes_sp_Values_curr;

	static HashMap<String, Geometrie> geometry = new HashMap<String, Geometrie>();;

	// la position du nom la mesure dans le nom de l'indiquateur
	// static int mesure_position = CreateIndicators.separator.length() +
	// CreateIndicators.initial.length();
	public static void execute(HttpServletRequest request, List<Indicateur> liste,
			List<Measure_Display> liste_measure_display, String gml_file, String sld_file, String legende_file,
			String maps_title_file, String export) {
		try {
			ecrireDonneesGML(request, liste_measure_display, liste, gml_file, maps_title_file, export);
			System.out.println("ecrireDonneesGML done");

			ecrireDonneesSLD(liste, liste_measure_display, Measure_Display.color_Background, sld_file, legende_file);
			System.out.println("ecrireDonneesSLD done");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

/////////////////////////////////////////////////////////////////////////	

	static void ecrireDonneesGML(HttpServletRequest request, List<Measure_Display> liste_mesure_display,
			List<Indicateur> liste, String gml_file, String maps_title_file, String export) {

		// List<String> contenuMaps_Title = new ArrayList<String>();

		Integer cpt = 0;
		shifting = 0.0;

		// cette variable est utilisée pour indiquer si il y az une mesure n'utilise pas
		// un type d'affichage multicarte
		boolean map0existe = false;

		List<Element> liste_root_map = new ArrayList<Element>();

		Namespace GML = Namespace.getNamespace("gml", "http://www.opengis.net/gml");
		Namespace TOPP = Namespace.getNamespace("topp", "http://www.openplans.org/topp");
		Namespace WFS = Namespace.getNamespace("wfs", "http://www.opengis.net/wfs");

		// créer une root basic, on va utiliser une copie de cette root pour chaque map
		Element root_base = new Element("FeatureCollection", WFS);

		root_base.addNamespaceDeclaration(WFS);
		root_base.addNamespaceDeclaration(GML);
		root_base.addNamespaceDeclaration(TOPP);

		Element boundedBy = new Element("boundedBy", GML);
		root_base.addContent(boundedBy);

		Element Box = new Element("Box", GML);
		boundedBy.addContent(Box);
		Attribute BB = new Attribute("srsName", "http://www.opengis.net/gml/srs/epsg.xml#4326");
		Box.setAttribute(BB);

		Element coordinatesIntro = new Element("coordinates", GML);
		Box.addContent(coordinatesIntro);

		coordinatesIntro.addNamespaceDeclaration(GML);
		Attribute coord1 = new Attribute("gml", "http://www.opengis.net/gml");
		coordinatesIntro.setAttribute(coord1);
		Attribute coord2 = new Attribute("decimal", ".");
		coordinatesIntro.setAttribute(coord2);
		Attribute coord3 = new Attribute("cs", ",");
		coordinatesIntro.setAttribute(coord3);
		Attribute coord4 = new Attribute("ts", " ");
		coordinatesIntro.setAttribute(coord4);
		/*
		 * String test = new String(); test =
		 * "145.971619,-43.031944 147.219696,-41.775558";
		 * coordinatesIntro.addContent(test);
		 */

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
		String dis_type = "";

		List<String> list_ind_done = new ArrayList<String>();
		List<String> list_measure_done = new ArrayList<String>();

		//////////////// Pour le background
		Map<Integer, List<String>> map_zone_background = new HashMap<Integer, List<String>>();
		// cette varaiable est utilisée pour vérifier si une zone background est déjà
		// ajouté à une carte ou non
		// si la zone déjà ajouté il ne faut pas l'ajouter une deuxième fois parce que
		// il peut se placer au-dessus d'un varaiable à afficher
		map_zone_background.put(0, new ArrayList<String>());
		//////////////// Fin pour le background

		//////////////// pour Carte Title
		// Map <Integer, List<String>> titles = new HashMap<Integer, List<String>>();
		// Map <String, List<String>> title = new HashMap<String, List<String>>();
		/*
		 * pour chaque carte, il y a une liste des mesures et pour chaque mesure il y a
		 * une liste de memebre de dimension qui compose le titre de la carte Exemple :
		 * Carte 0 : Titre {production [2007 - 2008 - 2009], Surface [2007]} carte 1 :
		 * Titre {Surface [2008]} carte 2 : Titre {Surface [2009]}
		 */
		Map<Integer, Map<String, List<String>>> titles = new HashMap<Integer, Map<String, List<String>>>();

		// on prépare le titre pour la première carte et en suite dans les mode
		// d'affichage MultiMap on ajoute des titres pour les nouvelles cartes
		titles.put(0, new HashMap<String, List<String>>());
		//////////////// Fin pour Carte Title

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//////////////// Fin pour la charge des géometries
		String zone_liste = null;
		int k = 0;

		// System.out.println("(1):"+new java.util.Date());

		for (Indicateur ind : liste) {

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
//					System.out.println(" find for mesure display => " + m_d.getMeasureName() + " affichage " + m_d.getDisplayType());
					if (m_d.getMeasureName().equals(measure)) {
						dis_type = m_d.getDisplayType();
						System.out.println(" ps : " + m_d.getDisplayType());
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
//			System.out.println(measure + ":"+count_mes_ind.get(measure)); 

			/*
			 * Recuperation des valeurs mix et max de chaque mesure Pour creer des
			 * intervales egaux (tailles des barres) et convenables dans le gml pour
			 * l'affichage Bars
			 */

			if ((dis_type.toUpperCase().equals(("Bars").toUpperCase()))
					|| (dis_type.toUpperCase().equals(("MultiCloropeth").toUpperCase()))) {
				if (min_mes_Values.containsKey(measure)) {
					if (ind_value < min_mes_Values.get(measure)) {
						min_mes_Values.put(measure, ind_value);
					}
					if (ind_value > max_mes_Values.get(measure)) {
						max_mes_Values.put(measure, ind_value);
					}
				} else {
					min_mes_Values.put(measure, ind_value);
					max_mes_Values.put(measure, ind_value);
				}
			}

			///////////////////////////////////
			/*
			 * Recuperation des sommes des valeurs par zone spatiale pour chaque mesure Pour
			 * creer des secteurs convenables dans le gml pour l'affichage Sectors
			 */

			if (dis_type.toUpperCase().equals(("Sectors").toUpperCase())) {
				String mes_sp = measure + ind_spatial;
				if (sum_mes_sp_Values.containsKey(mes_sp)) {
					double sum = sum_mes_sp_Values.get(mes_sp);
					sum_mes_sp_Values.put(mes_sp, sum + ind_value);
				} else {
					sum_mes_sp_Values.put(mes_sp, ind_value);
				}
				// System.out.println(mes_sp+":"+sum_mes_sp_Values.get(mes_sp));
			}

			///////////////////////////////////
		}
		System.out.println("geometry :" + geometry);

		Donnee_geo.get_donneesGeo_liste(table, list_Level, zone_liste, connexion, geometry);
		// System.out.println("(2):"+new java.util.Date());
		// System.out.println("geometry :"+geometry);

		// il faut vider la liste de mesures traitées
		list_measure_done.clear();
		list_ind_done.clear();

		// définir la première carte par une copie de root basic
		Element root = new Element("FeatureCollection");
		root.addContent(root_base.clone());
		// ajouter la carte à la liste
		liste_root_map.add(root);

		for (Indicateur ind : liste) {

			/*
			 * il faut d'abord trouver comment la measure concernée doit être affichée à
			 * partir de nom d'indicateur on trouve la measure ensuite, à partir de nom de
			 * mesure et en regardant dans l'extrait du fichier de display config, on trouve
			 * le mode d'affichage (zonale, point, bars etc..)
			 */

			String str = ind.getNom();
			if (!list_ind_done.contains(str)) {
				String measure = "";
				// measure = get_measure(str, CreateIndicators.separator, mesure_position);
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
//								shifting = shifting -bar_width;
								// -bar_width parcque on va faire un shefting pour le premier indiquateur avant
								// de commencer

								if (shifting != 0) // pour ne pas faire un shefting pour la première mesure
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
//				System.out.println(" geom 1 centroid " + geom1.getCentroid() );
			} else {
				geom1 = Donnee_geo.get_donneesGeo(table, list_Level, nomSpatial, connexion);
//				System.out.println("donneesGeo :"+ ++k);
				/*
				 * geom1 = donneesPays(request, nomSpatial) ; if
				 * (geom1.getListePoint().isEmpty() && geom1.getListePolygon().isEmpty()) { //
				 * ajout anael //System.out.println("On n'a pas trouvÃ© le dÃ©partement !");
				 * geom1 = donneesSubRegion(request, nomSpatial) ; if
				 * (geom1.getListePoint().isEmpty() && geom1.getListePolygon().isEmpty()) { //
				 * ajout anael //System.out.println("On n'a pas trouvÃ© le pays !"); } }
				 */
				// On ajoute la geometrie recupere a la liste des geometries pour ne pas la
				// rechercher plus tard
//				System.out.println(" geom 1 centroid " + geom1.getNom() );
				geometry.put(nomSpatial, geom1);
			}

			///////////////////////////////////////////////////////////////////////////
			// pour les Bars
			if (dis_type.toUpperCase().equals(("Bars").toUpperCase())) {

				if (geom1.getCentroid() != null) {
					String measure = "";
					// measure = get_measure(str, CreateIndicators.separator, mesure_position);
					measure = ind.getMeasure();
//					System.out.println(" nom spatial : " + nomSpatial + ", measure : " + measure);
//					System.out.println(geom1.toString());

					// un type d'affichage qui n'est pas MultiMap est utilisé
					map0existe = true;

					//////////////// pour carte title
					// on ajoute le nom de la mesure au titre de la première carte[0] (on n'a pas
					//////////////// encore plusieurs cartes)
					if (titles.get(0).get(measure) == null) {

						titles.get(0).put(measure, new ArrayList<String>());
					}
					//////////////// Fin pour carte title

					String ind_name = ind.getNom();
					if (!list_ind_done.contains(ind_name)) {
						shifting = shifting + bar_width;
						list_ind_done.add(ind_name);
					}

					//////////////// Pour le background
					if (!map_zone_background.get(0).contains(nomSpatial)) {
						map_zone_background.get(0).add(nomSpatial);
					}
					//////////////// Fin pour le background

					writeGMLBackGround(geom1, root, GML, TOPP, cpt);
					writeGMLBar(geom1, ind, root, GML, TOPP, cpt, shifting, bar_width, bar_min_high, bar_max_high);
//					String barGeoJon = getBarCoordinate(geom1, ind, shifting, bar_width, bar_min_high, bar_max_high, max_mes_Values);
//					System.out.println(" by getBarCoordinate = " + barGeoJon);
				}
			} else
			///////////////////////////////////////////////////////////////////////////
			// pour les Sectors
			if (dis_type.toUpperCase().equals(("Sectors").toUpperCase())) {
				if (geom1.getCentroid() != null) {
					String ind_name = ind.getNom();
					if (!list_ind_done.contains(ind_name)) {
						list_ind_done.add(ind_name);
					}

					String measure = "";
					measure = ind.getMeasure();// get_measure(str, CreateIndicators.separator, mesure_position);

					// un type d'affichage qui n'est pas MultiMap est utilisé
					map0existe = true;

					//////////////// pour carte title
					// on ajoute le nom de la mesure au titre de la première carte[0] (on n'a pas
					//////////////// encore plusieurs cartes)
					if (titles.get(0).get(measure) == null) {

						titles.get(0).put(measure, new ArrayList<String>());
					}
					//////////////// Fin pour carte title

					String mes_sp = measure + ind.getSpatial();
					double sum_sp_values = sum_mes_sp_Values.get(mes_sp);

					double R = 5;
					double ang1 = 0;
					double ang2 = 0;

					// truover l'angle de indiquateurs déjà affichés
					if (sum_mes_sp_Values_curr.containsKey(mes_sp))
						ang1 = sum_mes_sp_Values_curr.get(mes_sp);

					else
						ang1 = 0;

					// angle de l'indiquateur actuel à afficher
					ang2 = (ind.getValeur() / sum_sp_values) * 360;

					// ajouter pour l'affichage de l'indiquateur suivant
					sum_mes_sp_Values_curr.put(mes_sp, ang2 + ang1);

					//////////////// Pour le background
					if (!map_zone_background.get(0).contains(nomSpatial)) {
						map_zone_background.get(0).add(nomSpatial);
					}
					//////////////// Fin pour le background

					writeGMLBackGround(geom1, root, GML, TOPP, cpt);
					writeGMLSecteur(geom1, ind, root, GML, TOPP, cpt, ang1, ang2, R);

				}
			}
			///////////////////////////////////////////////////////////////////////////
			// pour les Circles
			else if (dis_type.toUpperCase().equals(("Circles").toUpperCase())) {
				if (geom1.getCentroid() != null) {

					// un type d'affichage qui n'est pas MultiMap est utilisé
					map0existe = true;

					//////////////// pour carte title
					// on ajoute le nom de la mesure au titre de la première carte[0] (on n'a pas
					//////////////// encore plusieurs cartes)
					String measure = ind.getMeasure();
					if (titles.get(0).get(measure) == null) {

						titles.get(0).put(measure, new ArrayList<String>());
					}

					// on ajoute les membres des dimension au titre de la measure de la première
					// carte[0] (on n'a pas encore plusieurs cartes)
					List<String> dimensions_members = ind.getAttributes();
					int i = 0;
					for (String member : dimensions_members) {
						if (i != 0) { // la première dimension est la measure. il est déjà ajoutée, donc on l'ajoute
										// pas une deuxième fois
							if (!titles.get(0).get(measure).contains(member)) {
								titles.get(0).get(measure).add(member);
							}
						}
						i++;
					}
					//////////////// Fin pour carte title

					//////////////// Pour le background
					if (!map_zone_background.get(0).contains(nomSpatial)) {
						map_zone_background.get(0).add(nomSpatial);
					}
					//////////////// Fin pour le background

					writeGMLBackGround(geom1, root, GML, TOPP, cpt);
					writeGMLCircle(geom1, ind, root, GML, TOPP, cpt);

				}
			}

			///////////////////////////////////////////////////////////////////////////
			// pour les Cloropeth
			else if (dis_type.toUpperCase().equals(("Cloropeth").toUpperCase())) {

				// un type d'affichage qui n'est pas MultiMap est utilisé
				map0existe = true;

				//////////////// pour carte title
				// on ajoute le nom de la mesure au titre de la première carte[0] (on n'a pas
				//////////////// encore plusieurs cartes)
				String measure = ind.getMeasure();
				if (titles.get(0).get(measure) == null) {

					titles.get(0).put(measure, new ArrayList<String>());
				}

				// on ajoute les membres des dimension au titre de la measure de la première
				// carte[0] (on n'a pas encore plusieurs cartes)
				List<String> dimensions_members = ind.getAttributes();
				int j = 0;
				for (String member : dimensions_members) {
					if (j != 0) { // la première dimension est la measure. il est déjà ajoutée, donc on l'ajoute
									// pas une deuxième fois
						if (!titles.get(0).get(measure).contains(member)) {
							titles.get(0).get(measure).add(member);
						}
					}
					j++;
				}
				//////////////// Fin pour carte title

				//////////////// Pour le background
				if (!map_zone_background.get(0).contains(nomSpatial)) {
					map_zone_background.get(0).add(nomSpatial);
				}
				//////////////// Fin pour le background

				writeGMLBackGround(geom1, root, GML, TOPP, cpt);
				writeGMLCloropeth(geom1, ind, root, GML, TOPP, cpt);

			}
			///////////////////////////////////////////////////////////////////////////
			// pour les MultiCloropeth
			else if (dis_type.toUpperCase().equals(("MultiCloropeth").toUpperCase())) {

				// Element featureMember = new Element("featureMember",GML);

				String ind_name = ind.getNom();

				if (!list_ind_done.contains(ind_name)) {
					list_ind_done.add(ind_name);

					if (count_map_by_type.containsKey("MultiCloropeth")) {
						map_number = count_map_by_type.get("MultiCloropeth") + 1;
						count_map_by_type.put("MultiCloropeth", count_map_by_type.get("MultiCloropeth") + 1);

						//////////////// pour carte title
						// on prépare le titre pour la nouvelle carte (on ajoute des titres pour les
						//////////////// nouvelles cartes)
						titles.put(map_number, new HashMap<String, List<String>>());
						//////////////// Fin pour carte title

						//////////////// Pour le background
						map_zone_background.put(map_number, new ArrayList<String>());
						//////////////// Fin pour le background

					} else {
						map_number = 1;
						count_map_by_type.put("MultiCloropeth", 1);
						// pour ne pas mélanger le multimap avec les single map
						Element root2 = new Element("FeatureCollection");
						root2.addContent(root_base.clone());
						liste_root_map.add(root2);

						//////////////// pour carte title
						// on prépare le titre pour la nouvelle carte (on ajoute des titres pour les
						//////////////// nouvelles cartes)
						titles.put(map_number, new HashMap<String, List<String>>());
						//////////////// Fin pour carte title

						//////////////// Pour le background
						map_zone_background.put(1, new ArrayList<String>());
						//////////////// Fin pour le background

					}

					if (liste_root_map.size() < count_map_by_type.get("MultiCloropeth") + 1) {
						Element root2 = new Element("FeatureCollection");
						root2.addContent(root_base.clone());
						// root2.addContent(featureMember);

						//////////////// Pour le background
						// map_zone_background.put(map_number, new ArrayList<String>());

						if (!map_zone_background.get(map_number).contains(nomSpatial)) {
							map_zone_background.get(map_number).add(nomSpatial);
						}
						//////////////// Fin pour le background

						writeGMLBackGround(geom1, root2, GML, TOPP, cpt);
						writeGMLCloropeth(geom1, ind, root2, GML, TOPP, cpt);
						// ajouter la carte à la liste
						liste_root_map.add(root2);
					} else {
						//////////////// Pour le background
						if (!map_zone_background.get(map_number).contains(nomSpatial)) {
							map_zone_background.get(map_number).add(nomSpatial);
						}
						//////////////// Fin pour le background

						// liste_root_map.get(map_number).addContent(featureMember);
						writeGMLBackGround(geom1, liste_root_map.get(map_number), GML, TOPP, cpt);
						writeGMLCloropeth(geom1, ind, liste_root_map.get(map_number), GML, TOPP, cpt);

					}
				} else {
					//////////////// Pour le background
					if (!map_zone_background.get(map_number).contains(nomSpatial)) {
						map_zone_background.get(map_number).add(nomSpatial);
					}
					//////////////// Fin pour le background

					// liste_root_map.get(map_number).addContent(featureMember);
					writeGMLBackGround(geom1, liste_root_map.get(map_number), GML, TOPP, cpt);
					writeGMLCloropeth(geom1, ind, liste_root_map.get(map_number), GML, TOPP, cpt);
				}

				//////////////// pour carte title
				// on ajoute le nom de la mesure au titre de la carte actuelle [map_number]
				String measure = ind.getMeasure();
				if (titles.get(map_number).get(measure) == null) {

					titles.get(map_number).put(measure, new ArrayList<String>());
				}

				// on ajoute les membres des dimension au titre de la measure de la carte
				// actuelle [map_number]
				List<String> dimensions_members = ind.getAttributes();
				int j = 0;
				for (String member : dimensions_members) {
					if (j != 0) { // la première dimension est la measure. il est déjà ajoutée, donc on l'ajoute
									// pas une deuxième fois
						if (!titles.get(map_number).get(measure).contains(member)) {
							titles.get(map_number).get(measure).add(member);
						}
					}
					j++;
				}
				//////////////// Fin pour carte title

			}

			//////////////////////////////////////////////////////////////////////////////
			else {
				// un type d'affichage qui n'est pas MultiMap est utilisé
				map0existe = true;

				//////////////// pour carte title
				// on ajoute le nom de la mesure au titre de la première carte[0] (on n'a pas
				//////////////// encore plusieurs cartes)
				String measure = ind.getMeasure();
				if (titles.get(0).get(measure) == null) {

					titles.get(0).put(measure, new ArrayList<String>());
				}

				// on ajoute les membres des dimension au titre de la measure de la première
				// carte[0] (on n'a pas encore plusieurs cartes)
				List<String> dimensions_members = ind.getAttributes();
				int j = 0;
				for (String member : dimensions_members) {
					if (j != 0) { // la première dimension est la measure. il est déjà ajoutée, donc on l'ajoute
									// pas une deuxième fois
						if (!titles.get(0).get(measure).contains(member)) {
							titles.get(0).get(measure).add(member);
						}
					}
					j++;
				}
				//////////////// Fin pour carte title

				//////////////// Pour le background
				if (!map_zone_background.get(0).contains(nomSpatial)) {
					map_zone_background.get(0).add(nomSpatial);
				}
				//////////////// Fin pour le background

				writeGMLBackGround(geom1, root, GML, TOPP, cpt);
				writeGMLDefault(geom1, ind, root, GML, TOPP, cpt);

			}
			cpt++;

		}

		// System.out.println("(3):"+new java.util.Date());
		// System.out.println("geometry :3:"+geometry);

		// fermer la connexion avec la base de données
		try {
			connexion.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Titles : " + titles);
		enregistre_Titles(maps_title_file, titles);

		int i = 0;
		map_number = liste_root_map.size();

		// si il y a aucun mesure avec un type qui n'est pas multiMap donc il faut
		// supprimer la première carte qui est à la base réservée pour le type non
		// MultiMap
		if (!map0existe)
			map_number = map_number - 1;

		for (Element map : liste_root_map) {
			org.jdom2.Document Doc = new Document(map);

			// on enregistre la première carte seulement si il y a une mesure non MultiMap
			if ((i == 0) && map0existe)
				enregistre(gml_file + i + ".xml", Doc);
			if ((i != 0) && map0existe)
				enregistre(gml_file + i + ".xml", Doc);
			if ((i != 0) && !map0existe)
				enregistre(gml_file + (i - 1) + ".xml", Doc);

			i++;
		}
		System.out.println(map_zone_background);
	}

	static void ecrireDonneesGMLGeoJSON(HttpServletRequest request, List<Measure_Display> liste_mesure_display,
			List<Indicateur> liste, String gml_file, String maps_title_file, String export) {

		// List<String> contenuMaps_Title = new ArrayList<String>();

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
		String dis_type = "";

		List<String> list_ind_done = new ArrayList<String>();
		List<String> list_measure_done = new ArrayList<String>();

		//////////////// Pour le background
		Map<Integer, List<String>> map_zone_background = new HashMap<Integer, List<String>>();
		// cette varaiable est utilisée pour vérifier si une zone background est déjà
		// ajouté à une carte ou non
		// si la zone déjà ajouté il ne faut pas l'ajouter une deuxième fois parce que
		// il peut se placer au-dessus d'un varaiable à afficher
		map_zone_background.put(0, new ArrayList<String>());
		//////////////// Fin pour le background

		//////////////// pour Carte Title
		// Map <Integer, List<String>> titles = new HashMap<Integer, List<String>>();
		// Map <String, List<String>> title = new HashMap<String, List<String>>();
		/*
		 * pour chaque carte, il y a une liste des mesures et pour chaque mesure il y a
		 * une liste de memebre de dimension qui compose le titre de la carte Exemple :
		 * Carte 0 : Titre {production [2007 - 2008 - 2009], Surface [2007]} carte 1 :
		 * Titre {Surface [2008]} carte 2 : Titre {Surface [2009]}
		 */
		Map<Integer, Map<String, List<String>>> titles = new HashMap<Integer, Map<String, List<String>>>();

		// on prépare le titre pour la première carte et en suite dans les mode
		// d'affichage MultiMap on ajoute des titres pour les nouvelles cartes
		titles.put(0, new HashMap<String, List<String>>());
		//////////////// Fin pour Carte Title

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//////////////// Fin pour la charge des géometries
		String zone_liste = null;
		int k = 0;

		// System.out.println("(1):"+new java.util.Date());

		for (Indicateur ind : liste) {

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
//					System.out.println(" find for mesure display => " + m_d.getMeasureName() + " affichage " + m_d.getDisplayType());
					if (m_d.getMeasureName().equals(measure)) {
						dis_type = m_d.getDisplayType();
//						System.out.println(" ps : " + m_d.getDisplayType());
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
//			System.out.println(measure + ":"+count_mes_ind.get(measure)); 

			/*
			 * Recuperation des valeurs mix et max de chaque mesure Pour creer des
			 * intervales egaux (tailles des barres) et convenables dans le gml pour
			 * l'affichage Bars
			 */

			if ((dis_type.toUpperCase().equals(("Bars").toUpperCase()))
					|| (dis_type.toUpperCase().equals(("MultiCloropeth").toUpperCase()))) {
				if (min_mes_Values.containsKey(measure)) {
					if (ind_value < min_mes_Values.get(measure)) {
						min_mes_Values.put(measure, ind_value);
					}
					if (ind_value > max_mes_Values.get(measure)) {
						max_mes_Values.put(measure, ind_value);
					}
				} else {
					min_mes_Values.put(measure, ind_value);
					max_mes_Values.put(measure, ind_value);
				}
			}

			///////////////////////////////////
			/*
			 * Recuperation des sommes des valeurs par zone spatiale pour chaque mesure Pour
			 * creer des secteurs convenables dans le gml pour l'affichage Sectors
			 */

			if (dis_type.toUpperCase().equals(("Sectors").toUpperCase())) {
				String mes_sp = measure + ind_spatial;
				if (sum_mes_sp_Values.containsKey(mes_sp)) {
					double sum = sum_mes_sp_Values.get(mes_sp);
					sum_mes_sp_Values.put(mes_sp, sum + ind_value);
				} else {
					sum_mes_sp_Values.put(mes_sp, ind_value);
				}
				// System.out.println(mes_sp+":"+sum_mes_sp_Values.get(mes_sp));
			}

			///////////////////////////////////
		}
		System.out.println("geometry :" + geometry);

		Donnee_geo.get_donneesGeo_liste(table, list_Level, zone_liste, connexion, geometry);
		// System.out.println("(2):"+new java.util.Date());
		// System.out.println("geometry :"+geometry);

		// il faut vider la liste de mesures traitées
		list_measure_done.clear();
		list_ind_done.clear();

		// ajouter la carte à la liste

		for (Indicateur ind : liste) {

			/*
			 * il faut d'abord trouver comment la measure concernée doit être affichée à
			 * partir de nom d'indicateur on trouve la measure ensuite, à partir de nom de
			 * mesure et en regardant dans l'extrait du fichier de display config, on trouve
			 * le mode d'affichage (zonale, point, bars etc..)
			 */

			String str = ind.getNom();
			if (!list_ind_done.contains(str)) {
				String measure = "";
				// measure = get_measure(str, CreateIndicators.separator, mesure_position);
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
//								shifting = shifting -bar_width;
								// -bar_width parcque on va faire un shefting pour le premier indiquateur avant
								// de commencer

								if (shifting != 0) // pour ne pas faire un shefting pour la première mesure
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
				System.out.println("donneesGeo :" + ++k);
				/*
				 * geom1 = donneesPays(request, nomSpatial) ; if
				 * (geom1.getListePoint().isEmpty() && geom1.getListePolygon().isEmpty()) { //
				 * ajout anael //System.out.println("On n'a pas trouvÃ© le dÃ©partement !");
				 * geom1 = donneesSubRegion(request, nomSpatial) ; if
				 * (geom1.getListePoint().isEmpty() && geom1.getListePolygon().isEmpty()) { //
				 * ajout anael //System.out.println("On n'a pas trouvÃ© le pays !"); } }
				 */
				// On ajoute la geometrie recupere a la liste des geometries pour ne pas la
				// rechercher plus tard
				geometry.put(nomSpatial, geom1);
			}

			///////////////////////////////////////////////////////////////////////////
			// pour les Bars
			if (dis_type.toUpperCase().equals(("Bars").toUpperCase())) {

				if (geom1.getCentroid() != null) {

//					System.out.println(" test geom 1 centroid " + geom1.getCentroid() );
					String measure = "";
					// measure = get_measure(str, CreateIndicators.separator, mesure_position);
					measure = ind.getMeasure();

//					System.out.println(geom1.toString());

					// un type d'affichage qui n'est pas MultiMap est utilisé
					map0existe = true;

					//////////////// pour carte title
					// on ajoute le nom de la mesure au titre de la première carte[0] (on n'a pas
					//////////////// encore plusieurs cartes)
					if (titles.get(0).get(measure) == null) {

						titles.get(0).put(measure, new ArrayList<String>());
					}
					//////////////// Fin pour carte title

					String ind_name = ind.getNom();
					if (!list_ind_done.contains(ind_name)) {
						shifting = shifting + bar_width;
						list_ind_done.add(ind_name);
					}

					//////////////// Pour le background
					if (!map_zone_background.get(0).contains(nomSpatial)) {
						map_zone_background.get(0).add(nomSpatial);
					}
					//////////////// Fin pour le background

					Double sh = null;
					String barGeoJon = getBarCoordinate(geom1, ind, shifting, sh, bar_width, bar_min_high, bar_max_high,
							max_mes_Values);
					System.out.println(" by getBarCoordinate = " + barGeoJon);
				}
			} else
			///////////////////////////////////////////////////////////////////////////
			// pour les Sectors
			if (dis_type.toUpperCase().equals(("Sectors").toUpperCase())) {
				if (geom1.getCentroid() != null) {
					String ind_name = ind.getNom();
					if (!list_ind_done.contains(ind_name)) {
						list_ind_done.add(ind_name);
					}

					String measure = "";
					measure = ind.getMeasure();// get_measure(str, CreateIndicators.separator, mesure_position);

					// un type d'affichage qui n'est pas MultiMap est utilisé
					map0existe = true;

					//////////////// pour carte title
					// on ajoute le nom de la mesure au titre de la première carte[0] (on n'a pas
					//////////////// encore plusieurs cartes)
					if (titles.get(0).get(measure) == null) {

						titles.get(0).put(measure, new ArrayList<String>());
					}
					//////////////// Fin pour carte title

					String mes_sp = measure + ind.getSpatial();
					double sum_sp_values = sum_mes_sp_Values.get(mes_sp);

					double R = 5;
					double ang1 = 0;
					double ang2 = 0;

					// truover l'angle de indiquateurs déjà affichés
					if (sum_mes_sp_Values_curr.containsKey(mes_sp))
						ang1 = sum_mes_sp_Values_curr.get(mes_sp);

					else
						ang1 = 0;

					// angle de l'indiquateur actuel à afficher
					ang2 = (ind.getValeur() / sum_sp_values) * 360;

					// ajouter pour l'affichage de l'indiquateur suivant
					sum_mes_sp_Values_curr.put(mes_sp, ang2 + ang1);

					//////////////// Pour le background
					if (!map_zone_background.get(0).contains(nomSpatial)) {
						map_zone_background.get(0).add(nomSpatial);
					}
					//////////////// Fin pour le background

				}
			}
			///////////////////////////////////////////////////////////////////////////
			// pour les Circles
			else if (dis_type.toUpperCase().equals(("Circles").toUpperCase())) {
				if (geom1.getCentroid() != null) {

					// un type d'affichage qui n'est pas MultiMap est utilisé
					map0existe = true;

					//////////////// pour carte title
					// on ajoute le nom de la mesure au titre de la première carte[0] (on n'a pas
					//////////////// encore plusieurs cartes)
					String measure = ind.getMeasure();
					if (titles.get(0).get(measure) == null) {

						titles.get(0).put(measure, new ArrayList<String>());
					}

					// on ajoute les membres des dimension au titre de la measure de la première
					// carte[0] (on n'a pas encore plusieurs cartes)
					List<String> dimensions_members = ind.getAttributes();
					int i = 0;
					for (String member : dimensions_members) {
						if (i != 0) { // la première dimension est la measure. il est déjà ajoutée, donc on l'ajoute
										// pas une deuxième fois
							if (!titles.get(0).get(measure).contains(member)) {
								titles.get(0).get(measure).add(member);
							}
						}
						i++;
					}
					//////////////// Fin pour carte title

					//////////////// Pour le background
					if (!map_zone_background.get(0).contains(nomSpatial)) {
						map_zone_background.get(0).add(nomSpatial);
					}
					//////////////// Fin pour le background

				}
			}

			///////////////////////////////////////////////////////////////////////////
			// pour les Cloropeth
			else if (dis_type.toUpperCase().equals(("Cloropeth").toUpperCase())) {

				// un type d'affichage qui n'est pas MultiMap est utilisé
				map0existe = true;

				//////////////// pour carte title
				// on ajoute le nom de la mesure au titre de la première carte[0] (on n'a pas
				//////////////// encore plusieurs cartes)
				String measure = ind.getMeasure();
				if (titles.get(0).get(measure) == null) {

					titles.get(0).put(measure, new ArrayList<String>());
				}

				// on ajoute les membres des dimension au titre de la measure de la première
				// carte[0] (on n'a pas encore plusieurs cartes)
				List<String> dimensions_members = ind.getAttributes();
				int j = 0;
				for (String member : dimensions_members) {
					if (j != 0) { // la première dimension est la measure. il est déjà ajoutée, donc on l'ajoute
									// pas une deuxième fois
						if (!titles.get(0).get(measure).contains(member)) {
							titles.get(0).get(measure).add(member);
						}
					}
					j++;
				}
				//////////////// Fin pour carte title

				//////////////// Pour le background
				if (!map_zone_background.get(0).contains(nomSpatial)) {
					map_zone_background.get(0).add(nomSpatial);
				}
				//////////////// Fin pour le background

			}
			///////////////////////////////////////////////////////////////////////////
			// pour les MultiCloropeth
			else if (dis_type.toUpperCase().equals(("MultiCloropeth").toUpperCase())) {

				// Element featureMember = new Element("featureMember",GML);

				String ind_name = ind.getNom();

				if (!list_ind_done.contains(ind_name)) {
					list_ind_done.add(ind_name);

					if (count_map_by_type.containsKey("MultiCloropeth")) {
						map_number = count_map_by_type.get("MultiCloropeth") + 1;
						count_map_by_type.put("MultiCloropeth", count_map_by_type.get("MultiCloropeth") + 1);

						//////////////// pour carte title
						// on prépare le titre pour la nouvelle carte (on ajoute des titres pour les
						//////////////// nouvelles cartes)
						titles.put(map_number, new HashMap<String, List<String>>());
						//////////////// Fin pour carte title

						//////////////// Pour le background
						map_zone_background.put(map_number, new ArrayList<String>());
						//////////////// Fin pour le background

					} else {
						map_number = 1;
						count_map_by_type.put("MultiCloropeth", 1);
						// pour ne pas mélanger le multimap avec les single map

						//////////////// pour carte title
						// on prépare le titre pour la nouvelle carte (on ajoute des titres pour les
						//////////////// nouvelles cartes)
						titles.put(map_number, new HashMap<String, List<String>>());
						//////////////// Fin pour carte title

						//////////////// Pour le background
						map_zone_background.put(1, new ArrayList<String>());
						//////////////// Fin pour le background

					}

					if (liste_root_map.size() < count_map_by_type.get("MultiCloropeth") + 1) {

						//////////////// Pour le background
						// map_zone_background.put(map_number, new ArrayList<String>());

						if (!map_zone_background.get(map_number).contains(nomSpatial)) {
							map_zone_background.get(map_number).add(nomSpatial);
						}
						//////////////// Fin pour le background

					} else {
						//////////////// Pour le background
						if (!map_zone_background.get(map_number).contains(nomSpatial)) {
							map_zone_background.get(map_number).add(nomSpatial);
						}
						//////////////// Fin pour le background

					}
				} else {
					//////////////// Pour le background
					if (!map_zone_background.get(map_number).contains(nomSpatial)) {
						map_zone_background.get(map_number).add(nomSpatial);
					}
					//////////////// Fin pour le background

					// liste_root_map.get(map_number).addContent(featureMember);

				}

				//////////////// pour carte title
				// on ajoute le nom de la mesure au titre de la carte actuelle [map_number]
				String measure = ind.getMeasure();
				if (titles.get(map_number).get(measure) == null) {

					titles.get(map_number).put(measure, new ArrayList<String>());
				}

				// on ajoute les membres des dimension au titre de la measure de la carte
				// actuelle [map_number]
				List<String> dimensions_members = ind.getAttributes();
				int j = 0;
				for (String member : dimensions_members) {
					if (j != 0) { // la première dimension est la measure. il est déjà ajoutée, donc on l'ajoute
									// pas une deuxième fois
						if (!titles.get(map_number).get(measure).contains(member)) {
							titles.get(map_number).get(measure).add(member);
						}
					}
					j++;
				}
				//////////////// Fin pour carte title

			}

			//////////////////////////////////////////////////////////////////////////////
			else {
				// un type d'affichage qui n'est pas MultiMap est utilisé
				map0existe = true;

				//////////////// pour carte title
				// on ajoute le nom de la mesure au titre de la première carte[0] (on n'a pas
				//////////////// encore plusieurs cartes)
				String measure = ind.getMeasure();
				if (titles.get(0).get(measure) == null) {

					titles.get(0).put(measure, new ArrayList<String>());
				}

				// on ajoute les membres des dimension au titre de la measure de la première
				// carte[0] (on n'a pas encore plusieurs cartes)
				List<String> dimensions_members = ind.getAttributes();
				int j = 0;
				for (String member : dimensions_members) {
					if (j != 0) { // la première dimension est la measure. il est déjà ajoutée, donc on l'ajoute
									// pas une deuxième fois
						if (!titles.get(0).get(measure).contains(member)) {
							titles.get(0).get(measure).add(member);
						}
					}
					j++;
				}
				//////////////// Fin pour carte title

				//////////////// Pour le background
				if (!map_zone_background.get(0).contains(nomSpatial)) {
					map_zone_background.get(0).add(nomSpatial);
				}
				//////////////// Fin pour le background

			}
			cpt++;

		}

		// System.out.println("(3):"+new java.util.Date());
		// System.out.println("geometry :3:"+geometry);

		// fermer la connexion avec la base de données
		try {
			connexion.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Titles : " + titles);
		enregistre_Titles(maps_title_file, titles);

		int i = 0;
		map_number = liste_root_map.size();

		// si il y a aucun mesure avec un type qui n'est pas multiMap donc il faut
		// supprimer la première carte qui est à la base réservée pour le type non
		// MultiMap
		if (!map0existe)
			map_number = map_number - 1;

		for (Element map : liste_root_map) {
			org.jdom2.Document Doc = new Document(map);

			// on enregistre la première carte seulement si il y a une mesure non MultiMap
			if ((i == 0) && map0existe)
				enregistre(gml_file + i + ".xml", Doc);
			if ((i != 0) && map0existe)
				enregistre(gml_file + i + ".xml", Doc);
			if ((i != 0) && !map0existe)
				enregistre(gml_file + (i - 1) + ".xml", Doc);

			i++;
		}
		System.out.println(map_zone_background);
	}

	static void ecrireDonneesSLD(List<Indicateur> liste, List<Measure_Display> liste_mesure_display,
			String color_Background, String sld_file, String legende_file) {

		File file = new File(legende_file);
		FileOutputStream fos = null;

		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		String contenuLegende = new String();

		Namespace SCHEMALOCATION = Namespace.getNamespace("schemaLocation",
				"http://www.opengis.net/sld StyledLayerDescriptor.xsd");

		Namespace OGC = Namespace.getNamespace("ogc", "http://www.opengis.net/ogc");
		Namespace XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Namespace XSI = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		Namespace GML = Namespace.getNamespace("ogc", "http://www.opengis.net/gml");

		Element root = new Element("StyledLayerDescriptor");
		org.jdom2.Document Doc = new Document(root);
		Attribute version = new Attribute("version", "1.0.0");
		root.setAttribute(version);

		root.addNamespaceDeclaration(OGC);
		root.addNamespaceDeclaration(XLINK);
		root.addNamespaceDeclaration(XSI);

		Element NamedLayer = new Element("NamedLayer");
		root.addContent(NamedLayer);

		Element Name = new Element("Name");
		NamedLayer.addContent(Name);
		String nom = new String();
		nom = "Simple point";
		Name.addContent(nom);

		List<String> list_ind_done = new ArrayList<String>();
		List<String> list_measure_done = new ArrayList<String>();

		String dis_type = "";

		Element FeatureTypeStyle = new Element("FeatureTypeStyle");
		;
		long val;
		long min = 0;
		long max = 0;
		int count = 1;// pour indiquer combien d'indiquateur à afficher par measure
		int i = 0; // pour compter combien d'indiquateur on a affiché jusqu'à maintenant
		String couleur = new String();
		int R_couleur_0x = 0;
		int G_couleur_0x = 0;
		int B_couleur_0x = 0;
		int count_levels = 0;
		int size_min = 0;
		int size_max = 0;

		writeSLDBackGround(NamedLayer, OGC, color_Background);

		for (Indicateur ind : liste) {
			if (!list_ind_done.contains(ind.getNom()))// Si le nom de l'indicateur n'a pas déjà  été écrit
			{
				///////////////////////////////////////////
				String str = ind.getNom();

				String measure = "";

				// measure = get_measure(str, CreateIndicators.separator, mesure_position);
				measure = ind.getMeasure();

				if (!list_measure_done.contains(measure)) {
					// on initiale dis_type parce que il est possible que la mesure ne soit pas
					// décrite dans le fichier display_conf
					// pour qu'il ne garde pas l'affichage de la dernière mesure trouvée
					dis_type = "";

					for (Measure_Display m_d : liste_mesure_display) {
						if (m_d.getMeasureName().equals(measure)) {
							dis_type = m_d.getDisplayType();
							couleur = m_d.getColor();
							R_couleur_0x = Integer.parseInt(couleur.subSequence(1, 3).toString(), 16);
							G_couleur_0x = Integer.parseInt(couleur.subSequence(3, 5).toString(), 16);
							B_couleur_0x = Integer.parseInt(couleur.subSequence(5, 7).toString(), 16);
							if (dis_type.toUpperCase().equals(("Circles").toUpperCase())) {
								count_levels = m_d.getCount_levels();
								size_max = (int) m_d.getSize_max();
								size_min = (int) m_d.getSize_min();
							} else if ((dis_type.toUpperCase().equals(("Cloropeth").toUpperCase()))
									|| (dis_type.toUpperCase().equals(("MultiCloropeth").toUpperCase()))) {
								count_levels = m_d.getCount_levels();
							}
							break;
						}
					}
				}
				////////////////////////////////////////////////////////////////////
				// SLD BARs
				if (dis_type.toUpperCase().equals(("Bars").toUpperCase())) {

					list_ind_done.add(ind.getNom());
					boolean new_measure = false;
					if (!list_measure_done.contains(measure)) {
						list_measure_done.add(measure);

						contenuLegende += measure + "<br>";
						// pour ne pas ajuoter le nom de la mesure plusieurs fois avec chaque indicateur

						min = min_mes_Values.get(measure).intValue();
						max = max_mes_Values.get(measure).intValue();
						count = count_mes_ind.get(measure).intValue();
						i = 0;

						new_measure = true;

						// pour chaque mesure, on ajoute un seul "FeatureTypeStyle" qui contient une
						// règle pour chaque indicateur
						// si un nouveau indicateur arrive pour une mesure déjà traitée on ajoute sa
						// règle dans le même "FeatureTypeStyle"
						FeatureTypeStyle = new Element("FeatureTypeStyle");
					}
					i++;
					int R = (255 - i * (255 - R_couleur_0x) / count);
					int G = (255 - i * (255 - G_couleur_0x) / count);
					int B = (255 - i * (255 - B_couleur_0x) / count);

					contenuLegende += "<svg width=\"20\" height=\"20\"> <rect width=\"20\" height=\"20\" style=\"fill:rgb("
							+ R + "," + G + "," + B + ");stroke-width:3;stroke:rgb(0,0,0)\"> </svg> ";

					// pour ajouter les memebres des dimensions :
					// String ind_nam = ind.getNom();
					List<String> ind_atts = ind.getAttributes();
					// la première attribute est la mesure, il ne faut pas l'ajouter dans la légende
					// pour cela on commence à partir de deuxième attribute (x=1)
					int x = 0;
					for (String ind_att : ind_atts) {
						if (x != 0) {
							if (x != ind_atts.size() - 1) {
								contenuLegende += ind_att + " - ";
							} else {
								contenuLegende += ind_att;
							}
						}
						x++;
					}
					contenuLegende += "<br>";
					if (i == count)
						contenuLegende += "<br> ";

					//////////////////
					writeSLDBar(NamedLayer, FeatureTypeStyle, OGC, new_measure, measure, ind, R, G, B, min);

				}
				////////////////////////////////////////////////////////////////////
				// pour Circles SLD
				else if (dis_type.toUpperCase().equals(("Circles").toUpperCase())) {
					list_ind_done.add(ind.getNom());

					min = min_ind_Values.get(ind.getNom()).intValue();
					max = max_ind_Values.get(ind.getNom()).intValue();
					long tailleInter = (max - min) / count_levels;
					int sizeInter = (int) ((size_max - size_min) / count_levels);
					// System.out.println("sizeInter :"+ sizeInter);
					// int val;

					String titre_final = "";

					/////////////////
					// pour ajouter les memebres des dimensions :
					List<String> ind_atts = ind.getAttributes();
					// la première attribute est la mesure, il ne faut pas l'ajouter dans la légende
					// pour cela on commence à partir de deuxième attribute (x=1)
					int x = 0;
					for (String ind_att : ind_atts) {
						// if (x != 0)
						{
							if (x != ind_atts.size() - 1) {
								titre_final += ind_att + " - ";
							} else {
								titre_final += ind_att;
							}
						}
						x++;
					}

					// légende

					contenuLegende += titre_final + "<br>";

					for (int j = 1; j <= count_levels; j++) {
						contenuLegende += "<svg width=" + (j * sizeInter + size_min + 2) + " height="
								+ (j * sizeInter + size_min + 2) + "> <circle cx=" + (j * sizeInter + size_min) / 2
								+ " cy=" + (j * sizeInter + size_min) / 2 + " r=" + (j * sizeInter + size_min) / 2
								+ " style=\"fill:rgb(" + R_couleur_0x + "," + G_couleur_0x + "," + B_couleur_0x
								+ ");stroke-width:3;stroke:rgb(0,0,0)\"> </svg> ";
						long val1 = tailleInter * (j - 1) + min;
						long val2 = tailleInter * j + min;
						contenuLegende += val1 + " - " + val2;
						contenuLegende += "<br>";
					}

					contenuLegende += "<br> ";

					//////////////////
					writeSLDCircle(NamedLayer, OGC, titre_final, ind, couleur, min, max, count_levels, size_min,
							sizeInter);

				}
				///////////////////////////////////////////
				// SLD Cloropeth
				else if (dis_type.toUpperCase().equals(("Cloropeth").toUpperCase())) {

					list_ind_done.add(ind.getNom());

					min = min_ind_Values.get(ind.getNom()).intValue();
					max = max_ind_Values.get(ind.getNom()).intValue();
					long tailleInter = (max - min) / count_levels;

					String titre_final = "";
					/////////////////
					// pour ajouter les memebres des dimensions :
					// String ind_nam = ind.getNom();
					List<String> ind_atts = ind.getAttributes();
					// la première attribute est la mesure, il ne faut pas l'ajouter dans la légende
					// pour cela on commence à partir de deuxième attribute (x=1)
					int x = 0;
					for (String ind_att : ind_atts) {
						// if (x != 0)
						{
							if (x != ind_atts.size() - 1) {
								titre_final += ind_att + " - ";
							} else {
								titre_final += ind_att;
							}
						}
						x++;
					}

					// légende
					contenuLegende += titre_final + "<br>";

					i = 0;
					int R = 0, G = 0, B = 0;
					for (int j = 1; j <= count_levels; j++) {

						// légende
						R = (255 - i * (255 - R_couleur_0x) / count_levels);
						G = (255 - i * (255 - G_couleur_0x) / count_levels);
						B = (255 - i * (255 - B_couleur_0x) / count_levels);

						contenuLegende += "<svg width=\"20\" height=\"20\"> <rect width=\"20\" height=\"20\" style=\"fill:rgb("
								+ R + "," + G + "," + B + ");stroke-width:3;stroke:rgb(0,0,0)\"> </svg> ";
//						contenuLegende += titre_final +"<br>";

						i++;

						long val1 = tailleInter * (j - 1) + min;
						long val2 = tailleInter * j + min;
						contenuLegende += val1 + " - " + val2;
						contenuLegende += "<br>";
					}

					contenuLegende += "<br> ";

					//////////////////
					writeSLDCloropeth(NamedLayer, OGC, titre_final, ind, R_couleur_0x, G_couleur_0x, B_couleur_0x, min,
							max, count_levels);
				}

				///////////////////////////////////////////
				// multiCloropeth SLD
				else if (dis_type.toUpperCase().equals(("MultiCloropeth").toUpperCase())) {
					boolean new_measure = false;
					list_ind_done.add(ind.getNom());
					long tailleInter = 0;

					// pour ne pas ajuoter le nom de la mesure plusieurs fois avec chaque indicateur
					if (!list_measure_done.contains(measure)) {
						list_measure_done.add(measure);

						new_measure = true;
						min = min_mes_Values.get(measure).intValue();
						max = max_mes_Values.get(measure).intValue();
						// count = count_mes_ind.get(measure).intValue();
						// i = 0;
						tailleInter = (max - min) / count_levels;

						// légende
						contenuLegende += measure + "<br>";

						for (int j = 1; j <= count_levels; j++) {

							// légende
							int R = (255 - (j - 1) * (255 - R_couleur_0x) / count_levels);
							int G = (255 - (j - 1) * (255 - G_couleur_0x) / count_levels);
							int B = (255 - (j - 1) * (255 - B_couleur_0x) / count_levels);

							contenuLegende += "<svg width=\"20\" height=\"20\"> <rect width=\"20\" height=\"20\" style=\"fill:rgb("
									+ R + "," + G + "," + B + ");stroke-width:3;stroke:rgb(0,0,0)\"> </svg> ";

							long val1 = tailleInter * (j - 1) + min;
							long val2 = tailleInter * j + min;
							contenuLegende += val1 + " - " + val2;
							contenuLegende += "<br>";
						}
						contenuLegende += "<br> ";

						// pour chaque mesure, on ajoute un seul "FeatureTypeStyle" qui contient une
						// règle pour chaque indicateur
						// si un nouveau indicateur arrive pour une mesure déjà traitée on ajoute sa
						// règle dans le même "FeatureTypeStyle"
						FeatureTypeStyle = new Element("FeatureTypeStyle");

					}

					//////////////////////////
					writeSLDMultiCloropeth(NamedLayer, FeatureTypeStyle, OGC, new_measure, measure, ind, R_couleur_0x,
							G_couleur_0x, B_couleur_0x, min, max, count_levels);

				}
				///////////////////////////////////////////
				// "AreaChoreme" SLD
				else if (ind.getNom().contains("AreaChoreme")) {// ("AreaChoreme")) {
					list_ind_done.add(ind.getNom());

					String titre_final = "";

					// pour ajouter les memebres des dimensions :
					// String ind_nam = ind.getNom();
					List<String> ind_atts = ind.getAttributes();
					// la première attribute est la mesure, il ne faut pas l'ajouter dans la légende
					// pour cela on commence à partir de deuxième attribute (x=1)
					int x = 0;
					for (String ind_att : ind_atts) {
						// if (x != 0)
						{
							if (x != ind_atts.size() - 1) {
								titre_final += ind_att + " - ";
							} else {
								titre_final += ind_att;
							}
						}
						x++;
					}

					contenuLegende += titre_final + "<br>";

					contenuLegende += " - <img src=\"up.png\" alt=\"Expansion\" height=\"20\" /> Expansion - <img src=\"equal.png\" alt=\"Diminution\" height=\"20\" /> Stagnation - <img src=\"down.png\" alt=\"Diminution\" height=\"20\" /> reduction <br><br> ";

					//////////////////////////
					writeSLDAreaChoreme(NamedLayer, FeatureTypeStyle, OGC, titre_final, ind);
				}

				////////////////////////////////////////////////////////////////////
				// "ProdChoreme" SLD
				else if (ind.getNom().contains("ProdChoreme")) {// ("ProdChoreme")) {
					list_ind_done.add(ind.getNom());

					String titre_final = "";

					// pour ajouter les memebres des dimensions :
					// String ind_nam = ind.getNom();
					List<String> ind_atts = ind.getAttributes();
					// la première attribute est la mesure, il ne faut pas l'ajouter dans la légende
					// pour cela on commence à partir de deuxième attribute (x=1)
					int x = 0;
					for (String ind_att : ind_atts) {
						// if (x != 0)
						{
							if (x != ind_atts.size() - 1) {
								titre_final += ind_att + " - ";
							} else {
								titre_final += ind_att;
							}
						}
						x++;
					}

					contenuLegende += titre_final + "<br>";

					contenuLegende += " <img src=\"green.png\" alt=\"Green\" height=\"20\" /> increase <br> <img src=\"yellow.png\" alt=\"Yellow\" height=\"20\" /> stagnation <br> <img src=\"red.png\" alt=\"Red\" height=\"20\" /> decrease <br><br> ";

					//////////////////////////
					writeSLDProdChoreme(NamedLayer, FeatureTypeStyle, OGC, titre_final, ind);

				}
				////////////////////////////////////////
				// "Rond" SLD

				else if (ind.getNom().contains("Rond")) {// ("Rond")) {
					list_ind_done.add(ind.getNom());

					min = min_ind_Values.get(ind.getNom()).intValue();
					max = max_ind_Values.get(ind.getNom()).intValue();
					long tailleInter = (max - min) / 5;
					// int val;

					String titre_final = "";

					// pour ajouter les memebres des dimensions :
					// String ind_nam = ind.getNom();
					List<String> ind_atts = ind.getAttributes();
					// la première attribute est la mesure, il ne faut pas l'ajouter dans la légende
					// pour cela on commence à partir de deuxième attribute (x=1)
					int x = 0;
					for (String ind_att : ind_atts) {
						// if (x != 0)
						{
							if (x != ind_atts.size() - 1) {
								titre_final += ind_att + " - ";
							} else {
								titre_final += ind_att;
							}
						}
						x++;
					}

					contenuLegende += titre_final + "<br>";
					contenuLegende += " <img src=\"greencircle.png\" alt=\"Very small rond\" height=\"15\" /> " + min
							+ " - ";
					val = tailleInter + min;
					contenuLegende += val + "<br>";
					contenuLegende += " <img src=\"greencircle.png\" alt=\"Small rond\" height=\"20\" /> " + val
							+ " - ";
					val = tailleInter * 2 + min;
					contenuLegende += val + "<br>";
					contenuLegende += " <img src=\"greencircle.png\" alt=\"Normal rond\" height=\"25\" /> " + val
							+ " - ";
					val = tailleInter * 3 + min;
					contenuLegende += val + "<br>";
					contenuLegende += " <img src=\"greencircle.png\" alt=\"Large\" height=\"30\" /> " + val + " - ";
					val = tailleInter * 4 + min;
					contenuLegende += val + "<br>";
					contenuLegende += " <img src=\"greencircle.png\" alt=\"Very large\" height=\"35\" /> " + val + " - "
							+ max + "<br><br> ";

					//////////////////////////
					writeSLDRond(NamedLayer, FeatureTypeStyle, OGC, titre_final, ind, min, tailleInter);
				}

				//////////////////////////////////////
				// Default SLD

				else {
					list_ind_done.add(ind.getNom());

					min = min_ind_Values.get(ind.getNom()).intValue();
					max = max_ind_Values.get(ind.getNom()).intValue();
					long tailleInter = (max - min) / 5;
					// int val;

					String titre = new String();
					titre = ind.getNom();

					String titre_final = "";
					// pour ajouter les memebres des dimensions :
					// String ind_nam = ind.getNom();
					List<String> ind_atts = ind.getAttributes();
					// la première attribute est la mesure, il ne faut pas l'ajouter dans la légende
					// pour cela on commence à partir de deuxième attribute (x=1)
					int x = 0;
					for (String ind_att : ind_atts) {
						// if (x != 0)
						{
							if (x != ind_atts.size() - 1) {
								titre_final += ind_att + " - ";
							} else {
								titre_final += ind_att;
							}
						}
						x++;
					}

					contenuLegende += titre_final + "<br>";
					contenuLegende += " <img src=\"redsquare.png\" alt=\"Colors\" height=\"20\" /> " + min + " - ";
					val = tailleInter + min;
					contenuLegende += val + "<br>";
					contenuLegende += " <img src=\"orangesquare.png\" alt=\"orange\" height=\"20\" /> " + val + " - ";
					val = tailleInter * 2 + min;
					contenuLegende += val + "<br>";
					contenuLegende += " <img src=\"yellowsquare.png\" alt=\"orange\" height=\"20\" /> " + val + " - ";
					val = tailleInter * 3 + min;
					contenuLegende += val + "<br>";
					contenuLegende += " <img src=\"greensquare.png\" alt=\"orange\" height=\"20\" /> " + val + " - ";
					val = tailleInter * 4 + min;
					contenuLegende += val + "<br>";
					contenuLegende += " <img src=\"green2square.png\" alt=\"orange\" height=\"20\" /> " + val + " - "
							+ max + " <br><br> ";

					//////////////////////////
					writeSLDDefault(NamedLayer, FeatureTypeStyle, OGC, titre_final, ind, min, tailleInter);
				}
			}
		}

		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
			try {
				out.write(contenuLegende);
			} finally {
				out.close();
			}
			/*
			 * fos = new FileOutputStream(file, "UTF-8");
			 * 
			 * byte[] contenuLegendeByte = contenuLegende.getBytes();
			 * 
			 * fos.write(contenuLegendeByte); fos.flush(); fos.close();
			 */
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ajout anael
		// affiche(Doc);
		enregistre(sld_file, Doc);
	}

	//////////////////////////////////

	public static void writeGMLBar(Geometrie geom1, Indicateur ind, Element root, Namespace GML, Namespace TOPP,
			Integer cpt, Double shifting, Double bar_width, Double bar_min_high, Double bar_max_high) {

		String measure = ind.getMeasure();
		double ind_value = ind.getValeur();
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element featureMember2 = new Element("featureMember", GML);
		root.addContent(featureMember2);

		// On crÃ©e une balise newpoint et on l'ajoute Ã  la balise racine.
		Element newpoint2 = new Element("newpointsres1", TOPP);
		featureMember2.addContent(newpoint2);

		// On crÃ©e un attribut "name" contenant le nom de l'indicateur et on ajoute
		// l'attribut Ã  la balise Indicateur
		Attribute NP2 = new Attribute("fid", "newpointsres1." + (cpt + 1) + "Bars");
		newpoint2.setAttribute(NP2);

		Element id2 = new Element("ID", TOPP);
		String localID2 = new String();
		localID2 = String.valueOf(cpt);
		id2.setText(localID2);
		newpoint2.addContent(id2);

		Element value2 = new Element(ind_name, TOPP);
		String localValue2 = new String();
		localValue2 = String.valueOf(ind_value);

		value2.setText(localValue2);
		newpoint2.addContent(value2);

		Element nom2 = new Element("nom", TOPP);
		String localNom2 = new String();
		localNom2 = geom1.getNom();
		nom2.setText(localNom2);
		newpoint2.addContent(nom2);

		Element the_geom2 = new Element("the_geom", TOPP);
		newpoint2.addContent(the_geom2);

		Element multiPolygon = new Element("MultiPolygon", GML);
		the_geom2.addContent(multiPolygon);

		Element polygonMember = new Element("polygonMember", GML);
		multiPolygon.addContent(polygonMember);

		Element polygon = new Element("Polygon", GML);
		polygonMember.addContent(polygon);

		Element outerBoundaryIs = new Element("outerBoundaryIs", GML);
		polygon.addContent(outerBoundaryIs);

		Element LinearRing = new Element("LinearRing", GML);
		outerBoundaryIs.addContent(LinearRing);

		Element coordinates = new Element("coordinates", GML);
		coordinates.addNamespaceDeclaration(GML);
		Attribute C2 = new Attribute("decimal", ".");
		coordinates.setAttribute(C2);
		Attribute C3 = new Attribute("cs", ",");
		coordinates.setAttribute(C3);
		Attribute C4 = new Attribute("ts", " ");
		coordinates.setAttribute(C4);
		String localCoordinates = new String();
		// ajout anael il faut ajouter quatre points pour créer un bar en commencant par
		// le centroid + shifting
		Point point = geom1.getCentroid();

		double high_by_value = bar_min_high + (ind_value / max_mes_Values.get(measure)) * (bar_max_high - bar_min_high);
		localCoordinates = "";

		localCoordinates = localCoordinates + (point.getX_() + shifting) + "," + point.getY_() + " ";
		localCoordinates = localCoordinates + (point.getX_() + shifting + bar_width) + "," + (point.getY_()) + " ";
		localCoordinates = localCoordinates + (point.getX_() + shifting + bar_width) + ","
				+ (point.getY_() + high_by_value) + " ";
		localCoordinates = localCoordinates + (point.getX_() + shifting) + "," + (point.getY_() + high_by_value) + " ";

		coordinates.setText(localCoordinates);

		// A RECUPERER LOCALCOORDINATES
//			System.out.println(" by writeGMLBar => " + localCoordinates);
		LinearRing.addContent(coordinates);
	}

	
	public static String getBarCoordinate(Geometrie geom1, Indicateur ind, Double shifting, Double sh, Double bar_width,
			Double bar_min_high, Double bar_max_high, Map<String, Double> max_mes_Values) {
		// Yassine
		String measure = ind.getMeasure();
		double ind_value = ind.getValeur();
		// System.out.println(" coordinates jdaaaad nom " + ind.getValeur());
		String localCoordinates = new String();
		// ajout anael il faut ajouter quatre points pour créer un bar en commencant par
		// le centroid + shifting
		Point point = geom1.getCentroid();
//			System.out.println(" get measure :::" + measure);
//			System.out.println(" get bar coordinnate :: nb niveau : " + countNiv + " type niveau spatial : " + niveauSpatial);
         
		if (ind_value == 99999) {
			localCoordinates = "";

			localCoordinates = localCoordinates + (point.getX_() + shifting + sh) + "," + (point.getY_() + 0.02)
					+ " ";
			localCoordinates = localCoordinates + (point.getX_() + shifting + bar_width + sh ) + ","
					+ (point.getY_() + 0.02) + " ";
			localCoordinates = localCoordinates + (point.getX_() + shifting + bar_width + sh ) + ","
					+ (point.getY_() + 0.03) + " ";
			localCoordinates = localCoordinates + (point.getX_() + shifting + sh) + "," + (point.getY_() + 0.03)
					+ " ";
		} else if (ind_value == 0.0) {

			localCoordinates = "";

			double radian = 0;
			double x = 0;
			double y = 0;
			double radius = 0.03;
			for (int i = 0; i < 60; i++) {

				x = (double) (radius * Math.cos(radian));
				y = (double) (radius * Math.sin(radian));
				radian = radian + (1. / 32 * Math.PI);

				localCoordinates = localCoordinates + (point.getX_() + shifting + sh + x +0.05) + ","
						+ (point.getY_() + y + 0.03) + " ";

			}

		} else {
			double high_by_value = ((bar_min_high
					+ (ind_value / max_mes_Values.get(measure)) * (bar_max_high - bar_min_high)) / 1.8);
//			System.out.println("bar_min_high :" + bar_min_high + "bar_max_high :" + bar_max_high + "max_mes_Values.get(measure) :" + max_mes_Values.get(measure));
			localCoordinates = "";

			localCoordinates = localCoordinates + (point.getX_() + shifting + sh ) + "," + (point.getY_()) + " ";
			localCoordinates = localCoordinates + (point.getX_() + shifting + bar_width + sh ) + ","
					+ (point.getY_()) + " ";
			localCoordinates = localCoordinates + (point.getX_() + shifting + bar_width + sh ) + ","
					+ (point.getY_() + high_by_value) + " ";
			localCoordinates = localCoordinates + (point.getX_() + shifting + sh ) + ","
					+ (point.getY_() + high_by_value) + " ";

		}
	
		String[] tmp = localCoordinates.split(" ");

		int ct = 0;
		String coord = "";
		String coord1 = "";
		for (String i : tmp) {
			if (ct == 0) {
				coord = "[" + i + "], ";
				coord1 = "[" + i + "] ";

			} else if (ct == tmp.length - 1) {
//					coord = coord + "["+i+"]";
				coord = coord + "[" + i + "]," + coord1;

			} else {
				coord = coord + "[" + i + "], ";

			}

			ct++;
		}

		coord = "[[" + coord + "]]";

		String barGeoJson = "{\"type\":\"Polygon\",\"coordinates\":" + coord + "}";

//			System.out.println(" bar coord = "+barGeoJson);

		return barGeoJson;
	}

	public static String getMultiBarCoordinate(Geometrie geom1, Indicateur ind, Double shifting, Double bar_width,
			Double bar_min_high, Double bar_max_high, Map<String, Double> max_mes_Values, int countNiv,
			String niveauSpatial) {
		String measure = ind.getMeasure();
		double ind_value = ind.getValeur();
		// System.out.println("hahnaaaaaaaaaa :" );

		String localCoordinates = new String();
		// ajout anael il faut ajouter quatre points pour créer un bar en commencant par
		// le centroid + shifting
		Point point = geom1.getCentroid();

//			System.out.println(" get measure :::" + measure);
//			System.out.println(" get bar coordinnate :: nb niveau : " + countNiv + " type niveau spatial : " + niveauSpatial);
		double high_by_value = bar_min_high + (ind_value / max_mes_Values.get(measure)) * (bar_max_high - bar_min_high);
		localCoordinates = "";

		localCoordinates = localCoordinates + (point.getX_() + shifting) + "," + point.getY_() + " ";
		localCoordinates = localCoordinates + (point.getX_() + shifting + bar_width) + "," + (point.getY_()) + " ";
		localCoordinates = localCoordinates + (point.getX_() + shifting + bar_width) + ","
				+ (point.getY_() + high_by_value) + " ";
		localCoordinates = localCoordinates + (point.getX_() + shifting) + "," + (point.getY_() + high_by_value) + " ";

		String[] tmp = localCoordinates.split(" ");

		int ct = 0;
		String coord = "";
		String coord1 = "";
		for (String i : tmp) {
//				System.out.println(" element in tmp "+i);
			if (ct == 0) {
				coord = "[" + i + "], ";
				coord1 = "[" + i + "] ";
			} else if (ct == tmp.length - 1) {
				coord = coord + "[" + i + "]," + coord1;
//					coord = coord + "["+i+"]" ;
			} else {
				coord = coord + "[" + i + "], ";
			}

			ct++;
		}

		coord = "[[" + coord + "]]";

		String barGeoJson = "{\"type\":\"Polygon\",\"coordinates\":" + coord + "}";

//			System.out.println(" bar coord = "+barGeoJson);

		return barGeoJson;
	}

	public static void writeGMLSecteur(Geometrie geom1, Indicateur ind, Element root, Namespace GML, Namespace TOPP,
			Integer cpt, double ang1, double ang2, double R) {
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element featureMember2 = new Element("featureMember", GML);
		root.addContent(featureMember2);

		// On crÃ©e une balise newpoint et on l'ajoute Ã  la balise racine.
		Element newpoint2 = new Element("newpointsres1", TOPP);
		featureMember2.addContent(newpoint2);

		// On crÃ©e un attribut "name" contenant le nom de l'indicateur et on ajoute
		// l'attribut Ã  la balise Indicateur
		Attribute NP2 = new Attribute("fid", "newpointsres1." + (cpt + 1) + "Sectors");
		newpoint2.setAttribute(NP2);

		Element id2 = new Element("ID", TOPP);
		String localID2 = new String();
		localID2 = String.valueOf(cpt);
		id2.setText(localID2);
		newpoint2.addContent(id2);

		Element value2 = new Element(ind_name, TOPP);
		String localValue2 = String.valueOf(ang2);

		value2.setText(localValue2);
		newpoint2.addContent(value2);

		Element nom2 = new Element("nom", TOPP);
		String localNom2 = new String();
		localNom2 = geom1.getNom();
		nom2.setText(localNom2);
		newpoint2.addContent(nom2);

		Element the_geom2 = new Element("the_geom", TOPP);
		newpoint2.addContent(the_geom2);

		Element multiPolygon = new Element("MultiPolygon", GML);
		the_geom2.addContent(multiPolygon);

		Element polygonMember = new Element("polygonMember", GML);
		multiPolygon.addContent(polygonMember);

		Element polygon = new Element("Polygon", GML);
		polygonMember.addContent(polygon);

		Element outerBoundaryIs = new Element("outerBoundaryIs", GML);
		polygon.addContent(outerBoundaryIs);

		Element LinearRing = new Element("LinearRing", GML);
		outerBoundaryIs.addContent(LinearRing);

		Element coordinates = new Element("coordinates", GML);
		coordinates.addNamespaceDeclaration(GML);
		Attribute C2 = new Attribute("decimal", ".");
		coordinates.setAttribute(C2);
		Attribute C3 = new Attribute("cs", ",");
		coordinates.setAttribute(C3);
		Attribute C4 = new Attribute("ts", " ");
		coordinates.setAttribute(C4);
		String localCoordinates = new String();
		// ajout anael il faut ajouter quatre points pour créer un bar en commencant par
		// le centroid + shifting
		Point point = geom1.getCentroid();

		localCoordinates = "";

		double rad1 = Math.toRadians(ang1);
		double rad2 = Math.toRadians(ang2);
		localCoordinates = localCoordinates + (point.getX_()) + "," + point.getY_() + " ";
		localCoordinates = localCoordinates + (point.getX_() + R * Math.cos(rad1)) + ","
				+ (point.getY_() + R * Math.sin(rad1)) + " ";
		localCoordinates = localCoordinates + (point.getX_() + R * Math.cos(rad1 + rad2)) + ","
				+ (point.getY_() + R * Math.sin(rad1 + rad2)) + " ";

		coordinates.setText(localCoordinates);
		LinearRing.addContent(coordinates);

	}

	public static void writeGMLCircle(Geometrie geom1, Indicateur ind, Element root, Namespace GML, Namespace TOPP,
			Integer cpt) {
		double ind_value = ind.getValeur();
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element featureMember2 = new Element("featureMember", GML);
		root.addContent(featureMember2);

		// On crÃ©e une balise newpoint et on l'ajoute Ã  la balise racine.
		Element newpoint2 = new Element("newpointsres1", TOPP);
		featureMember2.addContent(newpoint2);

		// On crÃ©e un attribut "name" contenant le nom de l'indicateur et on ajoute
		// l'attribut Ã  la balise Indicateur
		Attribute NP2 = new Attribute("fid", "newpointsres1." + (cpt + 1) + "Circles");
		newpoint2.setAttribute(NP2);

		Element id2 = new Element("ID", TOPP);
		String localID2 = new String();
		localID2 = String.valueOf(cpt);
		id2.setText(localID2);
		newpoint2.addContent(id2);

		Element value2 = new Element(ind_name, TOPP);
		String localValue2 = new String();
		localValue2 = String.valueOf(ind_value);

		value2.setText(localValue2);
		newpoint2.addContent(value2);

		Element nom2 = new Element("nom", TOPP);
		String localNom2 = new String();
		localNom2 = geom1.getNom();
		nom2.setText(localNom2);
		newpoint2.addContent(nom2);

		Element the_geom2 = new Element("the_geom", TOPP);
		newpoint2.addContent(the_geom2);

		Element Point2 = new Element("Point", GML);
		the_geom2.addContent(Point2);
		Attribute srsName2 = new Attribute("srsName", "http://www.opengis.net/gml/srs/epsg.xml#3857");
		Point2.setAttribute(srsName2);

		Element coordinates2 = new Element("coordinates", GML);
		Attribute C12 = new Attribute("fid", "http://www.opengis.net/gml");
		coordinates2.setAttribute(C12);
		Attribute C22 = new Attribute("decimal", ".");
		coordinates2.setAttribute(C22);
		Attribute C32 = new Attribute("cs", ",");
		coordinates2.setAttribute(C32);
		Attribute C42 = new Attribute("ts", " ");
		coordinates2.setAttribute(C42);
		String localCoordinates2 = new String();
		Point point = geom1.getCentroid();
		localCoordinates2 = "";
		localCoordinates2 = localCoordinates2 + point.getX_() + "," + point.getY_() + " ";
		coordinates2.setText(localCoordinates2);
		Point2.addContent(coordinates2);
	}

	public static void writeGMLCloropeth(Geometrie geom1, Indicateur ind, Element root, Namespace GML, Namespace TOPP,
			Integer cpt) {
		double ind_value = ind.getValeur();
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element featureMember = new Element("featureMember", GML);

		root.addContent(featureMember);

		// On crÃ©e une balise newpoint et on l'ajoute Ã  la balise racine.
		Element newpoint = new Element("newpointsres1", TOPP);
		featureMember.addContent(newpoint);

		// On crÃ©e un attribut "name" contenant le nom de l'indicateur et on ajoute
		// l'attribut Ã  la balise Indicateur
		Attribute NP = new Attribute("fid", "newpointsres1." + (cpt + 1));
		newpoint.setAttribute(NP);

		Element id = new Element("ID", TOPP);
		String localID = new String();
		localID = String.valueOf(cpt);
		id.setText(localID);
		newpoint.addContent(id);

		Element value = new Element(ind_name, TOPP);
		String localValue = String.valueOf(ind_value);

		value.setText(localValue);
		newpoint.addContent(value);

		Element nom = new Element("nom", TOPP);
		String localNom = new String();
		localNom = geom1.getNom();
		nom.setText(localNom);
		newpoint.addContent(nom);

		Element the_geom = new Element("the_geom", TOPP);
		newpoint.addContent(the_geom);

		if ("POLYGON".equals(geom1.getType())) {
			// ajout anael
			Element multiPolygon = new Element("MultiPolygon", GML);
			the_geom.addContent(multiPolygon);

			for (int i = 0; i < geom1.getListePolygon().size(); i++) {
				Element polygonMember = new Element("polygonMember", GML);
				multiPolygon.addContent(polygonMember);

				Element polygon = new Element("Polygon", GML);
				polygonMember.addContent(polygon);

				Element outerBoundaryIs = new Element("outerBoundaryIs", GML);
				polygon.addContent(outerBoundaryIs);

				Element LinearRing = new Element("LinearRing", GML);
				outerBoundaryIs.addContent(LinearRing);

				Element coordinates = new Element("coordinates", GML);
				coordinates.addNamespaceDeclaration(GML);
				Attribute C2 = new Attribute("decimal", ".");
				coordinates.setAttribute(C2);
				Attribute C3 = new Attribute("cs", ",");
				coordinates.setAttribute(C3);
				Attribute C4 = new Attribute("ts", " ");
				coordinates.setAttribute(C4);
				String localCoordinates = new String();
				// ajout anael
				List<Point> listePoint = geom1.getListePolygon().get(i);
				localCoordinates = "";
				for (Point p : listePoint) {
					localCoordinates = localCoordinates + p.getX_() + "," + p.getY_() + " ";
				}
				coordinates.setText(localCoordinates);
				LinearRing.addContent(coordinates);
			}
		} else {
			// ajout anael
			Element multiPolygon = new Element("MultiPolygon", GML);
			the_geom.addContent(multiPolygon);

			for (int i = 0; i < geom1.getListePolygon().size(); i++) {
				Element polygonMember = new Element("polygonMember", GML);
				multiPolygon.addContent(polygonMember);

				Element polygon = new Element("Polygon", GML);
				polygonMember.addContent(polygon);

				Element outerBoundaryIs = new Element("outerBoundaryIs", GML);
				polygon.addContent(outerBoundaryIs);

				Element LinearRing = new Element("LinearRing", GML);
				outerBoundaryIs.addContent(LinearRing);

				Element coordinates = new Element("coordinates", GML);
				coordinates.addNamespaceDeclaration(GML);
				Attribute C2 = new Attribute("decimal", ".");
				coordinates.setAttribute(C2);
				Attribute C3 = new Attribute("cs", ",");
				coordinates.setAttribute(C3);
				Attribute C4 = new Attribute("ts", " ");
				coordinates.setAttribute(C4);
				String localCoordinates = new String();
				// ajout anael
				List<Point> listePoint = geom1.getListePolygon().get(i);
				localCoordinates = "";
				for (Point p : listePoint) {
					localCoordinates = localCoordinates + p.getX_() + "," + p.getY_() + " ";
				}
				coordinates.setText(localCoordinates);
				LinearRing.addContent(coordinates);
			}
		}

	}

	public static void writeGMLDefault(Geometrie geom1, Indicateur ind, Element root, Namespace GML, Namespace TOPP,
			Integer cpt) {
		double ind_value = ind.getValeur();
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element featureMember = new Element("featureMember", GML);
		root.addContent(featureMember);

		// ajout anael
		// System.out.println("On a geom1."+geom1.getNom()+" en type :
		// "+geom1.getType());

		// On crÃ©e une balise newpoint et on l'ajoute Ã  la balise racine.
		Element newpoint = new Element("newpointsres1", TOPP);
		featureMember.addContent(newpoint);

		// On crÃ©e un attribut "name" contenant le nom de l'indicateur et on ajoute
		// l'attribut Ã  la balise Indicateur
		Attribute NP = new Attribute("fid", "newpointsres1." + (cpt + 1));
		newpoint.setAttribute(NP);

		Element id = new Element("ID", TOPP);
		String localID = new String();
		localID = String.valueOf(cpt);
		id.setText(localID);
		newpoint.addContent(id);

		Element value = new Element(ind_name, TOPP);
		String localValue = new String();
		localValue = String.valueOf(ind_value);

		value.setText(localValue);
		newpoint.addContent(value);

		Element nom = new Element("nom", TOPP);
		String localNom = new String();
		localNom = geom1.getNom();
		nom.setText(localNom);
		newpoint.addContent(nom);

		Element the_geom = new Element("the_geom", TOPP);
		newpoint.addContent(the_geom);

		if ("POINT".equals(geom1.getType())) {
			Element Point = new Element("Point", GML);
			the_geom.addContent(Point);
			Attribute srsName = new Attribute("srsName", "http://www.opengis.net/gml/srs/epsg.xml#3857");
			Point.setAttribute(srsName);

			Element coordinates = new Element("coordinates", GML);
			Attribute C1 = new Attribute("fid", "http://www.opengis.net/gml");
			coordinates.setAttribute(C1);
			Attribute C2 = new Attribute("decimal", ".");
			coordinates.setAttribute(C2);
			Attribute C3 = new Attribute("cs", ",");
			coordinates.setAttribute(C3);
			Attribute C4 = new Attribute("ts", " ");
			coordinates.setAttribute(C4);
			String localCoordinates = new String();
			List<Point> listePoint = geom1.getListePoint();
			localCoordinates = "";
			for (Point p : listePoint) {
				localCoordinates = localCoordinates + p.getX_() + "," + p.getY_() + " ";
			}
			coordinates.setText(localCoordinates);
			Point.addContent(coordinates);
		} else if ("POLYGON".equals(geom1.getType())) {
			// ajout anael
			Element multiPolygon = new Element("MultiPolygon", GML);
			the_geom.addContent(multiPolygon);

			for (int i = 0; i < geom1.getListePolygon().size(); i++) {
				Element polygonMember = new Element("polygonMember", GML);
				multiPolygon.addContent(polygonMember);

				Element polygon = new Element("Polygon", GML);
				polygonMember.addContent(polygon);

				Element outerBoundaryIs = new Element("outerBoundaryIs", GML);
				polygon.addContent(outerBoundaryIs);

				Element LinearRing = new Element("LinearRing", GML);
				outerBoundaryIs.addContent(LinearRing);

				Element coordinates = new Element("coordinates", GML);
				coordinates.addNamespaceDeclaration(GML);
				Attribute C2 = new Attribute("decimal", ".");
				coordinates.setAttribute(C2);
				Attribute C3 = new Attribute("cs", ",");
				coordinates.setAttribute(C3);
				Attribute C4 = new Attribute("ts", " ");
				coordinates.setAttribute(C4);
				String localCoordinates = new String();
				// ajout anael
				List<Point> listePoint = geom1.getListePolygon().get(i);
				localCoordinates = "";
				for (Point p : listePoint) {
					localCoordinates = localCoordinates + p.getX_() + "," + p.getY_() + " ";
				}
				coordinates.setText(localCoordinates);
				LinearRing.addContent(coordinates);
			}

		} else {
			// ajout anael
			Element multiPolygon = new Element("MultiPolygon", GML);
			the_geom.addContent(multiPolygon);

			for (int i = 0; i < geom1.getListePolygon().size(); i++) {
				Element polygonMember = new Element("polygonMember", GML);
				multiPolygon.addContent(polygonMember);

				Element polygon = new Element("Polygon", GML);
				polygonMember.addContent(polygon);

				Element outerBoundaryIs = new Element("outerBoundaryIs", GML);
				polygon.addContent(outerBoundaryIs);

				Element LinearRing = new Element("LinearRing", GML);
				outerBoundaryIs.addContent(LinearRing);

				Element coordinates = new Element("coordinates", GML);
				coordinates.addNamespaceDeclaration(GML);
				Attribute C2 = new Attribute("decimal", ".");
				coordinates.setAttribute(C2);
				Attribute C3 = new Attribute("cs", ",");
				coordinates.setAttribute(C3);
				Attribute C4 = new Attribute("ts", " ");
				coordinates.setAttribute(C4);
				String localCoordinates = new String();
				// ajout anael
				List<Point> listePoint = geom1.getListePolygon().get(i);
				localCoordinates = "";
				for (Point p : listePoint) {
					localCoordinates = localCoordinates + p.getX_() + "," + p.getY_() + " ";
				}
				coordinates.setText(localCoordinates);
				LinearRing.addContent(coordinates);
			}
		}

		if (geom1.getCentroid() != null) {
			Element featureMember2 = new Element("featureMember", GML);
			root.addContent(featureMember2);

			// On crÃ©e une balise newpoint et on l'ajoute Ã  la balise racine.
			Element newpoint2 = new Element("newpointsres1", TOPP);
			featureMember2.addContent(newpoint2);

			// On crÃ©e un attribut "name" contenant le nom de l'indicateur et on ajoute
			// l'attribut Ã  la balise Indicateur
			Attribute NP2 = new Attribute("fid", "newpointsres1." + (cpt + 1) + "centroid");
			newpoint2.setAttribute(NP2);

			Element id2 = new Element("ID", TOPP);
			String localID2 = new String();
			localID2 = String.valueOf(cpt);
			id2.setText(localID2);
			newpoint2.addContent(id2);

			Element value2 = new Element(ind_name, TOPP);
			String localValue2 = new String();
			localValue2 = String.valueOf(ind_value);

			value2.setText(localValue2);
			newpoint2.addContent(value2);

			Element nom2 = new Element("nom", TOPP);
			String localNom2 = new String();
			localNom2 = geom1.getNom();
			nom2.setText(localNom2);
			newpoint2.addContent(nom2);

			Element the_geom2 = new Element("the_geom", TOPP);
			newpoint2.addContent(the_geom2);

			Element Point2 = new Element("Point", GML);
			the_geom2.addContent(Point2);
			Attribute srsName2 = new Attribute("srsName", "http://www.opengis.net/gml/srs/epsg.xml#3857");
			Point2.setAttribute(srsName2);

			Element coordinates2 = new Element("coordinates", GML);
			Attribute C12 = new Attribute("fid", "http://www.opengis.net/gml");
			coordinates2.setAttribute(C12);
			Attribute C22 = new Attribute("decimal", ".");
			coordinates2.setAttribute(C22);
			Attribute C32 = new Attribute("cs", ",");
			coordinates2.setAttribute(C32);
			Attribute C42 = new Attribute("ts", " ");
			coordinates2.setAttribute(C42);
			String localCoordinates2 = new String();
			Point point = geom1.getCentroid();
			localCoordinates2 = "";
			localCoordinates2 = localCoordinates2 + point.getX_() + "," + point.getY_() + " ";
			coordinates2.setText(localCoordinates2);
			Point2.addContent(coordinates2);
		}
	}

	public static void writeGMLBackGround(Geometrie geom1, Element root, Namespace GML, Namespace TOPP, Integer cpt) {
		Element featureMember = new Element("featureMember", GML);

		root.addContent(featureMember);

		// On crÃ©e une balise newpoint et on l'ajoute Ã  la balise racine.
		Element newpoint = new Element("newpointsres1", TOPP);
		featureMember.addContent(newpoint);

		// On crÃ©e un attribut "name" contenant le nom de l'indicateur et on ajoute
		// l'attribut Ã  la balise Indicateur
		Attribute NP = new Attribute("fid", "newpointsres1." + (cpt + 1));
		newpoint.setAttribute(NP);

		Element id = new Element("ID", TOPP);
		String localID = new String();
		localID = String.valueOf(cpt);
		id.setText(localID);
		newpoint.addContent(id);

		/////////////////////////////////////////
		Element value = new Element("BackGround", TOPP);
		String localValue = "0";
		/////////////////////////////////////////

		value.setText(localValue);
		newpoint.addContent(value);

		Element nom = new Element("nom", TOPP);
		String localNom = new String();
		localNom = geom1.getNom();
		nom.setText(localNom);
		newpoint.addContent(nom);

		Element the_geom = new Element("the_geom", TOPP);
		newpoint.addContent(the_geom);

		if ("POLYGON".equals(geom1.getType())) {
			// ajout anael
			Element multiPolygon = new Element("MultiPolygon", GML);
			the_geom.addContent(multiPolygon);

			for (int i = 0; i < geom1.getListePolygon().size(); i++) {
				Element polygonMember = new Element("polygonMember", GML);
				multiPolygon.addContent(polygonMember);

				Element polygon = new Element("Polygon", GML);
				polygonMember.addContent(polygon);

				Element outerBoundaryIs = new Element("outerBoundaryIs", GML);
				polygon.addContent(outerBoundaryIs);

				Element LinearRing = new Element("LinearRing", GML);
				outerBoundaryIs.addContent(LinearRing);

				Element coordinates = new Element("coordinates", GML);
				coordinates.addNamespaceDeclaration(GML);
				Attribute C2 = new Attribute("decimal", ".");
				coordinates.setAttribute(C2);
				Attribute C3 = new Attribute("cs", ",");
				coordinates.setAttribute(C3);
				Attribute C4 = new Attribute("ts", " ");
				coordinates.setAttribute(C4);
				String localCoordinates = new String();
				// ajout anael
				List<Point> listePoint = geom1.getListePolygon().get(i);
				localCoordinates = "";
				for (Point p : listePoint) {
					localCoordinates = localCoordinates + p.getX_() + "," + p.getY_() + " ";
				}
				coordinates.setText(localCoordinates);
				LinearRing.addContent(coordinates);
			}
		} else {
			// ajout anael
			Element multiPolygon = new Element("MultiPolygon", GML);
			the_geom.addContent(multiPolygon);

			for (int i = 0; i < geom1.getListePolygon().size(); i++) {
				Element polygonMember = new Element("polygonMember", GML);
				multiPolygon.addContent(polygonMember);

				Element polygon = new Element("Polygon", GML);
				polygonMember.addContent(polygon);

				Element outerBoundaryIs = new Element("outerBoundaryIs", GML);
				polygon.addContent(outerBoundaryIs);

				Element LinearRing = new Element("LinearRing", GML);
				outerBoundaryIs.addContent(LinearRing);

				Element coordinates = new Element("coordinates", GML);
				coordinates.addNamespaceDeclaration(GML);
				Attribute C2 = new Attribute("decimal", ".");
				coordinates.setAttribute(C2);
				Attribute C3 = new Attribute("cs", ",");
				coordinates.setAttribute(C3);
				Attribute C4 = new Attribute("ts", " ");
				coordinates.setAttribute(C4);
				String localCoordinates = new String();
				// ajout anael
				List<Point> listePoint = geom1.getListePolygon().get(i);
				localCoordinates = "";
				for (Point p : listePoint) {
					localCoordinates = localCoordinates + p.getX_() + "," + p.getY_() + " ";
				}
				coordinates.setText(localCoordinates);
				LinearRing.addContent(coordinates);
			}
		}

	}

	//////////////////////////////////

	public static void writeSLDCircle(Element NamedLayer, Namespace OGC, String titre_final, Indicateur ind,
			String couleur, long min, long max, int count_levels, int size_min, int sizeInter) {
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element UserStyle = new Element("UserStyle");
		NamedLayer.addContent(UserStyle);
		Element Title = new Element("Title");
		UserStyle.addContent(Title);
		Title.addContent(titre_final);

		Element IsDefault = new Element("IsDefault");
		UserStyle.addContent(IsDefault);
		String def = new String();
		def = "0";
		IsDefault.addContent(def);
		Element FeatureTypeStyle = new Element("FeatureTypeStyle");
		UserStyle.addContent(FeatureTypeStyle);

		for (int j = 1; j <= count_levels; j++) {
			// Règles
			Element Rule = new Element("Rule");
			FeatureTypeStyle.addContent(Rule);

			Element Filter = new Element("Filter", OGC);
			Rule.addContent(Filter);

			Element And = new Element("And", OGC);
			Filter.addContent(And);

			Element PropertyIGTOE = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
			And.addContent(PropertyIGTOE);

			Element PropertyName1 = new Element("PropertyName", OGC);
			PropertyIGTOE.addContent(PropertyName1);
			String PropName1 = new String();
			PropName1 = String.valueOf(ind_name);
			PropertyName1.addContent(PropName1);

			Element Literal1 = new Element("Literal", OGC);
			PropertyIGTOE.addContent(Literal1);
			String literal1 = new String();
			long val = (j - 1) * (max - min) / count_levels + min;
			literal1 = "" + val;
			Literal1.addContent(literal1);

			Element PropertyIsLessThan2 = new Element("PropertyIsLessThanOrEqualTo", OGC);
			And.addContent(PropertyIsLessThan2);

			Element PropertyName2 = new Element("PropertyName", OGC);
			PropertyIsLessThan2.addContent(PropertyName2);
			String PropName2 = new String();
			PropName2 = String.valueOf(ind_name);
			PropertyName2.addContent(PropName2);

			Element Literal2 = new Element("Literal", OGC);
			PropertyIsLessThan2.addContent(Literal2);
			String literal2 = new String();
			val = j * (max - min) / count_levels + min;
			literal2 = "" + val;
			Literal2.addContent(literal2);

			// Règles de création pour les points
			Element PointSymbolizer = new Element("PointSymbolizer");
			Rule.addContent(PointSymbolizer);
			Element Graphic = new Element("Graphic");
			PointSymbolizer.addContent(Graphic);
			Element Mark = new Element("Mark");
			Graphic.addContent(Mark);
			Element WellKnownName = new Element("WellKnownName");
			Mark.addContent(WellKnownName);
			String WKN = new String();
			WKN = "circle";
			WellKnownName.addContent(WKN);
			Element Fill = new Element("Fill");
			Mark.addContent(Fill);
			Element CssParameter = new Element("CssParameter");
			Fill.addContent(CssParameter);
			Attribute cssPara = new Attribute("name", "fill");
			CssParameter.setAttribute(cssPara);

			CssParameter.addContent(couleur);
			Element Size = new Element("Size");
			Graphic.addContent(Size);
			String taille = new String();
			taille = "" + (j * sizeInter + size_min);
			Size.addContent(taille);
			// System.out.println("taille = j*sizeInter + size_min; j:"+j+" size_min:
			// "+size_min+" sizeInter: "+ sizeInter+" taille: "+taille);

			// Règle de création des polygones
			Element PolygonSymbolizer = new Element("PolygonSymbolizer");
			Element Fillbis = new Element("Fill");
			Element CssParameterBis = new Element("CssParameter");
			Attribute cssParaBis = new Attribute("name", "fill-opacity");
			CssParameterBis.setAttribute(cssParaBis);

			String color = "0.0";
			CssParameterBis.addContent(color);
			Fillbis.addContent(CssParameterBis);
			PolygonSymbolizer.addContent(Fillbis);
			Rule.addContent(PolygonSymbolizer);
		}

	}

	public static void writeSLDBar(Element NamedLayer, Element FeatureTypeStyle, Namespace OGC, boolean new_measure,
			String measure, Indicateur ind, int R, int G, int B, long min) {
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		if (new_measure) {
			// pour chaque mesure, on ajoute un seul style avec un seul "FeatureTypeStyle"
			// qui contient toutes les règles pour tous les indicateurs
			Element UserStyle = new Element("UserStyle");
			NamedLayer.addContent(UserStyle);
			Element Title = new Element("Title");
			UserStyle.addContent(Title);
			String titre = new String();
			titre = measure;
			Title.addContent(titre);

			Element IsDefault = new Element("IsDefault");
			UserStyle.addContent(IsDefault);
			String def = new String();
			def = "0";
			IsDefault.addContent(def);
			// FeatureTypeStyle = new Element("FeatureTypeStyle");
			UserStyle.addContent(FeatureTypeStyle);
		}

		// si un nouveau indicateur arrive pour une mesure déjà traitée on ajoute sa
		// règle dans le même "FeatureTypeStyle"
		// règle
		Element Rule = new Element("Rule");
		FeatureTypeStyle.addContent(Rule);

		Element Filter = new Element("Filter", OGC);
		Rule.addContent(Filter);

		Element PropertyIsLessThan = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
		Filter.addContent(PropertyIsLessThan);

		Element PropertyName = new Element("PropertyName", OGC);
		PropertyIsLessThan.addContent(PropertyName);
		String PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName.addContent(PropName);

		Element Literal = new Element("Literal", OGC);
		PropertyIsLessThan.addContent(Literal);
		String literal = new String();
		// val = tailleInter+min;
		long val = min;
		literal = "" + val;
		Literal.addContent(literal);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer = new Element("PolygonSymbolizer");
		Element Fillbis = new Element("Fill");
		Element CssParameterBis = new Element("CssParameter");
		Attribute cssParaBis = new Attribute("name", "fill");
		CssParameterBis.setAttribute(cssParaBis);

		String color = "#" + Integer.toHexString(R).toUpperCase() + Integer.toHexString(G).toUpperCase()
				+ Integer.toHexString(B).toUpperCase();
		CssParameterBis.addContent(color);
		Fillbis.addContent(CssParameterBis);
		PolygonSymbolizer.addContent(Fillbis);

		Element Fillbis10 = new Element("Stroke");
		Element CssParameterBis10 = new Element("CssParameter");
		Attribute cssParaBis10 = new Attribute("name", "stroke");
		CssParameterBis10.setAttribute(cssParaBis10);
		CssParameterBis10.addContent("#000000");
		Fillbis10.addContent(CssParameterBis10);
		PolygonSymbolizer.addContent(Fillbis10);

		Rule.addContent(PolygonSymbolizer);
	}

	public static void writeSLDCloropeth(Element NamedLayer, Namespace OGC, String titre_final, Indicateur ind,
			int R_couleur_0x, int G_couleur_0x, int B_couleur_0x, long min, long max, int count_levels) {
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element UserStyle = new Element("UserStyle");
		NamedLayer.addContent(UserStyle);
		Element Title = new Element("Title");
		UserStyle.addContent(Title);
		Title.addContent(titre_final);

		Element IsDefault = new Element("IsDefault");
		UserStyle.addContent(IsDefault);
		String def = new String();
		def = "0";
		IsDefault.addContent(def);
		Element FeatureTypeStyle = new Element("FeatureTypeStyle");
		UserStyle.addContent(FeatureTypeStyle);

		int R = 0, G = 0, B = 0;

		for (int j = 1; j <= count_levels; j++) {

			R = (255 - (j - 1) * (255 - R_couleur_0x) / count_levels);
			G = (255 - (j - 1) * (255 - G_couleur_0x) / count_levels);
			B = (255 - (j - 1) * (255 - B_couleur_0x) / count_levels);

			// Règles
			Element Rule = new Element("Rule");
			FeatureTypeStyle.addContent(Rule);

			Element Filter = new Element("Filter", OGC);
			Rule.addContent(Filter);

			Element And = new Element("And", OGC);
			Filter.addContent(And);

			Element PropertyIGTOE = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
			And.addContent(PropertyIGTOE);

			Element PropertyName1 = new Element("PropertyName", OGC);
			PropertyIGTOE.addContent(PropertyName1);
			String PropName1 = new String();
			PropName1 = String.valueOf(ind_name);
			PropertyName1.addContent(PropName1);

			Element Literal1 = new Element("Literal", OGC);
			PropertyIGTOE.addContent(Literal1);
			String literal1 = new String();
			long val = (j - 1) * (max - min) / count_levels + min;
			literal1 = "" + val;
			Literal1.addContent(literal1);

			Element PropertyIsLessThan2 = new Element("PropertyIsLessThanOrEqualTo", OGC);
			And.addContent(PropertyIsLessThan2);

			Element PropertyName2 = new Element("PropertyName", OGC);
			PropertyIsLessThan2.addContent(PropertyName2);
			String PropName2 = new String();
			PropName2 = String.valueOf(ind_name);
			PropertyName2.addContent(PropName2);

			Element Literal2 = new Element("Literal", OGC);
			PropertyIsLessThan2.addContent(Literal2);
			String literal2 = new String();
			val = j * (max - min) / count_levels + min;
			literal2 = "" + val;
			Literal2.addContent(literal2);

			// RÃ¨gle de crÃ©ation des polygones
			Element PolygonSymbolizer = new Element("PolygonSymbolizer");
			Element Fillbis = new Element("Fill");
			Element CssParameterBis = new Element("CssParameter");
			Attribute cssParaBis = new Attribute("name", "fill");
			CssParameterBis.setAttribute(cssParaBis);
			String color = "#" + Integer.toHexString(R).toUpperCase() + Integer.toHexString(G).toUpperCase()
					+ Integer.toHexString(B).toUpperCase();
			CssParameterBis.addContent(color);
			Fillbis.addContent(CssParameterBis);
			PolygonSymbolizer.addContent(Fillbis);

			Element Fillbis10 = new Element("Stroke");
			Element CssParameterBis10 = new Element("CssParameter");
			Attribute cssParaBis10 = new Attribute("name", "stroke");
			CssParameterBis10.setAttribute(cssParaBis10);
			CssParameterBis10.addContent("#000000");
			Fillbis10.addContent(CssParameterBis10);
			PolygonSymbolizer.addContent(Fillbis10);

			Rule.addContent(PolygonSymbolizer);
		}

	}

	public static void writeSLDMultiCloropeth(Element NamedLayer, Element FeatureTypeStyle, Namespace OGC,
			boolean new_measure, String measure, Indicateur ind, int R_couleur_0x, int G_couleur_0x, int B_couleur_0x,
			long min, long max, int count_levels) {
		if (new_measure) {
			// pour chaque mesure, on ajoute un seul style avec un seul "FeatureTypeStyle"
			// qui contient toutes les règles pour tous les indicateurs

			Element UserStyle = new Element("UserStyle");
			NamedLayer.addContent(UserStyle);
			Element Title = new Element("Title");
			UserStyle.addContent(Title);
			String titre = new String();
			titre = measure;
			Title.addContent(titre);

			Element IsDefault = new Element("IsDefault");
			UserStyle.addContent(IsDefault);
			String def = new String();
			def = "0";
			IsDefault.addContent(def);
			// FeatureTypeStyle = new Element("FeatureTypeStyle");
			UserStyle.addContent(FeatureTypeStyle);
		}

		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		for (int j = 1; j <= count_levels; j++) {

			int R = (255 - (j - 1) * (255 - R_couleur_0x) / count_levels);
			int G = (255 - (j - 1) * (255 - G_couleur_0x) / count_levels);
			int B = (255 - (j - 1) * (255 - B_couleur_0x) / count_levels);

			// Règles
			Element Rule = new Element("Rule");
			FeatureTypeStyle.addContent(Rule);

			Element Filter = new Element("Filter", OGC);
			Rule.addContent(Filter);

			Element And = new Element("And", OGC);
			Filter.addContent(And);

			Element PropertyIGTOE = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
			And.addContent(PropertyIGTOE);

			Element PropertyName1 = new Element("PropertyName", OGC);
			PropertyIGTOE.addContent(PropertyName1);
			String PropName1 = new String();
			PropName1 = String.valueOf(ind_name);
			PropertyName1.addContent(PropName1);

			Element Literal1 = new Element("Literal", OGC);
			PropertyIGTOE.addContent(Literal1);
			String literal1 = new String();
			long val = (j - 1) * (max - min) / count_levels + min;
			literal1 = "" + val;
			Literal1.addContent(literal1);

			Element PropertyIsLessThan2 = new Element("PropertyIsLessThanOrEqualTo", OGC);
			And.addContent(PropertyIsLessThan2);

			Element PropertyName2 = new Element("PropertyName", OGC);
			PropertyIsLessThan2.addContent(PropertyName2);
			String PropName2 = new String();
			PropName2 = String.valueOf(ind_name);
			PropertyName2.addContent(PropName2);

			Element Literal2 = new Element("Literal", OGC);
			PropertyIsLessThan2.addContent(Literal2);
			String literal2 = new String();
			val = j * (max - min) / count_levels + min;
			literal2 = "" + val;
			Literal2.addContent(literal2);

			// RÃ¨gle de crÃ©ation des polygones
			Element PolygonSymbolizer = new Element("PolygonSymbolizer");
			Element Fillbis = new Element("Fill");
			Element CssParameterBis = new Element("CssParameter");
			Attribute cssParaBis = new Attribute("name", "fill");
			CssParameterBis.setAttribute(cssParaBis);
			String color = "#" + Integer.toHexString(R).toUpperCase() + Integer.toHexString(G).toUpperCase()
					+ Integer.toHexString(B).toUpperCase();
			CssParameterBis.addContent(color);
			Fillbis.addContent(CssParameterBis);
			PolygonSymbolizer.addContent(Fillbis);

			Element Fillbis10 = new Element("Stroke");
			Element CssParameterBis10 = new Element("CssParameter");
			Attribute cssParaBis10 = new Attribute("name", "stroke");
			CssParameterBis10.setAttribute(cssParaBis10);
			CssParameterBis10.addContent("#000000");
			Fillbis10.addContent(CssParameterBis10);
			PolygonSymbolizer.addContent(Fillbis10);

			Rule.addContent(PolygonSymbolizer);
		}
	}

	// SLD codé en dure par les étudiants
	public static void writeSLDAreaChoreme(Element NamedLayer, Element FeatureTypeStyle, Namespace OGC,
			String titre_final, Indicateur ind) {
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element UserStyle = new Element("UserStyle");
		NamedLayer.addContent(UserStyle);
		Element Title = new Element("Title");
		UserStyle.addContent(Title);
		Title.addContent(titre_final);

		Element IsDefault = new Element("IsDefault");
		UserStyle.addContent(IsDefault);
		String def = new String();
		def = "0";
		IsDefault.addContent(def);
		FeatureTypeStyle = new Element("FeatureTypeStyle");
		UserStyle.addContent(FeatureTypeStyle);

		// PremiÃ¨re Rule
		Element Rule = new Element("Rule");
		FeatureTypeStyle.addContent(Rule);

		Element Filter = new Element("Filter", OGC);
		Rule.addContent(Filter);

		Element PropertyIsLessThan = new Element("PropertyIsEqualTo", OGC);
		Filter.addContent(PropertyIsLessThan);

		Element PropertyName = new Element("PropertyName", OGC);
		PropertyIsLessThan.addContent(PropertyName);
		String PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName.addContent(PropName);

		Element Literal = new Element("Literal", OGC);
		PropertyIsLessThan.addContent(Literal);
		String literal = new String();
		literal = "-1";
		Literal.addContent(literal);

		// RÃ¨gles de crÃ©ation pour les points
		Element PointSymbolizer = new Element("PointSymbolizer");
		Rule.addContent(PointSymbolizer);
		Element Graphic = new Element("Graphic");
		PointSymbolizer.addContent(Graphic);
		Element EG = new Element("ExternalGraphic");
		Graphic.addContent(EG);
		Element OR = new Element("OnlineResource");
		EG.addContent(OR);

		Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam = new Attribute("type", "simple", xlink);
		OR.setAttribute(orParam);
		Attribute orParam2 = new Attribute("href", "down.png", xlink);
		OR.setAttribute(orParam2);
		Element Format = new Element("Format");
		EG.addContent(Format);
		String format = new String();
		format = "image/gif";
		Format.addContent(format);
		Element Size = new Element("Size");
		Graphic.addContent(Size);
		String taille = new String();
		taille = "45";
		Size.addContent(taille);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer = new Element("PolygonSymbolizer");
		Element Fillbis = new Element("Fill");
		Element CssParameterBis = new Element("CssParameter");
		Attribute cssParaBis = new Attribute("name", "fill-opacity");
		CssParameterBis.setAttribute(cssParaBis);

		String couleur = "0.0";
		CssParameterBis.addContent(couleur);
		Fillbis.addContent(CssParameterBis);
		PolygonSymbolizer.addContent(Fillbis);
		Rule.addContent(PolygonSymbolizer);

		// DeuxiÃ¨me Rule
		Element Rule2 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule2);

		Element Filter2 = new Element("Filter", OGC);
		Rule2.addContent(Filter2);

		Element PropertyIGTOE = new Element("PropertyIsEqualTo", OGC);
		Filter2.addContent(PropertyIGTOE);

		Element PropertyName2 = new Element("PropertyName", OGC);
		PropertyIGTOE.addContent(PropertyName2);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName2.addContent(PropName);

		Element Literal2 = new Element("Literal", OGC);
		PropertyIGTOE.addContent(Literal2);
		literal = new String();
		literal = "1";
		Literal2.addContent(literal);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer2 = new Element("PointSymbolizer");
		Rule2.addContent(PointSymbolizer2);
		Element Graphic2 = new Element("Graphic");
		PointSymbolizer2.addContent(Graphic2);
		Element EG2 = new Element("ExternalGraphic");
		Graphic2.addContent(EG2);
		Element OR2 = new Element("OnlineResource");
		EG2.addContent(OR2);

		Namespace xlink2 = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam3 = new Attribute("type", "simple", xlink2);
		OR2.setAttribute(orParam3);
		Attribute orParam4 = new Attribute("href", "up.png", xlink2);
		OR2.setAttribute(orParam4);

		Element Format2 = new Element("Format");
		EG2.addContent(Format2);
		String format2 = new String();
		format2 = "image/gif";
		Format2.addContent(format2);
		Element Size2 = new Element("Size");
		Graphic2.addContent(Size2);
		String taille2 = new String();
		taille2 = "45";
		Size2.addContent(taille2);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolize2 = new Element("PolygonSymbolizer");
		Element Fillbi2 = new Element("Fill");
		Element CssParameterBis2 = new Element("CssParameter");
		Attribute cssParaBis2 = new Attribute("name", "fill-opacity");
		CssParameterBis2.setAttribute(cssParaBis2);
		CssParameterBis2.addContent("0.0");
		Fillbi2.addContent(CssParameterBis2);
		PolygonSymbolize2.addContent(Fillbi2);
		Rule2.addContent(PolygonSymbolize2);

		// Troisième Rule
		Element Rule3 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule3);

		Element Filter3 = new Element("Filter", OGC);
		Rule3.addContent(Filter3);

		Element PropertyIGTOE2 = new Element("PropertyIsEqualTo", OGC);
		Filter3.addContent(PropertyIGTOE2);

		Element PropertyName3 = new Element("PropertyName", OGC);
		PropertyIGTOE2.addContent(PropertyName3);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName3.addContent(PropName);

		Element Literal3 = new Element("Literal", OGC);
		PropertyIGTOE2.addContent(Literal3);
		literal = new String();
		literal = "0";
		Literal3.addContent(literal);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer3 = new Element("PointSymbolizer");
		Rule3.addContent(PointSymbolizer3);
		Element Graphic3 = new Element("Graphic");
		PointSymbolizer3.addContent(Graphic3);
		Element EG3 = new Element("ExternalGraphic");
		Graphic3.addContent(EG3);
		Element OR3 = new Element("OnlineResource");
		EG3.addContent(OR3);

		Namespace xlink3 = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam6 = new Attribute("type", "simple", xlink3);
		OR3.setAttribute(orParam6);
		Attribute orParam7 = new Attribute("href", "equal.png", xlink3);
		OR3.setAttribute(orParam7);

		Element Format3 = new Element("Format");
		EG3.addContent(Format3);
		String format3 = new String();
		format3 = "image/gif";
		Format3.addContent(format3);
		Element Size3 = new Element("Size");
		Graphic3.addContent(Size3);
		String taille3 = new String();
		taille3 = "45";
		Size3.addContent(taille3);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer3 = new Element("PolygonSymbolizer");
		Element Fillbis3 = new Element("Fill");
		Element CssParameterBis3 = new Element("CssParameter");
		Attribute cssParaBis3 = new Attribute("name", "fill-opacity");
		CssParameterBis3.setAttribute(cssParaBis3);
		CssParameterBis3.addContent("0.0");
		Fillbis3.addContent(CssParameterBis3);
		PolygonSymbolizer3.addContent(Fillbis3);
		Rule3.addContent(PolygonSymbolizer3);

	}

	// SLD codé en dure par les étudiants
	public static void writeSLDProdChoreme(Element NamedLayer, Element FeatureTypeStyle, Namespace OGC,
			String titre_final, Indicateur ind) {
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element UserStyle = new Element("UserStyle");
		NamedLayer.addContent(UserStyle);
		Element Title = new Element("Title");
		UserStyle.addContent(Title);
		Title.addContent(titre_final);

		Element IsDefault = new Element("IsDefault");
		UserStyle.addContent(IsDefault);
		String def = new String();
		def = "0";
		IsDefault.addContent(def);
		FeatureTypeStyle = new Element("FeatureTypeStyle");
		UserStyle.addContent(FeatureTypeStyle);

		// Première règle
		Element Rule = new Element("Rule");
		FeatureTypeStyle.addContent(Rule);

		Element Filter = new Element("Filter", OGC);
		Rule.addContent(Filter);

		Element PropertyIsLessThan = new Element("PropertyIsEqualTo", OGC);
		Filter.addContent(PropertyIsLessThan);

		Element PropertyName = new Element("PropertyName", OGC);
		PropertyIsLessThan.addContent(PropertyName);
		String PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName.addContent(PropName);

		Element Literal = new Element("Literal", OGC);
		PropertyIsLessThan.addContent(Literal);
		String literal = new String();
		literal = "-1";
		Literal.addContent(literal);

		Element PointSymbolizer = new Element("PointSymbolizer");
		Rule.addContent(PointSymbolizer);
		Element Graphic = new Element("Graphic");
		PointSymbolizer.addContent(Graphic);
		Element EG = new Element("ExternalGraphic");
		Graphic.addContent(EG);
		Element OR = new Element("OnlineResource");
		EG.addContent(OR);

		Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam = new Attribute("type", "simple", xlink);
		OR.setAttribute(orParam);
		Attribute orParam2 = new Attribute("href", "red.png", xlink);
		OR.setAttribute(orParam2);
		Element Format = new Element("Format");
		EG.addContent(Format);
		String format = new String();
		format = "image/gif";
		Format.addContent(format);
		Element Size = new Element("Size");
		Graphic.addContent(Size);
		String taille = new String();
		taille = "45";
		Size.addContent(taille);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer = new Element("PolygonSymbolizer");
		Element Fillbis = new Element("Fill");
		Element CssParameterBis = new Element("CssParameter");
		Attribute cssParaBis = new Attribute("name", "fill-opacity");
		CssParameterBis.setAttribute(cssParaBis);
		CssParameterBis.addContent("0.0");
		Fillbis.addContent(CssParameterBis);
		PolygonSymbolizer.addContent(Fillbis);
		Rule.addContent(PolygonSymbolizer);

		// Deuxième règle
		Element Rule2 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule2);

		Element Filter2 = new Element("Filter", OGC);
		Rule2.addContent(Filter2);

		Element And = new Element("And", OGC);
		Filter2.addContent(And);

		Element PropertyIGTOE = new Element("PropertyIsEqualTo", OGC);
		And.addContent(PropertyIGTOE);

		Element PropertyName2 = new Element("PropertyName", OGC);
		PropertyIGTOE.addContent(PropertyName2);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName2.addContent(PropName);

		Element Literal2 = new Element("Literal", OGC);
		PropertyIGTOE.addContent(Literal2);
		literal = new String();
		literal = "0";
		Literal2.addContent(literal);

		Element PointSymbolizer2 = new Element("PointSymbolizer");
		Rule2.addContent(PointSymbolizer2);
		Element Graphic2 = new Element("Graphic");
		PointSymbolizer2.addContent(Graphic2);
		Element EG2 = new Element("ExternalGraphic");
		Graphic2.addContent(EG2);
		Element OR2 = new Element("OnlineResource");
		EG2.addContent(OR2);

		Namespace xlink2 = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam3 = new Attribute("type", "simple", xlink2);
		OR2.setAttribute(orParam3);
		Attribute orParam4 = new Attribute("href", "yellow.png", xlink2);
		OR2.setAttribute(orParam4);

		Element Format2 = new Element("Format");
		EG2.addContent(Format2);
		String format2 = new String();
		format2 = "image/gif";
		Format2.addContent(format2);
		Element Size2 = new Element("Size");
		Graphic2.addContent(Size2);
		String taille2 = new String();
		taille2 = "45";
		Size2.addContent(taille2);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolize2 = new Element("PolygonSymbolizer");
		Element Fillbi2 = new Element("Fill");
		Element CssParameterBis2 = new Element("CssParameter");
		Attribute cssParaBis2 = new Attribute("name", "fill-opacity");
		CssParameterBis2.setAttribute(cssParaBis2);
		CssParameterBis2.addContent("0.0");
		Fillbi2.addContent(CssParameterBis2);
		PolygonSymbolize2.addContent(Fillbi2);
		Rule2.addContent(PolygonSymbolize2);

		// Troisième règle
		Element Rule3 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule3);

		Element Filter3 = new Element("Filter", OGC);
		Rule3.addContent(Filter3);

		Element PropertyIGTO4 = new Element("PropertyIsEqualTo", OGC);
		Filter3.addContent(PropertyIGTO4);

		Element PropertyName4 = new Element("PropertyName", OGC);
		PropertyIGTO4.addContent(PropertyName4);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName4.addContent(PropName);

		Element Literal4 = new Element("Literal", OGC);
		PropertyIGTO4.addContent(Literal4);
		literal = new String();
		literal = "1";
		Literal4.addContent(literal);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer3 = new Element("PointSymbolizer");
		Rule3.addContent(PointSymbolizer3);
		Element Graphic3 = new Element("Graphic");
		PointSymbolizer3.addContent(Graphic3);
		Element EG3 = new Element("ExternalGraphic");
		Graphic3.addContent(EG3);
		Element OR3 = new Element("OnlineResource");
		EG3.addContent(OR3);

		Namespace xlink3 = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam6 = new Attribute("type", "simple", xlink3);
		OR3.setAttribute(orParam6);
		Attribute orParam7 = new Attribute("href", "green.png", xlink3);
		OR3.setAttribute(orParam7);

		Element Format3 = new Element("Format");
		EG3.addContent(Format3);
		String format3 = new String();
		format3 = "image/gif";
		Format3.addContent(format3);
		Element Size3 = new Element("Size");
		Graphic3.addContent(Size3);
		String taille3 = new String();
		taille3 = "45";
		Size3.addContent(taille3);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer3 = new Element("PolygonSymbolizer");
		Element Fillbis3 = new Element("Fill");
		Element CssParameterBis3 = new Element("CssParameter");
		Attribute cssParaBis3 = new Attribute("name", "fill-opacity");
		CssParameterBis3.setAttribute(cssParaBis3);
		CssParameterBis3.addContent("0.0");
		Fillbis3.addContent(CssParameterBis3);
		PolygonSymbolizer3.addContent(Fillbis3);
		Rule3.addContent(PolygonSymbolizer3);
	}

	// SLD codé en dure par les étudiants
	public static void writeSLDRond(Element NamedLayer, Element FeatureTypeStyle, Namespace OGC, String titre_final,
			Indicateur ind, long min, long tailleInter) {
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element UserStyle = new Element("UserStyle");
		NamedLayer.addContent(UserStyle);
		Element Title = new Element("Title");
		UserStyle.addContent(Title);
		Title.addContent(titre_final);

		Element IsDefault = new Element("IsDefault");
		UserStyle.addContent(IsDefault);
		String def = new String();
		def = "0";
		IsDefault.addContent(def);
		FeatureTypeStyle = new Element("FeatureTypeStyle");
		UserStyle.addContent(FeatureTypeStyle);

		// Première règle
		Element Rule = new Element("Rule");
		FeatureTypeStyle.addContent(Rule);

		Element Filter = new Element("Filter", OGC);
		Rule.addContent(Filter);

		Element PropertyIsLessThan = new Element("PropertyIsLessThan", OGC);
		Filter.addContent(PropertyIsLessThan);

		Element PropertyName = new Element("PropertyName", OGC);
		PropertyIsLessThan.addContent(PropertyName);
		String PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName.addContent(PropName);

		Element Literal = new Element("Literal", OGC);
		PropertyIsLessThan.addContent(Literal);
		String literal = new String();
		long val = tailleInter + min;
		literal = "" + val;
		Literal.addContent(literal);

		// RÃ¨gles de crÃ©ation pour les points
		Element PointSymbolizer = new Element("PointSymbolizer");
		Rule.addContent(PointSymbolizer);
		Element Graphic = new Element("Graphic");
		PointSymbolizer.addContent(Graphic);
		Element EG = new Element("ExternalGraphic");
		Graphic.addContent(EG);
		Element OR = new Element("OnlineResource");
		EG.addContent(OR);

		Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam = new Attribute("type", "simple", xlink);
		OR.setAttribute(orParam);
		Attribute orParam2 = new Attribute("href", "greencircle.png", xlink);
		OR.setAttribute(orParam2);
		Element Format = new Element("Format");
		EG.addContent(Format);
		String format = new String();
		format = "image/gif";
		Format.addContent(format);
		Element Size = new Element("Size");
		Graphic.addContent(Size);
		String taille = new String();
		taille = "15";
		Size.addContent(taille);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer = new Element("PolygonSymbolizer");
		Element Fillbis = new Element("Fill");
		Element CssParameterBis = new Element("CssParameter");
		Attribute cssParaBis = new Attribute("name", "fill-opacity");
		CssParameterBis.setAttribute(cssParaBis);

		String couleur = "0.0";
		CssParameterBis.addContent(couleur);
		Fillbis.addContent(CssParameterBis);
		PolygonSymbolizer.addContent(Fillbis);
		Rule.addContent(PolygonSymbolizer);

		// Deuxième règle
		Element Rule2 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule2);

		Element Filter2 = new Element("Filter", OGC);
		Rule2.addContent(Filter2);

		Element And = new Element("And", OGC);
		Filter2.addContent(And);

		Element PropertyIGTOE = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
		And.addContent(PropertyIGTOE);

		Element PropertyName2 = new Element("PropertyName", OGC);
		PropertyIGTOE.addContent(PropertyName2);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName2.addContent(PropName);

		Element Literal2 = new Element("Literal", OGC);
		PropertyIGTOE.addContent(Literal2);
		literal = new String();
		val = tailleInter + min;
		literal = "" + val;
		Literal2.addContent(literal);

		Element PropertyIsLessThan3 = new Element("PropertyIsLessThan", OGC);
		And.addContent(PropertyIsLessThan3);

		Element PropertyName3 = new Element("PropertyName", OGC);
		PropertyIsLessThan3.addContent(PropertyName3);
		String PropName3 = new String();
		PropName3 = String.valueOf(ind_name);
		PropertyName3.addContent(PropName3);

		Element Literal3 = new Element("Literal", OGC);
		PropertyIsLessThan3.addContent(Literal3);
		String literal3 = new String();
		val = tailleInter * 2 + min;
		literal3 = "" + val;
		Literal3.addContent(literal3);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer2 = new Element("PointSymbolizer");
		Rule2.addContent(PointSymbolizer2);
		Element Graphic2 = new Element("Graphic");
		PointSymbolizer2.addContent(Graphic2);
		Element EG2 = new Element("ExternalGraphic");
		Graphic2.addContent(EG2);
		Element OR2 = new Element("OnlineResource");
		EG2.addContent(OR2);

		Namespace xlink2 = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam3 = new Attribute("type", "simple", xlink2);
		OR2.setAttribute(orParam3);
		Attribute orParam4 = new Attribute("href", "greencircle.png", xlink2);
		OR2.setAttribute(orParam4);

		Element Format2 = new Element("Format");
		EG2.addContent(Format2);
		String format2 = new String();
		format2 = "image/gif";
		Format2.addContent(format2);
		Element Size2 = new Element("Size");
		Graphic2.addContent(Size2);
		String taille2 = new String();
		taille2 = "20";
		Size2.addContent(taille2);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolize2 = new Element("PolygonSymbolizer");
		Element Fillbi2 = new Element("Fill");
		Element CssParameterBis2 = new Element("CssParameter");
		Attribute cssParaBis2 = new Attribute("name", "fill-opacity");
		CssParameterBis2.setAttribute(cssParaBis2);
		CssParameterBis2.addContent("0.0");
		Fillbi2.addContent(CssParameterBis2);
		PolygonSymbolize2.addContent(Fillbi2);
		Rule2.addContent(PolygonSymbolize2);

		// Troisième règle
		Element Rule3 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule3);

		Element Filter3 = new Element("Filter", OGC);
		Rule3.addContent(Filter3);

		Element An2 = new Element("And", OGC);
		Filter3.addContent(An2);

		Element PropertyIGTO4 = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
		An2.addContent(PropertyIGTO4);

		Element PropertyName4 = new Element("PropertyName", OGC);
		PropertyIGTO4.addContent(PropertyName4);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName4.addContent(PropName);

		Element Literal4 = new Element("Literal", OGC);
		PropertyIGTO4.addContent(Literal4);
		literal = new String();
		val = tailleInter * 2 + min;
		literal = "" + val;
		Literal4.addContent(literal);

		Element PropertyIsLessThan4 = new Element("PropertyIsLessThan", OGC);
		An2.addContent(PropertyIsLessThan4);

		Element PropertyName5 = new Element("PropertyName", OGC);
		PropertyIsLessThan4.addContent(PropertyName5);
		String PropName4 = new String();
		PropName4 = String.valueOf(ind_name);
		PropertyName5.addContent(PropName4);

		Element Literal5 = new Element("Literal", OGC);
		PropertyIsLessThan4.addContent(Literal5);
		literal = new String();
		val = tailleInter * 3 + min;
		literal = "" + val;
		Literal5.addContent(literal);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer3 = new Element("PointSymbolizer");
		Rule3.addContent(PointSymbolizer3);
		Element Graphic3 = new Element("Graphic");
		PointSymbolizer3.addContent(Graphic3);
		Element EG3 = new Element("ExternalGraphic");
		Graphic3.addContent(EG3);
		Element OR3 = new Element("OnlineResource");
		EG3.addContent(OR3);

		Namespace xlink3 = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam5 = new Attribute("type", "simple", xlink3);
		OR3.setAttribute(orParam5);
		Attribute orParam6 = new Attribute("href", "greencircle.png", xlink3);
		OR3.setAttribute(orParam6);

		Element Format3 = new Element("Format");
		EG3.addContent(Format3);
		String format3 = new String();
		format3 = "image/gif";
		Format3.addContent(format3);
		Element Size3 = new Element("Size");
		Graphic3.addContent(Size3);
		String taille3 = new String();
		taille3 = "25";
		Size3.addContent(taille3);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolize3 = new Element("PolygonSymbolizer");
		Element Fillbi3 = new Element("Fill");
		Element CssParameterBis3 = new Element("CssParameter");
		Attribute cssParaBis3 = new Attribute("name", "fill-opacity");
		CssParameterBis3.setAttribute(cssParaBis3);
		CssParameterBis3.addContent("0.0");
		Fillbi3.addContent(CssParameterBis3);
		PolygonSymbolize3.addContent(Fillbi3);
		Rule3.addContent(PolygonSymbolize3);

		// Quatirème règle
		Element Rule4 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule4);

		Element Filter4 = new Element("Filter", OGC);
		Rule4.addContent(Filter4);

		Element And3 = new Element("And", OGC);
		Filter4.addContent(And3);

		Element PropertyIGTO5 = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
		And3.addContent(PropertyIGTO5);

		Element PropertyName6 = new Element("PropertyName", OGC);
		PropertyIGTO5.addContent(PropertyName6);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName6.addContent(PropName);

		Element Literal7 = new Element("Literal", OGC);
		PropertyIGTO5.addContent(Literal7);
		literal = new String();
		val = tailleInter * 3 + min;
		literal = "" + val;
		Literal7.addContent(literal);

		Element PropertyIsLessThan5 = new Element("PropertyIsLessThan", OGC);
		And3.addContent(PropertyIsLessThan5);

		Element PropertyName7 = new Element("PropertyName", OGC);
		PropertyIsLessThan5.addContent(PropertyName7);
		String PropName5 = new String();
		PropName5 = String.valueOf(ind_name);
		PropertyName7.addContent(PropName5);

		Element Literal6 = new Element("Literal", OGC);
		PropertyIsLessThan5.addContent(Literal6);
		literal = new String();
		val = tailleInter * 4 + min;
		literal = "" + val;
		Literal6.addContent(literal);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer4 = new Element("PointSymbolizer");
		Rule4.addContent(PointSymbolizer4);
		Element Graphic4 = new Element("Graphic");
		PointSymbolizer4.addContent(Graphic4);
		Element EG4 = new Element("ExternalGraphic");
		Graphic4.addContent(EG4);
		Element OR4 = new Element("OnlineResource");
		EG4.addContent(OR4);

		Namespace xlink4 = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam7 = new Attribute("type", "simple", xlink4);
		OR4.setAttribute(orParam7);
		Attribute orParam8 = new Attribute("href", "greencircle.png", xlink4);
		OR4.setAttribute(orParam8);

		Element Format4 = new Element("Format");
		EG4.addContent(Format4);
		String format4 = new String();
		format4 = "image/gif";
		Format4.addContent(format4);
		Element Size4 = new Element("Size");
		Graphic4.addContent(Size4);
		String taille4 = new String();
		taille4 = "30";
		Size4.addContent(taille4);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolize4 = new Element("PolygonSymbolizer");
		Element Fillbi4 = new Element("Fill");
		Element CssParameterBis4 = new Element("CssParameter");
		Attribute cssParaBis4 = new Attribute("name", "fill-opacity");
		CssParameterBis4.setAttribute(cssParaBis4);
		CssParameterBis4.addContent("0.0");
		Fillbi4.addContent(CssParameterBis4);
		PolygonSymbolize4.addContent(Fillbi4);
		Rule4.addContent(PolygonSymbolize4);

		// Cinquième règle
		Element Rule5 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule5);

		Element Filter5 = new Element("Filter", OGC);
		Rule5.addContent(Filter5);

		Element PropertyIGTO6 = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
		Filter5.addContent(PropertyIGTO6);

		Element PropertyName9 = new Element("PropertyName", OGC);
		PropertyIGTO6.addContent(PropertyName9);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName9.addContent(PropName);

		Element Literal9 = new Element("Literal", OGC);
		PropertyIGTO6.addContent(Literal9);
		literal = new String();
		val = tailleInter * 4 + min;
		literal = "" + val;
		Literal9.addContent(literal);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer5 = new Element("PointSymbolizer");
		Rule5.addContent(PointSymbolizer5);
		Element Graphic5 = new Element("Graphic");
		PointSymbolizer5.addContent(Graphic5);
		Element EG5 = new Element("ExternalGraphic");
		Graphic5.addContent(EG5);
		Element OR5 = new Element("OnlineResource");
		EG5.addContent(OR5);

		Namespace xlink5 = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
		Attribute orParam9 = new Attribute("type", "simple", xlink5);
		OR5.setAttribute(orParam9);
		Attribute orParam10 = new Attribute("href", "greencircle.png", xlink5);
		OR5.setAttribute(orParam10);

		Element Format5 = new Element("Format");
		EG5.addContent(Format5);
		String format5 = new String();
		format5 = "image/gif";
		Format5.addContent(format5);
		Element Size5 = new Element("Size");
		Graphic5.addContent(Size5);
		String taille5 = new String();
		taille5 = "35";
		Size5.addContent(taille5);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolize5 = new Element("PolygonSymbolizer");
		Element Fillbi5 = new Element("Fill");
		Element CssParameterBis5 = new Element("CssParameter");
		Attribute cssParaBis5 = new Attribute("name", "fill-opacity");
		CssParameterBis5.setAttribute(cssParaBis5);
		CssParameterBis5.addContent("0.0");
		Fillbi5.addContent(CssParameterBis5);
		PolygonSymbolize5.addContent(Fillbi5);
		Rule5.addContent(PolygonSymbolize5);
	}

	// SLD codé en dure par les étudiants
	public static void writeSLDDefault(Element NamedLayer, Element FeatureTypeStyle, Namespace OGC, String titre_final,
			Indicateur ind, long min, long tailleInter) {
		// pour corriger les problème des espaces et caratère speciaux dans le nom des
		// mesures et des niveaux de granularité, on les remplace par '_'
		String ind_name = ind.getNom().replaceAll("[^\\p{L}\\p{Nd}]+", "_");

		Element UserStyle = new Element("UserStyle");
		NamedLayer.addContent(UserStyle);
		Element Title = new Element("Title");
		UserStyle.addContent(Title);
		Title.addContent(titre_final);

		Element IsDefault = new Element("IsDefault");
		UserStyle.addContent(IsDefault);
		String def = new String();
		def = "0";
		IsDefault.addContent(def);
		FeatureTypeStyle = new Element("FeatureTypeStyle");
		UserStyle.addContent(FeatureTypeStyle);

		// Première règle
		Element Rule = new Element("Rule");
		FeatureTypeStyle.addContent(Rule);

		Element Filter = new Element("Filter", OGC);
		Rule.addContent(Filter);

		Element PropertyIsLessThan = new Element("PropertyIsLessThan", OGC);
		Filter.addContent(PropertyIsLessThan);

		Element PropertyName = new Element("PropertyName", OGC);
		PropertyIsLessThan.addContent(PropertyName);
		String PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName.addContent(PropName);

		Element Literal = new Element("Literal", OGC);
		PropertyIsLessThan.addContent(Literal);
		String literal = new String();
		long val = tailleInter + min;
		literal = "" + val;
		Literal.addContent(literal);

		// RÃ¨gles de crÃ©ation pour les points
		Element PointSymbolizer = new Element("PointSymbolizer");
		Rule.addContent(PointSymbolizer);
		Element Graphic = new Element("Graphic");
		PointSymbolizer.addContent(Graphic);
		Element Mark = new Element("Mark");
		Graphic.addContent(Mark);
		Element WellKnownName = new Element("WellKnownName");
		Mark.addContent(WellKnownName);
		String WKN = new String();
		WKN = "circle";
		WellKnownName.addContent(WKN);
		Element Fill = new Element("Fill");
		Mark.addContent(Fill);
		Element CssParameter = new Element("CssParameter");
		Fill.addContent(CssParameter);
		Attribute cssPara = new Attribute("name", "fill");
		CssParameter.setAttribute(cssPara);

		String couleur = "#FFFFFF";
		CssParameter.addContent(couleur);
		Element CssParameter1 = new Element("CssParameter");
		Fill.addContent(CssParameter1);
		Attribute cssPara1 = new Attribute("name", "fill-opacity");
		CssParameter1.setAttribute(cssPara1);
		CssParameter1.addContent("0.0");
		Element Size = new Element("Size");
		Graphic.addContent(Size);
		String taille = new String();
		taille = "6";
		Size.addContent(taille);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer = new Element("PolygonSymbolizer");
		Element Fillbis = new Element("Fill");
		Element CssParameterBis = new Element("CssParameter");
		Attribute cssParaBis = new Attribute("name", "fill");
		CssParameterBis.setAttribute(cssParaBis);
		CssParameterBis.addContent(couleur);
		Fillbis.addContent(CssParameterBis);
		PolygonSymbolizer.addContent(Fillbis);

		Element Fillbis10 = new Element("Stroke");
		Element CssParameterBis10 = new Element("CssParameter");
		Attribute cssParaBis10 = new Attribute("name", "stroke");
		CssParameterBis10.setAttribute(cssParaBis10);
		CssParameterBis10.addContent("#000000");
		Fillbis10.addContent(CssParameterBis10);
		PolygonSymbolizer.addContent(Fillbis10);

		Rule.addContent(PolygonSymbolizer);

		// Deuxième règle
		Element Rule2 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule2);

		Element Filter2 = new Element("Filter", OGC);
		Rule2.addContent(Filter2);

		Element And = new Element("And", OGC);
		Filter2.addContent(And);

		Element PropertyIGTOE = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
		And.addContent(PropertyIGTOE);

		Element PropertyName2 = new Element("PropertyName", OGC);
		PropertyIGTOE.addContent(PropertyName2);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName2.addContent(PropName);

		Element Literal2 = new Element("Literal", OGC);
		PropertyIGTOE.addContent(Literal2);
		literal = new String();
		val = tailleInter + min;
		literal = "" + val;
		Literal2.addContent(literal);

		Element PropertyIsLessThan3 = new Element("PropertyIsLessThan", OGC);
		And.addContent(PropertyIsLessThan3);

		Element PropertyName3 = new Element("PropertyName", OGC);
		PropertyIsLessThan3.addContent(PropertyName3);
		String PropName3 = new String();
		PropName3 = String.valueOf(ind_name);
		PropertyName3.addContent(PropName3);

		Element Literal3 = new Element("Literal", OGC);
		PropertyIsLessThan3.addContent(Literal3);
		String literal3 = new String();
		val = tailleInter * 2 + min;
		literal3 = "" + val;
		Literal3.addContent(literal3);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer2 = new Element("PointSymbolizer");
		Rule2.addContent(PointSymbolizer2);
		Element Graphic2 = new Element("Graphic");
		PointSymbolizer2.addContent(Graphic2);
		Element Mark2 = new Element("Mark");
		Graphic2.addContent(Mark2);
		Element WellKnownName2 = new Element("WellKnownName");
		Mark2.addContent(WellKnownName2);
		String WK2 = new String();
		WK2 = "circle";
		WellKnownName2.addContent(WK2);
		Element Fill2 = new Element("Fill");
		Mark2.addContent(Fill2);
		Element CssParameter2 = new Element("CssParameter");
		Fill2.addContent(CssParameter2);
		Attribute cssPara2 = new Attribute("name", "fill");
		CssParameter2.setAttribute(cssPara2);
		String couleu2 = new String();
		couleu2 = "#FFB2B2";
		CssParameter2.addContent(couleu2);
		Element CssParameter7 = new Element("CssParameter");
		Fill2.addContent(CssParameter7);
		Attribute cssPara7 = new Attribute("name", "fill-opacity");
		CssParameter7.setAttribute(cssPara7);
		CssParameter7.addContent("0.0");
		Element Size2 = new Element("Size");
		Graphic2.addContent(Size2);
		taille = new String();
		taille = "8";
		Size2.addContent(taille);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolize2 = new Element("PolygonSymbolizer");
		Element Fillbi2 = new Element("Fill");
		Element CssParameterBis2 = new Element("CssParameter");
		Attribute cssParaBis2 = new Attribute("name", "fill");
		CssParameterBis2.setAttribute(cssParaBis2);
		CssParameterBis2.addContent(couleu2);
		Fillbi2.addContent(CssParameterBis2);
		PolygonSymbolize2.addContent(Fillbi2);

		Element Fillbis11 = new Element("Stroke");
		Element CssParameterBis11 = new Element("CssParameter");
		Attribute cssParaBis11 = new Attribute("name", "stroke");
		CssParameterBis11.setAttribute(cssParaBis11);
		CssParameterBis11.addContent("#000000");
		Fillbis11.addContent(CssParameterBis11);
		PolygonSymbolize2.addContent(Fillbis11);

		Rule2.addContent(PolygonSymbolize2);

		// Troisième règle
		Element Rule3 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule3);

		Element Filter3 = new Element("Filter", OGC);
		Rule3.addContent(Filter3);

		Element An2 = new Element("And", OGC);
		Filter3.addContent(An2);

		Element PropertyIGTO4 = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
		An2.addContent(PropertyIGTO4);

		Element PropertyName4 = new Element("PropertyName", OGC);
		PropertyIGTO4.addContent(PropertyName4);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName4.addContent(PropName);

		Element Literal4 = new Element("Literal", OGC);
		PropertyIGTO4.addContent(Literal4);
		literal = new String();
		val = tailleInter * 2 + min;
		literal = "" + val;
		Literal4.addContent(literal);

		Element PropertyIsLessThan4 = new Element("PropertyIsLessThan", OGC);
		An2.addContent(PropertyIsLessThan4);

		Element PropertyName5 = new Element("PropertyName", OGC);
		PropertyIsLessThan4.addContent(PropertyName5);
		String PropName4 = new String();
		PropName4 = String.valueOf(ind_name);
		PropertyName5.addContent(PropName4);

		Element Literal5 = new Element("Literal", OGC);
		PropertyIsLessThan4.addContent(Literal5);
		literal = new String();
		val = tailleInter * 3 + min;
		literal = "" + val;
		Literal5.addContent(literal);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer3 = new Element("PointSymbolizer");
		Rule3.addContent(PointSymbolizer3);
		Element Graphic3 = new Element("Graphic");
		PointSymbolizer3.addContent(Graphic3);
		Element Mark3 = new Element("Mark");
		Graphic3.addContent(Mark3);
		Element WellKnownName3 = new Element("WellKnownName");
		Mark3.addContent(WellKnownName3);
		String WK3 = new String();
		WK3 = "circle";
		WellKnownName3.addContent(WK3);
		Element Fill3 = new Element("Fill");
		Mark3.addContent(Fill3);
		Element CssParameter3 = new Element("CssParameter");
		Fill3.addContent(CssParameter3);
		Attribute cssPara3 = new Attribute("name", "fill");
		CssParameter3.setAttribute(cssPara3);
		String couleur3 = new String();
		couleur3 = "#FF6666";
		CssParameter3.addContent(couleur3);
		Element CssParameter8 = new Element("CssParameter");
		Fill3.addContent(CssParameter8);
		Attribute cssPara8 = new Attribute("name", "fill-opacity");
		CssParameter8.setAttribute(cssPara8);
		CssParameter8.addContent("0.0");
		Element Size3 = new Element("Size");
		Graphic3.addContent(Size3);
		taille = new String();
		taille = "10";
		Size3.addContent(taille);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer3 = new Element("PolygonSymbolizer");
		Element Fillbis3 = new Element("Fill");
		Element CssParameterBis3 = new Element("CssParameter");
		Attribute cssParaBis3 = new Attribute("name", "fill");
		CssParameterBis3.setAttribute(cssParaBis3);
		CssParameterBis3.addContent(couleur3);
		Fillbis3.addContent(CssParameterBis3);
		PolygonSymbolizer3.addContent(Fillbis3);

		Element Fillbis12 = new Element("Stroke");
		Element CssParameterBis12 = new Element("CssParameter");
		Attribute cssParaBis12 = new Attribute("name", "stroke");
		CssParameterBis12.setAttribute(cssParaBis12);
		CssParameterBis12.addContent("#000000");
		Fillbis12.addContent(CssParameterBis12);
		PolygonSymbolizer3.addContent(Fillbis12);

		Rule3.addContent(PolygonSymbolizer3);

		// Quatirème règle
		Element Rule4 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule4);

		Element Filter4 = new Element("Filter", OGC);
		Rule4.addContent(Filter4);

		Element And3 = new Element("And", OGC);
		Filter4.addContent(And3);

		Element PropertyIGTO5 = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
		And3.addContent(PropertyIGTO5);

		Element PropertyName6 = new Element("PropertyName", OGC);
		PropertyIGTO5.addContent(PropertyName6);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName6.addContent(PropName);

		Element Literal7 = new Element("Literal", OGC);
		PropertyIGTO5.addContent(Literal7);
		literal = new String();
		val = tailleInter * 3 + min;
		literal = "" + val;
		Literal7.addContent(literal);

		Element PropertyIsLessThan5 = new Element("PropertyIsLessThan", OGC);
		And3.addContent(PropertyIsLessThan5);

		Element PropertyName7 = new Element("PropertyName", OGC);
		PropertyIsLessThan5.addContent(PropertyName7);
		String PropName5 = new String();
		PropName5 = String.valueOf(ind_name);
		PropertyName7.addContent(PropName5);

		Element Literal6 = new Element("Literal", OGC);
		PropertyIsLessThan5.addContent(Literal6);
		literal = new String();
		val = tailleInter * 4 + min;
		literal = "" + val;
		Literal6.addContent(literal);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer4 = new Element("PointSymbolizer");
		Rule4.addContent(PointSymbolizer4);
		Element Graphic4 = new Element("Graphic");
		PointSymbolizer4.addContent(Graphic4);
		Element Mark4 = new Element("Mark");
		Graphic4.addContent(Mark4);
		Element WellKnownName4 = new Element("WellKnownName");
		Mark4.addContent(WellKnownName4);
		String WKN5 = new String();
		WKN5 = "circle";
		WellKnownName4.addContent(WKN5);
		Element Fill4 = new Element("Fill");
		Mark4.addContent(Fill4);
		Element CssParameter4 = new Element("CssParameter");
		Fill4.addContent(CssParameter4);
		Attribute cssPara4 = new Attribute("name", "fill");
		CssParameter4.setAttribute(cssPara4);
		String couleur4 = new String();
		couleur4 = "#FF1A1A";
		CssParameter4.addContent(couleur4);
		Element CssParameter9 = new Element("CssParameter");
		Fill4.addContent(CssParameter9);
		Attribute cssPara9 = new Attribute("name", "fill-opacity");
		CssParameter9.setAttribute(cssPara9);
		CssParameter9.addContent("0.0");
		Element Size4 = new Element("Size");
		Graphic4.addContent(Size4);
		taille = new String();
		taille = "12";
		Size4.addContent(taille);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer4 = new Element("PolygonSymbolizer");
		Element Fillbis4 = new Element("Fill");
		Element CssParameterBis4 = new Element("CssParameter");
		Attribute cssParaBis4 = new Attribute("name", "fill");
		CssParameterBis4.setAttribute(cssParaBis4);
		CssParameterBis4.addContent(couleur4);
		Fillbis4.addContent(CssParameterBis4);
		PolygonSymbolizer4.addContent(Fillbis4);

		Element Fillbis13 = new Element("Stroke");
		Element CssParameterBis13 = new Element("CssParameter");
		Attribute cssParaBis13 = new Attribute("name", "stroke");
		CssParameterBis13.setAttribute(cssParaBis13);
		CssParameterBis13.addContent("#000000");
		Fillbis13.addContent(CssParameterBis13);
		PolygonSymbolizer4.addContent(Fillbis13);

		Rule4.addContent(PolygonSymbolizer4);

		// Cinquième règle
		Element Rule5 = new Element("Rule");
		FeatureTypeStyle.addContent(Rule5);

		Element Filter5 = new Element("Filter", OGC);
		Rule5.addContent(Filter5);

		Element PropertyIGTO6 = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
		Filter5.addContent(PropertyIGTO6);

		Element PropertyName9 = new Element("PropertyName", OGC);
		PropertyIGTO6.addContent(PropertyName9);
		PropName = new String();
		PropName = String.valueOf(ind_name);
		PropertyName9.addContent(PropName);

		Element Literal9 = new Element("Literal", OGC);
		PropertyIGTO6.addContent(Literal9);
		literal = new String();
		val = tailleInter * 4 + min;
		literal = "" + val;
		Literal9.addContent(literal);

		// RÃ¨gle de crÃ©ation pour les points
		Element PointSymbolizer5 = new Element("PointSymbolizer");
		Rule5.addContent(PointSymbolizer5);
		Element Graphic5 = new Element("Graphic");
		PointSymbolizer5.addContent(Graphic5);
		Element Mark5 = new Element("Mark");
		Graphic5.addContent(Mark5);
		Element WellKnownName5 = new Element("WellKnownName");
		Mark5.addContent(WellKnownName5);
		String WKN7 = new String();
		WKN7 = "circle";
		WellKnownName5.addContent(WKN7);
		Element Fill5 = new Element("Fill");
		Mark5.addContent(Fill5);
		Element CssParameter5 = new Element("CssParameter");
		Fill5.addContent(CssParameter5);
		Attribute cssPara5 = new Attribute("name", "fill");
		CssParameter5.setAttribute(cssPara5);
		String couleu5 = new String();
		couleu5 = "#CD0000";
		CssParameter5.addContent(couleu5);
		Element CssParameter10 = new Element("CssParameter");
		Fill5.addContent(CssParameter10);
		Attribute cssPara10 = new Attribute("name", "fill-opacity");
		CssParameter10.setAttribute(cssPara10);
		CssParameter10.addContent("0.0");
		Element Size5 = new Element("Size");
		Graphic5.addContent(Size5);
		taille = new String();
		taille = "14";
		Size5.addContent(taille);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer5 = new Element("PolygonSymbolizer");
		Element Fillbis5 = new Element("Fill");
		Element CssParameterBis5 = new Element("CssParameter");
		Attribute cssParaBis5 = new Attribute("name", "fill");
		CssParameterBis5.setAttribute(cssParaBis5);
		CssParameterBis5.addContent(couleu5);
		Fillbis5.addContent(CssParameterBis5);
		PolygonSymbolizer5.addContent(Fillbis5);

		Element Fillbis14 = new Element("Stroke");
		Element CssParameterBis14 = new Element("CssParameter");
		Attribute cssParaBis14 = new Attribute("name", "stroke");
		CssParameterBis14.setAttribute(cssParaBis14);
		CssParameterBis14.addContent("#000000");
		Fillbis14.addContent(CssParameterBis14);
		PolygonSymbolizer5.addContent(Fillbis14);

		Rule5.addContent(PolygonSymbolizer5);
	}

	public static void writeSLDBackGround(Element NamedLayer, Namespace OGC, String couleur) {
		Element UserStyle = new Element("UserStyle");
		NamedLayer.addContent(UserStyle);
		Element Title = new Element("Title");
		UserStyle.addContent(Title);
		Title.addContent("BackGround");

		Element IsDefault = new Element("IsDefault");
		UserStyle.addContent(IsDefault);
		String def = new String();
		def = "0";
		IsDefault.addContent(def);
		Element FeatureTypeStyle = new Element("FeatureTypeStyle");
		UserStyle.addContent(FeatureTypeStyle);

		// Règles
		Element Rule = new Element("Rule");
		FeatureTypeStyle.addContent(Rule);

		Element Filter = new Element("Filter", OGC);
		Rule.addContent(Filter);

		Element And = new Element("And", OGC);
		Filter.addContent(And);

		Element PropertyIGTOE = new Element("PropertyIsGreaterThanOrEqualTo", OGC);
		And.addContent(PropertyIGTOE);

		Element PropertyName1 = new Element("PropertyName", OGC);
		PropertyIGTOE.addContent(PropertyName1);
		String PropName1 = new String();
		PropName1 = "BackGround";
		PropertyName1.addContent(PropName1);

		Element Literal1 = new Element("Literal", OGC);
		PropertyIGTOE.addContent(Literal1);
		String literal1 = new String();
		literal1 = "0";
		Literal1.addContent(literal1);

		// RÃ¨gle de crÃ©ation des polygones
		Element PolygonSymbolizer = new Element("PolygonSymbolizer");
		Element Fillbis = new Element("Fill");
		Element CssParameterBis = new Element("CssParameter");
		Attribute cssParaBis = new Attribute("name", "fill");
		CssParameterBis.setAttribute(cssParaBis);
		// String color= couleur;
		CssParameterBis.addContent(couleur);
		Fillbis.addContent(CssParameterBis);
		PolygonSymbolizer.addContent(Fillbis);

		Element Fillbis10 = new Element("Stroke");
		Element CssParameterBis10 = new Element("CssParameter");
		Attribute cssParaBis10 = new Attribute("name", "stroke");
		CssParameterBis10.setAttribute(cssParaBis10);
		CssParameterBis10.addContent("#000000");
		Fillbis10.addContent(CssParameterBis10);
		PolygonSymbolizer.addContent(Fillbis10);

		Rule.addContent(PolygonSymbolizer);

	}

	//////////////////////////////////

	static void affiche(Document doc) {
		try {
			// On utilise ici un affichage classique avec getPrettyFormat()
			XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
			sortie.output(doc, System.out);
		} catch (java.io.IOException e) {
		}
	}

	static void enregistre(String fichier, Document doc) {
		try {
			// On utilise ici un affichage classique avec getPrettyFormat()
			XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
			// Remarquez qu'il suffit simplement de crÃ©er une instance de FileOutputStream
			// avec en argument le nom du fichier pour effectuer la sÃ©rialisation.
			sortie.output(doc, new FileOutputStream(fichier));
		} catch (java.io.IOException e) {
		}
	}

	static void enregistre_Titles(String maps_title, Map<Integer, Map<String, List<String>>> titles) {
		File file_maps_title = new File(maps_title);
		FileOutputStream fos = null;

		if (!file_maps_title.exists()) {
			try {
				file_maps_title.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			fos = new FileOutputStream(file_maps_title);
			String contenuMaps_TitleString = new String();
			byte[] contenuMaps_TitleByte;
			int n = titles.size();
			// pour chaque carte
			// System.out.println("Yessss");
			for (int i = 0; i < n; i++) {
				// List <String> title = new ArrayList<String>();
				Map<String, List<String>> title;
				title = titles.get(i);
				int x = title.size();
				System.out.println(x);

				if (x != 0) {
					// pour chaque mesure afficher dans la carte
					Set<String> measure_names = titles.get(i).keySet();
					int a = 0;
					for (String measure_name : measure_names) {
						contenuMaps_TitleString += measure_name;
						List<String> dim_members = titles.get(i).get(measure_name);
						int b = 0;
						for (String dim_member : dim_members) {
							if (b == 0) {
								contenuMaps_TitleString += "[";
							}

							contenuMaps_TitleString += dim_member;

							if (b != dim_members.size() - 1) {
								contenuMaps_TitleString += " - ";
							}

							if (b == dim_members.size() - 1) {
								contenuMaps_TitleString += "]";
							}

							b++;
						}
						if (a != measure_names.size() - 1) {
							contenuMaps_TitleString += ", ";
						}
						a++;
					}

					contenuMaps_TitleString += "\n";
				}
			}

			contenuMaps_TitleByte = contenuMaps_TitleString.getBytes();
			fos.write(contenuMaps_TitleByte);
			fos.flush();
			fos.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String AjouterNom(String nomIndic, String nom) {
		if (nomIndic.isEmpty()) {
			nomIndic = nom;
		} else {
			nomIndic = nomIndic + "_" + nom;
		}
		return nomIndic;
	}

	public static String FormaterString(String s1) {
		String sortie;

		String l[] = s1.split("[.]");
		sortie = l[l.length - 1];

		return sortie;
	}

}
