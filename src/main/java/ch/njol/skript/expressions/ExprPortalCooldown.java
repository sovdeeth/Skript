/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.util.WeatherType;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Portal Cooldown")
@Description("The amount of time before an entity can use a portal. By default, this is 15 seconds after exiting a nether portal.")
@Examples({
	"on portal:",
	"\twait 1 tick",
	"\tset portal cooldown of event-entity to 5 seconds"
})
@Since("INSERT VERSION")
public class ExprPortalCooldown extends SimplePropertyExpression<Entity, Timespan> {

	static {
		register(ExprPortalCooldown.class, Timespan.class, "portal cool[-| ]down", "entities");
	}

	@Override
	@Nullable
	public Timespan convert(Entity entity) {
		return Timespan.fromTicks(entity.getPortalCooldown());
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(Changer.ChangeMode mode) {
		switch (mode) {
			case SET:
			case ADD:
			case RESET:
			case DELETE:
			case REMOVE:
				return CollectionUtils.array(Timespan.class);
		}
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		Entity[] entities = getExpr().getArray(event);
		int change = delta == null ? 0 : (int) ((Timespan) delta[0]).getTicks();
		switch (mode) {
			case REMOVE:
				change = -change;
			case ADD:
				for (Entity entity : entities) {
					entity.setPortalCooldown(Math.max(entity.getPortalCooldown() + change, 0));
				}
				break;
			case DELETE:
			case RESET:
			case SET:
				for (Entity entity : entities) {
					entity.setPortalCooldown(Math.max(change, 0));
				}
				break;
			default:
				assert false;
		}
	}

	@Override
	public Class<? extends Timespan> getReturnType() {
		return Timespan.class;
	}

	@Override
	protected String getPropertyName() {
		return "portal cooldown";
	}

}
