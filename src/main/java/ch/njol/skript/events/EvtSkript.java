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
import ch.njol.skript.events.bukkit.SkriptStartEvent;
import ch.njol.skript.events.bukkit.SkriptStopEvent;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EvtSkript extends SkriptEvent {

	static {
		Skript.registerEvent("Server Start/Stop", EvtSkript.class, CollectionUtils.array(SkriptStartEvent.class, SkriptStopEvent.class),
				"(:server|skript) (start|load|enable)", "(:server|skript) (stop|unload|disable)"
			)
			.description("Called when the server starts or stops (actually, when Skript starts or stops, so a /reload will trigger these events as well).")
			.examples("on skript start:", "on server stop:")
			.since("2.0");
	}

	private static final List<Trigger> START = Collections.synchronizedList(new ArrayList<>());
	private static final List<Trigger> STOP = Collections.synchronizedList(new ArrayList<>());

	public static void onSkriptStart() {
		Event event = new SkriptStartEvent();
		synchronized (START) {
			for (Trigger trigger : START)
				trigger.execute(event);
			START.clear();
		}
	}

	public static void onSkriptStop() {
		Event event = new SkriptStopEvent();
		synchronized (STOP) {
			for (Trigger trigger : STOP)
				trigger.execute(event);
			STOP.clear();
		}
	}
	
	private boolean isStart;
	
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
		isStart = matchedPattern == 0;
		if (parseResult.hasTag("server"))
			Skript.warning(
				"Server start/stop events are actually called when Skript is started or stopped." +
				"It is thus recommended to use 'on Skript start/stop' instead."
			);
		return true;
	}

	@Override
	public boolean postLoad() {
		(isStart ? START : STOP).add(trigger);
		return true;
	}

	@Override
	public void unload() {
		(isStart ? START : STOP).remove(trigger);
	}

	@Override
	public boolean check(Event event) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEventPrioritySupported() {
		return false;
	}
	
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "on skript " + (isStart ? "start" : "stop");
	}
	
}
