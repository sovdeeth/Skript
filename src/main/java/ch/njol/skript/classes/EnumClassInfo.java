package ch.njol.skript.classes;

import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.util.EnumUtils;
import ch.njol.skript.util.StringMode;
import ch.njol.util.coll.iterator.ArrayIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class can be used for an easier writing of ClassInfos that are enums,
 * it registers a language node with usage, a serializer, default expression and a parser.
 * Making it easier to register enum ClassInfos.
 * @param <T> The enum class.
 */
public class EnumClassInfo<T extends Enum<T>> extends ClassInfo<T> {

	/**
	 * @param enumClass The class
	 * @param codeName The name used in patterns
	 * @param languageNode The language node of the type
	 */
	public EnumClassInfo(Class<T> enumClass, String codeName, String languageNode) {
		this(enumClass, codeName, languageNode, new EventValueExpression<>(enumClass));
	}

	/**
	 * @param enumClass The class
	 * @param codeName The name used in patterns
	 * @param languageNode The language node of the type
	 * @param defaultExpression The default expression of the type
	 */
	public EnumClassInfo(Class<T> enumClass, String codeName, String languageNode, DefaultExpression<T> defaultExpression) {
		super(enumClass, codeName);
		EnumUtils<T> enumUtils = new EnumUtils<>(enumClass, languageNode);
		usage(enumUtils.getAllNames())
			.serializer(new EnumSerializer<>(enumClass))
			.defaultExpression(defaultExpression)
			.supplier(() -> new ArrayIterator<>(enumClass.getEnumConstants()))
			.parser(new Parser<T>() {
				@Override
				public @Nullable T parse(String input, ParseContext context) {
					return enumUtils.parse(input);
				}

				@Override
				public @NotNull String toString(T constant, int flags) {
					return enumUtils.toString(constant, StringMode.MESSAGE);
				}

				@Override
				public @NotNull String toVariableNameString(T constant) {
					return enumUtils.toString(constant, StringMode.VARIABLE_NAME);
				}
			});
	}

}
