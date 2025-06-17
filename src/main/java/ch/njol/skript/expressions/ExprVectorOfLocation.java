package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.vector.FastVector;

@Name("Vectors - Vector from Location")
@Description("Creates a vector from a location.")
@Examples("set {_v} to vector of {_loc}")
@Since("2.2-dev28")
public class ExprVectorOfLocation extends SimpleExpression<FastVector> {

	static {
		Skript.registerExpression(ExprVectorOfLocation.class, FastVector.class, ExpressionType.PROPERTY,
				"[the] vector (of|from|to) %location%",
				"%location%'s vector");
	}

	@SuppressWarnings("null")
	private Expression<Location> location;

	@Override
	@SuppressWarnings({"unchecked", "null"})
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		location = (Expression<Location>) exprs[0];
		return true;
	}

	@Override
	@SuppressWarnings("null")
	protected FastVector[] get(Event event) {
		Location location = this.location.getSingle(event);
		if (location == null)
			return null;
		return CollectionUtils.array(new FastVector(location));
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends FastVector> getReturnType() {
		return FastVector.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "vector from " + location.toString(event, debug);
	}

}
