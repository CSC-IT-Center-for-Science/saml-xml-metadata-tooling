package fi.csc.virtu.samlxmltooling.validator;

import java.util.List;

import org.custommonkey.xmlunit.Diff;
import org.w3c.dom.Document;

public class Comparison {
	
	public static boolean identical(List<Document> compList) {
		Document base = compList.get(0);
		for (Document doc: compList.subList(1, compList.size())) {
			Diff diff = new Diff(base, doc);
			if (!diff.identical()) {
				return false;
			}
		}
		return true;
	}

}
