package fi.csc.virtu.samlxmltooling.validator;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.Reference;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.signature.reference.ReferenceData;
import org.apache.xml.security.signature.reference.ReferenceSubTreeData;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.transforms.Transforms;
import org.opensaml.core.config.InitializationException;
import org.opensaml.xmlsec.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import net.shibboleth.tool.xmlsectool.InitializationSupport;
import net.shibboleth.tool.xmlsectool.ReturnCode;
import net.shibboleth.tool.xmlsectool.Terminator;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

public class SigChecker {
	
	final static Logger log  = LoggerFactory.getLogger(SigChecker.class);
	
	public static boolean checkSig (Document doc,
			X509Certificate checkCert) {

        try {
            InitializationSupport.initialize();
        } catch (final InitializationException e) {
            log.error("Unable to initialize OpenSAML library", e);
            return false;
        }
		
		log.debug("-- start checking signature");
        final Element signatureElement = getSignatureElement(doc);
        if (signatureElement == null) {
        	final String message = "Signature required but XML document is not signed"; 
            log.error(message);
            return false;
            
        }
        //log.debug("XML document contained Signature element\n{}", SerializeSupport.prettyPrintXML(signatureElement));

        log.debug("Creating XML security library XMLSignature object");
        final XMLSignature signature;
        try {
            signature = new XMLSignature(signatureElement, "");
        } catch (final XMLSecurityException e) {
            log.error("Unable to read XML signature", e);
            return false;
        }

        if (signature.getObjectLength() != 0) {
        	final String message = "Signature contained an Object element, this is not allowed";
            log.error(message);
            return false;
        }
        
        final Reference ref = extractReference(signature);
        markIdAttribute(doc.getDocumentElement(), ref);
        
        try {
            Key verificationKey = checkCert.getPublicKey();
            log.debug("Verifying XML signature with key\n{}", Base64.encodeBase64String(verificationKey.getEncoded()));
            if (signature.checkSignatureValue(verificationKey)) {
                /*
                 * Now that the signature has been verified, we need to check that the
                 * XML signature layer resolved the reference to the correct element
                 * (always the document element) and that only appropriate transforms have
                 * been applied.
                 * 
                 * Note that we need to re-extract the reference from the signature at
                 * this point, we can't use one from before the signature validation.
                 */
                validateSignatureReference(doc, extractReference(signature));
                log.debug("XML document signature verified.");
                return true;
            } else {
            	final String message = "XML document signature verification failed"; 
                log.error(message);
                return false;
            }
        } catch (final XMLSignatureException e) {
            log.error("XML document signature verification failed with an error", e);
            return false;
        }

	}
	
    /**
     * Gets the signature element from the document. The signature must be a child of the document root.
     * 
     * @param xmlDoc document from which to pull the signature
     * 
     * @return the signature element, or null
     */
    protected static Element getSignatureElement(final Document xmlDoc) {
        final List<Element> sigElements =
                ElementSupport
                        .getChildElementsByTagNameNS(xmlDoc.getDocumentElement(),
                                Signature.DEFAULT_ELEMENT_NAME.getNamespaceURI(),
                                Signature.DEFAULT_ELEMENT_NAME.getLocalPart());

        if (sigElements.isEmpty()) {
            return null;
        }

        if (sigElements.size() > 1) {
            log.error("XML document contained more than one signature, unable to process");
            throw new Terminator(ReturnCode.RC_SIG);
        }

        return sigElements.get(0);
    }

    /**
     * Reconcile the given reference with the document element, by making sure that
     * the appropriate attribute is marked as an ID attribute.
     * 
     * @param docElement document element whose appropriate attribute should be marked
     * @param reference reference which references the document element
     */
    protected static void markIdAttribute(final Element docElement, final Reference reference) {
        final String referenceUri = reference.getURI();
        
        /*
         * If the reference is empty, it implicitly references the document element
         * and no attribute is being referenced.
         */
        if (referenceUri == null || referenceUri.trim().isEmpty()) {
            log.debug("reference was empty; no ID marking required");
            return;
        }
        
        /*
         * If something has already identified an ID element, don't interfere
         */
        if (AttributeSupport.getIdAttribute(docElement) != null ) {
            log.debug("document element already has an ID attribute");
            return;
        }

        /*
         * The reference must be a fragment reference, from which we extract the
         * ID value.
         */
        if (!referenceUri.startsWith("#")) {
            log.error("Signature Reference URI was not a document fragment reference: " + referenceUri);
            throw new Terminator(ReturnCode.RC_SIG);
        }
        final String id = referenceUri.substring(1);

        /*
         * Now look for the attribute which holds the ID value, and mark it as the ID attribute.
         */
        final NamedNodeMap attributes = docElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            final Attr attribute = (Attr) attributes.item(i);
            if (id.equals(attribute.getValue())) {
                log.debug("marking ID attribute {}", attribute.getName());
                docElement.setIdAttributeNode(attribute, true);
                return;
            }
        }
        
