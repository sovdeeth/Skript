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
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Method;

public final class CommandUtils {

	private CommandUtils() { }

	@Nullable
	private static Method SYNC_COMMANDS_METHOD;

	static {
		try {
			Class<?> craftServer;
			String revision = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
			craftServer = Class.forName("org.bukkit.craftbukkit." + revision + ".CraftServer");

			SYNC_COMMANDS_METHOD = craftServer.getDeclaredMethod("syncCommands");
			SYNC_COMMANDS_METHOD.setAccessible(true);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			// Ignore except for debugging. This is not necessary or in any way supported functionality
			if (Skript.debug())
				e.printStackTrace();
		}
	}

	/**
	 * Attempts to force the server to sync commands.
	 * @param server The Server whose commands should be synced.
	 * @return Whether it is likely that commands were successfully synced.
	 */
	public static boolean syncCommands(Server server) {
		if (SYNC_COMMANDS_METHOD == null)
			return false; // Method not available, can't sync
		try {
			SYNC_COMMANDS_METHOD.invoke(server);
			return true; // Sync probably succeeded
		} catch (Throwable e) {
			if (Skript.debug()) {
				Skript.info("syncCommands failed; stack trace for debugging below");
				e.printStackTrace();
			}
			return false; // Something went wrong, sync probably failed
		}
	}

}
