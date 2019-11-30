package ch.njol.skript.variables;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a variable, possibly contained in a list.
 */
public class VariableEntry implements Comparable<VariableEntry> {

	/**
	 * Variable name.
	 */
	private final String name;
	
	/**
	 * Variable value.
	 */
	@Nullable
	private final Object value;
	
	public VariableEntry(String name, @Nullable Object value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	
	@Nullable
	public Object getValue() {
		return value;
	}

	@Override
	public int compareTo(@Nullable VariableEntry o) {
		if (o == null) {
			return -1;
		}
		return name.compareTo(o.name);
	}
	
}
