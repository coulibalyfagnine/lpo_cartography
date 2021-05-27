/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DataObjects;

/**
 *
 * @author Clément
 */
public class Tuple {
    private int number ; 
    private String name ; 

    public Tuple() {
    }

    public Tuple(int number, String name) {
        this.number = number;
        this.name = name;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }
    
}
