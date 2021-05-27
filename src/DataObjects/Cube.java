package DataObjects;

import Parser.ExecuteResponseXMLA;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.jdom2.Document;

public final class Cube {
	private LinkedList<Hierarchy> Filters;
	private LinkedList<Hierarchy> Axis0;
	private LinkedList<Hierarchy> Axis1;
	private String id;

	public Cube(LinkedList<Hierarchy> filters, LinkedList<Hierarchy> dimensions) throws NoSuchAlgorithmException {
		super();
		Filters = filters;
		this.setAxis(dimensions);
		id = idCompute(this.toWikiSyntax(),"MD5");
	}
	
	public Cube(LinkedList<Hierarchy> filters, LinkedList<Hierarchy> dimensions,String id) throws NoSuchAlgorithmException {
		super();
		Filters = filters;
		this.setAxis(dimensions);
		this.id = id;
	}
	
	public Cube(Cube c) throws NoSuchAlgorithmException {
		super();
		Filters = c.getFilters();
		Axis0 = c.getAxis1();
		Axis1 = c.getAxis0();
		id = idCompute(this.toWikiSyntax(),"MD5");
	}
	
	public String getId() {
		return id;
	}
	
	public void addFilter(Hierarchy filter){
		Filters.add(filter);
	}
	
	public Iterator<Hierarchy> getFilterByFilter(){
		return Filters.iterator();
	}
	
	public LinkedList<Hierarchy> getFilters(){
		return this.Filters;
	}
	
	public void setAxis(LinkedList<Hierarchy> dimensions){
		Iterator<Hierarchy> dim = dimensions.iterator();
		Hierarchy h;
		this.Axis0 = new LinkedList<Hierarchy>();
		this.Axis1 = new LinkedList<Hierarchy>();
		while(dim.hasNext()){
			h = dim.next();
			if(h.getAxis().contains("Axis0")){
				this.Axis0.add(h);
			}else{
				if(h.getAxis().contains("Axis1")){
					this.Axis1.add(h);
				}
			}
		}
	}
	public LinkedList<Hierarchy> getAxis0(){
		return this.Axis0;
	}
	public LinkedList<Hierarchy> getAxis1(){
		return this.Axis1;
	}
	
	public int getAxis0Length(){
		Iterator<Hierarchy> it = this.Axis0.iterator();
		int max=0;
		Hierarchy temp;
		while(it.hasNext()){
			temp = it.next();
			if(temp.getTuple()>max){max=temp.getTuple();}
		}
		return max;
	}
	
	public static String idCompute(String id, String algorithm) throws NoSuchAlgorithmException {
		byte[] hash = null;
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			hash = md.digest(id.getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hash.length; ++i) {
			String hex = Integer.toHexString(hash[i]);
			if (hex.length() == 1) {
				sb.append(0);
				sb.append(hex.charAt(hex.length() - 1));
			} else {
				sb.append(hex.substring(hex.length() - 2));
			}
		}
		return sb.toString();
	}
	
	public boolean comparFilters(Cube f){
		Iterator<Hierarchy> it = this.getFilters().iterator();
		Iterator<Hierarchy> it2 = f.getFilters().iterator();
		String original = "";
		String compare = "";
		while( it.hasNext() ){
			original=original+it.next().toString();
		}
		while( it2.hasNext() ){
			compare=it2.next().toString();
			if(original.contains(compare) != true ){
				return false;
			}
		}
		return true;
	}
	
	public boolean comparAxes(Cube f){
		Iterator<Hierarchy> it = this.getAxis0().iterator();
		Iterator<Hierarchy> it2 = f.getAxis0().iterator();
		String original = new String("");
		String compare = new String("");
		while( it.hasNext() ){
			original=original+it.next().toString();
		}
		while( it2.hasNext() ){
			compare=it2.next().toString();
			if(original.contains(compare) != true ){
				return false;
			}
		}
		it = this.getAxis1().iterator();
		it2 = f.getAxis1().iterator();
		while( it.hasNext() ){
			original=original+it.next().toString();
		}
		while( it2.hasNext() ){
			compare=it2.next().toString();
			if(original.contains(compare) != true ){
				return false;
			}
		}
		return true;
	}
	
	public boolean comparAxesTransposed(Cube f){
		Iterator<Hierarchy> it = this.getAxis0().iterator();
		Iterator<Hierarchy> it2 = f.getAxis1().iterator();
		String original = new String("");
		String compare = new String("");
		while( it.hasNext() ){
			original=original+it.next().toString();
		}
		while( it2.hasNext() ){
			compare=it2.next().toString();
			if(original.contains(compare) != true ){
				return false;
			}
		}
		it = this.getAxis1().iterator();
		it2 = f.getAxis0().iterator();
		while( it.hasNext() ){
			original=original+it.next().toString();
		}
		while( it2.hasNext() ){
			compare=it2.next().toString();
			if(original.contains(compare) != true ){
				return false;
			}
		}
		return true;
	}
	
