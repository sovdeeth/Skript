package ch.njol.skript.hooks.spark.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.hooks.spark.SparkHook;
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
@Description("Returns the MSPT usage readings, like the MSPT information from Spark's /mspt command. \n This expression is only supported with servers that have Spark on their server.")
@Examples({
	"broadcast server tick"
})
public class ExprMSPT extends SimpleExpression<Number> {

	private int index;
	private String expr = "server tick";
	private static final Spark spark = SparkHook.getSparkInstance();

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
		return switch (index) {
			case 0 -> {
				DoubleAverageInfo mspt10Sec = mspt.poll(StatisticWindow.MillisPerTick.SECONDS_10);
				double msptMin = mspt10Sec.min();
				double msptMean = mspt10Sec.mean();
				double mspt95Percentile = mspt10Sec.percentile95th();
				double msptMax = mspt10Sec.max();
				double msptMinRounded = Math.round(msptMin * 100.0) / 100.0;
				double msptMeanRounded = Math.round(msptMean * 100.0) / 100.0;
				double mspt95PercentileRounded = Math.round(mspt95Percentile * 100.0) / 100.0;
				double msptMaxRounded = Math.round(msptMax * 100.0) / 100.0;
				yield new Number[]{msptMinRounded, msptMeanRounded, mspt95PercentileRounded, msptMaxRounded};
			}
			case 1 -> {
				DoubleAverageInfo mspt1Min = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_1);
				double msptMin = mspt1Min.min();
				double msptMean = mspt1Min.mean();
				double mspt95Percentile = mspt1Min.percentile95th();
				double msptMax = mspt1Min.max();
				double msptMinRounded = Math.round(msptMin * 100.0) / 100.0;
				double msptMeanRounded = Math.round(msptMean * 100.0) / 100.0;
				double mspt95PercentileRounded = Math.round(mspt95Percentile * 100.0) / 100.0;
				double msptMaxRounded = Math.round(msptMax * 100.0) / 100.0;
				yield new Number[]{msptMinRounded, msptMeanRounded, mspt95PercentileRounded, msptMaxRounded};
			}
			case 2 -> {
				DoubleAverageInfo mspt5Mins = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_5);
				double msptMin = mspt5Mins.min();
				double msptMean = mspt5Mins.mean();
				double mspt95Percentile = mspt5Mins.percentile95th();
				double msptMax = mspt5Mins.max();
				double msptMinRounded = Math.round(msptMin * 100.0) / 100.0;
				double msptMeanRounded = Math.round(msptMean * 100.0) / 100.0;
				double mspt95PercentileRounded = Math.round(mspt95Percentile * 100.0) / 100.0;
				double msptMaxRounded = Math.round(msptMax * 100.0) / 100.0;
				yield new Number[]{msptMinRounded, msptMeanRounded, mspt95PercentileRounded, msptMaxRounded};
			}
			default -> {
				DoubleAverageInfo mspt10Sec = mspt.poll(StatisticWindow.MillisPerTick.SECONDS_10);
				double msptMin10Sec = mspt10Sec.min();
				double msptMean10Sec = mspt10Sec.mean();
				double mspt95Percentile10Sec = mspt10Sec.percentile95th();
				double msptMax10Sec = mspt10Sec.max();
				double msptMinRounded10Sec = Math.round(msptMin10Sec * 100.0) / 100.0;
				double msptMeanRounded10Sec = Math.round(msptMean10Sec * 100.0) / 100.0;
				double mspt95PercentileRounded10Sec = Math.round(mspt95Percentile10Sec * 100.0) / 100.0;
				double msptMaxRounded10Sec = Math.round(msptMax10Sec * 100.0) / 100.0;

				DoubleAverageInfo mspt1Min = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_1);
				double msptLastMinuteMin = mspt1Min.min();
				double msptMeanLastMin = mspt1Min.mean();
				double mspt95PercentileLastMin = mspt1Min.percentile95th();
				double msptMaxLastMin = mspt1Min.max();
				double msptMinRoundedLastMin = Math.round(msptLastMinuteMin * 100.0) / 100.0;
				double msptMeanRoundedLastMin = Math.round(msptMeanLastMin * 100.0) / 100.0;
				double mspt95PercentileRoundedLastMin = Math.round(mspt95PercentileLastMin * 100.0) / 100.0;
				double msptMaxRoundedLastMin = Math.round(msptMaxLastMin * 100.0) / 100.0;

				DoubleAverageInfo mspt5Mins = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_5);
				double msptMin5Mins = mspt5Mins.min();
				double msptMean5Mins = mspt5Mins.mean();
				double mspt95Percentile5Mins = mspt5Mins.percentile95th();
				double msptMax5Mins = mspt5Mins.max();
				double msptMinRounded5Mins = Math.round(msptMin5Mins * 100.0) / 100.0;
				double msptMeanRounded5Mins = Math.round(msptMean5Mins * 100.0) / 100.0;
				double mspt95PercentileRounded5Mins = Math.round(mspt95Percentile5Mins * 100.0) / 100.0;
				double msptMaxRounded5Mins = Math.round(msptMax5Mins * 100.0) / 100.0;

				yield new Number[]{msptMinRounded10Sec, msptMeanRounded10Sec, mspt95PercentileRounded10Sec, msptMaxRounded10Sec, msptMinRoundedLastMin, msptMeanRoundedLastMin, mspt95PercentileRoundedLastMin, msptMaxRoundedLastMin,  msptMinRounded5Mins, msptMeanRounded5Mins, mspt95PercentileRounded5Mins, msptMaxRounded5Mins};
			}
		};
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
