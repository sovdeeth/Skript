package org.skriptlang.skript.common.function;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.function.FunctionRegistry;
import ch.njol.skript.lang.function.Signature;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Language;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionReference.Argument;
import org.skriptlang.skript.common.function.FunctionReference.ArgumentType;
import org.skriptlang.skript.common.function.FunctionReferenceParser.EmptyExpression;
import org.skriptlang.skript.common.function.Parameter.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Resolves a function call to a {@link FunctionReference}, selecting between overloads and
 * binding the passed arguments to the chosen signature's parameters.
 * <p>
 * This class owns everything about that process which does not depend on how an argument's value
 * is represented. The representation-specific steps are supplied by an {@link ArgumentBinder},
 * so that the same resolution can be driven both from the raw argument text of a function call
 * written in a script and from already-parsed expressions.
 * </p>
 *
 * @param <T> The type an argument's value is represented by.
 */
public final class FunctionBinder<T> {

	private static final ArgsMessage UNEXPECTED_ARGUMENT = new ArgsMessage("functions.unexpected argument");
	private static final ArgsMessage UNKNOWN_FUNCTION = new ArgsMessage("functions.unknown function");
	private static final ArgsMessage POTENTIAL_SIGNATURE = new ArgsMessage("functions.potential signature");

	private final ArgumentBinder<T> binder;

	FunctionBinder(@NotNull ArgumentBinder<T> binder) {
		this.binder = binder;
	}

	/**
	 * Attempts to resolve a function call to a single {@link FunctionReference}.
	 * <p>
	 * Any failure is reported through {@link Skript#error(String)} before this returns, so callers
	 * holding a log handler should print it whenever this returns null.
	 * </p>
	 *
	 * @param namespace The namespace to resolve local functions in, or null for global functions only.
	 * @param name      The function name.
	 * @param arguments The passed arguments.
	 * @param <R>       The return type of the function.
	 * @return The matched reference, or null if none could be matched.
	 */
	public <R> @Nullable FunctionReference<R> resolve(
		@Nullable String namespace, @NotNull String name, @NotNull Argument<T>[] arguments
	) {
		// avoid assigning values to a parameter multiple times
		Set<String> named = new HashSet<>();
		for (Argument<T> argument : arguments) {
			if (argument.type() != ArgumentType.NAMED) {
				continue;
			}

			boolean added = named.add(argument.name());
			if (added) {
				continue;
			}

			Skript.error(Language.get("functions.already assigned value to parameter"), argument.name());
			return null;
		}

		// try to find a matching signature to get which types to parse args with
		Set<Signature<?>> options = FunctionRegistry.getRegistry().getSignatures(namespace, name);

		if (options.isEmpty()) {
			doesNotExist(name, arguments, options);
			return null;
		}

		// all signatures that have no single list param
		// example: function add(x: int, y: int)
		Set<Signature<?>> exacts = new HashSet<>();
		// all signatures with only single list params
		// these are functions that accept any number of arguments given a specific type
		// example: function sum(ns: numbers)
		Set<Signature<?>> lists = new HashSet<>();

		// first, sort into types
		for (Signature<?> option : options) {
			if (option.parameters().size() == 1 && !option.parameters().getFirst().isSingle()) {
				lists.add(option);
			} else {
				exacts.add(option);
			}
		}

		// second, try to match any exact functions
		Set<FunctionReference<R>> exactReferences = getExactReferences(namespace, name, exacts, arguments);
		if (exactReferences == null) { // a list error, so quit parsing
			return null;
		}

		// if we found an exact one, return first to avoid conflict with list references
		if (exactReferences.size() == 1) {
			return exactReferences.stream().findAny().orElse(null);
		}

		// last, find single list functions
		Set<FunctionReference<R>> listReferences = getListReferences(namespace, name, lists, arguments);
		if (listReferences == null) { // a list error, so quit parsing
			return null;
		}

		exactReferences.addAll(listReferences);

		if (exactReferences.isEmpty()) {
			doesNotExist(name, arguments, Sets.union(exacts, lists));
			return null;
		} else if (exactReferences.size() == 1) {
			return exactReferences.stream().findAny().orElse(null);
		} else {
			ambiguousError(name, exactReferences);
			return null;
		}
	}

