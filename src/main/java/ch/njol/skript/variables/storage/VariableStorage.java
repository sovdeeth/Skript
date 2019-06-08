package ch.njol.skript.variables.storage;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.variables.VariablePath;
import ch.njol.skript.variables.VariableScope;

/**
 * A variable storage handles persisting variables.
 */
public interface VariableStorage {
	
	/**
	 * Called when a variable at given path changes.
	 * @param path Path to the variable.
	 * @param newValue New value of the variable. Null here signifies a deleted
	 * variable.
	 */
	void variableChanged(VariablePath path, Object newValue);
	
	/**
	 * Called when a variable is unloaded from memory.
	 * @param path Path to the variable.
	 */
	void variableUnloaded(VariablePath path);
	
	/**
	 * Ensures that variables under given path are loaded. If null is given,
	 * or this variable storage implementation does not support streaming from
	 * disk, ALL variables will be loaded.
	 * @param scope Scope where to load the variables.
	 * @param path Path under which all variables are loaded.
	 */
	void loadVariables(VariableScope scope, @Nullable VariablePath path);
}
