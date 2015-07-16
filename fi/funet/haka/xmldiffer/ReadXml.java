package fi.funet.fi.haka.xmldiffer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.NodeDetail;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fi.funet.fi.haka.xmldiffer.DiffObj.ChangeType;

public class ReadXml {
	
	private static DocumentBuilderFactory dbFactory =
			DocumentBuilderFactory.newInstance();
	private static DocumentBuilder dBuilder;
	private static List<DiffObj> change = new ArrayList<DiffObj>();
	
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
			
			appendRemaining(remainingEntityList, baseDoc, comparableDoc);

			if (comparableEntityList.size() > 0) {
				System.out.println("\n* Added entities:");
				printList(comparableEntityList);
			}

			if (baseEntityList.size() > 0) {
				System.out.println("\n* Removed entities:");
				printList(removedEntities);
			}
			
			
			
			System.out.println("\n* New certificates:");
			printCerts(DiffObj.ChangeType.add);
			System.out.println("\n* Retired certificates:");
			printCerts(DiffObj.ChangeType.remove);
			System.out.println("\n* New attribute requests:");
			printAttrs(DiffObj.ChangeType.add);
			System.out.println("\n* Retired attribute requests:");
			printAttrs(DiffObj.ChangeType.remove);
			System.out.println("\n* New endpoints:");
			printEndpoints(DiffObj.ChangeType.add);
			System.out.println("\n* Retired endpoints:");
			printEndpoints(DiffObj.ChangeType.remove);
						
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void printCerts(DiffObj.ChangeType ct) {
		Iterator<DiffObj> i = change.iterator();
		while (i.hasNext()) {
			DiffObj d = (DiffObj) i.next();
			if (d.getType() == ct &&
				d.getElement() instanceof X509Certificate) {
					System.out.println(d.getEntity() + " : " + getCertDispStr((X509Certificate) d.getElement()));
				}
		}
	}
	
	private static void printAttrs(DiffObj.ChangeType ct) {
		Iterator<DiffObj> i = change.iterator();
		while (i.hasNext()) {
			DiffObj d = (DiffObj) i.next();
			if (d.getType() == ct &&
				d.getElement() instanceof RequestedAttribute) {
				RequestedAttribute rt = (RequestedAttribute) d.getElement();
				System.out.println(d.getEntity() + " : " + rt.toString());
			}
		}
	}
	
	private static void printEndpoints(DiffObj.ChangeType ct) {
		Iterator<DiffObj> i = change.iterator();
		while (i.hasNext()) {
			DiffObj d = (DiffObj) i.next();
			if (d.getType() == ct &&
				d.getElement() instanceof SamlEndpoint) {
				SamlEndpoint e = (SamlEndpoint) d.getElement();
				System.out.println(d.getEntity() + " : " + e.toString());
			}
		}
	}

	private static void appendRemaining (List<String> remainingEntityList, Document baseDoc, Document comparableDoc) {
		Iterator<String> it = remainingEntityList.iterator();
		while (it.hasNext()) {
			String entity = (String) it.next();
			Document baseEntDoc = getEntityDoc(baseDoc, entity); 
			Document compEntDoc = getEntityDoc(comparableDoc, entity); 
			Diff diff = new Diff(baseEntDoc, compEntDoc); 
			//DetailedDiff dd = new DetailedDiff(diff);
			//dd.overrideElementQualifier(null);
			
			/*String[] blackList = {"KeyDescriptor",
					"AssertionConsumerService",
					"SingleSignOnService",
					"RequestedAttribute"};*/
			if (!diff.similar()) {
				//System.out.println("> " + entity);
			
				change.addAll(changedCertificates(entity, baseEntDoc, compEntDoc));
				change.addAll(changedEndPoints(entity, baseEntDoc, compEntDoc));
				change.addAll(changedAttributeRequests(entity, baseEntDoc, compEntDoc));

				/*Iterator<Difference> i = dd.getAllDifferences().iterator();
				String previousLocation = "";
				while (i.hasNext()) {
					Difference d = (Difference) i.next();
				
					if (d.getControlNodeDetail().getNode() != null) {
						NodeDetail detail = d.getControlNodeDetail();
						printChangeDetail(detail, blackList, previousLocation);
						previousLocation = detail.getXpathLocation();
					} else if (d.getTestNodeDetail().getNode() != null) {
						NodeDetail detail = d.getTestNodeDetail();
						printChangeDetail(detail, blackList, previousLocation);
						previousLocation = detail.getXpathLocation();
					}
				}*/
			}
		}
		
	}
	
	private static void printChangeDetail(NodeDetail nodeDet, String[] blackList, String previousLocation) {
		if (previousLocation.isEmpty() ||
				nodeDet.getXpathLocation().contains(previousLocation)) return;
		boolean contains = false;
		String loc = nodeDet.getXpathLocation();
		for (String black: blackList) {
			if (loc.contains(black)) {
				contains = true;
				break;
			}
		}
		if (!contains) {
			System.out.println("- " + nodeDet.getXpathLocation());
		}
	}
	
