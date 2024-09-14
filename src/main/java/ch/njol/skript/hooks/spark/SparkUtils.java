package ch.njol.skript.hooks.spark;

import me.lucko.spark.api.Spark;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.GenericStatistic;

public class SparkUtils {

	private static final Spark spark = SparkHook.getSparkInstance();

	public static Number[] getMSPTStats(int index) {
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
		double msptMinRounded = Math.round(info.min() * 100.0) / 100.0;
		double msptMedianRounded = Math.round(info.median() * 100.0) / 100.0;
		double mspt95PercentileRounded = Math.round(info.percentile95th() * 100.0) / 100.0;
		double msptMaxRounded = Math.round(info.max() * 100.0) / 100.0;
		return new Number[]{msptMinRounded, msptMedianRounded, mspt95PercentileRounded, msptMaxRounded};
	}

	private static Number[] getAllMSPTStats() {
		GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();
		DoubleAverageInfo mspt10Sec = mspt.poll(StatisticWindow.MillisPerTick.SECONDS_10);
		double msptMinRounded10Sec = Math.round(mspt10Sec.min() * 100.0) / 100.0;
		double msptMeanRounded10Sec = Math.round(mspt10Sec.median() * 100.0) / 100.0;
		double mspt95PercentileRounded10Sec = Math.round(mspt10Sec.percentile95th() * 100.0) / 100.0;
		double msptMaxRounded10Sec = Math.round(mspt10Sec.max() * 100.0) / 100.0;

		DoubleAverageInfo mspt1Min = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_1);
		double msptMinRoundedLastMin = Math.round(mspt1Min.min() * 100.0) / 100.0;
		double msptMeanRoundedLastMin = Math.round(mspt1Min.median() * 100.0) / 100.0;
		double mspt95PercentileRoundedLastMin = Math.round(mspt1Min.percentile95th() * 100.0) / 100.0;
		double msptMaxRoundedLastMin = Math.round(mspt1Min.max() * 100.0) / 100.0;

		DoubleAverageInfo mspt5Mins = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_5);
		double msptMinRounded5Mins = Math.round(mspt5Mins.min() * 100.0) / 100.0;
		double msptMeanRounded5Mins = Math.round(mspt5Mins.median() * 100.0) / 100.0;
		double mspt95PercentileRounded5Mins = Math.round(mspt5Mins.percentile95th() * 100.0) / 100.0;
		double msptMaxRounded5Mins = Math.round(mspt5Mins.max() * 100.0) / 100.0;

		return new Number[]{
			msptMinRounded10Sec, msptMeanRounded10Sec, mspt95PercentileRounded10Sec, msptMaxRounded10Sec,
			msptMinRoundedLastMin, msptMeanRoundedLastMin, mspt95PercentileRoundedLastMin, msptMaxRoundedLastMin,
			msptMinRounded5Mins, msptMeanRounded5Mins, mspt95PercentileRounded5Mins, msptMaxRounded5Mins
		};
	}

}
