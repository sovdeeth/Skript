package ch.njol.skript.variables.storage;

import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.variables.VariablePath;
import ch.njol.skript.variables.VariableScope;

/**
 * A variable storage handles persisting variables.
 */
public interface VariableStorage {
	
	/**
	 * Called when a variable at given path changes.
	 * @param path Path to parent of the variable. Null for variables at
	 * root of their scope.
	 * @param name Name of variable.
	 * @param newValue New value of the variable. Null here signifies a deleted
	 * variable.
	 */
	void variableChanged(@Nullable VariablePath path, Object name, @Nullable Object newValue);
	
	/**
	 * Estimates how many top-level variables this storage contains.
	 * @return Estimated size.
	 */
	int estimatedSize();
	
	/**
	 * Loads variables from this storage to given scope.
	 * @param scope Scope where to load the variables.
	 * @throws IOException When implementation can't load variables.
	 */
	void loadVariables(VariableScope scope) throws IOException;
}
