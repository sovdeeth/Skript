package ch.njol.skript.hooks.spark;

import ch.njol.skript.Skript;
import ch.njol.skript.hooks.Hook;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import org.bukkit.plugin.Plugin;

import java.io.IOException;

public class SparkHook extends Hook<Plugin> {

	public SparkHook() throws IOException {}

	public static Object spark;

	public static Object getSparkInstance() {
		if (Skript.classExists("me.lucko.spark.api.SparkProvider")) {
			return SparkProvider.get();
		}
		return null;
	}

	@Override
	protected boolean init() {
		spark = getSparkInstance();
		return spark != null;
	}

	@Override
	protected void loadClasses() throws IOException {
		Skript.getAddonInstance().loadClasses(getClass().getPackage().getName());
	}

	@Override
	public String getName() {
		return "Spark";
	}
}
