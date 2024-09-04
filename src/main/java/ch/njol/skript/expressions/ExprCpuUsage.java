package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;


@Name("CPU Usage")
@RequiredPlugins("Spark")
@Description("Returns the 3 most recent CPU usage readings, like the information from Spark's /tps command. " +
			"This expression is only supported with servers that have Spark on their server.")
@Examples({"broadcast \"%cpu usage from the last 10 seconds%\"",
	"broadcast \"%cpu usage from the last 1 minute%\"",
	"broadcast \"%cpu usage from the last 15 minutes%\"",
	"broadcast \"%cpu usage%\""})
@Since("INSERT VERSION")
public class ExprCpuUsage extends SimpleExpression<Number> {

	private int index;
	private String expr = "cpu usage";

	static {
		Skript.registerExpression(ExprCpuUsage.class, Number.class, ExpressionType.SIMPLE,
			"cpu usage from [the] last 10[ ]s[econds]",
			"cpu usage from [the] last ([1] minute|1[ ]m[inute])",
			"cpu usage from [the] last 15[ ]m[inutes]",
			"[the] cpu usage");
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
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
		DoubleStatistic<StatisticWindow.CpuUsage> cpuUsage = spark.cpuSystem();
		return switch (index) {
			case 0 -> new Number[]{cpuUsage.poll(StatisticWindow.CpuUsage.SECONDS_10) * 100};
			case 1 -> new Number[]{cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_1) * 100};
			case 2 -> new Number[]{cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_15) * 100};
			default -> new Number[]{cpuUsage.poll(StatisticWindow.CpuUsage.SECONDS_10) * 100, cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_1) * 100, cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_15) * 100};
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
