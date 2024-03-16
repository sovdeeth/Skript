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
package ch.njol.skript.bukkitutil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import io.papermc.paper.entity.TeleportFlag;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * A utility interface to access the Entity::teleport with TeleportFlag vararg in Paper 1.19+.
 */
@FunctionalInterface
public interface TeleportFlags {

	public enum SkriptTeleportFlags {
		RETAIN_OPEN_INVENTORY(TeleportFlag.EntityState.RETAIN_OPEN_INVENTORY),
		RETAIN_PASSENGERS(TeleportFlag.EntityState.RETAIN_PASSENGERS),
		RETAIN_VEHICLE(TeleportFlag.EntityState.RETAIN_VEHICLE),
		RETAIN_DIRECTION(),
		RETAIN_PITCH(TeleportFlag.Relative.PITCH),
		RETAIN_YAW(TeleportFlag.Relative.YAW),
		RETAIN_X(TeleportFlag.Relative.X),
		RETAIN_Y(TeleportFlag.Relative.Y),
		RETAIN_Z(TeleportFlag.Relative.Z);

		TeleportFlag flag;

		SkriptTeleportFlags() {}

		SkriptTeleportFlags(TeleportFlag flag) {
			this.flag = flag;
		}

	}

	void teleport(
		@NotNull Entity entity, @NotNull Location location, @NotNull TeleportFlag... flags
	);

	static void teleport(
		@NotNull TeleportFlags teleportFlag, @NotNull Entity entity, @NotNull Location location, @NotNull SkriptTeleportFlags... teleportFlags
	) {
		if (location.getWorld() == null) {
			location = location.clone();
			location.setWorld(entity.getWorld());
		}

		List<TeleportFlag> flags = Arrays.stream(teleportFlags).flatMap(flag -> {
			if (flag == SkriptTeleportFlags.RETAIN_DIRECTION)
				return Stream.of(TeleportFlag.Relative.PITCH, TeleportFlag.Relative.YAW);
			return Stream.of(flag.flag);
		}).filter(Objects::nonNull).toList();
		entity.teleport(location, flags.toArray(TeleportFlag[]::new));
	}

}
