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
package org.skriptlang.skript.commands.api;

import ch.njol.skript.localization.Message;
import ch.njol.skript.util.chat.MessageComponent;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;
import java.util.UUID;

public interface ScriptCommandSender {

	/**
	 * @return The underlying sender object
	 */
	Object getOriginal();

	void sendMessage(String message);

	void sendMessage(List<MessageComponent> components);

	/**
	 * @return The unique ID of the sender, if one exists. If the sender is of type {@link CommandSenderType#PLAYER},
	 * 			this is expected to be non-null.
	 */
	@Nullable
	UUID getUniqueID();

	/**
	 * Checks whether this sender has the given permission.
	 *
	 * @param permission The permission to check
	 * @return Whether the sender has the given permission
	 */
	boolean hasPermission(String permission);

	/**
	 * @return The type of this sender
	 * @see CommandSenderType
	 */
	CommandSenderType getType();

	/**
	 * The type of command sender.
	 */
	enum CommandSenderType {
		/**
		 * A player.
		 */
		PLAYER(new Message("commands.not executable by players")),
		/**
		 * The server, or console.
		 */
		SERVER(new Message("commands.not executable by server")),
		/**
		 * A block, like a command block.
		 */
		BLOCK(new Message("commands.not executable by blocks")),
		/**
		 * A non-player entity, like a command block minecart.
		 */
		ENTITY(new Message("commands.not executable by entities")),
		/**
		 * An unknown sender.
		 */
		UNKNOWN(new Message("commands.not executable by unknown"));


		@Nullable
		private final Message message;

		CommandSenderType(@Nullable Message message) {
			this.message = message;
		}

		public String getErrorMessage() {
			return message != null ? message.toString() : "";
		}

	}
}
