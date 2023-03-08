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
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.IteratorIterable;
import com.google.common.collect.Iterables;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

@Name("Items")
@Description("Items or blocks of a specific type, useful for looping.")
@Examples({
		"loop items of type ore and log:",
		"\tblock contains loop-item",
		"\tmessage \"Theres at least one %loop-item% in this block\"",
		"drop all blocks at the player # drops one of every block at the player"
})
@Since("1.0 pre-5")
public class ExprItems extends SimpleExpression<ItemType> {

	private static final ItemType[] ALL_BLOCKS = Arrays.stream(Material.values())
		.filter(Material::isBlock)
		.map(ItemType::new)
		.toArray(ItemType[]::new);

	static {
		Skript.registerExpression(ExprItems.class, ItemType.class, ExpressionType.COMBINED,
			"[all [[of] the]|the] block[[ ]type]s",
			"every block[[ ]type]",
			"[all [[of] the]|the|every] block[s] of type[s] %itemtypes%",
			"[all [[of] the]|the|every] item[s] of type[s] %itemtypes%"
		);
	}

	@Nullable
	private Expression<ItemType> itemTypeExpr;
	private boolean items;
	private ItemType[] buffer = null;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		items = matchedPattern == 3;
		itemTypeExpr = matchedPattern == 0 || matchedPattern == 1 ? null : (Expression<ItemType>) exprs[0];
		if (itemTypeExpr instanceof Literal) {
			for (ItemType itemType : ((Literal<ItemType>) itemTypeExpr).getAll())
				itemType.setAll(true);
		}
		return true;
	}

	@Override
	@Nullable
	protected ItemType[] get(Event event) {
		if (buffer != null)
			return buffer;
		List<ItemType> items = new ArrayList<>();
		iterator(event).forEachRemaining(items::add);
		ItemType[] itemTypes = items.toArray(new ItemType[0]);
		if (itemTypeExpr instanceof Literal)
			buffer = itemTypes;
		return itemTypes;
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public Iterator<ItemType> iterator(Event event) {
		if (!items && itemTypeExpr == null)
			return Arrays.stream(ALL_BLOCKS)
				.map(ItemType::clone)
				.iterator();

		Iterable<ItemStack> itemStackIterable = Iterables.concat(itemTypeExpr.stream(event)
			.map(ItemType::getAll)
			.toArray(Iterable[]::new));

		if (items) {
			return StreamSupport.stream(itemStackIterable.spliterator(), false)
				.map(ItemType::new)
				.iterator();
		} else {
			return StreamSupport.stream(itemStackIterable.spliterator(), false)
				.filter(itemStack -> itemStack.getType().isBlock())
				.map(ItemType::new)
				.iterator();
		}
	}

	@Override
	public Class<? extends ItemType> getReturnType() {
		return ItemType.class;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "all of the " + (items ? "items" : "blocks") + (itemTypeExpr != null ? " of type " + itemTypeExpr.toString(event, debug) : "");
	}

	@Override
	public boolean isLoopOf(String string) {
		if (items) {
			return string.equals("item");
		} else {
			return string.equals("block");
		}
	}

}
