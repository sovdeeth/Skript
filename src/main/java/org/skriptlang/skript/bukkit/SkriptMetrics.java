package org.skriptlang.skript.bukkit;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.config.Option;
import ch.njol.skript.localization.Language;
import ch.njol.skript.update.Updater;
import ch.njol.skript.util.Version;
import ch.njol.skript.util.chat.ChatMessages;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * helper class to handle bstats metrics
 */
public class SkriptMetrics {

	/**
	 * Helper method to set up bstats charts on the supplied Metrics object
	 * @param metrics The Metrics object to which charts will be added.
	 */
	public static void setupMetrics(Metrics metrics) {
		// Enable metrics and register custom charts

		// sets up the old charts to prevent data splitting due to various user version
		setupLegacyMetrics(metrics);

		// add custom version charts for easier reading:
		metrics.addCustomChart(new DrilldownPie("drilldownPluginVersion", () -> {
			Version version = Skript.getVersion();
			//noinspection DuplicatedCode
			String featureVersionString = version.getMajor() + "." + version.getMinor();

			Map<String, Integer> preciseVersionEntry = new HashMap<>(1);
			preciseVersionEntry.put(version.toString(), 1);

			Map<String, Map<String, Integer>> featureVersionEntry = new HashMap<>(1);
			featureVersionEntry.put(featureVersionString, preciseVersionEntry);

			return featureVersionEntry;
		}));

		metrics.addCustomChart(new DrilldownPie("drilldownMinecraftVersion", () -> {
			Version version = Skript.getMinecraftVersion();
			//noinspection DuplicatedCode
			String majorVersionString = version.getMajor() + "." + version.getMinor();

			Map<String, Integer> preciseVersionEntry = new HashMap<>(1);
			preciseVersionEntry.put(version.toString(), 1);

			Map<String, Map<String, Integer>> majorVersionEntry = new HashMap<>(1);
			majorVersionEntry.put(majorVersionString, preciseVersionEntry);

			return majorVersionEntry;
		}));

		metrics.addCustomChart(new SimplePie("buildFlavor", () -> {
			Updater updater = Skript.getInstance().getUpdater();
			if (updater != null)
				return updater.getCurrentRelease().flavor;
			return "unknown";
		}));

		//
		// config options
		//

		metrics.addCustomChart(new DrilldownPie("drilldownPluginLanguage", () -> {
			String lang = Language.getName();
			return isDefaultMap(lang, SkriptConfig.language.defaultValue());
		}));

		metrics.addCustomChart(new DrilldownPie("drilldownUpdateChecker", () -> {
			Map<String, Integer> charEntry = new HashMap<>(1);
			charEntry.put(SkriptConfig.updateCheckInterval.value().toString(), 1);

			Map<String, Map<String, Integer>> checkingEntry = new HashMap<>(1);
			checkingEntry.put(SkriptConfig.checkForNewVersion.value().toString(), charEntry);

			return checkingEntry;
		}));
		metrics.addCustomChart(new SimplePie("releaseChannel", SkriptConfig.releaseChannel::value));

		// effect commands
		metrics.addCustomChart(new DrilldownPie("drilldownEffectCommands", () -> {
			Map<String, Integer> charEntry = new HashMap<>(1);
			charEntry.put(SkriptConfig.effectCommandToken.value(), 1);

			Map<String, Map<String, Integer>> hasEntry = new HashMap<>(1);
			hasEntry.put(SkriptConfig.enableEffectCommands.value().toString(), charEntry);

			return hasEntry;

		}));
		metrics.addCustomChart(new SimplePie("effectCommandsOps", () ->
			SkriptConfig.allowOpsToUseEffectCommands.value().toString()
		));
		metrics.addCustomChart(new SimplePie("logEffectCommands", () ->
			SkriptConfig.logEffectCommands.value().toString()
		));

		metrics.addCustomChart(new SimplePie("loadDefaultAliases", () ->
			SkriptConfig.loadDefaultAliases.value().toString()
		));

		metrics.addCustomChart(new SimplePie("playerVariableFix", () ->
			SkriptConfig.enablePlayerVariableFix.value().toString()
		));
		metrics.addCustomChart(new SimplePie("uuidsWithPlayers", () ->
			SkriptConfig.usePlayerUUIDsInVariableNames.value().toString()
		));

		metrics.addCustomChart(new DrilldownPie("drilldownDateFormat", () -> {
			SimpleDateFormat dateFormat = (SimpleDateFormat) SkriptConfig.dateFormat.value();
			boolean isDefault = dateFormat.equals(SkriptConfig.dateFormat.defaultValue());

			Map<String, Integer> valueEntry = new HashMap<>(1);
			valueEntry.put(dateFormat.toPattern(),  1);

			Map<String, Map<String, Integer>> isDefaultEntry = new HashMap<>(1);
			isDefaultEntry.put(isDefault ? "default" : "other", valueEntry);

			return isDefaultEntry;
		}));

		metrics.addCustomChart(new DrilldownPie("drilldownLogVerbosity", () -> {
			String verbosity = SkriptConfig.verbosity.value().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
			return isDefaultMap(verbosity, SkriptConfig.verbosity.defaultValue());
		}));

		metrics.addCustomChart(new DrilldownPie("drilldownPluginPriority", () -> {
			String priority = SkriptConfig.defaultEventPriority.value().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
			return isDefaultMap(priority, SkriptConfig.defaultEventPriority.defaultValue());
		}));
		metrics.addCustomChart(new SimplePie("cancelledByDefault", () ->
			SkriptConfig.listenCancelledByDefault.value().toString()
		));

		metrics.addCustomChart(new DrilldownPie("drilldownNumberAccuracy", () -> isDefaultMap(SkriptConfig.numberAccuracy)));

		metrics.addCustomChart(new DrilldownPie("drilldownMaxTargetDistance", () -> isDefaultMap(SkriptConfig.maxTargetBlockDistance)));

		metrics.addCustomChart(new SimplePie("caseSensitiveFunctions", () ->
			SkriptConfig.caseSensitive.value().toString()
		));
		metrics.addCustomChart(new SimplePie("caseSensitiveVariables", () ->
			SkriptConfig.caseInsensitiveVariables.value().toString()
		));
		metrics.addCustomChart(new SimplePie("caseSensitiveCommands", () ->
			SkriptConfig.caseInsensitiveCommands.value().toString()
		));

		metrics.addCustomChart(new SimplePie("disableSaveWarnings", () ->
			SkriptConfig.disableObjectCannotBeSavedWarnings.value().toString()
		));
		metrics.addCustomChart(new SimplePie("disableAndOrWarnings", () ->
			SkriptConfig.disableMissingAndOrWarnings.value().toString()
		));
		metrics.addCustomChart(new SimplePie("disableStartsWithWarnings", () ->
			SkriptConfig.disableVariableStartingWithExpressionWarnings.value().toString()
		));

		metrics.addCustomChart(new SimplePie("softApiExceptions", () ->
			SkriptConfig.apiSoftExceptions.value().toString()
		));

		metrics.addCustomChart(new SimplePie("timingsStatus", () -> {
			if (!Skript.classExists("co.aikar.timings.Timings"))
				return "unsupported";
			return SkriptConfig.enableTimings.value().toString();
		}));

		metrics.addCustomChart(new SimplePie("parseLinks", () ->
			ChatMessages.linkParseMode.name().toLowerCase(Locale.ENGLISH)
		));

		metrics.addCustomChart(new SimplePie("colorResetCodes", () ->
			SkriptConfig.colorResetCodes.value().toString()
		));

		metrics.addCustomChart(new SimplePie("keepLastUsage", () ->
			SkriptConfig.keepLastUsageDates.value().toString()
		));

		metrics.addCustomChart(new DrilldownPie("drilldownParsetimeWarningThreshold", () -> isDefaultMap(SkriptConfig.longParseTimeWarningThreshold, "disabled")));
	}

