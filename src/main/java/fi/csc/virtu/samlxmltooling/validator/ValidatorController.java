package fi.csc.virtu.samlxmltooling.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.vbauer.herald.annotation.Log;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;
import fi.csc.virtu.samlxmltooling.diffservlet.MainConfiguration;
import fi.csc.virtu.samlxmltooling.tools.CertTool;
import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;

@Controller
@RequestMapping("/validate/")
public class ValidatorController {
	
	@Log
	Logger log;
	
	@Autowired
	SamlDocBuilder docBuilder;
	
	@Autowired
	MainConfiguration conf;
	
	private Map<String, ValidatorTask> taskList = new HashMap<String, ValidatorTask>();
	private Timer taskCleaner = new Timer();
	
	public enum ops {
		checkSchema, checkSig, checkValidUntil, checkCertsEqual,
		checkCertValidity,
		reqTask
	}

	@GetMapping("ctrl")
	@ResponseBody
	public Map<String, String> get(@RequestParam String op,
			@RequestParam(required=false) String flavor,
			HttpSession session) {
		final Map<String, String> retMap = new HashMap<String, String>();
		
		if (flavor == null) {
			flavor = conf.getFederations().get(0);
		}
		ValidatorTask task;
		try {
			task = getTask(session, flavor);
		} catch (Exception e) {
			putErrors(retMap, e);
			log.debug("unable to fetch task", e);
			return retMap;
		}
		
		switch (ops.valueOf(op)) {
		case checkValidUntil:
			putStatus(retMap, task.checkValidUntil());
			break;
		case checkSchema: 
			putStatus(retMap, task.checkSechema());
			break;
		case checkSig:
			putStatus(retMap, task.checkSig());
			break;
		case checkCertsEqual:
			putStatus(retMap, task.checkCertsEqual());
			break;
		case checkCertValidity:
			putStatus(retMap, task.checkCertValidity());
			break;
		case reqTask:
			try {
				task = getNewTask(session, flavor);
				putStatus(retMap, true);
				retMap.put("task", task.getMyUuid());
				retMap.put("taskFlavor", task.getFlavor());
			} catch (Exception e) {
				putErrors(retMap, e);
				log.warn("error while creating new task", e);
			}
			break;
		}
		
		if (retMap.isEmpty()) {
			retMap.put(DiffController.STATUS_STR, DiffController.ERROR_STR);
			retMap.put("reason", "nothing to do");
		}
		
		log.debug("-- ctrl returning");
		return retMap;
	}
	
	@GetMapping("/getFlavors")
	@ResponseBody
	public List<String> getFlavors() {
		return conf.getFederations();
	}
	
	@GetMapping("/getOps")
	@ResponseBody
	public List<String> getOps() {
		List<String> opsList = new ArrayList<String>();
		for (ops op: ops.values()) {
			String opStr = op.toString();
			log.trace("-- op: " + opStr);
			opsList.add(opStr);
		}
		return opsList;
	}
	
	
	private ValidatorTask getNewTask(HttpSession session, String flavor) throws Exception {
		if (sessionHasTask(session)) {
			taskList.remove(session.getId());
		}
		return getTask(session, flavor);
	}
	
	private ValidatorTask getTask(HttpSession session, String flavor) throws Exception {
		final String sessionId = session.getId();
		if (sessionHasTask(session)) {
			return taskList.get(sessionId);
		} else {
			ValidatorTask task = new ValidatorTask(sessionId, 
					docBuilder.getCurrent(flavor),
					CertTool.getFedCheckCert(flavor, conf),
					conf,
					flavor);
			taskList.put(sessionId, task);
			return task;
		}
	}
	
	private boolean sessionHasTask(HttpSession session) {
		for (String taskOwner: taskList.keySet()) {
			if (taskOwner.equals(session.getId())) {
				return true;
			}
		}
		return false;
	}
	
	private static void putStatus(Map<String, String> retMap, boolean status) {
		if (status) {
			retMap.put(DiffController.STATUS_STR, DiffController.OK_STR);
		} else {
			retMap.put(DiffController.STATUS_STR, DiffController.ERROR_STR);
		}
		
	}
	
	private static void putErrors (Map<String, String> retMap, Exception e) {
		retMap.put(DiffController.STATUS_STR, DiffController.ERROR_STR);
		retMap.put(DiffController.ERROR_STR, e.getMessage());
		e.printStackTrace();
	}
	
	@PostConstruct
	public void scheduleCleaner() {
		taskCleaner.scheduleAtFixedRate(new TaskCleaner(taskList), 30000, 30000);
	}
	
	private static class TaskCleaner extends TimerTask {
		
		private Map<String, ValidatorTask> taskList;
		
		public TaskCleaner(Map<String, ValidatorTask> taskList) {
			this.taskList = taskList;
		}

		@Override
		public void run() {
			List<String> remList = new ArrayList<String>();
			for (String taskOwner: taskList.keySet()) {
				if (!taskList.get(taskOwner).isActive()) {
					remList.add(taskOwner);
				}
			}
			for (String rem: remList) {
				taskList.remove(rem);
			}
		}
		
	}
	
}
