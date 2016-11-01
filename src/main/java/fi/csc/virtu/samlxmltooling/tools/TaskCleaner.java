package fi.csc.virtu.samlxmltooling.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import fi.csc.virtu.samlxmltooling.Task;

public class TaskCleaner extends TimerTask{

	private Map<String, Task> taskList;
	
	public TaskCleaner(Map<String, Task> taskList) {
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
