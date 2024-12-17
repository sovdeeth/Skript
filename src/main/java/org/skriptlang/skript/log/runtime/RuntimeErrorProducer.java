package org.skriptlang.skript.log.runtime;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * A RuntimeErrorProducer can throw runtime errors in a standardized and controlled manner.
 * Only {@link SyntaxElement}s are intended to implement this interface.
 */
public interface RuntimeErrorProducer {

	// todo: try/catch, suppression when annotations are added

	/**
	 * Returns the source {@link Node} for any errors the implementing class emits.
	 * <br>
	 * Used for accessing the line contents via {@link Node#getKey()}
	 * and the line number via {@link Node#getLine()}.
	 * <br>
	 * A standard implementation is to store the Node during
	 * {@link SyntaxElement#init(Expression[], int, Kleenean, SkriptParser.ParseResult)}
	 * via {@link ParserInstance#getNode()}.
	 * @return The source that produced a runtime error.
	 */
	Node getNode();

	/**
	 * @return A source composed of the implementing {@link SyntaxElement} and the result of {@link #getNode()}
	 */
	@Contract(" -> new")
	private @NotNull ErrorSource getErrorSource() {
		return ErrorSource.fromNodeAndElement(getNode(), (SyntaxElement) this);
	}

	/**
	 * Gets the text that should be underlined within the line contents. This should match the text the user wrote that
	 * was parsed as the syntax that threw the runtime issue. For example, if the skull expression in
	 * {@code give skull of player to all players} throws a runtime error, this method should return
	 * {@code "skull of player"}
	 * <br>
	 * An example implementation for {@link Expression}s is to store {@link SkriptParser.ParseResult#expr} during
	 * {@link SyntaxElement#init(Expression[], int, Kleenean, SkriptParser.ParseResult)} and return that.
	 * <br>
	 * For other syntax types, this may vary. Effects, for example, may underline the whole line.
	 *
	 * @return The text to underline in the line that produced a runtime error. This may be null if no highlighting
	 * 			is desired or possible.
	 */
	@Nullable String toHighlight();

	/**
	 * Dispatches a runtime error with the given text.
	 * Metadata will be provided along with the message, including line number, the docs name of the producer,
	 * and the line content.
	 * <br>
	 * Implementations should ensure they call super() to print the error.
	 *
	 * @param message The text to display as the error message.
	 */
	default void error(String message) {
		Skript.getRuntimeErrorManager().error(
			new RuntimeError(Level.SEVERE, getErrorSource(), message, toHighlight())
		);
	}

	/**
	 * Dispatches a runtime warning with the given text.
	 * Metadata will be provided along with the message, including line number, the docs name of the producer,
	 * and the line content.
	 * <br>
	 * Implementations should ensure they call super() to print the warning.
	 *
	 * @param message The text to display as the error message.
	 */
	default void warning(String message) {
		Skript.getRuntimeErrorManager().warning(
			new RuntimeError(Level.WARNING, getErrorSource(), message, toHighlight())
		);
	}

}
