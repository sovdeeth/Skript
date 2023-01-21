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
package ch.njol.skript.aliases;


import org.skriptlang.skript.lang.script.ScriptData;

/**
 * Per-script aliases provider and parser container.
 */
public class ScriptAliases implements ScriptData {
	
	/**
	 * Aliases provider.
	 */
	public final AliasesProvider provider;
	
	/**
	 * Aliases parser linked to our provider.
	 */
	public final AliasesParser parser;
	
	ScriptAliases(AliasesProvider provider, AliasesParser parser) {
		this.provider = provider;
		this.parser = parser;
	}

}
