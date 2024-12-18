package org.skriptlang.skript.log.runtime;

import ch.njol.skript.localization.ArgsMessage;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public record RuntimeError(Level level, ErrorSource source, String error, @Nullable String toHighlight) {

	public static final String CONFIG_NODE = "log.runtime";
	public static final ArgsMessage WARNING_DETAILS = new ArgsMessage("skript command.reload.warning details");
	public static final ArgsMessage ERROR_DETAILS = new ArgsMessage( "skript command.reload.error details");
	public static final ArgsMessage OTHER_DETAILS = new ArgsMessage("skript command.reload.other details");

	public static final ArgsMessage ERROR_INFO = new ArgsMessage(CONFIG_NODE + ".error");
	public static final ArgsMessage WARNING_INFO = new ArgsMessage(CONFIG_NODE + ".warning");
	public static final ArgsMessage LINE_INFO = new ArgsMessage(CONFIG_NODE + ".line info");

}
