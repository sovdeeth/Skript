package ch.njol.skript.variables;

import java.util.Iterator;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.lang.Expression;

/**
 * A parsed path to a variable.
 */
public class VariablePath implements Iterable<Object> {
	
	/**
	 * Creates a new variable path. Only elements that are strings, integers
	 * or expressions that produce either of these are allowed.
	 * @param path Path elements.
	 */
	public static VariablePath create(Object... path) {
		assert checkPath(path);
		return new VariablePath(path);
	}
	
	/**
	 * Checks that path meets criteria.
	 * @param path Path to check.
	 * @return True if check passed, otherwise false.
	 */
	private static boolean checkPath(Object... path) {
		for (int i = 0; i < path.length; i++) {
			Object o = path[i];
			if (!(o instanceof Expression<?>) && !(o instanceof String) && !(o instanceof Integer)) {
				assert false : "path[" + i + "] = " + o + "(not expression, string or number)";
			}
		}
		return true;
	}

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
	
	/**
	 * If this variable is global, real scope of it is cached here.
	 */
	@Nullable
	VariableScope cachedGlobalScope;
	
	/**
	 * Creates a new variable path. Before this path is exposed,
	 * {@link #assertValid()} should be called on it.
	 * @param path Path elements.
	 */
	VariablePath(Object... path) {
		this.path = path;
	}
	
	void assertValid() {
		assert checkPath(path);
	}

	/**
	 * Checks if this path starts with given prefix path. Note that this will
	 * not be able to compare expression elements in paths.
	 * @param prefix Prefix path.
	 * @return Whether this starts with given path or not.
	 */
	public boolean startsWith(VariablePath prefix) {
		if (prefix.path.length > path.length) {
			return false; // Prefix can't be longer than this
		}
		
		// Require full equality for parts before last
		for (int i = 0; i < prefix.path.length - 1; i++) {
			if (path[i] instanceof Expression<?> || !path[i].equals(prefix.path[i])) {
				return false; // Prefix has part this doesn't have
			}
		}
		
		// Check if part of this starts with part in prefix
		int last = prefix.path.length - 1;
		if (path[last] instanceof Expression<?> || prefix.path[last] instanceof Expression) {
			return false; // String startsWith would be safe
		}
		return ((String) path[last]).startsWith((String) prefix.path[last]);
	}
	
	/**
	 * Executes a part of variable path.
	 * @param part Part. If it is null, assertion failure is triggered.
	 * @param event Event to use for execution. May be null.
	 * @return String or Integer variable path element.
	 */
	static Object executePart(@Nullable Object part, @Nullable Event event) {
		Object p;
		if (part instanceof Expression<?>) { // Execute part if it is expression
			assert event != null : "expression parts require event";
			p = ((Expression<?>) part).getSingle(event);
		} else { // Return string as-is
			p = part;
		}
		assert p != null : "null variable path element";
		assert p instanceof String || p instanceof Integer : "unknown variable path element " + p;
		return p;
	}
	
	/**
	 * Executes all expression parts in this path.
	 * @param event Execution context.
	 * @return Path that can be used without event.
	 */
	public VariablePath execute(@Nullable Event event) {
		VariablePath executed = new VariablePath(new Object[path.length]);
		for (int i = 0; i < path.length; i++) {
			executed.path[i] = executePart(path[i], event);
		}
		executed.assertValid();
		return executed;
	}

	@Override
	public Iterator<Object> iterator() {
		return new Iterator<Object>() {

			private int index;
			
			@Override
			public boolean hasNext() {
				return index < path.length;
			}

			@SuppressWarnings("null")
			@Override
			public Object next() {
				return path[index++];
			}
		};
	}
	
	
}
