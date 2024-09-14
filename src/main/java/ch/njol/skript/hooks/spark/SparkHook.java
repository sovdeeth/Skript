package ch.njol.skript.hooks.spark;

import ch.njol.skript.Skript;
import ch.njol.skript.hooks.Hook;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import org.bukkit.plugin.Plugin;

import java.io.IOException;

public class SparkHook extends Hook<Plugin> {

	public SparkHook() throws IOException {}

	public static Spark spark;

	public static Spark getSparkInstance() {
		return SparkProvider.get();
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
