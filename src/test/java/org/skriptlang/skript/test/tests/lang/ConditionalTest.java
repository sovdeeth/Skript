package org.skriptlang.skript.test.tests.lang;

import ch.njol.skript.lang.util.ContextlessEvent;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.skriptlang.skript.lang.condition.CompoundConditional;
import org.skriptlang.skript.lang.condition.Conditional;

public class ConditionalTest {

	TestConditional condTrue;
	TestConditional condFalse;
	TestConditional condUnknown;
	Event event;

	@Before
	public void setup() {
		condTrue = new TestConditional(Kleenean.TRUE);
		condFalse = new TestConditional(Kleenean.FALSE);
		condUnknown = new TestConditional(Kleenean.UNKNOWN);
		event = ContextlessEvent.get();
	}

	@Test
	public void testBasicConditionals() {
		Assert.assertEquals(Kleenean.TRUE, condTrue.evaluate(event));
		Assert.assertEquals(1, condTrue.timesEvaluated);
		condTrue.reset();

		Assert.assertEquals(Kleenean.FALSE, condFalse.evaluate(event));
		Assert.assertEquals(1, condFalse.timesEvaluated);
		condFalse.reset();

		Assert.assertEquals(Kleenean.UNKNOWN, condUnknown.evaluate(event));
		Assert.assertEquals(1, condUnknown.timesEvaluated);
		condUnknown.reset();
	}

	private void assertBasic(TestConditional conditional, Kleenean expected, Kleenean actual, int expectedEvals) {
		Assert.assertEquals("Incorrect evaluation!", expected, actual);
		assertEvals(conditional, expectedEvals);
	}

	private void assertEvals(TestConditional conditional, int expectedEvals) {
		Assert.assertEquals("Wrong number of evaluations!", expectedEvals, conditional.timesEvaluated);
		conditional.reset();
	}

	@Test
	public void testBasicAndKnown() {
		// true AND known x
		assertBasic(condTrue, Kleenean.TRUE, condTrue.and(Kleenean.TRUE, event), 1);
		assertBasic(condTrue, Kleenean.FALSE, condTrue.and(Kleenean.FALSE, event), 0);
		assertBasic(condTrue, Kleenean.UNKNOWN, condTrue.and(Kleenean.UNKNOWN, event), 1);

		// false AND known x
		assertBasic(condFalse, Kleenean.FALSE, condFalse.and(Kleenean.TRUE, event), 1);
		assertBasic(condFalse, Kleenean.FALSE, condFalse.and(Kleenean.FALSE, event), 0);
		assertBasic(condFalse, Kleenean.FALSE, condFalse.and(Kleenean.UNKNOWN, event), 1);

		// unknown AND known x
		assertBasic(condUnknown, Kleenean.UNKNOWN, condUnknown.and(Kleenean.TRUE, event), 1);
		assertBasic(condUnknown, Kleenean.FALSE, condUnknown.and(Kleenean.FALSE, event), 0);
		assertBasic(condUnknown, Kleenean.UNKNOWN, condUnknown.and(Kleenean.UNKNOWN, event), 1);
	}

	@Test
	public void testBasicOrKnown() {
		// true AND known x
		assertBasic(condTrue, Kleenean.TRUE, condTrue.or(Kleenean.TRUE, event), 0);
		assertBasic(condTrue, Kleenean.TRUE, condTrue.or(Kleenean.FALSE, event), 1);
		assertBasic(condTrue, Kleenean.TRUE, condTrue.or(Kleenean.UNKNOWN, event), 1);

		// false AND known x
		assertBasic(condFalse, Kleenean.TRUE, condFalse.or(Kleenean.TRUE, event), 0);
		assertBasic(condFalse, Kleenean.FALSE, condFalse.or(Kleenean.FALSE, event), 1);
		assertBasic(condFalse, Kleenean.UNKNOWN, condFalse.or(Kleenean.UNKNOWN, event), 1);

		// unknown AND known x
		assertBasic(condUnknown, Kleenean.TRUE, condUnknown.or(Kleenean.TRUE, event), 0);
		assertBasic(condUnknown, Kleenean.UNKNOWN, condUnknown.or(Kleenean.FALSE, event), 1);
		assertBasic(condUnknown, Kleenean.UNKNOWN, condUnknown.or(Kleenean.UNKNOWN, event), 1);
	}

	@Test
	public void testBasicNot() {
		assertBasic(condTrue, Kleenean.FALSE, condTrue.not(event), 1);
		assertBasic(condFalse, Kleenean.TRUE, condFalse.not(event), 1);
		assertBasic(condUnknown, Kleenean.UNKNOWN, condUnknown.not(event), 1);
	}

