package fi.csc.virtu.samlxmltooling.validator;

import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class ValidUntilChecker {
	
	final static Logger log  = LoggerFactory.getLogger(ValidUntilChecker.class);
	
	public static boolean checkRange(Document doc,
			int minDays,
			int maxDays) {
		String validUntilStr =
			doc.getElementsByTagName(GeneralStrings.SAML_MD_ENTITIESDESCR).item(0)
				.getAttributes().getNamedItem(GeneralStrings.SAML_MD_ATTR_VALIDUNTIL)
				.getTextContent();
		Calendar validUntilCal = DatatypeConverter.parseDateTime(validUntilStr);
		long deltaMillis = (validUntilCal.getTimeInMillis() - new Date().getTime());
		long millisInDay = 86400 * 1000;
		long minMillis = minDays * millisInDay;
		long maxMillis = maxDays * millisInDay;
		long deltaInDays = deltaMillis / millisInDay;
		//log.debug("--- validUntil: " + validUntilStr);
		//log.debug("--- validuMillis:" + validUntilCal.getTimeInMillis());
		//log.debug("--- deltaMillis: " + deltaMillis);
		log.debug("-- deltaDays: "+ deltaInDays);
		//log.debug("--- minMillis  : " + minMillis);
		//log.debug("--- maxMillis  : " + maxMillis);
		//log.debug("--- nowMillis  : " + new Date().getTime());
		if (deltaMillis < minMillis ||
				deltaMillis > maxMillis) {
			log.debug("-- validUntil not in range: " + minDays + " < " + deltaInDays + " > " + maxDays);
			return false;
		} else {
			log.debug("-- validUntil ok");
			return true;
		}
	}

}
