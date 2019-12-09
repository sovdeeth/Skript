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
	 * Variable change waiting to be {@link Journal#commitChanges() committed}.
	 */
	private static class VariableChange {
		
		/**
		 * Path to the variable's parent.
		 */
		@Nullable
		public final VariablePath path;
		
		/**
		 * Name of the variable.
		 */
		public Object name;
		
		/**
		 * Serialized variable value.
		 */
		public SerializedVariable value;
		
		public VariableChange(@Nullable VariablePath path, Object name, SerializedVariable value) {
			this.path = path;
			this.name = name;
			this.value = value;
		}
	}
	
	/**
	 * Changes waiting to be committed.
	 */
	private final Queue<VariableChange> waitingChanges;
	
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
		public SerializedVariable value;
		
		/**
		 * Total size is size of old values in list represented by this,
		 * plus new values we add that do not overwrite the old values.
		 */
		public int totalSize;
		
		public VariableTree() {
			this.contents = new HashMap<>();
		}
	}
	
	/**
	 * A tree of changed variables.
	 */
	final VariableTree root;
	
	public Journal(DatabaseSerializer dbSerializer, FieldsWriter userSerializer, Path file, int size) throws IOException {
		this.dbSerializer = dbSerializer;
		this.userSerializer = userSerializer;
		this.journalBuf = FileChannel.open(file).map(MapMode.READ_WRITE, 0, size);
		this.waitingChanges = new ArrayDeque<>();
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
		
		// Put path and reference to serialized waiting for next commit.
		waitingChanges.add(new VariableChange(path, name, new SerializedVariable(start, size)));
	}
	
	/**
	 * Commits all changes made with
	 * {@link #variableChanged(VariablePath, Object, Object)} that
	 * have not yet been committed.
	 */
	public void commitChanges() {
		// If we crash, variables added before this will now be recovered
		journalBuf.putInt(0, journalBuf.position());
		
		// Store all added variables in in-memory tree
		// This will make flushing the journal faster unless we crash
		VariableChange change;
		while ((change = waitingChanges.poll()) != null) {
			addToTree(change.path, change.name, change.value);
		}
	}
	
	private void addToTree(@Nullable VariablePath path, Object name, SerializedVariable value) {
		// Find or create this variable in change tree
		VariableTree var = root;
		if (path != null) {
			for (Object part : path) {
				var = var.contents.computeIfAbsent(part, k -> new VariableTree());
			}
		}
		var = var.contents.computeIfAbsent(name, k -> new VariableTree());
		
		// (Over)write in-memory representation of data
		var.value = value;
	}
	
	public void flush(ByteBuffer oldDb, int oldSize, ByteBuffer newDb) {
		DatabaseReader reader = new DatabaseReader(oldDb, oldSize);
		
		Deque<VariableTree> parents = new ArrayDeque<>();
		parents.push(root);
		reader.visit(new DatabaseReader.Visitor() {
			
			private VariableTree tree = root;
			
			/**
			 * Where to write final size of current list. It is not known
			 * in advance, because some new variables may replace old ones.
			 */
			private int listSizeOffset;
						
			@Override
			public void value(Object name, int size) {
				dbSerializer.writePathPart(name, newDb); // Variable name
				newDb.put(DatabaseReader.VALUE); // It is not a list
				
				VariableTree serialized = tree == null ? null : tree.contents.get(name);
				if (serialized != null && serialized.value != null) { // It has been overwritten!
					// Copy the new serialized value from journal
					SerializedVariable value = serialized.value;
					newDb.putInt(value.length);
					newDb.put(journalBuf.duplicate().position(value.position).limit(value.position + value.length));
					serialized.value = null; // We don't want to write this again
				} else { // Just copy the old serialized variable
					newDb.putInt(size);
					newDb.put(oldDb.duplicate().limit(oldDb.position() + size));
				}
				tree.totalSize++;
			}
			
			@Override
			public void listStart(Object name, int size, boolean isArray) {
				tree.totalSize++; // List is a member like anything else
				
				parents.push(tree);
				tree = tree.contents.remove(name); // May be null; in that case, we have nothing to add
				// TODO we're removing tree, but what if it has a shadow value?
				dbSerializer.writePathPart(name, newDb); // Variable name
				newDb.put(DatabaseReader.LIST); // It is a list
				
				// Remember offset of these 4 bytes, we'll write size later
				listSizeOffset = newDb.position();
				newDb.position(listSizeOffset + 4);
			}
			
			@Override
			public void listEnd(Object name) {
				// Add any remaining values from current list
				for (Map.Entry<Object, VariableTree> entry : tree.contents.entrySet()) {
					SerializedVariable value = entry.getValue().value;
					if (value != null) { // Add (shadow) value
						dbSerializer.writePathPart(entry.getKey(), newDb); // Variable name
						newDb.put(DatabaseReader.VALUE); // It is not a list
						newDb.putInt(value.length);
						newDb.put(journalBuf.duplicate().position(value.position).limit(value.position + value.length));
					}
					
					// TODO list contents of a new list need to be written
				}
				
				tree = parents.pop(); // Go back to parent
			}
		});
	}
}
