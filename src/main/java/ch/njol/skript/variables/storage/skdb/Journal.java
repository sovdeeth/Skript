package ch.njol.skript.variables.storage.skdb;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

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
	final DatabaseSerializer dbSerializer;
	
	/**
	 * Serializer for user-provided data.
	 */
	private final FieldsWriter userSerializer;
	
	/**
	 * Current journal buffer.
	 */
	final MappedByteBuffer journalBuf;
	
	/**
	 * Reference to a serialized variable.
	 */
	private static class SerializedVariable {
		
		/**
		 * Start of serialized data in journal buffer.
		 * Immediately before it is the variable path.
		 */
		public final int position;
		
		/**
		 * Size of serialized data.
		 */
		public final int length;
		
		public SerializedVariable(int position, int length) {
			this.position = position;
			this.length = length;
		}
	}
	
	/**
	 * Changed variables in a list or at root
	 */
	private static class ChangedVariables {
		
		/**
		 * Changed variables by their names.
		 */
		public final Map<Object, SerializedVariable> changes;
		
		/**
		 * If this is set, old contents of list are deleted before
		 * applying changes.
		 */
		public boolean deleteBefore;
		
		public ChangedVariables() {
			this.changes = new HashMap<>();
		}
	}
	
	/**
	 * Changed variables cached in memory for fast application.
	 */
	final Map<VariablePath, ChangedVariables> changes;
	
	public Journal(DatabaseSerializer dbSerializer, FieldsWriter userSerializer, Path file, int size) throws IOException {
		this.dbSerializer = dbSerializer;
		this.userSerializer = userSerializer;
		this.journalBuf = FileChannel.open(file).map(MapMode.READ_WRITE, 0, size);
		this.changes = new HashMap<>();
	}
	
	/**
	 * Adds a new variable to the journal. It is not yet committed; it is
	 * written to backing file, but will not be used in case a crash occurs
	 * before {@link #commitChanges()} is called.
	 * @param path Variable path, or null for variables at root.
	 * @param name Name of variable.
	 * @param newValue New value for the variable. Null is allowed, and causes
	 * the variable to be eventually deleted from the database.
	 * @param isList If a list variable was targeted.
	 * @throws StreamCorruptedException When a serialization failure occurs.
	 */
	public void variableChanged(@Nullable VariablePath path, Object name, @Nullable Object newValue, boolean isList) throws StreamCorruptedException {
		// Write full path to variable (path to list, name)
		if (path != null) {
			dbSerializer.writePath(path, journalBuf);
		}
		dbSerializer.writePathPart(name, journalBuf);
		
		// Write variable content
		int start = journalBuf.position();
		userSerializer.write(journalBuf, newValue);
		int size = journalBuf.position() - start;
		
		// Store path, name and serialized change waiting for next flush
		changes.computeIfAbsent(path, k -> new ChangedVariables()).changes.put(name, new SerializedVariable(start, size));
	}
	
	/**
	 * Commits all changes made with
	 * {@link #variableChanged(VariablePath, Object, Object)} that
	 * have not yet been committed.
	 */
	public void commitChanges() {
		// If we crash, variables added before this will now be recovered
		journalBuf.putInt(0, journalBuf.position());
	}
	
	public void flush(ByteBuffer oldDb, int oldSize, ByteBuffer newDb) {
		DatabaseReader reader = new DatabaseReader(oldDb, oldSize);
		
		reader.visit(new DatabaseReader.Visitor() {
			
			/**
			 * Variables that changed directly under current list
			 */
			@Nullable
			public ChangedVariables changed;
			
			/**
			 * Where the final list size should be written at.
			 */
			public int sizeOffset;
			
			/**
			 * Incremented whenever something goes to the list.
			 * In the end, this is new size of the list.
			 */
			private int finalSize;
			
			/**
			 * Skip existing contents of this list.
			 */
			private boolean skipExisting;
						
			@Override
			public void value(Object name, int size) {
				if (skipExisting) {
					return;
				}
				
				newDb.put(DatabaseReader.VALUE); // It is not a list
				dbSerializer.writePathPart(name, newDb); // Variable name

				// Try to get a change from journal just for us by removing it
				ChangedVariables changed = this.changed;
				if (changed != null && changed.changes.containsKey(name)) { // Journal is overwriting the old value
					SerializedVariable value = changed.changes.remove(name);
					if (value != null) { // If null, copy nothing (i.e. delete)
						// Copy the new serialized value from journal
						newDb.putInt(value.length);
						newDb.put(journalBuf.duplicate().position(value.position).limit(value.position + value.length));
					}
				} else { // Just copy the old serialized variable
					newDb.putInt(size);
					newDb.put(oldDb.duplicate().limit(oldDb.position() + size));
				}
				finalSize++;
			}
			
			@Override
			public void listStart(VariablePath path, int size, boolean isArray) {
				newDb.put(DatabaseReader.LIST); // It is a list
				sizeOffset = newDb.position(); // We'll write size here later
				newDb.position(sizeOffset + 4);
				dbSerializer.writePath(path, newDb); // Path to list
				
				changed = changes.get(path); // What, if anything, has changed?
				ChangedVariables changed = this.changed;
				skipExisting = changed != null && changed.deleteBefore;
				// FIXME change this shallow delete to deep delete, because that is what happens in memory
				finalSize = 0; // Incremented when values are added
			}
			
			@Override
			public void listEnd(VariablePath path) {
				// Write completely new content to list
				ChangedVariables changed = this.changed;
				if (changed != null) {
					for (Map.Entry<Object, SerializedVariable> entry : changed.changes.entrySet()) {
						SerializedVariable value = entry.getValue();
						if (value != null) { // Don't write variables that were added, then deleted
							newDb.put(DatabaseReader.VALUE); // It is not a list
							dbSerializer.writePathPart(entry.getKey(), newDb); // Variable name
							newDb.putInt(value.length);
							newDb.put(journalBuf.duplicate().position(value.position).limit(value.position + value.length));
							finalSize++;
						}
					}
				}
				
				// Write final size
				newDb.putInt(sizeOffset, finalSize);
				
				// Next list ready to go!
			}
		});
	}
}
