package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.util.slot.InventorySlot;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExprFirstEmptySlot extends SimplePropertyExpression<Inventory, Slot> {

	static {
		// support `first empty slot in inventory` as well as typical property syntax
		List<String> patterns = new ArrayList<>(Arrays.asList(getPatterns("first empty slot", "inventory")));
		patterns.add("[the] first empty slot in %inventory%");
		Skript.registerExpression(ExprFirstEmptySlot.class, Slot.class, ExpressionType.PROPERTY, patterns.toArray(new String[0]));
	}

	@Override
	public @Nullable Slot convert(Inventory from) {
		int slotIndex = from.firstEmpty();
		if (slotIndex == -1)
			return null; // No empty slot found
		return new InventorySlot(from, slotIndex);
	}

	@Override
	public Class<? extends Slot> getReturnType() {
		return Slot.class;
	}

	@Override
	protected String getPropertyName() {
		return "first empty slot";
	}
}
