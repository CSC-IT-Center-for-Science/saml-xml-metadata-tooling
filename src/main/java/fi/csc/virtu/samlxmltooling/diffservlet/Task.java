package fi.csc.virtu.samlxmltooling.diffservlet;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;
import fi.csc.virtu.samlxmltooling.xmldiffer.Change;
import fi.csc.virtu.samlxmltooling.xmldiffer.XmlDiffer;


public class Task {
	
	public static enum TaskFlavor {
		VIRTU, HAKA
	}
	
	private TaskFlavor myFlavor;

	private Document base;
	private Document comp;
	private String myUuid = UUID.randomUUID().toString();
	public enum status {
		initiated, fetchingCurrent, currentFetched, compSet,
		baseSet, readyForDiff, processingDiff, diffError, diffProcessed
	}
	private status myStatus = status.initiated;
	private Change change;
	private Date latestAccess = new Date();
	
	public Task (TaskFlavor myFlavorArg) {
		this.myFlavor = myFlavorArg;
	}
	
	public String getUuid() {
		return myUuid;
	}
	
	public status getStatus() {
		touch();
		return this.myStatus;
	}
	
	public TaskFlavor getFlavor() {
		return myFlavor;
	}
	
	public long idleSeconds() {
		return (new Date().getTime() - latestAccess.getTime()) / 1000;
	}
	
	public boolean isActive() {
		return idleSeconds() < 600;
	}
	
	private void touch() {
		this.latestAccess = new Date();
	}
	
	public void setComp (Document doc) {
		touch();
		this.comp = doc;
		if (!isDiffReady()) {
			this.myStatus = status.compSet;
		}
	}
	
	public void setBase (Document doc) {
		touch();
		this.base = doc;
		if (!isDiffReady()) {
			this.myStatus = status.baseSet;
		}
	}
	
	private boolean isDiffReady() {
		touch();
		if (base != null && comp != null) {
			this.myStatus = status.readyForDiff;
			return true;
		} else {
			return false;
		}
	}
	
	public boolean fetchCurrent() {
		touch();
		try {
			this.myStatus = status.fetchingCurrent;
			this.base = SamlDocBuilder.getCurrent(myFlavor);
			this.myStatus = status.currentFetched; 
			return true;
		} catch (IOException | ParserConfigurationException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean doDiff() {
		touch();
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
		touch();
		if (myStatus == status.diffProcessed) {
			return change.toString();
		} else {
			return "";
		}
	}
	
}
