package ch.njol.skript.lang.parser;

import ch.njol.skript.lang.DefaultExpression;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A Parser Data that stores custom default values that should be given preference over event-values and other
 * traditional default values.
 * <br>
 * These do not apply to arithmetic or other dynamic default values, they're only supplied during parsing.
 * @see ch.njol.skript.test.runner.SecCustomDefault SecCustomDefault for example usage
 */
public class DefaultValueData extends ParserInstance.Data {

	private final Map<Class<?>, DefaultExpression<?>> defaults = new HashMap<>();

	public DefaultValueData(ParserInstance parserInstance) {
		super(parserInstance);
	}

	/**
	 * Sets the default value for type to value.
	 * <br>
	 * Any custom default values should be removed after they go out of scope. 
	 * It will not be done automaticallly.
	 * 
	 * @see	#removeDefaultValue(Class) 
	 * 
	 * @param type The class which this value applies to.
	 * @param value The value to use.
	 * @param <T> The class of this value.
	 */
	public <T> void addDefaultValue(Class<T> type, DefaultExpression<T> value) {
		defaults.put(type, value);
	}

	/**
	 * Gets the default value for the given type.
	 * @param type The class which this value applies to.
	 * @param <T> The class of this value.
	 * @return The default value for type, or null if none is set.   
	 */
	public <T> @Nullable DefaultExpression<T> getDefaultValue(Class<T> type) {
		//noinspection unchecked
		return (DefaultExpression<T>) defaults.get(type);
	}

	/**
	 * Removes a default value from the data.
	 * @param type Which class to remove the default value of.
	 */
	public void removeDefaultValue(Class<?> type) {
		defaults.remove(type);
	}

}
