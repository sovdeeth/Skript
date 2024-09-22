package org.skriptlang.skript.bukkit.spark.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
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
	"broadcast all server tick times"
})
public class ExprMSPT extends SimpleExpression<Number> {

	private int index;
	private String expr = "server tick";
	private static Spark spark;
	private static int msptType;

	static {
		if (Skript.classExists("me.lucko.spark.api.SparkProvider")) {
			Skript.registerExpression(ExprMSPT.class, Number.class, ExpressionType.SIMPLE,
				"[the] (server tick (duration|time)|mspt) (from|of) [the] last (10|ten) seconds using (:min[imum]|:mean|:median|:95th percentile|:max[imum]|:all)",
				"[the] (server tick (duration|time)|mspt) (from|of) [the] last [(1|one)] minute using (:min[imum]|:mean|:median|:95th percentile|:max[imum]|:all)",
				"[the] (server tick (duration|time)|mspt) (from|of) [the] last (5|five) minutes using (:min[imum]|:mean|:median|:95th percentile|:max[imum]|:all)",
				"all [the] (server tick (durations|times)|mspt values)");
		}
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		spark = SparkProvider.get();
		expr = parseResult.expr;
		index = matchedPattern;
		msptType = parseResult.mark;
		return true;
	}

	@Override
	protected Number @Nullable [] get(Event event) {
		spark = SparkProvider.get();
		return getMSPTStats(index);
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
		return expr + " from the last " + index;
	}

	private static Number[] getMSPTStats(int index) {
		return switch (index) {
			case 0 -> getMSPTForWindow(StatisticWindow.MillisPerTick.SECONDS_10);
			case 1 -> getMSPTForWindow(StatisticWindow.MillisPerTick.MINUTES_1);
			case 2 -> getMSPTForWindow(StatisticWindow.MillisPerTick.MINUTES_5);
			default -> getAllMSPTStats();
		};
	}

	private static Number[] getMSPTForWindow(StatisticWindow.MillisPerTick window) {
		GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();
		DoubleAverageInfo info = mspt.poll(window);
		return switch (msptType) {
			case 1 -> new Number[]{info.min()};
			case 2 -> new Number[]{info.mean()};
			case 3 -> new Number[]{info.median()};
			case 4 -> new Number[]{info.percentile95th()};
			case 5 -> new Number[]{info.max()};
			default -> getAllMSPTStatsPerWindow(window);
		};
	}

	private static Number[] getAllMSPTStatsPerWindow(StatisticWindow.MillisPerTick window) {
		GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();
		DoubleAverageInfo info = mspt.poll(window);
		return new Number[]{info.min(), info.mean(), info.median(), info.percentile95th(), info.max()};
	}

	private static Number[] getAllMSPTStats() {
		GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();
		DoubleAverageInfo mspt10Sec = mspt.poll(StatisticWindow.MillisPerTick.SECONDS_10);
		DoubleAverageInfo mspt1Min = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_1);
		DoubleAverageInfo mspt5Mins = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_5);

		Number msptMin10Sec = mspt10Sec.min();
		Number msptMean10Sec = mspt10Sec.mean();
		Number msptMedian10Sec = mspt10Sec.median();
		Number mspt95Percentile10Sec = mspt10Sec.percentile95th();
		Number msptMax10Sec = mspt10Sec.max();

		Number msptMinLastMin = mspt1Min.min();
		Number msptMeanLastMin = mspt1Min.mean();
		Number msptMedianLastMin = mspt1Min.median();
		Number mspt95PercentileLastMin = mspt1Min.percentile95th();
		Number msptMaxLastMin = mspt1Min.max();

		Number msptMin5Mins = mspt5Mins.min();
		Number msptMean5Mins = mspt5Mins.mean();
		Number msptMedian5Mins = mspt5Mins.median();
		Number mspt95Percentile5Mins = mspt5Mins.percentile95th();
		Number msptMax5Mins = mspt5Mins.max();

		return new Number[]{
			msptMin10Sec, msptMean10Sec, msptMedian10Sec, mspt95Percentile10Sec, msptMax10Sec,
			msptMinLastMin, msptMeanLastMin, msptMedianLastMin, mspt95PercentileLastMin, msptMaxLastMin,
			msptMin5Mins, msptMean5Mins, msptMedian5Mins, mspt95Percentile5Mins, msptMax5Mins
		};
	}
}
