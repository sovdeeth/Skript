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
package ch.njol.skript.command;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.Effect;
import org.skriptlang.skript.lang.script.Script;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Message;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.SkriptColor;
import ch.njol.skript.variables.Variables;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.plugin.SimplePluginManager;
import org.eclipse.jdt.annotation.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

//TODO option to disable replacement of <color>s in command arguments?

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("deprecation")
public abstract class Commands {
	
	public final static ArgsMessage m_too_many_arguments = new ArgsMessage("commands.too many arguments");
	public final static Message m_internal_error = new Message("commands.internal error");
	public final static Message m_correct_usage = new Message("commands.correct usage");

	/**
	 * A Converter flag declaring that a Converter cannot be used for parsing command arguments.
	 */
	public static final int CONVERTER_NO_COMMAND_ARGUMENTS = 4;

	private final static Map<String, ScriptCommand> commands = new HashMap<>();
	
	@Nullable
	private static SimpleCommandMap commandMap = null;
	@Nullable
	private static Map<String, Command> cmKnownCommands;
	@Nullable
	private static Set<String> cmAliases;
	
	static {
		init(); // separate method for the annotation
	}
	public static Set<String> getScriptCommands(){
		return commands.keySet();
	}
	
	@Nullable
	public static SimpleCommandMap getCommandMap(){
		return commandMap;
	}
	
	@SuppressWarnings("unchecked")
	private static void init() {
		try {
			if (Bukkit.getPluginManager() instanceof SimplePluginManager) {
				final Field commandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
				commandMapField.setAccessible(true);
				commandMap = (SimpleCommandMap) commandMapField.get(Bukkit.getPluginManager());
				
				final Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
				knownCommandsField.setAccessible(true);
				cmKnownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
				
				try {
					final Field aliasesField = SimpleCommandMap.class.getDeclaredField("aliases");
					aliasesField.setAccessible(true);
					cmAliases = (Set<String>) aliasesField.get(commandMap);
				} catch (final NoSuchFieldException e) {}
			}
		} catch (final SecurityException e) {
			Skript.error("Please disable the security manager");
			commandMap = null;
		} catch (final Exception e) {
			Skript.outdatedError(e);
			commandMap = null;
		}
	}
	
	@Nullable
	public static List<Argument<?>> currentArguments = null;
	
	@SuppressWarnings("null")
	private final static Pattern escape = Pattern.compile("[" + Pattern.quote("(|)<>%\\") + "]");
	@SuppressWarnings("null")
	private final static Pattern unescape = Pattern.compile("\\\\[" + Pattern.quote("(|)<>%\\") + "]");
	
	public static String escape(String s) {
		return "" + escape.matcher(s).replaceAll("\\\\$0");
	}
	
	public static String unescape(String s) {
		return "" + unescape.matcher(s).replaceAll("$0");
	}
	
	private final static Listener commandListener = new Listener() {
		@SuppressWarnings("null")
		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onPlayerCommand(final PlayerCommandPreprocessEvent e) {
			if (handleCommand(e.getPlayer(), e.getMessage().substring(1)))
				e.setCancelled(true);
		}
		
		@SuppressWarnings("null")
		@EventHandler(priority = EventPriority.HIGHEST)
		public void onServerCommand(final ServerCommandEvent e) {
			if (e.getCommand() == null || e.getCommand().isEmpty() || e.isCancelled())
				return;
			if ((Skript.testing() || SkriptConfig.enableEffectCommands.value()) && e.getCommand().startsWith(SkriptConfig.effectCommandToken.value())) {
				if (handleEffectCommand(e.getSender(), e.getCommand())) {
					e.setCancelled(true);
				}
				return;
			}
		}
	};

