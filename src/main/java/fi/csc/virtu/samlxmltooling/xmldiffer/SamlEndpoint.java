package fi.csc.virtu.samlxmltooling.xmldiffer;

public class SamlEndpoint  {
	
	public enum SamlEndpointType {
		ACS, SSS, SLS
	}
	
	private String location;
	private String binding;
	private SamlEndpointType myType;
	
	public SamlEndpoint (String location, String binding, SamlEndpointType type) {
		this.location = location;
		this.binding = binding;
		this.myType = type;
	}

	public String getLocation() {
		return location;
	}
	public String getBinding() {
		return binding;
	}
	public SamlEndpointType getType() {
		return myType;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (!(other instanceof SamlEndpoint)) return false;
		SamlEndpoint oth = (SamlEndpoint) other;
		if (oth.getBinding().equals(this.getBinding()) &&
				oth.getLocation().equals(this.getLocation())) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return this.getType().toString() + " | "
				+ this.getBinding() + " | "
				+ this.getLocation() + "\n";
	}
	
	

}
