package ch.njol.skript.hooks.spark.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.hooks.spark.SparkHook;
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

public class ExprMSPT extends SimpleExpression<Number> {

	private int index;
	private String expr = "mspt";

	static {
		Skript.registerExpression(ExprMSPT.class, Number.class, ExpressionType.SIMPLE,
			"[the] server tick [(duration|time] [(within|from) the last 10[ ]s[econds]]",
			"[the] server tick [(duration|time] [(within|from) the last ([1] minute|1[ ]m[inute])]",
			"[the] server tick [(duration|time] [(within|from) the last 5[ ]m[inutes]]",
			"[the] server tick [(duration|time[s]");
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		Spark spark = SparkProvider.get();
		if (spark != null) {
			expr = parseResult.expr;
			index = matchedPattern;
			return true;
		}
		return false;
	}

	@Override
	protected Number @Nullable [] get(Event event) {
		Spark spark = SparkProvider.get();
		GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();
		return switch (index) {
			case 0 -> {
				DoubleAverageInfo mspt10Sec = mspt.poll(StatisticWindow.MillisPerTick.SECONDS_10);
				double msptMean = mspt10Sec.mean();
				double mspt95Percentile = mspt10Sec.percentile95th();
				yield new Number[]{msptMean / mspt95Percentile};
			}
			case 1 -> {
				DoubleAverageInfo msptLastMin = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_1);
				double msptMean = msptLastMin.mean();
				double mspt95Percentile = msptLastMin.percentile95th();
				yield new Number[]{msptMean / mspt95Percentile};
			}
			case 2 -> {
				DoubleAverageInfo msptLast5Min = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_5);
				double msptMean = msptLast5Min.mean();
				double mspt95Percentile = msptLast5Min.percentile95th();
				yield new Number[]{msptMean / mspt95Percentile};
			}
			default -> {
				DoubleAverageInfo mspt10Sec = mspt.poll(StatisticWindow.MillisPerTick.SECONDS_10);
				double msptMean10Sec = mspt10Sec.mean();
				double mspt95Percentile10Sec = mspt10Sec.percentile95th();
				DoubleAverageInfo msptLastMin = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_1);
				double msptMean1Min = msptLastMin.mean();
				double mspt95Percentile1Min = msptLastMin.percentile95th();
				DoubleAverageInfo msptLast5Min = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_5);
				double msptMean5Min = msptLast5Min.mean();
				double mspt95Percentile5Min = msptLast5Min.percentile95th();
				yield new Number[]{msptMean10Sec / mspt95Percentile10Sec, msptMean1Min / mspt95Percentile1Min, msptMean5Min / mspt95Percentile5Min};
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
