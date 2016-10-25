package fi.csc.virtu.samlxmltooling.validator;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.vbauer.herald.annotation.Log;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;
import fi.csc.virtu.samlxmltooling.diffservlet.Task.TaskFlavor;
import fi.csc.virtu.samlxmltooling.tools.CertTool;
import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;

@Controller
@RequestMapping("/validate/")
public class ValidatorController {
	
	final static String filename = "/Users/klaalo/Virtu/virtu-metadata-signing-crt-2015.pem";
	//final static String filename = "/Users/klaalo/Haka/haka-sign-v3.pem";
	
	@Log
	Logger log;
	
	public enum ops {
		validateCurrent, checkSignCurrent, checkValidUntil, checkCertsEqual,
		checkCertValidity
	}

	@GetMapping("ctrl")
	@ResponseBody
	public Map<String, String> get(@RequestParam String op) {
		final Map<String, String> retMap = new HashMap<String, String>();
		
		Document doc;
		log.debug("-- getting document");
		try {
			doc = SamlDocBuilder.getCurrent(TaskFlavor.VIRTU);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			putErrors(retMap, e);
			return retMap;
		}
		
		switch (ops.valueOf(op)) {
		case validateCurrent: 
			SchemaValidatorTool.validate(retMap, doc);
			break;
		case checkSignCurrent:
			SigChecker.checkSig(retMap, doc, filename);
			break;
		case checkValidUntil:
			putStatus(retMap, ValidUntilChecker.checkRange(doc, 27, 32));
			break;
		case checkCertsEqual:
			try {
				putStatus(retMap, CertChecker.certsEqual(doc, filename));
			} catch (CertificateException | XPathExpressionException | IOException e) {
				putErrors(retMap, e);
				e.printStackTrace();
				return retMap;
			}
			break;
		case checkCertValidity:
			try {
				putStatus(retMap, CertChecker.certValidityInRange(
						CertTool.getCertFromDoc(doc), 30, 735));
			} catch (XPathExpressionException e) {
				putErrors(retMap, e);
				e.printStackTrace();
				return retMap;
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
	
}
