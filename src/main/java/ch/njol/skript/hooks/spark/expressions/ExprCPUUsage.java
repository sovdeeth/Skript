package ch.njol.skript.hooks.spark.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.hooks.spark.SparkHook;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("CPU Usage")
@RequiredPlugins("Spark")
@Description("Returns the CPU usage readings, like the cpu information from Spark's /tps command. \n This expression can only be used if the server has the Spark plugin installed.")
@Examples({
	"broadcast cpu usage"
})
@Since("INSERT VERSION")
public class ExprCPUUsage extends SimpleExpression<Number> {

	private String expr = "cpu usage";
	private static final Spark spark = (Spark) SparkHook.getSparkInstance();
	private StatisticWindow.CpuUsage window;

	static {
		if (spark != null) {
			Skript.registerExpression(ExprCPUUsage.class, Number.class, ExpressionType.SIMPLE,
				"[the] cpu usage from [the] last 10[ ]s[econds]",
				"[the] cpu usage from [the] last ([1] minute|1[ ]m[inute])",
				"[the] cpu usage from [the] last 15[ ]m[inutes]",
				"[the] cpu usage");
		}
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		DoubleStatistic<StatisticWindow.CpuUsage> cpuUsage = spark.cpuSystem();
		expr = parseResult.expr;
		switch (matchedPattern) {
			case 0 -> window = StatisticWindow.CpuUsage.SECONDS_10;
			case 1 -> window = StatisticWindow.CpuUsage.MINUTES_1;
			case 2 -> window = StatisticWindow.CpuUsage.MINUTES_15;
			case 3 -> window = null;
		}
		return true;
	}

	@Override
	protected Number @Nullable [] get(Event event) {
		DoubleStatistic<StatisticWindow.CpuUsage> cpuUsage = spark.cpuSystem();
		if (window != null) {
			return new Number[]{cpuUsage.poll(window) * 100};
		}
		double usageLast10Seconds = cpuUsage.poll(StatisticWindow.CpuUsage.SECONDS_10);
		double usageLast1Minute = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_1);
		double usageLast15Minutes = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_15);
		double usageLast10SecondsRounded = (double) Math.round(usageLast10Seconds * 100);
		double usageLast1MinuteRounded = (double) Math.round(usageLast1Minute * 100);
		double usageLast15MinutesRounded = (double) Math.round(usageLast15Minutes * 100);
		return new Number[]{usageLast10SecondsRounded, usageLast1MinuteRounded, usageLast15MinutesRounded};
	}

	@Override
	public boolean isSingle() {
		return true;
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
