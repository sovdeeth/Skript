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
package org.skriptlang.skript.bukkit.command.elements;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.patterns.PatternCompiler;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.StringMode;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.bukkit.command.CommandModule;
import org.skriptlang.skript.bukkit.command.CommandUtils;
import org.skriptlang.skript.bukkit.command.api.Argument;
import org.skriptlang.skript.bukkit.command.api.CommandCooldown;
import org.skriptlang.skript.bukkit.command.api.ScriptCommand;
import org.skriptlang.skript.bukkit.command.api.ScriptCommand.ExecutableBy;
import org.skriptlang.skript.bukkit.command.api.ScriptCommandEvent;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;
import org.skriptlang.skript.lang.entry.util.LiteralEntryData;
import org.skriptlang.skript.lang.entry.util.VariableStringEntryData;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Name("Command")
@Description("Used for registering custom commands.")
@Examples({
	"command /broadcast <string>:",
	"\tusage: A command for broadcasting a message to all players.",
	"\tpermission: skript.command.broadcast",
	"\tpermission message: You don't have permission to broadcast messages",
	"\taliases: /bc",
	"\texecutable by: players and console",
	"\tcooldown: 15 seconds",
	"\tcooldown message: You last broadcast a message %elapsed time% ago. You can broadcast another message in %remaining time%.",
	"\tcooldown bypass: skript.command.broadcast.admin",
	"\tcooldown storage: {cooldown::%player%}",
	"\ttrigger:",
	"\t\tbroadcast the argument"
})
@Since("1.0")
public class StructCommand extends Structure {

	public static final Priority PRIORITY = new Priority(500);

	private static final Pattern
		ARGUMENT_PATTERN = Pattern.compile("<\\s*(?:([^>]+?)\\s*:\\s*)?(.+?)\\s*(?:=\\s*(" + SkriptParser.wildcard + "))?\\s*>"),
		DESCRIPTION_PATTERN = Pattern.compile("(?<!\\\\)%-?(.+?)%");


	private static final AtomicBoolean SYNC_COMMANDS = new AtomicBoolean();

	static {
		Skript.registerStructure(
			StructCommand.class,
			EntryValidator.builder()
				.addEntry("prefix", null, true)
				.addEntry("description", "", true)
				.addEntry("usage", null, true)
				.addEntry("prefix", null, true)
				.addEntry("permission", "", true)
				.addEntryData(new VariableStringEntryData("permission message", null, true))
				.addEntryData(new KeyValueEntryData<List<String>>("aliases", new ArrayList<>(), true) {
					private final Pattern pattern = Pattern.compile("\\s*,\\s*/?");

					@Override
					protected List<String> getValue(String value) {
						List<String> aliases = new ArrayList<>(Arrays.asList(pattern.split(value)));
						if (aliases.get(0).startsWith("/")) {
							aliases.set(0, aliases.get(0).substring(1));
						} else if (aliases.get(0).isEmpty()) {
							aliases = new ArrayList<>(0);
						}
						return aliases;
					}
				})
				.addEntryData(new KeyValueEntryData<ExecutableBy>("executable by", ExecutableBy.ALL, true) {
					private final Pattern pattern = Pattern.compile("\\s*,\\s*|\\s+(and|or)\\s+");

					@Override
					@Nullable
					protected ExecutableBy getValue(String value) {
						ExecutableBy executableBy = null;
						for (String b : pattern.split(value)) {
							if (b.equalsIgnoreCase("console") || b.equalsIgnoreCase("the console")) {
								executableBy = executableBy != null ? ExecutableBy.ALL : ExecutableBy.CONSOLE;
							} else if (b.equalsIgnoreCase("players") || b.equalsIgnoreCase("player")) {
								executableBy = executableBy != null ? ExecutableBy.ALL : ExecutableBy.PLAYERS;
							} else {
								return null;
							}
						}
						return executableBy;
					}
				})
				.addEntryData(new LiteralEntryData<>("cooldown", null, true, Timespan.class))
				.addEntryData(new VariableStringEntryData("cooldown message", null, true))
				.addEntry("cooldown bypass", null, true)
				.addEntryData(new VariableStringEntryData("cooldown storage", null, true, StringMode.VARIABLE_NAME))
				.addSection("trigger", false)
				.unexpectedEntryMessage(key ->
					"Unexpected entry '" + key + "'. Check that it's spelled correctly, and ensure that you have put all code into a trigger."
				)
				.build(),
			"command [/]<^(\\S+)\\s*(.+)?>"
		);
	}

