package ch.njol.skript.variables.storage.skdb;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.variables.VariablePath;
import ch.njol.skript.variables.VariableScope;
import ch.njol.skript.variables.serializer.FieldsReader;
import ch.njol.skript.variables.storage.VariableStorage;

/**
 * A specialized database built for Skript variables.
 */
public class SkDbStorage implements VariableStorage {
	
	public static SkDbStorage open(Path file) throws IOException {
		FileChannel ch = FileChannel.open(file);
		Metadata meta = new Metadata(ch, 0);
		// TODO
	}
	
	/**
	 * File channel for reading database content. Not used for writing, as this
	 * is a journaling database.
	 */
	private FileChannel readCh;
	
	private static class Metadata {
		
		/**
		 * Data version. May be used if DB format changes in future for
		 * migrations.
		 */
		public int dataVersion;
		
		/**
		 * Indicates if this database was cleanly closed last time it was used.
		 */
		public boolean dirty;
		
		/**
		 * Amount of variables in DB. May be inaccurate if DB is
		 * {@link #dirty}, because variables are still in journal.
		 */
		public int variableCount;
		
		public Metadata() {
			this.dataVersion = 0;
			this.dirty = true;
			this.variableCount = 0;
		}
		
		public Metadata(FileChannel ch, long pos) throws IOException {
			ByteBuffer buf = ByteBuffer.allocate(9);
			ch.read(buf, pos);
			this.dataVersion = buf.getInt();
			this.dirty = buf.get() == 1;
			this.variableCount = buf.getInt();
		}
		
		public void write(FileChannel ch, long pos) {
			ByteBuffer buf = ByteBuffer.allocate(9);
			buf.putInt(dataVersion);
			buf.put((byte) (dirty ? 1 : 0));
			buf.putInt(variableCount);
		}
	}
	
	/**
	 * Database metadata.
	 */
	private final Metadata meta;
	
	/**
	 * Journal of this database.
	 */
	private final Journal journal;
	
	/**
	 * Deserializes user data from database.
	 */
	private final FieldsReader fieldsReader;
	
	/**
	 * Variables currently in journal, waiting to be written.
	 */
	private int journalSize;
	
	private SkDbStorage(FileChannel ch, Metadata meta, Journal journal) {
		this.readCh = ch;
		this.meta = meta;
		this.journal = journal;
		this.fieldsReader = new FieldsReader();
	}

	@Override
	public void variableChanged(@Nullable VariablePath path, Object name, @Nullable Object newValue) {
		try {
			journal.variableChanged(path, name, newValue);
		} catch (StreamCorruptedException e) {
			// TODO handle this, but how?
			e.printStackTrace();
		}
		journalSize++;
		// TODO process journal once it grows large enough
	}
	
	public void commitChanges() {
		journal.commitChanges();
	}

	@Override
	public int estimatedSize() {
		return meta.variableCount;
	}

	@Override
	public void loadVariables(VariableScope scope) throws IOException {
		MappedByteBuffer buf = readCh.map(MapMode.READ_ONLY,
				readCh.position(), readCh.size() - readCh.position());
		// TODO deal with mmap issues (file can't be deleted and other fun stuff)
		assert buf != null;
		DatabaseReader reader = new DatabaseReader(buf);
		
		/**
		 * Array with path parts.
		 */
		Object[] pathArray = new Object[128]; // TODO figure out what is max path length
		
		/**
		 * For each list in path, how many elements have not been read.
		 */
		int[] listRemaining = new int[pathArray.length];
		listRemaining[0] = meta.variableCount; // For root, it is variable count from meta
		int pathIndex = 0; // Start at root
		outer: for (Object pathPart = reader.next();; pathPart = reader.next()) {
			pathArray[pathIndex] = pathPart;
			// TODO consider optimizing for simple lists (basically arrays)
			if (reader.isList()) {
				pathIndex++;
				listRemaining[pathIndex] = reader.getListSize();
			} else { // Normal value
				// Create path by copying used parts of pathArray
				Object[] pathCopy = new Object[pathIndex + 1];
				System.arraycopy(pathArray, 0, pathCopy, 0, pathIndex + 1);
				VariablePath path = VariablePath.create(pathCopy);
				
				Object value = fieldsReader.read(buf); // Deserialize value
				assert value != null : "database file should not store null values";
				scope.set(path, null, value); // TODO something faster?
				
				// Go towards root if we're out of list members
				while (true) { // Root doesn't have this limitation
					int remaining = --listRemaining[pathIndex];
					if (remaining == 0) { // This list is out of members, go up
						if (--pathIndex == -1) { // Root run out of members!
							break outer; // Finish reading
						}
					} else { // List still has members, continue reading them
						break;
					}
				}
			}
		}
	}
}
