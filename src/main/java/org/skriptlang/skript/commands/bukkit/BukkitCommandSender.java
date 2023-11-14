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
package org.skriptlang.skript.commands.bukkit;

import ch.njol.skript.util.chat.BungeeConverter;
import ch.njol.skript.util.chat.MessageComponent;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.commands.api.ScriptCommandSender;

import java.util.List;
import java.util.UUID;

public class BukkitCommandSender implements ScriptCommandSender {

	private final CommandSender sender;
	private final CommandSenderType type;

	public BukkitCommandSender(CommandSender sender) {
		this.sender = sender;

		// determine the type of the sender
		if (sender instanceof Player) {
			type = CommandSenderType.PLAYER;
		} else if (sender instanceof ConsoleCommandSender) {
			type = CommandSenderType.SERVER;
		} else if (sender instanceof BlockCommandSender) {
			type = CommandSenderType.BLOCK;
		} else if (sender instanceof Entity) {
			type = CommandSenderType.ENTITY;
		} else {
			type = CommandSenderType.UNKNOWN;
		}
	}

	@Override
	public void sendMessage(String message) {
		sender.sendMessage(message);
	}

	@Override
	public void sendMessage(List<MessageComponent> components) {
		sender.spigot().sendMessage(BungeeConverter.convert(components));
	}

	@Override
	public CommandSender getOriginal() {
		return sender;
	}

	@Override
	@Nullable
	public UUID getUniqueID() {
		if (type == CommandSenderType.PLAYER)
			return ((Player) sender).getUniqueId();
		return null;
	}

	@Override
	public boolean hasPermission(String permission) {
		return sender.hasPermission(permission);
	}

	@Override
	public CommandSenderType getType() {
		return type;
	}
}
