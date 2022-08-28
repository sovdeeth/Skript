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
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.sections.SecLoop;
import ch.njol.skript.sections.SecWhile;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@Name("Continue")
@Description("Skips the value currently being looped, moving on to the next value if it exists.")
@Examples({"loop all players:",
		"\tif loop-value does not have permission \"moderator\":",
		"\t\tcontinue # filter out non moderators",
		"\tbroadcast \"%loop-player% is a moderator!\" # Only moderators get broadcast"})
@Since("2.2-dev37, INSERT VERSION (while loops)")
public class EffContinue extends Effect {

	static {
		Skript.registerEffect(EffContinue.class, "continue [loop]");
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	private TriggerSection section;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		List<TriggerSection> currentSections = ParserInstance.get().getCurrentSections().stream()
			.filter(s -> s instanceof SecLoop || s instanceof SecWhile)
			.collect(Collectors.toList());
		
		if (currentSections.isEmpty()) {
			Skript.error("Continue may only be used in while or loops");
			return false;
		}
		
		section = currentSections.get(currentSections.size() - 1);
		return true;
	}

	@Override
	protected void execute(Event e) {
		throw new UnsupportedOperationException();
	}

	@Nullable
	@Override
	protected TriggerItem walk(Event e) {
		return section;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "continue";
	}

}
