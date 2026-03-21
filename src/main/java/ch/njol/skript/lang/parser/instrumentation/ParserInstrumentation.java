package ch.njol.skript.lang.parser.instrumentation;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates parse timing data with bounded memory usage.
 * Tracks inclusive time (wall-clock), self time (exclusive of children),
 * success/failure counts, and per-label aggregated entries for each parse level.
 * <p>
 * Because parsing is recursive (e.g. parsing an expression within a pattern
 * triggers a new input→syntax→pattern chain), a global nesting stack is used
 * to compute self-time (total minus time spent in child timings).
 * <p>
 * All attempts sharing the same label (e.g. the same SyntaxInfo class name
 * or the same pattern string) are combined into a single {@link TimingEntry}.
 */
public class ParserInstrumentation {

	/** The parse levels tracked by this instrumentation. */
	public enum Level {
		SCRIPT,
		INPUT,
		SYNTAX,
		PATTERN,
		/** Time spent in {@code parse_i} (pattern matching). */
		MATCH,
		/** Time spent in {@code element.init()}. */
		INIT
	}

	private static final int LEVEL_COUNT = Level.values().length;

	private final int keepLimit;

	// Per-level aggregated stats
	private final long[] totalInclusiveNs = new long[LEVEL_COUNT];
	private final long[] totalSelfNs = new long[LEVEL_COUNT];
	private final long[] count = new long[LEVEL_COUNT];

	// Per-level maps: label → aggregated TimingEntry
	private final Map<String, TimingEntry>[] entries;

	// Per-level nesting depth per label.
	// Tracks how many active (begun but not ended) timings exist for each
	// (level, label) pair. Inclusive time is only recorded into the
	// TimingEntry when depth drops to 0, preventing double-counting when
	// the same label appears recursively (e.g. parsing "foo" triggers a
	// child parse that also tries "foo").
	private final Map<String, Integer>[] labelDepth;

	// Global nesting stack for self-time computation.
	private static final class ActiveTiming {
		final Level level;
		final long startNs;
		final String label;
		long childTimeNs; // mutable: accumulated time of direct children
		long childInputCount; // mutable: number of INPUT attempts spawned underneath

//		// Child composition: which INPUT/PATTERN labels appeared underneath, with [count, inclusiveNs]
//		Map<String, long[]> childInputStats;
//		Map<String, long[]> childPatternStats;

		ActiveTiming(Level level, long startNs, String label) {
			this.level = level;
			this.startNs = startNs;
			this.label = label;
		}

//		void addChildInput(String label, long inclusiveNs) {
//			if (childInputStats == null)
//				childInputStats = new HashMap<>();
//			childInputStats.merge(label, new long[]{1, inclusiveNs},
//				(old, v) -> { old[0]++; old[1] += v[1]; return old; });
//		}
//
//		void addChildPattern(String label, long inclusiveNs) {
//			if (childPatternStats == null)
//				childPatternStats = new HashMap<>();
//			childPatternStats.merge(label, new long[]{1, inclusiveNs},
//				(old, v) -> { old[0]++; old[1] += v[1]; return old; });
//		}
	}

	private final Deque<ActiveTiming> globalStack = new ArrayDeque<>();

	// --- Call tree recording ---
	// A parallel stack of ParseNode references tracks the current position
	// in the tree being built. When a top-level INPUT ends, the tree is
	// serialized to the output file and discarded.
	private final Deque<ParseNode> nodeStack = new ArrayDeque<>();
	private final ParseNode.StringTable stringTable = new ParseNode.StringTable();
	private Path traceOutputPath;
	private boolean headerWritten;
	private int inputDepth; // tracks INPUT nesting to identify top-level

	@SuppressWarnings("unchecked")
	public ParserInstrumentation(int keepLimit) {
		this.keepLimit = keepLimit;
		this.entries = new Map[LEVEL_COUNT];
		this.labelDepth = new Map[LEVEL_COUNT];
		for (int i = 0; i < LEVEL_COUNT; i++) {
			entries[i] = new HashMap<>();
			labelDepth[i] = new HashMap<>();
		}
	}

