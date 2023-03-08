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
package org.skriptlang.skript.lang.converter;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converters are used to provide Skript with specific instructions for converting an object to a different type.
 * @see #registerConverter(Class, Class, Converter)
 */
public final class Converters {

	private Converters() {}

	/**
	 * A List containing information for all registered converters.
	 */
	private static final List<ConverterInfo<?, ?>> CONVERTERS = Collections.synchronizedList(new ArrayList<>(50));

	/**
	 * @return An unmodifiable, synchronized list containing all registered {@link ConverterInfo}s.
	 * When traversing this list, please refer to {@link Collections#synchronizedList(List)} to ensure that
	 *  the list is properly traversed due to its synchronized status.
	 * Please note that this does not include any special Converters resolved by Skript during runtime.
	 * This method ONLY returns converters explicitly registered during registration.
	 * Thus, it is recommended to use {@link #getConverter(Class, Class)}.
	 */
	public static List<ConverterInfo<?, ?>> getConverterInfo() {
		return Collections.unmodifiableList(CONVERTERS);
	}

	/**
	 * A map for quickly access converters that have already been resolved.
	 * This is useful for skipping complex lookups that may require chaining.
	 */
	private static final Map<Integer, ConverterInfo<?, ?>> QUICK_ACCESS_CONVERTERS = Collections.synchronizedMap(new HashMap<>(50));

	/**
	 * Registers a new Converter with Skript's collection of Converters.
	 * @param from The type to convert from.
	 * @param to The type to convert to.
	 * @param converter A Converter for converting objects of type 'from' to type 'to'.
	 */
	public static <F, T> void registerConverter(Class<F> from, Class<T> to, Converter<F, T> converter) {
		registerConverter(from, to, converter, Converter.ALL_CHAINING);
	}

	/**
	 * Registers a new Converter with Skript's collection of Converters.
	 * @param from The type to convert from.
	 * @param to The type to convert to.
	 * @param converter A Converter for converting objects of type 'from' to type 'to'.
	 * @param flags Flags to set for the Converter. Flags can be found under {@link Converter}.
	 */
	public static <F, T> void registerConverter(Class<F> from, Class<T> to, Converter<F, T> converter, int flags) {
		Skript.checkAcceptRegistrations();

		ConverterInfo<F, T> info = new ConverterInfo<>(from, to, converter, flags);

		if (exactConverterExists(from, to)) {
			throw new SkriptAPIException(
				"A Converter from '" + from + "' to '" + to + " already exists!"
			);
		}

		CONVERTERS.add(info);
	}

	/**
	 * This method is to be called after Skript has finished registration.
	 * It allows {@link ChainedConverter}s to be created so that Skript may do more complex conversions
	 *  involving multiple converters.
	 */
	// TODO Find a better way of doing this that doesn't require a method to be called (probably requires better Registration API)
	// REMIND how to manage overriding of converters? - shouldn't actually matter
	@SuppressWarnings("unchecked")
	public static <F, M, T> void createChainedConverters() {
		for (int i = 0; i < CONVERTERS.size(); i++) {

			ConverterInfo<?, ?> unknownInfo1 = CONVERTERS.get(i);
			for (int j = 0; j < CONVERTERS.size(); j++) { // Not from j = i+1 since new converters get added during the loops

				ConverterInfo<?, ?> unknownInfo2 = CONVERTERS.get(j);

				// chain info -> info2
				if (
					unknownInfo2.getFrom() != Object.class // Object can only exist at the beginning of a chain
					&& (unknownInfo1.getFlags() & Converter.NO_RIGHT_CHAINING) == 0
					&& (unknownInfo2.getFlags() & Converter.NO_LEFT_CHAINING) == 0
					&& unknownInfo2.getFrom().isAssignableFrom(unknownInfo1.getTo())
					&& !exactConverterExists(unknownInfo1.getFrom(), unknownInfo2.getTo())
				) {
					ConverterInfo<F, M> info1 = (ConverterInfo<F, M>) unknownInfo1;
					ConverterInfo<M, T> info2 = (ConverterInfo<M, T>) unknownInfo2;

					CONVERTERS.add(new ConverterInfo<>(
						info1.getFrom(),
						info2.getTo(),
						new ChainedConverter<>(info1.getConverter(), info2.getConverter()),
						info1.getFlags() | info2.getFlags()
					));
				}

				// chain info2 -> info
				else if (
					unknownInfo1.getFrom() != Object.class // Object can only exist at the beginning of a chain
					&& (unknownInfo1.getFlags() & Converter.NO_LEFT_CHAINING) == 0
					&& (unknownInfo2.getFlags() & Converter.NO_RIGHT_CHAINING) == 0
					&& unknownInfo1.getFrom().isAssignableFrom(unknownInfo2.getTo())
					&& !exactConverterExists(unknownInfo2.getFrom(), unknownInfo1.getTo())
				) {
					ConverterInfo<M, T> info1 = (ConverterInfo<M, T>) unknownInfo1;
					ConverterInfo<F, M> info2 = (ConverterInfo<F, M>) unknownInfo2;

					CONVERTERS.add(new ConverterInfo<>(
						info2.getFrom(),
						info1.getTo(),
						new ChainedConverter<>(info2.getConverter(), info1.getConverter()),
						info2.getFlags() | info1.getFlags()
					));
				}

			}

		}
	}

