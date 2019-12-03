package ch.njol.skript.variables.storage.skdb;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.variables.VariablePath;

/**
 * Reads database files.
 */
public class DatabaseReader {
	
	private static final int VALUE = 0, LIST = 1;
	
	/**
	 * Buffer of database content.
	 */
	private final ByteBuffer data;
	
	/**
	 * Serializes {@link VariablePath}s and their parts.
	 */
	private final DatabaseSerializer serializer;
	
	/**
	 * Is current value a list.
	 */
	private boolean isList;
	
	/**
	 * Size of last list we encountered.
	 */
	private int listSize;
	
	public DatabaseReader(ByteBuffer data) {
		this.data = data;
		this.serializer = new DatabaseSerializer();
	}
	
	public ByteBuffer getData() {
		return data;
	}
	
	@Nullable
	public Object next() {
		Object part = serializer.readPathPart(data);
		isList = data.get() == LIST;
		if (isList) {
			listSize = data.getInt();
		}
		return part;
	}
	
	public boolean isList() {
		return isList;
	}
	
	public int getListSize() {
		return listSize;
	}
}