	/**
	 * @param sender
	 * @param command full command string without the slash
	 * @return whether to cancel the event
	 */
	static boolean handleCommand(final CommandSender sender, final String command) {
		final String[] cmd = command.split("\\s+", 2);
		cmd[0] = cmd[0].toLowerCase(Locale.ENGLISH);
		if (cmd[0].endsWith("?")) {
			final ScriptCommand c = commands.get(cmd[0].substring(0, cmd[0].length() - 1));
			if (c != null) {
				c.sendHelp(sender);
				return true;
			}
		}
		final ScriptCommand c = commands.get(cmd[0]);
		if (c != null) {
//			if (cmd.length == 2 && cmd[1].equals("?")) {
//				c.sendHelp(sender);
//				return true;
//			}
			if (SkriptConfig.logPlayerCommands.value() && sender instanceof Player)
				SkriptLogger.LOGGER.info(sender.getName() + " [" + ((Player) sender).getUniqueId() + "]: /" + command);
			c.execute(sender, "" + cmd[0], cmd.length == 1 ? "" : "" + cmd[1]);
			return true;
		}
		return false;
	}
	
	static boolean handleEffectCommand(final CommandSender sender, String command) {
		if (!(sender instanceof ConsoleCommandSender || sender.hasPermission("skript.effectcommands") || SkriptConfig.allowOpsToUseEffectCommands.value() && sender.isOp()))
			return false;
		try {
			command = "" + command.substring(SkriptConfig.effectCommandToken.value().length()).trim();
			final RetainingLogHandler log = SkriptLogger.startRetainingLog();
			try {
				// Call the event on the Bukkit API for addon developers.
				EffectCommandEvent effectCommand = new EffectCommandEvent(sender, command);
				Bukkit.getPluginManager().callEvent(effectCommand);
				command = effectCommand.getCommand();
				ParserInstance parserInstance = ParserInstance.get();
				parserInstance.setCurrentEvent("effect command", EffectCommandEvent.class);
				Effect effect = Effect.parse(command, null);
				parserInstance.deleteCurrentEvent();
				
				if (effect != null) {
					log.clear(); // ignore warnings and stuff
					log.printLog();
					if (!effectCommand.isCancelled()) {
						sender.sendMessage(ChatColor.GRAY + "executing '" + SkriptColor.replaceColorChar(command) + "'");
						if (SkriptConfig.logPlayerCommands.value() && !(sender instanceof ConsoleCommandSender))
							Skript.info(sender.getName() + " issued effect command: " + SkriptColor.replaceColorChar(command));
						TriggerItem.walk(effect, effectCommand);
						Variables.removeLocals(effectCommand);
					} else {
						sender.sendMessage(ChatColor.RED + "your effect command '" + SkriptColor.replaceColorChar(command) + "' was cancelled.");
					}
				} else {
					if (sender == Bukkit.getConsoleSender()) // log as SEVERE instead of INFO like printErrors below
						SkriptLogger.LOGGER.severe("Error in: " + SkriptColor.replaceColorChar(command));
					else
						sender.sendMessage(ChatColor.RED + "Error in: " + ChatColor.GRAY + SkriptColor.replaceColorChar(command));
					log.printErrors(sender, "(No specific information is available)");
				}
			} finally {
				log.stop();
			}
			return true;
		} catch (final Exception e) {
			Skript.exception(e, "Unexpected error while executing effect command '" + SkriptColor.replaceColorChar(command) + "' by '" + sender.getName() + "'");
			sender.sendMessage(ChatColor.RED + "An internal error occurred while executing this effect. Please refer to the server log for details.");
			return true;
		}
	}

	@Nullable
	public static ScriptCommand getScriptCommand(String key) {
		return commands.get(key);
	}
	
	public static boolean skriptCommandExists(final String command) {
		final ScriptCommand c = commands.get(command);
		return c != null && c.getName().equals(command);
	}
	
