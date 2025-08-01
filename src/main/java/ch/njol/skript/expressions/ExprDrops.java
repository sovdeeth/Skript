package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Experience;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Drops")
@Description({
	"This expression only works in death and harvest events.",
	"In a death event, will hold the drops of the dying creature.",
	"Drops can be prevented by removing them with \"remove ... from drops\", "
		+ "e.g. \"remove all pickaxes from the drops\", or \"clear drops\" if you don't want any drops at all."
})
@Examples({
	"clear drops",
	"remove 4 planks from the drops"
})
@Since("1.0")
@Events("death")
public class ExprDrops extends SimpleExpression<ItemType> implements EventRestrictedSyntax {

	static {
		Skript.registerExpression(ExprDrops.class, ItemType.class, ExpressionType.SIMPLE, "[the] drops");
	}

	private boolean isDeathEvent;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (getParser().isCurrentEvent(EntityDeathEvent.class))
			isDeathEvent = true;
		return true;
	}

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return CollectionUtils.array(EntityDeathEvent.class, PlayerHarvestBlockEvent.class);
	}

	@Override
	protected ItemType @Nullable [] get(Event event) {
		if (event instanceof EntityDeathEvent entityDeathEvent) {
			return entityDeathEvent.getDrops()
				.stream()
				.map(ItemType::new)
				.toArray(ItemType[]::new);
		} else if (event instanceof PlayerHarvestBlockEvent harvestBlockEvent) {
			return harvestBlockEvent.getItemsHarvested()
				.stream()
				.map(ItemType::new)
				.toArray(ItemType[]::new);
		}
		assert false;
		return new ItemType[0];
	}

	@Override
	public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
		if (getParser().getHasDelayBefore().isTrue()) {
			Skript.error("Can't change the drops after the event has already passed");
			return null;
		}
		return switch (mode) {
			case SET, ADD, REMOVE -> {
				if (isDeathEvent)
					yield CollectionUtils.array(ItemType[].class, Inventory[].class, Experience[].class);
				yield CollectionUtils.array(ItemType[].class, Inventory[].class);
			}
			default -> null;
		};
	}

	@Override
	public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
		List<ItemStack> drops = null;
		int originalExperience = 0;
		if (event instanceof EntityDeathEvent entityDeathEvent) {
			drops = entityDeathEvent.getDrops();
			originalExperience = entityDeathEvent.getDroppedExp();
		} else if (event instanceof PlayerHarvestBlockEvent harvestBlockEvent) {
			drops = harvestBlockEvent.getItemsHarvested();
		} else {
			return;
		}

		assert delta != null;

		// separate the delta into experience and drops to make it easier to handle
		int deltaExperience = -1; // Skript does not support negative experience, so -1 is a safe "unset" value
		boolean removeAllExperience = false;
		List<ItemType> deltaDrops = new ArrayList<>();
		for (Object object : delta) {
			if (object instanceof Experience experience) {
				// Special case for `remove xp from the drops`
				if ((experience.getInternalXP() == -1 && mode == ChangeMode.REMOVE) || mode == ChangeMode.REMOVE_ALL) {
					removeAllExperience = true;
				}
				// add the value even if we're removing all experience, just so we know that experience was changed
				if (deltaExperience == -1) {
					deltaExperience = experience.getXP();
				} else {
					deltaExperience += experience.getXP();
				}
			} else if (object instanceof Inventory inventory) {
				// inventories are unrolled into their contents
				for (ItemStack item : inventory.getContents()) {
					if (item != null)
						deltaDrops.add(new ItemType(item));
				}
			} else if (object instanceof ItemType itemType) {
				deltaDrops.add(itemType);
			} else {
				assert false;
			}
		}

		// handle items and experience separately to maintain current behavior
		// `set drops to iron sword` should not affect experience
		// and `set drops to 1 xp` should not affect items
		// todo: All the experience stuff should be removed from this class for 2.8 and given to ExprExperience

		// handle experience
		if (deltaExperience > -1 && event instanceof EntityDeathEvent entityDeathEvent) {
			switch (mode) {
				case SET:
					entityDeathEvent.setDroppedExp(deltaExperience);
					break;
				case ADD:
					entityDeathEvent.setDroppedExp(originalExperience + deltaExperience);
					break;
				case REMOVE:
					entityDeathEvent.setDroppedExp(originalExperience - deltaExperience);
					// fallthrough to check for removeAllExperience
				case REMOVE_ALL:
					if (removeAllExperience)
						entityDeathEvent.setDroppedExp(0);
					break;
				case DELETE:
				case RESET:
					assert false;
			}
		}

		// handle items
		if (!deltaDrops.isEmpty()) {
			switch (mode) {
				case SET:
					// clear drops and fallthrough to add
					drops.clear();
				case ADD:
					for (ItemType item : deltaDrops) {
						item.addTo(drops);
					}
					break;
				case REMOVE:
					for (ItemType item : deltaDrops) {
						item.removeFrom(false, drops);
					}
					break;
				case REMOVE_ALL:
					for (ItemType item : deltaDrops) {
						item.removeAll(false, drops);
					}
					break;
				case DELETE:
				case RESET:
					assert false;
			}
		}
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends ItemType> getReturnType() {
		return ItemType.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "the drops";
	}

}
