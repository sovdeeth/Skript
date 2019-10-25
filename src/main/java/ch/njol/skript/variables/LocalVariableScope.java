package ch.njol.skript.variables;

import java.util.IdentityHashMap;
import java.util.Map;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A scope that separates variables associated with different events.
 */
public class LocalVariableScope implements VariableScope {

	private final Map<Event,VariableScope> scopes;
	
	public LocalVariableScope() {
		this.scopes = new IdentityHashMap<>();
	}
	
	private VariableScope getEventScope(@Nullable Event event) {
		VariableScope scope = scopes.computeIfAbsent(event, e -> SimpleVariableScope.createLocal());
		assert scope != null;
		return scope;
	}
	
	@Override
	@Nullable
	public Object get(VariablePath path, @Nullable Event event) {
		return getEventScope(event).get(path, event);
	}

	@Override
	public void set(VariablePath path, @Nullable Event event, Object value) {
		getEventScope(event).set(path, event, value);
	}

	@Override
	public void delete(VariablePath path, @Nullable Event event) {
		getEventScope(event).delete(path, event);
	}
	
	public void finishedEvent(Event event) {
		scopes.remove(event);
	}
}