	/**
	 * Helper method to set up legacy charts (pre 2.9.2)
	 * @param metrics The Metrics object to which charts will be added.
	 */
	private static void setupLegacyMetrics(Metrics metrics) {
		// Enable metrics and register legacy charts
		metrics.addCustomChart(new SimplePie("pluginLanguage", Language::getName));
		metrics.addCustomChart(new SimplePie("updateCheckerEnabled", () ->
			SkriptConfig.checkForNewVersion.value().toString()
		));
		metrics.addCustomChart(new SimplePie("logVerbosity", () ->
			SkriptConfig.verbosity.value().name().toLowerCase(Locale.ENGLISH).replace('_', ' ')
		));
		metrics.addCustomChart(new SimplePie("pluginPriority", () ->
			SkriptConfig.defaultEventPriority.value().name().toLowerCase(Locale.ENGLISH).replace('_', ' ')
		));
		metrics.addCustomChart(new SimplePie("effectCommands", () ->
			SkriptConfig.enableEffectCommands.value().toString()
		));
		metrics.addCustomChart(new SimplePie("maxTargetDistance", () ->
			SkriptConfig.maxTargetBlockDistance.value().toString()
		));
	}

	/**
	 * Provides a Map for use with a {@link DrilldownPie} chart. Meant to be used in cases where a single default option has majority share, with many or custom alternative options.
	 * Creates a chart where the default option is presented against "other", then clicking on "other" shows the alternative options.
	 * @param value The option the user chose.
	 * @param defaultValue The default option for this chart.
	 * @return A Map that can be returned directly to a {@link DrilldownPie}.
	 * @param <T> The type of the option.
	 */
	private static <T> Map<String,Map<String, Integer>> isDefaultMap(@Nullable T value, T defaultValue) {
		return isDefaultMap(value, defaultValue ,defaultValue.toString());
	}

