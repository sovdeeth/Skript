/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.script.Script;

import com.google.common.collect.Lists;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.ExprColoured;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.BlockingLogHandler;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.structures.StructVariables.DefaultVariables;
import ch.njol.skript.util.StringMode;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.chat.ChatMessages;
import ch.njol.skript.util.chat.MessageComponent;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.SingleItemIterator;

/**
 * Represents a string that may contain expressions, and is thus "variable".
 */
public class VariableString implements Expression<String> {

	@Nullable
	private final Script script;
	private final String orig;

	@Nullable
	private final Object[] string;

	@Nullable
	private Object[] stringUnformatted;
	private final boolean isSimple;

	@Nullable
	private final String simple;

	@Nullable
	private final String simpleUnformatted;
	private final StringMode mode;

	/**
	 * Message components that this string consists of. Only simple parts have
	 * been evaluated here.
	 */
	private final MessageComponent[] components;

	/**
	 * Creates a new VariableString which does not contain variables.
	 * 
	 * @param input Content for string.
	 */
	private VariableString(String input) {
		this.isSimple = true;
		this.simpleUnformatted = input.replace("%%", "%"); // This doesn't contain variables, so this wasn't done in newInstance!
		this.simple = Utils.replaceChatStyles(simpleUnformatted);
				
		this.orig = simple;
		this.string = null;
		this.mode = StringMode.MESSAGE;
		
		ParserInstance parser = getParser();
		this.script = parser.isActive() ? parser.getCurrentScript() : null;
		
		this.components = new MessageComponent[] {ChatMessages.plainText(simpleUnformatted)};
	}

	/**
	 * Creates a new VariableString which contains variables.
	 * 
	 * @param orig Original string (unparsed).
	 * @param string Objects, some of them are variables.
	 * @param mode String mode.
	 */
	private VariableString(String orig, Object[] string, StringMode mode) {
		this.orig = orig;
		this.string = new Object[string.length];
		this.stringUnformatted = new Object[string.length];

		ParserInstance parser = getParser();
		this.script = parser.isActive() ? parser.getCurrentScript() : null;
	
		// Construct unformatted string and components
		List<MessageComponent> components = new ArrayList<>(string.length);
		for (int i = 0; i < string.length; i++) {
			Object object = string[i];
			if (object instanceof String) {
				this.string[i] = Utils.replaceChatStyles((String) object);
				components.addAll(ChatMessages.parse((String) object));
			} else {
				this.string[i] = object;
				components.add(null); // Not known parse-time
			}

			// For unformatted string, don't format stuff
			this.stringUnformatted[i] = object;
		}
		this.components = components.toArray(new MessageComponent[0]);

		this.mode = mode;

		this.isSimple = false;
		this.simple = null;
		this.simpleUnformatted = null;
	}

	/**
	 * Prints errors
	 */
	@Nullable
	public static VariableString newInstance(String input) {
		return newInstance(input, StringMode.MESSAGE);
	}

