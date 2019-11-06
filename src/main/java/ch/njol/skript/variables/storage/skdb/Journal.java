package ch.njol.skript.variables.storage.skdb;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.variables.VariablePath;

/**
 * Change journal of the database.
 */
public class Journal {
	
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
	
	public Journal(Path file, int size) throws IOException {
		this.journalBuf = FileChannel.open(file).map(MapMode.READ_WRITE, 0, size);
	}
	
	public void variableChanged(VariablePath path, @Nullable Object newValue) {
		// Find or create this variable in change tree
		VariableTree var = root;
		for (Object name : path) {
			var = var.contents.computeIfAbsent(name, (k) -> new VariableTree());
		}
		
		// Write our data to variable (not caring if we overwrite)
		var.value = null; // TODO serialized format
	}
}
