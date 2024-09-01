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
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Name("Play Sound")
@Description({
	"Plays a sound at given location for everyone or just for given players, or plays a sound to specified players. " +
	"Both Minecraft sound names and " +
	"<a href=\"https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html\">Spigot sound names</a> " +
	"are supported. Playing resource pack sounds are supported too. The sound category is 'master' by default. ",
	"",
	"When running 1.18+, playing a sound from an entity directly will result in the sound coming from said entity, even while moving.",
	"If the sound is custom, a location emitter will follow the entity. Do note that pitch and volume ",
	"are reflected based on the entity, and Minecraft may not use the values from this syntax.",
	"",
	"If using Paper 1.19.4+ or Adventure API 4.12.0+ you can utilize sound seeds. Minecraft sometimes have a set of sounds under one sound ID ",
	"that will randomly play, to counter this, you can directly state which seed to use.",
	"",
	"Please note that sound names can get changed in any Minecraft or Spigot version, or even removed from Minecraft itself.",
})
@Examples({
	"play sound \"block.note_block.pling\" # It is block.note.pling in 1.12.2",
	"play sound \"entity.experience_orb.pickup\" with volume 0.5 to the player",
	"play sound \"custom.music.1\" in jukebox category at {speakerBlock}",
	"play sound \"BLOCK_AMETHYST_BLOCK_RESONATE\" with seed 1 on target entity for the player #1.20.1+"
})
@RequiredPlugins("Minecraft 1.18.1+ (entity emitters), Paper 1.19.4+ or Adventure API 4.12.0+ (sound seed)")
@Since("2.2-dev28, 2.4 (sound categories), 2.9.0 (sound seed & entity emitter)")
public class EffPlaySound extends Effect {

	// <=1.17:
	// 		Player - Location - Sound/String
	// 		World - Location - Sound/String
	// 1.18:
	// 		Player - Location - Sound/String
	// 		World - Location - Sound/String
	// 		Player - Entity - Sound
	// 		World - Entity - Sound
	// 1.19:
	// 		Player - Location/Entity - Sound/String
	// 		World - Location/Entity - Sound/String
	// 1.20 - spigot adds sound seeds

	private static final boolean ADVENTURE_API = Skript.classExists("net.kyori.adventure.sound.Sound$Builder");
	private static final boolean SPIGOT_SOUND_SEED = Skript.methodExists(Player.class, "playSound", Entity.class, Sound.class, SoundCategory.class, float.class, float.class, long.class);
	private static final boolean HAS_SEED = ADVENTURE_API || SPIGOT_SOUND_SEED;
	private static final boolean ENTITY_EMITTER_SOUND = Skript.methodExists(Player.class, "playSound", Entity.class, Sound.class, SoundCategory.class, float.class, float.class);
	private static final boolean ENTITY_EMITTER_STRING = Skript.methodExists(Player.class, "playSound", Entity.class, String.class, SoundCategory.class, float.class, float.class);
	private static final boolean ENTITY_EMITTER = ENTITY_EMITTER_SOUND || ENTITY_EMITTER_STRING;
	public static final Pattern KEY_PATTERN = Pattern.compile("([a-z0-9._-]+:)?([a-z0-9/._-]+)");

	static {
		String seedOption = "";
		if (HAS_SEED)
			seedOption = "[[with] seed %-number%] ";
		String emitterTypes = "locations";
		if (ENTITY_EMITTER)
			emitterTypes += "/entities";
		Skript.registerEffect(EffPlaySound.class,
				"play sound[s] %strings% " + seedOption + "[(in|from) %-soundcategory%] " +
						"[(at|with) volume %-number%] [(and|at|with) pitch %-number%] (at|on|from) %" + emitterTypes + "% [(to|for) %-players%]",
				"play sound[s] %strings% " + seedOption + "[(in|from) %-soundcategory%] " +
						"[(at|with) volume %-number%] [(and|at|with) pitch %-number%] [(to|for) %players%] [(at|on|from) %-" + emitterTypes + "%]"
		);
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<String> sounds;

	@Nullable
	private Expression<SoundCategory> category;

	@Nullable
	private Expression<Player> players;

	@Nullable
	private Expression<Number> volume;

	@Nullable
	private Expression<Number> pitch;

	@Nullable
	private Expression<Number> seed;

	@Nullable
	private Expression<?> emitters;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		sounds = (Expression<String>) exprs[0];
		int index = 1;
		if (HAS_SEED)
			seed = (Expression<Number>) exprs[index++];
		category = (Expression<SoundCategory>) exprs[index++];
		volume = (Expression<Number>) exprs[index++];
		pitch = (Expression<Number>) exprs[index++];
		if (matchedPattern == 0) {
			emitters = exprs[index++];
			players = (Expression<Player>) exprs[index];
		} else {
			players = (Expression<Player>) exprs[index++];
			emitters = exprs[index];
		}
		return true;
	}

