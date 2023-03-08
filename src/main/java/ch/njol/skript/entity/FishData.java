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
package ch.njol.skript.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Cod;
import org.bukkit.entity.Fish;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Salmon;
import org.bukkit.entity.Tadpole;
import org.bukkit.entity.TropicalFish;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;

public class FishData extends EntityData<Fish> {

	private static final boolean HAS_TADPOLE = Skript.classExists("org.bukkit.entity.Tadpole");
	private static final List<String> PATTERNS;

	static {
		PATTERNS = new ArrayList<>(Arrays.asList("fish", "cod", "puffer fish", "salmon", "tropical fish"));
		if (HAS_TADPOLE)
			PATTERNS.add("tadpole");
		register(FishData.class, "fish", Fish.class, 0, PATTERNS.toArray(new String[0]));
	}
	
	private boolean init = true;
	private boolean wildcard = false;
	private int pattern = -1;
	
	public FishData() {}
	
	public FishData(int pattern) {
		this.pattern = pattern;
		super.matchedPattern = pattern;
	}
	
	@Override
	protected boolean init(Literal<?>[] exprs, int matchedPattern, ParseResult parseResult) {
		if (matchedPattern == 0)
			wildcard = true;
		pattern = (matchedPattern == 0) ? ThreadLocalRandom.current().nextInt(1, PATTERNS.size()) : matchedPattern;
		return true;
	}

	@Override
	protected boolean init(@Nullable Class<? extends Fish> c, @Nullable Fish e) {
		int matchedPattern = getInitPattern(e);
		if (matchedPattern == 0)
			wildcard = true;
		pattern = (matchedPattern == 0) ? ThreadLocalRandom.current().nextInt(1, PATTERNS.size()) : matchedPattern;
		return true;
	}

	@Override
	public void set(Fish entity) {
		// Setting of entity already handled by init() functions 
	}

	@Override
	protected boolean match(Fish entity) {
		return wildcard ? true : getPattern(entity) == pattern;
	}

	@Override
	public Class<? extends Fish> getType() {
		if (!init && wildcard)
			return Fish.class;
		init = false;
		switch (pattern) {
			case 1:
				return Cod.class;
			case 2:
				return PufferFish.class;
			case 3:
				return Salmon.class;
			case 4:
				return TropicalFish.class;
			case 5:
				return Tadpole.class;
		}
		return Fish.class;
	}

	@Override
	public EntityData getSuperType() {
		return new FishData(pattern);
	}

	@Override
	protected int hashCode_i() {
		return wildcard ? 0 : pattern;
	}

	@Override
	protected boolean equals_i(EntityData<?> entityData) {
		return entityData instanceof Fish ? (wildcard ? true : getPattern((Fish) entityData) == pattern) : false;
	}

	@Override
	public boolean isSupertypeOf(EntityData<?> entityData) {
		return entityData instanceof Fish ? (wildcard ? true : getPattern((Fish) entityData) == pattern) : false;
	}
	
	private static int getInitPattern(@Nullable Fish fish) {
		if (fish == null)
			return 0;
		else if (fish instanceof Cod)
			return 1;
		else if (fish instanceof PufferFish)
			return 2;
		else if (fish instanceof Salmon)
			return 3;
		else if (fish instanceof TropicalFish)
			return 4;
		else if (HAS_TADPOLE && fish instanceof Tadpole)
			return 5;
		return 0;
	}
	
	private int getPattern(@Nullable Fish fish) {
		int p = getInitPattern(fish);
		return p == 0 ? pattern : p;
	}
	
}
