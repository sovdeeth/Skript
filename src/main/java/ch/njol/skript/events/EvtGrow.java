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
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.StructureType;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.eclipse.jdt.annotation.Nullable;

public class EvtGrow extends SkriptEvent {
	
	/**
	 * Growth event restriction.
	 * 
	 * ANY means any grow event goes.
	 * 
	 * Structure/block restrict for structure/block grow events only.
	 */
	public static final int ANY = 0, STRUCTURE = 1, BLOCK = 2;
	
	static {
		Skript.registerEvent("Grow", EvtGrow.class, CollectionUtils.array(StructureGrowEvent.class, BlockGrowEvent.class),
				"grow [of (1:%-structuretype%|2:%-itemtype%)]")
				.description("Called when a tree, giant mushroom or plant grows to next stage.")
				.examples("on grow:", "on grow of a tree:", "on grow of a huge jungle tree:")
				.since("1.0 (2.2-dev20 for plants)");
	}
	
	@Nullable
	private Literal<StructureType> types;
	@Nullable
	private Literal<ItemType> blocks;
	private int eventType;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
		eventType = parseResult.mark; // ANY, STRUCTURE or BLOCK
		if (eventType == STRUCTURE) {
			types = (Literal<StructureType>) args[0];
		} else if (eventType == BLOCK) {
			blocks = (Literal<ItemType>) args[1]; // Arg 1 may not be present... but it is in the array still, as null
		}
		// Else: no type restrictions specified
		return true;
	}
	
	@Override
	public boolean check(Event event) {
		if (eventType == STRUCTURE && types != null && event instanceof StructureGrowEvent) {
			return types.check(event, type -> type.is(((StructureGrowEvent) event).getSpecies()));
		} else if (eventType == BLOCK && blocks != null && event instanceof BlockGrowEvent) {
			return blocks.check(event, type -> type.isOfType(((BlockGrowEvent) event).getNewState()));
		}
		return event instanceof BlockGrowEvent || event instanceof StructureGrowEvent;
	}
	
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (eventType == STRUCTURE) {
			return "grow" + (types != null ? " of " + types.toString(event, debug) : "");
		} else if (eventType == BLOCK) {
			return "grow" + (blocks != null ? " of " + blocks.toString(event, debug) : "");
		}
		return "grow";
	}
	
}