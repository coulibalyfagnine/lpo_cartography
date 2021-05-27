package DataObjects;

public class CellValue {

	int ordianal;
	String value;
	
	
	public CellValue(int ordianal, String value) {
		super();
		this.ordianal = ordianal;
		this.value = new String(value);
	}
	
	
	/**
	 * @return the ordianal
	 */
	public int getOrdianal() {
		return ordianal;
	}
	/**
	 * @param ordianal the ordianal to set
	 */
	public void setOrdianal(int ordianal) {
		this.ordianal = ordianal;
	}
	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CellValue [ordianal=" + ordianal + ", value=" + value + "]";
	}
}
