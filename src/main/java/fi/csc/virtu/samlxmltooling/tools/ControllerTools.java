package fi.csc.virtu.samlxmltooling.tools;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import fi.csc.virtu.samlxmltooling.Task;
import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;

public class ControllerTools {
	
	public static boolean sessionHasTask(HttpSession session,
			Map<String, Task> taskList) {
		for (String taskOwner: taskList.keySet()) {
			if (taskOwner.equals(session.getId())) {
				return true;
			}
		}
		return false;
	}
	
	public static void putStatus(Map<String, String> retMap, boolean status) {
		if (status) {
			retMap.put(DiffController.STATUS_STR, DiffController.OK_STR);
		} else {
			retMap.put(DiffController.STATUS_STR, DiffController.ERROR_STR);
		}
		
	}
	
	public static void putErrors (Map<String, String> retMap, Exception e) {
		retMap.put(DiffController.STATUS_STR, DiffController.ERROR_STR);
		retMap.put(DiffController.ERROR_STR, e.getMessage());
		e.printStackTrace();
	}
	
	public static Map<String, String> putErrors (Exception e) {
		Map<String, String> retMap = new HashMap<String, String>();
		retMap.put(DiffController.STATUS_STR, DiffController.ERROR_STR);
		retMap.put(DiffController.ERROR_STR, e.getMessage());
		return retMap;
	}
	
	public static Map<String, String> getErrorMap(String error) {
		return new HashMap<String, String>() {
			private static final long serialVersionUID = 1L;
			{
				put(DiffController.STATUS_STR, DiffController.ERROR_STR);
				put(DiffController.ERROR_STR, error);
			}
		};
	}


}
