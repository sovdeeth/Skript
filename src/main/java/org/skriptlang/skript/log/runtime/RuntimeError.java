package org.skriptlang.skript.log.runtime;

import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public record RuntimeError(Level level, ErrorSource source, String error, @Nullable String toHighlight) {

	private static final String CONFIG_NODE = "skript command.reload";
	private static final ArgsMessage WARNING_DETAILS = new ArgsMessage(CONFIG_NODE + ".warning details");
	private static final ArgsMessage ERROR_DETAILS = new ArgsMessage(CONFIG_NODE + ".error details");
	private static final ArgsMessage OTHER_DETAILS = new ArgsMessage(CONFIG_NODE + ".other details");
	private static final ArgsMessage ERROR_INFO = new ArgsMessage(CONFIG_NODE + ".runtime.error");
	private static final ArgsMessage WARNING_INFO = new ArgsMessage(CONFIG_NODE + ".runtime.warning");
	private static final ArgsMessage LINE_INFO = new ArgsMessage(CONFIG_NODE + ".runtime.line info");

	/**
	 * @return A formatted string that includes the error as well as any relevant metadata from the source.
	 */
	public @NotNull String toFormattedString() {
		ArgsMessage details;
		ArgsMessage info;
		if (level.intValue() == Level.WARNING.intValue()) { // warnings
			details = WARNING_DETAILS;
			info = WARNING_INFO;
		} else if (level.intValue() == Level.SEVERE.intValue()) { // errors
			details = ERROR_DETAILS;
			info = ERROR_INFO;
		} else { // anything else
			details = OTHER_DETAILS;
			info = WARNING_INFO;
		}

		String skriptInfo = replaceNewline(Utils.replaceEnglishChatStyles(info.getValue() == null ? info.key : info.getValue()));
		String errorInfo = replaceNewline(Utils.replaceEnglishChatStyles(details.getValue() == null ? details.key : details.getValue()));
		String lineInfo = replaceNewline(Utils.replaceEnglishChatStyles(LINE_INFO.getValue() == null ? LINE_INFO.key : LINE_INFO.getValue()));

		String code = source.lineText();
		if (toHighlight != null)
			code = code.replaceAll(toHighlight, "§f§n" + toHighlight + "§7");

		return String.format(skriptInfo, source.script(), source.syntaxName(), source.syntaxType()) +
			String.format(errorInfo, error.replaceAll("§", "&")) +
			String.format(lineInfo, source.lineNumber(), code);
	}

	private static @NotNull String replaceNewline(@NotNull String s) {
		return s.replaceAll("\\\\n", "\n");
	}

}
