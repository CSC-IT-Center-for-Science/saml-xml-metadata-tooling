package fi.csc.virtu.samlxmltooling.xmldiffer;

public class SamlEndpoint  {
	
	private String location;
	private String binding;
	
	public SamlEndpoint (String location, String binding) {
		this.location = location;
		this.binding = binding;
	}

	public String getLocation() {
		return location;
	}
	public String getBinding() {
		return binding;
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
		return this.getBinding() + " | " + this.getLocation() + "\n";
	}
	
	

}
