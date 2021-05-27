package Main_App;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import DataObjects.Geometrie;
import DataObjects.Indicateur;

public class JsonData {
	
	@SuppressWarnings("unchecked")
	public static JSONObject get_jsondata (JSONObject geoJsonData, Indicateur ind, int i,  List<String> liste_dimensions, String label, List<String>dimension_value, HashMap<String, Geometrie> geometry, 
			int testNiveau, String barGeoJson,
			Map<String, Integer> features_id, JSONArray features, String check) throws ParseException {
		
//		System.out.println(" debut jsondata ");
		JSONObject jsondata = new JSONObject();
		
//		JSONObject jsondata = new JSONObject();
		jsondata.put("type", "Feature");

		JSONObject jsondata_properties = new JSONObject();
		jsondata_properties.put("ID", i);
		
		//pour identifier le ID de feature
		String feature_identifiant = ind.getSpatial();
		
		//pour ajouter les membres des dimensions
		//On crée une List contenant tous les attribute "attribute(i)" de l'Element racine
		List <String> dimesnions_membres = new ArrayList<String>();
		int j=0;
		dimesnions_membres = ind.getAttributes();

		label = "";
//		System.out.println(" debut parcours liste_dimension " + liste_dimensions.toString());
		int ct_dim = 0;
		for (String dimension : liste_dimensions){			
			//pour identifier le ID de feature on ajouter les valeurs de différents membres de dimensions (PAS de mesures)
			// si les valeurs sont identiques donc c'est le même feature. alors, il faut ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans le même feature 
			if (! dimension.toLowerCase().equals("Measures".toLowerCase())) {
				feature_identifiant = feature_identifiant + "_" + dimesnions_membres.get(j); 
//				System.out.println("brrrrr " + dimesnions_membres.get(j));
				if (!dimension_value.contains(dimesnions_membres.get(j))) {
						dimension_value.add(dimesnions_membres.get(j));					
				}
				if (ct_dim == 0) {
					label = dimesnions_membres.get(j);
					ct_dim++;
				}
				else {
					label = label+"_"+dimesnions_membres.get(j);
				}
				if (check.equals("region")) {
					System.out.println(" list dimensions :: " + dimension + " --- " + dimesnions_membres.get(j));
				}
				
				jsondata_properties.put(dimension, dimesnions_membres.get(j));
			}
			j++;
		}
		// fin d'ajout des memebres des dimensions
		//jsondata_properties.put("Nom", ind.getNom());
		jsondata_properties.put("_Label", label);
		jsondata_properties.put(ind.getMeasure(), ind.getValeur());
		jsondata_properties.put("_Location", ind.getSpatial());
		jsondata_properties.put("_niveau", geometry.get(ind.getSpatial()).getNiveau());

		
//		System.out.println(" debut recuperation centroid ");
		JSONObject jo = geometry.get(ind.getSpatial()).getCentroidJson();
		
		Iterator itr = jo.values().iterator();
		
		while(itr.hasNext()) {
	         Object element = itr.next();
	         
	         if (element.toString().length() > 5) {

	        	 String[] val =  element.toString().split(",");
	        	 
	        	 float lon = Float.parseFloat(val[0].replace("[", ""));
	        	 float lat = Float.parseFloat(val[1].replace("]", ""));
	        	 

	        	 jsondata_properties.put("long_centro", lon);
	        	 jsondata_properties.put("lat_centro", lat);
	        	 
	        	 
	         }
	      }
		
		jsondata.put("properties", jsondata_properties);
		

		if (testNiveau > 1) {

			JSONParser jsonParser = new JSONParser();
			jsondata.put("geometry",jsonParser.parse(barGeoJson));	
		}
		else
		{			

			JSONParser jsonParser = new JSONParser();
			jsondata.put("geometry",jsonParser.parse(barGeoJson));	
		}
		
//		System.out.println(" debut identification feature ");
		System.out.println(" valeur de i dans jsondata : " + i + " valeur de feature : " +  feature_identifiant);
		//pour identifier le ID de feature
		if (! features_id.containsKey(feature_identifiant)) {
			features_id.put(feature_identifiant, i);
			features.add(features_id.get(feature_identifiant), jsondata);
//			i++;
		}
		else {
			System.out.println(" non identifier : " + feature_identifiant);
			int id = features_id.get(feature_identifiant);
			System.out.println(" non identifier id new : " + id);
			JSONObject old_feature = new JSONObject();
			old_feature = (JSONObject) features.get(id);
			JSONObject properties = new JSONObject();
			
			// add les mesures déjà trouvées
			properties.putAll(((JSONObject)old_feature.get("properties")));
			
			// add la nouvelle mesure
			properties.putAll(((JSONObject)jsondata.get("properties")));
			
			jsondata.put("properties", properties);
			features.set(id, jsondata);
				
		}
		
		
		return jsondata;
		
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void get_jsondata2 (JSONObject geoJsonData, JSONObject jsondata,
			Indicateur ind, int i,  List<String> liste_dimensions, String label, List<String>dimension_value, HashMap<String, Geometrie> geometry, 
			int testNiveau, String barGeoJson,
			Map<String, Integer> features_id, JSONArray features, String check) throws ParseException {
		
//		System.out.println(" debut jsondata ");
		jsondata = new JSONObject();
		
//		JSONObject jsondata = new JSONObject();
		jsondata.put("type", "Feature");

		JSONObject jsondata_properties = new JSONObject();
		jsondata_properties.put("ID", i);
		
		//pour identifier le ID de feature
		String feature_identifiant = ind.getSpatial();
		
		//pour ajouter les membres des dimensions
		//On crée une List contenant tous les attribute "attribute(i)" de l'Element racine
		List <String> dimesnions_membres = new ArrayList<String>();
		int j=0;
		dimesnions_membres = ind.getAttributes();

		label = "";
//		System.out.println(" debut parcours liste_dimension " + liste_dimensions.toString());
		int ct_dim = 0;
		for (String dimension : liste_dimensions){			
			//pour identifier le ID de feature on ajouter les valeurs de différents membres de dimensions (PAS de mesures)
			// si les valeurs sont identiques donc c'est le même feature. alors, il faut ajouter toutes les indiquateurs (mesures) de ces valeurs de dimensions dans le même feature 
			if (! dimension.toLowerCase().equals("Measures".toLowerCase())) {
				feature_identifiant = feature_identifiant + "_" + dimesnions_membres.get(j); 
//				System.out.println("brrrrr " + dimesnions_membres.get(j));
				if (!dimension_value.contains(dimesnions_membres.get(j))) {
						dimension_value.add(dimesnions_membres.get(j));					
				}
				if (ct_dim == 0) {
					label = dimesnions_membres.get(j);
					ct_dim++;
				}
				else {
					label = label+"_"+dimesnions_membres.get(j);
				}
				if (check.equals("region")) {
					System.out.println(" list dimensions :: " + dimension + " --- " + dimesnions_membres.get(j));
				}
				
				jsondata_properties.put(dimension, dimesnions_membres.get(j));
			}
			j++;
		}
		// fin d'ajout des memebres des dimensions
		//jsondata_properties.put("Nom", ind.getNom());
		jsondata_properties.put("_Label", label);
		jsondata_properties.put(ind.getMeasure(), ind.getValeur());
		jsondata_properties.put("_Location", ind.getSpatial());
		jsondata_properties.put("_niveau", geometry.get(ind.getSpatial()).getNiveau());

		
//		System.out.println(" debut recuperation centroid ");
		JSONObject jo = geometry.get(ind.getSpatial()).getCentroidJson();
		
		Iterator itr = jo.values().iterator();
		
		while(itr.hasNext()) {
	         Object element = itr.next();
	         
	         if (element.toString().length() > 5) {

	        	 String[] val =  element.toString().split(",");
	        	 
	        	 float lon = Float.parseFloat(val[0].replace("[", ""));
	        	 float lat = Float.parseFloat(val[1].replace("]", ""));
	        	 

	        	 jsondata_properties.put("long_centro", lon);
	        	 jsondata_properties.put("lat_centro", lat);
	        	 
	        	 
	         }
	      }
		
		jsondata.put("properties", jsondata_properties);
		

		if (testNiveau > 1) {

			JSONParser jsonParser = new JSONParser();
			jsondata.put("geometry",jsonParser.parse(barGeoJson));	
		}
		else
		{			

			JSONParser jsonParser = new JSONParser();
			jsondata.put("geometry",jsonParser.parse(barGeoJson));	
		}
		
//		System.out.println(" debut identification feature ");
		System.out.println(" valeur de i dans jsondata : " + i + " valeur de feature : " +  feature_identifiant);
		//pour identifier le ID de feature
		if (! features_id.containsKey(feature_identifiant)) {
			System.out.println(" insertion ");
			features_id.put(feature_identifiant, i);
			features.add(features_id.get(feature_identifiant), jsondata);
			i++;
			System.out.println(" insertion " + i);
		}
		else {
			System.out.println(" non identifier : " + feature_identifiant);
			int id = features_id.get(feature_identifiant);
			System.out.println(" non identifier id new : " + id);
			JSONObject old_feature = new JSONObject();
			old_feature = (JSONObject) features.get(id);
			JSONObject properties = new JSONObject();
			
			// add les mesures déjà trouvées
			properties.putAll(((JSONObject)old_feature.get("properties")));
			
			// add la nouvelle mesure
			properties.putAll(((JSONObject)jsondata.get("properties")));
			
			jsondata.put("properties", properties);
			features.set(id, jsondata);
				
		}
		
		
//		return jsondata;
		
	}

}
