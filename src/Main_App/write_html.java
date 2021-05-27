package Main_App;

import java.util.List;

public class write_html {
	
	public static String getHtml (List<String> list_dimension_value) {
		String html = "";
		
		int ct = 0;
		for (String val : list_dimension_value) {
//			if (ct == 0) {
//				html = ""
//			}else {
//				
//			}
			
			html = html + "<div class=\"MyLegend-TableRow\"> \n "+
					"<div class=\"MyLegend-ColorCell\"> \n "+
					"<div style=\"background-color:#00ff00\"></div> \n" +
					"</div> \n" +
					"<div style=\"display: table-cell\"> \n"+
					val + "\n "+
					"</div> \n" +
					"</div> \n" ;  
		}
		
		
		
		return html;
		
	}

}
