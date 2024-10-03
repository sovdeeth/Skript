package org.skriptlang.skript.bukkit.tags;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import io.papermc.paper.tag.EntitySetTag;
import io.papermc.paper.tag.EntityTags;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class TagModule {

	// paper tags
	public static final List<Tag<Material>> PAPER_MATERIAL_TAGS = new ArrayList<>();
	public static final List<Tag<EntityType>> PAPER_ENTITY_TAGS = new ArrayList<>();
	public static final boolean PAPER_TAGS_EXIST = Skript.classExists("com.destroystokyo.paper.MaterialTags");

	// bukkit tags
	public record RegistryInfo(String registry, String pattern, Class<? extends Keyed> type) {}

	// Fluid and GameEvent registries also exist, but are not useful at this time.
	// Any new types added here must be handled in CondIsTagged and ExprTagsOf.
	public static final List<RegistryInfo> REGISTRIES = new ArrayList<>(List.of(
		new RegistryInfo(Tag.REGISTRY_ITEMS, "item", Material.class),
		new RegistryInfo(Tag.REGISTRY_BLOCKS, "block", Material.class),
		new RegistryInfo(Tag.REGISTRY_ENTITY_TYPES, "entity [type]", EntityType.class)
	));

	public static final String REGISTRIES_PATTERN;

	static {

		StringBuilder registriesPattern = new StringBuilder("[");
		int numRegistries = REGISTRIES.size();
		for (int i = 0; i < numRegistries; i++) {
			registriesPattern.append(i + 1).append(":").append(REGISTRIES.get(i).pattern());
			if (i + 1 != numRegistries)
				registriesPattern.append("|");
		}
		registriesPattern.append("]");
		REGISTRIES_PATTERN = registriesPattern.toString();
	}

	@Contract(value = "null -> null", pure = true)
	public static @Nullable Keyed getKeyed(Object input) {
		Keyed value = null;
		if (input == null)
			return null;
		if (input instanceof Entity entity) {
			value = entity.getType();
		} if (input instanceof EntityData<?> data) {
			value = EntityUtils.toBukkitEntityType(data);
		} else if (input instanceof ItemType itemType) {
			value = itemType.getMaterial();
		} else if (input instanceof ItemStack itemStack) {
			value = itemStack.getType();
		} else if (input instanceof Slot slot) {
			ItemStack stack = slot.getItem();
			if (stack == null)
				return null;
			value = stack.getType();
		} else if (input instanceof Block block) {
			value = block.getType();
		} else if (input instanceof BlockData data) {
			value = data.getMaterial();
		}
		return value;
	}

	public static void load() throws IOException {
		// abort if no class exists
		if (!Skript.classExists("org.bukkit.Tag"))
			return;

		// load classes (todo: replace with registering methods after regitration api
		Skript.getAddonInstance().loadClasses("org.skriptlang.skript.bukkit", "tags");

		// Classes
		Classes.registerClass(new ClassInfo<>(Tag.class, "minecrafttag")
			.user("minecraft ?tags?")
			.name("Minecraft Tag")
			.description("A tag that classifies a material, or entity.")
			.since("INSERT VERSION")
			.parser(new Parser<Tag<?>>() {
				@Override
				public boolean canParse(ParseContext context) {
					return false;
				}

				@Override
				public String toString(Tag<?> tag, int flags) {
					String key = tag.getKey().toString();
					if (TagModule.PAPER_TAGS_EXIST && (tag instanceof MaterialSetTag || tag instanceof EntitySetTag))
						key = key.replace("_settag", "");
					return "minecraft tag \"" + key + "\"";
				}

				@Override
				public String toVariableNameString(Tag<?> tag) {
					String key = tag.getKey().toString();
					if (TagModule.PAPER_TAGS_EXIST && (tag instanceof MaterialSetTag || tag instanceof EntitySetTag))
						key = key.replace("_settag", "");
					return "minecraft tag: " + key;
				}
			}));


		if (PAPER_TAGS_EXIST) {
			try {
				for (Field field : MaterialTags.class.getDeclaredFields()) {
					if (field.canAccess(null))
						//noinspection unchecked
						PAPER_MATERIAL_TAGS.add((Tag<Material>) field.get(null));
				}
				for (Field field : EntityTags.class.getDeclaredFields()) {
					if (field.canAccess(null))
						//noinspection unchecked
						PAPER_ENTITY_TAGS.add((Tag<EntityType>) field.get(null));
				}
			} catch (IllegalAccessException ignored) {}
		}
	}

}
