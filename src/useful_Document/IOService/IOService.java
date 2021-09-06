package useful_Document.IOService;

public interface IOService {
	
	public String read(String fileName);
	public String write(String fileName, String message);
	public String rewrite(String fileName, String message);

}
