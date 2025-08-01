package org.skriptlang.skript.lang.structure;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.lang.Debuggable;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.CheckedIterator;
import ch.njol.util.coll.iterator.ConsumingIterator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.entry.EntryValidator;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Structures are the root elements in every script. They are essentially the "headers".
 * Events and functions are both a type of Structure. However, each one has its own
 *  parsing requirements, order, and defined structure within.
 *
 * Structures may also contain "entries" that hold values or sections of code.
 * The values of these entries can be obtained by parsing the Structure's sub{@link Node}s
 *  through registered {@link EntryData}.
 */
public abstract class Structure implements SyntaxElement, Debuggable {

	/**
	 * The default {@link Priority} of every registered Structure.
	 */
	public static final Priority DEFAULT_PRIORITY = new Priority(1000);

	/**
	 * Priorities are used to determine the order in which Structures should be loaded.
	 * As the priority approaches 0, it becomes more important. Example:
	 * priority of 1 (loads first), priority of 2 (loads second), priority of 3 (loads third)
	 */
	public static class Priority implements Comparable<Priority> {

		private final int priority;

		public Priority(int priority) {
			this.priority = priority;
		}

		public int getPriority() {
			return priority;
		}

		@Override
		public int compareTo(@NotNull Structure.Priority o) {
			return Integer.compare(this.priority, o.priority);
		}

	}

	@Nullable
	private EntryContainer entryContainer = null;

	/**
	 * @return An EntryContainer containing this Structure's {@link EntryData} and {@link Node} parse results.
	 * Please note that this Structure <b>MUST</b> have been initialized for this to work.
	 * This method is not usable for simple structures.
	 * @deprecated This method will be removed in a future version.
	 * If the EntryContainer is needed outside of {@link #init(Literal[], int, ParseResult, EntryContainer)},
	 * the Structure should keep a reference to it.
	 */
	@Deprecated(since = "2.10.0", forRemoval = true)
	public final EntryContainer getEntryContainer() {
		if (entryContainer == null)
			throw new IllegalStateException("This Structure hasn't been initialized!");
		return entryContainer;
	}

	@Override
	public final boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		StructureData structureData = getParser().getData(StructureData.class);

		Literal<?>[] literals = Arrays.copyOf(expressions, expressions.length, Literal[].class);

		StructureInfo<? extends Structure> structureInfo = structureData.structureInfo;
		assert structureInfo != null;

		if (structureData.node instanceof SimpleNode) { // simple structures do not have validators
			return init(literals, matchedPattern, parseResult, null);
		}

		EntryValidator entryValidator = structureInfo.entryValidator;
		if (entryValidator == null) {
			// No validation necessary, the structure itself will handle it
			entryContainer = EntryContainer.withoutValidator((SectionNode) structureData.node);
		} else { // Validation required
			EntryContainer entryContainer = entryValidator.validate((SectionNode) structureData.node);
			if (entryContainer == null)
				return false;
			this.entryContainer = entryContainer;
		}

