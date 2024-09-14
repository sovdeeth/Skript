package ch.njol.skript.hooks.spark.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.hooks.spark.SparkHook;
import ch.njol.skript.hooks.spark.SparkUtils;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("MSPT Usage")
@RequiredPlugins("Spark")
@Description("Returns the MSPT (Milliseconds per tick) readings from the last 10 seconds, 1 minute and 5 minutes. \n This expression can only be used if the server has the Spark plugin installed.")
@Examples({
	"broadcast server tick"
})
public class ExprMSPT extends SimpleExpression<Number> {

	private int index;
	private String expr = "server tick";
	private static final Spark spark = (Spark) SparkHook.getSparkInstance();

	static {
		if (spark != null) {
			Skript.registerExpression(ExprMSPT.class, Number.class, ExpressionType.SIMPLE,
				"[the] (server tick|mspt) (duration|time) (within|from) the last 10[ ]s[econds]",
				"[the] (server tick|mspt) (duration|time) (within|from) the last ([1] minute|1[ ]m[inute])",
				"[the] (server tick|mspt) (duration|time) (within|from) the last 5[ ]m[inutes]",
				"[the] (server tick|mspt) [(duration[s]|time[s])]");
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
