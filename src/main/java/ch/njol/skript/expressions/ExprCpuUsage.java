package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
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


@Name("CPU (Central Processing Unit) Usage")
@Description("Returns the 3 most recent CPU Usage readings, like the information from Spark's /tps command." +
			"This expression is only supported with servers that have Spark on their server.")
@Examples({"broadcast \"%cpu usage from the last 10 seconds%\"",
	"broadcast \"%cpu usage from the last 1 minute%\"",
	"broadcast \"%cpu usage from the last 15 minutes%\"",
	"broadcast \"%cpu usage%\""})
@Since("INSERT VERSION")
public class ExprCpuUsage extends SimpleExpression<Number> {

	private int index;
	private String expr = "cpu";

	static {
		Skript.registerExpression(ExprCpuUsage.class, Number.class, ExpressionType.SIMPLE,
			"cpu usage from [the] last 10[ ]s[seconds]",
			"cpu usage from [the] last ([1] minute|1[ ]m[inute])",
			"cpu usage from [the] last 15[ ]m[inutes]",
			"[the] cpu usage");
	}

	private static Spark getSparkInstance() {
		try {
			Spark spark = SparkProvider.get();
			if (spark != null) {
				return SparkProvider.get();
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		Spark spark = getSparkInstance();
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
			case 0 -> {
				double usageLast10Seconds = cpuUsage.poll(StatisticWindow.CpuUsage.SECONDS_10);
				Number usage10Seconds = (Number) (usageLast10Seconds * 100);
				yield new Number[]{usage10Seconds};
			}
			case 1 -> {
				double usageLast1Minute = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_1);
				Number usage1Minute = (Number) (usageLast1Minute * 100);
				yield new Number[]{usage1Minute};
			}
			case 2 -> {
				double usageLast15Minutes = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_15);
				Number usage15Minutes = (Number) (usageLast15Minutes * 100);
				yield new Number[]{usage15Minutes};
			}
			default -> {
				double usageLast10Seconds = cpuUsage.poll(StatisticWindow.CpuUsage.SECONDS_10);
				double usageLast1Minute = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_1);
				double usageLast15Minutes = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_15);
				Number number1 = (Number) (usageLast10Seconds * 100);
				Number number2 = (Number) (usageLast1Minute * 100);
				Number number3 = (Number) (usageLast15Minutes * 100);
				yield new Number[]{number1, number2, number3};
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
