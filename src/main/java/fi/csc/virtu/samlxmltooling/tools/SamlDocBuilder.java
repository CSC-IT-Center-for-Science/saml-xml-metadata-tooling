package fi.csc.virtu.samlxmltooling.tools;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.csc.virtu.samlxmltooling.diffservlet.Configuration;
import fi.csc.virtu.samlxmltooling.diffservlet.Task.TaskFlavor;

public class SamlDocBuilder {
	
	public static Document getCurrent (TaskFlavor flavor) throws IOException, 
		ParserConfigurationException, SAXException {
		URL url;
		switch (flavor) {
			case HAKA:
				url = new URL(Configuration.getCurrentUrlHaka());
				break;
			case VIRTU:
				url = new URL(Configuration.getCurrentUrlVirtu());
				break;
			default:
				url = new URL(Configuration.getCurrentUrlHaka());
				break;
		}
		URLConnection conn = url.openConnection(); 
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();

		// without this, the validator will fail
        fac.setNamespaceAware(true);
		
		DocumentBuilder builder;
		builder = fac.newDocumentBuilder();
		Document doc = builder.parse(conn.getInputStream());
		return doc;
	}

}