	/**
	 * Internal method.
	 * @return Whether a Converter exists that EXACTLY matches the provided types.
	 */
	private static boolean exactConverterExists(Class<?> from, Class<?> to) {
		for (ConverterInfo<?, ?> info : CONVERTERS) {
			if (from == info.getFrom() && to == info.getTo())
				return true;
		}
		return false;
	}

	/**
	 * @return Whether a Converter capable of converting 'fromType' to 'toType' exists.
	 */
	public static boolean converterExists(Class<?> fromType, Class<?> toType) {
		if (toType.isAssignableFrom(fromType) || fromType.isAssignableFrom(toType))
			return true;
		return getConverter(fromType, toType) != null;
	}

	/**
	 * @return Whether a Converter capable of converting 'fromType' to one of the provided 'toTypes' exists.
	 */
	public static boolean converterExists(Class<?> fromType, Class<?>... toTypes) {
		for (Class<?> toType : toTypes) {
			if (converterExists(fromType, toType))
				return true;
		}
		return false;
	}

	/**
	 * A method for obtaining the ConverterInfo of a Converter that can convert
	 *  an object of 'fromType' into an object of 'toType'.
	 * @param fromType The type to convert from.
	 * @param toType The type to convert to.
	 * @return The ConverterInfo of a Converter capable of converting an object of 'fromType' into an object of 'toType'.
	 * Will return null if no such Converter exists.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <F, T> ConverterInfo<F, T> getConverterInfo(Class<F> fromType, Class<T> toType) {
		int hash = Objects.hash(fromType, toType);

		ConverterInfo<F, T> info = (ConverterInfo<F, T>) QUICK_ACCESS_CONVERTERS.get(hash);

		if (info == null) { // Manual lookup
			info = getConverter_i(fromType, toType);
			QUICK_ACCESS_CONVERTERS.put(hash, info);
		}

		return info;
	}

	/**
	 * A method for obtaining a Converter that can convert an object of 'fromType' into an object of 'toType'.
	 * @param fromType The type to convert from.
	 * @param toType The type to convert to.
	 * @return A Converter capable of converting an object of 'fromType' into an object of 'toType'.
	 * Will return null if no such Converter exists.
	 */
	@Nullable
	public static <F, T> Converter<F, T> getConverter(Class<F> fromType, Class<T> toType) {
		ConverterInfo<F, T> info = getConverterInfo(fromType, toType);
		return info != null ? info.getConverter() : null;
	}

