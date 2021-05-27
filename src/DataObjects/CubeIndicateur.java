/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DataObjects;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Clément
 */
public class CubeIndicateur {
    private String number ;
    private List<Measure> Mesure ; 
    private List<Tuple> Axis0;
    private List<Tuple> Axis1;

    public CubeIndicateur(String number) {
        this.number = number;
        this.Axis0 = new ArrayList<Tuple>() ; 
        this.Axis1 = new ArrayList<Tuple>() ; 
        this.Mesure = new ArrayList<Measure>() ; 
        
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void addMesure(String Mesure, int axe0, int axe1) {
        this.Mesure.add( new Measure(Mesure, axe0, axe1));
    }

    public void setAxis0(List<Tuple> Axis0) {
        this.Axis0 = Axis0;
    }

    public void setAxis1(List<Tuple> Axis1) {
        this.Axis1 = Axis1;
    }

    public String getNumber() {
        return number;
    }

    public String getMesure(int axe0, int axe1) {
        String valeur = "n/a" ; 
        for(Measure m : Mesure) {
            if ((m.getAxis0() == axe0) && (m.getAxis1() == axe1)){
                valeur = m.getValeur() ;
            }
        }
        return valeur;
    }

    public List<Tuple> getAxis0() {
        return Axis0;
    }

    public List<Tuple> getAxis1() {
        return Axis1;
    }
    
    public void addToAxis0(Tuple inTuple){
        this.Axis0.add(inTuple);
    }
    
    public void addToAxis1(Tuple inTuple){
        this.Axis1.add(inTuple);
    }
    
    public List<Tuple> getAxis1Tuples (int Number) {
        List<Tuple> ListeTemp = new ArrayList<Tuple>() ; 
        
        for(Tuple t : Axis1) {
            if (t.getNumber() == Number){
                ListeTemp.add(t);
            }
        }
        
        return ListeTemp;
    }
    
    public List<Tuple> getAxis0Tuples (int Number) {
        List<Tuple> ListeTemp = new ArrayList<Tuple>() ; 
        
        for(Tuple t : Axis0) {
            if (t.getNumber() == Number){
                ListeTemp.add(t);
            }
        }
        
        return ListeTemp;
    }
    
    public int getMaxTupleAxis0() {
        int max = -10 ; 
        
        for(Tuple t : Axis0) {
            if (t.getNumber() > max){
                max = t.getNumber(); 
            }
        }
        return max ; 
    }
    
    public int getMaxTupleAxis1() {
        int max = -10 ; 
        
        for(Tuple t : Axis1) {
            if (t.getNumber() > max){
                max = t.getNumber(); 
            }
        }
        return max ; 
    }
}
