package fi.csc.virtu.samlxmltooling.xmldiffer;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Change {
	
	private List<DiffObj> change = new ArrayList<DiffObj>();
	private ArrayList<String> addedEntities = new ArrayList<String>();
	private ArrayList<String> removedEntities = new ArrayList<String>();
	
	public void addAdded (String entity) {
		addedEntities.add(entity);
	}
	
	public void addAllAdded (ArrayList<String> added) {
		addedEntities.addAll(added);
	}
	
	public void addRemoved (String entity) {
		removedEntities.add(entity);
	}
	
	public void addAllChanges (List<DiffObj> list) {
		change.addAll(list);
	}
	
	public String changeToString() {
		StringBuffer out = new StringBuffer();
		StringBuffer tmpOut;
		if (addedEntities.size() > 0) {
			out.append("\n* Added entities:\n");
			out.append(appendList(addedEntities));
		}
		if (removedEntities.size() > 0) {
			out.append("\n* Removed entities:\n");
			out.append(appendList(removedEntities));
		}
		tmpOut = appendChange(DiffObj.ChangeType.add,
				X509Certificate.class);
		if (tmpOut.length() > 0) {
			out.append("\n* New certificates:\n");
			out.append(tmpOut);
		}
		tmpOut = appendChange(DiffObj.ChangeType.remove,
				X509Certificate.class);
		if (tmpOut.length() > 0) {
			out.append("\n* Retired certificates:\n");
			out.append(tmpOut);
		}
		tmpOut = appendChange(DiffObj.ChangeType.add,
				RequestedAttribute.class);
		if (tmpOut.length() > 0) {
			out.append("\n* New attribute requests:\n");
			out.append(tmpOut);
		}
		tmpOut = appendChange(DiffObj.ChangeType.remove,
				RequestedAttribute.class);
		if (tmpOut.length() > 0) {
			out.append("\n* Retired attribute requests:\n");
			out.append(tmpOut);
		}
		tmpOut = appendChange(DiffObj.ChangeType.add,
				SamlEndpoint.class);
		if (tmpOut.length() > 0) {
			out.append("\n* New endpoints:\n");
			out.append(tmpOut);
		}
		tmpOut = appendChange(DiffObj.ChangeType.add,
				SamlEndpoint.class);
		if (tmpOut.length() > 0) {
			out.append("\n* Retired endpoints:\n");
			out.append(tmpOut);
		}
		tmpOut = appendChange(DiffObj.ChangeType.add,
				DiscoveryEndpoint.class);
		if (tmpOut.length() > 0) {
			out.append("\n* New dsUrls:\n");
			out.append(tmpOut);
		}
		tmpOut = appendChange(DiffObj.ChangeType.remove,
				DiscoveryEndpoint.class);
		if (tmpOut.length() > 0) {
			out.append("\n* Retired dsUrls:\n");
			out.append(tmpOut);
		}
		return out.toString();
	}
	
	@Override
	public String toString() {
		StringBuffer out = new StringBuffer();
		out.append("\n* Added entities:\n");
		out.append(appendList(addedEntities));
		out.append("\n* Removed entities:\n");
		out.append(appendList(removedEntities));
		out.append("\n* New certificates:\n");
		out.append(
				appendChange(DiffObj.ChangeType.add,
						X509Certificate.class));
		out.append("\n* Retired certificates:\n");
		out.append(
				appendChange(DiffObj.ChangeType.remove,
						X509Certificate.class));
		out.append("\n* New attribute requests:\n");
		out.append(
				appendChange(DiffObj.ChangeType.add,
						RequestedAttribute.class));
		out.append("\n* Retired attribute requests:\n");
		out.append(
				appendChange(DiffObj.ChangeType.remove,
						RequestedAttribute.class));
		out.append("\n* New endpoints:\n");
		out.append(
				appendChange(DiffObj.ChangeType.add,
						SamlEndpoint.class));
		out.append("\n* Retired endpoints:\n");
		out.append(
				appendChange(DiffObj.ChangeType.remove,
						SamlEndpoint.class));
		out.append("\n* New dsUrls:\n");
		out.append(
				appendChange(DiffObj.ChangeType.add,
						DiscoveryEndpoint.class));
		out.append("\n* Retired dsUrls:\n");
		out.append(
				appendChange(DiffObj.ChangeType.remove,
						DiscoveryEndpoint.class));
		return out.toString();
	}
	
	private StringBuffer appendList(ArrayList<String> list) {
		Iterator<String> it = list.iterator();
		StringBuffer out = new StringBuffer();
		while (it.hasNext()) {
			out.append(it.next() + "\n");
		}
		return out;
	}
	
	private StringBuffer appendChange (DiffObj.ChangeType ct, Class<?> c) {
		StringBuffer out = new StringBuffer();
		Iterator<DiffObj> i = change.iterator();
		while (i.hasNext()) {
			DiffObj d = (DiffObj) i.next();
			if (d.getType() == ct) {
				if (c.equals(X509Certificate.class) &&
						X509Certificate.class.isAssignableFrom(d.getElement().getClass())) {
					out.append(d.getEntity() + " | " + getCertDispStr(
							(X509Certificate) d.getElement()));
				} else if (d.getElement().getClass().equals(c)) {
					Object o = d.getElement();
					out.append(d.getEntity() + " | " + o.toString());
				}
			}
		}
		return out;
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
		return str + " | NotAfter: " + cert.getNotAfter() + "\n";
	}

	
}
