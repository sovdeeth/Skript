package org.skriptlang.skript.bukkit.spark.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("CPU Usage")
@RequiredPlugins("Paper 1.21+ or Spark")
@Description({
	"Returns the CPU usage readings, like the cpu information from Spark's /tps command.",
	"These values return a percentage of how much of the cpu was being used in each specified time.",
	"This expression can only be used if the server has the Spark plugin installed."
})
@Examples({
	"broadcast cpu usage"
})
@Since("INSERT VERSION")
public class ExprCPUUsage extends SimpleExpression<Number> {

	private String expr = "cpu usage";
	private static Spark spark;
	private StatisticWindow.CpuUsage window;
	private int index;

	static {
		if (Skript.classExists("me.lucko.spark.api.Spark")) {
			Skript.registerExpression(ExprCPUUsage.class, Number.class, ExpressionType.SIMPLE,
				"[the] cpu usage (from|of) [the] last (10|ten) seconds",
				"[the] cpu usage (from|of) [the] last [1|one] minute",
				"[the] cpu usage (from|of) [the] last (15|fifteen) minutes",
				"[the] cpu usage");
		}
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		spark = SparkProvider.get();
		expr = parseResult.expr;
		index = matchedPattern;
		window = switch (matchedPattern) {
			case 0 -> StatisticWindow.CpuUsage.SECONDS_10;
			case 1 -> StatisticWindow.CpuUsage.MINUTES_1;
			case 2 -> StatisticWindow.CpuUsage.MINUTES_15;
			default -> null;
		};
		return true;
	}

	@Override
	protected Number @Nullable [] get(Event event) {
		DoubleStatistic<StatisticWindow.CpuUsage> cpuUsage = spark.cpuSystem();
		if (window != null) {
			return new Number[]{cpuUsage.poll(window) * 100};
		}
		Number usageLast10Seconds = cpuUsage.poll(StatisticWindow.CpuUsage.SECONDS_10) * 100;
		Number usageLast1Minute = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_1) * 100;
		Number usageLast15Minutes = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_15) * 100;
		return new Number[]{usageLast10Seconds, usageLast1Minute, usageLast15Minutes};
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
