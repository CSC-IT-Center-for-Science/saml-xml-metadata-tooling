package fi.csc.virtu.samlxmltooling.tools;

public class GeneralStrings {
	
	public final static String SAML_MD_ENTITIESDESCR = "EntitiesDescriptor";
	public final static String SAML_MD_ATTR_VALIDUNTIL = "validUntil";
	public final static String SAML_MD_SIGNATURE = "Signature";
	public final static String X509 = "X.509";
	
	public final static String PROP_FED_PREFIX = "my.validator.federationSpecs.";
	
	public final static String PROP_FED_URL = "mdUrl";
	public final static String PROP_FED_CERTFILE = "mdCertFile";
	public final static String PROP_FED_VALIDUNTIL_MIN = "validUntilMin";
	public final static String PROP_FED_VALIDUNTIL_MAX = "validUntilMax";
	public final static String PROP_FED_SIGCERTVALID_MIN = "sigCertValidMin";
	public final static String PROP_FED_SIGCERTVALID_MAX = "sigCertValidMax";
	
	public final static String PROP_FED_PUBLISH_FILE = "publishFile";
	
	public final static String XPATH_FOR_MD_SIG_CERT =
			"/*[local-name()='EntitiesDescriptor']/*[local-name()='Signature']/*[local-name()='KeyInfo']/*[local-name()='X509Data']/*[local-name()='X509Certificate']/text()";
}
