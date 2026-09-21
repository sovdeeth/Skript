package org.skriptlang.skript.bukkit.raytrace.elements.expressions;

import ch.njol.skript.SkriptConfig;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.util.Direction;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.raytrace.RaytraceConfig;
import org.skriptlang.skript.bukkit.raytrace.RaytraceSectionEvent;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.List;
import java.util.Objects;

@Name("Raytrace")
@Description("""
	Casts a ray from a location or an entity, returning what it hits.
	When an entity is used, the ray starts at its head, and that entity can never be hit by its own ray.
	The direction is optional. When it is left out, the yaw and pitch of the starting location or entity is used, \
	so a raytrace from a player travels wherever they are looking.
	When casting from one location to another, the ray stops at the second location.
	
	The section allows the use of 'ignore' effects to filter what the ray is allowed to hit.
	""")
@Example("""
	# travels wherever the player is looking, and can't hit the player
	set {_hit} to the results of a raytrace from player for 20 meters
	""")
@Example("""
	set {_hit} to the results of a raytrace from player's eye location along player's facing for 20 meters
	""")
@Example("""
	set {_hit} to the results of a raytrace from {_start} to {_end}:
		use a ray of size 0.5
		ignore passable blocks
		ignore flowing fluids
	
		if {_friendly} is true:
			ignore all entities
	
		ignore entities if any:
			event-entity is a player
			event-entity is tamed
	""")
@Since("INSERT VERSION")
public class ExprSecRaytrace extends SectionExpression<RayTraceResult> {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.EXPRESSION,
			SyntaxInfo.Expression.builder(ExprSecRaytrace.class, RayTraceResult.class)
				.supplier(ExprSecRaytrace::new)
				.addPatterns(
					"[[the] results of] [a] ray[ ](trace|cast) (starting at|[starting] from) %entity/location% [(facing|along) %-vector/direction%] [for %-number% (blocks|meters)]",
					"[[the] results of] [a] ray[ ](trace|cast) from %location% to %location%")
				.build()
		);
	}

	private Expression<?> start;
	private @Nullable Expression<?> direction; // when null, the direction of the starting location/entity is used
	private @Nullable Expression<Location> end;
	private Expression<Number> maxDistance = new SimpleLiteral<>(SkriptConfig.maxTargetBlockDistance.value().doubleValue());

	private @Nullable Trigger trigger;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, ParseResult result,
						@Nullable SectionNode node, @Nullable List<TriggerItem> list) {

		start = expressions[0];
		if (pattern == 0) {
			direction = expressions[1];
			if (expressions[2] != null)
				maxDistance = (Expression<Number>) expressions[2];
		} else {
			end = (Expression<Location>) expressions[1];
		}

		if (node != null) {
			trigger = SectionUtils.loadLinkedCode("raytrace", (beforeLoading, afterLoading)
				-> loadCode(node, "raytrace", beforeLoading, afterLoading, RaytraceSectionEvent.class));
			return trigger != null;
		}

		return true;
	}

	@Override
	protected RayTraceResult @Nullable [] get(Event event) {
		Object startObject = this.start.getSingle(event);
		if (startObject == null)
			return null;

		// a ray cast from an entity starts at its head, and may never hit that entity
		Entity startEntity = null;
		Location startLocation;
		if (startObject instanceof Entity entity) {
			startEntity = entity;
			startLocation = entity instanceof LivingEntity livingEntity
				? livingEntity.getEyeLocation()
				: entity.getLocation();
		} else if (startObject instanceof Location location) {
			startLocation = location;
		} else {
			return null;
		}

		Vector directionVector;
		double distance;
		if (end != null) {
			// the ray travels to the end location, so it determines both the direction and the distance
			Location endLocation = this.end.getSingle(event);
			if (endLocation == null)
				return null;
			if (!Objects.equals(startLocation.getWorld(), endLocation.getWorld())) {
				error("A raytrace can't be cast between two different worlds.");
				return null;
			}
			directionVector = endLocation.toVector().subtract(startLocation.toVector());
			distance = directionVector.length();
			if (distance == 0) {
				error("A raytrace can't start and end at the same location, as it has no direction to travel in.");
				return null;
			}
		} else {
			directionVector = getDirection(event, startLocation);
			Number maxDistance = this.maxDistance.getSingle(event);
			if (directionVector == null || maxDistance == null)
				return null;
			distance = maxDistance.doubleValue();
		}

		RaytraceConfig config = new RaytraceConfig();
		config.excludedEntity = startEntity;
		if (trigger != null) {
			RaytraceSectionEvent sectionEvent = new RaytraceSectionEvent(config);
			Variables.withLocalVariables(event, sectionEvent, () -> TriggerItem.walk(trigger, sectionEvent));
		}

		if (!config.canHitAnything()) {
			error("This raytrace ignores both entities and blocks, thereby being unable to hit anything. "
				+ "Please ensure it can hit something.");
			return null;
		}

		// perform the raytrace
		World world = startLocation.getWorld();
		if (world == null) {
			error("A raytrace can't be cast from a location that is not in a world.");
			return null;
		}

		RayTraceResult result = world.rayTrace(config.asBuilder(startLocation, directionVector, distance));
		if (result != null)
			return new RayTraceResult[]{result};
		return new RayTraceResult[0];
	}

	/**
	 * Resolves the direction the ray should travel in.
	 * A {@link Direction} is resolved against {@code startLocation}, as it may be relative to it.
	 *
	 * @param startLocation The location the ray starts at, whose yaw and pitch is used when no direction was provided.
	 * @return The direction of the ray, or null if it could not be resolved.
	 */
	private @Nullable Vector getDirection(Event event, Location startLocation) {
		if (direction == null)
			return startLocation.getDirection();
		Object directionObject = direction.getSingle(event);
		if (directionObject instanceof Direction dir)
			return dir.getDirection(startLocation);
		if (directionObject instanceof Vector vector)
			return vector;
		return null;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<RayTraceResult> getReturnType() {
		return RayTraceResult.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String result = "the results of a raytrace from " + start.toString(event, debug);
		if (end != null)
			return result + " to " + end.toString(event, debug);
		if (direction != null)
			result += " along " + direction.toString(event, debug);
		return result + " for " + maxDistance.toString(event, debug);
	}

}
