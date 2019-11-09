package ch.njol.skript.variables.serializer;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;

/**
 * Reads objects serialized with {@link FieldsWriter}.
 */
public class FieldsReader {
	
	/**
	 * Reads an object from a byte buffer. Its type must be serializable by
	 * running Skript instance.
	 * @param buf Buffer to read from.
	 * @return An object, including null if a null was serialized.
	 * @throws StreamCorruptedException Thrown by deserializer when it
	 * encounters corrupted data.
	 */
	@Nullable
	private Object read(ByteBuffer buf) throws StreamCorruptedException {
		// Read data for object
		String codeName = readId(buf);
		Fields fields = readFields(buf);
		
		// Get deserializer
		ClassInfo<?> ci = Classes.getClassInfo(codeName);
		@SuppressWarnings("unchecked")
		Serializer<Object> serializer = (Serializer<Object>) ci.getSerializer();
		if (serializer == null) {
			// TODO emit a warning about failed deserialization?
			return null;
		}
		
		assert serializer.mustSyncDeserialization() ? Bukkit.isPrimaryThread() : true : "thread-unsafe serializer";
		
		// Deserialize the object
		try {
			if (serializer.canBeInstantiated(ci.getC())) {
				return serializer.deserialize(ci.getC(), fields);
			} else {
				Object obj;
				try {
					obj = ci.getC().newInstance();
					assert obj != null;
				} catch (InstantiationException | IllegalAccessException e) {
					throw new AssertionError("cannot actually be instantiated");
				}
				serializer.deserialize(obj, fields);
				return obj;
			}
		} catch (NotSerializableException e) {
			throw new AssertionError("registered as serializable, but it is not");
		}
	}
	
	/**
	 * Reads an id serialized by {@link FieldsWriter}. No Unicode support,
	 * don't use for user-provided strings.
	 * @param buf Buffer.
	 * @return Id string.
	 */
	private String readId(ByteBuffer buf) {
		char len = buf.getChar();
		CharBuffer chars = CharBuffer.allocate(len);
		StandardCharsets.ISO_8859_1.newDecoder().decode(buf, chars, true);
		String str = chars.toString();
		assert str != null;
		return str;
	}
	
	/**
	 * Reads fields from given byte buffer.
	 * @param buf Buffer to read from.
	 * @return Fields.
	 * @throws StreamCorruptedException When reading objects to put to fields
	 * threw it.
	 */
	private Fields readFields(ByteBuffer buf) throws StreamCorruptedException {
		Fields fields = new Fields();
		char fieldCount = buf.getChar();
		for (int i = 0; i < fieldCount; i++) {
			String id = readId(buf);
			FieldType type = FieldType.BY_ID[buf.get()];
			assert type != null;
			if (type == FieldType.NULL) {
				//fields.putObject(id, null); // Unnecessary - it is before anything happens
			} else if (type == FieldType.OBJECT) { // Read normal object
				fields.putObject(id, read(buf));
			} else if (type == FieldType.STRING) { // String special handling
				fields.putObject(id, readString(buf));
			} else { // Primitive type
				fields.putPrimitive(id, readPrimitive(buf, type));
			}
		}
		return fields;
	}
	
	/**
	 * Reads a serialized string. Full Unicode support.
	 * @param buf Source buffer.
	 * @return A string.
	 */
	private String readString(ByteBuffer buf) {
		int len = buf.getInt();
		CharBuffer chars = CharBuffer.allocate(len);
		StandardCharsets.UTF_8.newDecoder().decode(buf, chars, true);
		String str = chars.toString();
		assert str != null;
		return str;
	}
	
	/**
	 * Reads a primitive value.
	 * @param buf Source buffer.
	 * @param type Type of primitive.
	 * @return Boxed primitive type.
	 */
	private Object readPrimitive(ByteBuffer buf, FieldType type) {
		switch (type) {
			case BOOLEAN:
				return buf.get() == 1;
			case BYTE:
				return buf.get();
			case SHORT:
				return buf.getShort();
			case CHAR:
				return buf.getChar();
			case INT:
				return buf.getInt();
			case LONG:
				return buf.getLong();
			case FLOAT:
				return buf.getFloat();
			case DOUBLE:
				return buf.getDouble();
			default:
				throw new AssertionError("not primitive");
		}
	}
}
