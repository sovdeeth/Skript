package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.BlockingLogHandler;
import ch.njol.skript.log.LogHandler;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.iterator.CheckedIterator;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.reflect.Array;
import java.util.*;

@Name("Entities")
@Description("All entities in all worlds, in a specific world, in a chunk, in a radius around a certain location or within two locations. " +
		"e.g. <code>all players</code>, <code>all creepers in the player's world</code>, or <code>players in radius 100 of the player</code>.")
@Examples({"kill all creepers in the player's world",
		"send \"Psst!\" to all players within 100 meters of the player",
		"give a diamond to all ops",
		"heal all tamed wolves in radius 2000 around {town center}",
		"delete all monsters in chunk at player",
		"size of all players within {_corner::1} and {_corner::2}}"})
@Since("1.2.1, 2.5 (chunks), 2.10 (within)")
public class ExprEntities extends SimpleExpression<Entity> {

	static {
		Skript.registerExpression(ExprEntities.class, Entity.class, ExpressionType.PATTERN_MATCHES_EVERYTHING,
				"[(all [[of] the]|the)] %*entitydatas% [(in|of) (world[s] %-worlds%|1:%-worlds/chunks%)]",
				"[(all [[of] the]|the)] entities of type[s] %entitydatas% [(in|of) (world[s] %-worlds%|1:%-worlds/chunks%)]",
				"[(all [[of] the]|the)] %*entitydatas% (within|[with]in radius) %number% [(block[s]|met(er|re)[s])] (of|around) %location%",
				"[(all [[of] the]|the)] entities of type[s] %entitydatas% in radius %number% (of|around) %location%",
				"[(all [[of] the]|the)] %*entitydatas% within %location% and %location%",
				"[(all [[of] the]|the)] entities of type[s] %entitydatas% within %location% and %location%");
	}

	@SuppressWarnings("null")
	Expression<? extends EntityData<?>> types;

	@UnknownNullability
	private Expression<?> worldsOrChunks;
	@UnknownNullability
	private Expression<Number> radius;
	@UnknownNullability
	private Expression<Location> center;
	@UnknownNullability
	private Expression<Location> from;
	@UnknownNullability
	private Expression<Location> to;

	private Class<? extends Entity> returnType = Entity.class;
	private boolean isUsingRadius;
	private boolean isUsingCuboid;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		types = (Expression<? extends EntityData<?>>) exprs[0];
		if (matchedPattern % 2 == 0) {
			for (EntityData<?> entityType : ((Literal<EntityData<?>>) types).getAll()) {
				if (entityType.isPlural().isFalse() || entityType.isPlural().isUnknown() && !StringUtils.startsWithIgnoreCase(parseResult.expr, "all"))
					return false;
			}
		}
		isUsingRadius = matchedPattern == 2 || matchedPattern == 3;
		isUsingCuboid = matchedPattern >= 4;
		if (isUsingRadius) {
			radius = (Expression<Number>) exprs[1];
			center = (Expression<Location>) exprs[2];
		} else if (isUsingCuboid) {
			from = (Expression<Location>) exprs[1];
			to = (Expression<Location>) exprs[2];
		} else {
			if (parseResult.mark == 1) {
				worldsOrChunks = exprs[2];
			} else {
				worldsOrChunks = exprs[1];
			}
		}
		if (types instanceof Literal && ((Literal<EntityData<?>>) types).getAll().length == 1)
			returnType = ((Literal<EntityData<?>>) types).getSingle().getType();
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isLoopOf(String s) {
		if (!(types instanceof Literal<?>))
			return false;
		try (LogHandler ignored = new BlockingLogHandler().start()) {
			EntityData<?> entityData = EntityData.parseWithoutIndefiniteArticle(s);
			if (entityData != null) {
				for (EntityData<?> entityType : ((Literal<EntityData<?>>) types).getAll()) {
					assert entityType != null;
					if (!entityData.isSupertypeOf(entityType))
						return false;
				}
				return true;
			}
		}
		return false;
	}

	@Override
	@SuppressWarnings("null")
	protected Entity @Nullable [] get(Event event) {
		if (isUsingRadius || isUsingCuboid) {
			Iterator<? extends Entity> iter = iterator(event);
			if (iter == null || !iter.hasNext())
				return null;

			List<Entity> list = new ArrayList<>();
			while (iter.hasNext())
				list.add(iter.next());
			return list.toArray((Entity[]) Array.newInstance(returnType, list.size()));
		} else {
			EntityData<?>[] types = this.types.getAll(event);
			if (worldsOrChunks == null) {
				return EntityData.getAll(types, returnType, (World[]) null);
			}
			List<Chunk> chunks = new ArrayList<>();
			List<World> worlds = new ArrayList<>();
			for (Object obj : worldsOrChunks.getArray(event)) {
				if (obj instanceof Chunk chunk) {
					chunks.add(chunk);
				} else if (obj instanceof World world) {
					worlds.add(world);
				}
			}
			Set<Entity> entities = new HashSet<>();
			if (!chunks.isEmpty()) {
				entities.addAll(Arrays.asList(EntityData.getAll(types, returnType, chunks.toArray(new Chunk[0]))));
			}
			if (!worlds.isEmpty()) {
				entities.addAll(Arrays.asList(EntityData.getAll(types, returnType, worlds.toArray(new World[0]))));
			}
			return entities.toArray((Entity[]) Array.newInstance(returnType, entities.size()));
		}
	}

	@Override
	@Nullable
	@SuppressWarnings("null")
	public Iterator<? extends Entity> iterator(Event event) {
		if (isUsingRadius) {
			Location location = center.getSingle(event);
			if (location == null)
				return null;
			Number number = radius.getSingle(event);
			if (number == null)
				return null;
			double rad = number.doubleValue();

			if (location.getWorld() == null) // safety
				return null;

			Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, rad, rad, rad);
			double radiusSquared = rad * rad * Skript.EPSILON_MULT;
			EntityData<?>[] entityTypes = types.getAll(event);
			return new CheckedIterator<>(nearbyEntities.iterator(), entity -> {
					if (entity == null || entity.getLocation().distanceSquared(location) > radiusSquared)
						return false;
					for (EntityData<?> entityType : entityTypes) {
						if (entityType.isInstance(entity))
							return true;
					}
					return false;
				});
		} else if (isUsingCuboid) {
			Location corner1 = from.getSingle(event);
			if (corner1 == null)
				return null;
			Location corner2 = to.getSingle(event);
			if (corner2 == null)
				return null;
			EntityData<?>[] entityTypes = types.getAll(event);
			World world = corner1.getWorld();
			if (world == null)
				world = corner2.getWorld();
			if (world == null)
				return null;
			Collection<Entity> entities = corner1.getWorld().getNearbyEntities(BoundingBox.of(corner1, corner2));
			return new CheckedIterator<>(entities.iterator(), entity -> {
				if (entity == null)
					return false;
				for (EntityData<?> entityType : entityTypes) {
					if (entityType.isInstance(entity))
						return true;
				}
				return false;
			});
		} else {
			return super.iterator(event);
		}
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends Entity> getReturnType() {
		return returnType;
	}

	@Override
	@SuppressWarnings("null")
	public String toString(@Nullable Event event, boolean debug) {
		String message = "all entities of type " + types.toString(event, debug);
		if (worldsOrChunks != null)
			message += " in " + worldsOrChunks.toString(event, debug);
		else if (radius != null && center != null)
			message += " in radius " + radius.toString(event, debug) + " around " + center.toString(event, debug);
		else if (from != null && to != null)
			message += " within " + from.toString(event, debug) + " and " + to.toString(event, debug);
		return message;
	}

}
