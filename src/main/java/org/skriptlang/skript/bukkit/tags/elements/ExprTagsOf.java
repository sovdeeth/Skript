package org.skriptlang.skript.bukkit.tags.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.Keyed;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagModule;
import org.skriptlang.skript.bukkit.tags.TagType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

public class ExprTagsOf extends PropertyExpression<Object, Tag> {

	static {
		Skript.registerExpression(ExprTagsOf.class, Tag.class, ExpressionType.PROPERTY,
				"[all [[of] the]] [minecraft] " + TagType.getFullPattern() + " tags of %itemtype/entity/entitydata%",
				"%itemtype/entity/entitydata%'[s] [minecraft] tags");
	}

	int type;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		this.setExpr(expressions[0]);
		type = parseResult.mark - 1;
		return true;
	}

	@Override
	protected Tag<?> @Nullable [] get(Event event, Object @NotNull [] source) {
		if (source.length == 0)
			return null;
		boolean isAny = (source[0] instanceof ItemType itemType && !itemType.isAll());
		Keyed[] values = TagModule.getKeyed(source[0]);
		if (values == null)
			return null;
		// choose single material if it's something like `any log`
		if (isAny) {
			ThreadLocalRandom random = ThreadLocalRandom.current();
			values = new Keyed[]{values[random.nextInt(0, values.length)]};
		}

		Set<Tag<?>> tags = new TreeSet<>(Comparator.comparing(Keyed::key));
		for (Keyed value : values) {
			tags.addAll(getTags(value));
		}

		return tags.toArray(new Tag[0]);
	}

	public <T extends Keyed> Collection<Tag<T>> getTags(T value) {
		List<Tag<T>> tags = new ArrayList<>();
		//noinspection unchecked
		Class<T> clazz = (Class<T>) value.getClass();
		for (Tag<T> tag : TagModule.TAGS.getTags(clazz)) {
			if (tag.isTagged(value))
				tags.add(tag);
		}
		return tags;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<Tag> getReturnType() {
		return Tag.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String registry = type == -1 ? "" : TagType.getType(type)[0].pattern();
		//noinspection DataFlowIssue
		return "minecraft " + registry + "tags of " + getExpr().toString(event, debug);
	}
}
