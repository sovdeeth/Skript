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
	 * This list is basically an array.
	 */
	private boolean isArray;
	
	/**
	 * Whether {@link #values} is currently in order or not.
	 */
	private boolean sorted;
	
	/**
	 * Variable names mapped to their values. This does not exit until a random
	 * access read is performed AND {@link #MIN_MAP_SIZE} is exceeded.
	 */
	@Nullable
	private Map<String, Object> map;
	
	public ListVariable() {
		this.size = 0;
		this.values = new Object[INITIAL_ARRAY_SIZE];
		this.sorted = true; // No content, so in order
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
		if (index > size) { // Need to do map read, array is not this large
			// Nothing
		} else if (isArray) { // Fast path: read from array
			return values[index];
		}
		
		// Do a map read
		createMap();
		assert map != null;
		return map.get("" + index);
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
	
	public void put(String name, Object value) {
		// Put both to array and map
		enlargeArray();
		values[size] = new VariableEntry(name, value);
		size++;
		createMap();
		assert map != null;
		map.put(name, value);
		sorted = false; // Array is not sorted after random put operation
	}
}
