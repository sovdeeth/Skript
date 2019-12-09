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
		// TODO don't hardcode limits
		int[] listRemaining = new int[128];
		listRemaining[0] = variableCount; // Treat root as a list here, even though it really isn't one
		Object[] listNames = new Object[128];
		int listIndex = 0;
		for (int i = 0; i < variableCount; i++) {
			if (listRemaining[listIndex]-- == 0) { // Go to parent list, to root or return
				if (listIndex == 0) {
					return; // Visited everything
				}
				visitor.listEnd(listNames[listIndex]); // Gone up one list
				listIndex--;
			}
			
			// List start or a singular value
			Object name = serializer.readPathPart(data);
			boolean isList = data.get() == LIST;
			int size = data.getInt(); // List size (in elements) or variable size (in bytes)
			if (isList) {
				visitor.listStart(name, size, false); // TODO read isArray status
				
				// Record size and name of this list
				listIndex++;
				listRemaining[listIndex] = size;
				listNames[listIndex] = name;
			} else { // Tell visitor it can now read a value
				visitor.value(name, size);
			}
		}
	}
	
	public interface Visitor {
		
		void value(Object name, int size);
		
		void listStart(Object name, int size, boolean isArray);
		
		void listEnd(Object name);
	}
}
