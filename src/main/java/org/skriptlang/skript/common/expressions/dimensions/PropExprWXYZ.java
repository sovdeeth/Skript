package org.skriptlang.skript.common.expressions.dimensions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyBaseExpression;
import org.skriptlang.skript.lang.properties.PropertyHandler.WXYZPropertyHandler;
import org.skriptlang.skript.lang.properties.PropertyHandler.WXYZPropertyHandler.Axis;

import java.util.ArrayList;
import java.util.Locale;

public class PropExprWXYZ extends PropertyBaseExpression<WXYZPropertyHandler<?, ?>> {

	static {
		register(PropExprWXYZ.class, "(:x|:y|:z|:w) [(component|coordinate)[s]]", "objects");
	}

	private Axis axis;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		axis = Axis.valueOf(parseResult.tags.get(0).toUpperCase(Locale.ENGLISH));
		if (!super.init(expressions, matchedPattern, isDelayed, parseResult))
			return false;

		// filter out unsupported handlers and set axis
		var tempProperties = new ArrayList<>(properties.entrySet());
		for (var entry : tempProperties) {
			var propertyInfo = entry.getValue();
			Class<?> type = entry.getKey();
			var handler = propertyInfo.handler();
			if (!handler.supportsAxis(axis)) {
				properties.remove(type);
				continue;
			}
			propertyInfo.handler().axis(axis);
		}

		// ensure we have at least one handler left
		if (properties.isEmpty()) {
			Skript.error("None of the types returned by " + expr + " support the " + axis.name().toLowerCase(Locale.ENGLISH) + " axis.");
			return false;
		}

		return true;
	}

	public Axis axis() {
		return axis;
	}

	@Override
	public @NotNull Property<WXYZPropertyHandler<?, ?>> getProperty() {
		return Property.WXYZ;
	}

	@Override
	public String getPropertyName() {
		return axis.name();
	}

}
