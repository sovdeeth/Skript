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
package ch.njol.skript.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Debuggable;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SelfRegisteringSkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ConsumingIterator;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Iterator;

/**
 * A {@link Structure} should be used instead of an event, in the following scenarios:
 * <ul>
 *     <li>
 *         The {@link SectionNode} does not just contain code
 *         <ul>
 *             <li>
 *                 For example, custom commands can contain other things, such as a permission or cooldown message.
 *                 Options are also applicable here, since they don't contain code at all.
 *             </li>
 *         </ul>
 *     </li>
 *     <li>
 *         The section should be (partially) read before <i>any</i> other code is loaded.
 *         In this case, a {@link PreloadingStructure} should be used.
 *         <ul>
 *             <li>
 *                 Functions use this to make sure they can be called from even before their definition in the script file.
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @see Skript#registerStructure(Class, String...)
 */
public abstract class Structure implements SyntaxElement, Debuggable {

	@Override
	public final boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		StructureData structureData = getParser().getData(StructureData.class);
		return init(exprs, matchedPattern, isDelayed, parseResult, structureData.sectionNode);
	}

	/**
	 * This method is the same as {@link SyntaxElement#init(Expression[], int, Kleenean, ParseResult)}, except
	 * the structure's {@link SectionNode} is also passed to this method.
	 */
	public abstract boolean init(Expression<?>[] exprs,
								 int matchedPattern,
								 Kleenean isDelayed,
								 ParseResult parseResult,
								 SectionNode node);

	/**
	 * Called when this structure is unloaded, similar to {@link SelfRegisteringSkriptEvent#unregister(Trigger)}.
	 */
	public void unload() { }

	@Override
	public String toString() {
		return toString(null, false);
	}

	@Nullable
	public static Structure parse(String expr, SectionNode sectionNode, @Nullable String defaultError) {
		Structure.setNode(sectionNode);

		Iterator<SyntaxElementInfo<? extends Structure>> iterator =
			new ConsumingIterator<>(Skript.getNormalStructures().iterator(),
				elementInfo -> ParserInstance.get().getData(StructureData.class).syntaxElementInfo = elementInfo);

		ParseLogHandler parseLogHandler = SkriptLogger.startParseLogHandler();
		try {
			Structure structure = SkriptParser.parseStatic(expr, iterator, ParseContext.EVENT, defaultError);
			if (structure != null) {
				parseLogHandler.printLog();
				return structure;
			}
			parseLogHandler.printError();
			return null;
		} finally {
			parseLogHandler.stop();
		}
	}

	static void setNode(SectionNode sectionNode) {
		StructureData structureData = ParserInstance.get().getData(StructureData.class);
		structureData.sectionNode = sectionNode;
	}

	@Nullable
	protected static SyntaxElementInfo<? extends Structure> getSyntaxElementInfo() {
		return ParserInstance.get().getData(StructureData.class).syntaxElementInfo;
	}

	static {
		ParserInstance.registerData(StructureData.class, StructureData::new);
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	private static class StructureData extends ParserInstance.Data {
		private SectionNode sectionNode;
		@Nullable
		private SyntaxElementInfo<? extends Structure> syntaxElementInfo;

		public StructureData(ParserInstance parserInstance) {
			super(parserInstance);
		}
	}

}