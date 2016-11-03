package fi.csc.virtu.samlxmltooling.validator;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import fi.csc.virtu.samlxmltooling.Task;
import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;
import fi.csc.virtu.samlxmltooling.diffservlet.MainConfiguration;
import fi.csc.virtu.samlxmltooling.tools.GeneralStrings;

public class ValidatorTask implements Task {
	
	private Document doc;
	private X509Certificate checkCert;
	private String myFlavor;
	private MainConfiguration conf;
	private LocalDateTime latestAccess = LocalDateTime.now();
	private String myUuid = UUID.randomUUID().toString();
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	public ValidatorTask( 
			Document doc,
			X509Certificate checkCert,
			MainConfiguration conf,
			String flavor) {
		this.doc = doc;
		this.checkCert = checkCert;
		this.conf = conf;
		this.myFlavor = flavor;
	}

	public Document getDoc() {
		update();
		return doc;
	}
	@Override
	public boolean isActive() {
		Duration dur = Duration.between(LocalDateTime.now(), latestAccess);
		long millis = dur.toMillis();
		return millis > 300000;
	}

	public String getMyUuid() {
		update();
		return myUuid;
	}
	
	public String getFlavor() {
		return myFlavor;
	}
	
	private void update() {
		this.latestAccess = LocalDateTime.now();
	}
	
	public boolean checkSchema() {
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
	
	public Map<String, String> prePublishChecks() {
		Map<String, String> retMap = new HashMap<String, String>();
		retMap.put(
				ValidatorController.ops.checkSchema.toString(),
				resultStr(checkSchema())
				);
		retMap.put(
				ValidatorController.ops.checkSig.toString(),
				resultStr(checkSig())
				);
		retMap.put(
				ValidatorController.ops.checkValidUntil.toString(),
				resultStr(checkValidUntil())
				);
		retMap.put(
				ValidatorController.ops.checkCertsEqual.toString(),
				resultStr(checkCertsEqual())
				);
		retMap.put(
				ValidatorController.ops.checkCertValidity.toString(),
				resultStr(checkCertValidity())
				);
		return retMap;
	}
	
	private String resultStr(Boolean result) {
		return result ? DiffController.OK_STR : DiffController.ERROR_STR;
	}
	
}
