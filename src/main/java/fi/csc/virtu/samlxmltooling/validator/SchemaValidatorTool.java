package fi.csc.virtu.samlxmltooling.validator;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;
import fi.csc.virtu.samlxmltooling.diffservlet.Task.TaskFlavor;
import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;
import net.shibboleth.tool.xmlsectool.SchemaValidator;
import net.shibboleth.utilities.java.support.xml.SchemaBuilder.SchemaLanguage;

public class SchemaValidatorTool {

	final static Logger log  = Logger.getLogger(SchemaValidatorTool.class);
	
	public static void validate (Map<String, String> retMap) {
		Document doc;
		log.debug("-- getting document");
		try {
			doc = SamlDocBuilder.getCurrent(TaskFlavor.VIRTU);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			putErrors(retMap, e);
			return;
		}
		
		log.debug("-- getting validator");
		final SchemaValidator validator;
		try {
			validator = new SchemaValidator(SchemaLanguage.XML,
					new File("/Users/klaalo/Haka/metadata_schema/"));
		} catch (SAXException e) {
			putErrors(retMap, e);
			return;
		}
		
		// Allow DTD fetch by http by setting:
		// /Library/Java/JavaVirtualMachines/jdk1.8.0_31.jdk/Contents/Home/jre/lib/jaxp.properties
		// javax.xml.accessExternalDTD=file,http
		// see: http://stackoverflow.com/questions/23011547/webservice-client-generation-error-with-jdk8

		//
		//System.setProperty("javax.xml.accessExternalSchema", "file,http");
		
		try {
			log.debug("-- validating");
			validator.validate(new DOMSource(doc.getDocumentElement()));
		} catch (SAXException | IOException e) {
			putErrors(retMap, e);
		}
		
		retMap.put(DiffController.STATUS_STR, DiffController.OK_STR);
		
	}
	
	private static void putErrors (Map<String, String> retMap, Exception e) {
		retMap.put(DiffController.STATUS_STR, DiffController.ERROR_STR);
		retMap.put(DiffController.ERROR_STR, e.getMessage());
		e.printStackTrace();
	}

	
}
