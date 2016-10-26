package fi.csc.virtu.samlxmltooling.tools;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.vbauer.herald.annotation.Log;

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
	
	private Document getDoc (URL url) throws IOException, ParserConfigurationException, SAXException {
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
