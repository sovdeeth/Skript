package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Last Damage")
@Description("The last damage that was done to an entity. Note that changing it doesn't deal more/less damage.")
@Examples({"set last damage of event-entity to 2"})
@Since("2.5.1")
public class ExprLastDamage extends SimplePropertyExpression<LivingEntity, Number> {

	static {
		register(ExprLastDamage.class, Number.class, "last damage", "livingentities");
	}

	@Nullable
	@Override
	@SuppressWarnings("null")
	public Number convert(LivingEntity livingEntity) {
		return livingEntity.getLastDamage() / 2;
	}

	@Nullable
	@Override
	public Class<?>[] acceptChange(ChangeMode mode) {
		switch (mode) {
			case ADD:
			case SET:
			case REMOVE:
				return CollectionUtils.array(Number.class);
			default:
				return null;
		}
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		if (delta != null && delta[0] instanceof Number) {
			double damage = ((Number) delta[0]).doubleValue() * 2;

			switch (mode) {
				case SET:
					for (LivingEntity entity : getExpr().getArray(e))
						entity.setLastDamage(damage);
					break;
				case REMOVE:
					damage = damage * -1;
				case ADD:
					for (LivingEntity entity : getExpr().getArray(e))
						entity.setLastDamage(damage + entity.getLastDamage());
					break;
				default:
					assert false;
			}
		}
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	protected String getPropertyName() {
		return "last damage";
	}

}