		return init(literals, matchedPattern, parseResult, entryContainer);
	}

	/**
	 * The initialization phase of a Structure.
	 * Typically, this should be used for preparing fields (e.g. handling arguments, parse tags)
	 * Logic such as trigger loading should be saved for a loading phase (e.g. {@link #load()}).
	 * @return Whether initialization was successful.
	 */
	public abstract boolean init(
		Literal<?>[] args, int matchedPattern, ParseResult parseResult,
		@Nullable EntryContainer entryContainer
	);

	/**
	 * The first phase of Structure loading.
	 * During this phase, all Structures across all loading scripts are loaded with respect to their priorities.
	 * @return Whether preloading was successful. An error should be printed prior to returning false to specify the cause.
	 */
	public boolean preLoad() {
		return true;
	}

	/**
	 * The second phase of Structure loading.
	 * During this phase, all Structures across all loading scripts are loaded with respect to their priorities.
	 * @return Whether loading was successful. An error should be printed prior to returning false to specify the cause.
	 */
	public abstract boolean load();

	/**
	 * The third and final phase of Structure loading.
	 * During this phase, all Structures across all loading scripts are loaded with respect to their priorities.
	 * This method is primarily designed for Structures that wish to execute actions after
	 *  most other Structures have finished loading.
	 * @return Whether postLoading was successful. An error should be printed prior to returning false to specify the cause.
	 */
	public boolean postLoad() {
		return true;
	}

	/**
	 * Called when this structure is unloaded.
	 */
	public void unload() { }

	/**
	 * Called when this structure is unloaded.
	 * This method is primarily designed for Structures that wish to execute actions after
	 * 	most other Structures have finished unloading.
	 */
	public void postUnload() { }

	/**
	 * The priority of a Structure determines the order in which it should be loaded.
	 * For more information, see the javadoc of {@link Priority}.
	 * @return The priority of this Structure. By default, this is {@link Structure#DEFAULT_PRIORITY}.
	 */
	public Priority getPriority() {
		return DEFAULT_PRIORITY;
	}

	@Override
	public String toString() {
		return toString(null, false);
	}

	@Override
	public @NotNull String getSyntaxTypeName() {
		return "structure";
	}

	@Nullable
	public static Structure parse(String expr, Node node, @Nullable String defaultError) {
		if (!(node instanceof SimpleNode) && !(node instanceof SectionNode))
			throw new IllegalArgumentException("only simple or section nodes may be parsed as a structure");
		ParserInstance.get().getData(StructureData.class).node = node;

		var iterator = Skript.instance().syntaxRegistry().syntaxes(org.skriptlang.skript.registration.SyntaxRegistry.STRUCTURE).iterator();
		if (node instanceof SimpleNode) { // filter out section only structures
			iterator = new CheckedIterator<>(iterator, info -> info != null && info.nodeType().canBeSimple());
		} else { // filter out simple only structures
			iterator = new CheckedIterator<>(iterator, info -> info != null && info.nodeType().canBeSection());
		}
		iterator = new ConsumingIterator<>(iterator, info -> ParserInstance.get().getData(StructureData.class).structureInfo =
			(StructureInfo<?>) SyntaxElementInfo.fromModern(info));

		try (ParseLogHandler parseLogHandler = SkriptLogger.startParseLogHandler()) {
			Structure structure = SkriptParser.parseStatic(expr, iterator, ParseContext.EVENT, defaultError);
			if (structure != null) {
				parseLogHandler.printLog();
				return structure;
			}
			parseLogHandler.printError();
			return null;
		}
	}

	@Nullable
	public static Structure parse(String expr, Node node, @Nullable String defaultError, Iterator<? extends StructureInfo<? extends Structure>> iterator) {
		if (!(node instanceof SimpleNode) && !(node instanceof SectionNode))
			throw new IllegalArgumentException("only simple or section nodes may be parsed as a structure");
		ParserInstance.get().getData(StructureData.class).node = node;

		if (node instanceof SimpleNode) { // filter out section only structures
			iterator = new CheckedIterator<>(iterator, item -> item != null && item.nodeType.canBeSimple());
		} else { // filter out simple only structures
			iterator = new CheckedIterator<>(iterator, item -> item != null && item.nodeType.canBeSection());
		}
		iterator = new ConsumingIterator<>(iterator, elementInfo -> ParserInstance.get().getData(StructureData.class).structureInfo = elementInfo);

		try (ParseLogHandler parseLogHandler = SkriptLogger.startParseLogHandler()) {
			Structure structure = SkriptParser.parseStatic(expr, iterator, ParseContext.EVENT, defaultError);
			if (structure != null) {
				parseLogHandler.printLog();
				return structure;
			}
			parseLogHandler.printError();
			return null;
		}
	}

	static {
		ParserInstance.registerData(StructureData.class, StructureData::new);
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	public static class StructureData extends ParserInstance.Data {

		@ApiStatus.Internal
		public Node node;
		@ApiStatus.Internal
		public @Nullable StructureInfo<? extends Structure> structureInfo;

		public StructureData(ParserInstance parserInstance) {
			super(parserInstance);
		}

		public @Nullable StructureInfo<? extends Structure> getStructureInfo() {
			return structureInfo;
		}

	}

}
