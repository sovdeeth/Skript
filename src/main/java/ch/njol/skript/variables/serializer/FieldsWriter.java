package ch.njol.skript.variables.serializer;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Converters;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.Fields.FieldContext;

/**
 * Writes objects to byte buffers.
 */
public class FieldsWriter {
	
	/**
	 * Writes an object that has a serializer available to a byte buffer.
	 * @param buf Target byte buffer.
	 * @param value Object to write.
	 * @return How many bytes were written to the buffer.
	 * @throws StreamCorruptedException Potentially thrown by the serializer.
	 */
	public int write(ByteBuffer buf, Object value) throws StreamCorruptedException {
		int startPos = buf.position();
		writeValue(buf, value);
		return buf.position() - startPos;
	}
	
	/**
	 * Serializes a value to a byte buffer.
	 * @param buf Target byte buffer.
	 * @param value Value to write.
	 * @throws StreamCorruptedException May be thrown by serializer.
	 */
	private void writeValue(ByteBuffer buf, @Nullable Object value) throws StreamCorruptedException {
		if (value instanceof Boolean) {
			buf.put(FieldType.BOOLEAN.id());
			buf.put((byte) (((boolean) value) ? 1 : 0));
		} else if (value instanceof Byte) {
			buf.put(FieldType.BYTE.id());
			buf.put((byte) value);
		} else if (value instanceof Short) {
			buf.put(FieldType.SHORT.id());
			buf.putShort((short) value);
		} else if (value instanceof Character) {
			buf.put(FieldType.CHAR.id());
			buf.putChar((char) value);
		} else if (value instanceof Integer) {
			buf.put(FieldType.INT.id());
			buf.putInt((int) value);
		} else if (value instanceof Long) {
			buf.put(FieldType.LONG.id());
			buf.putLong((long) value);
		} else if (value instanceof Float) {
			buf.put(FieldType.FLOAT.id());
			buf.putFloat((float) value);
		} else if (value instanceof Double) {
			buf.put(FieldType.DOUBLE.id());
			buf.putDouble((double) value);
		} else if (value instanceof String) {
			writeString(buf, (String) value);
		} else {
			writeObject(buf, value);
		}
	}
	
	/**
	 * Writes an object to given byte buffer.
	 * @param buf Buffer.
	 * @param value Object value.
	 * @throws StreamCorruptedException Thrown by serializer of class.
	 */
	private void writeObject(ByteBuffer buf, @Nullable Object value) throws StreamCorruptedException {
		if (value == null) { // Write null
			writeNull(buf);
			return;
		}
		ClassInfo<?> ci = Classes.getSuperClassInfo(value.getClass());
		if (ci.getSerializeAs() != null) { // Convert to type we want to serialize as
			ci = Classes.getExactClassInfo(ci.getSerializeAs());
			assert ci != null : "invalid serializeAs for " + value;
			
			value = Converters.convert(value, ci.getC());
			assert value != null : "serializeAs converter returned null";
		}
		
		// Get serializer responsible for type we serialize as
		@SuppressWarnings("unchecked")
		Serializer<Object> serializer = (Serializer<Object>) ci.getSerializer();
		if (serializer == null) {
			// TODO emit a warning about failed serialization?
			writeNull(buf);
			return;
		}
		
		// Assert checks if this serializer can be used in current thread
		assert serializer.mustSyncDeserialization() ? Bukkit.isPrimaryThread() : true : "thread-unsafe serializer";
		
		// Make class serializer dig out fields we need
		Fields fields;
		try {
			fields = serializer.serialize(value);
		} catch (NotSerializableException e) {
			throw new AssertionError("registered as serializable, but it is not");
		}
		
		// Write everything to buffer
		buf.put(FieldType.OBJECT.id()); // Writing an object of...
		writeId(buf, ci.getCodeName()); // ... some kind
		assert fields.size() <= Character.MAX_VALUE : "too many fields";
		buf.putChar((char) fields.size()); // How many fields there are
		write(buf, fields); // Fields in order
	}
	
	/**
	 * Writes given fields to a byte buffer.
	 * @param buf Buffer.
	 * @param fields Fields to write.
	 * @throws StreamCorruptedException Serializers may throw this.
	 */
	private void write(ByteBuffer buf, Fields fields) throws StreamCorruptedException {
		for (FieldContext field : fields) {
			writeId(buf, field.getID());
			Object value = field.isPrimitive() ? field.getPrimitive() : field.getObject();
			writeValue(buf, value);
		}
	}
	
	/**
	 * Writes an id string. It must not contain any characters illegal to
	 * ISO 8859-1. As such, it should be never used to serialize user-provided
	 * strings. It is likely faster than Unicode-aware encoders.
	 * @param buf Buffer to write to.
	 * @param id Id to write to there.
	 */
	private static void writeId(ByteBuffer buf, String id) {
		assert id.length() <= Byte.MAX_VALUE : "too long field name";
		buf.put((byte) id.length()); // ISO 8859-1 characters are never more than one code unit
		StandardCharsets.ISO_8859_1.newEncoder().encode(CharBuffer.wrap(id), buf, true);
	}
	
	/**
	 * Writes a string to a byte buffer. Uses UTF-8 as encoding.
	 * @param buf Buffer.
	 * @param value String value.
	 */
	private static void writeString(ByteBuffer buf, String value) {
		buf.put(FieldType.STRING.id());
		
		int lenIndex = buf.position(); // We'll write position here later
		buf.position(lenIndex + 4); // Space for int
		
		StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(value), buf, true);
		int len = buf.position() - lenIndex - 4; // How much did we write?
		buf.putInt(lenIndex, len); // That is length of encoded string
	}
	
	/**
	 * Writes a null.
	 * @param buf Buf where to write to.
	 */
	private static void writeNull(ByteBuffer buf) {
		buf.put(FieldType.NULL.id());
	}
}