	/**
	 * Creates an instance of VariableString by parsing given string.
	 * Prints errors and returns null if it is somehow invalid.
	 * 
	 * @param orig Unquoted string to parse.
	 * @return A new VariableString instance.
	 */
	@Nullable
	public static VariableString newInstance(String orig, StringMode mode) {
		if (mode != StringMode.VARIABLE_NAME && !isQuotedCorrectly(orig, false))
			return null;
		int n = StringUtils.count(orig, '%');
		if (n % 2 != 0) {
			Skript.error("The percent sign is used for expressions (e.g. %player%). To insert a '%' type it twice: %%.");
			return null;
		}

		// We must not parse color codes yet, as JSON support would be broken :(
		String s;
		if (mode != StringMode.VARIABLE_NAME) {
			// Replace every double " character with a single ", except for those in expressions (between %)
			StringBuilder stringBuilder = new StringBuilder();
			
			boolean expression = false;
			for (int i = 0; i < orig.length(); i++) {
				char c = orig.charAt(i);
				stringBuilder.append(c);
				
				if (c == '%')
					expression = !expression;
				
				if (!expression && c == '"')
					i++;
			}
			s = stringBuilder.toString();
		} else {
			s = orig;
		}

		List<Object> string = new ArrayList<>(n / 2 + 2); // List of strings and expressions
		int c = s.indexOf('%');
		if (c != -1) {
			if (c != 0)
				string.add(s.substring(0, c));
			while (c != s.length()) {
				int c2 = s.indexOf('%', c + 1);
				
				int a = c;
				int b;
				while (c2 != -1 && (b = s.indexOf('{', a + 1)) != -1 && b < c2) {
					a = nextVariableBracket(s, b + 1);
					if (a == -1) {
						Skript.error("Missing closing bracket '}' to end variable");
						return null;
					}
					c2 = s.indexOf('%', a + 1);
				}
				if (c2 == -1) {
					assert false;
					return null;
				}
				if (c + 1 == c2) {
					// %% escaped -> one % in result string
					if (string.size() > 0 && string.get(string.size() - 1) instanceof String) {
						string.set(string.size() - 1, (String) string.get(string.size() - 1) + "%");
					} else {
						string.add("%");
					}
				} else {
					RetainingLogHandler log = SkriptLogger.startRetainingLog();
					try {
						Expression<?> expr =
							new SkriptParser(s.substring(c + 1, c2), SkriptParser.PARSE_EXPRESSIONS, ParseContext.DEFAULT)
								.parseExpression(Object.class);
						if (expr == null) {
							log.printErrors("Can't understand this expression: " + s.substring(c + 1, c2));
							return null;
						} else {
							string.add(expr);
						}
						log.printLog();
					} finally {
						log.stop();
					}
				}
				c = s.indexOf('%', c2 + 1);
				if (c == -1)
					c = s.length();
				String l = s.substring(c2 + 1, c); // Try to get string (non-variable) part
				if (!l.isEmpty()) { // This is string part (no variables)
					if (string.size() > 0 && string.get(string.size() - 1) instanceof String) {
						// We can append last string part in the list, so let's do so
						string.set(string.size() - 1, (String) string.get(string.size() - 1) + l);
					} else { // Can't append, just add new part
						string.add(l);
					}
				}
			}
		} else {
			// Only one string, no variable parts
			string.add(s);
		}

		// Check if this isn't actually variable string, and return
		if (string.size() == 1 && string.get(0) instanceof String)
			return new VariableString(s);

		Object[] sa = string.toArray();
		if (string.size() == 1 && string.get(0) instanceof Expression &&
				((Expression<?>) string.get(0)).getReturnType() == String.class &&
				((Expression<?>) string.get(0)).isSingle() &&
				mode == StringMode.MESSAGE) {
			String expr = ((Expression<?>) string.get(0)).toString(null, false);
			Skript.warning(expr + " is already a text, so you should not put it in one (e.g. " + expr + " instead of " + "\"%" + expr.replace("\"", "\"\"") + "%\")");
		}
		return new VariableString(orig, sa, mode);
	}

	/**
	 * Attempts to properly quote a string (e.g. double the double quotations).
	 * Please note that the string itself will not be surrounded with double quotations.
	 * @param string The string to properly quote.
	 * @return The input where all double quotations outside of expressions have been doubled.
	 */
	public static String quote(String string) {
		StringBuilder fixed = new StringBuilder();
		boolean inExpression = false;
		for (char c : string.toCharArray()) {
			if (c == '%') // If we are entering an expression, quotes should NOT be doubled
				inExpression = !inExpression;
			if (!inExpression && c == '"')
				fixed.append('"');
			fixed.append(c);
		}
		return fixed.toString();
	}

	/**
	 * Tests whether a string is correctly quoted, i.e. only has doubled double quotes in it.
	 * Singular double quotes are only allowed between percentage signs.
	 * 
	 * @param s The string
	 * @param withQuotes Whether s must be surrounded by double quotes or not
	 * @return Whether the string is quoted correctly
	 */
	public static boolean isQuotedCorrectly(String s, boolean withQuotes) {
		if (withQuotes && (!s.startsWith("\"") || !s.endsWith("\"") || s.length() < 2))
			return false;
		boolean quote = false;
		boolean percentage = false;
		if (withQuotes)
			s = s.substring(1, s.length() - 1);
		for (char c : s.toCharArray()) {
			if (percentage) {
				if (c == '%')
					percentage = false;
				continue;
			}
			if (quote && c != '"')
				return false;
			if (c == '"') {
				quote = !quote;
			} else if (c == '%') {
				percentage = true;
			}
		}
		return !quote;
	}

