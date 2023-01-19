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
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.eclipse.jdt.annotation.Nullable;

@Name("Is Hand Raised")
@Description("Checks whether an entity has one or both of their hands raised. " +
		"Hands are raised when an entity is using an item (eg: blocking, drawing a bow, eating).")
@Examples({"on damage of player:",
	"\tif victim's main hand is raised:",
	"\t\tdrop player's tool at player",
	"\t\tset player's tool to air"})
@Since("INSERTVERSION")
@RequiredPlugins("Paper 1.12.2 or newer")
public class CondIsHandRaised extends Condition {

static {
		Skript.registerCondition(CondIsHandRaised.class,
		"%livingentities%'s [:main] hand[s] (is|are) raised",
			"%livingentities%'s [:main] hand[s] (isn't|is not|aren't|are not) raised",
			"%livingentities%'s off[( |-)]hand[s] (is|are) raised",
			"%livingentities%'s off[( |-)]hand[s] (isn't|is not|aren't|are not) raised");
	}

	private Expression<LivingEntity> entities;

	// 0 for either hand, 1 for main hand, 2 for off-hand.
	private int hand;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		entities = (Expression<LivingEntity>) exprs[0];
		setNegated(matchedPattern == 1 || matchedPattern == 3);
		if (matchedPattern >= 2) {
			hand = 2;
		} else if (parseResult.hasTag("main")) {
			hand = 1;
		} else {
			hand = 0;
		}
		return true;
	}

	@Override
	public boolean check(Event event) {
		return entities.check(event, (livingEntity) -> {
			if (hand == 0) {
				return livingEntity.isHandRaised();
			} else if (hand == 1) {
				return livingEntity.isHandRaised() && livingEntity.getHandRaised().equals(EquipmentSlot.HAND);
			} else {
				return livingEntity.isHandRaised() && livingEntity.getHandRaised().equals(EquipmentSlot.OFF_HAND);
			}
		}, isNegated());
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return entities.toString(event, debug) + "'s  " + (hand == 0 ? "" : (hand == 1 ? "main " : "off ")) + "hand " +
			(entities.isSingle() ? "is" : "s are") + " raised ";
	}

}