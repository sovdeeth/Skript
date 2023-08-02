package org.skriptlang.skript.bukkit.command.api;

import ch.njol.skript.effects.Delay;
import ch.njol.skript.util.Date;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ScriptCommandEvent extends Event {

	private final CommandSender sender;
	private final ScriptCommand scriptCommand;
	private final String label;
	private final String[] args;

	private final Date executionDate = new Date();
	private boolean cooldownCancelled;

	public ScriptCommandEvent(CommandSender sender, ScriptCommand scriptCommand, String label, String[] args) {
		this.sender = sender;
		this.scriptCommand = scriptCommand;
		this.label = label;
		this.args = args;
	}

	public CommandSender getSender() {
		return sender;
	}

	public ScriptCommand getScriptCommand() {
		return scriptCommand;
	}

	public String getLabel() {
		return label;
	}

	public String[] getArgs() {
		return args;
	}

	public boolean isCooldownCancelled() {
		return cooldownCancelled;
	}

	public void setCooldownCancelled(boolean cooldownCancelled) {
		if (Delay.isDelayed(this)) {
			if (sender instanceof Player)
				scriptCommand.setCooldownStart(
					((Player) sender).getUniqueId(),
					this,
					cooldownCancelled ? null : executionDate
				);
		} else {
			this.cooldownCancelled = cooldownCancelled;
		}
	}

	// Bukkit stuff
	private final static HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