	public ParserInstrumentation() {
		this(5);
	}

	/**
	 * Enables call tree recording. Each top-level INPUT parse will have its
	 * full call tree serialized as a JSON line to the given file.
	 */
	public void setTraceOutputPath(Path path) {
		this.traceOutputPath = path;
	}

	// --- Begin/End methods ---

	public void beginScript(String label) {
		begin(Level.SCRIPT, label);
	}

	public void endScript(boolean success) {
		end(Level.SCRIPT, success);
	}

	public void beginInput(String expr) {
		begin(Level.INPUT, expr);
	}

	public void endInput(boolean success) {
		end(Level.INPUT, success);
	}

	public void beginSyntax(String label) {
		begin(Level.SYNTAX, label);
	}

	public void endSyntax(boolean success) {
		end(Level.SYNTAX, success);
	}

	public void beginPattern(String pattern) {
		begin(Level.PATTERN, pattern);
	}

	public void endPattern(boolean success) {
		end(Level.PATTERN, success);
	}

	public void beginMatch(String pattern) {
		begin(Level.MATCH, pattern);
	}

	public void endMatch(boolean success) {
		end(Level.MATCH, success);
	}

	public void beginInit(String label) {
		begin(Level.INIT, label);
	}

	public void endInit(boolean success) {
		end(Level.INIT, success);
	}

	private void begin(Level level, String label) {
		globalStack.push(new ActiveTiming(level, System.nanoTime(), label));
		if (label != null) {
			labelDepth[level.ordinal()].merge(label, 1, Integer::sum);
		}

		// Call tree recording
		if (traceOutputPath != null) {
			if (level == Level.INPUT)
				inputDepth++;
			ParseNode node = new ParseNode(level.ordinal(), stringTable.intern(label));
			if (!nodeStack.isEmpty()) {
				nodeStack.peek().addChild(node);
			}
			nodeStack.push(node);
		}
	}

	private void end(Level level, boolean success) {
		if (globalStack.isEmpty())
			return;

		ActiveTiming timing = globalStack.peek();
		// Safety: if the top doesn't match the expected level, bail out.
		if (timing.level != level)
			return;

		globalStack.pop();

		long elapsed = System.nanoTime() - timing.startNs;
		long self = elapsed - timing.childTimeNs;

		int idx = level.ordinal();
		totalInclusiveNs[idx] += elapsed;
		totalSelfNs[idx] += self;
		count[idx]++;

		// Attribute our total elapsed time as child time of our parent
		if (!globalStack.isEmpty()) {
			globalStack.peek().childTimeNs += elapsed;
		}

		// If this was an INPUT attempt, count it as a child input for all ancestors
		if (level == Level.INPUT) {
			for (ActiveTiming ancestor : globalStack) {
				ancestor.childInputCount++;
			}
		}

//		// Record INPUT/PATTERN child composition into all ancestors
//		if (timing.label != null) {
//			if (level == Level.INPUT) {
//				for (ActiveTiming ancestor : globalStack) {
//					ancestor.addChildInput(timing.label, elapsed);
//				}
//			} else if (level == Level.PATTERN) {
//				for (ActiveTiming ancestor : globalStack) {
//					ancestor.addChildPattern(timing.label, elapsed);
//				}
//			}
//		}

		// Aggregate into per-label entry
		if (timing.label != null) {
			int depth = labelDepth[idx].merge(timing.label, -1, Integer::sum);
			// Only record inclusive time for the outermost invocation of this
			// label to avoid double-counting recursive calls with the same label.
			boolean outermost = depth == 0;
			if (outermost) {
				labelDepth[idx].remove(timing.label);
			}
			entries[idx].computeIfAbsent(timing.label, TimingEntry::new)
				.record(outermost ? elapsed : 0, self, success, timing.childInputCount);
		}

		// Call tree recording
		if (traceOutputPath != null) {
			if (!nodeStack.isEmpty()) {
				ParseNode node = nodeStack.pop();
				node.complete(elapsed, self, success);
				// Flush to disk when the top-level INPUT completes
				if (level == Level.INPUT) {
					inputDepth--;
					if (inputDepth == 0 && nodeStack.isEmpty()) {
						flushTree(node);
					}
				}
			}
		}
	}

