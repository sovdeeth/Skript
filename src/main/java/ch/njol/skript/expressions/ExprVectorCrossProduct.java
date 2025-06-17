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

@Name("Vectors - Cross Product")
@Description("Gets the cross product between two vectors.")
@Examples("send \"%vector 1, 0, 0 cross vector 0, 1, 0%\"")
@Since("2.2-dev28")
public class ExprVectorCrossProduct extends SimpleExpression<FastVector> {

	static {
		Skript.registerExpression(ExprVectorCrossProduct.class, FastVector.class, ExpressionType.COMBINED, "%vector% cross %vector%");
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
	protected FastVector[] get(Event event) {
		FastVector first = this.first.getSingle(event);
		FastVector second = this.second.getSingle(event);
		if (first == null || second == null)
			return null;
		return CollectionUtils.array(first.crossProduct(second, new FastVector()));
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
		return first.toString(event, debug) + " cross " + second.toString(event, debug);
	}

}