	@Test
	public void testBasicAndBasic() {

		TestConditional condTrueB = new TestConditional(Kleenean.TRUE);
		TestConditional condFalseB = new TestConditional(Kleenean.FALSE);
		TestConditional condUnknownB = new TestConditional(Kleenean.UNKNOWN);

		// true AND x
		Assert.assertEquals(Kleenean.TRUE, condTrue.and(condTrueB, event));
		assertEvals(condTrue, 1);
		assertEvals(condTrueB, 1);

		Assert.assertEquals(Kleenean.FALSE, condTrue.and(condFalse, event));
		assertEvals(condTrue, 0);
		assertEvals(condFalse, 1);

		Assert.assertEquals(Kleenean.UNKNOWN, condTrue.and(condUnknown, event));
		assertEvals(condTrue, 1);
		assertEvals(condUnknown, 1);

		// false AND x
		Assert.assertEquals(Kleenean.FALSE, condFalse.and(condFalseB, event));
		assertEvals(condFalse, 0);
		assertEvals(condFalseB, 1);

		Assert.assertEquals(Kleenean.FALSE, condFalse.and(condTrue, event));
		assertEvals(condFalse, 1);
		assertEvals(condTrue, 1);

		Assert.assertEquals(Kleenean.FALSE, condFalse.and(condUnknown, event));
		assertEvals(condFalse, 1);
		assertEvals(condUnknown, 1);

		// unknown AND x
		Assert.assertEquals(Kleenean.UNKNOWN, condUnknown.and(condUnknownB, event));
		assertEvals(condUnknown, 1);
		assertEvals(condUnknownB, 1);

		Assert.assertEquals(Kleenean.UNKNOWN, condUnknown.and(condTrue, event));
		assertEvals(condUnknown, 1);
		assertEvals(condTrue, 1);

		Assert.assertEquals(Kleenean.FALSE, condUnknown.and(condFalse, event));
		assertEvals(condUnknown, 0);
		assertEvals(condFalse, 1);
	}

	@Test
	public void testCombinedAnd() {

		TestConditional condTrueB = new TestConditional(Kleenean.TRUE);
		TestConditional condFalseB = new TestConditional(Kleenean.FALSE);
		TestConditional condUnknownB = new TestConditional(Kleenean.UNKNOWN);

		Conditional<Event> trueAndTrue = Conditional.builder(Event.class)
			.and(condTrue, condTrueB)
			.build();

		Assert.assertEquals(Kleenean.TRUE, trueAndTrue.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condTrueB, 1);


		Conditional<Event> trueAndFalse = Conditional.builder(Event.class)
			.and(condTrue, condFalse)
			.build();

		Assert.assertEquals(Kleenean.FALSE, trueAndFalse.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condFalse, 1);


		Conditional<Event> falseAndTrueAndFalse = Conditional.builder(Event.class)
			.and(condFalse, condTrue, condFalse)
			.build();

		Assert.assertEquals(Kleenean.FALSE, falseAndTrueAndFalse.evaluate(event));
		assertEvals(condTrue, 0);
		assertEvals(condFalse, 1);


		Conditional<Event> trueAndUnknown = Conditional.builder(Event.class)
			.and(condTrue, condUnknown)
			.build();

		Assert.assertEquals(Kleenean.UNKNOWN, trueAndUnknown.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condUnknown, 1);


		Conditional<Event> falseAndFalse = Conditional.builder(Event.class)
			.and(condFalse, condFalseB)
			.build();

		Assert.assertEquals(Kleenean.FALSE, falseAndFalse.evaluate(event));
		assertEvals(condFalseB, 0);
		assertEvals(condFalse, 1);

		Conditional<Event> falseAndUnknown = Conditional.builder(Event.class)
			.and(condFalse, condUnknown)
			.build();

		Assert.assertEquals(Kleenean.FALSE, falseAndUnknown.evaluate(event));
		assertEvals(condUnknown, 0);
		assertEvals(condFalse, 1);

		Conditional<Event> unknownAndUnknown = Conditional.builder(Event.class)
			.and(condUnknown, condUnknownB)
			.build();

		Assert.assertEquals(Kleenean.UNKNOWN, unknownAndUnknown.evaluate(event));
		assertEvals(condUnknown, 1);
		assertEvals(condUnknownB, 1);
	}

