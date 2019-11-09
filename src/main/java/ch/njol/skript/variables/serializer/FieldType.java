package ch.njol.skript.variables.serializer;


/**
 * Different kinds of fields that can be written.
 */
public enum FieldType {
		
	NULL(0),
	BOOLEAN(1),
	BYTE(2),
	SHORT(3),
	CHAR(4),
	INT(5),
	LONG(6),
	FLOAT(7),
	DOUBLE(8),
	OBJECT(9),
	STRING(10);
	
	public static final FieldType[] BY_ID = FieldType.values();
	
	private byte id;
	
	FieldType(int id) {
		this.id = (byte) id;
	}
	
	public byte id() {
		return id;
	}
}
