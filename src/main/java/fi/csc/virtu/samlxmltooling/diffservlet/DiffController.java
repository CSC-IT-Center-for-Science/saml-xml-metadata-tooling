package fi.csc.virtu.samlxmltooling.diffservlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.vbauer.herald.annotation.Log;

import fi.csc.virtu.samlxmltooling.Task;
import fi.csc.virtu.samlxmltooling.diffservlet.DiffTask.TaskFlavor;
import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;
import fi.csc.virtu.samlxmltooling.tools.TaskCleaner;

@RestController
public class DiffController {
	
	public final static String ERROR_STR = "error";
	public final static String OK_STR = "ok";
	public final static String STATUS_STR ="status";
	public final static String USAGE_PARAM_STR = "usage";
	public final static String FILE_PARAM_STR = "file";

	private Map<String, Task> taskList = new HashMap<String, Task>();
	private Timer cleaner = new Timer();
	
	@Log
	private Logger log;
	
	@Autowired
	MainConfiguration conf;
	
	@Autowired
	SamlDocBuilder docBuilder;
	
	@RequestMapping (path = "/ctrl/",
			method = RequestMethod.GET,
			params = "op=getChange")
	@ResponseBody
	public String getChangeController (HttpServletRequest req) {
		String sessId = req.getSession().getId();
		TaskFlavor flavor = conf.findFlavorFromRequest(req); 
		DiffTask task = getTask(sessId, flavor);
		return task.getChangeString();
	}


	@RequestMapping (path = "/ctrl/", method = RequestMethod.GET)
	public Map<String, String> getController (HttpServletRequest req) {
		Map<String, String> retMap = new HashMap<String, String>();
		
		String sessStr = (String) req.getSession().getAttribute("sessionStr");
		String sessId = req.getSession().getId();
		String op = req.getParameter("op");
		TaskFlavor flavor = conf.findFlavorFromRequest(req); 
		DiffTask task = getTask(sessId, flavor);
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
							conf.findFlavorFromRequest(req)); 
					retMap.put("opStat", "newTask");
					break;
			}
		}
		
		retMap.put("taskStatus", task.getStatus().toString());
		retMap.put("task", task.getUuid());
		retMap.put("taskListLength", String.valueOf(taskList.size()));
		retMap.put("flavor", task.getFlavor().toString());
		
		return retMap;
	}
	
	@PostMapping(path="/fileUpload")
	public Map<String, String> postFileController (HttpServletRequest req,
			@RequestParam(name=FILE_PARAM_STR, required=true) MultipartFile file,
			@RequestParam(name=USAGE_PARAM_STR, required=true) String usage) {
		
		String sessId = req.getSession().getId();
		Map<String, String> retMap = new HashMap<String, String>();

		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = fac.newDocumentBuilder();
			Document doc = builder.parse(file.getInputStream());
			switch (usage) {
			case "compFile":
				getTask(sessId).setComp(doc);
				retMap.put(STATUS_STR, OK_STR);
				break;
			case "baseFile":
				getTask(sessId).setBase(doc);
				retMap.put(STATUS_STR, OK_STR);
				break;
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			retMap.put(STATUS_STR, ERROR_STR);
			retMap.put(ERROR_STR, e.getMessage());
		}

		
		return retMap;
	}
	
	
	@PostMapping (path = "/xmlUpload")
	public Map<String, String> postController (HttpServletRequest req) {
		Map<String, String> retMap = new HashMap<String, String>();
	
		String sessId = req.getSession().getId();
		if (req.getContentType().matches("application/xml.*")) {
			try {
				InputStream is = req.getInputStream();
				DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder;
				builder = fac.newDocumentBuilder();
				Document doc = builder.parse(is);
				getTask(sessId).setComp(doc);
				retMap.put(STATUS_STR, OK_STR);
			} catch (IOException | ParserConfigurationException | SAXException e) {
				e.printStackTrace();
				retMap.put(STATUS_STR, ERROR_STR);
				retMap.put(ERROR_STR, e.getMessage());
			}
		} else {
			retMap.put(STATUS_STR, ERROR_STR);
			retMap.put(ERROR_STR, "wrong content type");
		}
		
		if (retMap.isEmpty()) {
			retMap.put(STATUS_STR, "nothing to do");
		}
		
		return retMap;
	}
	
	
	private DiffTask getTask(String sessId) {
		return getTask(sessId, conf.DEFAULT_FLAVOR);
	}
	
	private DiffTask getTask(String sessId, TaskFlavor flavor) {
		if (!taskList.containsKey(sessId)) {
			DiffTask task = getNewTask(sessId, flavor);
			taskList.put(sessId, task);
			return task;
		} else {
			return (DiffTask) taskList.get(sessId);
		}
	}
	
	private DiffTask getNewTask(String sessId, TaskFlavor flavor) {
		if (taskList.containsKey(sessId)) {
			taskList.remove(sessId);
		} 
		DiffTask task = taskFactory(flavor);
		taskList.put(sessId, task);
		return task;
	}
	
	private DiffTask taskFactory(TaskFlavor flavor) {
		return new DiffTask(flavor, docBuilder);
	}

	
	@PostConstruct
	public void scheduleCleaner() {
		cleaner.scheduleAtFixedRate(new TaskCleaner(taskList), 300000, 300000);
	}
	
}
