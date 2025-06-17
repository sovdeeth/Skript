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


/**
 * @author bi0qaw
 */
@Name("Vectors - Location Vector Offset")
@Description("Returns the location offset by vectors.")
@Examples({"set {_loc} to {_loc} ~ {_v}"})
@Since("2.2-dev28")
public class ExprLocationVectorOffset extends SimpleExpression<Location> {

	static {
		Skript.registerExpression(ExprLocationVectorOffset.class, Location.class, ExpressionType.COMBINED,
				"%location% offset by [[the] vectors] %vectors%",
				"%location%[ ]~[~][ ]%vectors%");
	}

	@SuppressWarnings("null")
	private Expression<Location> location;

	@SuppressWarnings("null")
	private Expression<FastVector> vectors;

	@Override
	@SuppressWarnings({"unchecked", "null"})
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		location = (Expression<Location>) exprs[0];
		vectors = (Expression<FastVector>) exprs[1];
		return true;
	}

	@SuppressWarnings("null")
	@Override
	protected Location[] get(Event e) {
		Location l = location.getSingle(e);
		if (l == null)
			return null;
		Location clone = l.clone();
		for (FastVector v : vectors.getArray(e))
			clone.add(v);
		return CollectionUtils.array(clone);
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Location> getReturnType() {
		return Location.class;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return location.toString() + " offset by " + vectors.toString();
	}

}
