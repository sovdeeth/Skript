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
package ch.njol.skript.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.LanguageChangeListener;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("deprecation")
public abstract class PotionEffectUtils {
	
	private static final boolean HAS_SUSPICIOUS_META = Skript.classExists("org.bukkit.inventory.meta.SuspiciousStewMeta");
	
	private PotionEffectUtils() {}
	
	final static Map<String, PotionEffectType> types = new HashMap<>();
	
	final static String[] names = new String[getMaxPotionId() + 1];
	
	// MCPC+ workaround
	private static int getMaxPotionId() {
		int i = 0;
		for (final PotionEffectType t : PotionEffectType.values()) {
			if (t != null && t.getId() > i)
				i = t.getId();
		}
		return i;
	}
	
	static {
		Language.addListener(new LanguageChangeListener() {
			@Override
			public void onLanguageChange() {
				types.clear();
				for (final PotionEffectType t : PotionEffectType.values()) {
					if (t == null)
						continue;
					final String[] ls = Language.getList("potions." + t.getName());
					names[t.getId()] = ls[0];
					for (final String l : ls) {
						types.put(l.toLowerCase(Locale.ENGLISH), t);
					}
				}
			}
		});
	}
	
	@Nullable
	public static PotionEffectType parseType(final String s) {
		return types.get(s.toLowerCase(Locale.ENGLISH));
	}
	
	// This is a stupid bandaid to fix comparison issues when converting potion datas
	@Nullable
	public static PotionEffectType parseByEffectType(PotionEffectType t) {
		for (PotionEffectType value : types.values()) {
			if (t.equals(value)) {
				return value;
			}
		}
		return null;
	}
	
	@SuppressWarnings("null")
	public static String toString(final PotionEffectType t) {
		return names[t.getId()];
	}
	
	// REMIND flags?
	@SuppressWarnings("null")
	public static String toString(final PotionEffectType t, final int flags) {
		return names[t.getId()];
	}
	
	public static String toString(PotionEffect potionEffect) {
		StringBuilder builder = new StringBuilder();
		if (potionEffect.isAmbient())
			builder.append("ambient ");
		builder.append("potion effect of ");
		builder.append(toString(potionEffect.getType()));
		
		builder.append(" of tier ").append(potionEffect.getAmplifier() + 1);
		
		if (!potionEffect.hasParticles())
			builder.append(" without particles");
		builder.append(" for ").append(Timespan.fromTicks_i(potionEffect.getDuration()));
		return builder.toString();
	}
	
	public static String[] getNames() {
		return names;
	}
	
	public static short guessData(final ThrownPotion p) {
		if (p.getEffects().size() == 1) {
			final PotionEffect e = p.getEffects().iterator().next();
			PotionType type = PotionType.getByEffect(e.getType());
			assert type != null;
			final Potion d = new Potion(type).splash();
			return d.toDamageValue();
		}
		return 0;
	}
	
	/**
	 * Checks if given string represents a known potion type and returns that type.
	 * Unused currently, will be used soon (TM).
	 * @param name Name of potion type
	 * @return
	 */
	@Nullable
	public static PotionType checkPotionType(String name) {
		switch (name) {
			case "uncraftable":
			case "empty":
				return PotionType.UNCRAFTABLE;
			case "mundane":
				return PotionType.MUNDANE;
			case "thick":
				return PotionType.THICK;
			case "night vision":
			case "darkvision":
				return PotionType.NIGHT_VISION;
			case "invisibility":
				return PotionType.INVISIBILITY;
			case "leaping":
			case "jump boost":
				return PotionType.JUMP;
			case "fire resistance":
			case "fire immunity":
				return PotionType.FIRE_RESISTANCE;
			case "swiftness":
			case "speed":
				return PotionType.SPEED;
			case "slowness":
				return PotionType.SLOWNESS;
			case "water breathing":
				return PotionType.WATER_BREATHING;
			case "instant health":
			case "healing":
			case "health":
				return PotionType.INSTANT_HEAL;
			case "instant damage":
			case "harming":
			case "damage":
				return PotionType.INSTANT_DAMAGE;
			case "poison":
				return PotionType.POISON;
			case "regeneration":
			case "regen":
				return PotionType.REGEN;
			case "strength":
				return PotionType.STRENGTH;
			case "weakness":
				return PotionType.WEAKNESS;
			case "luck":
				return PotionType.LUCK;
		}
		
		return null;
	}
	
	/**
	 * Wrapper around deprecated API function, in case it gets removed.
	 * Changing one method is easier that changing loads of them from different expressions.
	 * @param effect Type.
	 * @return Potion type.
	 */
	@Nullable
	public static PotionType effectToType(PotionEffectType effect) {
		return PotionType.getByEffect(effect);
	}
	
	/**
	 * Get potion string representation.
	 * @param effect
	 * @param extended
	 * @param strong
	 * @return
	 */
	public static String getPotionName(@Nullable PotionEffectType effect, boolean extended, boolean strong) {
		if (effect == null) return "bottle of water"; 
		
		String s = "";
		if (extended) s += "extended";
		else if (strong) s += "strong";
		s += " potion of ";
		s += toString(effect);
		
		return s;
	}
	
