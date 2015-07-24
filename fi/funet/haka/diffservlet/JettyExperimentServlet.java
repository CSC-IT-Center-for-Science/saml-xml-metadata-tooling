package fi.funet.haka.diffservlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
	private Timer cleaner = new Timer();

	private static class TasksCleaner extends TimerTask {

		private Map<String, Task> taskList = new HashMap<String, Task>();
		public TasksCleaner(Map<String, Task> taskList) {
			this.taskList = taskList;
		}

		@Override
		public void run() {
			List<String> removable = new ArrayList<String>();
			for (String key : taskList.keySet()) {
				Task task = taskList.get(key); 
				if (!task.isActive()) {
					removable.add(key);
				}
			}
			Iterator<String> i = removable.iterator();
			while (i.hasNext()) {
				taskList.remove(i.next());
			}
		}
		
	}
	
	public JettyExperimentServlet() {
		cleaner.scheduleAtFixedRate(new TasksCleaner(taskList), 300000, 300000);
	}
	
	@Override
	protected void doPost (HttpServletRequest req, HttpServletResponse resp) {
		
		JSONObject jsO = new JSONObject();
		String str = req.getParameter("sessionStr");
		if (str != null) {
			req.getSession().setAttribute("sessionStr", str);
			jsO.put("status", "ok");
		}
		
		String sessId = req.getSession().getId();
		if (ServletFileUpload.isMultipartContent(req)) {
			ServletFileUpload fu = new ServletFileUpload(new DiskFileItemFactory());
			List<FileItem> files;
			try {
				files = fu.parseRequest(req);
				String name = files.get(0).getFieldName();
				FileItem item = (FileItem) files.get(0);
				DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = fac.newDocumentBuilder();
				Document doc = builder.parse(item.getInputStream());
				switch (name) {
				case "compFile":
					getTask(sessId).setComp(doc);
					jsO.put("status", "ok");
					break;
				case "baseFile":
					getTask(sessId).setBase(doc);
					jsO.put("status", "ok");
					break;
				}
			} catch (FileUploadException | ParserConfigurationException | SAXException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				jsO.put("status", "error");
			}
		} else if (req.getContentType().equals("application/xml")) {
			try {
				InputStream is = req.getInputStream();
				DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder;
				try {
					builder = fac.newDocumentBuilder();
					Document doc = builder.parse(is);
					getTask(sessId).setComp(doc);
					jsO.put("status", "ok");
				} catch (ParserConfigurationException | SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					jsO.put("status", "error");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		JSONObject jsO = new JSONObject();
		String sessStr = (String) req.getSession().getAttribute("sessionStr");
		String sessId = req.getSession().getId();
		String op = req.getParameter("op");
		Task task = getTask(sessId);
		jsO.put("taskIdle", task.idleSeconds());
		if (op != null) {
			switch (op) {
				case "getSessStr":
					jsO.put("sessStr", sessStr);
					break;
				case "getChange":
					resp.setContentType("text/plain");
					try (PrintWriter wr = resp.getWriter()) {
						wr.write(task.getChangeString());
						wr.flush();
						wr.close();
						return;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				break;
				case "fetchCurrent":
					if (task.fetchCurrent()) {
						jsO.put("opStat", "currentFetchOk");
					} else {
						jsO.put("opStat", "currentFetchFail");
					}
					break;
				case "processDiff":
					if (task.doDiff()) {
						jsO.put("opStat", "diffOk");
					} else {
						jsO.put("opStat", "diffError");
					}
					break;
				case "reqTask":
					task = getNewTask(sessId); 
					jsO.put("opStat", "newTask");
					break;
			}
		}

		jsO.put("taskStatus", task.getStatus().toString());
		jsO.put("task", task.getUuid());
		jsO.put("taskListLength", taskList.size());
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
	
	private Task getNewTask(String sessId) {
		if (taskList.containsKey(sessId)) {
			taskList.remove(sessId);
		} 
		Task task = new Task();
		taskList.put(sessId, task);
		return task;
	}
	
}
