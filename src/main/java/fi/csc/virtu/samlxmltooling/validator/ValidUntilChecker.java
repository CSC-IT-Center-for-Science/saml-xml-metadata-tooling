package fi.csc.virtu.samlxmltooling.validator;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import fi.csc.virtu.samlxmltooling.tools.GeneralStrings;

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
		LocalDate validUntilLocal = validUntilCal.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		
		long deltaDays = ChronoUnit.DAYS.between(LocalDate.now(), validUntilLocal);
		
		if (deltaDays > minDays &&
				deltaDays < maxDays ) {
			log.debug("-- validUntil ok");
			return true;
		} else {
			log.debug("-- validUntil not in range: " + minDays + " < " + deltaDays + " > " + maxDays);
			return false;
		}
		
	}

}
