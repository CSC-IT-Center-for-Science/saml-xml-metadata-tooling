package fi.csc.virtu.samlxmltooling.validator;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.vbauer.herald.annotation.Log;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;

@Controller
@RequestMapping("/validate/")
public class ValidatorController {
	
	@Log
	Logger log;
	
	public enum ops {
		validateCurrent
	}

	@GetMapping("ctrl")
	@ResponseBody
	public Map<String, String> get(@RequestParam String op) {
		final Map<String, String> retMap = new HashMap<String, String>();
		
		switch (ops.valueOf(op)) {
		case validateCurrent: 
			SchemaValidatorTool.validate(retMap);
			break;
		}
		
		if (retMap.isEmpty()) {
			retMap.put(DiffController.STATUS_STR, DiffController.ERROR_STR);
			retMap.put("reason", "nothing to do");
		}
		
		log.debug("-- ctrl returning");
		return retMap;
	}
	
}