	/**
	 * Returns all possible {@link FunctionReference FunctionReferences} given the list of signatures
	 * which do not contain a single list parameter.
	 *
	 * @param namespace  The current namespace.
	 * @param name       The name of the function.
	 * @param signatures The possible signatures.
	 * @param arguments  The passed arguments.
	 * @param <R>        The return type of the references.
	 * @return All possible exact {@link FunctionReference FunctionReferences}.
	 */
	private <R> @Nullable Set<FunctionReference<R>> getExactReferences(
		String namespace, String name,
		Set<Signature<?>> signatures, Argument<T>[] arguments
	) {
		Set<FunctionReference<R>> exactReferences = new HashSet<>();

		// whether the arguments array contains any NAMED type parameters
		// if there are no named parameters, then we consider the user arguments to already be in order
		boolean hasNames = Arrays.stream(arguments).anyMatch(argument -> argument.type() == ArgumentType.NAMED);

		// TODO! cache results
		// 'arguments' may be reassigned when processing a signature
		// retain a reference to the original array for resetting each time
		Argument<T>[] originalArguments = arguments;
		for (Signature<?> signature : signatures) {
			// if arguments arent possible, skip
			if (arguments.length > signature.getMaxParameters() || arguments.length < signature.getMinParameters()) {
				continue;
			}
			arguments = originalArguments;

			// all remaining arguments to parse
			// if a passed argument is named it bypasses the regular argument order of unnamed arguments
			SequencedMap<String, Parameter<?>> parameters = signature.parameters().sequencedMap();

			// if a parameter is not passed, we need to create a dummy argument to allow binding in bindArguments
			//noinspection unchecked
			Argument<T>[] parseArguments = (Argument<T>[]) new Argument[parameters.size()];
			ArgumentParseTarget[] parseTargets = new ArgumentParseTarget[parameters.size()];

			// fill in named arguments, placeholders for unnamed arguments
			// essentially, we reorder the user provided arguments into the order defined by the parameters
			int index = 0;
			// whether, after this first pass completes, there are any placeholders to fill in
			boolean hasPlaceholders = false;
			for (var entry : parameters.entrySet()) {
				Parameter<?> parameter = entry.getValue();

				// attempt to match an argument to this parameter
				Argument<T> argument = null;
				if (hasNames) {
					for (var candidate : arguments) {
						if (entry.getKey().equals(candidate.name())) { // exact match
							argument = candidate;
							break;
						}
					}
				} else if (index < arguments.length) { // if there are no named arguments, simply take the next provided one
					argument = new Argument<>(ArgumentType.UNNAMED, entry.getKey(), arguments[index].value());
				}
				if (argument == null) { // could not resolve an argument, use a placeholder
					argument = new Argument<>(ArgumentType.UNNAMED, entry.getKey(), null);
					hasPlaceholders = true;
				}
				parseArguments[index] = argument;

				// prepare type information for binding
				parseTargets[index] = targetFor(parameter, Utils.getComponentType(parameter.type()));

				index++;
			}
			if (hasPlaceholders) { // fill in placeholder arguments as necessary
				// some arguments may be erroneously interpreted as name, but their value just contains a colon
				// for example, minecraft:stone
				// convert these unexpected named arguments to unnamed arguments
				boolean copied = false;
				for (int i = 0; i < arguments.length; i++) {
					Argument<T> argument = arguments[i];
					if (argument.type() == ArgumentType.NAMED && !parameters.containsKey(argument.name())) {
						if (!copied) {
							arguments = Arrays.copyOf(arguments, arguments.length);
							copied = true;
						}
						// we retain the name for later purposes, such as logging
						arguments[i] = new Argument<>(ArgumentType.UNNAMED, argument.name(), binder.rawValue(argument));
					}
				}

				// we track named arguments as we encounter them
				// we use this to avoid inserting the next unnamed argument too early
				// for example, consider func(x, optional y, z, optional aa)
				// for a call such as func(x: 1: z: 2, 3), '3' should map to 'aa' rather than 'y'
				List<String> priorNames = new ArrayList<>();
				// the index of the last verified argument index
				// this is used to avoid reverifying the position of named arguments multiple times
				int lastCheck = -1;
				// the index of the last unnamed argument slot that was filled
				int lastFilled = -1;
				fill:
				for (int i = 0; i < arguments.length; i++) {
					Argument<T> argument = arguments[i];
					if (argument.type() == ArgumentType.NAMED) {
						priorNames.add(argument.name());
						if (i > 0 && arguments[i - 1].type() == ArgumentType.NAMED) {
							// since the last argument was named, the position of this one has not been verified
							// thus, search through the parse arguments again
							lastCheck = -1;
						}
						continue;
					}
					// find the next named argument
					String nextName = null;
					for (int j = i + 1; j < arguments.length; j++) {
						Argument<T> nextArgument = arguments[j];
						if (nextArgument.type() == ArgumentType.NAMED) {
							nextName = nextArgument.name();
							break;
						}
					}
					// fill in using this unnamed argument
					for (int j = lastCheck + 1; j < parseArguments.length; j++) {
						Argument<T> parseArgument = parseArguments[j];
						if (parseArgument.type() == ArgumentType.NAMED) {
							if (parseArgument.name().equals(nextName)) { // this argument is in an invalid position
								break;
							}
							priorNames.remove(parseArgument.name());
						} else if (j > lastFilled && priorNames.isEmpty()) { // can occupy this slot
							parseArguments[j] = new Argument<>(ArgumentType.UNNAMED, parseArgument.name(), argument.value());
							lastCheck = j;
							lastFilled = j;
							continue fill;
						}
					}
					// unable to find a slot for this argument
					if (argument.name() != null) { // this was originally a named argument, error as if it is
						Skript.error(UNEXPECTED_ARGUMENT.toString(argument.name()));
					} else {
						Skript.error(Language.get("functions.mixing named and unnamed arguments"));
					}
					return null;
				}
			}

			ArgumentParseResult result = bindArguments(parseArguments, parseTargets);
			switch (result.type()) {
				case LIST_ERROR -> {
					return null;
				}
				case OK -> {
					//noinspection unchecked
					FunctionReference<R> reference =
						new FunctionReference<>(namespace, name, (Signature<R>) signature, result.parsed());
					if (!reference.validate()) {
						continue;
					}
					exactReferences.add(reference);
				}
				default -> {
					// continue
				}
			}
		}
		return exactReferences;
	}