	public boolean equality(Cube cube) {
		if(this.comparFilters(cube)==true){
			if(this.comparAxes(cube)==true || comparAxesTransposed(cube)==true){
				return true;
			}else{return false;}
		}else{return false;}
	}

	@Override
	public String toString() {
		Hierarchy h;
		String result = new String("<Cube id=\""+getId()+"\">\n");
		Iterator<Hierarchy> it = getFilterByFilter();
		while(it.hasNext()){
			result = result +it.next().toString()+"\n";
		}
		it = this.getAxis0().iterator();
		result = result + "\t<Axis name=\"Axis0\">\n";
		while(it.hasNext()){
			h=it.next();
			result = result+"\t\t<Tuple Dimension=\""+h.getDimension()+"\" number=\""+h.getTuple()+"\">"+h.getLevel()+"."+h.getCaption()+"</Tuple>\n";
		}
		result = result + "\t</Axis>\n";
		it = this.getAxis1().iterator();
		result = result + "\t<Axis name=\"Axis1\">\n";
		while(it.hasNext()){
			h=it.next();
			result = result+"\t\t<Tuple Dimension=\""+h.getDimension()+"\" number=\""+h.getTuple()+"\">"+h.getLevel()+"."+h.getCaption()+"</Tuple>\n";
		}
		result = result + "\t</Axis>\n";
		
		result=result+"</Cube>";
		return result;
	}
	
	public String toWikiSyntax() {
		LinkedList<Hierarchy> filters = getFilters();
		int taille,compteur=1,tuple=1;
		String temp = new String("");
		String result = new String("<html>\n<body>\n<TABLE BORDER=\"1\">\n<TR>\n<TH rowspan=\""+filters.size()+"\" colspan=\"2\">Slicer</TH>\n");
		Iterator<Hierarchy> it = getFilterByFilter();
		while(it.hasNext()){
			result = result +"<TD>"+it.next().toWikiSyntax()+"</TD></TR>\n";
		}
		taille = this.Axis0.size() + this.Axis1.size() +1;
		result = result+"<TR>\n<TH rowspan=\""+taille+"\">Dimensions</TH>\n<TR>\n<TH rowspan=\""+this.Axis0.size()+"\">Axis0(Columns)</TH>\n";
		
		taille = this.Axis0.size();
		
		while(compteur<=taille){
			it=this.Axis0.iterator();
			temp = temp +"<TD>";
			while(it.hasNext()){
				Hierarchy h = it.next();
				if(h.getTuple() == tuple){
					if(!temp.equals("<TD>")){
						temp = temp +" and "+h.toWikiSyntax();
					}else{temp = temp +h.toWikiSyntax();}					
					compteur++;
				}			
			}
			temp = temp + "</TD></TR>";
			if(! temp.contains("<TD></TD></TR>")){
				result = result + temp +"\n";
			}
			temp = new String("");
			tuple++;
		}
		tuple=1;
		compteur=1;
		result = result+"</TR>\n<TR>\n<TH rowspan=\""+this.Axis1.size()+"\">Axis1(Rows)</TH>\n";
		
		taille = this.Axis1.size();
		
		while(compteur<=taille){
			it=this.Axis1.iterator();
			temp = temp +"<TD>";
			while(it.hasNext()){
				Hierarchy h = it.next();
				if(h.getTuple() == tuple){
					if(!temp.equals("<TD>")){
						temp = temp +" and "+h.toWikiSyntax();
					}else{temp = temp +h.toWikiSyntax();}					
					compteur++;
				}		
			}
			temp = temp + "</TD></TR>";
			if(! temp.contains("<TD></TD></TR>")){
				result = result + temp + "\n";
			}
			temp = new String("");
			tuple++;
		}
		result=result+"</TR>\n</TABLE>\n</body>\n</html>";
		return result;
	}
	
