package ch.njol.skript.variables;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GlobalVariableScope {
	
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
	
	public GlobalVariableScope() {
		this.scopes = new ArrayList<>();
	}
	
	public void addScope(VariablePath prefix, VariableScope scope) {
		scopes.add(new ScopeEntry(prefix, scope));
	}
	
	public VariableScope getScope(VariablePath path) {
		for (ScopeEntry entry : scopes) {
			// TODO path starts with
		}
	}
}