	/**
	 * Removes quoted quotes from a string.
	 * 
	 * @param s The string
	 * @param surroundingQuotes Whether the string has quotes at the start & end that should be removed
	 * @return The string with double quotes replaced with signle ones and optionally with removed surrounding quotes.
	 */
	public static String unquote(String s, boolean surroundingQuotes) {
		assert isQuotedCorrectly(s, surroundingQuotes);
		if (surroundingQuotes)
			return s.substring(1, s.length() - 1).replace("\"\"", "\"");
		return s.replace("\"\"", "\"");
	}

	/**
	 * Copied from {@code SkriptParser#nextBracket(String, char, char, int, boolean)}, but removed escaping & returns -1 on error.
	 * 
	 * @param s
	 * @param start Index after the opening bracket
	 * @return The next closing curly bracket
	 */
	public static int nextVariableBracket(String s, int start) {
		int n = 0;
		for (int i = start; i < s.length(); i++) {
			if (s.charAt(i) == '}') {
				if (n == 0)
					return i;
				n--;
			} else if (s.charAt(i) == '{') {
				n++;
			}
		}
		return -1;
	}
	
	public static VariableString[] makeStrings(String[] args) {
		VariableString[] strings = new VariableString[args.length];
		int j = 0;
		for (String arg : args) {
			VariableString vs = newInstance(arg);
			if (vs != null)
				strings[j++] = vs;
		}
		if (j != args.length)
			strings = Arrays.copyOf(strings, j);
		return strings;
	}
	
	/**
	 * @param args Quoted strings - This is not checked!
	 * @return a new array containing all newly created VariableStrings, or null if one is invalid
	 */
	@Nullable
	public static VariableString[] makeStringsFromQuoted(List<String> args) {
		VariableString[] strings = new VariableString[args.size()];
		for (int i = 0; i < args.size(); i++) {
			assert args.get(i).startsWith("\"") && args.get(i).endsWith("\"");
			VariableString vs = newInstance(args.get(i).substring(1, args.get(i).length() - 1));
			if (vs == null)
				return null;
			strings[i] = vs;
		}
		return strings;
	}
	
	/**
	 * Parses all expressions in the string and returns it.
	 * Does not parse formatting codes!
	 * @param e Event to pass to the expressions.
	 * @return The input string with all expressions replaced.
	 */
	public String toUnformattedString(Event e) {
		if (isSimple) {
			assert simpleUnformatted != null;
			return simpleUnformatted;
		}
		Object[] string = this.stringUnformatted;
		assert string != null;
		StringBuilder b = new StringBuilder();
		for (Object o : string) {
			if (o instanceof Expression<?>) {
				b.append(Classes.toString(((Expression<?>) o).getArray(e), true, mode));
			} else {
				b.append(o);
			}
		}
		return b.toString();
	}
	
