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
package ch.njol.skript.entity;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.ChestBoat;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Random;

public class BoatChestData extends EntityData<ChestBoat> {

	private static Material oakBoat = null;
	private static Material spruceBoat = null;
	private static Material birchBoat = null;
	private static Material jungleBoat = null;
	private static Material acaciaBoat = null;
	private static Material darkOakBoat = null;

	static {
		if (Skript.classExists("org.bukkit.entity.ChestBoat")) {
			//noinspection ConstantConditions
			oakBoat = Material.getMaterial("OAK_CHEST_BOAT");
			//noinspection ConstantConditions
			spruceBoat = Material.getMaterial("SPRUCE_CHEST_BOAT");
			//noinspection ConstantConditions
			birchBoat = Material.getMaterial("BIRCH_CHEST_BOAT");
			//noinspection ConstantConditions
			jungleBoat = Material.getMaterial("JUNGLE_CHEST_BOAT");
			//noinspection ConstantConditions
			acaciaBoat = Material.getMaterial("ACACIA_CHEST_BOAT");
			//noinspection ConstantConditions
			darkOakBoat = Material.getMaterial("DARK_OAK_CHEST_BOAT");
			EntityData.register(BoatChestData.class, "chest boat", ChestBoat.class, 0,
				"chest boat", "any chest boat", "oak chest boat", "spruce chest boat", "birch chest boat",
				"jungle chest boat", "acacia chest boat", "dark oak chest boat");
		}
	}

	public BoatChestData() {
		this(0);
	}

	public BoatChestData(@Nullable TreeSpecies type) {
		this(type != null ? type.ordinal() + 2 : 1);
	}

	private BoatChestData(int type) {
		matchedPattern = type;
	}

	@Override
	protected boolean init(Literal<?>[] exprs, int matchedPattern, SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	protected boolean init(@Nullable Class<? extends ChestBoat> c, @Nullable ChestBoat e) {
		if (e != null)
			matchedPattern = 2 + e.getWoodType().ordinal();
		return true;
	}

	@Override
	public void set(ChestBoat entity) {
		if (matchedPattern == 1) // If the type is 'any boat'.
			matchedPattern += new Random().nextInt(TreeSpecies.values().length); // It will spawn a random boat type in case is 'any boat'.
		if (matchedPattern > 1) // 0 and 1 are excluded
			entity.setWoodType(TreeSpecies.values()[matchedPattern - 2]); // Removes 2 to fix the index.
	}

	@Override
	protected boolean match(ChestBoat entity) {
		return matchedPattern <= 1 || entity.getWoodType().ordinal() == matchedPattern - 2;
	}

	@Override
	public Class<? extends ChestBoat> getType() {
		return ChestBoat.class;
	}

	@Override
	public EntityData getSuperType() {
		return new BoatChestData(matchedPattern);
	}

	@Override
	protected int hashCode_i() {
		return matchedPattern <= 1 ? 0 : matchedPattern;
	}

	@Override
	protected boolean equals_i(EntityData<?> obj) {
		if (obj instanceof BoatData)
			return matchedPattern == ((BoatData) obj).matchedPattern;
		return false;
	}

	@Override
	public boolean isSupertypeOf(EntityData<?> e) {
		if (e instanceof BoatData)
			return matchedPattern <= 1 || matchedPattern == ((BoatData) e).matchedPattern;
		return false;
	}

	public boolean isOfItemType(ItemType itemType) {
		int ordinal = -1;

		ItemStack stack = itemType.getRandom();
		Material type = stack.getType();
		if (oakBoat == type)
			ordinal = 0;
		else if (type == spruceBoat)
			ordinal = TreeSpecies.REDWOOD.ordinal();
		else if (birchBoat == type)
			ordinal = TreeSpecies.BIRCH.ordinal();
		else if (jungleBoat == type)
			ordinal = TreeSpecies.JUNGLE.ordinal();
		else if (acaciaBoat == type)
			ordinal = TreeSpecies.ACACIA.ordinal();
		else if (darkOakBoat == type)
			ordinal = TreeSpecies.DARK_OAK.ordinal();
		return hashCode_i() == ordinal + 2 || (matchedPattern + ordinal == 0) || ordinal == 0;
	}

}