        /*
         * No attribute on the document element has the referenced ID value.
         * Signature validation will fail later, but let's give a warning here
         * as well to help people debug their signature code.
         */
        log.warn("did not find a document element attribute with value '{}'", id);
    }

    /**
     * Extract the reference within the provided XML signature while ensuring that there
     * is only one such reference.
     * 
     * @param signature signature to extract the reference from
     * @return the extracted reference
     */
    protected static Reference extractReference(final XMLSignature signature) {
        final int numReferences = signature.getSignedInfo().getLength();
        if (numReferences != 1) {
            log.error("Signature SignedInfo had invalid number of References: " + numReferences);
            throw new Terminator(ReturnCode.RC_SIG);
        }

        final Reference ref;
        try {
            ref = signature.getSignedInfo().item(0);
        } catch (final XMLSecurityException e) {
            log.error("Apache XML Security exception obtaining Reference", e);
            throw new Terminator(ReturnCode.RC_SIG);
        }
        if (ref == null) {
            log.error("Signature Reference was null");
            throw new Terminator(ReturnCode.RC_SIG);
        }
        return ref;
    }
    
    /**
     * Validates the reference within the XML signature by performing the following checks.
     * <ul>
     * <li>check that the XML signature layer resolves that reference to the same element as the DOM layer does</li>
     * <li>check that only enveloped and, optionally, exclusive canonicalization transforms are used</li>
     * </ul>
     * 
     * @param xmlDocument current XML document
     * @param ref reference to be verified
     */
    protected static void validateSignatureReference(final Document xmlDocument, final Reference ref) {
        validateSignatureReferenceUri(xmlDocument, ref);
        validateSignatureTransforms(ref);
    }

    /**
     * Validates that the element resolved by the signature validation layer is the same as the
     * element resolved by the DOM layer.
     * 
     * @param xmlDocument the signed document
     * @param reference the reference to be validated
     */
    protected static void validateSignatureReferenceUri(final Document xmlDocument, final Reference reference) {
        final ReferenceData refData = reference.getReferenceData();
        if (refData instanceof ReferenceSubTreeData) {
            final ReferenceSubTreeData subTree = (ReferenceSubTreeData) refData;
            final Node root = subTree.getRoot();
            Node resolvedSignedNode = root;
            if (root.getNodeType() == Node.DOCUMENT_NODE) {
                resolvedSignedNode = ((Document)root).getDocumentElement();
            }

            final Element expectedSignedNode = xmlDocument.getDocumentElement();

            if (!expectedSignedNode.isSameNode(resolvedSignedNode)) {
                log.error("Signature Reference URI \"" + reference.getURI()
                        + "\" was resolved to a node other than the document element");
                throw new Terminator(ReturnCode.RC_SIG);
            }
        } else {
            log.error("Signature Reference URI did not resolve to a subtree");
            throw new Terminator(ReturnCode.RC_SIG);
        }
    }

    /**
     * Validate the transforms included in the Signature Reference.
     * 
     * The Reference may contain at most 2 transforms. One of them must be the Enveloped signature transform. An
     * Exclusive Canonicalization transform (with or without comments) may also be present. No other transforms are
     * allowed.
     * 
     * @param reference the Signature reference containing the transforms to evaluate
     */
    protected static void validateSignatureTransforms(final Reference reference) {
        Transforms transforms = null;
        try {
            transforms = reference.getTransforms();
        } catch (final XMLSecurityException e) {
            log.error("Apache XML Security error obtaining Transforms instance", e);
            throw new Terminator(ReturnCode.RC_SIG);
        }

        if (transforms == null) {
            log.error("Error obtaining Transforms instance, null was returned");
            throw new Terminator(ReturnCode.RC_SIG);
        }

        final int numTransforms = transforms.getLength();
        if (numTransforms > 2) {
            log.error("Invalid number of Transforms was present: " + numTransforms);
            throw new Terminator(ReturnCode.RC_SIG);
        }

        boolean sawEnveloped = false;
        for (int i = 0; i < numTransforms; i++) {
            Transform transform = null;
            try {
                transform = transforms.item(i);
            } catch (final TransformationException e) {
                log.error("Error obtaining transform instance", e);
                throw new Terminator(ReturnCode.RC_SIG);
            }
            final String uri = transform.getURI();
            if (Transforms.TRANSFORM_ENVELOPED_SIGNATURE.equals(uri)) {
                log.debug("Saw Enveloped signature transform");
                sawEnveloped = true;
            } else if (Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS.equals(uri)
                    || Transforms.TRANSFORM_C14N_EXCL_WITH_COMMENTS.equals(uri)) {
                log.debug("Saw Exclusive C14N signature transform");
            } else {
                log.error("Saw invalid signature transform: " + uri);
                throw new Terminator(ReturnCode.RC_SIG);
            }
        }

        if (!sawEnveloped) {
            log.error("Signature was missing the required Enveloped signature transform");
            throw new Terminator(ReturnCode.RC_SIG);
        }
    }


}
