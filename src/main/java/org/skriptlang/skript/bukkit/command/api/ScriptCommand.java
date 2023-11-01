/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package org.skriptlang.skript.bukkit.command.api;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.log.Verbosity;
import ch.njol.skript.patterns.MatchResult;
import ch.njol.skript.patterns.SkriptPattern;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.chat.BungeeConverter;
import ch.njol.skript.variables.Variables;
import ch.njol.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;
import java.util.Locale;

public class ScriptCommand {

	// TODO figure out if used
	public static final ArgsMessage M_TOO_MANY_ARGUMENTS = new ArgsMessage("commands.too many arguments");
	public static final Message M_INTERNAL_ERROR = new Message("commands.internal error");
	public static final Message M_CORRECT_USAGE = new Message("commands.correct usage");

	private static final String DEFAULT_NAMESPACE = "skript";

	private final String label;
	private final String namespace;
	private final String description;
	private final String usage;
	private final List<String> aliases;

	private final ExecutableBy executableBy;
	@Nullable
	private final String permission;
	private final VariableString permissionMessage;

	@Nullable
	private final CommandCooldown cooldown;

	private final Trigger trigger;
	private final List<Argument<?>> arguments;
	private final SkriptPattern pattern;

	public enum ExecutableBy {
		ALL(null),
		PLAYERS(new Message("commands.executable by players")),
		CONSOLE(new Message("commands.executable by console"));

		@Nullable
		private final Message message;

		ExecutableBy(@Nullable Message message) {
			this.message = message;
		}

		public String getErrorMessage() {
			return message != null ? message.toString() : "";
		}
	}

	@SuppressWarnings("ConstantConditions")
	public ScriptCommand(
		String label, @Nullable String namespace, String description, String usage, List<String> aliases,
		ExecutableBy executableBy, @Nullable String permission, @Nullable VariableString permissionMessage,
		@Nullable CommandCooldown cooldown, Trigger trigger, List<Argument<?>> arguments, SkriptPattern pattern
	) {
		this.label = label.toLowerCase(Locale.ENGLISH);
		this.description = Utils.replaceEnglishChatStyles(description);
		this.usage = Utils.replaceEnglishChatStyles(usage);

		if (namespace != null) {
			for (char c : namespace.toCharArray()) {
				if (Character.isWhitespace(c)) {
					Skript.warning("command /" + label + " has a whitespace in its prefix. Defaulting to '" + ScriptCommand.DEFAULT_NAMESPACE + "'.");
					namespace = DEFAULT_NAMESPACE;
					break;
				}
				// char 167 is §
				if (c == 167) {
					Skript.warning("command /" + label + " has a section character in its prefix. Defaulting to '" + ScriptCommand.DEFAULT_NAMESPACE + "'.");
					namespace = DEFAULT_NAMESPACE;
					break;
				}
			}
		} else {
			namespace = DEFAULT_NAMESPACE;
		}
		this.namespace = namespace;

		aliases.removeIf(label::equalsIgnoreCase);
		this.aliases = aliases;

		this.executableBy = executableBy;
		this.permission = permission;
		this.permissionMessage = permissionMessage != null ? permissionMessage :
			VariableString.newInstance(Language.get("commands.no permission message"));

		this.cooldown = cooldown;

		this.trigger = trigger;
		this.arguments = arguments;
		this.pattern = pattern;
	}

	//
	// General
	//

	public String getLabel() {
		return label;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getDescription() {
		return description;
	}

	public String getUsage() {
		return usage;
	}

	public List<String> getAliases() {
		return aliases;
	}

	//
	// Permissions
	//

	public ExecutableBy getExecutableBy() {
		return executableBy;
	}

	@Nullable
	public String getPermission() {
		return permission;
	}

	public VariableString getPermissionMessage() {
		return permissionMessage;
	}

	@Nullable
	public CommandCooldown getCooldown() {
		return cooldown;
	}

	//
	// Parsing/Execution
	//

	public Trigger getTrigger() {
		return trigger;
	}

	public List<Argument<?>> getArguments() {
		return arguments;
	}

	public SkriptPattern getPattern() {
		return pattern;
	}

	public void execute(CommandSender sender, String label, String[] args) {
		// ensure command execution is done on main thread
		if (!Bukkit.isPrimaryThread()) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(Skript.getInstance(),
				() -> execute(sender, label, args)
			);
			return;
		}

		// executability checks
		if (
			(executableBy == ExecutableBy.PLAYERS && !(sender instanceof Player))
			|| (executableBy == ExecutableBy.CONSOLE && !(sender instanceof ConsoleCommandSender))
		) {
			Skript.error(executableBy.getErrorMessage());
			return;
		}

		ScriptCommandEvent event = new ScriptCommandEvent(sender, this, label, args);

		// permission checks
		if (permission != null && !sender.hasPermission(permission)) {
			sender.spigot().sendMessage(BungeeConverter.convert(
				permissionMessage.getMessageComponents(event)
			));
			return;
		}

		// cooldown checks
		if (cooldown != null && sender instanceof Player) {
			if (cooldown.isOnCooldown(((Player) sender), event)) {
				sender.spigot().sendMessage(BungeeConverter.convert(
					cooldown.getCooldownMessage().getMessageComponents(event)
				));
				return;
			}
		}

		// TODO: start cooldowns, handle async calls, update to current behavior, etc.

		// argument parsing
		ParseLogHandler log = SkriptLogger.startParseLogHandler();
		String joinedArgs = StringUtils.join(args, " ");
		try {

			MatchResult matchResult = pattern.match(joinedArgs, SkriptParser.PARSE_LITERALS, ParseContext.COMMAND);
			if (matchResult == null) {
				return;
			}

			ParseResult parseResult = matchResult.toParseResult();
			assert arguments.size() == parseResult.exprs.length;
			for (int i = 0; i < parseResult.exprs.length; i++) {
				if (parseResult.exprs[i] != null) {
					//noinspection unchecked - generics moment :)
					((Argument<Object>) arguments.get(i)).setValues(event, parseResult.exprs[i].getArray(event));
				}
			}

			log.clearError();
		} finally {
			log.stop();
		}

		// set variables for argument
		for (Argument<?> argument : arguments) {
			// if the argument is named, set variables too
			String name = argument.getName();
			if (name != null) {
				Object[] values = argument.getValues(event);
				if (argument.isSingle()) {
					if (values.length > 0) {
						Variables.setVariable(name, values[0], event, true);
					}
				} else {
					for (int i = 0; i < values.length; i++) {
						Variables.setVariable(name + Variable.SEPARATOR + (i + 1), values[i], event, true);
					}
				}
			}
		}

		boolean detailed = Skript.log(Verbosity.VERY_HIGH);
		if (detailed) {
			Skript.info("# /" + this.label + " " + joinedArgs);
		}

		long startTrigger = detailed ? System.nanoTime() : -1;
		if (!trigger.execute(event)) {
			sender.sendMessage(M_INTERNAL_ERROR.toString());
		}

		if (detailed) {
			Skript.info("# " + this.label + " took " + 1. * (System.nanoTime() - startTrigger) / 1000000. + " milliseconds");
		}
	}

}
