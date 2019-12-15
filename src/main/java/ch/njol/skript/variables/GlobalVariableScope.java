package ch.njol.skript.variables;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A variable scope designed for global variables. It delegates variables to
 * multiple scopes based on their {@link VariablePath} prefix.
 * 
 * <p>Usually, this is a singleton. Variable paths to multiple global scopes
 * should NEVER be mixed, because it breaks internal caching.
 */
public class GlobalVariableScope implements VariableScope {
	
	private static class ScopeEntry {
		
		/**
		 * Prefix. Paths matching this scope will 
		 */
		public final VariablePath prefix;
		
		/**
		 * Scope associated with prefix.
		 */
		public final VariableScope scope;

		public ScopeEntry(VariablePath prefix, VariableScope scope) {
			this.prefix = prefix;
			this.scope = scope;
		}
	}
	
	/**
	 * Scopes in which global variables can be placed.
	 */
	private final List<ScopeEntry> scopes;
	
	/**
	 * Default scope. Variables that do not have a prefix matching with any
	 * of other scopes go here.
	 */
	private final VariableScope defaultScope;
	
	/**
	 * Creates a new global variable scope.
	 * @param defaultScope Default scope. Variables that do not go to any
	 * scopes added by {@link #addScope(VariablePath, VariableScope)}
	 * go to this scope.
	 */
	public GlobalVariableScope(VariableScope defaultScope) {
		this.scopes = new ArrayList<>();
		this.defaultScope = defaultScope;
	}
	
	/**
	 * Adds a variable scope to this global scope.
	 * @param prefix Prefix that paths of variables must have to be placed
	 * to this scope.
	 * @param scope The scope.
	 */
	public void addScope(VariablePath prefix, VariableScope scope) {
		scopes.add(new ScopeEntry(prefix, scope));
	}
	
	/**
	 * Gets a correct scope for a variable with given path.
	 * @param path Variable path.
	 * @return A scope where the variable can be placed to.
	 */
	public VariableScope getScope(VariablePath path) {
		// Try to get cached scope
		VariableScope scope = path.cachedGlobalScope;
		if (scope == null) { // Not cached, do that now
			// Look for a scope with prefix
			for (ScopeEntry entry : scopes) {
				if (path.startsWith(entry.prefix)) {
					scope = entry.scope;
				}
			}
			
			if (scope == null) { // Still no scope? Fall back to default
				scope = defaultScope;
			}
			path.cachedGlobalScope = scope; // Cache what we just found
		}
		return scope;
	}

	@Override
	@Nullable
	public Object get(VariablePath path, @Nullable Event event) {
		return getScope(path).get(path, event);
	}

	@Override
	public void set(VariablePath path, @Nullable Event event, Object value) {
		getScope(path).set(path, event, value);
	}
	
	@Override
	public void append(VariablePath path, @Nullable Event event, Object value) {
		getScope(path).set(path, event, value);
	}

	@Override
	public void delete(VariablePath path, @Nullable Event event, boolean deleteList) {
		getScope(path).delete(path, event, deleteList);
	}

	@Override
	public void mergeList(VariablePath path, @Nullable Event event, ListVariable list) {
		getScope(path).mergeList(path, event, list);
	}
}
