package ch.njol.skript.variables;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.variables.storage.VariableStorage;

/**
 * A simple variable scope that can be used for implementing more complex
 * scopes. This scope is backed by a hash table, and supports caching
 * in variable paths.
 */
public class SimpleVariableScope implements VariableScope {
	
	/**
	 * Creates a variable scope without associated {@link VariableStorage},
	 * suitable for e.g. local variables.
	 * @return Variable scope.
	 */
	public static SimpleVariableScope createLocal() {
		// TODO estimate how many local variables there really are
		return new SimpleVariableScope(10, null);
	}
	
	/**
	 * Creates with variables from given storage. Changes to variables are
	 * saved to the storage.
	 * @param storage Variable storage.
	 * @return Variable scope.
	 * @throws IOException When storage given could not load variables.
	 */
	public static SimpleVariableScope createPersistent(VariableStorage storage) throws IOException {
		SimpleVariableScope scope = new SimpleVariableScope(storage.estimatedSize(), storage);
		storage.loadVariables(scope);
		return scope;
	}
	
	/**
	 * All variables by their names. Unlike list variables, this map contains
	 * only string keys.
	 */
	private final Map<String, Object> variables;
	
	/**
	 * Storage backing this scope. Null when there is not storage.
	 * TODO implement sending changes back to storage
	 */
	@Nullable
	private final VariableStorage storage;
	
	private SimpleVariableScope(int expectedVars, @Nullable VariableStorage storage) {
		this.variables = new HashMap<>();
		this.storage = storage;
	}
	
	/**
	 * Creates a list variable with given name.
	 * @param parent Parent of new list. If null, root of this scope is used.
	 * @param name Name of list to create.
	 * @param shadow Shadow value for the upcoming list, if there is any.
	 * @return A new list variable.
	 */
	private ListVariable createList(@Nullable ListVariable parent, Object name, @Nullable Object shadow) {
		ListVariable list = new ListVariable();
		list.shadowValue = shadow; // Assign shadow value, in case it is not null
		
		if (parent == null) { // Root variable
			variables.put("" + name, list);
		} else {
			if (name instanceof Integer) {
				parent.put((int) name, list);
			} else {
				parent.put((String) name, list);
			}
		}
		
		return list;
	}
	
	/**
	 * Queries for a variable. Does not use any kind of caching.
	 * @param path Variable path.
	 * @param event Event to use for execution.
	 * @param prepareList Ensures that the given path will, in future, lead to
	 * a {@link ListVariable}. Causes list variables to be created along
	 * the path, wrapping existing values as shadow values.
	 * @return A variable value, or null.
	 */
	@Nullable
	private Object query(VariablePath path, @Nullable Event event, boolean prepareList) {
		Object[] parts = path.path;
		ListVariable parent = null; // Parent of var
		Object var = variables.get("" + parts[0]);
		
		// If path has multiple parts, go through them
		for (int i = 1; i < path.length(); i++) {
			if (!(var instanceof ListVariable)) {
				// Target of path is under this, but this isn't list
				if (prepareList) { // Make it a list
					Object part = parts[i - 1];
					assert part != null;
					var = createList(parent, part, var);
				} else { // Ok, it doesn't exist
					return null;
				}
			}
			
			// Go deeper
			parent = (ListVariable) var;
			Object part = parts[i];
			assert part != null;
			if (part instanceof Integer) {
				var = ((ListVariable) var).get((int) part);
			} else {
				var = ((ListVariable) var).get((String) part);
			}
		}
		
		// If resulting var needs to be a list, ensure it is
		if (!(var instanceof ListVariable) && prepareList) {
			Object part = parts[path.length() - 1];
			assert part != null;
			var = createList(parent, part, var);
		}
		assert !(prepareList && !(var instanceof ListVariable)) : "list was not created";
		return var;
	}
	
	@Override
	@Nullable
	public Object get(VariablePath path, @Nullable Event event) {
		if (path.length() == 1) { // Variable directly under root
			return variables.get(VariablePath.executePart(path.path[0], event));
		}
		
		// Try to get cached parent list of target variable
		ListVariable parent = path.cachedParent;
		if (parent != null) {
			if (parent.isValid()) {
				Object part = VariablePath.executePart(path.path[path.length() - 1], event);
				if (part instanceof Integer) {
					return parent.get((int) part);
				} else {
					return parent.get((String) part);
				}
			} else { // Discard invalid cached list
				path.cachedParent = null;
			}
		}
		
		// Resolve parent list of what we're looking for
		Object maybeList = query(path.execute(event, path.length() - 1), event, false);
		if (!(maybeList instanceof ListVariable) ) {
			return null; // This variable can't possibly exist
		}
		ListVariable list = (ListVariable) maybeList;
		path.cachedParent = list; // Cache parent list to avoid costly lookup in future
		
		// Get wanted variable from list
		Object part = path.path[path.length() - 1];
		assert part != null;
		Object var;
		if (part instanceof Integer) {
			var = list.get((int) part);
		} else {
			var = list.get((String) part);
		}
		return var;
	}
	
	/**
	 * Puts a variable with given name to a list. If the given list contains
	 * a list with same name, and the given value is not a list, the given
	 * is set to be shadow value of found list.
	 * @param list List to put to.
	 * @param name Name of variable.
	 * @param value New value.
	 */
	private static void putOrShadow(ListVariable list, Object name, Object value) {
		if (name instanceof Integer) {
			int index = (int) name;
			Object oldValue = list.get(index);
			if (oldValue instanceof ListVariable && !(value instanceof ListVariable)) {
				((ListVariable) oldValue).shadowValue = value;
			} else {
				list.put(index, value);
			}
		} else {
			String str = (String) name;
			Object oldValue = list.get(str);
			if (oldValue instanceof ListVariable && !(value instanceof ListVariable)) {
				((ListVariable) oldValue).shadowValue = value;
			} else {
				list.put(str, value);
			}
		}
	}
	
