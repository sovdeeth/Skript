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
package ch.njol.skript.lang.function;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.effects.EffReturn;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.function.Functions.FunctionData;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.variables.ListVariable;
import ch.njol.skript.variables.VariablePath;
import ch.njol.skript.variables.VariableScope;
import ch.njol.skript.variables.Variables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
/**
 * @author Peter Güttinger
 */
public class ScriptFunction<T> extends Function<T> {
	
	@Nullable
	final Trigger trigger;
	
	@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
	public ScriptFunction(Signature<T> sign, SectionNode node) {
		super(sign);
		
		Functions.currentFunction = this;
		try {
			trigger = new Trigger(node.getConfig().getFile(), "function " + sign.getName(),
					new SimpleEvent(), ScriptLoader.loadItems(node));
		} finally {
			Functions.currentFunction = null;
		}
	}
	
	private boolean returnValueSet = false;
	@Nullable
	private T[] returnValue = null;
	
	/**
	 * Should only be called by {@link EffReturn}.
	 * 
	 * @param e
	 * @param value
	 */
	public final void setReturnValue(final FunctionEvent e, final @Nullable T[] value) {
		assert !returnValueSet;
		returnValueSet = true;
		returnValue = value;
	}
	
	@Override
	@Nullable
	public T[] execute(FunctionEvent<?> e, Object[][] params) {
		throw new UnsupportedOperationException("ScriptFunction API changed"); // TODO map this to our other execute
	}
	
	// REMIND track possible types of local variables (including undefined variables) (consider functions, commands, and EffChange) - maybe make a general interface for this purpose
	// REM: use patterns, e.g. {_a%b%} is like "a.*", and thus subsequent {_axyz} may be set and of that type.
	@Nullable
	public T[] execute(final FunctionEvent<?> e, final Object[] params) {
		if (trigger == null)
			throw new IllegalStateException("trigger for function is not available");
		
		Parameter<?>[] parameters = sign.getParameters();
		// Get local variables inside function
		assert trigger != null;
		VariableScope localVars = trigger.getLocalVariables().getEventScope(e);
		
		// Put parameters to local variables
		for (int i = 0; i < parameters.length; i++) {
			Parameter<?> p = parameters[i];
			Object val = params[i];
			assert p.single || val instanceof ListVariable : "incorrect amount of values for arg '" + p.getName() + "'";
			if (val != null) { // Null values simply need not to be written
				if (val instanceof ListVariable) { // Changes inside function need to stay there
					// TODO go through the trigger, look for EffChanges
					// If none of them write to this list, we don't need to clone
					val = new ListVariable((ListVariable) val);
				}
				localVars.set(VariablePath.create(p.getName()), e, val);
			}
		}
		
		assert trigger != null;
		trigger.execute(e);
		assert trigger != null;
		trigger.getLocalVariables().finishedEvent(e); // Even never reaches SkriptEventHandler, we do this here
		return returnValue;
	}

	@Override
	public boolean resetReturnValue() {
		returnValue = null;
		returnValueSet = false;
		return true;
	}

}
