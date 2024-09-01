package ch.njol.skript.bukkitutil.sounds;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.OptionalLong;

// Player adapter pattern
class PlayerSoundReceiver implements SoundReceiver {
	private final Player player;

	protected PlayerSoundReceiver(Player player) {
		this.player = player;
	}

	@Override
	public void playSound(Location location, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
		//noinspection DuplicatedCode
		if (ADVENTURE_API) {
			AdventureSoundUtils.playSound(player, location, sound, category, volume, pitch, seed);
		} else if (!SPIGOT_SOUND_SEED || seed.isEmpty()) {
			player.playSound(location, sound.getKey(), category, volume, pitch);
		} else {
			player.playSound(location, sound.getKey(), category, volume, pitch, seed.getAsLong());
		}
	}

	private void playSound(Entity entity, String sound, SoundCategory category, float volume, float pitch) {
		//noinspection DuplicatedCode
		if (ENTITY_EMITTER_STRING) {
			player.playSound(entity, sound, category, volume, pitch);
		} else if (ENTITY_EMITTER_SOUND) {
			Sound enumSound;
			try {
				enumSound = Sound.valueOf(sound.replace('.','_').toUpperCase(Locale.ENGLISH));
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
			AdventureSoundUtils.playSound(player, entity, sound, category, volume, pitch, seed);
		} else if (!SPIGOT_SOUND_SEED || seed.isEmpty()) {
			this.playSound(entity, sound.getKey(), category, volume, pitch);
		} else {
			player.playSound(entity, sound.getKey(), category, volume, pitch, seed.getAsLong());
		}
	}
}
