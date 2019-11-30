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
 *
 * Copyright 2011-2017 Peter Güttinger and contributors
 */
package ch.njol.skript.command;

import java.util.WeakHashMap;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.Utils;
import ch.njol.skript.variables.ListVariable;
import ch.njol.skript.variables.LocalVariableScope;
import ch.njol.skript.variables.VariablePath;
import ch.njol.skript.variables.VariableScope;
import ch.njol.skript.variables.Variables;

/**
 * Represents an argument of a command
 * 
 * @author Peter Güttinger
 */
public class Argument<T> {
	
	@Nullable
	private final String name;
	
	@Nullable
	private final Expression<? extends T> def;
	
	private final ClassInfo<T> type;
	private final boolean single;
	
	private final int index;
	
	private final boolean optional;
	
	/**
	 * Local variables where this argument is put.
	 */
	private final LocalVariableScope localVars;
	
	private transient WeakHashMap<Event, T[]> current = new WeakHashMap<>();
	
	private Argument(@Nullable String name, @Nullable Expression<? extends T> def, ClassInfo<T> type,
			boolean single, int index, boolean optional, LocalVariableScope localVars) {
		this.name = name;
		this.def = def;
		this.type = type;
		this.single = single;
		this.index = index;
		this.optional = optional;
		this.localVars = localVars;
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> Argument<T> newInstance(@Nullable String name, ClassInfo<T> type, @Nullable String def, int index, boolean single, boolean forceOptional) {
		if (name != null && !Variable.isValidVariableName(name, false, false)) {
			Skript.error("An argument's name must be a valid variable name, and cannot be a list variable.");
			return null;
		}
		Expression<? extends T> d = null;
		if (def != null) {
			if (def.startsWith("%") && def.endsWith("%")) {
				final RetainingLogHandler log = SkriptLogger.startRetainingLog();
				try {
					d = new SkriptParser("" + def.substring(1, def.length() - 1), SkriptParser.PARSE_EXPRESSIONS, ParseContext.COMMAND).parseExpression(type.getC());
					if (d == null) {
						log.printErrors("Can't understand this expression: " + def + "");
						return null;
					}
					log.printLog();
				} finally {
					log.stop();
				}
			} else {
				final RetainingLogHandler log = SkriptLogger.startRetainingLog();
				try {
					if (type.getC() == String.class) {
						if (def.startsWith("\"") && def.endsWith("\""))
							d = (Expression<? extends T>) VariableString.newInstance("" + def.substring(1, def.length() - 1));
						else
							d = (Expression<? extends T>) new SimpleLiteral<>(def, false);
					} else {
						d = new SkriptParser(def, SkriptParser.PARSE_LITERALS, ParseContext.DEFAULT).parseExpression(type.getC());
					}
					if (d == null) {
						log.printErrors("Can't understand this expression: '" + def + "'");
						return null;
					}
					log.printLog();
				} finally {
					log.stop();
				}
			}
		}
		return new Argument<>(name, d, type, single, index, def != null || forceOptional, ScriptLoader.getLocalVariables());
	}
	
	@Override
	public String toString() {
		final Expression<? extends T> def = this.def;
		return "<" + (name != null ? name + ": " : "") + Utils.toEnglishPlural(type.getCodeName(), !single) + (def == null ? "" : " = " + def.toString()) + ">";
	}
	
	public boolean isOptional() {
		return optional;
	}
	
	public void setToDefault(final ScriptCommandEvent event) {
		if (def != null)
			set(event, def.getArray(event));
	}
	
	@SuppressWarnings("unchecked")
	public void set(final ScriptCommandEvent e, final Object[] o) {
		if (!(type.getC().isAssignableFrom(o.getClass().getComponentType())))
			throw new IllegalArgumentException();
		current.put(e, (T[]) o);
		
		// Set argument to local variables
		if (name != null) {
			if (single) {
				if (o.length > 0) {
					Object value = o[0];
					assert value != null;
					localVars.set(VariablePath.create(name), e, o);
				}
			} else {
				ListVariable list = new ListVariable();
				for (int i = 0; i < o.length; i++)
					list.add(o[i]);
				localVars.set(VariablePath.create(name), e, list);
			}
		}
	}
	
	@Nullable
	public T[] getCurrent(final Event e) {
		return current.get(e);
	}
	
	public Class<T> getType() {
		return type.getC();
	}
	
	public int getIndex() {
		return index;
	}
	
	public boolean isSingle() {
		return single;
	}
	
}
