package org.skriptlang.skript.lang.simplification;

import ch.njol.skript.lang.Debuggable;
import ch.njol.skript.lang.SyntaxElement;
import org.bukkit.event.Event;

public interface Simplifiable<S extends SyntaxElement> {

	/**
	 * Simplifies this object. This should be called immediately after init() returns true.
	 * If simplification is not possible, the object is returned as is.
	 * <br>
	 * References to the original object should be replaced with the simplified object.
	 * <br>
	 * Any returned object should attempt to maintain the original value of {@link Debuggable#toString(Event, boolean)}.
	 * An addition indicating that the value was simplified can be added in the debug string. See {@link SimplifiedLiteral}
	 * for an example.
	 * <br>
	 * Simplification should never widen contracts. For example, any simplified expression should take care to return
	 * the same or a more specific type than the original expression, never a more generic type. Likewise, be sure to
	 * maintain the behavior of change() and acceptsChange(). Failure to do so can result in unexpected behavior and
	 * tricky bugs.
	 *
	 * @return the simplified object.
	 * @see SimplifiedLiteral
	 */
	S simplify();
}
