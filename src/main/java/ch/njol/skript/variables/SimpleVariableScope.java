package ch.njol.skript.variables;

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
	 */
	public static SimpleVariableScope createPersistent(VariableStorage storage) {
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
	
	@Override
	@Nullable
	public Object get(VariablePath path, @Nullable Event event) {
		ListVariable parent = path.cachedParent;
		if (parent != null) {
			if (parent.isValid()) {
				Object part = VariablePath.executePart(path.path[path.path.length - 1], event);
				if (part instanceof Integer) {
					return parent.get((int) part);
				} else {
					return parent.get((String) part);
				}
			} else { // Discard invalid cached list
				path.cachedParent = null;
			}
		}
		
		// Look up the global variable in this scope
		VariablePath original = path;
		path = path.execute(event);
		Object var = variables.get("" + path.path[0]);
		
		// Variable inside at least one list
		for (int i = 1; i < path.path.length; i++) {
			if (!(var instanceof ListVariable)) { // No list variable here
				return null; // Requested variable can't possibly exist
			}
			
			Object part = path.path[i];
			assert part != null;
			if (part instanceof Integer) {
				var = ((ListVariable) var).get((int) part);
			} else {
				var = ((ListVariable) var).get((String) part);
			}
			if (i == path.path.length - 1) { // Cache list
				original.cachedParent = (ListVariable) var; // If we're writing null, there is already null there
			}
		}
		return var;
	}
	
	@Override
	public void set(VariablePath path, @Nullable Event event, Object value) {
		ListVariable parent = path.cachedParent;
		if (parent != null) {
			if (parent.isValid()) {
				Object part = VariablePath.executePart(path.path[path.path.length - 1], event);
				if (part instanceof Integer) {
					parent.put((int) part, value);
				} else {
					parent.put((String) part, value);
				}
				
				if (storage != null) { // Notify storage before returning
					// We execute before this, because doing it beyond last part
					// may be unnecessary for local variables
					storage.variableChanged(path.execute(event), value);
				}
				return;
			} else { // Discard invalid cached list
				path.cachedParent = null;
			}
		}
		
		VariablePath original = path;
		path = path.execute(event);
		if (storage != null) {
			storage.variableChanged(path, value); // Notify the storage
		}
		String rootKey = "" + path.path[0];
		if (path.path.length == 1) { // Variable not in list
			variables.put(rootKey, value);
			return;
		}
		
		// Variable is in at least one list; create them as needed
		ListVariable var = (ListVariable) variables.get(rootKey);
		if (var == null) {
			var = new ListVariable();
			variables.put(rootKey, var);
		}
		
		// Variable inside at least one list; create lists as needed
		for (int i = 1; i < path.path.length - 1; i++) {
			Object part = path.path[i];
			if (part instanceof Integer) {
				int index = (int) part;
				Object child = var.get(index);
				if (child == null) {
					child = new ListVariable();
				}
				var.put(index, new ListVariable());
			} else {
				String name = (String) part;
				assert name != null;
				Object child = var.get(name);
				if (child == null) {
					child = new ListVariable();
				}
				var.put(name, new ListVariable());
			}
		}
		
		// Put to list variable with last path part as name
		Object last = path.path[path.path.length - 1];
		assert last != null;
		if (last instanceof Integer) {
			var.put((int) last, value);
		} else {
			var.put((String) last, value);
		}
		original.cachedParent = var; // Cache parent to make access faster next time
	}
	
	/**
	 * Deletes a variable from list. If it is a list, invalidates it.
	 * @param parent Parent list.
	 * @param part Name of variable in list.
	 */
	private static void deleteFromList(ListVariable parent, Object part) {
		Object removed;
		if (part instanceof Integer) {
			removed = parent.remove((int) part);
		} else {
			removed = parent.remove((String) part);
		}
		if (removed instanceof ListVariable) {
			((ListVariable) removed).invalidate();
		}
	}
	
	@Override
	public void delete(VariablePath path, @Nullable Event event) {
		ListVariable parent = path.cachedParent;
		if (parent != null) {
			if (parent.isValid()) {
				Object part = VariablePath.executePart(path.path[path.path.length - 1], event);
				deleteFromList(parent, part);
				if (storage != null) { // Notify storage before returning
					// We execute before this, because doing it beyond last part
					// may be unnecessary for local variables
					storage.variableChanged(path.execute(event), null);
				}
				return;
			} else { // Discard invalid cached list
				path.cachedParent = null;
			}
		}
		
		// Look up the global variable in this scope
		path = path.execute(event);
		if (storage != null) {
			storage.variableChanged(path, null); // Notify the storage
		}
		String rootKey = "" + path.path[0];
		if (path.path.length == 1) { // Target variable is not in any list
			Object removed = variables.remove(rootKey); // Remove it
			if (removed instanceof ListVariable) {
				((ListVariable) removed).invalidate();
			}
			return;
		}
		Object var = variables.get(rootKey);
		
		// Variable inside at least one list
		for (int i = 1; i < path.path.length - 1; i++) {
			if (!(var instanceof ListVariable)) { // No list variable here
				return; // Requested variable can't possibly exist
			}
			
			Object part = path.path[i];
			assert part != null;
			if (part instanceof Integer) {
				var = ((ListVariable) var).get((int) part);
			} else {
				var = ((ListVariable) var).get((String) part);
			}
			if (i == path.path.length - 2) { // Cache list
				assert var != null;
				part = path.path[path.path.length - 1];
				assert part != null;
				deleteFromList((ListVariable) var, part);
				break;
			}
		}
	}
}

