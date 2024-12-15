package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.data.JavaClasses;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

@Name("Continue")
@Description("Moves the loop to the next iteration. You may also continue an outer loop from an inner one." +
	" The loops are labelled from 1 until the current loop, starting with the outermost one.")
@Examples({
	"# Broadcast online moderators",
	"loop all players:",
		"\tif loop-value does not have permission \"moderator\":",
			"\t\tcontinue # filter out non moderators",
		"\tbroadcast \"%loop-player% is a moderator!\" # Only moderators get broadcast",
	" ",
	"# Game starting counter",
	"set {_counter} to 11",
	"while {_counter} > 0:",
		"\tremove 1 from {_counter}",
		"\twait a second",
		"\tif {_counter} != 1, 2, 3, 5 or 10:",
			"\t\tcontinue # only print when counter is 1, 2, 3, 5 or 10",
		"\tbroadcast \"Game starting in %{_counter}% second(s)\"",
})
@Since("2.2-dev37, 2.7 (while loops), 2.8.0 (outer loops)")
public class EffContinue extends Effect {

	static {
		Skript.registerEffect(EffContinue.class,
			"continue [this loop|[the] [current] loop]",
			"continue [the] <" + JavaClasses.INTEGER_NUMBER_PATTERN + ">(st|nd|rd|th) loop"
		);
	}

	// Used for toString
	private int level;

	private @UnknownNullability LoopSection loop;
	private @UnknownNullability List<SectionExitHandler> sectionsToExit;
	private int breakLevels;

	@Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		ParserInstance parser = getParser();
		int loops = parser.getCurrentSections(LoopSection.class).size();
		if (loops == 0) {
			Skript.error("The 'continue' effect may only be used in loops");
			return false;
		}

		level = matchedPattern == 0 ? loops : Integer.parseInt(parseResult.regexes.get(0).group());
		if (level < 1)
			return false;

		// ParserInstance#getSections counts from the innermost section, so we need to invert the level 
		int levels = loops - level + 1;
		if (levels <= 0) {
			Skript.error("Can't continue the " + StringUtils.fancyOrderNumber(level) + " loop as there " +
				(loops == 1 ? "is only 1 loop" : "are only " + loops + " loops") + " present");
			return false;
		}

        List<TriggerSection> innerSections = parser.getSections(levels, LoopSection.class);
		breakLevels = innerSections.size();
		loop = (LoopSection) innerSections.remove(0);
		sectionsToExit = innerSections.stream()
			.filter(SectionExitHandler.class::isInstance)
			.map(SectionExitHandler.class::cast)
			.toList();
		return true;
	}

	@Override
	protected void execute(Event event) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		debug(event, false);
		for (SectionExitHandler section : sectionsToExit)
			section.exit(event);
		return loop;
	}

	@Override
	public ExecutionIntent executionIntent() {
		return ExecutionIntent.stopSections(breakLevels);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "continue" + (level == -1 ? "" : " the " + StringUtils.fancyOrderNumber(level) + " loop");
	}

}
