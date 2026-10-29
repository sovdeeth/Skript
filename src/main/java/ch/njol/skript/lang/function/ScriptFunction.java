package ch.njol.skript.lang.function;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.util.Utils;
import ch.njol.skript.variables.HintManager;
import ch.njol.skript.variables.Variables;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionArguments;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.common.function.Parameters;

import java.util.Arrays;

public class ScriptFunction<T> extends Function<T> implements ReturnHandler<T> {

	private final Trigger trigger;

	private static final class ReturnState<T> {
		boolean set;
		T @Nullable [] values;
		String @Nullable [] keys;

		void reset() {
			set = false;
			values = null;
			keys = null;
		}
	}

	private final ThreadLocal<ReturnState<T>> returnState = ThreadLocal.withInitial(ReturnState::new);

	public ScriptFunction(Signature<T> sign, SectionNode node) {
		super(sign);

		Functions.currentFunction = this;
		HintManager hintManager = ParserInstance.get().getHintManager();
		try {
			hintManager.enterScope(false);
			for (Parameter<?> parameter : sign.parameters().all()) {
				String hintName = parameter.name();
				if (!parameter.isSingle()) {
					hintName += Variable.SEPARATOR + "*";
					assert parameter.type().isArray();
					hintManager.set(hintName, parameter.type().componentType());
				} else {
					assert !parameter.type().isArray();
					hintManager.set(hintName, parameter.type());
				}
			}
			trigger = loadReturnableTrigger(node, "function " + sign.getName(), new SimpleEvent());
		} finally {
			hintManager.exitScope();
			Functions.currentFunction = null;
		}
		trigger.setLineNumber(node.getLine());
	}

	// REMIND track possible types of local variables (including undefined variables) (consider functions, commands, and EffChange) - maybe make a general interface for this purpose
	// REM: use patterns, e.g. {_a%b%} is like "a.*", and thus subsequent {_axyz} may be set and of that type.
	@Override
	public T @Nullable [] execute(FunctionEvent<?> event, Object[][] params) {
		int i = 0;
		for (Parameter<?> parameter :  getSignature().parameters().all()) {
			Object[] val = params[i];
			if (parameter.isSingle() && val.length > 0) {
				Variables.setVariable(parameter.name(), val[0], event, true);
				i++;
				continue;
			}

			if (val.length == 0) {
				i++;
				continue;
			}

			boolean keyed = Arrays.stream(val).allMatch(it -> it instanceof KeyedValue<?>);
			setListParameter(parameter.name(), val, keyed, event);
			i++;
		}

		trigger.execute(event);
		return type() != null ? returnState.get().values : null;
	}

	/**
	 * Sets a list parameter's local variable to the given values in one go.
	 * Null values are skipped.
	 *
	 * @param keyed whether every value is a {@link KeyedValue}. If not, values are numbered from 0.
	 */
	private static void setListParameter(String name, Object[] values, boolean keyed, Event event) {
		String[] keys = new String[values.length];
		Object[] listValues = new Object[values.length];
		int count = 0;
		for (int i = 0; i < values.length; i++) {
			String key;
			Object value;
			if (keyed) {
				KeyedValue<?> keyedValue = (KeyedValue<?>) values[i];
				key = keyedValue.key();
				value = keyedValue.value();
			} else {
				key = String.valueOf(i);
				value = values[i];
			}
			if (value == null)
				continue;
			keys[count] = key;
			listValues[count] = value;
			count++;
		}
		if (count < values.length) {
			keys = Arrays.copyOf(keys, count);
			listValues = Arrays.copyOf(listValues, count);
		}
		Variables.setList(name, keys, listValues, event, true);
	}

	@Override
	public T execute(@NotNull FunctionEvent<?> event, @NotNull FunctionArguments arguments) {
		Parameters parameters = getSignature().parameters();
		FunctionEvent<?> newEvent = new FunctionEvent<>(this);

		for (String name : arguments.names()) {
			Parameter<?> parameter = parameters.get(name);
			Object value = arguments.get(name);

			if (value == null) {
				continue;
			}

			if (parameter.isSingle()) {
				Variables.setVariable(name, value, newEvent, true);
			} else {
				Object[] values = (Object[]) value;
				if (values.length > 0)
					setListParameter(name, values, value instanceof KeyedValue<?>[], newEvent);
			}
		}

		trigger.execute(newEvent);

		T[] vs = returnState.get().values;
		if (type() == null || vs == null || vs.length == 0) {
			return null;
		}

		if (vs.length == 1) {
			return vs[0];
		} else {
			//noinspection unchecked
			return (T) vs;
		}
	}

	@Override
	public @NotNull String @Nullable [] returnedKeys() {
		return type() != null ? returnState.get().keys : null;
	}

	@Override
	public boolean resetReturnValue() {
		returnState.get().reset();
		return true;
	}

	@Override
	public final void returnValues(Event event, Expression<? extends T> value) {
		// evaluate before touching state: the expression may call this same function recursively
		T[] values = value.getArray(event);
		String[] keys = KeyProviderExpression.canReturnKeys(value)
				? ((KeyProviderExpression<?>) value).getArrayKeys(event)
				: null;

		ReturnState<T> state = returnState.get();
		assert !state.set;
		state.set = true;
		state.values = values;
		state.keys = keys;
	}

	@Override
	public final boolean isSingleReturnValue() {
		return isSingle();
	}

	@Override
	public final @Nullable Class<? extends T> returnValueType() {
		//noinspection unchecked
		return (Class<? extends T>) Utils.getComponentType(type());
	}

}
