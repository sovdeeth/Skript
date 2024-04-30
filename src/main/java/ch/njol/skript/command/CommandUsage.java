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

import ch.njol.skript.lang.VariableString;
import ch.njol.skript.util.Utils;
import com.google.errorprone.annotations.Var;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds info about the usage of a command.
 * TODO: replace with record when java 17
 */
public class CommandUsage {

	/**
	 * A dynamic usage message that can contain expressions.
	 */
	private final VariableString usage;

	/**
	 * A fallback usage message that can be used in non-event environments,
	 * like when registering the Bukkit command.
	 */
	private final String defaultUsage;

	/**
	 * @param usage The dynamic usage message, can contain expressions.
	 * @param defaultUsage A fallback usage message for use in non-event environments.
	 */
	public CommandUsage(VariableString usage, String defaultUsage) {
		this.usage = usage;
		this.defaultUsage = Utils.replaceChatStyles(defaultUsage);
	}

	/**
	 * @return The usage message as a {@link VariableString}
	 */
	public VariableString getRawUsage() {
		return usage;
	}

	/**
	 * @param event The event used to evaluate the usage message.
	 * @return The evaluated usage message.
	 */
	public String getUsage(Event event) {
		return usage.getSingle(event);
	}

	/**
	 * @return The fallback usage message, only for use in environments without access to an event.
	 */
	public String getDefaultUsage() {
		return defaultUsage;
	}
}
