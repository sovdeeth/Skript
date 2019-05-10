package ch.njol.skript.variables;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents Skript's list variable. For Java code, it is basically a sorted
 * map.
 */
public class ListVariable {
	
	/**
	 * Initial size of {@link #values}.
	 */
	private static final int INITIAL_ARRAY_SIZE = 4;
	
	/**
	 * When {@link #size} grows larger than this, {@link #map} is created
	 * and filled.
	 */
	private static final int MIN_MAP_SIZE = 4;
	
	/**
	 * Size of this list variable.
	 */
	private int size;
	
	/**
	 * Resizable array with contents of the list variable.
	 */
	private Object[] values;
	
	/**
	 * This list is basically an array. Implies {@link #isSorted}.
	 */
	private boolean isArray;
	
	/**
	 * Whether {@link #values} is currently in order or not.
	 */
	private boolean isSorted;
	
	/**
	 * Variable names mapped to their values. This does not exit until a random
	 * access read is performed AND {@link #MIN_MAP_SIZE} is exceeded.
	 */
	@Nullable
	private Map<String, Object> map;
	
	public ListVariable() {
		this.size = 0;
		this.values = new Object[INITIAL_ARRAY_SIZE];
		this.isArray = true; // Map not initially created
		this.isSorted = true; // No content, so in order
	}
	
	private void enlargeArray() {
		if (size < values.length) {
			return; // No need to enlarge
		}
		Object[] newValues = new Object[values.length * 2];
		System.arraycopy(values, 0, newValues, 0, size);
		values = newValues;
	}
	
	public void add(Object value) {
		enlargeArray();
		if (isArray) { // Fast path: append to array end
			values[size] = value;
		} else { // Array may be out of order, need to explicitly save name
			values[size] = new VariableEntry("" + size, value);
		}
		size++;
	}
	
	@Nullable
	public Object get(int index) {
		if (index > size) { // Value might be in map even if it is not in array
			// Bail out of fast path
		} else if (isArray) { // Fast path: read from array
			return values[index];
		} else if (size < MIN_MAP_SIZE) { // Try to find by iterating
			for (Object o : values) {
				assert o instanceof VariableEntry;
				VariableEntry entry = (VariableEntry) o;
				if (entry.getName().equals("" + index)) {
					return entry.getValue();
				}
			}
			return null;
		}
		
		// Do a map read
		assert map != null;
		return map.get("" + index);
	}
	
	@Nullable
	public Object get(String name) {
		if (isArray) { // All names are numeric indices
			try {
				int index = Integer.parseInt(name);
				return get(index);
			} catch (NumberFormatException e) {
				return null; // Everything in this list has numeric index
			}
		} else if (size < MIN_MAP_SIZE) { // Try to find the name by iterating
			for (Object o : values) {
				assert o instanceof VariableEntry;
				VariableEntry entry = (VariableEntry) o;
				if (entry.getName().equals(name)) {
					return entry.getValue();
				}
			}
			return null;
		} else { // Look up the name from map
			createMap();
			assert map != null;
			return map.get(name);
		}
	}
	
	private void createMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<>();
		for (int i = 0; i < size; i++) {
			Object value = values[i];
			if (value instanceof VariableEntry) {
				assert map != null;
				map.put(((VariableEntry) value).getName(), ((VariableEntry) value).getValue());
			} else {
				assert map != null;
				map.put("" + i, value);
			}
		}
	}
	
	public void put(int index, Object value) {
		if (isArray && index > 0) { // Fast paths
			if (index < size) { // Overwrite
				values[index] = value;
			} else { // Append
				enlargeArray();
				values[index] = value;
				size++;
			}
		} else { // Use generic put
			put("" + index, value);
		}
	}
	
	public void put(String name, Object value) {
		if (map != null) { // Map exists; add only to it
			if (map.put(name, value) == null) { // Not overwriting old value
				enlargeArray();
				// Also add it to end of array
				values[size] = new VariableEntry(name, value);
				isArray = false; // Not just numeric indices any more
				if (((VariableEntry) values[size - 1]).compareTo(((VariableEntry) values[size])) > 0) {
					isSorted = false; // Array is no longer in order
				}
				size++;
			}
		} else { // No map; add to array
			if (isArray) { // Must wrap values so they remember their indices
				for (int i = 0; i < size; i++) {
					Object val = values[i];
					assert val != null;
					values[i] = new VariableEntry("" + i, val);
				}
			}
			
			// Add value to end of array
			VariableEntry entry = new VariableEntry(name, value);
			int comp = ((VariableEntry) values[size - 1]).compareTo(entry);
			if (comp == 0) { // Found a duplicate name
				values[size - 1] = entry; // Overwrite its value
				return;
			} else if (comp > 0) { // No obvious duplicate
				isSorted = false; // Array is no longer in order
			}
			values[size] = entry;
			size++;
			
			// TODO deduplication?
		}
	}
}