	/**
	 * Gets message components from this string. Formatting is parsed only
	 * in simple parts for security reasons.
	 * @param e Currently running event.
	 * @return Message components.
	 */
	public List<MessageComponent> getMessageComponents(Event e) {
		if (isSimple) { // Trusted, constant string in a script
			assert simpleUnformatted != null;
			return ChatMessages.parse(simpleUnformatted);
		}
		
		// Parse formating
		Object[] string = this.stringUnformatted;
		assert string != null;
		List<MessageComponent> message = new ArrayList<>(components.length); // At least this much space
		int stringPart = -1;
		MessageComponent previous = null;
		for (MessageComponent component : components) {
			if (component == null) { // This component holds place for variable part
				// Go over previous expression part (stringPart >= 0) or take first part (stringPart == 0)
				stringPart++;
				if (previous != null) { // Also jump over literal part
					stringPart++;
				}
				Object o = string[stringPart];
				previous = null;
				
				// Convert it to plain text
				String text = null;
				if (o instanceof ExprColoured && ((ExprColoured) o).isUnsafeFormat()) { // Special case: user wants to process formatting
					String unformatted = Classes.toString(((ExprColoured) o).getArray(e), true, mode);
					if (unformatted != null) {
						message.addAll(ChatMessages.parse(unformatted));
					}
					continue;
				} else if (o instanceof Expression<?>) {
					text = Classes.toString(((Expression<?>) o).getArray(e), true, mode);
				}
				
				assert text != null;
				List<MessageComponent> components = ChatMessages.fromParsedString(text);
				if (!message.isEmpty()) { // Copy styles from previous component
					int startSize = message.size();
					for (int i = 0; i < components.size(); i++) {
						MessageComponent plain = components.get(i);
						ChatMessages.copyStyles(message.get(startSize + i - 1), plain);
						message.add(plain);
					}
				} else {
					message.addAll(components);
				}
			} else {
				MessageComponent componentCopy = component.copy();
				if (!message.isEmpty()) { // Copy styles from previous component
					ChatMessages.copyStyles(message.get(message.size() - 1), componentCopy);
				}
				message.add(componentCopy);
				previous = componentCopy;
			}
		}
		
		return message;
	}
	
	/**
	 * Gets message components from this string. Formatting is parsed
	 * everywhere, which is a potential security risk.
	 * @param e Currently running event.
	 * @return Message components.
	 */
	public List<MessageComponent> getMessageComponentsUnsafe(Event e) {
		if (isSimple) { // Trusted, constant string in a script
			assert simpleUnformatted != null;
			return ChatMessages.parse(simpleUnformatted);
		}
		
		return ChatMessages.parse(toUnformattedString(e));
	}
	
	/**
	 * Parses all expressions in the string and returns it in chat JSON format.
	 * 
	 * @param e Event to pass to the expressions.
	 * @return The input string with all expressions replaced.
	 */
	public String toChatString(Event e) {
		return ChatMessages.toJson(getMessageComponents(e));
	}
	
