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
	 * Token signifying that a {@link ListVariable} can be returned.
	 */
	private static final String LIST_VALUES_TOKEN = "*";
	
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
		if (parent != null) { // TODO shadow values
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
				if (LIST_VALUES_TOKEN.equals(part)) {
					assert i == path.path.length - 1;
					return var;
				} else {
					var = ((ListVariable) var).get((String) part);
				}
			}
			if (i == path.path.length - 2) { // Cache list to second-last
				path.cachedParent = (ListVariable) var; // If we're writing null, there is already null there
			}
		}
		return var instanceof ListVariable ? ((ListVariable) var).getShadowValue() : var;
	}
	
	public void set(VariablePath path, @Nullable Event event, Object value) {
		ListVariable parent = path.cachedParent;
		if (parent != null) { // TODO shadow values!
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
		for (int i = 1; i < path.path.length - 2; i++) {
			Object part = executePart(path.path[i], event);
			if (part instanceof Integer) {
				int index = (int) part;
				Object child = var.get(index);
				if (!(child instanceof ListVariable)) {
					ListVariable list = new ListVariable();
					list.setShadowValue(child);
					var.put(index, list);
					child = list;
				}
				var = (ListVariable) child;
			} else {
				String name = (String) part;
				Object child = var.get(name);
				if (!(child instanceof ListVariable)) {
					ListVariable list = new ListVariable();
					list.setShadowValue(child);
					var.put(name, list);
					child = list;
				}
				var = (ListVariable) child;
			}
		}
		
		// If last is list token, second-last (ListVariable) in third-last (var)
		Object last = executePart(path.path[path.path.length - 1], event);
		if (last instanceof Integer) {
			assert !(value instanceof ListVariable); // List to singular value is error
			// TODO handle this case
			//var.put((int) last, value);
		} else {
			if (LIST_VALUES_TOKEN.equals(last)) { // Replace whole list
				assert value instanceof ListVariable; // Must not replace list with singular!
				var.put((String) last, value); // Just replace whole list
				path.cachedParent = var;
			} else { // Replace shadow value in second-last
				assert !(value instanceof ListVariable); // List to singular value is error
				Object secondLast = executePart(path.path[path.path.length - 2], event);
				// TODO handle this case
			}
		}
	}
	
	public void delete(VariablePath path, @Nullable Event event) {
		ListVariable parent = path.cachedParent;
		if (parent != null) { // TODO shadow values
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
			if (i == path.path.length - 2) { // Second-last: 
				assert var != null;
				part = executePart(path.path[path.path.length - 1], event);
				if (part instanceof Integer) {
					((ListVariable) var).remove((int) part);
				} else {
					if (LIST_VALUES_TOKEN.equals(part)) { // Remove whole list
						// TODO remove this list from third-last
					} else { // Normal list remove
						// TODO handle list with shadow
						((ListVariable) var).remove((String) part);
					}
				}
				break;
			}
		}
	}
}
