package DataObjects;

import java.util.LinkedList;

public class ValueTable {

	LinkedList<Hierarchy> axis0tuple;
        Integer axe0Val ; 
        Integer axe1Val ; 
	LinkedList<Hierarchy> axis1tuple;
	CellValue c;
	
	public ValueTable() {
		super();
		axis0tuple = new LinkedList<Hierarchy>();
		axis1tuple = new LinkedList<Hierarchy>();
	}

	public ValueTable(LinkedList<Hierarchy> axis0tuple,
			LinkedList<Hierarchy> axis1tuple, CellValue c) {
		super();
		this.axis0tuple = axis0tuple;
		this.axis1tuple = axis1tuple;
		this.c = c;
	}

  public Integer getAxe0Val() {
    return axe0Val;
  }

  public Integer getAxe1Val() {
    return axe1Val;
  }

  public LinkedList<Hierarchy> getAxis0tuple() {
    return axis0tuple;
  }

  public LinkedList<Hierarchy> getAxis1tuple() {
    return axis1tuple;
  }
	
	public void addAxis0Tuple(Hierarchy h){
		this.axis0tuple.add(h);
        this.axe0Val = h.getTuple();
	}
	
	public void addAxis1Tuple(Hierarchy h){
		this.axis1tuple.add(h);
        this.axe1Val = h.getTuple() ; 
	}

	/**
	 * @return the c
	 */
	public CellValue getC() {
		return c;
	}

	/**
	 * @param c the c to set
	 */
	public void setC(CellValue c) {
		this.c = c;
	}

	
}