	/**
	 * Provides a Map for use with a {@link DrilldownPie} chart. Meant to be used in cases where a single default option has majority share, with many or custom alternative options.
	 * Creates a chart where the default option is presented against "other", then clicking on "other" shows the alternative options.
	 * @param value The option the user chose.
	 * @param defaultValue The default option for this chart.
	 * @param defaultLabel The label to use as the default option for this chart
	 * @return A Map that can be returned directly to a {@link DrilldownPie}.
	 * @param <T> The type of the option.
	 */
	private static <T> Map<String,Map<String, Integer>> isDefaultMap(@Nullable T value, @Nullable T defaultValue, String defaultLabel) {
		Map<String, Integer> valueEntry = new HashMap<>(1);
		valueEntry.put(String.valueOf(value), 1);

		Map<String, Map<String, Integer>> isDefault = new HashMap<>(1);
		isDefault.put(Objects.equals(value, defaultValue) ? defaultLabel : "other", valueEntry);

		return isDefault;
	}

	/**
	 * Provides a Map for use with a {@link DrilldownPie} chart. Meant to be used in cases where a single default option has majority share, with many or custom alternative options.
	 * Creates a chart where the default option is presented against "other", then clicking on "other" shows the alternative options.
	 * @param option The {@link Option} from which to pull the current and default values
	 * @return A Map that can be returned directly to a {@link DrilldownPie}.
	 * @param <T> The type of the option.
	 */
	private static <T> Map<String,Map<String, Integer>> isDefaultMap(Option<T> option) {
		return isDefaultMap(option.value(), option.defaultValue(), option.defaultValue().toString());

	}

	/**
	 * Provides a Map for use with a {@link DrilldownPie} chart. Meant to be used in cases where a single default option has majority share, with many or custom alternative options.
	 * Creates a chart where the default option is presented against "other", then clicking on "other" shows the alternative options.
	 * @param option The {@link Option} from which to pull the current and default values
	 * @param defaultLabel The label to use as the default option for this chart
	 * @return A Map that can be returned directly to a {@link DrilldownPie}.
	 * @param <T> The type of the option.
	 */
	private static <T> Map<String,Map<String, Integer>> isDefaultMap(Option<T> option, String defaultLabel) {
		return isDefaultMap(option.value(), option.defaultValue(), defaultLabel);
	}

}
