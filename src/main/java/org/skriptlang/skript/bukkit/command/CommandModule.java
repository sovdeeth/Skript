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
package org.skriptlang.skript.bukkit.command;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.SkriptColor;
import ch.njol.skript.variables.Variables;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.skriptlang.skript.bukkit.command.api.CommandHandler;
import org.skriptlang.skript.bukkit.command.api.EffectCommandEvent;
import org.skriptlang.skript.bukkit.command.base.BukkitCommandHandler;
import org.skriptlang.skript.bukkit.command.elements.ExprAllCommands;
import org.skriptlang.skript.bukkit.command.elements.ExprArgument;
import org.skriptlang.skript.bukkit.command.elements.ExprCommand;
import org.skriptlang.skript.bukkit.command.elements.ExprCommandInfo;
import org.skriptlang.skript.bukkit.command.elements.StructCommand;

public class CommandModule {

	private static final String EFFECT_COMMANDS_PERMISSION = "skript.effectcommands";

	@SuppressWarnings("NotNullFieldNotInitialized")
	private static CommandHandler commandHandler;

	public void load(SkriptAddon addon) {
		commandHandler = new BukkitCommandHandler();

		// Setup EffectCommand listener
		Bukkit.getPluginManager().registerEvents(new Listener() {
			@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
			public void onServerCommand(ServerCommandEvent event) {
				if (
					!event.getCommand().isEmpty()
						&& SkriptConfig.enableEffectCommands.value()
						&& event.getCommand().startsWith(SkriptConfig.effectCommandToken.value())
				) {
					if (handleEffectCommand(event.getCommand(), event.getSender()))
						event.setCancelled(true);
				}
			}
		}, Skript.getInstance());

		// Load Syntax
		new ExprAllCommands();
		new ExprArgument();
		new ExprCommand();
		new ExprCommandInfo();
		new StructCommand();
	}

	public static void setCommandHandler(CommandHandler commandHandler) {
		Skript.isAcceptRegistrations();
		CommandModule.commandHandler = commandHandler;
	}

	public static CommandHandler getCommandHandler() {
		return commandHandler;
	}

	private static boolean handleEffectCommand(String command, CommandSender sender) {
		if (!(
			sender instanceof ConsoleCommandSender
			|| sender.hasPermission(EFFECT_COMMANDS_PERMISSION)
			|| (SkriptConfig.allowOpsToUseEffectCommands.value() && sender.isOp())
		)) {
			return false;
		}

		try (RetainingLogHandler log = new RetainingLogHandler()) {
			command = command.substring(SkriptConfig.effectCommandToken.value().length()).trim();
			// Call the event on the Bukkit API for addon developers
			// TODO call old event for legacy reasons
			EffectCommandEvent effectCommand = new EffectCommandEvent(sender, command);
			Bukkit.getPluginManager().callEvent(effectCommand);
			command = effectCommand.getCommand();

			ParserInstance parser = ParserInstance.get();
			parser.setCurrentEvent("effect command", EffectCommandEvent.class);
			Effect effect = Effect.parse(command, null);
			parser.deleteCurrentEvent();

			if (effect != null) {
				log.clear(); // ignore warnings and stuff
				log.printLog();
				if (!effectCommand.isCancelled()) {
					sender.sendMessage(ChatColor.GRAY + "executing '" + SkriptColor.replaceColorChar(command) + "'");
					if (SkriptConfig.logPlayerCommands.value() && !(sender instanceof ConsoleCommandSender)) {
						Skript.info(sender.getName() + " issued effect command: " + SkriptColor.replaceColorChar(command));
					}
					TriggerItem.walk(effect, effectCommand);
					Variables.removeLocals(effectCommand);
				} else {
					sender.sendMessage(ChatColor.RED + "your effect command '" + SkriptColor.replaceColorChar(command) + "' was cancelled.");
				}
			} else {
				if (sender == Bukkit.getConsoleSender()) { // log as SEVERE instead of INFO like printErrors below
					SkriptLogger.LOGGER.severe("Error in: " + SkriptColor.replaceColorChar(command));
				} else {
					sender.sendMessage(ChatColor.RED + "Error in: " + ChatColor.GRAY + SkriptColor.replaceColorChar(command));
				}
				log.printErrors(sender, "(No specific information is available)");
			}
			return true;
		} catch (Exception e) {
			Skript.exception(e, "Unexpected error while executing effect command '" + SkriptColor.replaceColorChar(command) + "' by '" + sender.getName() + "'");
			sender.sendMessage(ChatColor.RED + "An internal error occurred while executing this effect. Please refer to the server log for details.");
			return true;
		}
	}

}
