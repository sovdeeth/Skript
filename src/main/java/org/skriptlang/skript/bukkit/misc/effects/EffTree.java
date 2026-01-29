package org.skriptlang.skript.bukkit.misc.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Direction;
import ch.njol.skript.util.StructureType;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Tree")
@Description({"Creates a tree.",
		"This may require that there is enough space above the given location and that the block below is dirt/grass, but it is possible that the tree will just grow anyways, possibly replacing every block in its path."})
@Example("grow a tall redwood tree above the clicked block")
@Since("1.0")
public class EffTree extends Effect {

	public static void register(SyntaxRegistry registry) {
		registry.register(
				SyntaxRegistry.EFFECT,
				SyntaxInfo.builder(EffTree.class)
					.addPattern("(grow|create|generate) tree [of type %structuretype%] %directions% %locations%")
					.addPattern("(grow|create|generate) %structuretype% %directions% %locations%")
					.supplier(EffTree::new)
					.priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)
					.build()
		);
	}

	private Expression<Location> blocks;
	private Expression<StructureType> type;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parser) {
		type = (Expression<StructureType>) exprs[0];
		blocks = Direction.combine((Expression<? extends Direction>) exprs[1], (Expression<? extends Location>) exprs[2]);
		return true;
	}
	
	@Override
	public void execute(Event event) {
		StructureType type = this.type.getSingle(event);
		if (type == null)
			return;
		for (Location location : blocks.getArray(event)) {
			assert location != null : blocks;
			type.grow(location.getBlock());
		}
	}
	
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "grow tree of type " + type.toString(event, debug) + " " + blocks.toString(event, debug);
	}
	
}
