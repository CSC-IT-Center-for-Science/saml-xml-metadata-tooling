package fi.funet.haka.diffservlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;


public class JettyExperimentServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4132043273553512718L;
	
	private Map<String, Task> taskList = new HashMap<String, Task>();

	@Override
	protected void doPost (HttpServletRequest req, HttpServletResponse resp) {
		
		JSONObject jsO = new JSONObject();
		String str = req.getParameter("sessionStr");
		if (str != null) {
			req.getSession().setAttribute("sessionStr", str);
			jsO.put("status", "ok");
		}
		
		str = req.getParameter("files");
		if (str != null && ServletFileUpload.isMultipartContent(req)) {
			ServletFileUpload fu = new ServletFileUpload(new DiskFileItemFactory());
			String sessId = req.getSession().getId();
			List<?> files;
			try {
				files = fu.parseRequest(req);
				FileItem item = (FileItem) files.get(0);
				DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = fac.newDocumentBuilder();
				Document doc = builder.parse(item.getInputStream());
				getTask(sessId).setComp(doc);
				jsO.put("status", "ok");
			} catch (FileUploadException | ParserConfigurationException | SAXException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				jsO.put("status", "error");
			}
			
			
		}
		
		if (jsO.length() == 0) {
			jsO.put("status", "nothing to do");
		}
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json");
		try {
			jsO.write(resp.getWriter());
		} catch (JSONException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	protected void doGet (HttpServletRequest req, HttpServletResponse resp) {
		String str = req.getParameter("stat");
		String sessStr = (String) req.getSession().getAttribute("sessionStr");
		JSONObject jsO = new JSONObject();
		String sessId = req.getSession().getId();
		jsO.put("task", getTask(sessId));
		if (str != null && str.equals("true") && sessStr != null) {
			jsO.put("sessStr", sessStr);
		}
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json");
		try {
			jsO.write(resp.getWriter());
		} catch (JSONException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Task getTask(String sessId) {
		if (!taskList.containsKey(sessId)) {
			Task task = new Task();
			taskList.put(sessId, task);
			return task;
		} else {
			return taskList.get(sessId);
		}

	}
	
}
