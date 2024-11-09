package ch.njol.skript.bukkitutil;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public class SoundUtils {

	private static final boolean SOUND_IS_INTERFACE;
	private static final Method SOUND_GET_KEY;

	static {
		try {
			Class<?> SOUND_CLASS = Class.forName("org.bukkit.Sound");
			SOUND_IS_INTERFACE = SOUND_CLASS.isInterface();
			SOUND_GET_KEY = SOUND_CLASS.getDeclaredMethod("getKey");
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the key of a sound, given its enum-name-style name.
	 * @param soundString The enum name to use to find the sound.
	 * @return The key of the sound.
	 */
	public static @Nullable NamespacedKey getKey(String soundString) {
		soundString = soundString.toUpperCase(Locale.ENGLISH);
		// Sound.class is an Interface (rather than an enum) as of MC 1.21.3
		if (SOUND_IS_INTERFACE) {
			try {
				//noinspection deprecation
				return Sound.valueOf(soundString).getKey();
			} catch (IllegalArgumentException ignore) {
			}
		} else {
			try {
				//noinspection unchecked,rawtypes
				Enum soundEnum = Enum.valueOf((Class) Sound.class, soundString);
				return ((NamespacedKey) SOUND_GET_KEY.invoke(soundEnum));
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignore) {
			}
		}
		return null;
	}

	/**
	 * returns the key string for a sound. For version compat.
	 * @param sound The sound to get the key string of.
	 * @return The key string of the {@link NamespacedKey} of the sound.
	 */
	public static @NotNull NamespacedKey getKey(Sound sound) {
		if (SOUND_IS_INTERFACE) {
			//noinspection deprecation
			return sound.getKey();
		} else {
			try {
				return ((NamespacedKey) SOUND_GET_KEY.invoke(sound));
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
