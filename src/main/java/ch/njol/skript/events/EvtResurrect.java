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
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.eclipse.jdt.annotation.Nullable;

public class EvtResurrect extends SkriptEvent {
	static {
		Skript.registerEvent("Resurrect Attempt", EvtResurrect.class, EntityResurrectEvent.class, "[entity] resurrect[ion] [attempt]")
			.description("Called when an entity dies, always. If they are not holding a totem, the event will be cancelled - you can, however, uncancel it.")
			.examples(
				"on resurrect attempt:",
					"\tentity is player",
					"\tentity has permission \"admin.undying\"",
					"\tuncancel the event"
			)
			.since("2.2-dev28");
	}

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
		// if the user hasn't specified a listening behavior, we'll default to ANY
		if (listeningBehavior == null)
			listeningBehavior = ListeningBehavior.ANY;
		return true;
	}

	@Override
	public boolean check(Event event) {
		return true;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "resurrect attempt";
	}

}