	@Override
	public void set(VariablePath path, @Nullable Event event, Object value) {
		if (path.length() == 1) { // Variable directly under root
			String name = VariablePath.executePart(path.path[0], event).toString();
			Object oldValue = variables.get(name);
			if (oldValue instanceof ListVariable) { // Set as shadow value
				((ListVariable) oldValue).shadowValue = value;
			} else { // Normal map put
				variables.put(name, value);
			}
			if (storage != null) { // Notify storage before returning
				// Number keys are not meaningful for top-level variables
				assert name != null;
				storage.variableChanged(null, name, value);
			}
			return;
		}
		
		// If we have cached parent, use it
		ListVariable parent = path.cachedParent;
		if (parent != null) {
			if (parent.isValid()) {
				Object name = VariablePath.executePart(path.path[path.length() - 1], event);
				putOrShadow(parent, name, value);
				
				if (storage != null) { // Notify storage before returning
					// We execute before this, because doing it beyond last part
					// may be unnecessary for local variables
					storage.variableChanged(path.execute(event, path.length() - 1), name, value);
				}
				return;
			} else { // Discard invalid cached list
				path.cachedParent = null;
			}
		}
		
		// Resolve parent list of what we're looking to set
		// (create lists as needed)
		VariablePath pathToList = path.execute(event, path.length() - 1);
		ListVariable list = (ListVariable) query(pathToList, event, true);
		assert list != null;
		
		// Write to that list
		Object name = VariablePath.executePart(path.path[path.length() - 1], event);
		putOrShadow(list, name, value);
		path.cachedParent = list; // Cache parent list to avoid costly lookup in future
		
		if (storage != null) { // Notify the storage if necessary
			storage.variableChanged(pathToList, name, value);
		}
	}
	
	@Override
	public void append(VariablePath path, @Nullable Event event, Object value) {
		ListVariable self = path.cachedSelf;
		if (self == null || !self.isValid()) {
			self = (ListVariable) query(path, event, true);
			assert self != null;
			path.cachedSelf = self;
		}
		int lastIndex = self.getSize();
		self.add(value);
		if (storage != null) {
			storage.variableChanged(path, lastIndex, value);
		}
	}
	
	/**
	 * Deletes a variable from list. If the deleted variable is a list,
	 * invalidates it.
	 * @param parent Parent list.
	 * @param part Name of variable in list.
	 * @param deleteList If a list should be deleted. Otherwise, singular
	 * values (including shadow values) are deleted.
	 */
	private static void deleteFromList(ListVariable parent, Object part, boolean deleteList) {
		Object oldValue;
		if (part instanceof Integer) {
			oldValue = parent.get((int) part);
		} else {
			oldValue = parent.get((String) part);
		}
		
		if (oldValue instanceof ListVariable) {
			if (deleteList) { // Replace list with its shadow value, or delete if there are none
				((ListVariable) oldValue).invalidate(); // Invalidate, list is deleted soon
				
				Object shadow = ((ListVariable) oldValue).shadowValue;
				if (shadow == null) { // Just delete
					if (part instanceof Integer) {
						parent.remove((int) part);
					} else {
						parent.remove((String) part);
					}
				} else { // Replace with its old shadow
					if (part instanceof Integer) {
						parent.put((int) part, shadow);
					} else {
						parent.put((String) part, shadow);
					}
				}
			} else { // Delete shadow value
				((ListVariable) oldValue).shadowValue = null;
			}
		} else if (!deleteList) { // Delete a normal variable
			if (part instanceof Integer) {
				parent.remove((int) part);
			} else {
				parent.remove((String) part);
			}
		} // else: can't delete nonexistent list
	}
	
	@Override
	public void delete(VariablePath path, @Nullable Event event, boolean deleteList) {
		if (path.length() == 1) { // Variable directly under root
			// TODO shadow value support
			Object name = VariablePath.executePart(path.path[0], event);
			variables.remove(name.toString());
			if (storage != null) { // Notify storage if necessary
				storage.variableChanged(null, name, null);
			}
			return;
		}
		
		ListVariable parent = path.cachedParent;
		if (parent != null) {
			if (parent.isValid()) {
				Object name = VariablePath.executePart(path.path[path.length() - 1], event);
				deleteFromList(parent, name, deleteList);
				if (storage != null) { // Notify storage before returning
					// We execute before this, because doing it beyond last part
					// may be unnecessary for local variables
					storage.variableChanged(path.execute(event, path.length() -1), name, null);
				}
				return;
			} else { // Discard invalid cached list
				path.cachedParent = null;
			}
		}
		
		// Resolve parent list of what we're looking to set
		VariablePath pathToList = path.execute(event, path.length() - 1);
		Object maybeList = query(pathToList, event, false);
		if (!(maybeList instanceof ListVariable) ) {
			return; // Variable can't exist, nothing to delete
		}
		
		// Delete from list
		ListVariable list = (ListVariable) maybeList;
		Object name = VariablePath.executePart(path.path[path.length() - 1], event);
		deleteFromList(list, name, deleteList);
		path.cachedParent = null; // Variable no longer exists, no point keeping this around
		
		if (storage != null) { // Tell storage about this deletion
			storage.variableChanged(pathToList, name, null);
		}
	}
}

