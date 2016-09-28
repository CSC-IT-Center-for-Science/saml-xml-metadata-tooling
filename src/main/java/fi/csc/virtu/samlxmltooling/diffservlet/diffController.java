package fi.csc.virtu.samlxmltooling.diffservlet;

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

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.csc.virtu.samlxmltooling.diffservlet.Task.TaskFlavor;

@RestController
public class diffController {

	private Map<String, Task> taskList = new HashMap<String, Task>();
	private Timer cleaner = new Timer();
	

	@RequestMapping (path = "/ctrl/",
			method = RequestMethod.GET,
			params = "op=getChange")
	@ResponseBody
	public String getChangeController (HttpServletRequest req) {
		String sessId = req.getSession().getId();
		TaskFlavor flavor = Configuration.findFlavorFromRequest(req); 
		Task task = getTask(sessId, flavor);
		return task.getChangeString();
	}


	@RequestMapping (path = "/ctrl/", method = RequestMethod.GET)
	public Map<String, String> getController (HttpServletRequest req) {
		Map<String, String> retMap = new HashMap<String, String>();
		
		String sessStr = (String) req.getSession().getAttribute("sessionStr");
		String sessId = req.getSession().getId();
		String op = req.getParameter("op");
		TaskFlavor flavor = Configuration.findFlavorFromRequest(req); 
		Task task = getTask(sessId, flavor);
		retMap.put("taskIdle", String.valueOf(task.idleSeconds()));
		if (op != null) {
			switch (op) {
				case "getSessStr":
					retMap.put("sessStr", sessStr);
					break;
				case "fetchCurrent":
					if (task.fetchCurrent()) {
						retMap.put("opStat", "currentFetchOk");
					} else {
						retMap.put("opStat", "currentFetchFail");
					}
					break;
				case "processDiff":
					if (task.doDiff()) {
						retMap.put("opStat", "diffOk");
					} else {
						retMap.put("opStat", "diffError");
					}
					break;
				case "reqTask":
					task = getNewTask(sessId, 
							Configuration.findFlavorFromRequest(req)); 
					retMap.put("opStat", "newTask");
					break;
			}
		}
		
		retMap.put("taskStatus", task.getStatus().toString());
		retMap.put("task", task.getUuid());
		retMap.put("taskListLength", String.valueOf(taskList.size()));
		
		return retMap;
	}
	
	
	@RequestMapping (path = "/ctrl/", method = RequestMethod.POST)
	public Map<String, String> postController (HttpServletRequest req) {
		Map<String, String> retMap = new HashMap<String, String>();

		String str = req.getParameter("sessionStr");
		if (str != null) {
			req.getSession().setAttribute("sessionStr", str);
			retMap.put("status", "ok");
		}
		
		String sessId = req.getSession().getId();
		if (ServletFileUpload.isMultipartContent(req)) {
			ServletFileUpload fu = new ServletFileUpload(new DiskFileItemFactory());
			List<?> files;
			try {
				files = fu.parseRequest(req);
				String name = ((FileItem) files.get(0)).getFieldName();
				FileItem item = (FileItem) files.get(0);
				DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = fac.newDocumentBuilder();
				Document doc = builder.parse(item.getInputStream());
				switch (name) {
				case "compFile":
					getTask(sessId).setComp(doc);
					retMap.put("status", "ok");
					break;
				case "baseFile":
					getTask(sessId).setBase(doc);
					retMap.put("status", "ok");
					break;
				}
			} catch (FileUploadException | ParserConfigurationException | SAXException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				retMap.put("status", "error");
			}
		} else if (req.getContentType().matches("application/xml.*")) {
			try {
				InputStream is = req.getInputStream();
				DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder;
				try {
					builder = fac.newDocumentBuilder();
					Document doc = builder.parse(is);
					getTask(sessId).setComp(doc);
					retMap.put("status", "ok");
				} catch (ParserConfigurationException | SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					retMap.put("status", "error");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println(req.getContentType());
		}
		
		if (retMap.isEmpty()) {
			retMap.put("status", "nothing to do");
		}
		
		
		return retMap;
	}
	
	
	private Task getTask(String sessId) {
		return getTask(sessId, Configuration.DEFAULT_FLAVOR);
	}
	
	private Task getTask(String sessId, TaskFlavor flavor) {
		if (!taskList.containsKey(sessId)) {
			Task task = getNewTask(sessId, flavor);
			taskList.put(sessId, task);
			return task;
		} else {
			return taskList.get(sessId);
		}
	}
	
	private Task getNewTask(String sessId, TaskFlavor flavor) {
		if (taskList.containsKey(sessId)) {
			taskList.remove(sessId);
		} 
		Task task = new Task(flavor);
		taskList.put(sessId, task);
		return task;
	}

	
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

	@PostConstruct
	public void scheduleCleaner() {
		cleaner.scheduleAtFixedRate(new TasksCleaner(taskList), 300000, 300000);
	}
	
}
