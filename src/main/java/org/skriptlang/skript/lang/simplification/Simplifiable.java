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
	 *
	 * @return the simplified object.
	 * @see SimplifiedLiteral
	 */
	S simplify();
}
