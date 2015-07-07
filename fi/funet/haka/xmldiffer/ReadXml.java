package fi.funet.haka.xmldiffer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

//import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ReadXml {
	
	private static DocumentBuilderFactory dbFactory =
			DocumentBuilderFactory.newInstance();
	private static DocumentBuilder dBuilder;
	
	private static void init() {
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {

		init();
		File base = new File (args[0]);
		File comparable = new File (args[1]);
		
		try {
			Document baseDoc = dBuilder.parse(base);
			dBuilder.reset();
			Document comparableDoc = dBuilder.parse(comparable);
			dBuilder.reset();
			
			
			ArrayList<String> baseEntityList =
					getEntityList(baseDoc);
			ArrayList<String> comparableEntityList =
					getEntityList(comparableDoc);
			ArrayList<String> remainingEntityList =
					new ArrayList<String>();
			
			ArrayList<String> removedEntities = new ArrayList<String>();
			Iterator<String> it = baseEntityList.iterator();
			while (it.hasNext()) {
				String str = it.next();
				if (comparableEntityList.contains(str)) {
					comparableEntityList.remove(str);
					remainingEntityList.add(str);
				} else {
					removedEntities.add(str);
				}
			}
			
			if (baseEntityList.size() > 0) {
				System.out.println("\n* Removed entities:");
				printList(removedEntities);
			}
			
			if (comparableEntityList.size() > 0) {
				System.out.println("\n* Added entities:");
				printList(comparableEntityList);
			}
			
			System.out.println("\n* Changed entities:");
			it = remainingEntityList.iterator();
			while (it.hasNext()) {
				String entity = it.next();
				Diff diff = new Diff(
						getEntityDoc(baseDoc, entity),
						getEntityDoc(comparableDoc, entity)); 
				//DetailedDiff dd = new DetailedDiff(diff);
				//dd.overrideElementQualifier(null);
				if (!diff.identical()) {
					System.out.println(entity);
					/*Iterator iter = 
							dd.getAllDifferences().iterator();
					while (iter.hasNext()) {
						Difference d = (Difference) iter.next();
						System.out.println(dd.toString());
					}*/
				}
			}
						
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static ArrayList<String> getEntityList(Document doc) {
		NodeList nl = doc.getElementsByTagName("EntityDescriptor");
		ArrayList<String> list = new ArrayList<String>();
		for (int i=0, length = nl.getLength(); i < length; i++) {
			list.add(nl.item(i).getAttributes().getNamedItem("entityID").getNodeValue());
		}
		return list;
	}
	
	private static Document getEntityDoc (Document doc, String entityId) {
		NodeList nl = doc.getElementsByTagName("EntityDescriptor");
		for (int i=0, length = nl.getLength(); i < length; i++) {
			if (nl.item(i).getAttributes().getNamedItem("entityID").getNodeValue().
					equals(entityId)) {
				Document retDoc = dBuilder.newDocument(); 
				dBuilder.reset();
				retDoc.appendChild(
					retDoc.adoptNode(nl.item(i).cloneNode(true))
					);
				return retDoc;
			}
		}
		return null;
	}
	
	private static void printList(ArrayList<String> list) {
		Iterator<String> it = list.iterator();
		while (it.hasNext()) {
			System.out.println(it.next());
		}
	}

}
