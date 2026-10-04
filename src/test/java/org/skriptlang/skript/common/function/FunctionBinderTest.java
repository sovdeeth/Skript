package org.skriptlang.skript.common.function;

import org.junit.Test;
import org.skriptlang.skript.common.function.Parameter.Modifier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests the allocation of passed arguments across a signature's parameters.
 *
 * @see FunctionBinder#allocate(Parameter[], int)
 */
public class FunctionBinderTest {

	private static Parameter<?> single(String name) {
		return new ScriptParameter<>(name, Number.class);
	}

	private static Parameter<?> list(String name) {
		return new ScriptParameter<>(name, Number[].class);
	}

	private static Parameter<?> optionalSingle(String name) {
		return new ScriptParameter<>(name, Number.class, Modifier.OPTIONAL);
	}

	private static Parameter<?> optionalList(String name) {
		return new ScriptParameter<>(name, Number[].class, Modifier.OPTIONAL);
	}

	private static Parameter<?>[] parameters(Parameter<?>... parameters) {
		return parameters;
	}

	@Test
	public void testNoParameters() {
		assertArrayEquals(new int[0], FunctionBinder.allocate(parameters(), 0));
		assertNull(FunctionBinder.allocate(parameters(), 1));
	}

	@Test
	public void testOnlySingleParameters() {
		Parameter<?>[] parameters = parameters(single("a"), single("b"));

		assertArrayEquals(new int[]{1, 1}, FunctionBinder.allocate(parameters, 2));

		// without a list parameter there is nothing to absorb a surplus, and nothing may be omitted
		assertNull(FunctionBinder.allocate(parameters, 3));
		assertNull(FunctionBinder.allocate(parameters, 1));
		assertNull(FunctionBinder.allocate(parameters, 0));
	}

	@Test
	public void testListParameterAbsorbsSurplus() {
		// f(a: number, b: numbers) given 1, 2, 3 binds a=1 and b=(2, 3)
		assertArrayEquals(new int[]{1, 2}, FunctionBinder.allocate(parameters(single("a"), list("b")), 3));
		assertArrayEquals(new int[]{1, 1}, FunctionBinder.allocate(parameters(single("a"), list("b")), 2));

		// a required list parameter must still be given something
		assertNull(FunctionBinder.allocate(parameters(single("a"), list("b")), 1));
	}

	@Test
	public void testAllocatesLeftToRight() {
		// the leftmost list parameter takes as much as it can while leaving enough for the rest
		assertArrayEquals(new int[]{2, 1}, FunctionBinder.allocate(parameters(list("a"), single("b")), 3));
		assertArrayEquals(new int[]{2, 1}, FunctionBinder.allocate(parameters(list("a"), list("b")), 3));
		assertArrayEquals(new int[]{1, 2, 1},
			FunctionBinder.allocate(parameters(single("a"), list("b"), single("c")), 4));
	}

	@Test
	public void testSingleListParameter() {
		assertArrayEquals(new int[]{3}, FunctionBinder.allocate(parameters(list("a")), 3));
		assertArrayEquals(new int[]{1}, FunctionBinder.allocate(parameters(list("a")), 1));

		// a required list parameter cannot be given nothing
		assertNull(FunctionBinder.allocate(parameters(list("a")), 0));
		assertArrayEquals(new int[]{0}, FunctionBinder.allocate(parameters(optionalList("a")), 0));
	}

	@Test
	public void testOptionalParametersMayBeOmitted() {
		Parameter<?>[] parameters = parameters(single("a"), optionalList("b"));

		assertArrayEquals(new int[]{1, 0}, FunctionBinder.allocate(parameters, 1));
		assertArrayEquals(new int[]{1, 1}, FunctionBinder.allocate(parameters, 2));
		assertArrayEquals(new int[]{1, 2}, FunctionBinder.allocate(parameters, 3));

		assertNull(FunctionBinder.allocate(parameters, 0));
	}

	@Test
	public void testOptionalBeforeRequiredIsStillRequired() {
		// matching Signature#getMinParameters, a parameter may only be omitted when it and every
		// parameter after it is optional
		Parameter<?>[] parameters = parameters(optionalSingle("a"), list("b"));

		assertArrayEquals(new int[]{1, 1}, FunctionBinder.allocate(parameters, 2));
		assertArrayEquals(new int[]{1, 2}, FunctionBinder.allocate(parameters, 3));

		assertNull(FunctionBinder.allocate(parameters, 1));
	}

	@Test
	public void testTrailingOptionalSingleParameters() {
		Parameter<?>[] parameters = parameters(single("a"), optionalSingle("b"), optionalSingle("c"));

		assertArrayEquals(new int[]{1, 0, 0}, FunctionBinder.allocate(parameters, 1));
		assertArrayEquals(new int[]{1, 1, 0}, FunctionBinder.allocate(parameters, 2));
		assertArrayEquals(new int[]{1, 1, 1}, FunctionBinder.allocate(parameters, 3));

		// no list parameter, so a surplus cannot be absorbed
		assertNull(FunctionBinder.allocate(parameters, 4));
	}

}
