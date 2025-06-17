package org.skriptlang.skript.test.tests.syntaxes.events;

import ch.njol.skript.test.runner.SkriptJUnitTest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Sheep;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skriptlang.skript.bukkit.vector.FastVector;

public class EvtVehicleCollisionTest extends SkriptJUnitTest {

	private Pig pig;
	private Sheep sheep;
	private Material old;

	@Before
	public void setUp() {
		pig = spawnTestPig();
		sheep = spawnTestEntity(EntityType.SHEEP);
		old = getBlock().getType();
		setBlock(Material.OBSIDIAN);
	}

	@Test
	public void test() {
		Bukkit.getPluginManager().callEvent(new VehicleBlockCollisionEvent(pig, getBlock(), new FastVector()));
		Bukkit.getPluginManager().callEvent(new VehicleEntityCollisionEvent(pig, sheep));
	}

	@After
	public void cleanUp() {
		pig.remove();
		sheep.remove();
		setBlock(old);
	}

}
