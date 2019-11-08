package ch.njol.skript.variables;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents Skript's list variable. For Java code, it is basically a sorted
 * map.
 * 
 * <p>This class operates in one of three modes, based on data it contains.
 * 
 * <ul>
 * <li>Array mode: continuous range of numeric keys
 * <li>Small list mode: a small amount of variables in list
 * <li>Map mode: fallback; uses HashMap when possible,
 * TreeMap after call to {@link #ensureSorted()}
 * </ul>
 */
public class ListVariable {
	
	/**
	 * Set to {@link #size} when this list variable is not to be used anymore.
	 */
	static final int INVALID_LIST = -1;
	
	/**
	 * Initial size of {@link #values}.
	 */
	private static final int INITIAL_ARRAY_SIZE = 4;
	
	/**
	 * When {@link #size} grows larger than this, and this is not
	 * {@link #isArray}, the {@link #map} is created and filled.
	 */
	private static final int MIN_MAP_SIZE = 4;
	
	@SuppressWarnings("unused")
	private static void staticAssert() {
		assert INVALID_LIST < 0 : "invalid overlaps with valid";
		assert INITIAL_ARRAY_SIZE >= MIN_MAP_SIZE : "small list must fit to initial array";

	}
	static {
		staticAssert();
	}
	
	/**
	 * Size of this list variable. When this is {@link #INVALID_LIST}, this
	 * list variable should not be used anymore.
	 */
	int size;
	
	/**
	 * Resizable array with contents of the list variable. Null when
	 * {@link #map} is not present.
	 */
	@Nullable
	Object[] values;
	
	/**
	 * This list is basically a plain Java array. Implies {@link #isSorted}.
	 * When this is not set, but {@link #values}
	 * is present, this is small list.
	 */
	private boolean isArray;
	
	/**
	 * Whether {@link #values} is in order or not.
	 */
	boolean isSorted;
	
	/**
	 * Variable names mapped to their values. This does not exit until
	 * {@link #isArray} is false and {@link #size} is at least
	 * {@link #MIN_MAP_SIZE}.
	 */
	@Nullable
	private Map<String, Object> map;
	
	/**
	 * If present, the {@link VariablePath} that refers to this list also
	 * refers to a singular value. This must be possible to preserve
	 * compatibility with previous variable system Skript had.
	 */
	@Nullable
	Object shadowValue;
	
	public ListVariable() {
		this.size = 0;
		this.values = new Object[INITIAL_ARRAY_SIZE];
		this.isArray = true; // Map not initially created
		this.isSorted = true; // No content, so in order
		assertValid();
		assertPlainArray();
	}
	
	/**
	 * Checks whether this list variable is valid. It is only necessary to call
	 * this when this has been cached and could've been deleted.
	 * @return Whether this list is valid.
	 */
	public boolean isValid() {
		return size != INVALID_LIST;
	}
	
	void invalidate() {
		size = INVALID_LIST; // Set invalid marker
		
		// Free contents to GC
		values = null;
		map = null;
	}
	
	/**
	 * Gets how many elements this list has.
	 * @return List size.
	 */
	public int getSize() {
		return size;
	}
	
	void assertValid() {
		assert size != INVALID_LIST : "invalid list, caching is not working";
	}
	
	void assertPlainArray() {
		assert isArray : "not a plain array";
		assert values != null : "plain array without array";
		assert map == null : "map is unnecessary with plain array";
	}
	
	void assertSparseArray() {
		assertPlainArray();
		assert !isSorted : "not sparse array";
	}
	
	void assertSmallList() {
		assert size < MIN_MAP_SIZE : "list too large, should have map";
		assert values != null : "list without array";
		assert map == null : "map is unnecessary with small list";
	}
	
	void assertMap() {
		assert map != null : "not a map";
		assert values == null : "array is unnecessary with map";
		assert !isArray : "plain array is impossible with map";
	}
	
	/**
	 * If current {@link #size} is less than length of {@link #values},
	 * enlarge {@link #values} and pad the end with nulls.
	 */
	private void enlargeArray() {
		Object[] vals = values;
		assert vals != null;
		if (size < vals.length) {
			return; // No need to enlarge
		}
		Object[] newValues = new Object[vals.length * 2];
		System.arraycopy(vals, 0, newValues, 0, size);
		values = newValues;
	}
	
	/**
	 * Adds a value at end of this list.
	 * @param value Value to add.
	 */
	public void add(Object value) {
		assertValid();
		if (isArray) { // Fast path: append to array end
			assertPlainArray();
			enlargeArray();
			assert values != null;
			values[size] = value;
			size++;
		} else { // Array may be out of order, need to explicitly save name
			String name = "" + size;
			if (map != null) { // Additionally, we have a map
				assertMap();
				assert map != null;
				map.put(name, value);
			} else {
				assertSmallList();
				if (size + 1 == MIN_MAP_SIZE ) { // Hit minimum map size, create map
					createMap();
					assert map != null;
					map.put(name, value);
				} else { // Add to small list
					assert values != null;
					values[size] = new VariableEntry(name, value);
					size++;
				}
			}
		}
	}
	
	/**
	 * Gets a value with given index.
	 * @param index Numeric index.
	 * @return Value, or null if the index does not have a value.
	 */
	@Nullable
	public Object get(int index) {
		assertValid();
		if (isArray) { // Fast path: read from array
			assertPlainArray();
			if (index < 0 || index >= size) {
				return null; // Out of range, doesn't exist
			}
			assert values != null;
			return values[index];
		} else if (size < MIN_MAP_SIZE) { // Try to find by iterating
			assertSmallList();
			assert values != null;
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
		assertValid();
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
			for (int i = 0; i < size; i++) {
				assert values != null;
				VariableEntry entry = (VariableEntry) values[i];
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
			assert values != null;
			Object value = values[i];
			if (value instanceof VariableEntry) {
				assert map != null;
				map.put(((VariableEntry) value).getName(), ((VariableEntry) value).getValue());
			} else {
				assert map != null;
				map.put("" + i, value);
			}
		}
		
		// Clear the array; it won't be needed anymore
		values = null;
		assertMap();
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
			assert values != null;
			values[index] = value;
			return true;
		} else if (index == size) { // Append
			enlargeArray();
			assert values != null;
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
		assertValid();
		if (isArray && index >= 0) { // Fast paths
			if (!putArray(index, value)) { // Use generic put; we have a sparse array
				put("" + index, value);
			}
		} else { // Use generic put; we don't have plain array
			put("" + index, value);
		}
	}
	
	/**
	 * Puts a value to this list with given name.
	 * @param name Name for the value.
	 * @param value Value to put.
	 */
	public void put(String name, Object value) {
		assertValid();
		if (map != null) { // Map exists; add only to it
			map.put(name, value);
		} else { // No map; consider making one now
			Object[] vals = values;
			assert vals != null;
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
				
				// See if we can append and still keep small list
				if (size + 1 < MIN_MAP_SIZE) {
					// Convert all plain values to variable entries
					for (int i = 0; i < size; i++) {
						Object val = vals[i];
						assert val != null;
						vals[i] = new VariableEntry("" + i, val);
					}
					isArray = false; // Definitely not a plain array, we have non-number name
					
					// Given name was not array index, so it can't be duplicate here
					
					// Append to list
					vals[size] = new VariableEntry(name, value);
					size++;
					assertSmallList();
					return; // Fast path ok: didn't check for duplicates
				}
			}
			
			VariableEntry entry = new VariableEntry(name, value);
			
			// Search array for duplicates, overwriting them if found
			if (size < MIN_MAP_SIZE) {
				assertSmallList();
				for (int i = 0; i < size; i++) {
					if (((VariableEntry) vals[i]).compareTo(entry) == 0) {
						vals[i] = entry; // Overwrite this entry
						return;
					}
				}
				
				// New name to add
				if (size + 1 < MIN_MAP_SIZE) { // Check if this is still small list
					// Add to end of array
					vals[size] = entry;
					if (size > 1 && ((VariableEntry) vals[size - 1]).compareTo(((VariableEntry) vals[size])) > 0) {
						isSorted = false; // Array is no longer in order
					}
					size++;
					return;
				}
			}
			
			createMap();
			assertMap();
			assert map != null;
			map.put(name, value);
		}
	}
	
	@Nullable
	public Object remove(int index) {
		if (isArray && index >= 0 && index < size) { // Fast path: array
			assertPlainArray();
			assert values != null;
			Object oldValue = values[index] = null;
			if (index == size - 1) { // Removed last
				size--;
			} else { // Sparse array
				isSorted = false;
			}
			return oldValue;
		} else { // Delegate to generic remove
			return remove("" + index);
		}
	}
	
	@Nullable
	public Object remove(String name) {
		assertValid();
		if (isArray) { // Fast path: remove from array
			assertPlainArray();
			int index;
			try {
				index = Integer.parseInt(name);
			} catch (NumberFormatException e) {
				return null; // Nothing to remove
			}
			assert values != null;
			Object oldValue = values[index];
			assert values != null;
			values[index] = null;
			if (index == size - 1) { // Removed last
				index--;
			} else { // Sparse array
				isSorted = false;
			}
			return oldValue;
		} else if (size < MIN_MAP_SIZE) { // Small list; iterate, remove and don't leave sparse
			assertSmallList();
			Object[] vals = values;
			assert vals != null;
			for (int i = 0; i < size; i++) {
				VariableEntry entry = (VariableEntry) vals[i];
				if (name.equals(entry.getName())) {
					Object oldValue = vals[i];
					// Remove everything but last by overwriting with next
					for (int j = i + 1; j < size; j++) {
						vals[i] = vals[j];
						i++;
					}
					size--;
					vals[size] = null; // Free last for GC
					return oldValue;
				}
			}
			return null; // Nothing found with that name
		} else { // Remove from map
			assertMap();
			assert map != null;
			return map.remove(name);
		}
	}
	
	public Iterator<Object> unorderedValues() {
		assertValid();
		if (map != null) { // Map iterator
			Iterator<Object> it = map.values().iterator();
			assert it != null;
			return it;
		} else {
			return new Iterator<Object>() {
				private int index = 0;
				private int removeIndex;
				
				@Override
				public Object next() {
					removeIndex = index; // Back up for remove()
					Object[] vals = values;
					assert vals != null : "mutation in list variable (next)";
					// Skip nulls in sparse array
					for (Object value = vals[index]; index < size; value = vals[index]) {
						index++;
						if (value != null) {
							if (value instanceof VariableEntry) {
								return ((VariableEntry) value).getValue();
							}
							return value;
						}
					}
					throw new NoSuchElementException(); // User forgot to call hasNext
				}
				
				@Override
				public boolean hasNext() {
					return index < size;
				}
				
				@Override
				public void remove() {
					assert values != null : "mutation in list variable (remove)";
					values[removeIndex] = null; // Remove from array
					if (removeIndex == size - 1) { // Removed last
						size--;
					} else { // Sparse array
						isSorted = false;
					}
				}
			};
		}
	}
	
	public Iterator<Object> orderedValues() {
		ensureSorted();
		return unorderedValues();
	}
	
	/**
	 * Ensures that this list is sorted. This will guarantee that next call to
	 * {@link #orderedValues()} will be fast, but may reduce performance of
	 * future random access operations.
	 */
	public void ensureSorted() {
		assertValid();
		if (isSorted) { // Ignore sparse plain arrays
			return; // No action needed
		}
		map = new TreeMap<>(map);
		isSorted = true;
	}
	
	/**
	 * Compacts this list, if possible. This may reduce memory usage if
	 * compaction can be performed.
	 */
	public void compact() {
		if (isSorted) {
			return;
		}
		assertSparseArray();
		Object[] vals = values;
		assert vals != null;
		int nullCount = 0;
		for (int i = 0; i < size; i++) {
			if (vals[i] == null) {
				nullCount++;
			} else {
				vals[i - nullCount] = vals[i];
			}
		}
		isSorted = true;
	}

	/**
	 * Returns shadow value of this list, if one exists.
	 * @return Shadow value or null.
	 */
	@Nullable
	public Object getShadowValue() {
		return shadowValue;
	}
}
