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

import java.util.stream.Stream;

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
	private int msptType;
	private static final GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();

	static {
		if (Skript.classExists("me.lucko.spark.api.SparkProvider")) {
			Skript.registerExpression(ExprMSPT.class, Number.class, ExpressionType.SIMPLE,
				"[the] (server tick (duration|time)|mspt) (from|of) [the] last (10|ten) seconds using (:min[imum]|:mean|:median|:95th percentile|:max[imum]|:all [the] values)",
				"[the] (server tick (duration|time)|mspt) (from|of) [the] last [(1|one)] minute using (:min[imum]|:mean|:median|:95th percentile|:max[imum]|:all [the] values)",
				"[the] (server tick (duration|time)|mspt) (from|of) [the] last (5|five) minutes using (:min[imum]|:mean|:median|:95th percentile|:max[imum]|:all [the] values)",
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
		return getMSPTStats(index, msptType);
	}

	@Override
	public boolean isSingle() {
		return index != 3 && msptType != 6;
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return expr;
	}

	private static Number[] getMSPTStats(int index, int msptType) {
		return switch (index) {
			case 0 -> getMSPTForWindow(StatisticWindow.MillisPerTick.SECONDS_10, msptType);
			case 1 -> getMSPTForWindow(StatisticWindow.MillisPerTick.MINUTES_1, msptType);
			case 2 -> getMSPTForWindow(StatisticWindow.MillisPerTick.MINUTES_5, msptType);
			default -> Stream.of(
				getAllMSPTStatsPerWindow(StatisticWindow.MillisPerTick.SECONDS_10),
				getAllMSPTStatsPerWindow(StatisticWindow.MillisPerTick.MINUTES_1),
				getAllMSPTStatsPerWindow(StatisticWindow.MillisPerTick.MINUTES_5)
			).flatMap(Stream::of).toArray(Number[]::new);
		};
	}

	private static Number[] getMSPTForWindow(StatisticWindow.MillisPerTick window, int index) {
		assert mspt != null;
		DoubleAverageInfo info = mspt.poll(window);
		return switch (index) {
			case 1 -> new Number[]{info.min()};
			case 2 -> new Number[]{info.mean()};
			case 3 -> new Number[]{info.median()};
			case 4 -> new Number[]{info.percentile95th()};
			case 5 -> new Number[]{info.max()};
			default -> getAllMSPTStatsPerWindow(window);
		};
	}

	private static Number[] getAllMSPTStatsPerWindow(StatisticWindow.MillisPerTick window) {
		assert mspt != null;
		DoubleAverageInfo info = mspt.poll(window);
		return new Number[]{info.min(), info.mean(), info.median(), info.percentile95th(), info.max()};
	}
}
