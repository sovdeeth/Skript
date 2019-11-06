package ch.njol.skript.variables.storage.skdb;

import java.nio.ByteBuffer;
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
			byte[] bytes = ((String) part).getBytes(StandardCharsets.UTF_8);
			
			buf.put(PATH_STRING);
			buf.putChar((char) bytes.length); // 2 bytes for length should be enough
			buf.put(bytes);
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
			byte[] bytes = new byte[len];
			buf.get(bytes);
			return new String(bytes, StandardCharsets.UTF_8);
		}
	}
}
