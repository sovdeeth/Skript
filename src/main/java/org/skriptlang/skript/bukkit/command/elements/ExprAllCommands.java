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

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.skriptlang.skript.bukkit.command.CommandModule;

@Name("All Commands")
@Description("An expression to obtain all registered commands or all registered script commands.")
@Examples({
	"send \"number of all commands: %size of all commands%\"",
	"send \"number of all script commands: %size of all script commands%\""
})
@Since("2.6")
public class ExprAllCommands extends SimpleExpression<String> {
	
	static {
		Skript.registerExpression(ExprAllCommands.class, String.class, ExpressionType.SIMPLE,
			"[all [[of] the]|the] [registered] [:script] commands"
		);
	}
	
	private boolean scriptCommands;
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		scriptCommands = parseResult.hasTag("script");
		return true;
	}
	
	@Nullable
	@Override
	protected String[] get(Event event) {
		if (scriptCommands) {
			return CommandModule.getCommandHandler().getScriptCommands().toArray(new String[0]);
		}
		return CommandModule.getCommandHandler().getServerCommands().toArray(new String[0]);
	}
	
	@Override
	public boolean isSingle() {
		return false;
	}
	
	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}
	
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "all of the registered" + (scriptCommands ? " script " : " ") + "commands";
	}
	
}
