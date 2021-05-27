package DataObjects;

public class Hierarchy {
	private String type;
	private String dimension;
	private String level;
	private String caption;
	private int tuple;
	private String axis;
	
	
	public Hierarchy(String type, String dimension, String level, String caption) {
		super();
		this.type = type;
		this.dimension = dimension;
		this.level = level;
		this.caption = caption;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getAxis() {
		return axis;
	}

	public void setAxis(String axis) {
		this.axis = axis;
	}
	
	public int getTuple() {
		return tuple;
	}
	
	public void setTuple(int tuple) {
		this.tuple = tuple;
	}

	public String getDimension() {
		return dimension;
	}

	public void setDimension(String dimension) {
		this.dimension = dimension;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	@Override
	public String toString() {
		String testType = new String(this.type);
		if(testType.contains("Filter") ){testType="\t<Slicer name=\"";}
		else{testType="\t<Dimension name=\"";}
		String result = new String("");
		result = testType+dimension+"\">"+level+"."+caption;
		if(testType.contains("Filter")){result = result+"</Slicer>";}
		else{result = result+"</Dimension>";}
		return result;
	}

	public String toWikiSyntax() {
		String result = new String("");
		result = level+"."+caption;
		return result;
	}
	
	public boolean equality(Hierarchy h) {
		if(this.type.equals(h.getType())){
			if(this.dimension.equals(h.getDimension())){
				if(this.caption.equals(h.getCaption())){
					if(this.level.equals(h.getLevel())){
						return true;
					}else{return false;}
				}else{return false;}
			}else{return false;}
		}else{return false;}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((caption == null) ? 0 : caption.hashCode());
		result = prime * result
				+ ((dimension == null) ? 0 : dimension.hashCode());
		result = prime * result + ((level == null) ? 0 : level.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	
}
