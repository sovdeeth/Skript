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
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

@Name("Max Item Use Time")
@Description("Returns the max duration an item can be used for, before the action completes. " +
		"Eg: it takes 1.6 seconds to drink a potion, or 1.4 seconds to load an unenchanted crossbow. " +
		"Some items, like bows and shields, do not have a limit to their use. They will return 1 hour.")
@Examples({"on right click:",
		"\tbroadcast max usage duration of player's tool"})
@Since("INSERTVERSION")
@RequiredPlugins("Paper 1.12.2 or newer")
public class ExprMaxItemUseTime extends SimplePropertyExpression<ItemType, Timespan> {

	static {
		if (Skript.methodExists(ItemStack.class, "getMaxItemUseDuration"))
			register(ExprMaxItemUseTime.class, Timespan.class, "max[imum] [item] us(e|age) (time|duration)", "itemtypes");
	}

	@Override
	public @Nullable Timespan convert(ItemType itemData) {
		return Timespan.fromTicks(itemData.getRandom().getMaxItemUseDuration());
	}

	@Override
	public Class<? extends Timespan> getReturnType() {
		return Timespan.class;
	}

	@Override
	protected String getPropertyName() {
		return "max use time";
	}

}
