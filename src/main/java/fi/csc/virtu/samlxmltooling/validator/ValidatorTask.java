package fi.csc.virtu.samlxmltooling.validator;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import fi.csc.virtu.samlxmltooling.diffservlet.MainConfiguration;
import fi.csc.virtu.samlxmltooling.tools.GeneralStrings;

public class ValidatorTask {
	
	private Document doc;
	private X509Certificate checkCert;
	private String ownerSessionId;
	private String myFlavor;
	private MainConfiguration conf;
	private LocalDateTime latestAccess = LocalDateTime.now();
	private String myUuid = UUID.randomUUID().toString();
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	public ValidatorTask(String sessionId, 
			Document doc,
			X509Certificate checkCert,
			MainConfiguration conf,
			String flavor) {
		this.ownerSessionId = sessionId;
		this.doc = doc;
		this.checkCert = checkCert;
		this.conf = conf;
		this.myFlavor = flavor;
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
	
	public boolean checkSechema() {
		return SchemaValidatorTool.validate(doc, conf);
	}
	
	public boolean checkSig() {
		return SigChecker.checkSig(doc, checkCert);
	}

	public boolean checkValidUntil() {
		return ValidUntilChecker.checkRange(doc, 
				conf.getFedConfInt(myFlavor, GeneralStrings.PROP_FED_VALIDUNTIL_MIN), 
				conf.getFedConfInt(myFlavor, GeneralStrings.PROP_FED_VALIDUNTIL_MAX));
	}
	
	public boolean checkCertsEqual() {
		return CertChecker.certsEqual(doc, checkCert);
	}
	
	public boolean checkCertValidity() {
		return CertChecker.certValidityInRange(doc, 
				conf.getFedConfInt(myFlavor, GeneralStrings.PROP_FED_SIGCERTVALID_MIN), 
				conf.getFedConfInt(myFlavor, GeneralStrings.PROP_FED_SIGCERTVALID_MAX));
	}
	
}
