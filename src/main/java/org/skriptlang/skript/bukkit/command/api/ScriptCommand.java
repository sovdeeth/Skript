package org.skriptlang.skript.bukkit.command.api;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
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
import ch.njol.skript.util.Date;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.chat.BungeeConverter;
import ch.njol.skript.variables.Variables;
import ch.njol.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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

	private final Map<UUID, Date> cooldownStartDates = new HashMap<>();
	@Nullable
	private final Timespan cooldown;
	private final VariableString cooldownMessage;
	@Nullable
	private final String cooldownBypassPermission;
	@Nullable
	private final VariableString cooldownStorageVariableName;

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
		@Nullable Timespan cooldown, @Nullable VariableString cooldownMessage,
		@Nullable String cooldownBypassPermission, @Nullable VariableString cooldownStorageVariableName,
		Trigger trigger, List<Argument<?>> arguments, SkriptPattern pattern
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
		this.cooldownMessage = cooldownMessage != null ? cooldownMessage :
			VariableString.newInstance(Language.get("commands.cooldown message"));
		this.cooldownBypassPermission = cooldownBypassPermission;
		this.cooldownStorageVariableName = cooldownStorageVariableName;

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

	//
	// Cooldowns
	//

	@Nullable
	public Date getCooldownStart(UUID uuid, Event event) {
		if (cooldownStorageVariableName == null) {
			return cooldownStartDates.get(uuid);
		} else {
			String name = getStorageVariableName(event);
			assert name != null;
			return (Date) Variables.getVariable(name, null, false);
		}
	}

	public void setCooldownStart(UUID uuid, Event event, @Nullable Date date) {
		if (cooldownStorageVariableName != null) {
			// Using a variable
			String name = getStorageVariableName(event);
			assert name != null;
			Variables.setVariable(name, date, null, false);
		} else {
			// Use the map
			if (date == null) {
				cooldownStartDates.remove(uuid);
			} else {
				cooldownStartDates.put(uuid, date);
			}
		}
	}

	public long getRemainingMilliseconds(UUID uuid, Event event) {
		if (cooldown == null || getCooldownStart(uuid, event) == null) {
			return 0;
		}
		return Math.max(cooldown.getMilliSeconds() - getElapsedMilliseconds(uuid, event), 0);
	}

	public void setRemainingMilliseconds(UUID uuid, Event event, long milliseconds) {
		if (cooldown == null) {
			return;
		}
		setElapsedMilliSeconds(uuid, event, cooldown.getMilliSeconds() - milliseconds);
	}

	public long getElapsedMilliseconds(UUID uuid, Event event) {
		Date lastUsage = getCooldownStart(uuid, event);
		return lastUsage == null ? 0 : new Date().getTimestamp() - lastUsage.getTimestamp();
	}

	public void setElapsedMilliSeconds(UUID uuid, Event event, long milliseconds) {
		setCooldownStart(uuid, event, new Date(System.currentTimeMillis() - milliseconds));
	}

	@Nullable
	public Timespan getCooldown() {
		return cooldown;
	}

	public VariableString getCooldownMessage() {
		return cooldownMessage;
	}

	@Nullable
	public String getCooldownBypassPermission() {
		return cooldownBypassPermission;
	}

	@Nullable
	public VariableString getCooldownStorageVariableName() {
		return cooldownStorageVariableName;
	}

	@Nullable
	private String getStorageVariableName(Event event) {
		if (cooldownStorageVariableName == null) {
			return null;
		}
		String variableString = cooldownStorageVariableName.getSingle(event);
		if (variableString.startsWith("{")) {
			variableString = variableString.substring(1);
		}
		if (variableString.endsWith("}")) {
			variableString = variableString.substring(0, variableString.length() - 1);
		}
		return variableString;
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
			Player player = ((Player) sender);
			UUID uuid = player.getUniqueId();

			// cooldown bypass
			if (cooldownBypassPermission != null && player.hasPermission(cooldownBypassPermission)) {
				setCooldownStart(uuid, event, null);
			} else {
				if (getRemainingMilliseconds(uuid, event) == 0) {
					if (!SkriptConfig.keepLastUsageDates.value()) {
						setCooldownStart(uuid, event, null);
					}
				} else {
					sender.spigot().sendMessage(BungeeConverter.convert(
						cooldownMessage.getMessageComponents(event)
					));
					return;
				}
			}
		}

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