	@Test
	public void testCombinedOr() {

		TestConditional condTrueB = new TestConditional(Kleenean.TRUE);
		TestConditional condFalseB = new TestConditional(Kleenean.FALSE);
		TestConditional condUnknownB = new TestConditional(Kleenean.UNKNOWN);

		Conditional<Event> trueOrTrue = Conditional.builder(Event.class)
			.or(condTrue, condTrueB)
			.build();

		Assert.assertEquals(Kleenean.TRUE, trueOrTrue.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condTrueB, 0);


		Conditional<Event> trueOrFalse = Conditional.builder(Event.class)
			.or(condTrue, condFalse)
			.build();

		Assert.assertEquals(Kleenean.TRUE, trueOrFalse.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condFalse, 0);


		Conditional<Event> falseOrTrueOrFalse = Conditional.builder(Event.class)
			.or(condFalse, condTrue, condFalseB)
			.build();

		Assert.assertEquals(Kleenean.TRUE, falseOrTrueOrFalse.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condFalse, 1);
		assertEvals(condFalseB, 0);


		Conditional<Event> trueOrUnknown = Conditional.builder(Event.class)
			.or(condTrue, condUnknown)
			.build();

		Assert.assertEquals(Kleenean.TRUE, trueOrUnknown.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condUnknown, 0);


		Conditional<Event> falseOrFalse = Conditional.builder(Event.class)
			.or(condFalse, condFalseB)
			.build();

		Assert.assertEquals(Kleenean.FALSE, falseOrFalse.evaluate(event));
		assertEvals(condFalseB, 1);
		assertEvals(condFalse, 1);

		Conditional<Event> falseOrUnknown = Conditional.builder(Event.class)
			.or(condFalse, condUnknown)
			.build();

		Assert.assertEquals(Kleenean.UNKNOWN, falseOrUnknown.evaluate(event));
		assertEvals(condUnknown, 1);
		assertEvals(condFalse, 1);

		Conditional<Event> unknownAndUnknown = Conditional.builder(Event.class)
			.or(condUnknown, condUnknownB)
			.build();

		Assert.assertEquals(Kleenean.UNKNOWN, unknownAndUnknown.evaluate(event));
		assertEvals(condUnknown, 1);
		assertEvals(condUnknownB, 1);
	}

	@Test
	public void testComplexCombined() {
		Conditional<Event> trueAndFalseOrUnknownOrTrue = Conditional.builder(Event.class)
			.and(condTrue, condFalse)
			.or(condUnknown, condTrue)
			.build();

		Assert.assertEquals(Kleenean.TRUE, trueAndFalseOrUnknownOrTrue.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condFalse, 1);
		assertEvals(condUnknown, 1);

		Conditional<Event> trueOrTrueAndFalseOrUnknown = Conditional.builder(Event.class)
			.or(condTrue)
			.or(new CompoundConditional<>(Conditional.Operator.AND, condTrue, condFalse))
			.or(condUnknown)
			.build();

		Assert.assertEquals(Kleenean.TRUE, trueOrTrueAndFalseOrUnknown.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condFalse, 0);
		assertEvals(condUnknown, 0);

		// should compose to (U && T && F) || (T && T && F)
		Conditional<Event> unknownOrTrueAndTrueAndFalse = Conditional.builder(Event.class)
			.or(condUnknown, condTrue)
			.and(condTrue, condFalse)
			.build();

		Assert.assertEquals(Kleenean.FALSE, unknownOrTrueAndTrueAndFalse.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condFalse, 1);
		assertEvals(condUnknown, 1);
	}

	@Test
	public void testCombinedAndOrNot() {
		TestConditional condFalseB = new TestConditional(Kleenean.FALSE);

		Conditional<Event> falseOrNotTrueAndFalse = Conditional.builder(condFalse)
			.orNot(new CompoundConditional<>(Conditional.Operator.AND, condTrue, condFalseB))
			.build();

		Assert.assertEquals(Kleenean.TRUE, falseOrNotTrueAndFalse.evaluate(event));
		assertEvals(condTrue, 1);
		assertEvals(condFalse, 1);
		assertEvals(condFalseB, 1);


		Conditional<Event> unknownAndNotTrueOrFalseOrNotFalse = Conditional.builder(Event.class)
			.and(condUnknown)
			.andNot(new CompoundConditional<>(Conditional.Operator.OR, condTrue, condFalse))
			.orNot(condFalseB)
			.build();

		Assert.assertEquals(Kleenean.TRUE, unknownAndNotTrueOrFalseOrNotFalse.evaluate(event));
		assertEvals(condUnknown, 1);
		assertEvals(condTrue, 1);
		assertEvals(condFalse, 0);
		assertEvals(condFalseB, 1);
	}

	@Ignore
	private static class TestConditional implements Conditional<Event> {

		public int timesEvaluated;
		public final Kleenean value;

		TestConditional(Kleenean value) {
			this.value = value;
		}

		@Override
		public Kleenean evaluate(Event event) {
			++timesEvaluated;
			return value;
		}

		public void reset() {
			timesEvaluated = 0;
		}

		@Override
		public String toString(@Nullable Event event, boolean debug) {
			return value.toString();
		}
	}
}
