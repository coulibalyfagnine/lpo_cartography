package Servlet;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
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

import useful_Document.TEXT;

@WebServlet("/Accueil")
public class Accueil extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public Accueil() {
		super();
		// TODO Auto-generated constructor stub
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		this.getServletContext().getRequestDispatcher("/WEB-INF/jsp/accueil.jsp").forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			processRequest(request, response);
		} catch (InterruptedException | ParseException | SQLException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
		HttpSession session = request.getSession();
		ServletContext sc = session.getServletContext();
		String file = sc.getRealPath("VM_Local.xml");
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			final DocumentBuilder builder = factory.newDocumentBuilder();
			Document document;
			document = builder.parse(file);
			String protocole = document.getDocumentElement().getElementsByTagName("protocole").item(0).getTextContent();
			String hote = document.getDocumentElement().getElementsByTagName("hote").item(0).getTextContent();
			String port = document.getDocumentElement().getElementsByTagName("port").item(0).getTextContent();

			response.sendRedirect(protocole+"://" + hote +":"+ port + "/sROOT/");

		} catch (SAXException | ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, InterruptedException, ParseException, SQLException,
			ParserConfigurationException, SAXException {
		HttpSession session = request.getSession();
		ServletContext sc = session.getServletContext();

		String src = "";
		String src1 = "";
		String cube = request.getParameter("Cube");
		String dst = sc.getRealPath("configBase.xml");
		String dst1 = sc.getRealPath("/config/display_conf.xml");
		if (cube.contentEquals("OAB")) {
			src = sc.getRealPath("/configbase/configBase_OAB.xml");
			src1 = sc.getRealPath("/displayconf/display_conf_OAB.xml");
		} else if (cube.contentEquals("LPO")) {
			src = sc.getRealPath("/configbase/configBase_LPO.xml");
			src1 = sc.getRealPath("/displayconf/display_conf_LPO.xml");
		}
		File dest = new File(dst);
		File source = new File(src);
		File dest1 = new File(dst1);
		File source1 = new File(src1);
		// System.out.println("Cube: " + cube);
//		System.out.println("dest: " + dest1);
//		System.out.println("source: " + source1);
		TEXT.copyFile(source, dest);
		TEXT.copyFile(source1, dest1);

	}
}
