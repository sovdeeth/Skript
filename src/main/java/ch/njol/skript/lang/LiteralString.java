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
package ch.njol.skript.lang;

import ch.njol.skript.lang.util.ConvertedLiteral;
import ch.njol.skript.util.Utils;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.Optional;

public class LiteralString extends VariableString implements Literal<String> {

	/**
	 * Creates a new VariableString which does not contain variables.
	 *
	 * @param input Content for string.
	 */
	protected LiteralString(String input) {
		super(input);
	}

	@Override
	public String[] getArray() {
		return new String[]{original};
	}

	@Override
	public String getSingle() {
		return original;
	}

	@Override
	public String[] getAll() {
		return new String[]{original};
	}

	@Override
	public Optional<String> getOptionalSingle(Event event) {
		return Optional.of(original);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> @Nullable Literal<? extends R> getConvertedExpression(Class<R>... to) {
		if (CollectionUtils.containsSuperclass(to, String.class))
			return (Literal<? extends R>) this;
		Class<R> superType = (Class<R>) Utils.getSuperType(to);
		R[] parsedData = Converters.convert(this.getArray(), to, superType);
		if (parsedData.length != 1)
			return null;
		return new ConvertedLiteral<>(this, parsedData, superType);
	}

	/**
	 * Use {@link #toString(Event)} to get the actual string. This method is for debugging.
	 */
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return '"' + original + '"';
	}

}
