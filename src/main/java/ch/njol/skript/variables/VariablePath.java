package ch.njol.skript.variables;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.lang.Expression;

/**
 * A parsed path to a variable.
 */
public class VariablePath {

	/**
	 * Name of variable split by list token ('::'). Elements are constant
	 * Strings or expressions. Note: expression elements must NOT return
	 * strings that may contain list token.
	 * 
	 * <p>Contents of this array should be considered stable; do not write to
	 * it.
	 */
	final Object[] path;
	
	/**
	 * List containing this variable. Cached when possible.
	 */
	@Nullable
	ListVariable cachedParent;
	
	/**
	 * If this variable is global, real scope of it is cached here.
	 */
	@Nullable
	VariableScope cachedGlobalScope;
	
	public VariablePath(Object... path) {
		this.path = path;
	}

	/**
	 * Checks if this path starts with given prefix path.
	 * @param prefix Prefix path.
	 * @return Whether this starts with given path or not.
	 */
	public boolean startsWith(VariablePath prefix) {
		if (prefix.path.length > path.length) {
			return false; // Prefix can't be longer than this
		}
		for (int i = 0; i < prefix.path.length; i++) {
			if (!path[i].equals(prefix.path[i])) {
				return false; // Prefix has part this doesn't have
			}
		}
		return true;
	}
}