	/**
	 * Clear all the active {@link PotionEffect PotionEffects} from an Entity
	 *
	 * @param entity Entity to clear effects for
	 */
	public static void clearAllEffects(LivingEntity entity) {
		entity.getActivePotionEffects().forEach(potionEffect -> entity.removePotionEffect(potionEffect.getType()));
	}
	
	/**
	 * Add PotionEffects to an entity
	 *
	 * @param entity Entity to add effects to
	 * @param effects {@link PotionEffect} or {@link PotionEffectType} to add
	 */
	public static void addEffects(LivingEntity entity, Object[] effects) {
		for (Object object : effects) {
			PotionEffect effect;
			if (object instanceof PotionEffect)
				effect = (PotionEffect) object;
			else if (object instanceof PotionEffectType)
				effect = new PotionEffect((PotionEffectType) object, 15 * 20, 0, false);
			else
				continue;
			
			entity.addPotionEffect(effect);
		}
	}
	
	/**
	 * Remove a PotionEffect from an entity
	 *
	 * @param entity Entity to remove effects for
	 * @param effects {@link PotionEffect} or {@link PotionEffectType} to remove
	 */
	public static void removeEffects(LivingEntity entity, Object[] effects) {
		for (Object object : effects) {
			PotionEffectType effectType;
			if (object instanceof PotionEffect)
				effectType = ((PotionEffect) object).getType();
			else if (object instanceof PotionEffectType)
				effectType = (PotionEffectType) object;
			else
				continue;
			
			entity.removePotionEffect(effectType);
		}
	}
	
	/**
	 * Clear all {@link PotionEffect PotionEffects} from an ItemType
	 *
	 * @param itemType Item to remove effects from
	 */
	public static void clearAllEffects(ItemType itemType) {
		ItemMeta meta = itemType.getItemMeta();
		if (meta instanceof PotionMeta)
			((PotionMeta) meta).clearCustomEffects();
		else if (HAS_SUSPICIOUS_META && meta instanceof SuspiciousStewMeta)
			((SuspiciousStewMeta) meta).clearCustomEffects();
		itemType.setItemMeta(meta);
	}
	
	/**
	 * Add PotionEffects to an ItemTye
	 *
	 * @param itemType Item to add effects to
	 * @param effects {@link PotionEffect} or {@link PotionEffectType} to add
	 */
	public static void addEffects(ItemType itemType, Object[] effects) {
		ItemMeta meta = itemType.getItemMeta();
		for (Object object : effects) {
			PotionEffect effect;
			if (object instanceof PotionEffect)
				effect = (PotionEffect) object;
			else if (object instanceof PotionEffectType)
				effect = new PotionEffect((PotionEffectType) object, 15 * 20, 0, false);
			else
				continue;
			
			if (meta instanceof PotionMeta)
				((PotionMeta) meta).addCustomEffect(effect, false);
			else if (HAS_SUSPICIOUS_META && meta instanceof SuspiciousStewMeta)
				((SuspiciousStewMeta) meta).addCustomEffect(effect, false);
		}
		itemType.setItemMeta(meta);
	}
	
	/**
	 * Remove a PotionEffect from an ItemType
	 *
	 * @param itemType Item to remove effects from
	 * @param effects {@link PotionEffect} or {@link PotionEffectType} to remove
	 */
	public static void removeEffects(ItemType itemType, Object[] effects) {
		ItemMeta meta = itemType.getItemMeta();
		
		for (Object object : effects) {
			PotionEffectType effectType;
			if (object instanceof PotionEffect)
				effectType = ((PotionEffect) object).getType();
			else if (object instanceof PotionEffectType)
				effectType = (PotionEffectType) object;
			else
				continue;
			
			if (meta instanceof PotionMeta)
				((PotionMeta) meta).removeCustomEffect(effectType);
			else if (HAS_SUSPICIOUS_META && meta instanceof SuspiciousStewMeta)
				((SuspiciousStewMeta) meta).removeCustomEffect(effectType);
		}
		itemType.setItemMeta(meta);
	}
	
	/**
	 * Get all the PotionEffects of an ItemType
	 *
	 * This will also include the base potion as well
	 *
	 * @param itemType Item to get potions from
	 * @return List of PotionEffects on the item
	 */
	public static List<PotionEffect> getEffects(ItemType itemType) {
		List<PotionEffect> effects = new ArrayList<>();
		ItemMeta meta = itemType.getItemMeta();
		if (meta instanceof PotionMeta) {
			PotionMeta potionMeta = ((PotionMeta) meta);
			effects.addAll(potionMeta.getCustomEffects());
			effects.addAll(PotionDataUtils.getPotionEffects(potionMeta.getBasePotionData()));
		} else if (HAS_SUSPICIOUS_META && meta instanceof SuspiciousStewMeta)
			effects.addAll(((SuspiciousStewMeta) meta).getCustomEffects());
		return effects;
	}
	
}
