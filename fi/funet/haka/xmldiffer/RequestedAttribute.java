package fi.funet.fi.haka.xmldiffer;

public class RequestedAttribute {

	private String name;
	private String friendlyName;
	
	public RequestedAttribute (String name, String friendlyName) {
		this.name = name;
		this.friendlyName = friendlyName;
	}

	public String getName() {
		return name;
	}
	public String getFriendlyName() {
		return friendlyName;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (!(other instanceof RequestedAttribute)) return false;
		RequestedAttribute oth = (RequestedAttribute) other;
		if (oth.getFriendlyName().equals(this.getFriendlyName()) &&
				oth.getName().equals(this.getName())) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return this.getName() + " | " + this.getFriendlyName();
	}
	
	
}
