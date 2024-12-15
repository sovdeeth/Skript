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
package org.skriptlang.skript.lang.entry.util;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.localization.Message;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.ParseLogHandler;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;

/**
 * A type of {@link KeyValueEntryData} designed to parse its value as an {@link Expression}.
 * This data <b>CAN</b> return null if expression parsing fails.
 * Note that it <b>will</b> print an error.
 */
public class ExpressionEntryData<T> extends KeyValueEntryData<Expression<? extends T>> {

	private static final Message M_IS = new Message("is");

	private final Class<T>[] returnTypes;

	private final int flags;

	/**
	 * @param returnType The expected return type of the matched expression.
	 */
	public ExpressionEntryData(
		String key, @Nullable Expression<T> defaultValue, boolean optional, Class<T> returnType
	) {
		this(key, defaultValue, optional, SkriptParser.ALL_FLAGS, returnType);
	}

	/**
	 * @param returnType The expected return type of the matched expression.
	 * @param flags Parsing flags. See {@link SkriptParser#SkriptParser(String, int, ParseContext)}
	 *              javadoc for more details.
	 */
	public ExpressionEntryData(
		String key, @Nullable Expression<T> defaultValue, boolean optional, Class<T> returnType, int flags
	) {
		this(key, defaultValue, optional, flags, returnType);
	}

	/**
	 * @param returnTypes The expected return types of the matched expression.
	 */
	@SafeVarargs
	public ExpressionEntryData(
		String key, @Nullable Expression<T> defaultValue, boolean optional, Class<T>... returnTypes
	) {
		this(key, defaultValue, optional, SkriptParser.ALL_FLAGS, returnTypes);
	}

	/**
	 * @param returnTypes The expected return types of the matched expression.
	 * @param flags Parsing flags. See {@link SkriptParser#SkriptParser(String, int, ParseContext)}
	 *              javadoc for more details.
	 */
	@SafeVarargs
	public ExpressionEntryData(
		String key, @Nullable Expression<T> defaultValue, boolean optional, int flags, Class<T>... returnTypes
	) {
		super(key, defaultValue, optional);
		this.returnTypes = returnTypes;
		this.flags = flags;
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	protected Expression<? extends T> getValue(String value) {
		Expression<? extends T> expression;
		try (ParseLogHandler log = new ParseLogHandler().start()) {
			expression = new SkriptParser(value, flags, ParseContext.DEFAULT)
				.parseExpression(returnTypes);
			if (expression == null) // print an error if it couldn't parse
				log.printError(
					"'" + value + "' " + M_IS + " " + SkriptParser.notOfType(returnTypes),
					ErrorQuality.NOT_AN_EXPRESSION
				);
		}
		return expression;
	}

}
