package ch.njol.skript.lang.parser.instrumentation;

/**
 * An aggregated timing record that combines all attempts sharing the same label.
 * Tracks both inclusive (total wall-clock) and self (exclusive of children) time,
 * split by success and failure, along with attempt counts.
 * <p>
 * Inclusive time is only recorded for outermost invocations of a label
 * (to avoid double-counting recursive calls), so inclusive counts and
 * averages use a separate counter from self-time counts.
 */
public final class TimingEntry implements Comparable<TimingEntry> {

	private final String label;

	// Inclusive time (wall-clock, including children)
	// Only recorded for outermost invocations of this label.
	private long inclusiveNs;
	private long inclusiveSuccessNs;
	private long inclusiveFailNs;
	private long inclusiveCount;
	private long inclusiveSuccessCount;
	private long inclusiveFailCount;

	// Self time (exclusive of children)
	// Recorded for every invocation.
	private long selfNs;
	private long selfSuccessNs;
	private long selfFailNs;

	// Total attempt counts (every invocation, regardless of nesting)
	private long count;
	private long successCount;
	private long failCount;

	// Number of INPUT-level parse attempts spawned underneath this label's attempts
	private long childInputCount;

	public TimingEntry(String label) {
		this.label = label;
	}

	/**
	 * Records a single attempt.
	 *
	 * @param elapsedNs Wall-clock time inclusive of all child timings.
	 *                  Pass 0 for nested (non-outermost) invocations to
	 *                  avoid double-counting.
	 * @param selfNs Time exclusive of children (always recorded).
	 * @param success Whether this attempt succeeded.
	 * @param childInputCount Number of INPUT-level parse attempts spawned underneath.
	 */
	public void record(long elapsedNs, long selfNs, boolean success, long childInputCount) {
		this.selfNs += selfNs;
		this.childInputCount += childInputCount;
		count++;

		if (elapsedNs > 0) {
			inclusiveNs += elapsedNs;
			inclusiveCount++;
			if (success) {
				inclusiveSuccessNs += elapsedNs;
				inclusiveSuccessCount++;
			} else {
				inclusiveFailNs += elapsedNs;
				inclusiveFailCount++;
			}
		}

		if (success) {
			selfSuccessNs += selfNs;
			successCount++;
		} else {
			selfFailNs += selfNs;
			failCount++;
		}
	}

	public String label() {
		return label;
	}

	// --- Inclusive (wall-clock) time ---
	// Only counts outermost invocations to avoid recursive double-counting.

	/** Cumulative wall-clock time across outermost attempts (inclusive of children). */
	public long inclusiveNs() {
		return inclusiveNs;
	}

	public long inclusiveSuccessNs() {
		return inclusiveSuccessNs;
	}

	public long inclusiveFailNs() {
		return inclusiveFailNs;
	}

	public double averageInclusiveNs() {
		return inclusiveCount == 0 ? 0 : (double) inclusiveNs / inclusiveCount;
	}

	public double averageInclusiveSuccessNs() {
		return inclusiveSuccessCount == 0 ? 0 : (double) inclusiveSuccessNs / inclusiveSuccessCount;
	}

	public double averageInclusiveFailNs() {
		return inclusiveFailCount == 0 ? 0 : (double) inclusiveFailNs / inclusiveFailCount;
	}

	/** Number of outermost invocations (those that contributed inclusive time). */
	public long inclusiveCount() {
		return inclusiveCount;
	}

	// --- Self time ---
	// Recorded for every invocation.

	/** Cumulative self-time across all attempts (exclusive of children). */
	public long selfNs() {
		return selfNs;
	}

	public long selfSuccessNs() {
		return selfSuccessNs;
	}

	public long selfFailNs() {
		return selfFailNs;
	}

	public double averageSelfNs() {
		return count == 0 ? 0 : (double) selfNs / count;
	}

	public double averageSelfSuccessNs() {
		return successCount == 0 ? 0 : (double) selfSuccessNs / successCount;
	}

	public double averageSelfFailNs() {
		return failCount == 0 ? 0 : (double) selfFailNs / failCount;
	}

	// --- Counts ---

	/** Total number of attempts (including nested recursive ones). */
	public long count() {
		return count;
	}

	public long successCount() {
		return successCount;
	}

	public long failCount() {
		return failCount;
	}

	// --- Child spawns ---

	/** Total number of INPUT-level parse attempts spawned underneath all invocations. */
	public long childInputCount() {
		return childInputCount;
	}

	public double averageChildInputCount() {
		return count == 0 ? 0 : (double) childInputCount / count;
	}

	/**
	 * Natural ordering by total inclusive time ascending.
	 */
	@Override
	public int compareTo(TimingEntry other) {
		return Long.compare(this.inclusiveNs, other.inclusiveNs);
	}

}
