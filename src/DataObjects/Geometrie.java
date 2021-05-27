/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package DataObjects;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
/**
 *
 * @author Céline
 */
public class Geometrie {
    private int ID;
    private String type;
    private String nom;
    private String niveau;
    private String typeCollect;
    private int count;
    private int countElem;

	private int geom;
    private List<Point> listePoint ;
    
    

	// ajoute anael
    private List<List <Point> > listePolygon;
    private Point centroid;
    private JSONObject geoJson;
    private JSONObject centroidJson;
    private JSONObject carre;
    private JSONObject geoJsonCollect;

    public Geometrie() {
      listePoint = new ArrayList<Point>() ; 
      //ajout anael
      listePolygon = new ArrayList<List<Point> >();
      geoJson = new JSONObject();
    }
    
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public int getCountElem() {
		return countElem;
	}

	public void setCountElem(int countElem) {
		this.countElem = countElem;
	}

    public int getID() {
        return ID;
    }

    public String getType() {
        return type;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }
    
    public String getNiveau() {
		return niveau;
	}

	public void setNiveau(String niveau) {
		this.niveau = niveau;
	}

    public int getGeom() {
        return geom;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setGeom(int geom) {
        this.geom = geom;
    }

  public void setListePoint(List<Point> listePoint) {
    this.listePoint = listePoint;
  }
  
  public void AddPoint(Point point) {
    this.listePoint.add(point);
  }

  public List<Point> getListePoint() {
    return listePoint;
  }

  // ajout anael
	public List<List<Point>> getListePolygon() {
		return listePolygon;
	}
	
	public void addpolygon(List<Point> polygon) {
	    this.listePolygon.add(polygon);
	  }
	
	public void setListePolygon(List<List<Point>> listePolygon) {
		this.listePolygon = listePolygon;
	}

	public Point getCentroid() {
		return centroid;
	}

	public String getTypeCollect() {
		return typeCollect;
	}

	public void setTypeCollect(String typeCollect) {
		this.typeCollect = typeCollect;
	}

	public JSONObject getGeoJsonCollect() {
		return geoJsonCollect;
	}

	public void setGeoJsonCollect(JSONObject geoJsonCollect) {
		this.geoJsonCollect = geoJsonCollect;
	}

	public void setCentroid(Point centroid) {
		this.centroid = centroid;
	}
    
    public JSONObject getGeoJson() {
    	return geoJson;
    }
    
    public void setGeoJson(JSONObject geoDonnee) {
    	this.geoJson = geoDonnee;    	
    }
    
    public JSONObject getCentroidJson() {
    	return centroidJson;
    }
    
    public void setCentroidJson(JSONObject centroidJson) {
    	this.centroidJson = centroidJson;    	
    }    
    
    public JSONObject getCarre() {
    	return carre;
    }
    
    public void setCarre(JSONObject carre) {
    	this.carre = carre;    	
    }    
    
}
