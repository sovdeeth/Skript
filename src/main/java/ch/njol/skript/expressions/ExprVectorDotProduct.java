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
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.vector.FastVector;

@Name("Vectors - Dot Product")
@Description("Gets the dot product between two vectors.")
@Examples("set {_dot} to {_v1} dot {_v2}")
@Since("2.2-dev28")
public class ExprVectorDotProduct extends SimpleExpression<Number> {

	static {
		Skript.registerExpression(ExprVectorDotProduct.class, Number.class, ExpressionType.COMBINED, "%vector% dot %vector%");
	}

	@SuppressWarnings("null")
	private Expression<FastVector> first, second;

	@Override
	@SuppressWarnings({"unchecked", "null"})
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		first = (Expression<FastVector>) exprs[0];
		second = (Expression<FastVector>) exprs[1];
		return true;
	}

	@Override
	@SuppressWarnings("null")
	protected Number[] get(Event event) {
		FastVector first = this.first.getSingle(event);
		FastVector second = this.second.getSingle(event);
		if (first == null || second == null)
			return null;
		return CollectionUtils.array(first.getX() * second.getX() + first.getY() * second.getY() + first.getZ() * second.getZ());
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return first.toString(event, debug) + " dot " + second.toString(event, debug);
	}

}
