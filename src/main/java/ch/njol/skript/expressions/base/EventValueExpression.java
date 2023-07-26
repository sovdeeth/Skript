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
package ch.njol.skript.expressions.base;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.Changer.ChangerUtils;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;

/**
 * A useful class for creating default expressions. It simply returns the event value of the given type.
 * <p>
 * This class can be used as default expression with <code>new EventValueExpression&lt;T&gt;(T.class)</code> or extended to make it manually placeable in expressions with:
 * 
 * <pre>
 * class MyExpression extends EventValueExpression&lt;SomeClass&gt; {
 * 	public MyExpression() {
 * 		super(SomeClass.class);
 * 	}
 * 	// ...
 * }
 * </pre>
 * 
 * @see Classes#registerClass(ClassInfo)
 * @see ClassInfo#defaultExpression(DefaultExpression)
 * @see DefaultExpression
 */
public class EventValueExpression<T> extends SimpleExpression<T> implements DefaultExpression<T> {
	
	private final Class<? extends T> c;
	private final Class<?> componentType;
	@Nullable
	private Changer<? super T> changer;
	private final Map<Class<? extends Event>, Getter<? extends T, ?>> getters = new HashMap<>();
	private final boolean single;
	private final boolean exact;
	
	public EventValueExpression(Class<? extends T> c) {
		this(c, null);
	}

	/**
	 * Construct an event value expression.
	 * 
	 * @param c The class that this event value represents.
	 * @param exact If false, the event value can be a subclass or a converted event value.
	 */
	public EventValueExpression(Class<? extends T> c, boolean exact) {
		this(c, null, exact);
	}

	public EventValueExpression(Class<? extends T> c, @Nullable Changer<? super T> changer) {
		this(c, changer, false);
	}

	public EventValueExpression(Class<? extends T> c, @Nullable Changer<? super T> changer, boolean exact) {
		assert c != null;
		this.c = c;
		this.exact = exact;
		this.changer = changer;
		single = !c.isArray();
		componentType = single ? c : c.getComponentType();
	}
	
	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	protected T[] get(Event event) {
		T value = getValue(event);
		if (value == null)
			return (T[]) Array.newInstance(componentType, 0);
		if (single) {
			T[] one = (T[]) Array.newInstance(c, 1);
			one[0] = value;
			return one;
		}
		T[] dataArray = (T[]) value;
		T[] array = (T[]) Array.newInstance(componentType, dataArray.length);
		System.arraycopy(dataArray, 0, array, 0, array.length);
		return array;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private <E extends Event> T getValue(E event) {
		if (getters.containsKey(event.getClass())) {
			final Getter<? extends T, ? super E> g = (Getter<? extends T, ? super E>) getters.get(event.getClass());
			return g == null ? null : g.get(event);
		}
		
		for (final Entry<Class<? extends Event>, Getter<? extends T, ?>> p : getters.entrySet()) {
			if (p.getKey().isAssignableFrom(event.getClass())) {
				getters.put(event.getClass(), p.getValue());
				return p.getValue() == null ? null : ((Getter<? extends T, ? super E>) p.getValue()).get(event);
			}
		}
		
		getters.put(event.getClass(), null);
		
		return null;
	}
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parser) {
		if (exprs.length != 0)
			throw new SkriptAPIException(this.getClass().getName() + " has expressions in its pattern but does not override init(...)");
		return init();
	}
	
	@Override
	public boolean init() {
		final ParseLogHandler log = SkriptLogger.startParseLogHandler();
		try {
			boolean hasValue = false;
			Class<? extends Event>[] events = getParser().getCurrentEvents();
			if (events == null) {
				assert false;
				return false;
			}
			for (Class<? extends Event> event : events) {
				if (getters.containsKey(event)) {
					hasValue = getters.get(event) != null;
					continue;
				}
				Getter<? extends T, ?> getter;
				if (exact) {
					getter = EventValues.getExactEventValueGetter(event, c, getTime());
				} else {
					getter = EventValues.getEventValueGetter(event, c, getTime());
				}
				if (getter != null) {
					getters.put(event, getter);
					hasValue = true;
				}
			}
			if (!hasValue) {
				log.printError("There's no " + Classes.getSuperClassInfo(componentType).getName().toString(!single) + " in " + Utils.a(getParser().getCurrentEventName()) + " event");
				return false;
			}
			log.printLog();
			return true;
		} finally {
			log.stop();
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends T> getReturnType() {
		return (Class<? extends T>) componentType;
	}
	
	@Override
	public boolean isSingle() {
		return single;
	}
	
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (!debug || event == null)
			return "event-" + Classes.getSuperClassInfo(componentType).getName().toString(!single);
		return Classes.getDebugMessage(getValue(event));
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (changer == null)
			changer = (Changer<? super T>) Classes.getSuperClassInfo(componentType).getChanger();
		return changer == null ? null : changer.acceptChange(mode);
	}
	
	@Override
	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
		if (changer == null)
			throw new SkriptAPIException("The changer cannot be null");
		ChangerUtils.change(changer, getArray(event), delta, mode);
	}
	
	@Override
	public boolean setTime(int time) {
		Class<? extends Event>[] events = getParser().getCurrentEvents();
		if (events == null) {
			assert false;
			return false;
		}
		for (Class<? extends Event> event : events) {
			assert event != null;
			boolean has;
			if (exact) {
				has = EventValues.doesExactEventValueHaveTimeStates(event, c);
			} else {
				has = EventValues.doesEventValueHaveTimeStates(event, c);
			}
			if (has) {
				super.setTime(time);
				// Since the time was changed, we now need to re-initalize the getters we already got. START
				getters.clear();
				init();
				// END
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return true
	 */
	@Override
	public boolean isDefault() {
		return true;
	}
	
}
