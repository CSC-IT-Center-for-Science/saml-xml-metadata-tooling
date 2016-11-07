package fi.csc.virtu.samlxmltooling.validator;

import java.util.List;

import org.custommonkey.xmlunit.Diff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class Comparison {

	private static Logger log = LoggerFactory.getLogger(Comparison.class);
	
	public static boolean identical(List<Document> compList) {
		Document base = compList.get(0);
		int count = 0;
		for (Document doc: compList.subList(1, compList.size())) {
			Diff diff = new Diff(base, doc);
			count++;
			if (!diff.identical()) {
				log.debug("-- # of compared docs: " + count);
				return false;
			}
		}
		log.debug("-- # of compared docs: " + count);
		return true;
	}

}
