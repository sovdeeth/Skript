package org.skriptlang.skript.bukkit.raytrace.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.raytrace.RaytraceSectionEvent;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Raytrace - Ray Size")
@Description("""
	Sets the size of the ray used by the raytrace this section belongs to.
	The size is the radius, in blocks, of the ray. A size of 0, the default, is a ray with no thickness.
	Can only be used within a raytrace section.
	""")
@Example("""
	set {_hit} to the results of a raytrace from {_start} along {_direction}:
		use a ray of size 0.5
	""")
@Since("INSERT VERSION")
public class EffRaytraceRaySize extends Effect implements EventRestrictedSyntax {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.EFFECT,
			SyntaxInfo.builder(EffRaytraceRaySize.class)
				.supplier(EffRaytraceRaySize::new)
				.addPattern("use [a] ray ([of|with] size|size of) %number% [blocks|meters]")
				.build()
		);
	}

	private Expression<Number> raySize;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		raySize = (Expression<Number>) expressions[0];
		return true;
	}

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return CollectionUtils.array(RaytraceSectionEvent.class);
	}

	@Override
	protected void execute(Event event) {
		if (!(event instanceof RaytraceSectionEvent sectionEvent))
			return;
		Number raySize = this.raySize.getSingle(event);
		if (raySize == null) {
			error("The ray size was not set, as it could not be resolved.");
			return;
		}
		double size = raySize.doubleValue();
		if (size < 0) {
			error("The size of a ray cannot be negative, but was " + size + ".");
			return;
		}
		sectionEvent.getConfig().raySize = size;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "use a ray of size " + raySize.toString(event, debug);
	}

}
