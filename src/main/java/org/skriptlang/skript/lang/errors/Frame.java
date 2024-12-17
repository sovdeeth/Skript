package org.skriptlang.skript.lang.errors;

import ch.njol.skript.config.Node;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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

	private final static String plural = "s were";
	private final static String singular= " was";

	private final String type;
	private final FrameLimit limits;

	private int skipped;
	private int printed;
	private final Map<Node, Integer> lineTotals;
	private final Map<Node, Integer> lineSkipped;
	private final Map<Node, Integer> timeouts;

	public Frame(String type, FrameLimit limits) {
		this.type = type;
		this.limits = limits;
		lineTotals = new ConcurrentHashMap<>();
		lineSkipped = new ConcurrentHashMap<>();
		timeouts = new ConcurrentHashMap<>();
	}

	boolean add(Node node) {
		// increment counter
		int lineTotal = lineTotals.compute(node, (key, count) -> (count == null ? 1 : count + 1));

		// don't print if in timeout
		if (timeouts.containsKey(node)) {
			skipped++;
			lineSkipped.compute(node, (key, count) -> (count == null ? 1 : count + 1));
			return false;
		}

		// decide whether to print
		if (printed < limits.totalLimit && lineTotal <= limits.lineLimit) {
			printed++;
			return true;
		} else {
			skipped++;
			lineSkipped.compute(node, (key, count) -> (count == null ? 1 : count + 1));
			if (lineTotal == limits.lineTimeoutLimit) {
				timeouts.put(node, limits.timeoutDuration);
			}
			return false;
		}
	}

	void nextFrame() {
		skipped = 0;
		printed = 0;
		lineTotals.clear();
		lineSkipped.clear();
		for (Iterator<Map.Entry<Node, Integer>> it = timeouts.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Node, Integer> entry = it.next();
			if (entry.getValue() > 0)
				entry.setValue(entry.getValue() - 1);
			else
				it.remove();
		}
	}

	void printFrame() {
		if (skipped > 0) {
			String linesNotDisplayed = lineSkipped.entrySet().stream()
				.map(entry -> "'"+ entry.getKey().getConfig().getFileName() + "' line " + entry.getKey().getLine() + " (" + entry.getValue() + ")")
				.collect(Collectors.joining(", "));

			SkriptLogger.sendFormatted(Bukkit.getConsoleSender(), Utils.replaceEnglishChatStyles(
				"<gold>" + (skipped) + "<light red> runtime " + type + (skipped > 1 ? plural : singular) +
					" thrown in the last second but not displayed. From: " +
					"<gray>" + linesNotDisplayed + "\n \n"));
		}

		for (Map.Entry<Node, Integer> entry : timeouts.entrySet()) {
			if (entry.getValue() == limits.timeoutDuration) {
				Node node = entry.getKey();
				SkriptLogger.sendFormatted(Bukkit.getConsoleSender(), Utils.replaceEnglishChatStyles(
					"<gold>Line " + node.getLine() + "<light red> of script '<gray>" + node.getConfig().getFileName() +
						"<light red>' produced too many runtime errors (<gray>" + limits.lineTimeoutLimit +
						"<light red> allowed per second). No errors from this line will be printed for " + limits.timeoutDuration + " frames.\n \n"));
			}
		}

		if (printed > 0) {
			// get various scripts from nodes
			Set<String> scripts = lineTotals.keySet().stream()
				.map((node) -> node.getConfig().getFileName())
				.collect(Collectors.toSet());
			String message = "<light red>Script '<gray>" + scripts.iterator().next();
			if (scripts.size() > 1) {
				message += "<light red>' and " + (scripts.size() - 1) + " others";
			}
			message += "<light red> produced runtime errors. Please check console logs for details.";

			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.hasPermission(RuntimeErrorManager.ERROR_NOTIF_PERMISSION))
					SkriptLogger.sendFormatted(player, message);
			}
		}

	}

}