	/**
	 * Returns all possible {@link FunctionReference FunctionReferences} given the list of signatures
	 * which only contain a single list parameter.
	 *
	 * @param namespace  The current namespace.
	 * @param name       The name of the function.
	 * @param signatures The possible signatures.
	 * @param arguments  The passed arguments.
	 * @param <R>        The return type of the references.
	 * @return All possible {@link FunctionReference FunctionReferences} which contain a single list parameter.
	 */
	private <R> @Nullable Set<FunctionReference<R>> getListReferences(
		String namespace, String name,
		Set<Signature<?>> signatures, Argument<T>[] arguments
	) {
		// disallow naming any arguments other than the first
		if (arguments.length > 1) {
			for (Argument<T> argument : arguments) {
				if (argument.type() != ArgumentType.NAMED) {
					continue;
				}

				doesNotExist(name, arguments, signatures);
				return null;
			}
		}

		Set<FunctionReference<R>> references = new HashSet<>();

		signatures:
		for (Signature<?> signature : signatures) {
			Parameter<?> parameter = signature.parameters().getFirst();

			ArgumentParseTarget[] parseTargets = new ArgumentParseTarget[]{
				targetFor(parameter, parameter.type().componentType())
			};

			if (arguments.length == 1 && arguments[0].type() == ArgumentType.NAMED) {
				if (!arguments[0].name().equals(parameter.name())) {
					doesNotExist(name, arguments, signatures);
					continue;
				}
			}

			// join all args to a single arg
			Argument<T> argument = binder.joinForList(arguments, parameter.name());
			Argument<T>[] array = CollectionUtils.array(argument);

			ArgumentParseResult result = bindArguments(array, parseTargets);

			if (result.type() == ArgumentParseResultType.LIST_ERROR) {
				return null;
			}

			if (result.type() == ArgumentParseResultType.OK) {
				// avoid allowing lists inside lists
				if (result.parsed().length == 1 && result.parsed()[0].value() instanceof ExpressionList<?> list) {
					for (Expression<?> expression : list.getExpressions()) {
						if (expression instanceof ExpressionList<?>) {
							doesNotExist(name, arguments, signatures);
							continue signatures;
						}
					}
				}

				//noinspection unchecked
				FunctionReference<R> reference =
					new FunctionReference<>(namespace, name, (Signature<R>) signature, result.parsed());

				if (!reference.validate()) {
					continue;
				}

				references.add(reference);
			}
		}

		return references;
	}

