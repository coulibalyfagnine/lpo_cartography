/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Main_App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import DataObjects.CubeIndicateur;
import DataObjects.Indicateur;
import DataObjects.Tuple;

/**
 *
 * @author Clément
 */

public class Indicators {
	// MMMMMMMMMM
	public static String separator = "_._";
	public static String initial = "ind";

	// ces variables sont utiliser pour compter les nombre de membres de chaque
	// dimensiosn (sauf la dimension spatiale) et le nombre totales d'indicateur
	static Map<String, Double> dim_member_count;
	static Map<String, List<String>> dim_mem_done;
	static int NB_total = 0;

	public static String last_attribute = "";

	public static void execute(String CubeLog_file, String Indicateur_file) {

		try {
			List<CubeIndicateur> liste = new ArrayList<CubeIndicateur>();
			List<String> list_dim_done = new ArrayList<String>();
			List<String> list_dim_done_upd = new ArrayList<String>();

			RecupererDonnesCubes(CubeLog_file, liste, list_dim_done);

			String last_attribute = "";

			int x = 0;
			int lo = 0;
			for (String dime : list_dim_done) {
				// la première attribute (dimension) est la measure on ajoute cette attribute
				// par contre la dernière attribute (dimension) est l'attribute spatiale on ne
				// l'ajoute pas

				System.out.println(dime);
				if ((x == list_dim_done.size() - 1)) {
					last_attribute = dime;
					lo = x;
				}
				x++;
			}

			ecrireDonneesIndicateur(liste, Indicateur_file, list_dim_done);
			System.out.println("ecrireDonneesIndicateur done " + last_attribute);

			String[] dim_new = new String[list_dim_done.size() - 1];

			CheckIndicateur(Indicateur_file, list_dim_done, last_attribute, dim_new);
			list_dim_done = new ArrayList<String>();
			for (String dime : dim_new) {
//        	System.out.println(" nex list dim done : " + dime);
				list_dim_done.add(dime);
			}

			System.out.println(" fin check indicateur ");
			CheckOrderIndicateur(Indicateur_file, list_dim_done, last_attribute);
//        System.out.println("-------------------------------------------");


		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static void RecupererDonnesCubes(String CubeLog_file, List<CubeIndicateur> liste,
			List<String> list_dim_done) {
		org.jdom2.Document document;
		Element racine;
		// On crée une instance de SAXBuilder
		SAXBuilder sxb = new SAXBuilder();
		try {
			// On crée un nouveau document JDOM avec en argument le fichier XML
			// Le parsing est terminé ;)
			document = sxb.build(new File(CubeLog_file));

			// On initialise un nouvel élément racine avec l'élément racine du document.
			racine = document.getRootElement();

			// On crée une List contenant tous les noeuds "CubeIndicateur" fils de l'Element
			// racine
			List ListeCube = racine.getChildren("Cube");

			// On crée un Iterator sur notre liste
			Iterator i = ListeCube.iterator();
			while (i.hasNext()) { // Pour toutes les balises "CubeIndicateur" listées :

				Element courant = (Element) i.next();

				// On récupère la valeur de l'attribut "id" de la balise courante
				String id = courant.getAttributeValue("id");
				CubeIndicateur c = new CubeIndicateur(id); // On crée le cube

				{// Traitement des axes
					// On crée une liste des balises "Axis" fils de la balise courante
					List listeAxes = courant.getChildren("Axis");
					Iterator i2 = listeAxes.iterator(); // On crée un itérateur sur notre liste
					while (i2.hasNext()) {// Pour toutes les balises "Axis" listées :
						Element cour = (Element) i2.next();

						if ("Axis0".equals(cour.getAttributeValue("name"))) {// Si la balise à  pour attribut "name" la
																				// valeur "Axis0"

							// On récuprèe la liste des balises "Tuple" fils de la balise courante
							List listeTuple = cour.getChildren("Tuple");
							Iterator i3 = listeTuple.iterator(); // On crée un itérateur sur notre liste
							while (i3.hasNext()) {// Pour toutes les balises "Tuple" listées :
								Element curent = (Element) i3.next();

								// On récupère la valeur de l'attribut "number" de la balise courante
								int number = Integer.parseInt(curent.getAttributeValue("number"));
								// On crée le tuple correspondant au tuple du fichier
								Tuple temp = new Tuple(number, curent.getText());

								// On ajoute le tuple au cube sur l'axe 0
								c.addToAxis0(temp);

								// On récupère la valeur de l'attribut "Dimension" de la balise courante pour
								// trouver les attributs de l'indicateur
								String dim = curent.getAttributeValue("Dimension");

								int x = dim.indexOf(".");
								if (x != -1)
									dim = (dim.subSequence(0, x)).toString();

								if (!list_dim_done.contains(dim)) {
									list_dim_done.add(dim);
								}
							}
						} else if ("Axis1".equals(cour.getAttributeValue("name"))) {// Si la balise à  pour attribut
																					// "name" la valeur "Axis1"

							// On récuprèe la liste des balises "Tuple" fils de la balise courante
							List listeTuple = cour.getChildren("Tuple");
							Iterator i3 = listeTuple.iterator(); // On crée un itérateur sur notre liste
							while (i3.hasNext()) {// Pour toutes les balises "Tuple" listées :
								Element curent = (Element) i3.next();

								// On récupère la valeur de l'attribut "number" de la balise courante
								int number = Integer.parseInt(curent.getAttributeValue("number"));
								// On crée le tuple correspondant au tuple du fichier
								Tuple temp = new Tuple(number, curent.getText());

								// On ajoute le tuple au cube sur l'axe 1
								c.addToAxis1(temp);

								// On récupère la valeur de l'attribut "Dimension" de la balise courante pour
								// trouver les attributs de contexte de l'indicateur
								String dim = curent.getAttributeValue("Dimension");

								int x = dim.indexOf(".");
								if (x != -1)
									dim = (dim.subSequence(0, x)).toString();

								if (!list_dim_done.contains(dim)) {
									list_dim_done.add(dim);
								}
							}
						}
					}
				}
				{// traiter les objets Measure
					// On récuprèe la liste des balises "Measure" fils de la balise courante
					List listeMesure = courant.getChildren("Measure");
					Iterator i4 = listeMesure.iterator(); // On crée un itéreteur sur notre liste
					while (i4.hasNext()) {// Pour toutes les balises "Measure" listées :
						Element curent = (Element) i4.next();

						// On récupère la valeur de l'attribut "Ax0" et de "Ax1"
						int axe0 = Integer.parseInt(curent.getAttributeValue("Ax0"));
						int axe1 = Integer.parseInt(curent.getAttributeValue("Ax1"));
						// On récupère la valeur texte de la balise "value" fille de la balise courante
						String value = curent.getChild("Value").getText();

						// On ajoute la mesure au cube
						c.addMesure(value, axe0, axe1);
					}
				}

				// On ajoute le cube à  la liste des cubes du fichier
				liste.add(c);
			}

		} catch (JDOMException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	// ecriture du fichier indicateur.xml
	// mesure en colonne et spatial en ligne
	public static void ecrireDonneesIndicateur(List<CubeIndicateur> liste, String Indicateur_file,
			List<String> list_dim_done) {
		Element root = new Element("Indicateurs"); // On crée la racine du document : une balise Indicateurs
		org.jdom2.Document Doc = new Document(root); // On ajoute la racine au document virtuel
		List<String> liste_attribute = new ArrayList<String>();

		int x = 0;
		for (String dime : list_dim_done) {
			// la première attribute (dimension) est la measure on ajoute cette attribute
			// par contre la dernière attribute (dimension) est l'attribute spatiale on ne
			// l'ajoute pas
			if ((x != list_dim_done.size() - 1)) {
				Attribute attribute = new Attribute("attribute" + x, dime);
				root.setAttribute(attribute);
			}
			x++;
		}

		for (CubeIndicateur c : liste) { // Pour tous les cubes de la liste

			String nomIndic = new String();
			int axe0Max = c.getMaxTupleAxis0(); // On récupère la valeur max des tuples de l'axe 0
			int axe1Max = c.getMaxTupleAxis1(); // On récupère la valeur max des tuples de l'axe 1

			int taille1;

			for (int i = 1; i <= axe0Max; i++) { // Boucle de 1 à  axe0Max
				for (int j = 1; j <= axe1Max; j++) { // Boucle de 1 à  axe1Max
					nomIndic = "";
					liste_attribute.clear();
					// On récupère les listes de tuples correspondant des axes 0 et 1
					List<Tuple> tuples0 = new ArrayList<Tuple>(c.getAxis0Tuples(i));
					List<Tuple> tuples1 = new ArrayList<Tuple>(c.getAxis1Tuples(j));

					// On récupère la taille du tuple de l'axe 1
					taille1 = tuples1.size();

					for (Tuple t : tuples0) {// récupération tuples Axe0 dans la variable nomIndic
						nomIndic = AjouterNom(nomIndic, t.getName());
						liste_attribute.add(FormaterString(t.getName()));
					}
					for (int k = 0; k < taille1 - 1; k++) {// Pour tous les tuples de l'axe 1 sauf le dernier, on
															// récupère le nom dans la variable nomIndic
						String nom = TupleNumeroName(tuples1, k);
						nomIndic = AjouterNom(nomIndic, nom);
						liste_attribute.add(FormaterString(nom));
					}
					// On récupère la valeur de la loclisation spatiale du couple de tuple en cours
					String local = new String();
					local = tuples1.get(taille1 - 1).getName();

					// On récupère la valeur correspondante au tuple et on l'enregistre dans un
					// string
					String valString = c.getMesure(i, j);
					valString = valString.replaceAll(",", ".");
					if (!"n/a".equals(valString)) {
						// On crée une balise Indicateur et on l'ajoute à  la balise racine.
						Element indicateur = new Element("Indicateur");
						root.addContent(indicateur);

						// On crée un attribut "name" contenant le nom de l'indicateur et on ajoute
						// l'attribut à  la balise Indicateur
						Attribute classe = new Attribute("name", nomIndic);
						indicateur.setAttribute(classe);
						int k = 0;
						for (String att : liste_attribute) {
							{
								Attribute attribute = new Attribute("attribute" + k, att);
								indicateur.setAttribute(attribute);
							}
							k++;
						}

						// On crée la balise value et on lui ajoute le texte corespondant
						Element value = new Element("Value");
						String val_numeric_String = valString.replaceAll("[^\\d.]", "");
						value.setText(val_numeric_String);
						// On crée une balise spatial et on lui ajoute le texte correspondant.
						Element spatial = new Element("Spatial");
						spatial.setText(FormaterString(local));

						// On ajoute les balises value et spatial à  la balise indicateur
						indicateur.addContent(value);
						indicateur.addContent(spatial);
					}

				}
			}

		}
		// CountMemberCube(CubeLog, root);
		enregistreDocumentXML(Indicateur_file, Doc);
	}

//   methode de vérification du fichier indicateur.xml écrit
	// OBJECTIF : réécrire le fichier dans le cas où on a d'autre dimension non
	// spatiale en ligne
	public static void CheckIndicateur(String Indicateur_file, List<String> list_dim_done, String last_attribute,
			String[] dim_new) {

		org.jdom2.Document document;
		Element racine;

		Element root = new Element("Indicateurs");
		org.jdom2.Document Doc = new Document(root);
		List<String> liste_attribute = new ArrayList<String>();

		// On crÃ©e une instance de SAXBuilder
		SAXBuilder sxb = new SAXBuilder();
		try {
			// On crÃ©e un nouveau document JDOM avec en argument le fichier XML
			// Le parsing est terminÃ© ;)
			document = sxb.build(new File(Indicateur_file)); // ou fichier Resultat

			// On initialise un nouvel Ã©lÃ©ment racine avec l'Ã©lÃ©ment racine du document.
			racine = document.getRootElement();

			// On crée une List contenant tous les attribute "attribute(i)" de l'Element
			// racine
			List ListeAttribute = racine.getAttributes();
			Iterator att = ListeAttribute.iterator();

			// Récupérer valeur attribute = localisation dimension spatiale
			// RECUPERER POSTION DE LOCALICATION LOC, sa position va nous permettre de
			// deplacer vers la fin des attributs dans indicateurs.xml
			int loc = 1; // pour ne pas recuperer la mesure
			while (att.hasNext()) {
				Attribute courant_att = (Attribute) att.next();
				if (courant_att.getValue().equals("localisation")) {
					break;
				}
				loc++;
			}

			// active si la dimension spatiale n'est pas à la fin des attributs des
			// dimensions utilisées.
			if (loc - 1 != ListeAttribute.size()) {
				int x = 0;
				int cp = 0;
//				   List<String> templisdim = null;
//					Collections.copy(templisdim, list_dim_done);
//			    	System.out.println( " templisdim :" + list_dim_done);
				for (String dime : list_dim_done) {
					// la première attribute (dimension) est la measure on ajoute cette attribute
					// par contre la dernière attribute (dimension) est l'attribute spatiale on ne
					// l'ajoute pas
					Attribute attribute = null;

					if (x != loc - 1) {

						if ((x != list_dim_done.size() - 1)) {
							attribute = new Attribute("attribute" + (cp), dime);
							dim_new[cp] = dime;

							
						} else if (x == list_dim_done.size() - 1) {
							attribute = new Attribute("attribute" + (cp), last_attribute);
							dim_new[cp] = last_attribute;

							System.out.println(" attribute last : " + attribute); // dimention spatial

						}
						root.setAttribute(attribute);
						cp++;
					}

					x++;
				}
				// LECTURE FICHIER XML A CORRIGER
				List list = racine.getChildren("Indicateur");
				ArrayList<String> ind_name = new ArrayList<String>();

				for (int i = 0; i < list.size(); i++) {

					int tt = 0;
					Element node = (Element) list.get(i);

					List ListeAttribute0 = node.getAttributes();

					Iterator att0 = ListeAttribute0.iterator();
					tt = 0;
					Attribute courant_att0 = null;

					while (att0.hasNext()) {
						courant_att0 = (Attribute) att0.next();

						if (tt == 0) {

							if (!ind_name.contains(courant_att0.getValue())) {
								ind_name.add(courant_att0.getValue());
							}
						}
						tt++;
					}
				}
				for (int i = 0; i < list.size(); i++) {

					int loc1 = 0;
					Element node = (Element) list.get(i);

					List ListeAttribute1 = node.getAttributes();

					Iterator att1 = ListeAttribute1.iterator();
					loc1 = 0;
					Attribute courant_att = null;
					Attribute classe = null;
					Attribute attribute = null;
					Element indicateur = new Element("Indicateur");
					root.addContent(indicateur);

					Element spatial = null;
					Element value = null;
					String attr = "";
					while (att1.hasNext()) {
						courant_att = (Attribute) att1.next();

						if (loc1 == loc) {
							spatial = new Element("Spatial");
							spatial.setText(courant_att.getValue());

						} else if (loc1 == list_dim_done.size() - 1) {

						} else if (loc1 == 0) {
							// modification tmp_name indicateur.xml

							String[] tmp_name = courant_att.getValue().split("_._");

							value = new Element("Value");
							value.setText(node.getChildText("Value"));
							String tmp_name1 = null;
							tmp_name1 = node.getChildText("Spatial");// changement valeur dernier dimension dans balise
																		// spatiale et 2eme valeur dans ind_._XXX
							String str_ind = "";
							for (int j = 0; j < tmp_name.length; j++) {
								if (j == 0) {
									str_ind = tmp_name[j];

								} else if (j == loc) {
								}

								else {
									str_ind = str_ind + "_._" + tmp_name[j];

								}
							}
							str_ind = str_ind + "_._" + tmp_name1;

							classe = new Attribute("name", str_ind);

							indicateur.setAttribute(classe);
							int j = 0;

							for (String retval : str_ind.split("_._")) {
								if (j != 0) {
									attribute = new Attribute("attribute" + (j - 1), retval);

									indicateur.setAttribute(attribute);
								}

								j++;
							}
						} else {
						}
						loc1++;
					}

					indicateur.addContent(value);
					indicateur.addContent(spatial);

				}

				enregistreDocumentXML(Indicateur_file, Doc);
			}

		} catch (

		JDOMException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}

	}

	@SuppressWarnings("null")
	public static void CheckOrderIndicateur(String Indicateur_file, List<String> list_dim_done, String last_attribute) {
		// Yassine (adaptation du fichier Indicateur.xml)
		// System.out.println( "last dimnew " + dim_new + " tte");
		org.jdom2.Document document;
		Element racine;

		Element root = new Element("Indicateurs");
		org.jdom2.Document Doc = new Document(root);

		List<String> liste_attribute = new ArrayList<String>();

		// On crÃ©e une instance de SAXBuilder
		SAXBuilder sxb = new SAXBuilder();
		try {
			// On crÃ©e un nouveau document JDOM avec en argument le fichier XML
			// Le parsing est terminÃ© ;)
			document = sxb.build(new File(Indicateur_file)); // ou fichier Resultat

			// On initialise un nouvel Ã©lÃ©ment racine avec l'Ã©lÃ©ment racine du document.
			racine = document.getRootElement();

			// On crée une List contenant tous les attribute "attribute(i)" de l'Element
			// racine
			List ListeAttribute = racine.getAttributes();
			Iterator att = ListeAttribute.iterator();

			// Récupérer valeur attribute = localisation dimension spatiale
			// RECUPERER POSTION DE LOCALICATION LOC

			// LECTURE FICHIER XML A CORRIGER
			List list = racine.getChildren("Indicateur");

//				System.out.println(" recupérer list first attrib");
			ArrayList<String> ind_name = new ArrayList<String>();

			for (int i = 0; i < list.size(); i++) {

				int tt = 0;
				Element node = (Element) list.get(i);

				List ListeAttribute0 = node.getAttributes();
				Iterator att0 = ListeAttribute0.iterator();
				tt = 0;
				Attribute courant_att0 = null;
//						

				while (att0.hasNext()) {
					courant_att0 = (Attribute) att0.next();

					if (tt == 0) {
//								System.out.println(" first attrib " + courant_att0.getValue());

						if (!ind_name.contains(courant_att0.getValue())) {
							ind_name.add(courant_att0.getValue());
						}

					}
//							else {
//								System.out.println(" attrib " + courant_att.getValue());
//							}
					tt++;
				}

			}

			int x = 0;

//				for (String  dime : list_dim_done) {
//					System.out.println(" dimension done " + dime);
//				}

//				int x = 0;
			int nb_dim = 0;
			for (String dime : list_dim_done) {
				// la première attribute (dimension) est la measure on ajoute cette attribute
				// par contre la dernière attribute (dimension) est l'attribute spatiale on ne
				// l'ajoute pas
				if (!dime.equals("localisation")) {
					/*
					 * if ((x != list_dim_done.size()-1)){ Attribute attribute = new
					 * Attribute("attribute" +x ,dime); root.setAttribute(attribute); }
					 */
					Attribute attribute = new Attribute("attribute" + x, dime);
					root.setAttribute(attribute);

					x++;

				}
			}

			nb_dim = x - 1;
			System.out.println();

			// RECUPERATION DES COMBINAISONS VALEURS DES DIMENSIONS
			String[] tmp = new String[nb_dim];
			ArrayList<String> ind_ = new ArrayList<String>();

			if (nb_dim == ind_name.size()) {
				ind_ = ind_name;
			} else {
				for (int z = 0; z < ind_name.size(); z++) {
					String stmp = "";
					tmp = ind_name.get(z).split("_._");

					for (int i = 2; i < tmp.length; i++) {
//						System.out.print( " " + tmp[i] + " -- " + nb_dim);
						if (i == 2) {
							stmp = "ind" + "_._" + tmp[1] + "_._" + tmp[i];
						} else {
							stmp = stmp + "_._" + tmp[i];

						}

					}
					ind_.add(stmp);
//					System.out.println();

				}
			}
			System.out.println();
			Collections.sort(ind_);
			System.out.println(ind_);
			System.out.println();
			// ORDONNACEMENT DU FICHIER INDICATEUR PAR indicator_name
			for (int z = 0; z < ind_.size(); z++) {
//					System.out.println(ind_.get(z));

//					System.out.println(" new "  +ind_.get(z));

				// Ecrire tous les indicateurs avec le même indicateur name

				for (int i = 0; i < list.size(); i++) {

					Element node = (Element) list.get(i);

					List ListeAttribute1 = node.getAttributes();
					Iterator att1 = ListeAttribute1.iterator();

					Attribute courant_att = null;
					Attribute classe = null;
					Attribute attribute = null;

					// création indicateur

					int atr = 0;
					while (att1.hasNext()) {
						courant_att = (Attribute) att1.next();
						if (courant_att.getValue().equals(ind_.get(z))) {

							break;
						}

						atr++;

					}
					if (atr == 0) {
						Element indicateur = new Element("Indicateur");
						root.addContent(indicateur);

						Element spatial = null;
						Element value = null;
						String attr = "";
						int atr1 = 0;
						int alt = 0;
						Iterator att2 = ListeAttribute1.iterator();
						while (att2.hasNext()) {
							courant_att = (Attribute) att2.next();

							if (atr1 == 0 && alt == 0) {
								classe = new Attribute("name", courant_att.getValue());
								indicateur.setAttribute(classe);
								alt++;
							} else {
								attribute = new Attribute("attribute" + atr1, courant_att.getValue());
								indicateur.setAttribute(attribute);
								atr1++;
							}

						}

						spatial = new Element("Spatial");
						spatial.setText(node.getChildText("Spatial"));

						value = new Element("Value");
						value.setText(node.getChildText("Value"));

						indicateur.addContent(value);
						indicateur.addContent(spatial);

					}
							
				}

				// ------------------------------------------------

			}
			enregistreDocumentXML(Indicateur_file, Doc);
			System.out.println();

			System.out.println(" end recuperatino");

		} catch (JDOMException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}

	}

	static void ecrireDonneesIndicateurUpdate(List<CubeIndicateur> liste, String Indicateur_file,
			List<String> list_dim_done, String last_attribute) {
//   static void ecrireDonneesIndicateurUpdate(List<CubeIndicateur> liste, String Indicateur_file,  List<String> list_dim_done, String last_attribute){
		Element root = new Element("Indicateurs"); // On crée la racine du document : une balise Indicateurs
		org.jdom2.Document Doc = new Document(root); // On ajoute la racine au document virtuel
		List<String> liste_attribute = new ArrayList<String>();

		int x = 0;
		for (String dime : list_dim_done) {
			// la première attribute (dimension) est la measure on ajoute cette attribute
			// par contre la dernière attribute (dimension) est l'attribute spatiale on ne
			// l'ajoute pas
			if ((x != list_dim_done.size() - 1)) {
				Attribute attribute = new Attribute("attribute" + x, dime);
				root.setAttribute(attribute);
			}
			if ((x == list_dim_done.size() - 1)) {
//         	 Attribute attribute = new Attribute("attribute" +x ,dime);
//         	 root.setAttribute(attribute); 
				last_attribute = dime;
			}
//    	   Attribute attribute = new Attribute("attribute" +x ,dime);
//       	 root.setAttribute(attribute);
			x++;
		}

		for (CubeIndicateur c : liste) { // Pour tous les cubes de la liste

			String nomIndic = new String();
			int axe0Max = c.getMaxTupleAxis0(); // On récupère la valeur max des tuples de l'axe 0
			int axe1Max = c.getMaxTupleAxis1(); // On récupère la valeur max des tuples de l'axe 1

			int taille1;

			for (int i = 1; i <= axe0Max; i++) { // Boucle de 1 à  axe0Max
				for (int j = 1; j <= axe1Max; j++) { // Boucle de 1 à  axe1Max
					nomIndic = "";
					liste_attribute.clear();
					// On récupère les listes de tuples correspondant des axes 0 et 1
					List<Tuple> tuples0 = new ArrayList<Tuple>(c.getAxis0Tuples(i));
					List<Tuple> tuples1 = new ArrayList<Tuple>(c.getAxis1Tuples(j));

					// On récupère la taille du tuple de l'axe 1
					taille1 = tuples1.size();

					for (Tuple t : tuples0) {// récupération tuples Axe0 dans la variable nomIndic
												// System.out.println("tuples Axe0 name" + t.getName());
						nomIndic = AjouterNom(nomIndic, t.getName());
						liste_attribute.add(FormaterString(t.getName()));
					}
					for (int k = 0; k < taille1 - 1; k++) {// Pour tous les tuples de l'axe 1 sauf le dernier, on
															// récupère le nom dans la variable nomIndic
						String nom = TupleNumeroName(tuples1, k);
						// System.out.println("tuples Axe0 name" + nom);
						nomIndic = AjouterNom(nomIndic, nom);
						liste_attribute.add(FormaterString(nom));
					}
					// On récupère la valeur de la loclisation spatiale du couple de tuple en cours
					String local = new String();
					local = tuples1.get(taille1 - 1).getName();

					// On récupère la valeur correspondante au tuple et on l'enregistre dans un
					// string
					String valString = c.getMesure(i, j);
					valString = valString.replaceAll(",", ".");
					if (!"n/a".equals(valString)) {
						// On crée une balise Indicateur et on l'ajoute à  la balise racine.
						Element indicateur = new Element("Indicateur");
						root.addContent(indicateur);

						// On crée un attribut "name" contenant le nom de l'indicateur et on ajoute
						// l'attribut à  la balise Indicateur
						Attribute classe = new Attribute("name", nomIndic);
						indicateur.setAttribute(classe);
						int k = 0;
						for (String att : liste_attribute) {
							{
								Attribute attribute = new Attribute("attribute" + k, att);
								indicateur.setAttribute(attribute);
							}
							k++;
						}

						// On crée la balise value et on lui ajoute le texte corespondant
						Element value = new Element("Value");
//                         System.out.println(" string value indicator " + valString);
						String val_numeric_String = valString.replaceAll("[^\\d.]", "");
						value.setText(val_numeric_String);
						// On crée une balise spatial et on lui ajoute le texte correspondant.
						Element spatial = new Element("Spatial");
						// System.out.println("local :" + local);
						spatial.setText(FormaterString(local));

						// On ajoute les balises value et spatial à  la balise indicateur
						indicateur.addContent(value);
						indicateur.addContent(spatial);
					}

					// System.out.println("Tuple "+i+"/"+j+": "+nomIndic+" et localisation :
					// "+local+" et valeur : "+valString);
				}
			}

		}
		// CountMemberCube(CubeLog, root);
		enregistreDocumentXML(Indicateur_file, Doc);
	}

	static void RecupererDonnesIndicateur(String Indicateur_file, String s_queryTempFile, List<Indicateur> liste,
			List<String> liste_dimensions) {
		org.jdom2.Document document;
		Element racine;

		// On crÃ©e une instance de SAXBuilder
		SAXBuilder sxb = new SAXBuilder();
		try {
			// On crÃ©e un nouveau document JDOM avec en argument le fichier XML
			// Le parsing est terminÃ© ;)
			document = sxb.build(new File(Indicateur_file)); // ou fichier Resultat

			// On initialise un nouvel Ã©lÃ©ment racine avec l'Ã©lÃ©ment racine du document.
			racine = document.getRootElement();

			// On crée une List contenant tous les attribute "attribute(i)" de l'Element
			// racine
			List ListeAttribute = racine.getAttributes();
			Iterator att = ListeAttribute.iterator();
			while (att.hasNext()) {
				Attribute courant_att = (Attribute) att.next();
				// System.out.println(courant_att.getValue());
				liste_dimensions.add(courant_att.getValue());
			}

			// pour compter le nombre de membres de chaque dimanension et le nombre total
			// [nDi & nb]
			// a fin de verifier dans la suite s'il y a une règle d'affichage qui correspond
			// au résultat de l'analyse courant
			dim_member_count = new HashMap<String, Double>();
			dim_mem_done = new HashMap<String, List<String>>();

			// on ajoute les dimesnions
			for (String dimension : liste_dimensions) {
				dim_member_count.put(dimension, 0.0);
				dim_mem_done.put(dimension, new ArrayList<String>());
			}
			/////////////////////////////////////////////////////////

			// On crée une List contenant tous les noeuds "indicateur" de l'Element racine
			List ListeIndicateur = racine.getChildren("Indicateur");

			// On crÃ©e un Iterator sur notre liste
			Iterator i = ListeIndicateur.iterator();

			while (i.hasNext()) {
				Element courant = (Element) i.next();
				String nom = courant.getAttributeValue("name");
				Indicateur ind = new Indicateur(nom);

				{// Traitement de la valeur
//					System.out.println(courant.getChild("Value").getText());

					double valeur = Double.parseDouble(courant.getChild("Value").getText());
					ind.setValeur(valeur);
				}

				{// Traitement du spatial
					String spatial = courant.getChild("Spatial").getText();
					ind.setSpatial(spatial);
				}

				{// Traitement des attributes
					List<String> list_att = new ArrayList<String>();
					List ListeAttribute2 = courant.getAttributes();
					Iterator att2 = ListeAttribute2.iterator();
					while (att2.hasNext()) {
						Attribute courant_at = (Attribute) att2.next();
						// on ajoute tous les attributs de contexte sauf le nom
						if (!courant_at.getName().toUpperCase().equals(("name").toUpperCase())) {
							list_att.add(courant_at.getValue());
						}
					}
					ind.setAttributes(list_att);
				}

				liste.add(ind);

				// pour compter le nombre de membres de chaque dimanension et le nombre total
				// [nDi & nb]
				List<String> dimesnions_membres = new ArrayList<String>();
				int j = 0;
				dimesnions_membres = ind.getAttributes();
				for (String dim_mem : dimesnions_membres) {
					if (!dim_mem_done.get(liste_dimensions.get(j)).contains(dim_mem)) {
						dim_mem_done.get(liste_dimensions.get(j)).add(dim_mem);
						double c = dim_member_count.get(liste_dimensions.get(j));
						dim_member_count.put(liste_dimensions.get(j), c + 1);
					}
					j++;
				}
				////////////////////////////////////////////////////
			}

			// pour compter le nombre de membres de chaque dimanension et le nombre total
			// [nDi & nb]
			NB_total = 1;
			for (String dimension : liste_dimensions) {
				NB_total = NB_total * dim_member_count.get(dimension).intValue();
			}
			System.out.println("NB_total : " + NB_total);
			System.out.println("dim_member_count : " + dim_member_count);
			System.out.println("dim_mem_done : " + dim_mem_done);
			////////////////////////////////////////////////////////
		} catch (JDOMException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}
	}


	static void enregistreDocumentXML(String fichier, Document doc) {
		try {
			// On utilise ici un affichage classique avec getPrettyFormat()
			XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
			// Remarquez qu'il suffit simplement de créer une instance de FileOutputStream
			// avec en argument le nom du fichier pour effectuer la sérialisation.
			sortie.output(doc, new FileOutputStream(fichier));
		} catch (java.io.IOException e) {
		}
	}

	static String TupleNumeroName(List<Tuple> liste, int numero) {
		Tuple temp = new Tuple();
		temp = liste.get(numero);
		return temp.getName();
	}

	private static String AjouterNom(String nomIndic, String nom) {
		nom = FormaterString(nom);
		if (nomIndic.isEmpty()) {
			nomIndic = initial + separator + nom;
			// nomIndic = initial+nom;
			// nomIndic = separator+nom;
		} else {
			nomIndic = nomIndic + separator + nom;
			// nomIndic = nomIndic+nom;
		}
		return nomIndic;
	}

	public static String FormaterString(String s1) {
		String sortie;

		String l[] = s1.split("].");
		sortie = l[l.length - 1];

		return sortie;
	}
}
