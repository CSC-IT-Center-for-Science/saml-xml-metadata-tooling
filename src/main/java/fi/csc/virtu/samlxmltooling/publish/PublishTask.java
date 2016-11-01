package fi.csc.virtu.samlxmltooling.publish;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.csc.virtu.samlxmltooling.Task;
import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;

public class PublishTask implements Task {
	
	private LocalDateTime latestAccess = LocalDateTime.now();
	private File publishFile;
	private Document publishDoc;
	private String myFlavor;
	private SamlDocBuilder builder;

	public PublishTask(String flavor, SamlDocBuilder builder) {
		this.myFlavor = flavor;
		this.builder = builder;
	}

	@Override
	public boolean isActive() {
		Duration dur = Duration.between(LocalDateTime.now(), latestAccess);
		long millis = dur.toMillis();
		return millis > 300000;
	}
	
	public boolean fileIsDocifiable(File file) {
		this.publishFile = file;
		try {
			this.publishDoc = builder.getDoc(file);
			return true;
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
			return false;
		}
	}

}
