/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Servlet;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import Main_App.GML_SLD;
import Main_App.MainApp;

import useful_Document.JSON;
import useful_Document.IOService.BufferedFactoryImpl;
import useful_Document.IOService.IOServiceWithBuffered;

/**
 *
 * @author Clément
 */
public class TraitementDonnees extends HttpServlet {

	
	IOServiceWithBuffered iOServiceWithBuffered = new IOServiceWithBuffered(new BufferedFactoryImpl());
	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
	 * methods.
	 *
	 * @param request  servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException      if an I/O error occurs
	 * @throws ParseException
	 * @throws SQLException
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, InterruptedException, ParseException, SQLException, ParserConfigurationException, SAXException {
		// response.setStatus(HttpServletResponse.SC_NO_CONTENT);
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		HttpSession session = request.getSession();
    	ServletContext sc = session.getServletContext();
    	String file = sc.getRealPath("VM_Local.xml");
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		final DocumentBuilder builder = factory.newDocumentBuilder();
		final Document document = builder.parse(file);
		String export = document.getDocumentElement().getElementsByTagName("export").item(0).getTextContent();
		String protocole =document.getDocumentElement().getElementsByTagName("protocole").item(0).getTextContent();	
		String hote =document.getDocumentElement().getElementsByTagName("hote").item(0).getTextContent();		
		String disc =document.getDocumentElement().getElementsByTagName("disc").item(0).getTextContent();
		String geojsonpath =document.getDocumentElement().getElementsByTagName("geojsonpath").item(0).getTextContent();
		String templatepath =document.getDocumentElement().getElementsByTagName("templatepath").item(0).getTextContent();
		String featureAnalyzerURL =document.getDocumentElement().getElementsByTagName("featureAnalyzerURL").item(0).getTextContent();
		String caractereAjoutParamURL =document.getDocumentElement().getElementsByTagName("caractereAjoutParamURL").item(0).getTextContent();
		
		featureAnalyzerURL =featureAnalyzerURL.replace(caractereAjoutParamURL, "&");
		
		System.out.println("******************** TTTTTTTTT03/09/2021TTTTTTTTt **************************");
		String templateAsJSON = iOServiceWithBuffered.read(templatepath);
		System.out.println(templateAsJSON);
		System.out.println("******************** TTTTTTTTT03/09/2021TTTTTTTTt **************************");
		
		/* CHOIX D'EXPORTATION POUR LA VM OU EN LOCAL */
		// String export = "VM";
		// String export = "local";

		/*
		 * CHOIX DE CREER PLUSIEURS FICHIERS PAR RAPPORT A LA DATE OU UN SEUL FICHIER
		 * GEOJSON
		 */
		int date = 0; // UN SEUL FICHIER
//    int data = 1; // PLUSIEURS FICHIERS

		// MainApp.main(null);
		String typeCarte = MainApp.main(request, export,hote,disc,geojsonpath,templatepath, date, protocole);

		try {

//    	Circles crl = new Circles();

			/* TODO output your page here. You may use following sample code. */
			out.println("<html>");
			out.println("<head>");
			out.println("<title>Servlet data processing</title>");
			out.println("<script language=\"JavaScript\" >");
			out.println("function OpenInNewTab(url) {");
			out.println("var win = window.open(url, '_self');");
			out.println("win.focus();");
			out.println("}");
			out.println("</script>");

			out.println("</head>");
			
			if (typeCarte.toUpperCase().contains(("GML_SLD").toUpperCase())) {
				out.println(
						"<body onload=\"window.open(" + "'map/sld.html?N=" + GML_SLD.map_number + "', '_self');\" >");
			} else if (typeCarte.toUpperCase().contains(("geoJSON").toUpperCase())) {

				if (export.equals("VM")) {
					out.println(
							
							" <body onload=\"window.location.replace(' "+ featureAnalyzerURL +" ');  \" >"
							);
					out.println("<script language=\"JavaScript\" >");
					out.println("setTimeout(() => {");
					out.println("	analyzer.Analyzer.initialize(" +templateAsJSON + ");");
					out.println("}, 5000);");
					out.println("</script>");
				}

				if (export.equals("local")) {
					out.println(
							"<body onload=\"window.open('https://mapps.geosystems.fr/FeatureAnalyzer/?view=Ali_TESTE&tenant=Edoh#', '_self');\" >");
				}

			} else if (typeCarte.toUpperCase().contains(("None").toUpperCase())) {
				out.println(
						"<body onload=\"window.open(" + "'map/sld.html?N=" + GML_SLD.map_number + "', '_self');\" >");
			}
			
			out.println("</body>");
			out.println("</html>");

		} finally {
			out.close();
		}

	}

	// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the
	// + sign on the left to edit the code.">
	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request  servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException      if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			processRequest(request, response);
		} catch (InterruptedException | ParseException | SQLException | ParserConfigurationException | SAXException ex) {
			Logger.getLogger(TraitementDonnees.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 *
	 * @param request  servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException      if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			processRequest(request, response);
		} catch (InterruptedException | ParseException | SQLException | ParserConfigurationException | SAXException ex) {
			Logger.getLogger(TraitementDonnees.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Returns a short description of the servlet.
	 *
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Short description";
	}// </editor-fold>
}
