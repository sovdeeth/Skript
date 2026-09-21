package org.skriptlang.skript.bukkit.raytrace;

import io.papermc.paper.raytracing.PositionedRayTraceConfigurationBuilder;
import io.papermc.paper.raytracing.RayTraceTarget;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.condition.Conditional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The mutable configuration of a single raytrace execution.
 * @see RaytraceSectionEvent
 */
public class RaytraceConfig {

	public enum BlockCollisionMode {
		/**
		 * Ignore blocks.
		 */
		NEVER,
		/**
		 * Only collide with impassable blocks.
		 */
		IGNORE_PASSABLES,
		/**
		 * Collide with all blocks.
		 */
		ALWAYS
	}

	public enum EntityCollisionMode {
		/**
		 * Ignore entities.
		 */
		NEVER,
		/**
		 * Collide with all entities.
		 */
		ALWAYS
	}

	public double raySize = 0.0;

	public FluidCollisionMode fluidCollisionMode = FluidCollisionMode.ALWAYS;
	public BlockCollisionMode blockCollisionMode = BlockCollisionMode.ALWAYS;
	public EntityCollisionMode entityCollisionMode = EntityCollisionMode.ALWAYS;

	/**
	 * An entity the ray may never hit, being the entity the ray was cast from.
	 * This mirrors the behaviour of {@link org.bukkit.entity.LivingEntity#rayTraceEntities(int)}, which never
	 * returns the entity the ray originates from.
	 */
	public @Nullable Entity excludedEntity;

	public final List<Conditional<Event>> entityFilters = new ArrayList<>();
	public final List<Conditional<Event>> blockFilters = new ArrayList<>();

	/**
	 * @return Whether this configuration allows the raytrace to hit anything at all.
	 */
	public boolean canHitAnything() {
		return blockCollisionMode != BlockCollisionMode.NEVER || entityCollisionMode != EntityCollisionMode.NEVER;
	}

	/**
	 * Builds a consumer applying this configuration to a raytrace builder.
	 * @see org.bukkit.World#rayTrace(Consumer)
	 *
	 * @param start The location the ray starts at.
	 * @param direction The direction the ray travels in.
	 * @param maxDistance The maximum distance the ray travels.
	 */
	public Consumer<PositionedRayTraceConfigurationBuilder> asBuilder(Location start, Vector direction, double maxDistance) {
		return builder -> {
			// set simple info
			builder.start(start)
				.direction(direction)
				.maxDistance(maxDistance)
				.raySize(raySize)
				.ignorePassableBlocks(blockCollisionMode == BlockCollisionMode.IGNORE_PASSABLES)
				.fluidCollisionMode(fluidCollisionMode);

			// set targets
			if (blockCollisionMode != BlockCollisionMode.NEVER) {
				if (entityCollisionMode != EntityCollisionMode.NEVER) {
					builder.targets(RayTraceTarget.BLOCK, RayTraceTarget.ENTITY);
				} else {
					builder.targets(RayTraceTarget.BLOCK);
				}
			} else if (entityCollisionMode != EntityCollisionMode.NEVER) {
				builder.targets(RayTraceTarget.ENTITY);
			}

			// set filters
			Conditional<Event> entityIgnoreIf = entityFilters.isEmpty() ? null
				: Conditional.compound(Conditional.Operator.OR, entityFilters);
			if (excludedEntity != null || entityIgnoreIf != null) {
				builder.entityFilter(entity -> {
					// the entity the ray was cast from can never be hit
					if (entity.equals(excludedEntity))
						return false;
					return entityIgnoreIf == null || !entityIgnoreIf.evaluate(new RaytraceEntityEvent(entity)).isTrue();
				});
			}
			if (!blockFilters.isEmpty()) {
				Conditional<Event> blockIgnoreIf = Conditional.compound(Conditional.Operator.OR, blockFilters);
				builder.blockFilter(block -> !blockIgnoreIf.evaluate(new RaytraceBlockEvent(block)).isTrue());
			}
		};
	}

	@Override
	public String toString() {
		return "RaytraceConfig{" +
			"raySize=" + raySize +
			", fluidCollisionMode=" + fluidCollisionMode +
			", blockCollisionMode=" + blockCollisionMode +
			", entityCollisionMode=" + entityCollisionMode +
			", excludedEntity=" + excludedEntity +
			", entityFilters=" + entityFilters +
			", blockFilters=" + blockFilters +
			'}';
	}

}
