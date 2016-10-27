package fi.csc.virtu.samlxmltooling.tools;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;

import fi.csc.virtu.samlxmltooling.diffservlet.MainConfiguration;

public class CertTool {
	
	public static X509Certificate getCertFromStr(String certStr) {
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
	
	private static byte[] getByteArray(String certStr) {
		return Base64.decodeBase64(certStr);
	}
	
	public static boolean certsEqual (X509Certificate base,
			X509Certificate comp) {
		return base.equals(comp);
	}
	
	public static boolean certsEqual (String baseStr, String compStr) {
		return certsEqual(getCertFromStr(baseStr),
				getCertFromStr(compStr));
	}
	
	public static boolean certsEqual(Document doc, X509Certificate comp) {
		try {
			return certsEqual(getCertFromDoc(doc), comp);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static X509Certificate getCertFromFile(String filename) throws CertificateException, IOException {

		CertificateFactory fac = CertificateFactory.getInstance(GeneralStrings.X509);
		FileInputStream is = new FileInputStream(filename);
		X509Certificate cert = (X509Certificate) fac.generateCertificate(is);
		is.close();
		return cert;
	}
	
	public static X509Certificate getCertFromDoc (Document doc) throws XPathExpressionException {
		final XPathFactory xpFac = XPathFactory.newInstance();
		final XPath xpath = xpFac.newXPath();
		XPathExpression expr = xpath.compile(GeneralStrings.XPATH_FOR_MD_SIG_CERT);
		String certStr =
				expr.evaluate(doc);
		return getCertFromStr(certStr);
	}
	
	public static X509Certificate getFedCheckCert (String flavor, MainConfiguration conf) throws Exception {
		return getCertFromFile(conf.getFedConfStr(flavor, GeneralStrings.PROP_FED_CERTFILE));
	}
	
}
