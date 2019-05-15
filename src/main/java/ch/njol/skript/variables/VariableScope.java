package ch.njol.skript.variables;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.lang.Expression;

/**
 * Stores variables of a scope. Usually, one scope stores all global variables,
 * and each trigger gets their own temporary local variable scope.
 */
public class VariableScope {
	
	/**
	 * All variables by their names. Unlike list variables, this map contains
	 * only string keys.
	 */
	private final Map<String, Object> variables;
	
	public VariableScope() {
		this.variables = new HashMap<>(); // TODO configurable map size
	}
	
	private static Object executePart(@Nullable Object part, @Nullable Event event) {
		Object p;
		if (part instanceof Expression<?>) { // Execute part if it is expression
			assert event != null : "expression parts require event";
			p = ((Expression<?>) part).getSingle(event);
		} else {
			p = part;
		}
		assert p != null : "null variable path element";
		assert p instanceof String || p instanceof Integer : "unknown variable path element";
		return p;
	}
	
	@Nullable
	public Object get(VariablePath path, @Nullable Event event) {
		ListVariable parent = path.cachedParent;
		if (parent != null) {
			if (parent.isValid()) {
				Object part = executePart(path.path[path.path.length - 1], event);
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
		Object var = variables.get("" + executePart(path.path[0], event));
		
		// Variable inside at least one list
		for (int i = 1; i < path.path.length; i++) {
			if (!(var instanceof ListVariable)) { // No list variable here
				return null; // Requested variable can't possibly exist
			}
			
			Object part = executePart(path.path[i], event);
			if (part instanceof Integer) {
				var = ((ListVariable) var).get((int) part);
			} else {
				var = ((ListVariable) var).get((String) part);
			}
			if (i == path.path.length - 1) { // Cache list
				path.cachedParent = (ListVariable) var; // If we're writing null, there is already null there
			}
		}
		return var;
	}
	
	public void set(VariablePath path, @Nullable Event event, Object value) {
		ListVariable parent = path.cachedParent;
		if (parent != null) {
			if (parent.isValid()) {
				Object part = executePart(path.path[path.path.length - 1], event);
				if (part instanceof Integer) {
					parent.put((int) part, value);
				} else {
					parent.put((String) part, value);
				}
			} else { // Discard invalid cached list
				path.cachedParent = null;
			}
		}
		
		String rootKey = "" + executePart(path.path[0], event);
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
			Object part = executePart(path.path[i], event);
			if (part instanceof Integer) {
				int index = (int) part;
				Object child = var.get(index);
				if (child == null) {
					child = new ListVariable();
				}
				var.put(index, new ListVariable());
			} else {
				String name = (String) part;
				Object child = var.get(name);
				if (child == null) {
					child = new ListVariable();
				}
				var.put(name, new ListVariable());
			}
		}
		
		// Put to list variable with last path part as name
		Object last = executePart(path.path[path.path.length - 1], event);
		if (last instanceof Integer) {
			var.put((int) last, value);
		} else {
			var.put((String) last, value);
		}
		path.cachedParent = var; // Cache parent to make access faster next time
	}
	
	public void delete(VariablePath path, @Nullable Event event) {
		ListVariable parent = path.cachedParent;
		if (parent != null) {
			if (parent.isValid()) {
				Object part = executePart(path.path[path.path.length - 1], event);
				if (part instanceof Integer) {
					parent.remove((int) part);
				} else {
					parent.remove((String) part);
				}
			} else { // Discard invalid cached list
				path.cachedParent = null;
			}
		}
		
		// Look up the global variable in this scope
		Object var = variables.get("" + executePart(path.path[0], event));
		
		// Variable inside at least one list
		for (int i = 1; i < path.path.length - 1; i++) {
			if (!(var instanceof ListVariable)) { // No list variable here
				return; // Requested variable can't possibly exist
			}
			
			Object part = executePart(path.path[i], event);
			if (part instanceof Integer) {
				var = ((ListVariable) var).get((int) part);
			} else {
				var = ((ListVariable) var).get((String) part);
			}
			if (i == path.path.length - 2) { // Cache list
				assert var != null;
				part = executePart(path.path[path.path.length - 1], event);
				if (part instanceof Integer) {
					((ListVariable) var).remove((int) part);
				} else {
					((ListVariable) var).remove((String) part);
				}
				break;
			}
		}
	}
}
