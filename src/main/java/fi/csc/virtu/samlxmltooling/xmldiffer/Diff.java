package fi.csc.virtu.samlxmltooling.xmldiffer;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Diff {
	
	public static void main (String[] args) {

		DocumentBuilderFactory dbFactory =
				DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			File base = new File (args[0]);
			File comparable = new File (args[1]);
			
			Document baseDoc = dBuilder.parse(base);
			dBuilder.reset();
			Document comparableDoc = dBuilder.parse(comparable);
			dBuilder.reset();

			
			Change change = XmlDiffer.diff(baseDoc, comparableDoc);
			System.out.println(change.toString());
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

}
