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

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("Respawn location")
@Description("The location that a player should respawn at. This is used within the respawn event.")
@Examples({"on respawn:",
	"	set respawn location to {example::spawn}"})
@Since("2.2-dev35")
public class ExprRespawnLocation extends SimpleExpression<Location> {

	static {
		Skript.registerExpression(ExprRespawnLocation.class, Location.class, ExpressionType.SIMPLE, "[the] respawn location");
	}
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!getParser().isCurrentEvent(PlayerRespawnEvent.class)) {
			Skript.error("The expression 'respawn location' may only be used in the respawn event", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		return true;
	}
	
	@Override
	@Nullable
	protected Location[] get(Event event) {
		if (!(event instanceof PlayerRespawnEvent))
			return null;

		return CollectionUtils.array(((PlayerRespawnEvent)event).getRespawnLocation());
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Location> getReturnType() {
		return Location.class;
	}
	
	@Override
	public String toString(final @Nullable Event event, final boolean debug) {
		return "the respawn location " + ((event != null) ? ": " + ((PlayerRespawnEvent)event).getRespawnLocation() : "");
	}
	
	@Nullable
	@Override
	public Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == ChangeMode.SET)
			return CollectionUtils.array(Location.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		if (!(event instanceof PlayerRespawnEvent))
			return;

		if (delta != null) ((PlayerRespawnEvent)event).setRespawnLocation((Location)delta[0]);
	}
	
}