	public String valuestoWikiSyntax(Document d,boolean transposed) throws NoSuchAlgorithmException {
		Cube cube;
		if(transposed == true) { cube = new Cube(this); }else{cube=this;} 
		CellValue[] c = ExecuteResponseXMLA.getValues(d);
//		System.out.println("(((((((("+c[0]);
		ValueTable valuetab[] = new ValueTable[c.length];
		
		Iterator<Hierarchy> axis0it = cube.getAxis0().iterator();
		Iterator<Hierarchy> axis1it = cube.getAxis1().iterator();
		
		Hierarchy tempAxis0,tempAxis1;
		int cellOrdinal;
		int count = 0, tuple_axis1=1, tuple_axis0=1, countprime = 0;
		while(count < valuetab.length){
			valuetab[count] = new ValueTable();
			while(axis1it.hasNext()){
				tempAxis1 = axis1it.next();
				if(tempAxis1.getTuple()==tuple_axis1){
					valuetab[count].addAxis1Tuple(tempAxis1);
					Iterator<Hierarchy> it = valuetab[count].axis1tuple.iterator();
					while(it.hasNext()){System.out.println("valuetab["+count+"]="+it.next());}
				}				
				else{System.out.println("false ????????????:"+tempAxis1.getTuple());} 
			}
			
			
			while(axis0it.hasNext()){
				tempAxis0 = axis0it.next();
				System.out.println("Axis0tuples ????????????:"+tuple_axis0);
				if(tempAxis0.getTuple() == tuple_axis0){
					valuetab[count].addAxis0Tuple(tempAxis0);
					Iterator<Hierarchy> it = valuetab[count].axis0tuple.iterator();
					while(it.hasNext()){System.out.println("valuetab["+count+"]="+it.next());}
				}
			}
			axis0it = cube.getAxis0().iterator(); 
			axis1it = cube.getAxis1().iterator();
			
			cellOrdinal = (tuple_axis1 - 1) * (cube.getAxis0Length()) + (tuple_axis0 - 1);
//			System.out.println(tuple_axis0);
			valuetab[count].setC(c[cellOrdinal]);
			count++;
			System.out.println("Compteur ...... :"+count);
//			if(count < valuetab.length){
				tuple_axis0 ++;
//				System.out.println("Axis0tuples ????????????:"+tuple_axis0);
				if(tuple_axis0 > cube.getAxis0Length()){
					tuple_axis0 =1;
					tuple_axis1++;
					System.out.println("Axis1tuples ????????????:"+tuple_axis1);
//				}
			}
		}
			
			while(countprime<count){
				Iterator<Hierarchy> axis0tuple = valuetab[countprime].axis0tuple.iterator();
				Iterator<Hierarchy> axis1tuple = valuetab[countprime].axis1tuple.iterator();
				CellValue cell = valuetab[countprime].c;
				System.out.println("------------------------------------------");
				while(axis0tuple.hasNext()){
					System.out.println("Axis0:"+axis0tuple.next().toString());
				}
				while(axis1tuple.hasNext()){
					System.out.println("Axis1:"+axis1tuple.next().toString());
				}
                
                System.out.println("Axis0 : tuple numero : "+valuetab[countprime].getAxe0Val());
                System.out.println("Axis1 : tuple numero : "+valuetab[countprime].getAxe1Val());
				System.out.println("----------------------"+cell.toString());
				countprime++;
			}
			
			
		
		String result =  new String("<html>\n<body>\n<TABLE BORDER=\"1\">\n");
		return result;
	}
    
    public ValueTable[] ValuesEtTuples(Document d,boolean transposed) throws NoSuchAlgorithmException {
		Cube cube;
		if(transposed == true) { cube = new Cube(this); }else{cube=this;} 
		CellValue[] c = ExecuteResponseXMLA.getValues(d);
        
		ValueTable valuetab[] = new ValueTable[c.length];
		
		Iterator<Hierarchy> axis0it = cube.getAxis0().iterator();
		Iterator<Hierarchy> axis1it = cube.getAxis1().iterator();
		
		Hierarchy tempAxis0,tempAxis1;
		int cellOrdinal;
		int count = 0, tuple_axis1=1, tuple_axis0=1, countprime = 0;
		while(count < valuetab.length){
			valuetab[count] = new ValueTable();
			while(axis1it.hasNext()){
				tempAxis1 = axis1it.next();
				if(tempAxis1.getTuple()==tuple_axis1){
					valuetab[count].addAxis1Tuple(tempAxis1);
					Iterator<Hierarchy> it = valuetab[count].axis1tuple.iterator();
					
				}
			}
			
			
			while(axis0it.hasNext()){
				tempAxis0 = axis0it.next();
				
				if(tempAxis0.getTuple() == tuple_axis0){
					valuetab[count].addAxis0Tuple(tempAxis0);
					Iterator<Hierarchy> it = valuetab[count].axis0tuple.iterator();
					
				}
			}
			axis0it = cube.getAxis0().iterator(); 
			axis1it = cube.getAxis1().iterator();
			
			cellOrdinal = (tuple_axis1 - 1) * (cube.getAxis0Length()) + (tuple_axis0 - 1);
            
			valuetab[count].setC(c[cellOrdinal]);
			count++;
            tuple_axis0 ++;

            if(tuple_axis0 > cube.getAxis0Length())
            {
                tuple_axis0 =1;
                tuple_axis1++;
			}
		}
		
        return valuetab;
	}
}
