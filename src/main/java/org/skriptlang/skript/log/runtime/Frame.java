package org.skriptlang.skript.log.runtime;

import ch.njol.skript.util.Utils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
	 * Stores outputs of a frame. Unused values will be empty strings or empty lists.
	 * @param notDisplayed The text detailing what errors were not displayed.
	 * @param timeouts A list of strings that detail which sources were put in timeout.
	 * @param notification A string used to notify admins of runtime errors.
	 */
	public record FrameOutput(String notDisplayed, List<String> timeouts, String notification) { }

	private final static String plural = "s were";
	private final static String singular= " was";

	private final String type;
	private final FrameLimit limits;

	private int skipped;
	private int printed;
	private final Map<ErrorSource, Integer> lineTotals;
	private final Map<ErrorSource, Integer> lineSkipped;
	private final Map<ErrorSource, Integer> timeouts;

	public Frame(String type, FrameLimit limits) {
		this.type = type;
		this.limits = limits;
		lineTotals = new ConcurrentHashMap<>();
		lineSkipped = new ConcurrentHashMap<>();
		timeouts = new ConcurrentHashMap<>();
	}

	public boolean add(@NotNull RuntimeError error) {
		ErrorSource source = error.source();
		// increment counter
		int lineTotal = lineTotals.compute(source, (key, count) -> (count == null ? 1 : count + 1));

		// don't print if in timeout
		if (timeouts.containsKey(source)) {
			skipped++;
			lineSkipped.compute(source, (key, count) -> (count == null ? 1 : count + 1));
			return false;
		}

		// decide whether to print
		if (printed < limits.totalLimit && lineTotal <= limits.lineLimit) {
			printed++;
			return true;
		} else {
			skipped++;
			lineSkipped.compute(source, (key, count) -> (count == null ? 1 : count + 1));
			if (lineTotal == limits.lineTimeoutLimit) {
				timeouts.put(source, limits.timeoutDuration);
			}
			return false;
		}
	}

	public void nextFrame() {
		skipped = 0;
		printed = 0;
		lineTotals.clear();
		lineSkipped.clear();
		for (Iterator<Map.Entry<ErrorSource, Integer>> it = timeouts.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<ErrorSource, Integer> entry = it.next();
			if (entry.getValue() > 0)
				entry.setValue(entry.getValue() - 1);
			else
				it.remove();
		}
	}

	@Contract(" -> new")
	public @NotNull FrameOutput printFrame() {
		String skippedText = "";
		if (skipped > 0) {
			String linesNotDisplayed = lineSkipped.entrySet().stream()
				.map(entry -> "'"+ entry.getKey().script() + "' line " + entry.getKey().lineNumber() + " (" + entry.getValue() + ")")
				.collect(Collectors.joining(", "));

			skippedText = Utils.replaceEnglishChatStyles(
				"<gold>" + (skipped) + "<light red> runtime " + type + (skipped > 1 ? plural : singular) +
					" thrown in the last second but not displayed. From: " +
					"<gray>" + linesNotDisplayed + "\n \n");
		}

		List<String> timeoutTexts  = new ArrayList<>();
		for (Map.Entry<ErrorSource, Integer> entry : timeouts.entrySet()) {
			if (entry.getValue() == limits.timeoutDuration) {
				ErrorSource source = entry.getKey();
				timeoutTexts.add(Utils.replaceEnglishChatStyles(
					"<gold>Line " + source.lineNumber() + "<light red> of script '<gray>" + source.script() +
						"<light red>' produced too many runtime errors (<gray>" + limits.lineTimeoutLimit +
						"<light red> allowed per second). No errors from this line will be printed for " + limits.timeoutDuration + " frames.\n \n"));
			}
		}

		String notification = "";
		if (printed > 0) {
			// get various scripts from nodes
			Set<String> scripts = lineTotals.keySet().stream()
				.map(ErrorSource::script)
				.collect(Collectors.toSet());
			notification = "<light red>Script '<gray>" + scripts.iterator().next();
			if (scripts.size() > 1) {
				notification += "<light red>' and " + (scripts.size() - 1) + " others";
			}
			notification += "<light red> produced runtime errors. Please check console logs for details.";
		}

		return new FrameOutput(skippedText, timeoutTexts, notification);

	}

}
