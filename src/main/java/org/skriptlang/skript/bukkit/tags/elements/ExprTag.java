package org.skriptlang.skript.bukkit.tags.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagModule;
import org.skriptlang.skript.bukkit.tags.TagType;
import org.skriptlang.skript.bukkit.tags.sources.TagOrigin;

public class ExprTag extends SimpleExpression<Tag> {

	static {
		Skript.registerExpression(ExprTag.class, Tag.class, ExpressionType.COMBINED,
				TagOrigin.getFullPattern() + " " + TagType.getFullPattern() + " tag %string%");
	}

	Expression<String> name;
	int type;
	TagOrigin origin;
	boolean datapackOnly;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		//noinspection unchecked
		name = (Expression<String>) expressions[0];
		type = parseResult.mark - 1;
		origin = TagOrigin.fromParseTags(parseResult.tags);
		datapackOnly = origin == TagOrigin.BUKKIT && parseResult.hasTag("datapack");
		return true;
	}

	@Override
	protected Tag<?> @Nullable [] get(Event event) {
		String name = this.name.getSingle(event);
		if (name == null)
			return null;

		NamespacedKey key = NamespacedKey.fromString(name);
		if (key == null)
			return null;

		Tag<?> tag;
		TagType<?>[] types = TagType.getType(type);
		for (TagType<?> type : types) {
			tag = TagModule.TAGS.getTag(origin, type, key);
			if (tag != null
				// ensures that only datapack/minecraft tags are sent when specifically requested
				&& (origin != TagOrigin.BUKKIT || (datapackOnly ^ tag.getKey().getNamespace().equals("minecraft")))
			) {
				return new Tag[]{tag};
			}
		}

		return null;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Tag> getReturnType() {
		return Tag.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String registry = type == -1 ? "" : TagType.getType(type)[0].pattern();
		return origin.toString(datapackOnly) + registry + "tag " + name.toString(event, debug);
	}

}
