package ch.njol.skript.variables;

import static org.junit.Assert.assertEquals;

import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import ch.njol.skript.variables.serializer.FieldsReader;
import ch.njol.skript.variables.serializer.FieldsWriter;

/**
 * Tests that data produced by {@link FieldsWriter} can be read by
 * {@link FieldsReader} without losing data. After all, this is what
 * really matters.
 */
public class SerializerTest {
	
	@SuppressWarnings("null")
	private FieldsWriter writer;
	@SuppressWarnings("null")
	private FieldsReader reader;
	@SuppressWarnings("null")
	private ByteBuffer buf;
	
	@SuppressWarnings("null")
	@Before
	public void init() {
		writer = new FieldsWriter();
		reader = new FieldsReader();
		buf = ByteBuffer.allocate(1024);
	}
	
	@Test
	public void primitiveTest() throws StreamCorruptedException {
		// byte: full range
		for (byte i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++) {
			writer.write(buf, i);
			buf.flip();
			assertEquals(i, reader.read(buf));
			buf.clear();
		}
		
		// short: full range
		for (short i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
			writer.write(buf, i);
			buf.flip();
			assertEquals(i, reader.read(buf));
			buf.clear();
		}
		
		// char: full range
		for (char i = 0; i < Character.MAX_VALUE; i++) {
			writer.write(buf, i);
			buf.flip();
			assertEquals(i, reader.read(buf));
			buf.clear();
		}
		
		// int: same range as with short (full range too big)
		for (int i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
			writer.write(buf, i);
			buf.flip();
			assertEquals(i, reader.read(buf));
			buf.clear();
		}
		
		// long: same as int
		for (long i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
			writer.write(buf, i);
			buf.flip();
			assertEquals(i, reader.read(buf));
			buf.clear();
		}
	}
	
	@Test
	public void stringTest() throws StreamCorruptedException {
		writer.write(buf, "abc");
		buf.flip();
		assertEquals("abc", reader.read(buf));
		buf.clear();
	}
	
	// TODO test complex objects; challenging, becauses Classes is not built for that
}
