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

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class ExprEntityItemUseTime extends PropertyExpression<LivingEntity, Timespan> {

	static {
		register(ExprEntityItemUseTime.class, Timespan.class, "[elapsed|:remaining] (item|tool) use time", "livingentities");
	}

	private boolean remaining;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		setExpr((Expression<LivingEntity>) exprs[0]);
		remaining = parseResult.hasTag("remaining");
		return true;
	}

	@Override
	protected Timespan[] get(Event e, LivingEntity[] source) {
		Timespan[] timespans = new Timespan[source.length];
		if (remaining) {
			for (int i = 0; i < source.length; i++) {
				timespans[i] = Timespan.fromTicks(source[i].getItemUseRemainingTime());
			}
		} else {
			for (int i = 0; i < source.length; i++) {
				timespans[i] = Timespan.fromTicks(source[i].getHandRaisedTime());
			}
		}
		return timespans;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return (remaining ? "remaining" : "elapsed") + " time of " + getExpr().toString(event, debug) + "'s item use";
	}

	@Override
	public Class<? extends Timespan> getReturnType() {
		return Timespan.class;
	}

}
