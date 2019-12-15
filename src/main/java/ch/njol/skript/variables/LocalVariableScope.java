package ch.njol.skript.variables;

import java.util.IdentityHashMap;
import java.util.Map;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.SkriptEventHandler;

/**
 * A scope that separates variables associated with different events.
 */
public class LocalVariableScope implements VariableScope {

	private final Map<Event,VariableScope> scopes;
	
	public LocalVariableScope() {
		this.scopes = new IdentityHashMap<>();
	}
	
	/**
	 * Gets scope with variables associated with given event. If it does not
	 * yet exist, it is created. {@link SkriptEventHandler} will destroy event
	 * scopes once the events have been handled by calling
	 * {@link #finishedEvent(Event)}. If event does not go through normal event
	 * handling, caller must ensure it is removed when the event is no longer
	 * needed to avoid memory leaks.
	 * 
	 * <p>{@link VariableScope} methods implemented by
	 * {@link LocalVariableScope} use this. Calling it manually once may
	 * improve performance when many variables are written.
	 * @param event Event.
	 * @return Scope for given event.
	 */
	public VariableScope getEventScope(@Nullable Event event) {
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
	public void append(VariablePath path, @Nullable Event event, Object value) {
		getEventScope(event).append(path, event, value);
	}

	@Override
	public void delete(VariablePath path, @Nullable Event event, boolean deleteList) {
		getEventScope(event).delete(path, event, deleteList);
	}
	
	@Override
	public void mergeList(VariablePath path, @Nullable Event event, ListVariable list) {
		getEventScope(event).mergeList(path, event, list);
	}
	
	public void finishedEvent(Event event) {
		scopes.remove(event);
	}

}
