/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DataObjects;

/**
 *
 * @author Clément
 */
public class Measure {
    private String Valeur ; 
    private int Axis0 ; 
    private int Axis1 ; 

    public Measure(String Valeur, int Axis0, int Axis1) {
        this.Valeur = Valeur;
        this.Axis0 = Axis0;
        this.Axis1 = Axis1;
    }

    public Measure() {
    }

    public String getValeur() {
        return Valeur;
    }

    public int getAxis0() {
        return Axis0;
    }

    public int getAxis1() {
        return Axis1;
    }

    public void setValeur(String Valeur) {
        this.Valeur = Valeur;
    }

    public void setAxis0(int Axis0) {
        this.Axis0 = Axis0;
    }

    public void setAxis1(int Axis1) {
        this.Axis1 = Axis1;
    }
    
    
}
