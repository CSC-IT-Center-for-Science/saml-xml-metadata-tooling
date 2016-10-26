package fi.csc.virtu.samlxmltooling.validator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.w3c.dom.Document;

public class ValidatorTask {
	
	private Document doc;
	private LocalDateTime latestAccess = LocalDateTime.now();
	private String myUuid = UUID.randomUUID().toString();
	private String ownerSessionId;
	
	public ValidatorTask(String sessionId, Document doc) {
		this.ownerSessionId = sessionId;
		this.doc = doc;
	}

	public Document getDoc() {
		update();
		return doc;
	}
	public String getOwnerSessionId() {
		return ownerSessionId;
	}
	
	public boolean isActive() {
		Duration dur = Duration.between(LocalDateTime.now(), latestAccess);
		long millis = dur.toMillis();
		return millis > 300000;
	}

	public String getMyUuid() {
		update();
		return myUuid;
	}
	
	private void update() {
		this.latestAccess = LocalDateTime.now();
	}

	
}