	private static List<DiffObj> changedAttributeRequests (String entity, Document baseEntDoc, Document compEntDoc) {
		List<DiffObj> diffList = new ArrayList<DiffObj>();
		List<RequestedAttribute> raList = getRaList(baseEntDoc);
		Iterator<RequestedAttribute> i = raList.iterator();
		while (i.hasNext()) {
			RequestedAttribute ra = (RequestedAttribute) i.next();
			if (!requestedAttributeFound(compEntDoc, ra)) {
				diffList.add(new DiffObj(entity, ChangeType.remove, ra));
				//ystem.out.println("- attributeRequest removed: " + ra.toString());
			}
		}
		raList = getRaList(compEntDoc);
		i = raList.iterator();
		while (i.hasNext()) {
			RequestedAttribute ra = (RequestedAttribute) i.next();
			if (!requestedAttributeFound(baseEntDoc, ra)) {
				diffList.add(new DiffObj(entity, ChangeType.add, ra));
				//System.out.println("- attributeRequest added: " + ra.toString());
			}
		}
		return diffList;
	}
	
	private static boolean requestedAttributeFound(Document doc, RequestedAttribute ra) {
		List<RequestedAttribute> raList = getRaList(doc);
		if (raList.contains(ra)) {
			return true;
		} else {
			return false;
		}
	}
	
	private static List<RequestedAttribute> getRaList(Document doc) {
		List<RequestedAttribute> raList = new ArrayList<RequestedAttribute>();
		if (spFound(doc)) {
			NodeList nl = doc.getElementsByTagName("RequestedAttribute");
			for (int t = 0, l=nl.getLength(); t<l; t++) {
				NamedNodeMap nm = nl.item(t).getAttributes();
				RequestedAttribute ra = new RequestedAttribute(nm.getNamedItem("Name").getNodeValue(),
						nm.getNamedItem("FriendlyName").getNodeValue());
				raList.add(ra);
			}
		}
		return raList;
	}
	
	private static List<DiffObj> changedEndPoints (String entity, Document baseEntDoc, Document compEntDoc) {
		List<DiffObj> diffList = new ArrayList<DiffObj>();
		List<SamlEndpoint> epList = getEpList(baseEntDoc);

		Iterator<SamlEndpoint> i = epList.iterator();
		while (i.hasNext()) {
			SamlEndpoint ep = (SamlEndpoint) i.next();
			if (!endpointFound(compEntDoc, ep)) {
				diffList.add(new DiffObj(entity, ChangeType.remove, ep));
				//System.out.println("- endpoint removed: " + ep.toString());
			}
		}
		
		epList = getEpList(compEntDoc);
		i = epList.iterator();
		while (i.hasNext()) {
			SamlEndpoint ep = (SamlEndpoint) i.next();
			if (!endpointFound(baseEntDoc, ep)) {
				diffList.add(new DiffObj(entity, ChangeType.add, ep));
				//System.out.println("- endpoint added  : " + ep.toString());
			}
		}
		return diffList;
	}
	
	private static boolean endpointFound(Document compEntDoc, SamlEndpoint ep) {
		List<SamlEndpoint> epList = getEpList(compEntDoc);
		if (epList.contains(ep)) {
			return true;
		} else {
			return false;
		}
	}
	
	private static List<SamlEndpoint> getEpList (Document doc) {
		List<SamlEndpoint> epList = new ArrayList<SamlEndpoint>();
		if (spFound(doc)) {
			NodeList nl = doc.getElementsByTagName("AssertionConsumerService");
			epList.addAll(getEpList(nl));
		}
		if (idpFound(doc)) {
			NodeList nl = doc.getElementsByTagName("SingleSignOnService");
			epList.addAll(getEpList(nl));
		}
		return epList;
	}
	
	private static List<SamlEndpoint> getEpList (NodeList nl) {
		List<SamlEndpoint> epList = new ArrayList<SamlEndpoint>();
		for (int t = 0, l = nl.getLength(); t < l; t++) {
			NamedNodeMap ml = nl.item(t).getAttributes();
			SamlEndpoint el = new SamlEndpoint(ml.getNamedItem("Location").getNodeValue(), 
					ml.getNamedItem("Binding").getNodeValue());
			epList.add(el);
		}
		return epList;
	}
	
	private static boolean spFound(Document doc) {
		return doc.getElementsByTagName("SPSSODescriptor").getLength() > 0;
	}
	
	private static boolean idpFound(Document doc) {
		return doc.getElementsByTagName("IDPSODescriptor").getLength() > 0;
	}
	
	private static List<DiffObj> changedCertificates(String entity, Document baseEntDoc, Document compEntDoc) {
		List<DiffObj> diffList = new ArrayList<DiffObj>();
		String certStr;
		NodeList nl = baseEntDoc.getElementsByTagName("ds:X509Certificate");
		for (int t = 0, l = nl.getLength(); t < l; t++) {
			certStr = nl.item(t).getTextContent();
			if (!certFound(compEntDoc, certStr)) {
				diffList.add(new DiffObj(entity, ChangeType.remove, getCert(certStr)));
				//System.out.println("- cert removed: " + getCertDispStr(certStr));
			}
		}
		nl = compEntDoc.getElementsByTagName("ds:X509Certificate");
		for (int t = 0, l = nl.getLength(); t < l; t++) {
			certStr = nl.item(t).getTextContent();
			if (!certFound(baseEntDoc, certStr)) {
				diffList.add(new DiffObj(entity, ChangeType.add, getCert(certStr)));
				//System.out.println("- cert added: " + getCertDispStr(certStr));
			}
		}
		return diffList;
	}
	
	private static String getCertDispStr (X509Certificate cert) {
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
	
	private static String getCertDispStr (String certStr) {
		return getCertDispStr(getCert(certStr));
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
