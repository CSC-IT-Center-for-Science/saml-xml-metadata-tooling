package fi.csc.virtu.samlxmltooling.validator;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.vbauer.herald.annotation.Log;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;
import fi.csc.virtu.samlxmltooling.diffservlet.MainConfiguration;
import fi.csc.virtu.samlxmltooling.tools.CertTool;
import fi.csc.virtu.samlxmltooling.tools.GeneralStrings;
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
		checkSchemaCurrent, checkSignCurrent, checkValidUntilCurrent, checkCertsEqualCurrent,
		checkCertValidityCurrent,
		reqTask
	}

	@GetMapping("ctrl")
	@ResponseBody
	public Map<String, String> get(@RequestParam String op,
			@RequestParam(required=false) String flavor,
			HttpSession session) {
		final Map<String, String> retMap = new HashMap<String, String>();
		
		ValidatorTask task;
		try {
			task = getTask(session, "Virtu");
		} catch (IOException | ParserConfigurationException | SAXException e1) {
			putErrors(retMap, e1);
			e1.printStackTrace();
			return retMap;
		}
		
		log.debug("-- getting document");
		Document doc = task.getDoc();
		
		String filename = conf.getFedConfStr("Virtu", GeneralStrings.PROP_FED_CERTFILE);
		
		switch (ops.valueOf(op)) {
		case checkSchemaCurrent: 
			SchemaValidatorTool.validate(retMap, doc);
			break;
		case checkSignCurrent:
			SigChecker.checkSig(retMap, doc, filename);
			break;
		case checkValidUntilCurrent:
			putStatus(retMap, ValidUntilChecker.checkRange(doc, 27, 32));
			break;
		case checkCertsEqualCurrent:
			try {
				putStatus(retMap, CertChecker.certsEqual(doc, filename));
			} catch (CertificateException | XPathExpressionException | IOException e) {
				putErrors(retMap, e);
				e.printStackTrace();
				return retMap;
			}
			break;
		case checkCertValidityCurrent:
			try {
				putStatus(retMap, CertChecker.certValidityInRange(
						CertTool.getCertFromDoc(doc), 30, 735));
			} catch (XPathExpressionException e) {
				putErrors(retMap, e);
				e.printStackTrace();
				return retMap;
			}
			break;
		case reqTask:
			putStatus(retMap, true);
			retMap.put("task", task.getMyUuid());
			break;
		}
		
		if (retMap.isEmpty()) {
			retMap.put(DiffController.STATUS_STR, DiffController.ERROR_STR);
			retMap.put("reason", "nothing to do");
		}
		
		log.debug("-- ctrl returning");
		return retMap;
	}
	
	private ValidatorTask getTask(HttpSession session, String flavor) throws IOException, ParserConfigurationException, SAXException {
		final String sessionId = session.getId();
		if (sessionHasTask(session)) {
			return taskList.get(sessionId);
		} else {
			ValidatorTask task = new ValidatorTask(sessionId, docBuilder.getCurrent(flavor));
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
			for (String taskOwner: taskList.keySet()) {
				if (!taskList.get(taskOwner).isActive()) {
					taskList.remove(taskOwner);
				}
			}
		}
		
	}
	
}
