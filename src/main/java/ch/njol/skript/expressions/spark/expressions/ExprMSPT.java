package ch.njol.skript.expressions.spark.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.expressions.spark.SparkUtils;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("MSPT Usage")
@RequiredPlugins("Paper 1.21+ or Spark")
@Description({
    "Returns the MSPT (milliseconds per tick) readings from the last 10 seconds, 1 minute and 5 minutes.",
    "This expression can only be used if the server has Spark or you have Spark installed as a plugin."
})
@Examples({
	"broadcast server tick"
})
public class ExprMSPT extends SimpleExpression<Number> {

	private int index;
	private String expr = "server tick";
	private static Spark spark;

	static {
		if (Skript.classExists("me.lucko.spark.api.SparkProvider")) {
			spark = SparkProvider.get();
			Skript.registerExpression(ExprMSPT.class, Number.class, ExpressionType.SIMPLE,
				"[the] (server tick (duration|time)|mspt) (from|of) the last 10 seconds",
				"[the] (server tick (duration|time)|mspt) (from|of) the last [(1|one) ]minute",
				"[the] (server tick (duration|time)|mspt) (from|of) the last (5|five) minutes",
				"[the] (server tick (durations|times)|mspt values)");
		}
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		expr = parseResult.expr;
		index = matchedPattern;
		return true;
	}

	@Override
	protected Number @Nullable [] get(Event event) {
		GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();
		return SparkUtils.getMSPTStats(index);
	}

	@Override
	public boolean isSingle() {
		return index != 3;
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return expr;
	}
}
