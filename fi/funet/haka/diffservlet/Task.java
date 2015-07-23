package fi.funet.haka.diffservlet;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.funet.haka.xmldiffer.Change;
import fi.funet.haka.xmldiffer.XmlDiffer;


public class Task {

	private Document base;
	private Document comp;
	private String myUuid = UUID.randomUUID().toString();
	public enum status {
		initiated, fetchingCurrent, currentFetched, compSet,
		processingDiff, diffError, diffProcessed
	}
	private status myStatus = status.initiated;
	private Change change;
	
	public String getUuid() {
		return myUuid;
	}
	
	public status getStatus() {
		return this.myStatus;
	}
	
	public void setComp (Document doc) {
		this.comp = doc;
		this.myStatus = status.compSet;
	}
	
	public boolean fetchCurrent() {
		try {
			this.myStatus = status.fetchingCurrent;
			URL url = new URL("https://haka.funet.fi/metadata/haka-metadata.xml");
			URLConnection conn = url.openConnection(); 
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			builder = fac.newDocumentBuilder();
			Document doc = builder.parse(conn.getInputStream());
			this.base = doc;
			this.myStatus = status.currentFetched;
			return true;
		} catch (IOException | ParserConfigurationException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean doDiff() {
		if (base == null || comp == null) { return false; }
		this.myStatus = status.processingDiff;
		try {
			this.change = XmlDiffer.diff(base, comp);
			this.myStatus = status.diffProcessed;
			return true;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			this.myStatus = status.diffError;
			return false;
		}
	}
	
	public String getChangeString() {
		if (myStatus == status.diffProcessed) {
			return change.toString();
		} else {
			return "";
		}
	}
	
}
