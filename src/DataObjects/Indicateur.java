/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package DataObjects;

import java.util.List;

/**
 *
 * @author Céline
 */
public class Indicateur {
    private String nom ;
    private double valeur ;

    private String spatial;
    private List<String> attributes;
    
    private String region;
    private String departement;
    
    /* rajout pour la position sur la carte */
    private String position;
    private Geometrie geom;

    


	public Indicateur() {
    }

    public Indicateur(String nom) {
        this.nom = nom;
    }
    
    public Indicateur(String nom, double valeur, String spatial, String position, String departement, String region, Geometrie geom) {
        this.nom = nom;
        this.valeur = valeur;
        this.spatial = spatial;
        this.position = position;
        this.departement = departement;
        this.region = region;
        this.geom = geom;
    }
    
    public Indicateur(String nom, double valeur, String spatial, String position, String departement, String region, Geometrie geom, List<String> attributes) {
        this.nom = nom;
        this.valeur = valeur;
        this.spatial = spatial;
        this.position = position;
        this.departement = departement;
        this.region = region;
        this.attributes = attributes;
        this.geom = geom;
    }

    public String getNom() {
        return nom;
    }

    public double getValeur() {
        return valeur;
    }
    
    public String getSpatial() {
        return spatial;
    }
    
    public String getMeasure() {
        return this.attributes.get(0);
    }

    public List<String> getAttributes() {
        return attributes;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setValeur(double valeur) {
        this.valeur = valeur;
    }
  
    public void setSpatial(String spatial) {
        this.spatial = spatial;
    }
    
    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }
    
    public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getDepartement() {
		return departement;
	}

	public void setDepartement(String departement) {
		this.departement = departement;
	}
	
	public Geometrie getGeom() {
		return geom;
	}

	public void setGeom(Geometrie geom) {
		this.geom = geom;
	}

    
}
