package ch.njol.skript.variables.storage.skdb;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.variables.VariablePath;

/**
 * Reads database files.
 */
public class DatabaseReader {
	
	static final byte VALUE = 0, LIST = 1;
	
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
		 * How many values current list has remaining.
		 */
		int listRemaining = 0;
		
		/**
		 * Path to currently processed list.
		 */
		VariablePath path = null;
		for (int i = 0; i < variableCount; i++) {
			if (listRemaining-- == 0) { // Go to parent list, to root or return
				assert path != null;
				visitor.listEnd(path); // Gone up one list
			}
			
			// List start or a singular value
			boolean isList = data.get() == LIST;
			int size = data.getInt(); // List size (in elements) or variable size (in bytes)
			if (isList) {
				listRemaining = size;
				path = serializer.readPath(data);
				visitor.listStart(path, size, false); // TODO read isArray status
			} else { // Tell visitor it can now read a value
				Object name = serializer.readPathPart(data);
				visitor.value(name, size);
			}
		}
	}
	
	public interface Visitor {
		
		void value(Object name, int size);
		
		void listStart(VariablePath path, int size, boolean isArray);
		
		void listEnd(VariablePath path);
	}
}
