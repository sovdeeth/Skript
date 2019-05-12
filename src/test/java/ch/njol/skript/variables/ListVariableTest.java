package ch.njol.skript.variables;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

/**
 * Tests {@link ListVariable}.
 */
public class ListVariableTest {
	
	@Test
	public void creationTest() {
		ListVariable list = new ListVariable();
		list.assertPlainArray();
		assertEquals(0, list.getSize());
	}
	
	@Test
	public void simpleArrayTest() {
		ListVariable list = new ListVariable();
		for (int i = 0; i < 1000; i++) {
			list.add(i);
		}
		list.assertPlainArray();
		for (int i = 0; i < 1000; i++) {
			assertEquals(i, list.get(i));
		}
		Iterator<VariableEntry> it = list.orderedIterator();
		for (int i = 0; i < 1000; i++) {
			assertEquals(i, it.next().getValue());
		}
	}
	
	@Test
	public void smallListTest() {
		ListVariable list = new ListVariable();
		list.put("a", "a");
		list.assertSmallList();
		assertEquals("a", list.get("a"));
	}
	
	@Test
	public void simpleMapTest() {
		ListVariable list = new ListVariable();
		for (char c = 0; c < 1000; c++) {
			list.put("" + c, c);
		}
		Iterator<VariableEntry> it = list.orderedIterator();
		for (char c = 0; c < 1000; c++) {
			assertEquals(c, it.next().getValue());
		}
	}
}
