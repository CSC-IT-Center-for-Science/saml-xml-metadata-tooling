package fi.funet.fi.haka.xmldiffer;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.custommonkey.xmlunit.Diff;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import fi.funet.fi.haka.xmldiffer.DiffObj.ChangeType;

public class XmlDiffer {
	
	private static DocumentBuilderFactory dbFactory =
			DocumentBuilderFactory.newInstance();
	private static DocumentBuilder dBuilder;

	public static Change diff(Document base, Document comparable) throws ParserConfigurationException {
		
		dBuilder = dbFactory.newDocumentBuilder();
		
		ArrayList<String> baseEntityList =
				getEntityList(base);
		ArrayList<String> comparableEntityList =
				getEntityList(comparable);
		ArrayList<String> remainingEntityList =
				new ArrayList<String>();

		Change change = new Change();
		
		Iterator<String> it = baseEntityList.iterator();
		while (it.hasNext()) {
			String str = it.next();
			if (comparableEntityList.contains(str)) {
				comparableEntityList.remove(str);
				remainingEntityList.add(str);
			} else {
				change.addRemoved(str);
			}
		}
		change.addAllAdded(comparableEntityList);
		
		it = remainingEntityList.iterator();
		while (it.hasNext()) {
			String entity = (String) it.next();
			Document baseEntDoc = getEntityDoc(base, entity); 
			Document compEntDoc = getEntityDoc(comparable, entity); 
			Diff diff = new Diff(baseEntDoc, compEntDoc);
			if (!diff.similar()) {
				change.addAllChanges(
						changedCertificates(
								entity, baseEntDoc, compEntDoc));
				change.addAllChanges(
						changedEndPoints(
								entity, baseEntDoc, compEntDoc));
				change.addAllChanges(
						changedAttributeRequests(
								entity, baseEntDoc, compEntDoc));

			}
		}
		return change;
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
	
	private static List<DiffObj> changedCertificates(String entity, Document baseEntDoc, Document compEntDoc) {
		List<DiffObj> diffList = new ArrayList<DiffObj>();
		String certStr;
		NodeList nl = baseEntDoc.getElementsByTagName("ds:X509Certificate");
		for (int t = 0, l = nl.getLength(); t < l; t++) {
			certStr = nl.item(t).getTextContent();
			if (!certFound(compEntDoc, certStr)) {
				diffList.add(new DiffObj(entity, ChangeType.remove, getCert(certStr)));
			}
		}
		nl = compEntDoc.getElementsByTagName("ds:X509Certificate");
		for (int t = 0, l = nl.getLength(); t < l; t++) {
			certStr = nl.item(t).getTextContent();
			if (!certFound(baseEntDoc, certStr)) {
				diffList.add(new DiffObj(entity, ChangeType.add, getCert(certStr)));
			}
		}
		return diffList;
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
			e.printStackTrace();
			return null;
		}
	}

	private static List<DiffObj> changedEndPoints (String entity, Document baseEntDoc, Document compEntDoc) {
		List<DiffObj> diffList = new ArrayList<DiffObj>();
		List<SamlEndpoint> epList = getEpList(baseEntDoc);

		Iterator<SamlEndpoint> i = epList.iterator();
		while (i.hasNext()) {
			SamlEndpoint ep = (SamlEndpoint) i.next();
			if (!endpointFound(compEntDoc, ep)) {
				diffList.add(new DiffObj(entity, ChangeType.remove, ep));
			}
		}
		
		epList = getEpList(compEntDoc);
		i = epList.iterator();
		while (i.hasNext()) {
			SamlEndpoint ep = (SamlEndpoint) i.next();
			if (!endpointFound(baseEntDoc, ep)) {
				diffList.add(new DiffObj(entity, ChangeType.add, ep));
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
	
	private static List<DiffObj> changedAttributeRequests (String entity, Document baseEntDoc, Document compEntDoc) {
		List<DiffObj> diffList = new ArrayList<DiffObj>();
		List<RequestedAttribute> raList = getRaList(baseEntDoc);
		Iterator<RequestedAttribute> i = raList.iterator();
		while (i.hasNext()) {
			RequestedAttribute ra = (RequestedAttribute) i.next();
			if (!requestedAttributeFound(compEntDoc, ra)) {
				diffList.add(new DiffObj(entity, ChangeType.remove, ra));
			}
		}
		raList = getRaList(compEntDoc);
		i = raList.iterator();
		while (i.hasNext()) {
			RequestedAttribute ra = (RequestedAttribute) i.next();
			if (!requestedAttributeFound(baseEntDoc, ra)) {
				diffList.add(new DiffObj(entity, ChangeType.add, ra));
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

	
}
