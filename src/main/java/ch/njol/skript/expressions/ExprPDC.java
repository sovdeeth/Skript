package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static ch.njol.util.StringUtils.hexStringToByteArray;

public class ExprPDC extends PropertyExpression<Object, Object> {

	static {
		register(ExprPDC.class, Object.class, "pdc tag %string%", "chunks/worlds/entities/blocks/itemtypes/offlineplayers");
	}

	private Expression<String> tag;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		tag = (Expression<String>) expressions[matchedPattern];
		setExpr(expressions[matchedPattern == 0 ? 1 : 0]);
		return true;
	}

	@Override
	protected Object[] get(Event event, Object[] source) {
		String tagName = tag.getSingle(event);
		if (tagName == null)
			return new Object[0];
		var key = NamespacedKey.fromString(tagName);

		List<Object> values = new ArrayList<>();
		for (Object holder : source) {
			if (holder == null)
				continue;
			editPersistentDataContainer(holder, container -> {
				assert key != null;
				if (!container.has(key, PersistentDataType.STRING)) {
					// If the key does not exist, we skip this holder
					return;
				}
				String stringValue = container.get(key, PersistentDataType.STRING);
				Object object;
				if (stringValue != null && (object = deserialize(stringValue)) != null) {
					values.add(object);
				}
			});
		}
		return values.toArray(new Object[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, DELETE -> new Class<?>[]{Object.class};
			default -> null;
		};
	}

	@Override
	public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
		var tagName = tag.getSingle(event);
		if (tagName == null)
			return;
		var key = NamespacedKey.fromString(tagName);
		if (key == null)
			return; // Invalid key, cannot proceed

		for (Object holder : getExpr().getArray(event)) {
			editPersistentDataContainer(holder, container -> {
				if (mode == Changer.ChangeMode.SET) {
					assert delta != null;
					container.set(key, PersistentDataType.STRING, serialize(delta[0]));
				} else if (mode == Changer.ChangeMode.DELETE) {
					container.remove(key);
				}
			});
		}
	}

	private void editPersistentDataContainer(Object holder, Consumer<PersistentDataContainer> consumer) {
		if (holder instanceof PersistentDataHolder dataHolder)
			consumer.accept(dataHolder.getPersistentDataContainer());
		else if (holder instanceof ItemType itemType) {
			var meta = itemType.getItemMeta();
			consumer.accept(meta.getPersistentDataContainer());
			itemType.setItemMeta(meta);
		} else if (holder instanceof ItemStack itemStack) {
			if (!itemStack.hasItemMeta()) return;
			var meta = itemStack.getItemMeta();
			consumer.accept(meta.getPersistentDataContainer());
			itemStack.setItemMeta(meta);
		} else if (holder instanceof Slot slot) {
			var item =  slot.getItem();
			if (item == null || !item.hasItemMeta()) return;
			var meta = item.getItemMeta();
			consumer.accept(meta.getPersistentDataContainer());
			item.setItemMeta(meta);
			slot.setItem(item);
		} else if (holder instanceof Block block && block.getState() instanceof TileState tileState) {
			consumer.accept(tileState.getPersistentDataContainer());
			tileState.update();
		}
	}

	@Override
	public Class<?> getReturnType() {
		return Object.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "pdc tag " + tag.toString(event, debug) + " of " + getExpr().toString(event, debug);
	}

	private @NotNull String serialize(Object object) {
		var value = Classes.serialize(object);
		assert value != null;
		return value.type + ":" + bytesToHex(value.data);
	}

	private Object deserialize(@NotNull String input) {
		var values = input.split(":", 2);
		var type = values[0];
		var data = values[1];
		return Classes.deserialize(type, hexStringToByteArray(data));
	}

	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

	private static String bytesToHex(byte[] bytes) {
		byte[] hexChars = new byte[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars, StandardCharsets.UTF_8);
	}
}