	public static void registerCommand(final ScriptCommand command) {
		// Validate that there are no duplicates
		final ScriptCommand existingCommand = commands.get(command.getLabel());
		if (existingCommand != null && existingCommand.getLabel().equals(command.getLabel())) {
			Script script = existingCommand.getScript();
			Skript.error("A command with the name /" + existingCommand.getName() + " is already defined"
				+ (script != null ? (" in " + script.getConfig().getFileName()) : "")
			);
			return;
		}
		
		if (commandMap != null) {
			assert cmKnownCommands != null;// && cmAliases != null;
			command.register(commandMap, cmKnownCommands, cmAliases);
		}
		commands.put(command.getLabel(), command);
		for (final String alias : command.getActiveAliases()) {
			commands.put(alias.toLowerCase(Locale.ENGLISH), command);
		}
		command.registerHelp();
	}

	@Deprecated
	public static int unregisterCommands(File script) {
		int numCommands = 0;
		for (ScriptCommand c : new ArrayList<>(commands.values())) {
			if (c.getScript() != null && c.getScript().equals(ScriptLoader.getScript(script))) {
				numCommands++;
				unregisterCommand(c);
			}
		}
		return numCommands;
	}

	public static void unregisterCommand(ScriptCommand scriptCommand) {
		scriptCommand.unregisterHelp();
		if (commandMap != null) {
			assert cmKnownCommands != null;// && cmAliases != null;
			scriptCommand.unregister(commandMap, cmKnownCommands, cmAliases);
		}
		commands.values().removeIf(command -> command == scriptCommand);
	}
	
	private static boolean registeredListeners = false;
	
	public static void registerListeners() {
		if (!registeredListeners) {
			Bukkit.getPluginManager().registerEvents(commandListener, Skript.getInstance());

			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
				public void onPlayerChat(final AsyncPlayerChatEvent e) {
					if (!SkriptConfig.enableEffectCommands.value() || !e.getMessage().startsWith(SkriptConfig.effectCommandToken.value()))
						return;
					if (!e.isAsynchronous()) {
						if (handleEffectCommand(e.getPlayer(), e.getMessage()))
							e.setCancelled(true);
					} else {
						final Future<Boolean> f = Bukkit.getScheduler().callSyncMethod(Skript.getInstance(), new Callable<Boolean>() {
							@Override
							public Boolean call() throws Exception {
								return handleEffectCommand(e.getPlayer(), e.getMessage());
							}
						});
						try {
							while (true) {
								try {
									if (f.get())
										e.setCancelled(true);
									break;
								} catch (final InterruptedException e1) {
								}
							}
						} catch (final ExecutionException e1) {
							Skript.exception(e1);
						}
					}
				}
			}, Skript.getInstance());

			registeredListeners = true;
		}
	}
	
	/**
	 * copied from CraftBukkit (org.bukkit.craftbukkit.help.CommandAliasHelpTopic)
	 */
	public static final class CommandAliasHelpTopic extends HelpTopic {
		
		private final String aliasFor;
		private final HelpMap helpMap;
		
		public CommandAliasHelpTopic(final String alias, final String aliasFor, final HelpMap helpMap) {
			this.aliasFor = aliasFor.startsWith("/") ? aliasFor : "/" + aliasFor;
			this.helpMap = helpMap;
			name = alias.startsWith("/") ? alias : "/" + alias;
			Validate.isTrue(!name.equals(this.aliasFor), "Command " + name + " cannot be alias for itself");
			shortText = ChatColor.YELLOW + "Alias for " + ChatColor.WHITE + this.aliasFor;
		}
		
		@Override
		public String getFullText(final CommandSender forWho) {
			final StringBuilder sb = new StringBuilder(shortText);
			final HelpTopic aliasForTopic = helpMap.getHelpTopic(aliasFor);
			if (aliasForTopic != null) {
				sb.append("\n");
				sb.append(aliasForTopic.getFullText(forWho));
			}
			return "" + sb.toString();
		}
		
		@Override
		public boolean canSee(final CommandSender commandSender) {
			if (amendedPermission == null) {
				final HelpTopic aliasForTopic = helpMap.getHelpTopic(aliasFor);
				if (aliasForTopic != null) {
					return aliasForTopic.canSee(commandSender);
				} else {
					return false;
				}
			} else {
				assert amendedPermission != null;
				return commandSender.hasPermission(amendedPermission);
			}
		}
	}
	
}
