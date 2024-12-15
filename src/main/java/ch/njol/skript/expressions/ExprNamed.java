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

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Getter;
import ch.njol.util.Kleenean;

/**
 * @author Peter Güttinger
 */
@Name("Named Item/Inventory")
@Description("Directly names an item/inventory, useful for defining a named item/inventory in a script. " +
		"If you want to (re)name existing items/inventories you can either use this expression or use <code>set <a href='#ExprName'>name of &lt;item/inventory&gt;</a> to &lt;text&gt;</code>.")
@Examples({"give a diamond sword of sharpness 100 named \"&lt;gold&gt;Excalibur\" to the player",
		"set tool of player to the player's tool named \"&lt;gold&gt;Wand\"",
		"set the name of the player's tool to \"&lt;gold&gt;Wand\"",
		"open hopper inventory named \"Magic Hopper\" to player"})
@Since("2.0, 2.2-dev34 (inventories)")
public class ExprNamed extends PropertyExpression<Object, Object> {
	static {
		Skript.registerExpression(ExprNamed.class, Object.class, ExpressionType.PROPERTY,
				"%itemtype/inventorytype% (named|with name[s]) %string%");
	}
	
	@SuppressWarnings("null")
	private Expression<String> name;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		setExpr(exprs[0]);
		name = (Expression<String>) exprs[1];
		return true;
	}
	
	@Override
	protected Object[] get(final Event e, final Object[] source) {
		String name = this.name.getSingle(e);
		if (name == null)
			return get(source, obj -> obj); // No name provided, do nothing
		return get(source, new Getter<Object, Object>() {
			@Override
			@Nullable
			public Object get(Object obj) {
				if (obj instanceof InventoryType)
					return Bukkit.createInventory(null, (InventoryType) obj, name);
				if (obj instanceof ItemStack) {
					ItemStack stack = (ItemStack) obj;
					stack = stack.clone();
					ItemMeta meta = stack.getItemMeta();
					if (meta != null) {
						meta.setDisplayName(name);
						stack.setItemMeta(meta);
					}
					return new ItemType(stack);
				}
				ItemType item = (ItemType) obj;
				item = item.clone();
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(name);
				item.setItemMeta(meta);
				return item;
			}
		});
	}
	
	@Override
	public Class<? extends Object> getReturnType() {
		return getExpr().getReturnType() == InventoryType.class ? Inventory.class : ItemType.class;
	}
	
	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return getExpr().toString(e, debug) + " named " + name;
	}
	
}
