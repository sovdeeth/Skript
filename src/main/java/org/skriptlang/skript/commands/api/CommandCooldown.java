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

import ch.njol.skript.lang.VariableString;
import ch.njol.skript.localization.Language;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.variables.Variables;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a cooldown for a Skript command.
 * Provides functionality to check if a user is on cooldown, to modify the cooldown duration per user, to get the remaining time, etc.
 */
public class CommandCooldown {

	private final Map<UUID, Date> cooldownEndDates = new HashMap<>();

	private final Timespan cooldown;
	private final VariableString cooldownMessage;

	private final String cooldownBypassPermission;
	@Nullable
	private final VariableString cooldownStorageVariableName;

	@SuppressWarnings("ConstantConditions")
	public CommandCooldown(Timespan cooldown, @Nullable VariableString cooldownMessage, String cooldownBypassPermission, @Nullable VariableString cooldownStorageVariableName) {
		this.cooldown = cooldown;
		this.cooldownMessage = cooldownMessage != null ? cooldownMessage :
			VariableString.newInstance(Language.get("commands.cooldown message"));
		this.cooldownBypassPermission = cooldownBypassPermission;
		this.cooldownStorageVariableName = cooldownStorageVariableName;
	}

	/**
	 * Checks if the given {@link ScriptCommandSender} is on cooldown.
	 *
	 * @param sender The {@link ScriptCommandSender} to check.
	 * @param event The event used to evaluate the cooldown storage variable name, if one exists.
	 * @return {@code true} if the {@link ScriptCommandSender} is on cooldown, {@code false} otherwise.
	 */
	public boolean isOnCooldown(ScriptCommandSender sender, Event event) {
		return !hasBypassPermission(sender) && getEndDate(sender, event) != null;
	}

	/**
	 * Puts the given {@link ScriptCommandSender} on cooldown using the command's default cooldown.
	 * If the {@link ScriptCommandSender} has the permission to bypass the cooldown, this method will do nothing.
	 * This should be preferred over other methods when putting a {@link ScriptCommandSender} on cooldown.
	 *
	 * @param sender The {@link ScriptCommandSender} to put on cooldown.
	 * @param event The event used to evaluate the cooldown storage variable name, if one exists.
	 * @see #setRemainingDuration(ScriptCommandSender, Event, Timespan)
	 * @see #setEndDate(ScriptCommandSender, Event, Date)
	 */
	public void applyCooldown(ScriptCommandSender sender, Event event) {
		if (hasBypassPermission(sender))
			return;
		setRemainingDuration(sender, event, cooldown);
	}

	/**
	 * Retroactively puts the given {@link ScriptCommandSender} on cooldown using the command's default cooldown.
	 * This acts like the {@link ScriptCommandSender} was put on cooldown at the given start date.
	 * If the {@link ScriptCommandSender} has the permission to bypass the cooldown, this method will do nothing.
	 * This should be preferred over other methods when putting a {@link ScriptCommandSender} on cooldown.
	 *
	 * @param sender The {@link ScriptCommandSender} to put on cooldown.
	 * @param event The event used to evaluate the cooldown storage variable name, if one exists.
	 * @param startDate The retroactive start date of the cooldown. Must be in the past.
	 * @see #setRemainingDuration(ScriptCommandSender, Event, Timespan)
	 * @see #setEndDate(ScriptCommandSender, Event, Date)
	 */
	public void applyCooldown(ScriptCommandSender sender, Event event, Date startDate) {
		if (startDate.isAfter(new Date()))
			throw new IllegalArgumentException("Start date must be in the past.");
		if (hasBypassPermission(sender))
			return;
		Timespan elapsed = new Date().difference(startDate);
		Timespan remaining = new Timespan(cooldown.getMilliSeconds() - elapsed.getMilliSeconds());
		setRemainingDuration(sender, event, remaining);
	}

	/**
	 * Removes the cooldown for the given {@link ScriptCommandSender}.
	 * This should be preferred over other methods when removing a {@link ScriptCommandSender}'s cooldown.
	 *
	 * @param sender The {@link ScriptCommandSender} to remove the cooldown for.
	 * @param event The event used to evaluate the cooldown storage variable name, if one exists.
	 */
	public void cancelCooldown(ScriptCommandSender sender, Event event) {
		setEndDate(sender, event, null);
	}