	@Override
	protected void execute(Event event) {
		OptionalLong seed = OptionalLong.empty();
		if (this.seed != null) {
			Number number = this.seed.getSingle(event);
			if (number != null)
				seed = OptionalLong.of(number.longValue());
		}
		SoundCategory category = this.category == null ? SoundCategory.MASTER : this.category.getOptionalSingle(event)
				.orElse(SoundCategory.MASTER);
		float volume = this.volume == null ? 1 : this.volume.getOptionalSingle(event)
				.orElse(1)
				.floatValue();
		float pitch = this.pitch == null ? 1 : this.pitch.getOptionalSingle(event)
				.orElse(1)
				.floatValue();

		// validate strings
		List<NamespacedKey> validSounds = new ArrayList<>();
		for (String sound : sounds.getArray(event)) {
			NamespacedKey key = null;
			try {
				Sound enumSound = Sound.valueOf(sound.toUpperCase(Locale.ENGLISH));
				key = enumSound.getKey();
			} catch (IllegalArgumentException alternative) {
				sound = sound.toLowerCase(Locale.ENGLISH);
				Matcher keyMatcher = KEY_PATTERN.matcher(sound);
				if (!keyMatcher.matches())
					continue;
				try {
					String namespace = keyMatcher.group(1);
					String keyValue = keyMatcher.group(2);
					if (namespace == null) {
						key = NamespacedKey.minecraft(keyValue);
					} else {
						namespace = namespace.substring(0, namespace.length() - 1);
						key = new NamespacedKey(namespace, keyValue);
					}
				} catch (IllegalArgumentException argument) {
					// The user input invalid characters
				}
			}

			if (key == null)
				continue;
			validSounds.add(key);
		}

		// play sounds
		if (players != null) {
			if (emitters == null) {
				for (Player player : players.getArray(event)) {
					SoundReceiver receiver = SoundReceiver.of(player);
					Location emitter = player.getLocation();
					for (NamespacedKey sound : validSounds)
						receiver.playSound(emitter, sound, category, volume, pitch, seed);
				}
			} else {
				for (Player player : players.getArray(event)) {
					SoundReceiver receiver = SoundReceiver.of(player);
					for (Object emitter : emitters.getArray(event)) {
						if (emitter instanceof Location location) {
							for (NamespacedKey sound : validSounds)
								receiver.playSound(location, sound, category, volume, pitch, seed);
						} else if (emitter instanceof Entity entity) {
							for (NamespacedKey sound : validSounds)
								receiver.playSound(entity, sound, category, volume, pitch, seed);
						}
					}
				}
			}
		} else if (emitters != null) {
			for (Object emitter : emitters.getArray(event)) {
				if (ENTITY_EMITTER && emitter instanceof Entity entity) {
					SoundReceiver receiver = SoundReceiver.of(entity.getWorld());
					for (NamespacedKey sound : validSounds)
						receiver.playSound(entity, sound, category, volume, pitch, seed);
				} else if (emitter instanceof Location location) {
					SoundReceiver receiver = SoundReceiver.of(location.getWorld());
					for (NamespacedKey sound : validSounds)
						receiver.playSound(location, sound, category, volume, pitch, seed);
				}
			}
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		StringBuilder builder = new StringBuilder()
				.append("play sound ")
				.append(sounds.toString(event, debug));

		if (seed != null)
			builder.append(" with seed ").append(seed.toString(event, debug));
		if (category != null)
			builder.append(" in ").append(category.toString(event, debug));
		if (volume != null)
			builder.append(" with volume ").append(volume.toString(event, debug));
		if (pitch != null)
			builder.append(" with pitch ").append(pitch.toString(event, debug));
		if (emitters != null)
			builder.append(" from ").append(emitters.toString(event, debug));
		if (players != null)
			builder.append(" to ").append(players.toString(event, debug));
		
		return builder.toString();
	}

	/**
	 * Adapter pattern to unify {@link World} and {@link Player} playSound methods.
	 * Methods can be called without determining version support, it is handled internally.
	 * Non-supported methods will simply delegate to supported methods.
	 */
	private interface SoundReceiver {
		void playSound(Location location, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed);
		void playSound(Entity entity, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed);

		static SoundReceiver of(Player player) { return new PlayerSoundReciever(player); }
		static SoundReceiver of(World world) { return new WorldSoundReciever(world); }

		static net.kyori.adventure.sound.Sound getAdventureSound(NamespacedKey key, SoundCategory category, float volume, float pitch, OptionalLong seed) {
			return net.kyori.adventure.sound.Sound.sound()
				.source(category)
				.volume(volume)
				.pitch(pitch)
				.seed(seed)
				.type(key)
				.build();
		}
	}

	// Player adapter pattern
	private record PlayerSoundReciever(Player player) implements SoundReceiver {

		@Override
		public void playSound(Location location, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
			//noinspection DuplicatedCode
			if (ADVENTURE_API) {
				player.playSound(
					SoundReceiver.getAdventureSound(sound, category, volume, pitch, seed),
					location.x(),
					location.y(),
					location.z()
				);
			} else if (!SPIGOT_SOUND_SEED || seed.isEmpty()) {
				player.playSound(location, sound.getKey(), category, volume, pitch);
			} else {
				player.playSound(location, sound.getKey(), category, volume, pitch, seed.getAsLong());
			}
		}

		private void playSound(Entity entity, String sound, SoundCategory category, float volume, float pitch) {
			//noinspection DuplicatedCode
			if (!ENTITY_EMITTER_STRING) {
				player.playSound(entity, sound, category, volume, pitch);
			} else if (ENTITY_EMITTER_SOUND) {
				Sound enumSound;
				try {
					enumSound = Sound.valueOf(sound);
				} catch (IllegalArgumentException e) {
					return;
				}
				player.playSound(entity, enumSound, category, volume, pitch);
			} else {
				player.playSound(entity.getLocation(), sound, category, volume, pitch);
			}
		}

		@Override
		public void playSound(Entity entity, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
			//noinspection DuplicatedCode
			if (ADVENTURE_API) {
				player.playSound(SoundReceiver.getAdventureSound(sound, category, volume, pitch, seed), entity);
			} else if (!SPIGOT_SOUND_SEED || seed.isEmpty()) {
				this.playSound(entity, sound.getKey(), category, volume, pitch);
			} else {
				player.playSound(entity, sound.getKey(), category, volume, pitch, seed.getAsLong());
			}
		}
	}

	// World adapter pattern
	private record WorldSoundReciever(World world) implements SoundReceiver {

		@Override
		public void playSound(Location location, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
			//noinspection DuplicatedCode
			if (ADVENTURE_API) {
				world.playSound(
					SoundReceiver.getAdventureSound(sound, category, volume, pitch, seed),
					location.x(),
					location.y(),
					location.z()
				);
			} else if (!SPIGOT_SOUND_SEED || seed.isEmpty()) {
				world.playSound(location, sound.getKey(), category, volume, pitch);
			} else {
				world.playSound(location, sound.getKey(), category, volume, pitch, seed.getAsLong());
			}
		}

		private void playSound(Entity entity, String sound, SoundCategory category, float volume, float pitch) {
			//noinspection DuplicatedCode
			if (!ENTITY_EMITTER_STRING) {
				world.playSound(entity, sound, category, volume, pitch);
			} else if (ENTITY_EMITTER_SOUND) {
				Sound enumSound;
				try {
					enumSound = Sound.valueOf(sound);
				} catch (IllegalArgumentException e) {
					return;
				}
				world.playSound(entity, enumSound, category, volume, pitch);
			} else {
				world.playSound(entity.getLocation(), sound, category, volume, pitch);
			}
		}

		@Override
		public void playSound(Entity entity, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
			//noinspection DuplicatedCode
			if (ADVENTURE_API) {
				world.playSound(SoundReceiver.getAdventureSound(sound, category, volume, pitch, seed), entity);
			} else if (!SPIGOT_SOUND_SEED || seed.isEmpty()) {
				this.playSound(entity, sound.getKey(), category, volume, pitch);
			} else {
				world.playSound(entity, sound.getKey(), category, volume, pitch, seed.getAsLong());
			}
		}
	}

}
