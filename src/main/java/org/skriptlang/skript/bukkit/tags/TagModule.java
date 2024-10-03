package org.skriptlang.skript.bukkit.tags;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import org.bukkit.Tag;

import java.io.IOException;

public class TagModule {
	public static void load() throws IOException {
		// abort if no class exists
		if (!Skript.classExists("org.bukkit.Tag"))
			return;

		// load classes (todo: replace with registering methods after regitration api
		Skript.getAddonInstance().loadClasses("org.skriptlang.skript.bukkit", "tags");

		// Classes
		Classes.registerClass(new ClassInfo<>(Tag.class, "minecrafttag")
			.user("minecraft ?tags?")
			.name("Minecraft Tag")
			.description("A tag that classifies a material, or entity.")
			.since("INSERT VERSION")
			.parser(new Parser<Tag<?>>() {
				@Override
				public boolean canParse(ParseContext context) {
					return false;
				}

				@Override
				public String toString(Tag<?> tag, int flags) {
					return "minecraft tag \"" + tag.getKey() + "\"";
				}

				@Override
				public String toVariableNameString(Tag<?> tag) {
					return "minecraft tag: " + tag.getKey();
				}
			}));
	}

}