	@Nullable
	private static ChatColor getLastColor(CharSequence s) {
		for (int i = s.length() - 2; i >= 0; i--) {
			if (s.charAt(i) == ChatColor.COLOR_CHAR) {
				ChatColor c = ChatColor.getByChar(s.charAt(i + 1));
				if (c != null && (c.isColor() || c == ChatColor.RESET))
					return c;
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return toString(null, false);
	}

	/**
	 * Parses all expressions in the string and returns it.
	 * If this is a simple string, the event may be null.
	 * 
	 * @param event Event to pass to the expressions.
	 * @return The input string with all expressions replaced.
	 */
	public String toString(@Nullable Event event) {
		if (isSimple) {
			assert simple != null;
			return simple;
		}
		if (event == null)
			throw new IllegalArgumentException("Event may not be null in non-simple VariableStrings!");

		Object[] string = this.string;
		assert string != null;
		StringBuilder builder = new StringBuilder();
		List<Class<?>> types = new ArrayList<>();
		for (Object object : string) {
			if (object instanceof Expression<?>) {
				Object[] objects = ((Expression<?>) object).getArray(event);
				if (objects != null && objects.length > 0)
					types.add(objects[0].getClass());
				builder.append(Classes.toString(objects, true, mode));
			} else {
				builder.append(object);
			}
		}
		String complete = builder.toString();
		if (script != null && mode == StringMode.VARIABLE_NAME && !types.isEmpty()) {
			DefaultVariables data = script.getData(DefaultVariables.class);
			if (data != null)
				data.add(complete, types.toArray(new Class<?>[0]));
		}
		return complete;
	}

	/**
	 * Use {@link #toString(Event)} to get the actual string. This method is for debugging.
	 */
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (isSimple) {
			assert simple != null;
			return '"' + simple + '"';
		}
		Object[] string = this.string;
		assert string != null;
		StringBuilder builder = new StringBuilder("\"");
		for (Object object : string) {
			if (object instanceof Expression) {
				builder.append("%").append(((Expression<?>) object).toString(event, debug)).append("%");
			} else {
				builder.append(object);
			}
		}
		builder.append('"');
		return builder.toString();
	}

	/**
	 * Builds all possible default variable type hints based on the super type of the expression.
	 * 
	 * @return List<String> of all possible super class code names.
	 */
	@NotNull
	public List<String> getDefaultVariableNames(String variableName, Event event) {
		if (script == null || mode != StringMode.VARIABLE_NAME)
			return Lists.newArrayList();

		if (isSimple) {
			assert simple != null;
			return Lists.newArrayList(simple, "object");
		}

		DefaultVariables data = script.getData(DefaultVariables.class);
		// Checked in Variable#getRaw already
		assert data != null : "default variables not present in current script";

		Class<?>[] savedHints = data.get(variableName);
		if (savedHints == null || savedHints.length == 0)
			return Lists.newArrayList();

		List<StringBuilder> typeHints = Lists.newArrayList(new StringBuilder());
		// Represents the index of which expression in a variable string, example name::%entity%::%object% the index of 0 will be entity.
		int hintIndex = 0;
		assert string != null;
		for (Object object : string) {
			if (!(object instanceof Expression)) {
				typeHints.forEach(builder -> builder.append(object));
				continue;
			}
			StringBuilder[] current = typeHints.toArray(new StringBuilder[0]);
			for (ClassInfo<?> classInfo : Classes.getAllSuperClassInfos(savedHints[hintIndex])) {
				for (StringBuilder builder : current) {
					String hint = builder.toString() + "<" + classInfo.getCodeName() + ">";
					// Has to duplicate the builder as it builds multiple off the last builder.
					typeHints.add(new StringBuilder(hint));
					typeHints.remove(builder);
				}
			}
			hintIndex++;
		}
		return typeHints.stream().map(StringBuilder::toString).collect(Collectors.toList());
	}

	public boolean isSimple() {
		return isSimple;
	}

	public StringMode getMode() {
		return mode;
	}

	public VariableString setMode(StringMode mode) {
		if (this.mode == mode || isSimple)
			return this;
		@SuppressWarnings("resource")
		BlockingLogHandler h = new BlockingLogHandler().start();
		try {
			VariableString vs = newInstance(orig, mode);
			if (vs == null) {
				assert false : this + "; " + mode;
				return this;
			}
			return vs;
		} finally {
			h.stop();
		}
	}
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getSingle(Event e) {
		return toString(e);
	}
	
	@Override
	public String[] getArray(Event e) {
		return new String[] {toString(e)};
	}
	
	@Override
	public String[] getAll(Event e) {
		return new String[] {toString(e)};
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public boolean check(Event e, Checker<? super String> c, boolean negated) {
		return SimpleExpression.check(getAll(e), c, negated, false);
	}
	
	@Override
	public boolean check(Event e, Checker<? super String> c) {
		return SimpleExpression.check(getAll(e), c, false, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <R> Expression<? extends R> getConvertedExpression(Class<R>... to) {
		if (CollectionUtils.containsSuperclass(to, String.class))
			return (Expression<? extends R>) this;
		return ConvertedExpression.newInstance(this, to);
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		return null;
	}
	
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean getAnd() {
		return true;
	}
	
	@Override
	public boolean setTime(int time) {
		return false;
	}
	
	@Override
	public int getTime() {
		return 0;
	}
	
	@Override
	public boolean isDefault() {
		return false;
	}
	
	@Override
	public Iterator<? extends String> iterator(Event e) {
		return new SingleItemIterator<>(toString(e));
	}
	
	@Override
	public boolean isLoopOf(String s) {
		return false;
	}
	
	@Override
	public Expression<?> getSource() {
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Expression<T> setStringMode(Expression<T> e, StringMode mode) {
		if (e instanceof ExpressionList) {
			Expression<?>[] ls = ((ExpressionList<?>) e).getExpressions();
			for (int i = 0; i < ls.length; i++) {
				Expression<?> l = ls[i];
				assert l != null;
				ls[i] = setStringMode(l, mode);
			}
		} else if (e instanceof VariableString) {
			return (Expression<T>) ((VariableString) e).setMode(mode);
		}
		return e;
	}
	
	@Override
	public Expression<String> simplify() {
		return this;
	}

}
