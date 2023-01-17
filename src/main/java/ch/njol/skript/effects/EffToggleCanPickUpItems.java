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
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class EffToggleCanPickUpItems extends Effect {

	static {
		Skript.registerEffect(EffToggleCanPickUpItems.class,
				"allow %livingentities% to pick (up items|items up)",
				"[negated:(don't|do not)] let %livingentities% pick (up items|items up)",
				"(forbid|disallow) %livingentities% (from|to) pick[ing] (up items|items up)");
	}

	private Expression<LivingEntity> entityExpr;
	private boolean allowPickUp;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		entityExpr = (Expression<LivingEntity>) exprs[0];
		allowPickUp = !(matchedPattern == 2 || parseResult.hasTag("negated"));
		return true;
	}


	@Override
	protected void execute(Event event) {
		for (LivingEntity entity : entityExpr.getArray(event))
			entity.setCanPickupItems(allowPickUp);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (allowPickUp)
			return "allow " + entityExpr.toString(event, debug) + " to pick up items";
		else
			return "forbid " + entityExpr.toString(event, debug) + " from picking up items";
	}
}
