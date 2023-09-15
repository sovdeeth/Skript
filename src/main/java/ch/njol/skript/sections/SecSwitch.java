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
package ch.njol.skript.sections;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import com.google.common.collect.Iterables;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecSwitch extends Section {

	static {
		Skript.registerSection(SecSwitch.class, "switch %~object%");
		Skript.registerSection(SecSwitchCase.class, "case %*object%");
	}

	Map<Object, SecSwitchCase> cases = new HashMap<>();
	Expression<?> input;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {

		input = LiteralUtils.defendExpression(expressions[0]);

		ParserInstance parser = getParser();
		int nonEmptyNodeCount = Iterables.size(sectionNode);
		if (nonEmptyNodeCount < 2) {
			Skript.error("Switch sections must contain at least two conditions");
			return false;
		}

		List<TriggerSection> sections = parser.getCurrentSections();
		sections.add(this);
		parser.setCurrentSections(sections);
		Kleenean hasDelayAfter = isDelayed;
		for (Node childNode : sectionNode) {
			if (!(childNode instanceof SectionNode)) {
				Skript.error("Switch sections may only contain case sections");
				return false;
			}
			String childKey = childNode.getKey();
			if (childKey != null) {
				childKey = ScriptLoader.replaceOptions(childKey);

				parser.setNode(childNode);
				parser.setHasDelayBefore(isDelayed);
				Section section = Section.parse(childKey, "Can't understand this condition: '" + childKey + "'", (SectionNode) childNode, new ArrayList<>());
				// if this condition was invalid, don't bother parsing the rest
				if (!(section instanceof SecSwitchCase)) {
					Skript.error("Switch sections may only contain case sections");
					return false;
				}
				cases.put(((SecSwitchCase) section).getValue(), (SecSwitchCase) section);
				if (!parser.getHasDelayBefore().isTrue()) {
					hasDelayAfter = parser.getHasDelayBefore();
				}
				parser.setHasDelayBefore(isDelayed);
			}
		}
		parser.setNode(sectionNode);
		parser.setHasDelayBefore(hasDelayAfter);
		return LiteralUtils.canInitSafely(input);
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		Object input = this.input.getSingle(event);
		if (input == null) {
			return null;
		}
		Section toRun = cases.get(input);
		setTriggerItems(toRun == null ? new ArrayList<>() : Collections.singletonList(toRun));
		return walk(event, true);
	}


	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "switch " + input.toString(event, debug);
	}

	public static class SecSwitchCase extends Section {
		private Literal<?> value;

		@Override
		public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {

			if (!getParser().isCurrentSection(SecSwitch.class)) {
				Skript.error("Case sections must be inside a switch section");
				return false;
			}

			value = (Literal<?>) LiteralUtils.defendExpression(expressions[0]);
			loadOptionalCode(sectionNode);
			return LiteralUtils.canInitSafely(value);
		}

		@Override
		protected @Nullable TriggerItem walk(Event event) {
			return walk(event, true);
		}

		public Object getValue() {
			return value.getSingle();
		}

		@Override
		public String toString(@Nullable Event event, boolean debug) {
			return "case " + value.toString(event, debug);
		}
	}
}
