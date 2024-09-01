package ch.njol.skript.bukkitutil.sounds;

import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;

import java.util.OptionalLong;

public class AdventureSoundUtils {
	public static net.kyori.adventure.sound.Sound getAdventureSound(NamespacedKey key, SoundCategory category, float volume, float pitch, OptionalLong seed) {
		return net.kyori.adventure.sound.Sound.sound()
			.source(category)
			.volume(volume)
			.pitch(pitch)
			.seed(seed)
			.type(key)
			.build();
	}
}
