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

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.util.ConvertedExpression;
import org.skriptlang.skript.lang.converter.Converters;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.eclipse.jdt.annotation.Nullable;

@Name("Type of")
@Description({
	"Type of a block, item, entity, inventory or potion effect.",
	"Types of items, blocks and block datas are item types similar to them but have amounts",
	"of one, no display names and, on Minecraft 1.13 and newer versions, are undamaged.",
	"Types of entities and inventories are entity types and inventory types known to Skript.",
	"Types of potion effects are potion effect types."
})
@Examples({"on rightclick on an entity:",
	"\tmessage \"This is a %type of clicked entity%!\""})
@Since("1.4, 2.5.2 (potion effect), 2.7 (block datas)")
public class ExprTypeOf extends SimplePropertyExpression<Object, Object> {

	static {
		register(ExprTypeOf.class, Object.class, "type", "entitydatas/itemtypes/inventories/potioneffects/blockdatas");
	}

	@Override
	protected String getPropertyName() {
		return "type";
	}

	@Override
	@Nullable
	public Object convert(Object o) {
		if (o instanceof EntityData) {
			return ((EntityData<?>) o).getSuperType();
		} else if (o instanceof ItemType) {
			return ((ItemType) o).getBaseType();
		} else if (o instanceof Inventory) {
			return ((Inventory) o).getType();
		} else if (o instanceof PotionEffect) {
			return ((PotionEffect) o).getType();
		} else if (o instanceof BlockData) {
			return new ItemType(((BlockData) o).getMaterial());
		}
		assert false;
		return null;
	}

	@Override
	public Class<?> getReturnType() {
		Class<?> returnType = getExpr().getReturnType();
		return EntityData.class.isAssignableFrom(returnType) ? EntityData.class
			: ItemType.class.isAssignableFrom(returnType) ? ItemType.class
			: PotionEffectType.class.isAssignableFrom(returnType) ? PotionEffectType.class
			: BlockData.class.isAssignableFrom(returnType) ? ItemType.class : Object.class;
	}

	@Override
	@Nullable
	protected <R> ConvertedExpression<Object, ? extends R> getConvertedExpr(final Class<R>... to) {
		if (!Converters.converterExists(EntityData.class, to) && !Converters.converterExists(ItemType.class, to))
			return null;
		return super.getConvertedExpr(to);
	}
}
