package fi.csc.virtu.samlxmltooling.xmldiffer;

public class DiscoveryEndpoint {

	private String location;
	
	public DiscoveryEndpoint (String location) {
		this.location = location;
	}
	
	public String getLocation () {
		return this.location;
	}
	
	@Override
	public String toString() {
		return this.getLocation() + "\n";
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (!(other instanceof DiscoveryEndpoint)) return false;
		DiscoveryEndpoint oth = (DiscoveryEndpoint) other;
		if (oth.getLocation().equals(this.getLocation())) {
			return true;
		} else {
			return false;
		}
	}
}
