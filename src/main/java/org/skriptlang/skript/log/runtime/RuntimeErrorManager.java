package org.skriptlang.skript.log.runtime;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.util.Task;
import ch.njol.skript.util.Timespan;
import org.skriptlang.skript.log.runtime.Frame.FrameLimit;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

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

		List<RuntimeErrorConsumer> oldConsumers = List.of();
		if (instance != null) {
			instance.close();
			oldConsumers = instance.consumers;
		}
		instance = new RuntimeErrorManager(Math.max((int) frameLength, 1), errorFrame, warningFrame);
		oldConsumers.forEach(consumer -> instance.addConsumer(consumer));
	}

	private final Frame errorFrame, warningFrame;
	private final Task task;

	private final List<RuntimeErrorConsumer> consumers = new ArrayList<>();

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
		task = new Task(Skript.getInstance(), frameLength, frameLength, true) {
			@Override
			public void run() {
				consumers.forEach(consumer -> consumer.printFrameOutput(errorFrame.printFrame()));
				errorFrame.nextFrame();

				consumers.forEach(consumer -> consumer.printFrameOutput(warningFrame.printFrame()));
				warningFrame.nextFrame();
			}
		};
	}

	public void error(RuntimeError error) {
		// print if < limit
		if (errorFrame.add(error)) {
			consumers.forEach((consumer -> consumer.printError(error)));
		}
	}

	public void warning(RuntimeError error){
		// print if < limit
		if (warningFrame.add(error)) {
			consumers.forEach((consumer -> consumer.printError(error)));
		}
	}

	public Frame getErrorFrame() {
		return errorFrame;
	}

	public Frame getWarningFrame() {
		return warningFrame;
	}

	public void addConsumer(RuntimeErrorConsumer consumer) {
		consumers.add(consumer);
	}

	public void removeConsumer(RuntimeErrorConsumer consumer) {
		consumers.remove(consumer);
	}

	@Override
	public void close() {
		task.close();
	}

}
