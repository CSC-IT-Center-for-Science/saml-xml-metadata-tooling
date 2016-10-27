package fi.csc.virtu.samlxmltooling.validator;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.dom.DOMSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.csc.virtu.samlxmltooling.diffservlet.MainConfiguration;
import net.shibboleth.tool.xmlsectool.SchemaValidator;
import net.shibboleth.utilities.java.support.xml.SchemaBuilder.SchemaLanguage;

public class SchemaValidatorTool {

	final static Logger log  = Logger.getLogger(SchemaValidatorTool.class);
	
	public static boolean validate (Document doc, MainConfiguration conf) {

		log.debug("-- getting validator");
		final SchemaValidator validator;
		try {
			validator = new SchemaValidator(SchemaLanguage.XML,
					new File(conf.getSchemaDir()));
		} catch (SAXException e) {
			log.debug(e);
			return false;
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
			log.debug(e);
			return false;
		}
		
		return true;
		
	}
		
}
