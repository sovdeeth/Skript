package ch.njol.skript.lang.parser.instrumentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A node in a parse call tree, recording one begin/end attempt.
 * Built during parsing and serialized to a compact JSON array format
 * when the owning top-level INPUT completes, then discarded.
 * <p>
 * The compact format uses a string table to intern labels, and
 * positional arrays instead of keyed objects:
 * <pre>[levelIdx, labelId, elapsedNs, selfNs, success, [child, ...]]</pre>
 * where {@code labelId} is an index into the string table, and
 * {@code success} is 1 or 0.
 * <p>
 * The string table is maintained by the owning {@link ParserInstrumentation}
 * and written as the first line of the output file.
 */
public final class ParseNode {

	private final int levelOrdinal;
	private final int labelId;
	private long elapsedNs;
	private long selfNs;
	private boolean success;
	private List<ParseNode> children;

	ParseNode(int levelOrdinal, int labelId) {
		this.levelOrdinal = levelOrdinal;
		this.labelId = labelId;
	}

	void complete(long elapsedNs, long selfNs, boolean success) {
		this.elapsedNs = elapsedNs;
		this.selfNs = selfNs;
		this.success = success;
	}

	void addChild(ParseNode child) {
		if (children == null)
			children = new ArrayList<>();
		children.add(child);
	}

	/**
	 * Appends this node in compact array format to the given StringBuilder.
	 */
	void appendCompact(StringBuilder sb) {
		sb.append('[');
		sb.append(levelOrdinal).append(',');
		sb.append(labelId).append(',');
		sb.append(elapsedNs).append(',');
		sb.append(selfNs).append(',');
		sb.append(success ? 1 : 0);
		if (children != null && !children.isEmpty()) {
			sb.append(",[");
			for (int i = 0; i < children.size(); i++) {
				if (i > 0) sb.append(',');
				children.get(i).appendCompact(sb);
			}
			sb.append(']');
		}
		sb.append(']');
	}

	/**
	 * A string table that maps labels to integer IDs for compact serialization.
	 */
	static final class StringTable {
		private final Map<String, Integer> map = new HashMap<>();
		private final List<String> list = new ArrayList<>();

		int intern(String s) {
			if (s == null) return -1;
			Integer id = map.get(s);
			if (id != null) return id;
			id = list.size();
			map.put(s, id);
			list.add(s);
			return id;
		}

		/**
		 * Serializes the string table as a JSON array of strings.
		 */
		void appendJson(StringBuilder sb) {
			sb.append('[');
			for (int i = 0; i < list.size(); i++) {
				if (i > 0) sb.append(',');
				appendJsonString(sb, list.get(i));
			}
			sb.append(']');
		}

		int size() {
			return list.size();
		}

		private static void appendJsonString(StringBuilder sb, String s) {
			sb.append('"');
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				switch (c) {
					case '"' -> sb.append("\\\"");
					case '\\' -> sb.append("\\\\");
					case '\n' -> sb.append("\\n");
					case '\r' -> sb.append("\\r");
					case '\t' -> sb.append("\\t");
					default -> {
						if (c < 0x20) {
							sb.append(String.format("\\u%04x", (int) c));
						} else {
							sb.append(c);
						}
					}
				}
			}
			sb.append('"');
		}
	}

}
