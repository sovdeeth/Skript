package ch.njol.skript.variables;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
	int size;
	
	/**
	 * Resizable array with contents of the list variable.
	 */
	Object[] values;
	
	/**
	 * This list is basically a plain Java array. Implies {@link #isSorted}.
	 */
	private boolean isArray;
	
	/**
	 * Whether {@link #values} is currently in order or not.
	 */
	private boolean isSorted;
	
	/**
	 * Variable names mapped to their values. This does not exit until
	 * {@link #isArray} is false and {@link #size} is at least
	 * {@link #MIN_MAP_SIZE}.
	 */
	@Nullable
	private Map<String, Object> map;
	
	public ListVariable() {
		this.size = 0;
		this.values = new Object[INITIAL_ARRAY_SIZE];
		this.isArray = true; // Map not initially created
		this.isSorted = true; // No content, so in order
	}
	
	/**
	 * Gets how many elements this list has.
	 * @return List size.
	 */
	public int getSize() {
		return size;
	}
	
	void assertPlainArray() {
		assert isArray : "not a plain array";
		assert isSorted : "arrays are always sorted";
		assert map == null : "map is unnecessary with plain array";
	}
	
	void assertSortable() {
		assert !isArray : "plain array does not need sorting";
		assert !isSorted : "already sorted";
	}
	
	void assertSmallList() {
		assert size < MIN_MAP_SIZE : "list too large, should have map";
		assert map == null : "map is unnecessary with small list";
	}
	
	void assertMap() {
		assert map != null : "not a map";
		assert !isArray : "plain array is impossible with map";
	}
	
	void assertSorted() {
		assert isSorted : "array not sorted";
	}
	
	/**
	 * If current {@link #size} is less than length of {@link #values},
	 * enlarge {@link #values} and pad the end with nulls.
	 */
	private void enlargeArray() {
		if (size < values.length) {
			return; // No need to enlarge
		}
		Object[] newValues = new Object[values.length * 2];
		System.arraycopy(values, 0, newValues, 0, size);
		values = newValues;
	}
	
	/**
	 * Adds a value at end of this list.
	 * @param value Value to add.
	 */
	public void add(Object value) {
		enlargeArray();
		if (isArray) { // Fast path: append to array end
			assertPlainArray();
			values[size] = value;
		} else { // Array may be out of order, need to explicitly save name
			assertMap();
			String name = "" + size;
			values[size] = new VariableEntry(name, value);
			if (map != null) { // Additionally, we have a map
				map.put(name, value);
			}
		}
		size++;
	}
	
	/**
	 * Gets a value with given index.
	 * @param index Numeric index.
	 * @return Value, or null if the index does not have a value.
	 */
	@Nullable
	public Object get(int index) {
		if (index > size) { // Value might be in map even if it is not in array
			// Bail out of fast path
		} else if (isArray) { // Fast path: read from array
			assertPlainArray();
			return values[index];
		} else if (size < MIN_MAP_SIZE) { // Try to find by iterating
			assertSmallList();
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
		assertMap();
		assert map != null;
		return map.get("" + index);
	}
	
	/**
	 * Gets a value with given name.
	 * @param name Name of list member.
	 * @return Value, or null if there is no value set for given name.
	 */
	@Nullable
	public Object get(String name) {
		if (isArray) { // All names are numeric indices
			assertPlainArray();
			try {
				int index = Integer.parseInt(name);
				return get(index);
			} catch (NumberFormatException e) {
				return null; // Everything in this list has numeric index
			}
		} else if (size < MIN_MAP_SIZE) { // Try to find the name by iterating
			assertSmallList();
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
			assertMap();
			assert map != null;
			return map.get(name);
		}
	}
	
	/**
	 * Ensures that map exists.
	 */
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
	
	/**
	 * Tries to put a value to array.
	 * @param index Index to write to.
	 * @param value Value.
	 * @return Whether writing to array succeeded or not.
	 */
	private boolean putArray(int index, Object value) {
		assertPlainArray();
		if (index < size) { // Overwrite
			values[index] = value;
			return true;
		} else if (index == size) { // Append
			enlargeArray();
			values[index] = value;
			size++;
			return true;
		} else { // Can't write this to array
			return false;
		}
	}
	
	/**
	 * Puts a value to given index in this list.
	 * @param index Index.
	 * @param value The value.
	 */
	public void put(int index, Object value) {
		if (isArray && index > 0) { // Fast paths
			if (!putArray(index, value)) { // Use generic put; we have a sparse array
				put("" + index, value);
			}
		} else { // Use generic put; we don't have plain array
			put("" + index, value);
		}
	}
	
	/**
	 * Puts a value to map with given name. If it does not overwrite something,
	 * the value will also be added to list.
	 * @param name Name.
	 * @param value Value to put.
	 */
	private void putMap(String name, Object value) {
		assertMap();
		assert map != null;
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
	}
	
	/**
	 * Puts a value to this list with given name.
	 * @param name Name for the value.
	 * @param value Value to put.
	 */
	public void put(String name, Object value) {
		if (map != null) { // Map exists; add only to it
			putMap(name, value);
		} else { // No map; consider making one now
			if (isArray) { // Try to preserve plain array we have
				try { // Maybe the name is actually a numeric index
					int index = Integer.parseInt(name);
					if (index > 0) {
						if (putArray(index, value)) {
							return; // Managed to make this put just an array write
						}
					}
				} catch (NumberFormatException e) {
					// Name isn't a number, and we need a map
				}
				
				// Convert all plain values to variable entries
				for (int i = 0; i < size; i++) {
					Object val = values[i];
					assert val != null;
					values[i] = new VariableEntry("" + i, val);
				}
				isArray = false; // Definitely not a plain array, we have non-number name
			}
			
			VariableEntry entry = new VariableEntry(name, value);
			
			// Search array for duplicates, overwriting them if found
			if (size < MIN_MAP_SIZE) {
				assertSmallList();
				for (int i = 0; i < size; i++) {
					if (((VariableEntry) values[i]).compareTo(entry) == 0) {
						values[i] = entry; // Overwrite this entry
						return;
					}
				}
			}
			
			// Add to end of array
			values[size] = entry;
			if (size > 1 && ((VariableEntry) values[size - 1]).compareTo(((VariableEntry) values[size])) > 0) {
				isSorted = false; // Array is no longer in order
			}
			size++;
			
			if (size >= MIN_MAP_SIZE) { // Time to create map
				createMap(); // This will also put our new value
			}
		}
	}
	
	public Iterator<Object> unorderedIterator() {
		if (!isArray) {
			return new Iterator<Object>() {
				private int index = 0;
				
				@Override
				public Object next() {
					VariableEntry entry = (VariableEntry) values[index];
					index++;
					assert entry != null : "forgot to call hasNext()";
					return entry.getValue();
				}
				
				@Override
				public boolean hasNext() {
					return index < size;
				}
			};
		} else {
			return new Iterator<Object>() {
				private int index = 0;
				
				@Override
				public Object next() {
					Object value = values[index];
					index++;
					assert value != null : "forgot to call hasNext()";
					return value;
				}
				
				@Override
				public boolean hasNext() {
					return index < size;
				}
			};
		}
	}
	
	/**
	 * Ensures that this list is sorted. This will guarantee that next call to
	 * {@link #orderedIterator()} will be fast, provided that nothing is added
	 * to this list between call to this and it.
	 */
	public void ensureSorted() {
		if (isSorted) {
			return; // No action needed
		}
		assertSortable();
		Arrays.sort(values);
		isSorted = true;
	}
	
	public Iterator<VariableEntry> orderedIterator() {
		ensureSorted(); // We need to return elements in order
		if (!isArray) {
			return new Iterator<VariableEntry>() {
				private int index = 0;
				
				@Override
				public VariableEntry next() {
					VariableEntry entry = (VariableEntry) values[index];
					index++;
					assert entry != null : "forgot to call hasNext()";
					return entry;
				}
				
				@Override
				public boolean hasNext() {
					return index < size;
				}
			};
		} else {
			return new Iterator<VariableEntry>() {
				
				/**
				 * Next index.
				 */
				private int index = 0;
				
				@Override
				public VariableEntry next() {
					Object value = values[index];
					index++;
					assert value != null : "forgot to call hasNext()";
					return new VariableEntry("" + index, value);
				}
				
				@Override
				public boolean hasNext() {
					return index < size;
				}
			};
		}
	}
}