	/**
	 * Builds the target type and fallback expression used to bind an argument to {@code parameter}.
	 *
	 * @param parameter  The parameter being bound to.
	 * @param targetType The type an argument for this parameter must be bound as. Never an array.
	 * @return The target data.
	 */
	private static ArgumentParseTarget targetFor(Parameter<?> parameter, Class<?> targetType) {
		Expression<?> fallback;
		if (parameter instanceof ScriptParameter<?> sp) {
			fallback = sp.defaultValue();
		} else if (parameter.hasModifier(Modifier.OPTIONAL)) {
			fallback = new EmptyExpression();
		} else {
			fallback = null;
		}
		return new ArgumentParseTarget(targetType, fallback);
	}

	/**
	 * Prints the error for when multiple function references have been matched.
	 *
	 * @param name       The function name.
	 * @param references The possible references.
	 * @param <R>        The return types of the references.
	 */
	private <R> void ambiguousError(String name, Set<FunctionReference<R>> references) {
		List<String> parts = new ArrayList<>();

		for (FunctionReference<R> reference : references) {
			String builder = reference.name() +
				"(" +
				Arrays.stream(reference.signature().parameters().all())
					.map(it -> {
						if (it.type().isArray()) {
							return Classes.getSuperClassInfo(it.type().componentType()).getName().getPlural();
						} else {
							return Classes.getSuperClassInfo(it.type()).getName().getSingular();
						}
					})
					.collect(Collectors.joining(", ")) +
				")";

			parts.add(builder);
		}

		Skript.error(Language.get("functions.ambiguous function call"),
			name, StringUtils.join(parts, ", ", " or "));
	}

	/**
	 * Prints the error for when a function does not exist.
	 *
	 * @param name      The function name.
	 * @param arguments The passed arguments to the function call.
	 * @param possibleSignatures A set of signatures that may contain what the user intended to match.
	 */
	private void doesNotExist(String name, Argument<T>[] arguments, Set<Signature<?>> possibleSignatures) {
		StringJoiner joiner = new StringJoiner(", ");

		List<Class<?>> argumentTypes = new ArrayList<>();
		for (Argument<T> argument : arguments) {
			Expression<?> expression = binder.describe(argument);

			String argumentName;
			if (argument.type() == ArgumentType.NAMED) {
				argumentName = argument.name() + ": ";
			} else {
				argumentName = "";
			}

			if (!LiteralUtils.canInitSafely(expression)) {
				joiner.add(argumentName + "?");
				continue;
			}

			Class<?> returnType = expression.getReturnType();
			argumentTypes.add(returnType);
			ClassInfo<?> classInfo = Classes.getSuperClassInfo(returnType);
			if (expression.isSingle()) {
				joiner.add(argumentName + classInfo.getName().getSingular());
			} else {
				joiner.add(argumentName + classInfo.getName().getPlural());
			}
		}

		String possibleMatch = "";
		var intended = possibleSignatures.stream()
			.filter(signature -> { // filter for signatures that contain all known arguments
				List<Class<?>> currentArgumentTypes = new ArrayList<>(argumentTypes);
				Arrays.stream(signature.parameters().all())
					.map(parameter -> Utils.getComponentType(parameter.type()))
					.forEach(type -> {
						var iterator = currentArgumentTypes.iterator();
						while (iterator.hasNext()) {
							if (type.isAssignableFrom(iterator.next())) {
								iterator.remove();
								break;
							}
						}
					});
				return currentArgumentTypes.isEmpty();
			})
			.min(Comparator.comparingInt(signature -> Math.abs(arguments.length - signature.getMaxParameters())));
		if (intended.isPresent()) {
			possibleMatch = " " + POTENTIAL_SIGNATURE.toString(intended.get().toString(false, false));
		}
		Skript.error(UNKNOWN_FUNCTION.toString(name, joiner) + possibleMatch);
	}

