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

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Time Since/Until")
@Description({
	"The time since a date has passed or the time until a date will pass. will return 0 seconds.",
	"This expression will return 0 seconds if the time since or time until would be negative, eg. if one tries to get the time since a future date."
})
@Examples({
	"send \"%time since 5 minecraft days ago% has passed since 5 minecraft days ago!\" to player",
	"send \"%time until {countdown::ends}% until the game begins!\" to player"
})
@Since("2.5, INSERT VERSION (time until)")
public class ExprTimeSince extends SimplePropertyExpression<Date, Timespan> {

	static {
		Skript.registerExpression(ExprTimeSince.class, Timespan.class, ExpressionType.PROPERTY,
				"[the] time since %dates%",
				"[the] (time [remaining]|remaining time) until %dates%");
	}

	private boolean isSince;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		isSince = matchedPattern == 0;
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}


	@Override
	@Nullable
	public Timespan convert(Date date) {
		Date now = Date.now();
		// Ensure that we have a valid date
		// Since should have a date in the past ( -1 or 0 )
		// Until should have a date in the future ( 0 or 1 )
		if (isSince ? (date.compareTo(now) < 1) : (date.compareTo(now) > -1))
			return date.difference(now);
		return new Timespan();
	}

	@Override
	public Class<? extends Timespan> getReturnType() {
		return Timespan.class;
	}

	@Override
	protected String getPropertyName() {
		return "time " + (isSince ? "since" : "until");
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "the time " + (isSince ? "since " : "until ") + getExpr().toString(event, debug);
	}

}
