package fi.funet.haka.diffservlet;
import java.util.UUID;

import org.w3c.dom.Document;


public class Task {

	Document base;
	Document comp;
	String myUuid = UUID.randomUUID().toString();
	
	public String getUuid() {
		return myUuid;
	}
	
	public void setComp (Document doc) {
		this.comp = doc;
	}
	
}
