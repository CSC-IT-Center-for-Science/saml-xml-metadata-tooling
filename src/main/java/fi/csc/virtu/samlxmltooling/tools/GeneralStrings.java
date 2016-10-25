package fi.csc.virtu.samlxmltooling.tools;

public class GeneralStrings {
	
	public final static String SAML_MD_ENTITIESDESCR = "EntitiesDescriptor";
	public final static String SAML_MD_ATTR_VALIDUNTIL = "validUntil";
	public final static String SAML_MD_SIGNATURE = "Signature";
	public final static String X509 = "X.509";
	
	public final static String XPATH_FOR_MD_SIG_CERT =
			"/*[local-name()='EntitiesDescriptor']/*[local-name()='Signature']/*[local-name()='KeyInfo']/*[local-name()='X509Data']/*[local-name()='X509Certificate']/text()";
}
