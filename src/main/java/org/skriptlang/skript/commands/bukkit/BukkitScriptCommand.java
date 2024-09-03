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

import ch.njol.skript.Skript;
import ch.njol.skript.command.CommandUsage;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.patterns.SkriptPattern;
import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.commands.api.Argument;
import org.skriptlang.skript.commands.api.CommandCooldown;
import org.skriptlang.skript.commands.api.ScriptCommand;
import org.skriptlang.skript.commands.api.ScriptCommandSender;
import org.skriptlang.skript.commands.api.ScriptCommandSender.CommandSenderType;

import java.util.List;

/**
 * A Bukkit implementation of {@link ScriptCommand}.
 * Since Skript is currently Bukkit-only, this implementation doesn't do anything special.
 * This structure is mainly to support further decoupling of Skript from Bukkit.
 */
public class BukkitScriptCommand extends ScriptCommand {

	public BukkitScriptCommand(
		String label, @Nullable String namespace, String description, CommandUsage usage, List<String> aliases,
		List<CommandSenderType> executableBy, @Nullable String permission, @Nullable VariableString permissionMessage,
		@Nullable CommandCooldown cooldown, Trigger trigger, List<Argument<?>> arguments, SkriptPattern pattern
	) {
		super(
			label, namespace, description, usage, aliases,
			executableBy, permission, permissionMessage,
			cooldown, trigger, arguments, pattern
		);
	}

	@Override
	public void execute(ScriptCommandSender sender, String label, String[] args) {
		if (!Bukkit.isPrimaryThread()) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(Skript.getInstance(),
				() -> execute_i(sender, label, args)
			);
			return;
		}
		execute_i(sender, label, args);
	}

}
