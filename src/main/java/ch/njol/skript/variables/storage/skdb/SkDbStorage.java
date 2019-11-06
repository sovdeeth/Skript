package ch.njol.skript.variables.storage.skdb;

import java.io.FileOutputStream;
import java.io.IOException;
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
import ch.njol.skript.variables.storage.VariableStorage;

/**
 * A specialized database built for Skript variables.
 */
public class SkDbStorage implements VariableStorage {
	
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
	
	/**
	 * Database metadata.
	 */
	private Metadata meta;
	
	private SkDbStorage(FileChannel ch, Metadata meta) {
		this.readCh = ch;
		this.meta = meta;
	}

	@Override
	public void variableChanged(VariablePath path, @Nullable Object newValue) {
		// TODO journal
	}

	@Override
	public int estimatedSize() {
		return meta.variableCount;
	}

	@Override
	public void loadVariables(VariableScope scope) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
