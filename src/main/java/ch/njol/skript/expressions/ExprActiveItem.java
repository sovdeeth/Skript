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

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

public class ExprActiveItem extends PropertyExpression<LivingEntity, ItemStack> {

	static {
		register(ExprActiveItem.class, ItemStack.class, "(raised|active) (tool|item|weapon)", "livingentities");
	}

	@Override
	protected ItemStack[] get(Event event, LivingEntity[] source) {
		return get(source, LivingEntity::getActiveItem);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "active item of " + getExpr().toString(event, debug);
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<LivingEntity>) exprs[0]);
		return true;
	}

	@Override
	public Class<? extends ItemStack> getReturnType() {
		return ItemStack.class;
	}


}