	/**
	 * The internal method for obtaining the ConverterInfo of a Converter that can convert
	 *  an object of 'fromType' into an object of 'toType'.
	 *
	 * @param fromType The type to convert from.
	 * @param toType The type to convert to.
	 * @return The ConverterInfo of a Converter capable of converting an object of 'fromType' into an object of 'toType'.
	 * Will return null if no such Converter exists.
	 *
	 * @param <F> The type to convert from.
	 * @param <T> The type to convert to.
	 * @param <SubType> The 'fromType' for a Converter that may only convert certain objects of 'fromType'
	 * @param <ParentType> The 'toType' for a Converter that may only sometimes convert objects of 'fromType'
	 * into objects of 'toType' (e.g. the converted object may only share a parent with 'toType')
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	private static <F, T extends ParentType, SubType extends F, ParentType> ConverterInfo<F, T> getConverter_i(
		Class<F> fromType,
		Class<T> toType
	) {
		// Check for an exact match
		for (ConverterInfo<?, ?> info : CONVERTERS) {
			if (fromType == info.getFrom() && toType == info.getTo())
				return (ConverterInfo<F, T>) info;
		}

		// Check for an almost perfect match
		for (ConverterInfo<?, ?> info : CONVERTERS) {
			if (info.getFrom().isAssignableFrom(fromType) && toType.isAssignableFrom(info.getTo()))
				return (ConverterInfo<F, T>) info;
		}

		// We don't want to create "maybe" converters for 'Object -> X' conversions
		// Instead, we should just try and convert during runtime when we have a better idea of the fromType
		if (fromType == Object.class)
			return new ConverterInfo<>(fromType, toType, fromObject -> Converters.convert(fromObject, toType), 0);

		// Attempt to find converters that have either 'from' OR 'to' not exactly matching
		for (ConverterInfo<?, ?> unknownInfo : CONVERTERS) {
			if (unknownInfo.getFrom().isAssignableFrom(fromType) && unknownInfo.getTo().isAssignableFrom(toType)) {
				ConverterInfo<F, ParentType> info = (ConverterInfo<F, ParentType>) unknownInfo;

				// 'to' doesn't exactly match and needs to be filtered
				// Basically, this converter might convert 'F' into something that's shares a parent with 'T'
				return new ConverterInfo<>(fromType, toType, fromObject -> {
					Object converted = info.getConverter().convert(fromObject);
					if (toType.isInstance(converted))
						return (T) converted;
					return null;
				}, 0);

			} else if (fromType.isAssignableFrom(unknownInfo.getFrom()) && toType.isAssignableFrom(unknownInfo.getTo())) {
				ConverterInfo<SubType, T> info = (ConverterInfo<SubType, T>) unknownInfo;

				// 'from' doesn't exactly match and needs to be filtered
				// Basically, this converter will only convert certain 'F' objects
				return new ConverterInfo<>(fromType, toType, fromObject -> {
					if (!info.getFrom().isInstance(fromType))
						return null;
					return info.getConverter().convert((SubType) fromObject);
				}, 0);

			}
		}

		// At this point, accept both 'from' AND 'to' not exactly matching
		for (ConverterInfo<?, ?> unknownInfo : CONVERTERS) {
			if (fromType.isAssignableFrom(unknownInfo.getFrom()) && unknownInfo.getTo().isAssignableFrom(toType)) {
				ConverterInfo<SubType, ParentType> info = (ConverterInfo<SubType, ParentType>) unknownInfo;

				// 'from' and 'to' both don't exactly match and need to be filtered
				// Basically, this converter will only convert certain 'F' objects
				//   and some conversion results will only share a parent with 'T'
				return new ConverterInfo<>(fromType, toType, fromObject -> {
					if (!info.getFrom().isInstance(fromObject))
						return null;
					Object converted = info.getConverter().convert((SubType) fromObject);
					if (toType.isInstance(converted))
						return (T) converted;
					return null;
				}, 0);

			}
		}

		// No converter available
		return null;
	}

	/**
	 * Standard method for converting an object into a different type.
	 * @param from The object to convert.
	 * @param toType The type that 'from' should be converted into.
	 * @return An object of 'toType', or null if 'from' couldn't be successfully converted.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <From, To> To convert(@Nullable From from, Class<To> toType) {
		if (from == null)
			return null;

		if (toType.isInstance(from))
			return (To) from;

		Converter<From, To> converter = getConverter((Class<From>) from.getClass(), toType);
		if (converter == null)
			return null;

		return converter.convert(from);
	}

	/**
	 * A method for converting an object into one of several provided types.
	 * @param from The object to convert.
	 * @param toTypes A list of types that should be tried for converting 'from'.
	 * @return An object of one of the provided 'toTypes', or null if 'from' couldn't successfully be converted.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <From, To> To convert(@Nullable From from, Class<? extends To>[] toTypes) {
		if (from == null)
			return null;

		for (Class<? extends To> toType : toTypes) {
			if (toType.isInstance(from))
				return (To) from;
		}

		Class<From> fromType = (Class<From>) from.getClass();
		for (Class<? extends To> toType : toTypes) {
			Converter<From, ? extends To> converter = getConverter(fromType, toType);
			if (converter != null)
				return converter.convert(from);
		}

		return null;
	}

	/**
	 * Standard method for bulk-conversion of objects into a different type.
	 * @param from The objects to convert.
	 * @param toType The type that 'from' should be converted into.
	 * @return Objects of 'toType'. Will return null if 'from' is null.
	 * Please note that the returned array may not be the same size as 'from'.
	 * This can happen if an object contained within 'from' is not successfully converted.
	 */
	@SuppressWarnings("unchecked")
	public static <To> To[] convert(@Nullable Object[] from, Class<To> toType) {
		//noinspection ConstantConditions
		if (from == null)
			return (To[]) Array.newInstance(toType, 0);

		if (toType.isAssignableFrom(from.getClass().getComponentType()))
			return (To[]) from;

		List<To> converted = new ArrayList<>(from.length);
		for (Object fromSingle : from) {
			To convertedSingle = convert(fromSingle, toType);
			if (convertedSingle != null)
				converted.add(convertedSingle);
		}

		return converted.toArray((To[]) Array.newInstance(toType, converted.size()));
	}

