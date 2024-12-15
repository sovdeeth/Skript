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

import java.util.List;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

@Name("Exploded Blocks")
@Description("Get all the blocks that were destroyed in an explode event. Supports add/remove/set/clear/delete blocks.")
@Examples({
	"on explode:",
		"\tloop exploded blocks:",
			"\t\tadd loop-block to {exploded::blocks::*}",
	"",
	"on explode:",
		"\tloop exploded blocks:",
			"\t\tif loop-block is grass:",
				"\t\t\tremove loop-block from exploded blocks",
	"",
	"on explode:",
		"\tclear exploded blocks",
	"",
	"on explode:",
		"\tset exploded blocks to blocks in radius 10 around event-entity",
	"",
	"on explode:",
		"\tadd blocks above event-entity to exploded blocks"})
@Events("explode")
@Since("2.5, 2.8.6 (modify blocks)")
public class ExprExplodedBlocks extends SimpleExpression<Block> {

	static {
		Skript.registerExpression(ExprExplodedBlocks.class, Block.class, ExpressionType.COMBINED, "[the] exploded blocks");
	}
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (!getParser().isCurrentEvent(EntityExplodeEvent.class)) {
			Skript.error("Exploded blocks can only be retrieved from an explode event.");
			return false;
		}
		return true;
	}
	
	@Nullable
	@Override
	protected Block[] get(Event e) {
		if (!(e instanceof EntityExplodeEvent))
			return null;

		List<Block> blockList = ((EntityExplodeEvent) e).blockList();
		return blockList.toArray(new Block[blockList.size()]);
	}

	@Override
	public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
		switch (mode) {
			case ADD:
			case REMOVE:
			case SET:
			case DELETE:
				return CollectionUtils.array(Block[].class);
			default:
				return null;
		}
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
		if (!(event instanceof EntityExplodeEvent))
			return;

		List<Block> blocks = ((EntityExplodeEvent) event).blockList();
		switch (mode) {
			case DELETE:
				blocks.clear();
				break;
			case SET:
				blocks.clear();
				// Fallthrough intended
			case ADD:
				for (Object object : delta) {
					if (object instanceof Block)
						blocks.add((Block) object);
				}
				break;
			case REMOVE:
				for (Object object : delta) {
					if (object instanceof Block)
						blocks.remove((Block) object);
				}
				break;
		}
	}
	
	@Override
	public boolean isSingle() {
		return false;
	}
	
	@Override
	public Class<? extends Block> getReturnType() {
		return Block.class;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean d) {
		return "exploded blocks";
	}
	
}
