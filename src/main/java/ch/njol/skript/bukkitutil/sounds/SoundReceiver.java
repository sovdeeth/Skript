package ch.njol.skript.bukkitutil.sounds;

import ch.njol.skript.Skript;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.OptionalLong;

/**
 * Adapter pattern to unify {@link World} and {@link Player} playSound methods.
 * Methods can be called without determining version support, it is handled internally.
 * Non-supported methods will simply delegate to supported methods.
 */
public interface SoundReceiver {

	boolean ADVENTURE_API = Skript.classExists("net.kyori.adventure.sound.Sound$Builder");
	boolean SPIGOT_SOUND_SEED = Skript.methodExists(Player.class, "playSound", Entity.class, Sound.class, SoundCategory.class, float.class, float.class, long.class);
	boolean ENTITY_EMITTER_SOUND = Skript.methodExists(Player.class, "playSound", Entity.class, Sound.class, SoundCategory.class, float.class, float.class);
	boolean ENTITY_EMITTER_STRING = Skript.methodExists(Player.class, "playSound", Entity.class, String.class, SoundCategory.class, float.class, float.class);

	void playSound(Location location, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed);
	void playSound(Entity entity, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed);

	static SoundReceiver of(Player player) { return new PlayerSoundReceiver(player); }
	static SoundReceiver of(World world) { return new WorldSoundReceiver(world); }
}
