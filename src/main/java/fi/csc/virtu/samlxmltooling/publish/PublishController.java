package fi.csc.virtu.samlxmltooling.publish;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.github.vbauer.herald.annotation.Log;

import fi.csc.virtu.samlxmltooling.Task;
import fi.csc.virtu.samlxmltooling.diffservlet.MainConfiguration;
import fi.csc.virtu.samlxmltooling.tools.ControllerTools;
import fi.csc.virtu.samlxmltooling.tools.GeneralStrings;
import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;
import fi.csc.virtu.samlxmltooling.tools.TaskCleaner;

@RestController
@RequestMapping("/publish/")
public class PublishController {
	
	private final String POSTFILE_PAR = "importFile";
	private Map<String, Task> taskList = new HashMap<String, Task>();
	private Timer taskCleaner = new Timer();
	
	public enum ops {
		prePublishChecks
	}
	
	@Log
	Logger log;
	
	@Autowired
	MainConfiguration conf;

	@Autowired
	SamlDocBuilder docBuilder;
	
	@PostMapping(path="ctrl")
	public Map<String, String> postFile (
			@RequestParam (name=POSTFILE_PAR, required=true) MultipartFile requestFile,
			@RequestParam (required=false) String flavor,
			HttpSession session) {
		
		Map<String, String> retMap = new HashMap<String, String>(); 
		
		if (flavor == null) {
			flavor = conf.getFederations().get(0);
		}

		PublishTask task = getNewTask(session, flavor);
		File publishFile = new File(conf.getFedConfStr(flavor, GeneralStrings.PROP_FED_PUBLISH_FILE));
		try {
			requestFile.transferTo(publishFile);
			ControllerTools.putStatus(retMap, task.fileIsDocifiable(publishFile));
		} catch (IllegalStateException | IOException e) {
			ControllerTools.putErrors(retMap, e);
			e.printStackTrace();
		}
		return retMap;
	}
	
	@GetMapping(path="ctrl")
	public Map<String, String> getOp(HttpSession session,
			@RequestParam String op) {

		PublishTask task = getTask(session);
		if (task == null) {
			return ControllerTools.getErrorMap("no task");
		}

		switch (ops.valueOf(op)) {
			case prePublishChecks:
				return task.runPrePublishChecks(conf);
		}
		
		return ControllerTools.getErrorMap("nothing to do");
	}
	
	private PublishTask getNewTask(HttpSession session, String flavor) {
		if (ControllerTools.sessionHasTask(session, taskList)) {
			taskList.remove(session.getId());
		}
		return getTask(session, flavor);
	}
	
	private PublishTask getTask(HttpSession session) {
		if (ControllerTools.sessionHasTask(session, taskList)) {
			return (PublishTask) taskList.get(session.getId());
		} else {
			return null;
		}
	}
	
	private PublishTask getTask (HttpSession session, String flavor) {
		PublishTask task = getTask(session);
		if (task == null) {
			task = new PublishTask(flavor, docBuilder);
			taskList.put(session.getId(), task);
		}
		return task;
	}
	
	@PostConstruct
	public void scheduleCleaner() {
		taskCleaner.scheduleAtFixedRate(new TaskCleaner(taskList), 30000, 30000);
	}
	
}
