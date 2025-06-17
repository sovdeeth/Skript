package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.vector.FastVector;

/**
 * @author Sashie
 */
@Name("Vectors - Velocity")
@Description("Gets or changes velocity of an entity.")
@Examples({"set player's velocity to {_v}"})
@Since("2.2-dev31")
public class ExprVelocity extends SimplePropertyExpression<Entity, FastVector> {

	static {
		register(ExprVelocity.class, FastVector.class, "velocit(y|ies)", "entities");
	}

	@Override
	@Nullable
	public FastVector convert(Entity e) {
		return FastVector.asFastVector(e.getVelocity());
	}

	@Override
	@Nullable
	@SuppressWarnings("null")
	public Class<?>[] acceptChange(ChangeMode mode) {
		if ((mode == ChangeMode.ADD || mode == ChangeMode.REMOVE || mode == ChangeMode.SET || mode == ChangeMode.DELETE || mode == ChangeMode.RESET))
			return CollectionUtils.array(FastVector.class);
		return null;
	}

	@Override
	@SuppressWarnings("null")
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		assert mode == ChangeMode.DELETE || mode == ChangeMode.RESET || delta != null;
		for (final Entity entity : getExpr().getArray(e)) {
			if (entity == null)
				return;
			switch (mode) {
				case ADD:
					entity.setVelocity(entity.getVelocity().add((Vector) delta[0]));
					break;
				case REMOVE:
					entity.setVelocity(entity.getVelocity().subtract((Vector) delta[0]));
					break;
				case REMOVE_ALL:
					break;
				case DELETE:
				case RESET:
					entity.setVelocity(new FastVector());
					break;	
				case SET:
					entity.setVelocity((Vector) delta[0]);
			}
		}
	}

	@Override
	protected String getPropertyName() {
		return "velocity";
	}

	@Override
	public Class<FastVector> getReturnType() {
		return FastVector.class;
	}

}
