package org.skriptlang.skript.log.runtime;

/**
 * Consumes runtime errors. Some use cases include printing errors to console or redirecting to a Discord channel.
 */
public interface RuntimeErrorConsumer {

	void printError(RuntimeError error);

	void printFrameOutput(Frame.FrameOutput output);

}
