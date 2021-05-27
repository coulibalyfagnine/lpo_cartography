/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DataObjects;

/**
 *
 * @author Clément
 */
public class Point {
  private double x_ ; 
  private double y_ ;

  public Point(double x_, double y_) {
    this.x_ = x_;
    this.y_ = y_;
  }

  public double getX_() {
    return x_;
  }

  public double getY_() {
    return y_;
  }

  public void setX_(double x_) {
    this.x_ = x_;
  }

  public void setY_(double y_) {
    this.y_ = y_;
  }

  public void Ecrire() {
	// ajout anael
			//System.out.println("Point{" + "x_=" + x_ + ", y_=" + y_ + '}');
  }
  
}
