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
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Active Item")
@Description("Returns the item the entity is currently using (ie: the food they're eating, " +
		"the bow they're drawing back, etc.). This cannot be changed. " +
		"If an entity is not using any item, this will return 0 air.")
@Examples({"on damage of player:",
		"\tif victim's active tool is a bow:",
		"\t\tinterrupt player's active item use"})
@Since("INSERT VERSION")
@RequiredPlugins("Paper")
public class ExprActiveItem extends PropertyExpression<LivingEntity, ItemType> {

	static {
		if (Skript.methodExists(LivingEntity.class, "getActiveItem"))
			register(ExprActiveItem.class, ItemType.class, "(raised|active) (tool|item|weapon)", "livingentities");
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<LivingEntity>) exprs[0]);
		return true;
	}

	@Override
	protected ItemType[] get(Event event, LivingEntity[] source) {
		return get(source, (livingEntity -> {
			return new ItemType(livingEntity.getActiveItem());
		}));
	}

	@Override
	public Class<? extends ItemType> getReturnType() {
		return ItemType.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "active item of " + getExpr().toString(event, debug);
	}

}