	/**
	 * A method for bulk-conversion of objects into one of several provided types.
	 * @param from The objects to convert.
	 * @param toTypes A list of types that should be tried for converting each object.
	 * @param superType A parent type of all provided 'toTypes'.
	 * @return Objects of 'superType'. Will return any empty array if 'from' is null.
	 * Please note that the returned array may not be the same size as 'from'.
	 * This can happen if an object contained within 'from' is not successfully converted.
	 * And, of course, the returned array may contain objects of a different type.
	 */
	@SuppressWarnings("unchecked")
	public static <To> To[] convert(@Nullable Object[] from, Class<? extends To>[] toTypes, Class<To> superType) {
		//noinspection ConstantConditions
		if (from == null)
			return (To[]) Array.newInstance(superType, 0);

		Class<?> fromType = from.getClass().getComponentType();

		for (Class<? extends To> toType : toTypes) {
			if (toType.isAssignableFrom(fromType))
				return (To[]) from;
		}

		List<To> converted = new ArrayList<>(from.length);
		for (Object fromSingle : from) {
			To convertedSingle = convert(fromSingle, toTypes);
			if (convertedSingle != null)
				converted.add(convertedSingle);
		}

		return converted.toArray((To[]) Array.newInstance(superType, converted.size()));
	}

	/**
	 * A method for bulk-converting objects of a specific type using a specific Converter.
	 * @param from The objects to convert.
	 * @param toType The type to convert into.
	 * @param converter The converter to use for conversion.
	 * @return Objects of 'toType'.
	 * Please note that the returned array may not be the same size as 'from'.
	 * This can happen if an object contained within 'from' is not successfully converted.
	 */
	@SuppressWarnings("unchecked")
	public static <From, To> To[] convert(From[] from, Class<To> toType, Converter<? super From, ? extends To> converter) {
		To[] converted = (To[]) Array.newInstance(toType, from.length);

		int j = 0;
		for (From fromSingle : from) {
			To convertedSingle = fromSingle == null ? null : converter.convert(fromSingle);
			if (convertedSingle != null)
				converted[j++] = convertedSingle;
		}

		if (j != converted.length)
			converted = Arrays.copyOf(converted, j);

		return converted;
	}

	/**
	 * A method that guarantees an object of 'toType' is returned.
	 * @param from The object to convert.
	 * @param toType The type to convert into.
	 * @return An object of 'toType'.
	 * @throws ClassCastException If 'from' cannot be converted.
	 */
	public static <To> To convertStrictly(Object from, Class<To> toType) {
		To converted = convert(from, toType);
		if (converted == null)
			throw new ClassCastException("Cannot convert '" + from + "' to an object of type '" + toType + "'");
		return converted;
	}

	/**
	 * A method for bulk-conversion that guarantees objects of 'toType' are returned.
	 * @param from The object to convert.
	 * @param toType The type to convert into.
	 * @return Objects of 'toType'. The returned array will be the same size as 'from'.
	 * @throws ClassCastException If any of the provided objects cannot be converted.
	 */
	@SuppressWarnings("unchecked")
	public static <To> To[] convertStrictly(Object[] from, Class<To> toType) {
		To[] converted = (To[]) Array.newInstance(toType, from.length);

		for (int i = 0; i < from.length; i++) {
			To convertedSingle = convert(from[i], toType);
			if (convertedSingle == null)
				throw new ClassCastException("Cannot convert '" + from[i] + "' to an object of type '" + toType + "'");
			converted[i] = convertedSingle;
		}

		return converted;
	}

	/**
	 * A method for bulk-converting objects of a specific type using a specific Converter.
	 * @param from The objects to convert.
	 * @param toType A superclass for all objects to be converted.
	 * @param converter The converter to use for conversion.
	 * @return Objects of 'toType'.
	 * Please note that the returned array may not be the same size as 'from'.
	 * This can happen if an object contained within 'from' is not successfully converted.
	 * @throws ArrayStoreException If 'toType' is not a superclass of all objects returned by the converter.
	 * @throws ClassCastException If 'toType' is not of 'T'.
	 */
	@SuppressWarnings("unchecked")
	public static <From, To> To[] convertUnsafe(From[] from, Class<?> toType, Converter<? super From, ? extends To> converter) {
		return convert(from, (Class<To>) toType, converter);
	}

}
