package org.skriptlang.skript.log.runtime;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.skriptlang.skript.log.runtime.ErrorSource.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores the accumulated runtime errors over a span of time, then prints them.
 */
public final class Frame {

	/**
	 * Store limits for the number of issues a frame can print per frame.
	 * @param totalLimit total issues printed per frame
	 * @param lineLimit issues printed by one line per frame
	 * @param lineTimeoutLimit the limit at which a line will be put in timeout for exceeding.
	 * @param timeoutDuration the duration a line will stay in timeout, in frames.
	 */
	public record FrameLimit(int totalLimit, int lineLimit, int lineTimeoutLimit, int timeoutDuration) { }

	/**
	 * Stores outputs of a frame.
	 * @param totalErrors A map of error locations to number of errors produced for this frame.
	 * @param skippedErrors A map of error locations to number of errors not printed for this frame.
	 * @param newTimeouts A set of error locations that were timed out this frame.
	 * @param frameLimits The limits this frame is using.
	 */
	public record FrameOutput(
		@UnmodifiableView Map<Location, Integer> totalErrors,
		@UnmodifiableView Map<Location, Integer> skippedErrors,
		@UnmodifiableView Set<Location> newTimeouts,
		FrameLimit frameLimits
	) { }

	private final FrameLimit limits;

	private int printed;
	private final Map<Location, Integer> lineTotals;
	private final Map<Location, Integer> lineSkipped;
	private final Map<Location, Integer> timeouts;

	public Frame(FrameLimit limits) {
		this.limits = limits;
		lineTotals = new ConcurrentHashMap<>();
		lineSkipped = new ConcurrentHashMap<>();
		timeouts = new ConcurrentHashMap<>();
	}

	public boolean add(@NotNull RuntimeError error) {
		Location location = error.source().location();
		// increment counter
		int lineTotal = lineTotals.compute(location, (key, count) -> (count == null ? 1 : count + 1));

		// don't print if in timeout
		if (timeouts.containsKey(location)) {
			lineSkipped.compute(location, (key, count) -> (count == null ? 1 : count + 1));
			return false;
		}

		// decide whether to print
		if (printed < limits.totalLimit && lineTotal <= limits.lineLimit) {
			printed++;
			return true;
		} else {
			lineSkipped.compute(location, (key, count) -> (count == null ? 1 : count + 1));
			if (lineTotal == limits.lineTimeoutLimit) {
				timeouts.put(location, limits.timeoutDuration);
			}
			return false;
		}
	}

	public void nextFrame() {
		printed = 0;
		lineTotals.clear();
		lineSkipped.clear();
		for (Iterator<Map.Entry<Location, Integer>> it = timeouts.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Location, Integer> entry = it.next();
			if (entry.getValue() > 0) {
				entry.setValue(entry.getValue() - 1);
			} else {
				it.remove();
			}
		}
	}

	@Contract(" -> new")
	public @NotNull FrameOutput printFrame() {
		Set<Location> newTimeouts = new HashSet<>();
		for (Map.Entry<Location, Integer> entry : timeouts.entrySet()) {
			if (entry.getValue() == limits.timeoutDuration) {
				newTimeouts.add(entry.getKey());
			}
		}

		return new FrameOutput(
				Collections.unmodifiableMap(lineTotals),
				Collections.unmodifiableMap(lineSkipped),
				Collections.unmodifiableSet(newTimeouts),
				limits);
	}

}
