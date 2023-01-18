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

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import ch.njol.skript.util.slot.EquipmentSlot.EquipSlot;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Collections;

public class ExprActiveItem extends PropertyExpression<LivingEntity, ItemStack> {

	static {
		register(ExprActiveItem.class, ItemStack.class, "(raised|active) (tool|item|weapon)", "livingentities");
	}

	@Override
	public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
		return CollectionUtils.array(ItemStack.class);
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
		LivingEntity entity = getExpr().getSingle(event);
		if (entity == null) return;

		if (!entity.isHandRaised()) return;

		EquipmentSlot bukkitSlot = entity.getHandRaised();
		EntityEquipment equipment = entity.getEquipment();
		ch.njol.skript.util.slot.EquipmentSlot skriptSlot = new ch.njol.skript.util.slot.EquipmentSlot(equipment, (bukkitSlot == EquipmentSlot.HAND ? EquipSlot.TOOL : EquipSlot.OFF_HAND));

		switch (mode) {
			case SET:
				skriptSlot.setItem((ItemStack) delta[0]);
		}
	}

	@Override
	protected ItemStack[] get(Event event, LivingEntity[] source) {
		return get(source, LivingEntity::getActiveItem);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "raised tool of " + getExpr().toString(event, debug);
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
