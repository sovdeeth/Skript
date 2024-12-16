package org.skriptlang.skript.lang.errors;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.config.Node;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.Timespan;
import org.bukkit.Bukkit;
import org.skriptlang.skript.lang.errors.Frame.FrameLimit;

import java.io.Closeable;

/**
 * Handles duplicate and spammed runtime errors.
 */
public class RuntimeErrorManager implements Closeable {


	public final static String ERROR_NOTIF_PERMISSION = "skript.see_runtime_errors";

	private static RuntimeErrorManager instance;

	public static RuntimeErrorManager getInstance() {
		return instance;
	}

	/**
	 * Refreshes the runtime error manager for Skript, pulling from the config values.
	 */
	public static void refresh() {

		long frameLength = SkriptConfig.runtimeErrorFrameDuration.value().getAs(Timespan.TimePeriod.TICK);

		int errorLimit = SkriptConfig.runtimeErrorLimitTotal.value();
		int errorLineLimit = SkriptConfig.runtimeErrorLimitLine.value();
		int errorLineTimeout = SkriptConfig.runtimeErrorLimitLineTimeout.value();
		int errorTimeoutLength = Math.max(SkriptConfig.runtimeErrorTimeoutDuration.value(), 1);
		FrameLimit errorFrame = new FrameLimit(errorLimit, errorLineLimit, errorLineTimeout, errorTimeoutLength);

		int warningLimit = SkriptConfig.runtimeWarningLimitTotal.value();
		int warningLineLimit = SkriptConfig.runtimeWarningLimitLine.value();
		int warningLineTimeout = SkriptConfig.runtimeWarningLimitLineTimeout.value();
		int warningTimeoutLength = Math.max(SkriptConfig.runtimeWarningTimeoutDuration.value(), 1);
		FrameLimit warningFrame = new FrameLimit(warningLimit, warningLineLimit, warningLineTimeout, warningTimeoutLength);

		if (instance != null)
			instance.close();
		instance = new RuntimeErrorManager(Math.max((int) frameLength, 1), errorFrame, warningFrame);
	}

	private final Frame errorFrame, warningFrame;
	private final int taskId;

	/**
	 * Creates a new error manager, which also creates its own frames.
	 * <br>
	 * Must be closed when no longer being used.
	 *
	 * @param frameLength The length of a frame in ticks.
	 * @param errorLimits The limits to the error frame.
	 * @param warningLimits The limits to the warning frame.
	 */
	public RuntimeErrorManager(int frameLength, FrameLimit errorLimits, FrameLimit warningLimits) {
		errorFrame = new Frame("error", errorLimits);
		warningFrame = new Frame("warning", warningLimits);
		taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Skript.getInstance(), () -> {
			errorFrame.printFrame();
			errorFrame.nextFrame();

			warningFrame.printFrame();
			warningFrame.nextFrame();
		}, frameLength, frameLength);
	}


	void error(Node node, String message) {
		// print if < limit
		if (errorFrame.add(node)) {
			SkriptLogger.sendFormatted(Bukkit.getConsoleSender(), message);
		}
	}

	void warning(Node node, String message){
		// print if < limit
		if (warningFrame.add(node)) {
			SkriptLogger.sendFormatted(Bukkit.getConsoleSender(), message);
		}
	}

	public Frame getErrorFrame() {
		return errorFrame;
	}

	public Frame getWarningFrame() {
		return warningFrame;
	}

	@Override
	public void close() {
		Bukkit.getScheduler().cancelTask(taskId);
	}
}
