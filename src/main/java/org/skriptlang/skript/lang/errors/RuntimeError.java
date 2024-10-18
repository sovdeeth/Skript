package org.skriptlang.skript.lang.errors;

import ch.njol.skript.config.Node;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;

import java.util.logging.Level;

public record RuntimeError(Level level, String message, Node node) {

	public void log(LogHandler log) {
		SkriptLogger.log(new LogEntry(level, message, node));
	}


}
