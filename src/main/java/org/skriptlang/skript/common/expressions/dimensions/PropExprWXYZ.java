package org.skriptlang.skript.common.expressions.dimensions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyBaseExpression;
import org.skriptlang.skript.lang.properties.PropertyHandler.WXYZPropertyHandler;
import org.skriptlang.skript.lang.properties.PropertyHandler.WXYZPropertyHandler.Axis;

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
		for (var propertyInfo : properties.values()) {
			propertyInfo.handler().axis(axis);
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
