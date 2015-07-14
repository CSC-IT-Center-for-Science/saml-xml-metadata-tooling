package fi.funet.fi.haka.xmldiffer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
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
				Document baseEntDoc = getEntityDoc(baseDoc, entity); 
				Document compEntDoc = getEntityDoc(comparableDoc, entity); 
				Diff diff = new Diff(baseEntDoc, compEntDoc); 
				//DetailedDiff dd = new DetailedDiff(diff);
				//dd.overrideElementQualifier(null);
				if (!diff.similar()) {
					System.out.println("> " + entity);
					
					String certStr;
					NodeList nl = baseEntDoc.getElementsByTagName("ds:X509Certificate");
					for (int t = 0, l = nl.getLength(); t < l; t++) {
						certStr = nl.item(t).getTextContent();
						if (!certFound(compEntDoc, certStr)) {
							System.out.println("- cert removed: " + getCertDispStr(certStr));
					}
					nl = compEntDoc.getElementsByTagName("ds:X509Certificate");
					for (t = 0, l = nl.getLength(); t < l; t++) {
						certStr = nl.item(t).getTextContent();
						if (!certFound(baseEntDoc, certStr)) {
							System.out.println("- cert added: " + getCertDispStr(certStr));
						}
					}
					/*Iterator i = dd.getAllDifferences().iterator();
					while (i.hasNext()) {
						Difference d = (Difference) i.next();
						}*/
					}
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
	
	private static String getCertDispStr (String certStr) {
		X509Certificate cert = getCert(certStr);
		Pattern p = Pattern.compile(".*(CN=[^,]+).*");
		String prName = cert.getSubjectX500Principal().getName();
		Matcher m = p.matcher(prName);
		String str;
		if (m.matches()) {
			str = m.group(1);
		} else {
			str = prName.split(",")[0];
		}
		return str + " | NotAfter: " + cert.getNotAfter();
	}
	
	private static boolean certFound(Document doc, String certStr) {
		NodeList nl = doc.getElementsByTagName("ds:X509Certificate");
		for (int t = 0, length = nl.getLength(); t < length; t++) {
			String nodeCertStr = nl.item(t).getTextContent();
			if (Arrays.equals(
					getByteArray(certStr),
					getByteArray(nodeCertStr))) {
				return true;
			}
		}
		return false;
	}
	
	private static byte[] getByteArray(String certStr) {
		return Base64.decodeBase64(certStr);
	}
	
	private static X509Certificate getCert(String certStr) {
		CertificateFactory cf;
		byte[] dec = getByteArray(certStr); 
		try {
			cf = CertificateFactory.getInstance("X.509");
			return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(dec));
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
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
