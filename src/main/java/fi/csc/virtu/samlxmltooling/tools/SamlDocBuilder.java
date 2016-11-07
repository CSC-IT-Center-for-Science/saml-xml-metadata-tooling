package fi.csc.virtu.samlxmltooling.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.vbauer.herald.annotation.Log;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffTask.TaskFlavor;
import fi.csc.virtu.samlxmltooling.diffservlet.MainConfiguration;

public class SamlDocBuilder {
	
	@Autowired
	MainConfiguration conf;
	
	@Log
	Logger log;
	
	
	public Document getCurrent(String flavor) throws IOException, 
	ParserConfigurationException, SAXException {

		URL url = new URL(conf.getFedConfStr(flavor, GeneralStrings.PROP_FED_URL));
		return getDoc(url);
	}
	
	public Document getCurrent (TaskFlavor flavor) throws IOException, 
	ParserConfigurationException, SAXException {
		URL url;
		switch (flavor) {
			case HAKA:
				url = new URL(conf.getCurrentUrlHaka());
				break;
			case VIRTU:
				url = new URL(conf.getCurrentUrlVirtu());
				break;
			default:
				url = new URL(conf.getCurrentUrlHaka());
				break;
		}
		return getDoc(url);
	}
	
	private DocumentBuilder getBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();

		// without this, the validator will fail
        fac.setNamespaceAware(true);
		
		return fac.newDocumentBuilder();
	}

	public Document getDoc (URL url) throws IOException, ParserConfigurationException, SAXException {
		return getBuilder().parse(url.toString());
	}
	
	public Document getDoc (File file) throws SAXException, IOException, ParserConfigurationException {
		return getBuilder().parse(file);
	}

}
