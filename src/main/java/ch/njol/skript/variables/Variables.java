package ch.njol.skript.variables;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.ConfigurationSerializer;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import ch.njol.yggdrasil.Yggdrasil;

/**
 * Static support code for variable system.
 */
public class Variables {
	
	/**
	 * Gets scope that refers to all global variables.
	 * @return Global variables.
	 */
	public static GlobalVariableScope getGlobalVariables() {
		throw new UnsupportedOperationException("not implemented yet");
	}
	
	// Yggdrasil variable deserialization
	
	public final static short YGGDRASIL_VERSION = 1;
	
	public final static Yggdrasil yggdrasil = new Yggdrasil(YGGDRASIL_VERSION);

	public static boolean caseInsensitiveVariables = true;
	
	private final static String configurationSerializablePrefix = "ConfigurationSerializable_";
	static {
		yggdrasil.registerSingleClass(Kleenean.class, "Kleenean");
		yggdrasil.registerClassResolver(new ConfigurationSerializer<ConfigurationSerializable>() {
			{
				init(); // separate method for the annotation
			}
			
			@SuppressWarnings("unchecked")
			private final void init() {
				// used by asserts
				info = (ClassInfo<? extends ConfigurationSerializable>) Classes.getExactClassInfo(Object.class);
			}
			
			@SuppressWarnings({"unchecked"})
			@Override
			@Nullable
			public String getID(final @NonNull Class<?> c) {
				if (ConfigurationSerializable.class.isAssignableFrom(c) && Classes.getSuperClassInfo(c) == Classes.getExactClassInfo(Object.class))
					return configurationSerializablePrefix + ConfigurationSerialization.getAlias((Class<? extends ConfigurationSerializable>) c);
				return null;
			}
			
			@Override
			@Nullable
			public Class<? extends ConfigurationSerializable> getClass(final @NonNull String id) {
				if (id.startsWith(configurationSerializablePrefix)) {
					String classId = id.substring(configurationSerializablePrefix.length());
					assert classId != null;
					return ConfigurationSerialization.getClassByAlias(classId);
				}
				return null;
			}
		});
	}
}