	/**
	 * Helper method to check if the given {@link ScriptCommandSender} has the permission to bypass the cooldown.
	 *
	 * @param sender The {@link ScriptCommandSender} to check.
	 * @return {@code true} if the {@link ScriptCommandSender} has the permission to bypass the cooldown, {@code false} otherwise.
	 */
	public boolean hasBypassPermission(ScriptCommandSender sender) {
		return !cooldownBypassPermission.isEmpty() && sender.hasPermission(cooldownBypassPermission);
	}

	/**
	 * Gets the remaining cooldown duration for the given {@link ScriptCommandSender}.
	 *
	 * @param sender The {@link ScriptCommandSender} to check.
	 * @param event The event used to evaluate the cooldown storage variable name, if one exists.
	 * @return The remaining cooldown duration. If the {@link ScriptCommandSender} is not
	 * 			on cooldown, a {@link Timespan} of {@code 0} milliseconds will be returned.
	 */
	public Timespan getRemainingDuration(ScriptCommandSender sender, Event event) {
		Date endDate = getEndDate(sender, event);
		if (endDate == null) {
			return new Timespan(0);
		}
		return new Date().difference(endDate);
	}

	/**
	 * Sets the remaining cooldown duration for the given {@link ScriptCommandSender}. Does not respect the cooldown bypass permission.
	 * If the user is not on cooldown, setting the remaining cooldown duration to a value greater than {@code 0} milliseconds will put the user on cooldown.
	 * Note that multiple {@link ScriptCommandSender}s may share the same cooldown variable, and thus this method may affect multiple {@link ScriptCommandSender}s.
	 *
	 * @param sender The {@link ScriptCommandSender} to set the cooldown for.
	 * @param event The event used to evaluate the cooldown storage variable name, if one exists.
	 * @param newDuration The remaining cooldown duration. If equal to {@code 0} milliseconds,
	 *                    the {@link ScriptCommandSender} will be taken off cooldown.
	 * @see #applyCooldown(ScriptCommandSender, Event)
	 * @see #setEndDate(ScriptCommandSender, Event, Date)
	 */
	public void setRemainingDuration(ScriptCommandSender sender, Event event, Timespan newDuration) {
		Date endDate = null;
		if (newDuration.getMilliSeconds() > 0) {
			endDate = new Date();
			endDate.add(newDuration);
		}
		setEndDate(sender, event, endDate);
	}

	/**
	 * Gets the end date of the cooldown for the given {@link ScriptCommandSender}.
	 *
	 * @param sender The {@link ScriptCommandSender} to get the cooldown of.
	 * @param event The event used to evaluate the cooldown storage variable name, if one exists.
	 * @return The end date of the cooldown. Will be {@code null} if the {@link ScriptCommandSender} is not on cooldown.
	 */
	@Nullable
	public Date getEndDate(ScriptCommandSender sender, Event event) {
		// prefer the cooldown storage variable if it exists
		if (cooldownStorageVariableName != null)
			return getEndDateVariable(event);
		// otherwise, use the cooldown end date map
		UUID senderUUID = sender.getUniqueID();
		if (senderUUID == null)
			return null;

		Date endDate = cooldownEndDates.get(senderUUID);
		if (endDate == null)
			return null;
		// If the cooldown has expired, remove the player from the cooldown map.
		if (endDate.isBefore(new Date())) {
			cooldownEndDates.remove(senderUUID);
			return null;
		}
		return endDate;
	}

	/**
	 * Gets the end date of the cooldown for the given {@link ScriptCommandSender} from the cooldown storage variable.
	 * Requires the cooldown storage variable name to be non-null.
	 *
	 * @param event The event used to evaluate the cooldown storage variable.
	 * @return The end date of the cooldown. Will be {@code null} if the {@link ScriptCommandSender} is not on cooldown.
	 */
	@Nullable
	private Date getEndDateVariable(Event event) {
		String variableName = getStorageVariableName(event);
		Object variable = Variables.getVariable(variableName, null, false);
		Date endDate = null;
		if (variable instanceof Date) {
			endDate = (Date) variable;
			if (endDate.isBefore(new Date())) {
				Variables.deleteVariable(variableName,null, false);
				return null;
			}
		}
		return endDate;
	}

