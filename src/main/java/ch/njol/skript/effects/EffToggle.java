package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@Name("Toggle")
@Description("Toggle the state of a block or boolean.")
@Examples({"# use arrows to toggle switches, doors, etc.",
		"on projectile hit:",
		"\tprojectile is arrow",
		"\ttoggle the block at the arrow"})
@Since("1.4")
public class EffToggle extends Effect {

	private static final Patterns<Action> patterns = new Patterns<>(new Object[][]{
			{"(open|turn on|activate) %blocks%", Action.ACTIVATE},
			{"(close|turn off|de[-]activate) %blocks%", Action.DEACTIVATE},
			{"(toggle|switch) [[the] state of] %blocks/booleans%", Action.TOGGLE}
		});

	static {
		Skript.registerEffect(EffToggle.class, patterns.getPatterns());
	}

	private enum Action {
		ACTIVATE, DEACTIVATE, TOGGLE
	}

	private enum Type {
		BLOCKS, BOOLEANS, MIXED
	}

	private Expression<?> togglables;
	private Action action;
	private Type type;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		togglables = expressions[0];
		action = patterns.getInfo(matchedPattern);

		// try to determine if only booleans or only blocks are used
		type = Type.MIXED;
		if (Block.class.isAssignableFrom(togglables.getReturnType())) {
			type = Type.BLOCKS;
		} else if (Boolean.class.isAssignableFrom(togglables.getReturnType())) {
			type = Type.BOOLEANS;
			// ensure the expression can accept boolean changes
			if (!Changer.ChangerUtils.acceptsChange(togglables, Changer.ChangeMode.SET, Boolean.class)) {
				Skript.error("Cannot toggle '" + togglables + "' as it cannot be set to booleans.");
				return false;
			}
		}

		// Only allow mixed types if the expression accepts Objects.
		if (type == Type.MIXED && !Changer.ChangerUtils.acceptsChange(togglables, Changer.ChangeMode.SET, Object.class)) {
			Skript.error("Cannot toggle '" + togglables + "' as it cannot be set to both blocks and booleans.");
			return false;
		}

		return true;
	}

	@Override
	protected void execute(Event event) {
		if (type == Type.BOOLEANS) {
			toggleBooleans(event);
		} else if (type == Type.BLOCKS) {
			toggleBlocks(event);
		} else {
			// mixed types, special handling
			toggleMixed(event);
		}

	}

	/**
	 * Toggles blocks by opening/closing or powering/unpowering them.
	 * No changes are performed, so this is safe for all expressions. Other types, like {@link Boolean}s, are ignored.
	 * @param event the event used for evaluation
	 */
	private void toggleBlocks(Event event) {
		for (Object obj : togglables.getArray(event)) {
			if (obj instanceof Block block)
				toggleSingleBlock(block);
		}
	}

	/**
	 * Toggles a single block, either by opening/closing or powering/unpowering it.
	 * @param block The block to toggle
	 */
	private void toggleSingleBlock(@NotNull Block block) {
		BlockData data = block.getBlockData();
		if (action == Action.TOGGLE) {
			if (data instanceof Openable openable) // open = NOT was open
				openable.setOpen(!openable.isOpen());
			else if (data instanceof Powerable powerable) // power = NOT power
				powerable.setPowered(!powerable.isPowered());
		} else {
			boolean value = action == Action.ACTIVATE;
			if (data instanceof Openable openable)
				openable.setOpen(value);
			else if (data instanceof Powerable powerable)
				powerable.setPowered(value);
		}

		block.setBlockData(data);
	}

	/**
	 * Uses {@link Expression#changeInPlace(Event, Function)} to toggle booleans.
	 * This is safe if the expression accepts Boolean changes,
	 * but may lead to data loss if the expression returns more than just booleans,
	 * as the other objects are discarded.
	 * @param event the event used for evaluation
	 */
	private void toggleBooleans(Event event) {
		togglables.changeInPlace(event, (obj) -> {
			if (!(obj instanceof Boolean bool))
				return null;
			if (action == Action.TOGGLE)
				return !bool;
			return action == Action.ACTIVATE;
		});
	}

	/**
	 * Uses {@link Expression#changeInPlace(Event, Function)} to toggle both blocks and booleans.
	 * This is only safe if the expression accepts Object changes.
	 * @param event the event used for evaluation
	 */
	private void toggleMixed(Event event) {
		togglables.changeInPlace(event, (obj) -> {
			if (obj instanceof Block block) {
				toggleSingleBlock(block);
				return block;
			} else if (obj instanceof Boolean bool) {
				if (action == Action.TOGGLE)
					return !bool;
				return action == Action.ACTIVATE;
			}
			return obj;
		});
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "toggle " + togglables.toString(event, debug);
	}
	
}
