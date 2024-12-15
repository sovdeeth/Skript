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

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;

@Name("Entity Owner")
@Description("The owner of a tameable entity, such as a horse or wolf.")
@Examples({"set owner of target entity to player",
	"delete owner of target entity",
	"set {_t} to uuid of tamer of target entity"})
@Since("2.5")
public class ExprEntityTamer extends SimplePropertyExpression<LivingEntity, OfflinePlayer> {
	
	static {
		register(ExprEntityTamer.class, OfflinePlayer.class, "(owner|tamer)", "livingentities");
	}
	
	@Nullable
	@Override
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.SET || mode == ChangeMode.DELETE || mode == ChangeMode.RESET)
			return CollectionUtils.array(OfflinePlayer.class);
		return null;
	}
	
	@Nullable
	@Override
	public OfflinePlayer convert(LivingEntity entity) {
		if (entity instanceof Tameable) {
			Tameable t = ((Tameable) entity);
			if (t.isTamed()) {
				return ((OfflinePlayer) t.getOwner());
			}
		}
		return null;
	}
	
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		OfflinePlayer player = delta == null ? null : ((OfflinePlayer) delta[0]);
		switch (mode) {
			case SET:
				for (LivingEntity entity : getExpr().getAll(e)) {
					if (!(entity instanceof Tameable))
						continue;
					((Tameable) entity).setOwner(player);
				}
				break;
			case DELETE:
			case RESET:
				for (LivingEntity entity : getExpr().getAll(e)) {
					if (!(entity instanceof Tameable))
						continue;
					((Tameable) entity).setOwner(null);
				}
		}
	}
	
	@Override
	public Class<? extends OfflinePlayer> getReturnType() {
		return OfflinePlayer.class;
	}
	
	@Override
	protected String getPropertyName() {
		return "entity owner";
	}
	
	@Override
	public String toString(@Nullable Event e, boolean d) {
		return "owner of " + getExpr().toString(e, d);
	}
	
}
