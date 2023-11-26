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
package ch.njol.skript.util;

import org.bukkit.Location;

/**
 * A class that contains methods based around
 * making it easier to deal with {@link Location}
 * objects.
 */
public class LocationUtils {

	/**
	 * Normalizes the location by ensuring that the pitch is between -90 and 90, the yaw is between -180 and 180,
	 * and all -0.0 values are converted to 0.0.
	 *
	 * @param location The location to normalize
	 * @return The same location with normalized values.
	 */
	public static Location normalize(Location location) {
		location.setPitch(Location.normalizePitch(location.getPitch()));
		location.setYaw(Location.normalizeYaw(location.getYaw()));

		if (location.getYaw() == -0.0)
			location.setYaw(0);
		if (location.getPitch() == -0.0)
			location.setPitch(0);
		if (location.getX() == -0.0)
			location.setX(0);
		if (location.getY() == -0.0)
			location.setY(0);
		if (location.getZ() == -0.0)
			location.setZ(0);

		return location;
	}

	/**
	 * Compares two locations without taking into account their yaw or pitch.
	 *
	 * @param loc1 The first location
	 * @param loc2 The second location
	 * @return Whether the x, y, z, and world of the two locations are the same.
	 */
	public static boolean compareWithoutDirection(Location loc1, Location loc2) {
		return compareWithoutDirection(loc1, loc2, 0.0);
	}

	/**
	 * Compares two locations without taking into account their yaw or pitch, with a tolerance for the difference
	 * between the x, y, and z values. This essentially checks if loc2 is within a cube of side length 2*epsilon centered
	 * around loc1.
	 * <p>
	 * Intended for rough comparisons of locations in the future.
	 *
	 * @param loc1 The first location
	 * @param loc2 The second location
	 * @param epsilon The maximum difference between each of the x, y, and z values of the two locations.
	 * @return Whether the x, y, z, and world of the two locations are the same.
	 */
	public static boolean compareWithoutDirection(Location loc1, Location loc2, double epsilon) {
		return loc1.getWorld().equals(loc2.getWorld()) &&
				Math.abs(loc1.getX() - loc2.getX()) <= epsilon &&
				Math.abs(loc1.getY() - loc2.getY()) <= epsilon &&
				Math.abs(loc1.getZ() - loc2.getZ()) <= epsilon;
	}

}
