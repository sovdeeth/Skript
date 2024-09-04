package ch.njol.skript.hooks;

import ch.njol.skript.Skript;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import org.bukkit.plugin.Plugin;

import java.io.IOException;

public class SparkHook extends Hook<Plugin> {

	public static String NO_SPARK_SUPPORT = "Your server does not have Spark installed.";

	public SparkHook() throws IOException {}

	public static Spark spark;

	@Override
	protected boolean init() {
		spark = SparkProvider.get();
		return spark != null;
	}

	@Override
	protected void loadClasses() throws IOException {
		if (spark != null) {
			Skript.getAddonInstance().loadClasses(getClass().getPackage().getName() + ".Spark");
		}
	}

	@Override
	public String getName() {
		return "Spark";
	}
}
