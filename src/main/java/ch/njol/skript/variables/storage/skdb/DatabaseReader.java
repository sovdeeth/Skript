package ch.njol.skript.variables.storage.skdb;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.variables.VariablePath;

/**
 * Reads database files.
 */
public class DatabaseReader {
		
	/**
	 * Buffer of database content.
	 */
	private final ByteBuffer data;
	
	/**
	 * Exact count of variables in database.
	 */
	private final int variableCount;
	
	/**
	 * Serializes {@link VariablePath}s and their parts.
	 */
	private final DatabaseSerializer serializer;
	
	public DatabaseReader(ByteBuffer data, int variableCount) {
		this.data = data;
		this.variableCount = variableCount;
		this.serializer = new DatabaseSerializer();
	}
	
	public void visit(Visitor visitor) {
		/**
		 * How many values root or current list has remaining.
		 */
		int remaining = 0;
		
		/**
		 * Path to currently processed list. Null when at root.
		 */
		VariablePath path = null;
		
		for (int i = 0; i < variableCount; i++) {
			int size = data.getInt(); // List size (in elements) or variable size (in bytes)
			if (remaining == 0) { // New list begins
				visitor.listEnd(path);
				remaining = size;
				path = serializer.readPath(data);
				visitor.listStart(path, size, false); // TODO isArray for simple list optimizations
			}
			
			// Read a value
			Object name = serializer.readPathPart(data); // Name in list or at root
			visitor.value(name, size);
			
			remaining--;
		}
	}
	
	public interface Visitor {
		
		void value(Object name, int size);
		
		void listStart(VariablePath path, int size, boolean isArray);
		
		void listEnd(@Nullable VariablePath path);
	}
}