	@Nullable
	private ScriptCommand command;

	@SuppressWarnings("NotNullFieldNotInitialized")
	private MatchResult matchResult;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, EntryContainer entryContainer) {
		matchResult = parseResult.regexes.get(0);
		return true;
	}

	@Override
	public boolean load() {
		getParser().setCurrentEvent("command", ScriptCommandEvent.class);

		String command = matchResult.group(0).toLowerCase(Locale.ENGLISH);

		// check whether this command already exists
		ScriptCommand existingCommand = CommandModule.getCommandHandler().getScriptCommand(command);
		if (existingCommand != null && existingCommand.getLabel().equals(command)) {
			Script script = existingCommand.getTrigger().getScript();
			Skript.error("A command with the name /" + existingCommand.getLabel() + " is already defined"
				+ (script != null ? (" in " + script.getConfig().getFileName()) : "")
			);
			getParser().deleteCurrentEvent();
			return false;
		}

		// parse the arguments

		String rawArguments = matchResult.group(1);

		Matcher matcher = ARGUMENT_PATTERN.matcher(rawArguments);
		List<Argument<?>> arguments = new ArrayList<>();
		// arguments are converted into a SkriptPattern
		StringBuilder pattern = new StringBuilder();

		int lastEnd = 0;
		int optionals = 0; // special bracket tracking to avoid counting any within arguments
		while (matcher.find()) {
			int start = matcher.start();

			// append all the stuff between the end of the last argument and the beginning of this one
			pattern.append(escape(rawArguments.substring(lastEnd, start)));

			// if this argument is required, we should expect this to be 0
			optionals += StringUtils.count(rawArguments, '[', lastEnd, start);
			optionals -= StringUtils.count(rawArguments, ']', lastEnd, start);

			lastEnd = matcher.end();

			String rawType = matcher.group(2);
			ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(rawType);
			NonNullPair<String, Boolean> pluralPair = Utils.getEnglishPlural(rawType);
			if (classInfo == null) { // Attempt parsing the singular version as a backup
				classInfo = Classes.getClassInfoFromUserInput(pluralPair.getFirst());

				if (classInfo == null) { // We tried!
					Skript.error("Unknown type '" + rawType + "'");
					getParser().deleteCurrentEvent();
					return false;
				}
			}
			Parser<?> parser = classInfo.getParser();
			if (parser == null || !parser.canParse(ParseContext.COMMAND)) {
				Skript.error("Can't use " + classInfo + " as argument of a command");
				getParser().deleteCurrentEvent();
				return false;
			}

			// parse argument

			Argument<?> argument = Argument.of(
				matcher.group(1), classInfo.getC(), optionals > 0, !pluralPair.getSecond(), matcher.group(3)
			);

			if (argument == null) { // Our argument parsing method should've printed an error
				getParser().deleteCurrentEvent();
				return false;
			}

			arguments.add(argument);

			// append type portion to argument
			pattern.append("%")
					.append(argument.isOptional() ? "-" : "")
					.append(Utils.toEnglishPlural(classInfo.getCodeName(), pluralPair.getSecond()))
					.append("%");
		}

		// append the rest of the user's input
		pattern.append(escape(rawArguments.substring(lastEnd)));

		// ensure brackets have been properly closed
		optionals += StringUtils.count(rawArguments, '[', lastEnd);
		optionals -= StringUtils.count(rawArguments, ']', lastEnd);
		if (optionals != 0) {
			Skript.error("Unclosed brackets within command declaration. Ensure all opening brackets have a respective closing bracket");
			getParser().deleteCurrentEvent();
			return false;
		}

		// obtain and validate the entries

		EntryContainer entryContainer = getEntryContainer();

		String desc = "/" + command + " ";
		desc += StringUtils.replaceAll(pattern, DESCRIPTION_PATTERN, m1 -> {
			assert m1 != null;
			NonNullPair<String, Boolean> p = Utils.getEnglishPlural("" + m1.group(1));
			String s1 = p.getFirst();
			return "<" + Classes.getClassInfo(s1).getName().toString(p.getSecond()) + ">";
		});
		desc = unescape(desc).trim();

		String usage = entryContainer.getOptional("usage", String.class, false);
		if (usage == null) {
			usage = ScriptCommand.M_CORRECT_USAGE + " " + desc;
		}

		String description = entryContainer.get("description", String.class, true);
		String prefix = entryContainer.getOptional("prefix", String.class, false);

		String permission = entryContainer.get("permission", String.class, true);
		VariableString permissionMessage = entryContainer.getOptional("permission message", VariableString.class, false);
		if (permissionMessage != null && permission.isEmpty())
			Skript.warning("command /" + command + " has a permission message set, but not a permission");

		List<String> aliases = (List<String>) entryContainer.get("aliases", true);
		ExecutableBy executableBy = (ExecutableBy) entryContainer.get("executable by", true);

		Timespan cooldown = entryContainer.getOptional("cooldown", Timespan.class, false);
		VariableString cooldownMessage = entryContainer.getOptional("cooldown message", VariableString.class, false);
		if (cooldownMessage != null && cooldown == null)
			Skript.warning("command /" + command + " has a cooldown message set, but not a cooldown");
		String cooldownBypass = entryContainer.getOptional("cooldown bypass", String.class, false);
		if (cooldownBypass == null) {
			cooldownBypass = "";
		} else if (cooldownBypass.isEmpty() && cooldown == null) {
			Skript.warning("command /" + command + " has a cooldown bypass set, but not a cooldown");
		}
		VariableString cooldownStorage = entryContainer.getOptional("cooldown storage", VariableString.class, false);
		if (cooldownStorage != null && cooldown == null)
			Skript.warning("command /" + command + " has a cooldown storage set, but not a cooldown");

		CommandCooldown commandCooldown = null;
		if (cooldown != null) {
			commandCooldown = new CommandCooldown(cooldown, cooldownMessage, cooldownBypass, cooldownStorage);
		}


		SectionNode node = entryContainer.getSource();

		if (Skript.debug() || node.debug())
			Skript.debug("command " + desc + ":");

		SectionNode triggerNode = entryContainer.get("trigger", SectionNode.class, false);
		Trigger trigger = new Trigger(getParser().getCurrentScript(), "command /" + command, new SimpleEvent(), ScriptLoader.loadItems(triggerNode));
		trigger.setLineNumber(triggerNode.getLine());

		// construct our command

		if (Skript.logVeryHigh() && !Skript.debug())
			Skript.info("Registered command " + desc);

		getParser().deleteCurrentEvent();

		this.command = new ScriptCommand(
			command, prefix, description, usage, aliases,
			executableBy, permission, permissionMessage,
			commandCooldown, trigger, arguments,
			PatternCompiler.compile(pattern.toString())
		);
		if (!CommandModule.getCommandHandler().registerCommand(this.command)) {
			// something went wrong, let's hope the register method printed an error
			return false;
		}
		SYNC_COMMANDS.set(true);

		return true;
	}

	@Override
	public boolean postLoad() {
		attemptCommandSync();
		return true;
	}

	@Override
	public void unload() {
		if (command != null) {
			if (!CommandModule.getCommandHandler().unregisterCommand(command)) {
				throw new SkriptAPIException(
					"An error occurred while attempting to unregister the command '" + command.getLabel() + "'"
				);
			}
			SYNC_COMMANDS.set(true);
		}
	}

	@Override
	public void postUnload() {
		attemptCommandSync();
	}

	private static void attemptCommandSync() {
		if (SYNC_COMMANDS.compareAndSet(true, false)) {
			if (CommandUtils.syncCommands(Bukkit.getServer())) {
				Skript.debug("Commands synced to clients");
			} else {
				Skript.debug("Commands changed but not synced to clients (normal on 1.12 and older)");
			}
		}
	}

	@Override
	public Priority getPriority() {
		return PRIORITY;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "command";
	}

	public List<Argument<?>> getArguments() {
		// TODO complete method
		return new ArrayList<>();
	}

	private static final Pattern ESCAPE = Pattern.compile("[" + Pattern.quote("(|)<>%\\") + "]");
	private static final  Pattern UNESCAPE = Pattern.compile("\\\\[" + Pattern.quote("(|)<>%\\") + "]");

	public static String escape(String s) {
		return ESCAPE.matcher(s).replaceAll("\\\\$0");
	}

	public static String unescape(String s) {
		return UNESCAPE.matcher(s).replaceAll("$0");
	}

}
