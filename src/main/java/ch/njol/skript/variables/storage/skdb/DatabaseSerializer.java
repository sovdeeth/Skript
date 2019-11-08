package ch.njol.skript.variables.storage.skdb;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import ch.njol.skript.variables.VariablePath;

/**
 * Providers serialization for core variable system types.
 */
public class DatabaseSerializer {
	
	/**
	 * The byte before path part tells type of it.
	 */
	private static final byte PATH_INT = 0, PATH_STRING = 1;
	
	/**
	 * Writes the given path to a byte buffer. Caller must ensure that there
	 * is enough space.
	 * @param path Path to write.
	 * @param buf Byte buffer.
	 */
	public void writePath(VariablePath path, ByteBuffer buf) {
		buf.putChar((char) path.length()); // Length of path
		for (Object part : path) {
			assert part != null;
			writePathPart(part, buf);
		}
	}
	
	/**
	 * Writes the given path part to a byte buffer. Caller must ensure that
	 * there is enough space.
	 * @param part Part of a path.
	 * @param buf Byte buffer.
	 */
	public void writePathPart(Object part, ByteBuffer buf) {
		if (part instanceof Integer) {
			buf.put(PATH_INT);
			buf.putInt((int) part);
		} else {
			buf.put(PATH_STRING);
			int lenIndex = buf.position();
			buf.position(lenIndex + 2); // We'll come back to write length of encoded part
			
			StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap((String) part), buf, true);
			int len = buf.position() - lenIndex; // Length of what we wrote = encoded string length
			assert len <= Character.MAX_VALUE : "too long variable name part";
			buf.putChar(lenIndex, (char) len); // Go back and write length
		}
	}
	
	/**
	 * Reads a path from a byte buffer.
	 * @param buf Buffer.
	 * @return Variable path.
	 */
	public VariablePath readPath(ByteBuffer buf) {
		int length = buf.getChar();
		Object[] parts = new Object[length];
		for (int i = 0; i < length; i++) {
			parts[i] = readPathPart(buf);
		}
		return VariablePath.create(parts);
	}
	
	/**
	 * Reads part of a path from a byte buffer.
	 * @param buf Buffer.
	 * @return Path part, i.e. {@link Integer} or {@link String}.
	 */
	public Object readPathPart(ByteBuffer buf) {
		byte type = buf.get();
		if (type == PATH_INT) {
			return buf.getInt();
		} else {
			assert type == PATH_STRING;
			int len = buf.getChar();
			CharBuffer chars = CharBuffer.allocate(len);
			
			StandardCharsets.UTF_8.newDecoder().decode(buf, chars, true);
			String str = chars.toString();
			assert str != null;
			return str;
		}
	}
}