	/**
	 * Attempts to bind every argument in {@code arguments} to the matching target in {@code targets}.
	 *
	 * @param arguments The arguments to bind.
	 * @param targets   The target data to bind each argument with.
	 * @return An {@link ArgumentParseResult} with the results.
	 */
	private ArgumentParseResult bindArguments(Argument<T>[] arguments, ArgumentParseTarget[] targets) {
		assert arguments.length == targets.length;
		assert Arrays.stream(targets).map(ArgumentParseTarget::type).noneMatch(Class::isArray);

		//noinspection unchecked
		Argument<Expression<?>>[] parsed = (Argument<Expression<?>>[]) new Argument[arguments.length];

		for (int i = 0; i < arguments.length; i++) {
			Argument<T> argument = arguments[i];
			ArgumentParseTarget targetData = targets[i];

			Expression<?> expression = binder.bind(argument, targetData);
			if (expression == null) {
				return new ArgumentParseResult(ArgumentParseResultType.PARSE_FAIL, null);
			}

			if (expression instanceof ExpressionList<?> list && !list.getAnd()) {
				Skript.error(Language.get("functions.or in arguments"));
				return new ArgumentParseResult(ArgumentParseResultType.LIST_ERROR, null);
			}

			parsed[i] = new Argument<>(argument.type(), argument.name(), expression);
		}

		return new ArgumentParseResult(ArgumentParseResultType.OK, parsed);
	}

	/**
	 * Supplies the steps of {@link FunctionBinder} which depend on how an argument's value is
	 * represented.
	 *
	 * @param <T> The type an argument's value is represented by.
	 */
	interface ArgumentBinder<T> {

		/**
		 * Binds a single argument to an expression of the target type.
		 *
		 * @param argument The argument.
		 * @param target   The target type to bind to, and the fallback value.
		 * @return {@code target.fallback()} if the argument does not have a value.
		 * 	Otherwise, the bound expression, or null if it could not be bound.
		 */
		@Nullable Expression<?> bind(Argument<T> argument, ArgumentParseTarget target);

		/**
		 * Joins every passed argument into a single argument, for binding against a signature
		 * whose only parameter accepts a list.
		 *
		 * @param arguments     The passed arguments. May be empty.
		 * @param parameterName The name of the parameter the result will be bound to.
		 * @return The joined argument.
		 */
		Argument<T> joinForList(Argument<T>[] arguments, String parameterName);

		/**
		 * Returns the value of {@code argument} as it was originally written, used when an argument
		 * turns out not to have been named after all.
		 *
		 * @param argument The argument.
		 * @return The raw value.
		 */
		@Nullable T rawValue(Argument<T> argument);

		/**
		 * Describes an argument as an expression, so that its type can be named in an error
		 * message. The result is only used for logging and need not be initialisable.
		 *
		 * @param argument The argument.
		 * @return An expression describing the argument, or null if it cannot be described.
		 */
		@Nullable Expression<?> describe(Argument<T> argument);

	}

	/**
	 * The type of result from attempting to bind function arguments.
	 */
	enum ArgumentParseResultType {

		/**
		 * All arguments were successfully bound to the specified type.
		 */
		OK,

		/**
		 * An argument failed to bind to the specified target type.
		 */
		PARSE_FAIL,

		/**
		 * An expression list contained "or", thus parsing should be stopped.
		 */
		LIST_ERROR

	}

	/**
	 * The results of attempting to bind function arguments.
	 *
	 * @param type   The type of result.
	 * @param parsed The resulting bound arguments, or null if binding was not successful.
	 */
	record ArgumentParseResult(
		@NotNull ArgumentParseResultType type,
		Argument<Expression<?>>[] parsed) {

	}

	/**
	 * Represents the data related to binding an argument to a target type.
	 *
	 * @param type     The target type.
	 * @param fallback If the argument cannot be bound as the target type, the expression to use as fallback.
	 */
	record ArgumentParseTarget(@NotNull Class<?> type, @Nullable Expression<?> fallback) {

	}

}
