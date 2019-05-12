package ch.njol.skript.variables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Variable scope tests.
 */
public class VariableScopeTest {
	
	@Test
	public void globalsTest() {
		VariablePath path = new VariablePath("test");
		VariableScope scope = new VariableScope();
		scope.set(path, null, "foo");
		assertEquals("foo", scope.get(path, null));
		scope.set(path, null, "bar");
		assertEquals("bar", scope.get(path, null));
	}
	
	@Test
	public void simpleListTest() {
		VariablePath path = new VariablePath("test", "foo");
		VariableScope scope = new VariableScope();
		scope.set(path, null, "foo");
		ListVariable list = (ListVariable) scope.get(new VariablePath("test"), null);
		assertNotNull(list);
		assertEquals(1, list.getSize());
		assertEquals(list, path.cachedParent);
		assertEquals("foo", scope.get(path, null));
		scope.set(path, null, "bar");
		assertEquals("bar", scope.get(path, null));
	}
}
