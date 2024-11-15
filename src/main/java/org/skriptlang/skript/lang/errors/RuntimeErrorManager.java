package org.skriptlang.skript.lang.errors;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.Utils;
import org.bukkit.Bukkit;
import org.skriptlang.skript.lang.script.Script;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles duplicate and spammed runtime errors.
 */
public class RuntimeErrorManager {

	/*
	a) route all errors through here
	b) handle formatting and sending
	c) keep track of source and frequency to prevent spam/duplicates
	d) config option to limit error count per tick, and # of ticks before a node can trigger another error.

	plan:
	per-node counter
	global counter

	if global counter for current tick > config (8) && warning, don't send more
	if global counter for current tick > config (16) && error, don't send more

	if per-node counter > config (1) && warning, don't send more from this node
	if per-node counter > config (2) && error, don't send more from this node

	all unsent errors/warnings tallied up and total sent at end of tick

	if a node errors config (10 ticks) in a row, notify user and pause this node's warning for config (5 seconds)

	Use annotations to allow per-node configuration?

	 */

	final int globalErrorLimit = 4;
	final int globalWarningLimit = 4;

	final int perNodeErrorLimit = 1;
	final int perNodeWarningLimit = 1;

	int globalErrors;
	int globalWarnings;
	final Map<Node, Integer> perLineErrors = new ConcurrentHashMap<>();
	final Map<Node, Integer> perLineWarnings = new ConcurrentHashMap<>();


	final Set<Node> lineTimeouts = new HashSet<>();


	public RuntimeErrorManager() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(Skript.getInstance(), ()->{
			if (globalErrors > globalErrorLimit) {
				SkriptLogger.sendFormatted(Bukkit.getConsoleSender(), Utils.replaceEnglishChatStyles("\n<gold>" + (globalErrors - globalErrorLimit) + "<light red> runtime errors were thrown but not displayed this tick.\n"));
			}
			if (globalWarnings > globalWarningLimit) {
				SkriptLogger.sendFormatted(Bukkit.getConsoleSender(), Utils.replaceEnglishChatStyles("\n<gold>" + (globalWarnings - globalWarningLimit) + "<light red> runtime warnings were thrown but not displayed this tick.\n"));
			}
			globalErrors = 0;
			globalWarnings = 0;
			perLineErrors.clear();
			perLineWarnings.clear();
		}, 1, 1);
	}

	void error(Node node, String message) {
		// ignore nodes that have been put in timeout.
		if (lineTimeouts.contains(node))
			return;
		// increment counters
		++globalErrors;
		int lineErrors = perLineErrors.compute(node, (key, count) -> (count == null ? 0 : count + 1));
		// print if < limit
		if (globalErrors <= globalErrorLimit && lineErrors <= perNodeErrorLimit) {
			SkriptLogger.sendFormatted(Bukkit.getConsoleSender(), message);
		}
		// timeout if too many
		if (lineErrors > perNodeErrorLimit * 2) {
			lineTimeouts.add(node);
			SkriptLogger.sendFormatted(Bukkit.getConsoleSender(), Utils.replaceEnglishChatStyles("<gold>Line " + node.getLine() + "<light red> of script '<gray>" + node.getConfig().getFileName() + "<light red>' produced too many runtime errors in one tick (<gray>" + perNodeErrorLimit * 2 + "<light red> allowed per tick). No further errors from this line will be printed until the script is reloaded."));
		}
	}

	public void warning(Node node, String message) {
		// increment counters
		++globalWarnings;
		int lineWarnings = perLineWarnings.compute(node, (key, count) -> (count == null ? 0 : count + 1));
		// print if < limit
		if (globalWarnings <= globalWarningLimit && lineWarnings <= perNodeWarningLimit) {
			SkriptLogger.sendFormatted(Bukkit.getConsoleSender(), message);
		}

	}

	public void unlockNodes(Script script) {
		lineTimeouts.removeIf((node -> node.getConfig().equals(script.getConfig())));
	}

}
