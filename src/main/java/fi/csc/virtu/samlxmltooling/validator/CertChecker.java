package fi.csc.virtu.samlxmltooling.validator;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import fi.csc.virtu.samlxmltooling.tools.CertTool;

public class CertChecker {
	
	final static Logger log  = LoggerFactory.getLogger(CertChecker.class);
	
	public static boolean certsEqual (Document doc,
			String filename) throws CertificateException, IOException, XPathExpressionException {
		
		return CertTool.certsEqual(CertTool.getCertFromDoc(doc), filename);
	}
	
	public static boolean certValidityInRange (Document doc,
			int minDays,
			int maxDays) throws XPathExpressionException {
		return certValidityInRange(CertTool.getCertFromDoc(doc), minDays, maxDays);
	}
	
	public static boolean certValidityInRange (X509Certificate cert,
			int minDays,
			int maxDays) {
		try {
			cert.checkValidity();
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			return false;
		}
		
		Date notAfter = cert.getNotAfter();
		LocalDate notAfterLocal = notAfter.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		long deltaDays = ChronoUnit.DAYS.between(LocalDate.now(), notAfterLocal);
		log.trace("deltaDays: " + deltaDays);

		if (deltaDays > minDays &&
				deltaDays < maxDays) {
			log.debug("certValidity in range");
			return true;
		} else {
			log.debug("certValidity out of range");
			return false;
		}
	}

}
