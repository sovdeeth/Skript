package ch.njol.skript.variables.storage.skdb;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.variables.VariablePath;
import ch.njol.skript.variables.serializer.FieldsWriter;

/**
 * SkDb's change journal. All changes are immediately written to a
 * memory-mapped file, and metadata of them is kept in memory to allow
 * rewriting the database file fast. In-memory metadata can be re-created
 * from the journal file contents, so a crash or a power loss is not likely
 * to cause (much) data loss.
 */
public class Journal {
	
	/**
	 * Serializer for database's internal data types.
	 */
	private final DatabaseSerializer dbSerializer;
	
	/**
	 * Serializer for user-provided data.
	 */
	private final FieldsWriter userSerializer;
	
	/**
	 * Current journal buffer.
	 */
	private final MappedByteBuffer journalBuf;
	
	private static class ChangedVariable {
		
		/**
		 * Start of serialized data in journal buffer.
		 */
		public final int position;
		
		/**
		 * Size of serialized data.
		 */
		public final int length;
		
		public ChangedVariable(int position, int length) {
			this.position = position;
			this.length = length;
		}
	}
	
	private static class VariableTree {
		
		/**
		 * If there are other variables under this, they are stored here.
		 */
		public final Map<Object, VariableTree> contents;
		
		/**
		 * Changed variable. Null if this exists only because some variables
		 * under this were changed.
		 */
		@Nullable
		public ChangedVariable value;
		
		public VariableTree() {
			this.contents = new HashMap<>();
		}
	}
	
	/**
	 * A tree of changed variables.
	 */
	private final VariableTree root;
	
	public Journal(DatabaseSerializer dbSerializer, FieldsWriter userSerializer, Path file, int size) throws IOException {
		this.dbSerializer = dbSerializer;
		this.userSerializer = userSerializer;
		this.journalBuf = FileChannel.open(file).map(MapMode.READ_WRITE, 0, size);
		this.root = new VariableTree();
	}
	
	/**
	 * Adds a new variable to the journal. It is not yet committed; it is
	 * written to backing file, but will not be used in case a crash occurs
	 * before {@link #commitChanges()} is called.
	 * @param path Variable path, or null for variables at root.
	 * @param name Name of variable.
	 * @param newValue New value for the variable. Null is allowed, and causes
	 * the variable to be eventually deleted from the database.
	 * @throws StreamCorruptedException When a serialization failure occurs.
	 */
	public void variableChanged(@Nullable VariablePath path, Object name, @Nullable Object newValue) throws StreamCorruptedException {
		// Write full path to variable (path to list, name)
		if (path != null) {
			dbSerializer.writePath(path, journalBuf);
		}
		dbSerializer.writePathPart(name, journalBuf);
		
		// Write variable content
		int start = journalBuf.position();
		userSerializer.write(journalBuf, newValue);
		int size = journalBuf.position() - start;
		
		// Find or create this variable in change tree
		VariableTree var = root;
		if (path != null) {
			for (Object part : path) {
				var = var.contents.computeIfAbsent(part, k -> new VariableTree());
			}
		}
		var = var.contents.computeIfAbsent(name, k -> new VariableTree());
		
		// (Over)write in-memory representation of data
		var.value = new ChangedVariable(start, size);
	}
	
	/**
	 * Commits all changes made with
	 * {@link #variableChanged(VariablePath, Object, Object)} that
	 * have not yet been committed.
	 */
	public void commitChanges() {
		journalBuf.putInt(0, journalBuf.position());
	}
}
