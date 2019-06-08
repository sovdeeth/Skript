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
	
	public VariablePath(Object... path) {
		this.path = path;
	}

}
