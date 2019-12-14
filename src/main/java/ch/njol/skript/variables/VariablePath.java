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
		return create(path, path.length);
	}
	
	public static VariablePath create(Object[] path, int length) {
		assert checkPath(path, length);
		return new VariablePath(path, length);
	}
	
	/**
	 * Checks that path meets criteria.
	 * @param path Path to check.
	 * @param length Length of the path.
	 * @return True if check passed, otherwise false.
	 */
	private static boolean checkPath(Object[] path, int length) {
		for (int i = 0; i < length; i++) {
			Object o = path[i];
			if (!(o instanceof Expression<?>) && !(o instanceof String) && !(o instanceof Integer)) {
				assert false : "path[" + i + "] = " + o + " (not expression, string or number)";
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
	 * Length of the path. This may be less that {@link #path} array length
	 * in some cases.
	 */
	private final int length;
	
	/**
	 * List containing this variable. Cached when possible.
	 */
	@Nullable
	ListVariable cachedParent;
	
	/**
	 * If this points to a list variable, this is it. Cached when possible.
	 */
	@Nullable
	ListVariable cachedSelf;
	
	/**
	 * If this variable is global, real scope of it is cached here.
	 */
	@Nullable
	VariableScope cachedGlobalScope;
	
	/**
	 * Creates a new variable path. Before this path is exposed,
	 * {@link #assertValid()} should be called on it.
	 * @param path Path elements.
	 * @param length Length of path.
	 */
	VariablePath(Object[] path, int length) {
		this.path = path;
		this.length = length;
	}
	
	void assertValid() {
		assert checkPath(path, length);
	}

	/**
	 * Checks if this path starts with given prefix path. Note that this will
	 * not be able to compare expression elements in paths.
	 * @param prefix Prefix path.
	 * @return Whether this starts with given path or not.
	 */
	public boolean startsWith(VariablePath prefix) {
		if (prefix.length > length) {
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
	 * Executes expression parts in this path before given limit.
	 * Parts after it are not present in returned path.
	 * @param event Execution context.
	 * @param limit How many parts to include in new path
	 * @return Cut and executed path.
	 */
	VariablePath execute(@Nullable Event event, int limit) {
		VariablePath executed = new VariablePath(new Object[limit], limit);
		for (int i = 0; i < limit; i++) {
			executed.path[i] = executePart(path[i], event);
		}
		executed.assertValid();
		return executed;
	}
	
	/**
	 * Executes all expression parts in this path.
	 * @param event Execution context.
	 * @return Path that can be used without event.
	 */
	public VariablePath execute(@Nullable Event event) {
		return execute(event, length);
	}
	
	public int length() {
		return length;
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