	/**
	 * Sets the end date of the cooldown for the given {@link ScriptCommandSender}. Does not respect the cooldown bypass permission.
	 * Note that multiple {@link ScriptCommandSender}s may share the same cooldown variable, and thus this method may affect multiple {@link ScriptCommandSender}s.
	 *
	 * @param event The event used to evaluate the cooldown storage variable name, if one exists.
	 * @param sender The {@link ScriptCommandSender} to set the cooldown for.
	 * @param newEndDate The end date of the cooldown. If {@code null} or in the past, the {@link ScriptCommandSender} will be taken off cooldown.
	 * @see #applyCooldown(ScriptCommandSender, Event)
	 * @see #setRemainingDuration(ScriptCommandSender, Event, Timespan)
	 */
	public void setEndDate(ScriptCommandSender sender, Event event, @Nullable Date newEndDate) {
		// prefer the cooldown storage variable if it exists
		if (cooldownStorageVariableName != null) {
			setEndDateVariable(event, newEndDate);
			return;
		}
		// Otherwise, use the cooldown end date map
		UUID senderUUID = sender.getUniqueID();
		if (senderUUID == null)
			return;

		if (newEndDate == null) {
			cooldownEndDates.remove(senderUUID);
			return;
		}
		cooldownEndDates.put(senderUUID, newEndDate);
	}

	/**
	 * Sets the end date of the cooldown in the cooldown storage variable.
	 * Requires the cooldown storage variable name to be non-null.
	 *
	 * @param event The event used to evaluate the cooldown storage variable.
	 * @param newEndDate The end date of the cooldown. If {@code null}, the {@link ScriptCommandSender} will be taken off cooldown.
	 */
	private void setEndDateVariable(Event event, @Nullable Date newEndDate) {
		String variableName = getStorageVariableName(event);
		if (newEndDate == null) {
			Variables.deleteVariable(variableName, null, false);
			return;
		}
		Variables.setVariable(variableName, newEndDate, null, false);
	}

	/**
	 * Gets the elapsed cooldown duration for the given {@link ScriptCommandSender} in milliseconds.
	 *
	 * @param sender The {@link ScriptCommandSender} to check.
	 * @param event The event used to evaluate the cooldown storage variable name, if one exists.
	 * @return The elapsed cooldown duration, or {@code null} if the {@link ScriptCommandSender} is not on cooldown.
	 */
	@Nullable
	public Timespan getElapsedDuration(ScriptCommandSender sender, Event event) {
		return null;
	}

	/**
	 * Sets the elapsed cooldown duration for the given {@link ScriptCommandSender} in milliseconds.
	 * Does not respect the cooldown bypass permission.
	 *
	 * @param sender The {@link ScriptCommandSender} to set the cooldown for.
	 * @param event The event used to evaluate the cooldown storage variable name, if one exists.
	 * @param newElapsedDuration The elapsed cooldown duration in milliseconds. If less than {@code 0} or greater than the {@link ScriptCommandSender}'s
	 *                    current cooldown, the {@link ScriptCommandSender} will be taken off cooldown.
	 */
	public void setElapsedDuration(ScriptCommandSender sender, Event event, Timespan newElapsedDuration) {
	}

	/**
	 * Gets the default cooldown duration. This may not be the same as the cooldown duration for a specific {@link ScriptCommandSender}, as the
	 * cooldown for an individual {@link ScriptCommandSender} may be modified.
	 *
	 * @return The default cooldown duration.
	 */
	public Timespan getDefaultCooldown() {
		return cooldown;
	}

	/**
	 * Gets the cooldown message, as an unevaluated {@link VariableString}.
	 *
	 * @return The cooldown message, unevaluated.
	 */
	public VariableString getCooldownMessage() {
		return cooldownMessage;
	}

	/**
	 * @return The permission required to bypass the cooldown.
	 */
	@Nullable
	public String getCooldownBypassPermission() {
		return cooldownBypassPermission;
	}

	/**
	 * @return The name of the variable used to store the remaining cooldown duration.
	 */
	@Nullable
	public VariableString getCooldownStorageVariableName() {
		return cooldownStorageVariableName;
	}

	/**
	 * Gets the name of the variable used to store the remaining cooldown duration.
	 * Requires the cooldown storage variable name to be non-null.
	 *
	 * @param event The event used to evaluate the cooldown storage variable name.
	 * @return The evaluated cooldown storage variable name.
	 */
	private String getStorageVariableName(Event event) {
		assert cooldownStorageVariableName != null;
		String variableString = cooldownStorageVariableName.getSingle(event);
		if (variableString.startsWith("{"))
			variableString = variableString.substring(1);
		if (variableString.endsWith("}"))
			variableString = variableString.substring(0, variableString.length() - 1);
		return variableString;
	}

}
