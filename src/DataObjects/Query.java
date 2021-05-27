package DataObjects;

public class Query {
	private String query;
	private int id;
	
	
	public Query(String query) {
		this.query = query;
		this.id = idCompute();
	}
	
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public int idCompute() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Query other = (Query) obj;
		if (id != other.id) {
			return false;
		}
		if (query == null) {
			if (other.query != null) {
				return false;
			}
		} else if (!query.equals(other.query)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "<Query id=\""+getId()+"\">"+getQuery()+"</Query>";
	}
	
	public String toWikiSyntax() {
		String result = getQuery().replaceAll(",", ",\n");
		result = result.replaceFirst(" FROM ", "\nFROM ");
		return "<code sql>\n"+result+"\n</code>";
	}
	
		
}