	private void flushTree(ParseNode root) {
		try (Writer writer = Files.newBufferedWriter(traceOutputPath,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
			if (!headerWritten) {
				writeHeader(writer);
				headerWritten = true;
			}
			StringBuilder sb = new StringBuilder(4096);
			root.appendCompact(sb);
			sb.append('\n');
			writer.write(sb.toString());
		} catch (IOException e) {
			System.err.println("[Skript] Failed to write parse trace: " + e.getMessage());
		}
	}

	/**
	 * Writes the string table as a header. The string table grows over the
	 * session, so we rewrite the full file on each flush: header + all trees.
	 * <p>
	 * Actually, since the string table grows incrementally and rewriting would
	 * be expensive, we instead write the string table at the end via
	 * {@link #finalizeTrace()}.
	 */
	private void writeHeader(Writer writer) throws IOException {
		// Write a placeholder line indicating format version.
		// The string table is written at the end by finalizeTrace().
		writer.write("{\"format\":\"skript-parse-trace\",\"version\":1,");
		writer.write("\"levels\":[");
		Level[] levels = Level.values();
		for (int i = 0; i < levels.length; i++) {
			if (i > 0) writer.write(',');
			writer.write('"');
			writer.write(levels[i].name());
			writer.write('"');
		}
		writer.write("],\"nodeFormat\":[\"level\",\"labelId\",\"elapsedNs\",\"selfNs\",\"success\",\"children\"]}\n");
	}

	/**
	 * Finalizes the trace output by appending the string table as the last line.
	 * Must be called after all parsing is complete (e.g. in getSummary or explicitly).
	 */
	public void finalizeTrace() {
		if (traceOutputPath == null || !headerWritten)
			return;
		try (Writer writer = Files.newBufferedWriter(traceOutputPath,
				StandardOpenOption.APPEND)) {
			StringBuilder sb = new StringBuilder();
			sb.append("{\"strings\":");
			stringTable.appendJson(sb);
			sb.append("}\n");
			writer.write(sb.toString());
		} catch (IOException e) {
			System.err.println("[Skript] Failed to finalize parse trace: " + e.getMessage());
		}
	}

	// --- Query methods ---

	public long totalInclusiveNs(Level level) {
		return totalInclusiveNs[level.ordinal()];
	}

	public long totalSelfNs(Level level) {
		return totalSelfNs[level.ordinal()];
	}

	public long count(Level level) {
		return count[level.ordinal()];
	}

	/**
	 * Returns the top-N entries for the given level sorted by the given comparator (descending).
	 * Each entry aggregates all attempts with the same label.
	 */
	public List<TimingEntry> topEntries(Level level, Comparator<TimingEntry> comparator) {
		List<TimingEntry> list = new ArrayList<>(entries[level.ordinal()].values());
		list.sort(comparator.reversed());
		if (list.size() > keepLimit)
			list = list.subList(0, keepLimit);
		return list;
	}

	/** Top-N by total inclusive time descending. */
	public List<TimingEntry> topByTotalInclusiveTime(Level level) {
		return topEntries(level, Comparator.comparingLong(TimingEntry::inclusiveNs));
	}

	/** Top-N by average inclusive time descending. */
	public List<TimingEntry> topByAverageInclusiveTime(Level level) {
		return topEntries(level, Comparator.comparingDouble(TimingEntry::averageInclusiveNs));
	}

	/** Top-N by total self-time descending. */
	public List<TimingEntry> topByTotalSelfTime(Level level) {
		return topEntries(level, Comparator.comparingLong(TimingEntry::selfNs));
	}

	/** Top-N by average self-time descending. */
	public List<TimingEntry> topByAverageSelfTime(Level level) {
		return topEntries(level, Comparator.comparingDouble(TimingEntry::averageSelfNs));
	}

	/** Top-N by attempt count descending. */
	public List<TimingEntry> topByCount(Level level) {
		return topEntries(level, Comparator.comparingLong(TimingEntry::count));
	}

	/** Top-N by total wasted inclusive time (inclusive time on failed attempts) descending. */
	public List<TimingEntry> topByWastedTime(Level level) {
		return topEntries(level, Comparator.comparingLong(TimingEntry::inclusiveFailNs));
	}

	/** Top-N by average child input parse count descending. */
	public List<TimingEntry> topByChildInputs(Level level) {
		return topEntries(level, Comparator.comparingDouble(TimingEntry::averageChildInputCount));
	}

	/**
	 * Returns a human-readable summary of all levels.
	 */
	public String getSummary() {
		finalizeTrace();
		StringBuilder sb = new StringBuilder("=== Parser Instrumentation Summary ===\n");
		for (Level level : Level.values()) {
			int idx = level.ordinal();
			if (count[idx] == 0)
				continue;
			sb.append(level.name())
				.append(": inclusive=").append(formatMs(totalInclusiveNs[idx]))
				.append("ms, self=").append(formatMs(totalSelfNs[idx]))
				.append("ms, count=").append(count[idx])
				.append('\n');

			appendRanking(sb, "By total inclusive time", topByTotalInclusiveTime(level));
			appendRanking(sb, "By avg inclusive time", topByAverageInclusiveTime(level));
			appendRanking(sb, "By total self-time", topByTotalSelfTime(level));
			appendRanking(sb, "By avg self-time", topByAverageSelfTime(level));
			appendRanking(sb, "By attempt count", topByCount(level));
			appendRanking(sb, "By wasted inclusive time (failed)", topByWastedTime(level));
			appendRanking(sb, "By avg child input parses", topByChildInputs(level));
		}
		return sb.toString();
	}

	private void appendRanking(StringBuilder sb, String heading, List<TimingEntry> entries) {
		if (entries.isEmpty())
			return;
		sb.append("  ").append(heading).append(":\n");
		for (TimingEntry entry : entries) {
			sb.append("    ")
				.append(formatMs(entry.inclusiveNs())).append("ms incl, ")
				.append(formatMs(entry.averageInclusiveNs())).append("ms avg incl, ")
				.append(formatMs(entry.selfNs())).append("ms self, ")
				.append(formatMs(entry.averageSelfNs())).append("ms avg self, ")
				.append(entry.successCount()).append(" ok / ")
				.append(entry.failCount()).append(" fail (")
				.append(entry.count()).append(" total), ")
				.append(formatMs(entry.inclusiveFailNs())).append("ms wasted incl, ")
				.append(entry.childInputCount()).append(" child inputs (")
				.append(String.format("%.1f", entry.averageChildInputCount())).append(" avg)")
				.append(" - ").append(truncate(entry.label(), 100))
				.append('\n');
//			appendChildComposition(sb, "Top child inputs", entry.topChildInputs(keepLimit));
//			appendChildComposition(sb, "Top child patterns", entry.topChildPatterns(keepLimit));
		}
	}

//	private static void appendChildComposition(StringBuilder sb, String heading, List<TimingEntry.ChildStat> stats) {
//		if (stats.isEmpty())
//			return;
//		sb.append("      ").append(heading).append(":\n");
//		for (TimingEntry.ChildStat stat : stats) {
//			sb.append("        ")
//				.append(stat.count()).append("x, ")
//				.append(formatMs(stat.inclusiveNs())).append("ms incl")
//				.append(" - ").append(truncate(stat.label(), 80))
//				.append('\n');
//		}
//	}

	private static String formatMs(long ns) {
		return String.format("%.2f", ns / 1_000_000.0);
	}

	private static String formatMs(double ns) {
		return String.format("%.4f", ns / 1_000_000.0);
	}

	private static String truncate(String s, int maxLen) {
		return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
	}

}
