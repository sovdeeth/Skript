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
public interface VariableScope {
	
	/**
	 * Gets a variable at given path.
	 * @param path Path to variable.
	 * @param event Currently executing event.
	 * @return A variable value, or null if given path does not contain
	 * a variable.
	 */
	@Nullable
	Object get(VariablePath path, @Nullable Event event);
	
	/**
	 * Sets a variable at given path. Parent list variables will be created
	 * as needed.
	 * @param path Path to variable.
	 * @param event Currently executing event.
	 * @param value A new value for the variable.
	 */
	public void set(VariablePath path, @Nullable Event event, Object value);
	
	/**
	 * Deletes a variable at given path. If there is no such variable, nothing
	 * will be done.
	 * @param path Path to variable to be deleted.
	 * @param event Currently executing event.
	 */
	public void delete(VariablePath path, @Nullable Event event);

}
