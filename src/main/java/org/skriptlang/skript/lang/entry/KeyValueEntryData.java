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
package org.skriptlang.skript.lang.entry;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SimpleNode;
import org.skriptlang.skript.lang.entry.EntryValidator.EntryValidatorBuilder;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An entry based on {@link SimpleNode}s containing a key and a value.
 * Unlike a traditional {@link ch.njol.skript.config.EntryNode}, this entry data
 *  may have a value that is <i>not</i> a String.
 * @param <T> The type of the value.
 */
public abstract class KeyValueEntryData<T> extends EntryData<T> {

	public KeyValueEntryData(String key, @Nullable T defaultValue, boolean optional) {
		super(key, defaultValue, optional);
	}

	/**
	 * Used to obtain and parse the value of a {@link SimpleNode}. This method accepts
	 *  any type of node, but assumes the input to be a {@link SimpleNode}. Before calling this method,
	 *  the caller should first check {@link #canCreateWith(Node)} to make sure that the node is viable.
	 * @param node A {@link SimpleNode} to obtain (and possibly convert) the value of.
	 * @return The value obtained from the provided {@link SimpleNode}.
	 */
	@Override
	@Nullable
	public final T getValue(Node node) {
		assert node instanceof SimpleNode;
		String key = node.getKey();
		if (key == null)
			throw new IllegalArgumentException("EntryData#getValue() called with invalid node.");
		return getValue(ScriptLoader.replaceOptions(key).substring(getKey().length() + getSeparator().length()));
	}

	/**
	 * Parses a String value using this entry data.
	 * @param value The String value to parse.
	 * @return The parsed value.
	 */
	@Nullable
	protected abstract T getValue(String value);

	/**
	 * @return The String acting as a separator between the key and the value.
	 */
	public String getSeparator() {
		return EntryValidatorBuilder.DEFAULT_ENTRY_SEPARATOR;
	}

	/**
	 * Checks whether the provided node can have its value obtained using this entry data.
	 * A check is done to verify that the node is a {@link SimpleNode}, and that it starts
	 *  with the necessary key.
	 * @param node The node to check.
	 * @return Whether the provided {@link Node} works with this entry data.
	 */
	@Override
	public boolean canCreateWith(Node node) {
		if (!(node instanceof SimpleNode))
			return false;
		String key = node.getKey();
		if (key == null)
			return false;
		key = ScriptLoader.replaceOptions(key);
		return key.startsWith(getKey() + getSeparator());
	}

}
