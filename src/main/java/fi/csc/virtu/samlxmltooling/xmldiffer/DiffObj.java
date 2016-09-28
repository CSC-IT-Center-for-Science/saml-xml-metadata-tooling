package fi.csc.virtu.samlxmltooling.xmldiffer;

public class DiffObj {
	public enum ChangeType { add, remove, other };
	private String entity;
	private ChangeType type;
	private Object element;
	
	public DiffObj (String entity, ChangeType type, Object element) {
		this.entity = entity;
		this.type = type;
		this.element = element;
	}

	public ChangeType getType() {
		return type;
	}

	public Object getElement() {
		return element;
	}
	
	public String getEntity() {
		return entity;
	}
	
	@Override
	public String toString() {
		return type.toString() + element.getClass().getName(); 
	}
	
}
