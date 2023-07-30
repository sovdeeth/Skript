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
package ch.njol.skript.classes.data;

import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Panda.Gene;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent.QuitReason;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.Metadatable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.CachedServerIcon;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.EnchantmentUtils;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.ConfigurationSerializer;
import ch.njol.skript.classes.EnumClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.ExprDamageCause;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.localization.Language;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.BlockUtils;
import ch.njol.skript.util.EnchantmentType;
import ch.njol.skript.util.PotionEffectUtils;
import ch.njol.skript.util.StringMode;
import ch.njol.util.StringUtils;
import ch.njol.yggdrasil.Fields;
import io.papermc.paper.world.MoonPhase;

/**
 * @author Peter Güttinger
 */
public class BukkitClasses {

	public BukkitClasses() {}

	public static final Pattern UUID_PATTERN = Pattern.compile("(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}");

	static {
		final boolean GET_ENTITY_METHOD_EXISTS = Skript.methodExists(Bukkit.class, "getEntity", UUID.class);
		Classes.registerClass(new ClassInfo<>(Entity.class, "entity")
				.user("entit(y|ies)")
				.name("Entity")
				.description("An entity is something in a <a href='#world'>world</a> that's not a <a href='#block'>block</a>, " +
						"e.g. a <a href='#player'>player</a>, a skeleton, or a zombie, but also " +
						"<a href='#projectile'>projectiles</a> like arrows, fireballs or thrown potions, " +
						"or special entities like dropped items, falling blocks or paintings.")
				.usage("player, op, wolf, tamed ocelot, powered creeper, zombie, unsaddled pig, fireball, arrow, dropped item, item frame, etc.")
				.examples("entity is a zombie or creeper",
						"player is an op",
						"projectile is an arrow",
						"shoot a fireball from the player")
				.since("1.0")
				.defaultExpression(new EventValueExpression<>(Entity.class))
				.parser(new Parser<Entity>() {
					@Override
					@Nullable
					public Entity parse(final String s, final ParseContext context) {
						UUID uuid;
						try {
							uuid = UUID.fromString(s);
						} catch (IllegalArgumentException iae) {
							return null;
						}
						if (GET_ENTITY_METHOD_EXISTS) {
							return Bukkit.getEntity(uuid);
						} else {
							for (World world : Bukkit.getWorlds()) {
								for (Entity entity : world.getEntities()) {
									if (entity.getUniqueId().equals(uuid)) {
										return entity;
									}
								}
							}
						}
						return null;
					}
					
					@Override
					public boolean canParse(final ParseContext context) {
						return context == ParseContext.COMMAND;
					}
					
					@Override
					public String toVariableNameString(final Entity e) {
						return "entity:" + e.getUniqueId().toString().toLowerCase(Locale.ENGLISH);
					}

					@Override
					public String toString(final Entity e, final int flags) {
						return EntityData.toString(e, flags);
					}
				})
				.changer(DefaultChangers.entityChanger));
		
		Classes.registerClass(new ClassInfo<>(LivingEntity.class, "livingentity")
				.user("living ?entit(y|ies)")
				.name("Living Entity")
				.description("A living <a href='#entity'>entity</a>, i.e. a mob or <a href='#player'>player</a>, " +
						"not inanimate entities like <a href='#projectile'>projectiles</a> or dropped items.")
				.usage("see <a href='#entity'>entity</a>, but ignore inanimate objects")
				.examples("spawn 5 powered creepers",
						"shoot a zombie from the creeper")
				.since("1.0")
				.defaultExpression(new EventValueExpression<>(LivingEntity.class))
				.changer(DefaultChangers.entityChanger));
		
		Classes.registerClass(new ClassInfo<>(Projectile.class, "projectile")
				.user("projectiles?")
				.name("Projectile")
				.description("A projectile, e.g. an arrow, snowball or thrown potion.")
				.usage("arrow, fireball, snowball, thrown potion, etc.")
				.examples("projectile is a snowball",
						"shoot an arrow at speed 5 from the player")
				.since("1.0")
				.defaultExpression(new EventValueExpression<>(Projectile.class))
				.changer(DefaultChangers.nonLivingEntityChanger));
		
		Classes.registerClass(new ClassInfo<>(Block.class, "block")
				.user("blocks?")
				.name("Block")
				.description("A block in a <a href='#world'>world</a>. It has a <a href='#location'>location</a> and a <a href='#itemstack'>type</a>, " +
						"and can also have a <a href='#direction'>direction</a> (mostly a <a href='expressions.html#ExprFacing'>facing</a>), an <a href='#inventory'>inventory</a>, or other special properties.")
				.usage("")
				.examples("")
				.since("1.0")
				.defaultExpression(new EventValueExpression<>(Block.class))
				.parser(new Parser<Block>() {
					@Override
					@Nullable
					public Block parse(final String s, final ParseContext context) {
						return null;
					}
					
					@Override
					public boolean canParse(final ParseContext context) {
						return false;
					}
					
					@Override
					public String toString(final Block b, final int flags) {
						return BlockUtils.blockToString(b, flags);
					}
					
					@Override
					public String toVariableNameString(final Block b) {
						return b.getWorld().getName() + ":" + b.getX() + "," + b.getY() + "," + b.getZ();
					}

					@Override
					public String getDebugMessage(final Block b) {
						return toString(b, 0) + " block (" + b.getWorld().getName() + ":" + b.getX() + "," + b.getY() + "," + b.getZ() + ")";
					}
				})
				.changer(DefaultChangers.blockChanger)
				.serializer(new Serializer<Block>() {
					@Override
					public Fields serialize(final Block b) {
						final Fields f = new Fields();
						f.putObject("world", b.getWorld());
						f.putPrimitive("x", b.getX());
						f.putPrimitive("y", b.getY());
						f.putPrimitive("z", b.getZ());
						return f;
					}
					
					@Override
					public void deserialize(final Block o, final Fields f) {
						assert false;
					}
					
					@Override
					protected Block deserialize(final Fields fields) throws StreamCorruptedException {
						final World w = fields.getObject("world", World.class);
						final int x = fields.getPrimitive("x", int.class), y = fields.getPrimitive("y", int.class), z = fields.getPrimitive("z", int.class);
						if (w == null)
							throw new StreamCorruptedException();
						return w.getBlockAt(x, y, z);
					}
					
					@Override
					public boolean mustSyncDeserialization() {
						return true;
					}
					
					@Override
					public boolean canBeInstantiated() {
						return false;
					}
					
					// return b.getWorld().getName() + ":" + b.getX() + "," + b.getY() + "," + b.getZ();
					@Override
					@Nullable
					public Block deserialize(final String s) {
						final String[] split = s.split("[:,]");
						if (split.length != 4)
							return null;
						final World w = Bukkit.getWorld(split[0]);
						if (w == null)
							return null;
						try {
							final int[] l = new int[3];
							for (int i = 0; i < 3; i++)
								l[i] = Integer.parseInt(split[i + 1]);
							return w.getBlockAt(l[0], l[1], l[2]);
						} catch (final NumberFormatException e) {
							return null;
						}
					}
				}));

		Classes.registerClass(new ClassInfo<>(BlockData.class, "blockdata")
				.user("block ?datas?")
				.name("Block Data")
				.description("Block data is the detailed information about a block, referred to in Minecraft as BlockStates, " +
						"allowing for the manipulation of different aspects of the block, including shape, waterlogging, direction the block is facing, " +
						"and so much more. Information regarding each block's optional data can be found on Minecraft's Wiki. Find the block you're " +
						"looking for and scroll down to 'Block States'. Different states must be separated by a semicolon (see examples). " +
						"The 'minecraft:' namespace is optional, as well as are underscores.")
				.examples("set block at player to campfire[lit=false]",
						"set target block of player to oak stairs[facing=north;waterlogged=true]",
						"set block at player to grass_block[snowy=true]",
						"set loop-block to minecraft:chest[facing=north]",
						"set block above player to oak_log[axis=y]",
						"set target block of player to minecraft:oak_leaves[distance=2;persistent=false]")
				.after("itemtype")
				.since("2.5")
				.parser(new Parser<BlockData>() {
					@Nullable
					@Override
					public BlockData parse(String input, ParseContext context) {
						return BlockUtils.createBlockData(input);
					}
	
					@Override
					public String toString(BlockData o, int flags) {
						return o.getAsString().replace(",", ";");
					}
	
					@Override
					public String toVariableNameString(BlockData o) {
						return "blockdata:" + o.getAsString();
					}
				})
				.serializer(new Serializer<BlockData>() {
					@Override
					public Fields serialize(BlockData o) {
						Fields f = new Fields();
						f.putObject("blockdata", o.getAsString());
						return f;
					}
	
					@Override
					public void deserialize(BlockData o, Fields f) {
						assert false;
					}
	
					@Override
					protected BlockData deserialize(Fields f) throws StreamCorruptedException {
						String data = f.getObject("blockdata", String.class);
						assert data != null;
						try {
							return Bukkit.createBlockData(data);
						} catch (IllegalArgumentException ex) {
							throw new StreamCorruptedException("Invalid block data: " + data);
						}
					}
	
					@Override
					public boolean mustSyncDeserialization() {
						return true;
					}
	
					@Override
					protected boolean canBeInstantiated() {
						return false;
					}
				}));

		Classes.registerClass(new ClassInfo<>(Location.class, "location")
				.user("locations?")
				.name("Location")
				.description("A location in a <a href='#world'>world</a>. Locations are world-specific and even store a <a href='#direction'>direction</a>, " +
						"e.g. if you save a location and later teleport to it you will face the exact same direction you did when you saved the location.")
				.usage("")
				.examples("")
				.since("1.0")
				.defaultExpression(new EventValueExpression<>(Location.class))
				.parser(new Parser<Location>() {
					@Override
					@Nullable
					public Location parse(final String s, final ParseContext context) {
						return null;
					}
					
					@Override
					public boolean canParse(final ParseContext context) {
						return false;
					}
					
					@Override
					public String toString(final Location l, final int flags) {
						String worldPart = l.getWorld() == null ? "" : " in '" + l.getWorld().getName() + "'"; // Safety: getWorld is marked as Nullable by spigot
						return "x: " + Skript.toString(l.getX()) + ", y: " + Skript.toString(l.getY()) + ", z: " + Skript.toString(l.getZ()) + ", yaw: " + Skript.toString(l.getYaw()) + ", pitch: " + Skript.toString(l.getPitch()) + worldPart;
					}
					
					@Override
					public String toVariableNameString(final Location l) {
						return l.getWorld().getName() + ":" + l.getX() + "," + l.getY() + "," + l.getZ();
					}

					@Override
					public String getDebugMessage(final Location l) {
						return "(" + l.getWorld().getName() + ":" + l.getX() + "," + l.getY() + "," + l.getZ() + "|yaw=" + l.getYaw() + "/pitch=" + l.getPitch() + ")";
					}
				}).serializer(new Serializer<Location>() {
					@Override
					public Fields serialize(Location location) {
						Fields fields = new Fields();
						World world = null;
						try {
							world = location.getWorld();
						} catch (IllegalArgumentException exception) {
							Skript.warning("A location failed to serialize with its defined world, as the world was unloaded.");
						}
						fields.putObject("world", world);
						fields.putPrimitive("x", location.getX());
						fields.putPrimitive("y", location.getY());
						fields.putPrimitive("z", location.getZ());
						fields.putPrimitive("yaw", location.getYaw());
						fields.putPrimitive("pitch", location.getPitch());
						return fields;
					}
					
					@Override
					public void deserialize(final Location o, final Fields f) {
						assert false;
					}
					
					@Override
					public Location deserialize(final Fields f) throws StreamCorruptedException {
						return new Location(f.getObject("world", World.class),
								f.getPrimitive("x", double.class), f.getPrimitive("y", double.class), f.getPrimitive("z", double.class),
								f.getPrimitive("yaw", float.class), f.getPrimitive("pitch", float.class));
					}
					
					@Override
					public boolean canBeInstantiated() {
						return false; // no nullary constructor - also, saving the location manually prevents errors should Location ever be changed
					}
					
					@Override
					public boolean mustSyncDeserialization() {
						return true;
					}
					
					// return l.getWorld().getName() + ":" + l.getX() + "," + l.getY() + "," + l.getZ() + "|" + l.getYaw() + "/" + l.getPitch();
					@Override
					@Nullable
					public Location deserialize(final String s) {
						final String[] split = s.split("[:,|/]");
						if (split.length != 6)
							return null;
						final World w = Bukkit.getWorld(split[0]);
						if (w == null)
							return null;
						try {
							final double[] l = new double[5];
							for (int i = 0; i < 5; i++)
								l[i] = Double.parseDouble(split[i + 1]);
							return new Location(w, l[0], l[1], l[2], (float) l[3], (float) l[4]);
						} catch (final NumberFormatException e) {
							return null;
						}
					}
				})
				.cloner(Location::clone));
		
		Classes.registerClass(new ClassInfo<>(Vector.class, "vector")
				.user("vectors?")
				.name("Vector")
				.description("Vector is a collection of numbers. In Minecraft, 3D vectors are used to express velocities of entities.")
				.usage("vector(x, y, z)")
				.examples("")
				.since("2.2-dev23")
				.defaultExpression(new EventValueExpression<>(Vector.class))
				.parser(new Parser<Vector>() {
					@Override
					@Nullable
					public Vector parse(final String s, final ParseContext context) {
						return null;
					}
					
					@Override
					public boolean canParse(final ParseContext context) {
						return false;
					}
					
					@Override
					public String toString(final Vector vec, final int flags) {
						return "x: " + Skript.toString(vec.getX()) + ", y: " + Skript.toString(vec.getY()) + ", z: " + Skript.toString(vec.getZ());
					}
					
					@Override
					public String toVariableNameString(final Vector vec) {
						return "vector:" + vec.getX() + "," + vec.getY() + "," + vec.getZ();
					}

					@Override
					public String getDebugMessage(final Vector vec) {
						return "(" + vec.getX() + "," + vec.getY() + "," + vec.getZ() + ")";
					}
				})
				.serializer(new Serializer<Vector>() {
					@Override
					public Fields serialize(Vector o) {
						Fields f = new Fields();
						f.putPrimitive("x", o.getX());
						f.putPrimitive("y", o.getY());
						f.putPrimitive("z", o.getZ());
						return f;
					}
					
					@Override
					public void deserialize(Vector o, Fields f) {
						assert false;
					}
					
					@Override
					public Vector deserialize(final Fields f) throws StreamCorruptedException {
						return new Vector(f.getPrimitive("x", double.class), f.getPrimitive("y", double.class), f.getPrimitive("z", double.class));
					}
					
					@Override
					public boolean mustSyncDeserialization() {
						return false;
					}
					
					@Override
					protected boolean canBeInstantiated() {
						return false;
					}
				})
				.cloner(Vector::clone));
		
		Classes.registerClass(new ClassInfo<>(World.class, "world")
				.user("worlds?")
				.name("World")
				.description("One of the server's worlds. Worlds can be put into scripts by surrounding their name with double quotes, e.g. \"world_nether\", " +
						"but this might not work reliably as <a href='#string'>text</a> uses the same syntax.")
				.usage("<code>\"world_name\"</code>, e.g. \"world\"")
				.examples("broadcast \"Hello!\" to the world \"world_nether\"")
				.since("1.0, 2.2 (alternate syntax)")
				.after("string")
				.defaultExpression(new EventValueExpression<>(World.class))
				.parser(new Parser<World>() {
					@SuppressWarnings("null")
					private final Pattern parsePattern = Pattern.compile("(?:(?:the )?world )?\"(.+)\"", Pattern.CASE_INSENSITIVE);
					
					@Override
					@Nullable
					public World parse(final String s, final ParseContext context) {
						// REMIND allow shortcuts '[over]world', 'nether' and '[the_]end' (server.properties: 'level-name=world') // inconsistent with 'world is "..."'
						if (context == ParseContext.COMMAND || context == ParseContext.CONFIG)
							return Bukkit.getWorld(s);
						final Matcher m = parsePattern.matcher(s);
						if (m.matches())
							return Bukkit.getWorld(m.group(1));
						return null;
					}
					
					@Override
					public String toString(final World w, final int flags) {
						return "" + w.getName();
					}
					
					@Override
					public String toVariableNameString(final World w) {
						return "" + w.getName();
					}
				}).serializer(new Serializer<World>() {
					@Override
					public Fields serialize(final World w) {
						final Fields f = new Fields();
						f.putObject("name", w.getName());
						return f;
					}
					
					@Override
					public void deserialize(final World o, final Fields f) {
						assert false;
					}
					
					@Override
					public boolean canBeInstantiated() {
						return false;
					}
					
					@Override
					protected World deserialize(final Fields fields) throws StreamCorruptedException {
						final String name = fields.getObject("name", String.class);
						assert name != null;
						final World w = Bukkit.getWorld(name);
						if (w == null)
							throw new StreamCorruptedException("Missing world " + name);
						return w;
					}
					
					// return w.getName();
					@Override
					@Nullable
					public World deserialize(final String s) {
						return Bukkit.getWorld(s);
					}
					
					@Override
					public boolean mustSyncDeserialization() {
						return true;
					}
				}));
		
		Classes.registerClass(new ClassInfo<>(Inventory.class, "inventory")
				.user("inventor(y|ies)")
				.name("Inventory")
				.description("An inventory of a <a href='#player'>player</a> or <a href='#block'>block</a>. " +
								"Inventories have many effects and conditions regarding the items contained.",
						"An inventory has a fixed amount of <a href='#slot'>slots</a> which represent a specific place in the inventory, " +
								"e.g. the <a href='expressions.html#ExprArmorSlot'>helmet slot</a> for players " +
								"(Please note that slot support is still very limited but will be improved eventually).")
				.usage("")
				.examples("")
				.since("1.0")
				.defaultExpression(new EventValueExpression<>(Inventory.class))
				.parser(new Parser<Inventory>() {
					@Override
					@Nullable
					public Inventory parse(final String s, final ParseContext context) {
						return null;
					}
					
					@Override
					public boolean canParse(final ParseContext context) {
						return false;
					}
					
					@Override
					public String toString(final Inventory i, final int flags) {
						return "inventory of " + Classes.toString(i.getHolder());
					}
					
					@Override
					public String getDebugMessage(final Inventory i) {
						return "inventory of " + Classes.getDebugMessage(i.getHolder());
					}
					
					@Override
					public String toVariableNameString(final Inventory i) {
						return "inventory of " + Classes.toString(i.getHolder(), StringMode.VARIABLE_NAME);
					}
				}).changer(DefaultChangers.inventoryChanger));
		
		Classes.registerClass(new EnumClassInfo<>(InventoryAction.class, "inventoryaction", "inventory actions")
				.user("inventory ?actions?")
				.name("Inventory Action")
				.description("What player just did in inventory event. Note that when in creative game mode, most actions do not work correctly.")
				.examples("")
				.since("2.2-dev16"));

		Classes.registerClass(new EnumClassInfo<>(ClickType.class, "clicktype", "click types")
				.user("click ?types?")
				.name("Click Type")
				.description("Click type, mostly for inventory events. Tells exactly which keys/buttons player pressed, " +
						"assuming that default keybindings are used in client side.")
				.examples("")
				.since("2.2-dev16b, 2.2-dev35 (renamed to click type)"));
		
		Classes.registerClass(new EnumClassInfo<>(InventoryType.class, "inventorytype", "inventory types")
				.user("inventory ?types?")
				.name("Inventory Type")
				.description("Minecraft has several different inventory types with their own use cases.")
				.examples("")
				.since("2.2-dev32"));

		Classes.registerClass(new ClassInfo<>(Player.class, "player")
				.user("players?")
				.name("Player")
				.description("A player. Depending on whether a player is online or offline several actions can be performed with them, " +
								"though you won't get any errors when using effects that only work if the player is online (e.g. changing their inventory) on an offline player.",
						"You have two possibilities to use players as command arguments: &lt;player&gt; and &lt;offline player&gt;. " +
								"The first requires that the player is online and also accepts only part of the name, " +
								"while the latter doesn't require that the player is online, but the player's name has to be entered exactly.")
				.usage("")
				.examples("")
				.since("1.0")
				.defaultExpression(new EventValueExpression<>(Player.class))
				.after("string", "world")
				.parser(new Parser<Player>() {
					@Override
					@Nullable
					public Player parse(String s, ParseContext context) {
						if (context == ParseContext.COMMAND) {
							if (s.isEmpty())
								return null;
							if (UUID_PATTERN.matcher(s).matches())
								return Bukkit.getPlayer(UUID.fromString(s));
							List<Player> ps = Bukkit.matchPlayer(s);
							if (ps.size() == 1)
								return ps.get(0);
							if (ps.size() == 0)
								Skript.error(String.format(Language.get("commands.no player starts with"), s));
							else
								Skript.error(String.format(Language.get("commands.multiple players start with"), s));
							return null;
						}
						assert false;
						return null;
					}
					
					@Override
					public boolean canParse(final ParseContext context) {
						return context == ParseContext.COMMAND;
					}
					
					@Override
					public String toString(final Player p, final int flags) {
						return "" + p.getName();
					}
					
					@Override
					public String toVariableNameString(final Player p) {
						if (SkriptConfig.usePlayerUUIDsInVariableNames.value())
							return "" + p.getUniqueId();
						else
							return "" + p.getName();
					}

					@Override
					public String getDebugMessage(final Player p) {
						return p.getName() + " " + Classes.getDebugMessage(p.getLocation());
					}
				})
				.changer(DefaultChangers.playerChanger)
				.serializeAs(OfflinePlayer.class));

		Classes.registerClass(new ClassInfo<>(OfflinePlayer.class, "offlineplayer")
				.user("offline ?players?")
				.name("Offline Player")
				.description("A player that is possibly offline. See <a href='#player'>player</a> for more information. " +
						"Please note that while all effects and conditions that require a player can be used with an " +
						"offline player as well, they will not work if the player is not actually online.")
				.usage("")
				.examples("")
				.since("")
				.defaultExpression(new EventValueExpression<>(OfflinePlayer.class))
				.after("string", "world")
				.parser(new Parser<OfflinePlayer>() {
					@Override
					@Nullable
					@SuppressWarnings("deprecation")
					public OfflinePlayer parse(final String s, final ParseContext context) {
						if (context == ParseContext.COMMAND) {
							if (UUID_PATTERN.matcher(s).matches())
								return Bukkit.getOfflinePlayer(UUID.fromString(s));
							else if (!SkriptConfig.playerNameRegexPattern.value().matcher(s).matches())
								return null;
							return Bukkit.getOfflinePlayer(s);
						}
						assert false;
						return null;
					}
					
					@Override
					public boolean canParse(ParseContext context) {
						return context == ParseContext.COMMAND;
					}
					
					@Override
					public String toString(OfflinePlayer p, int flags) {
						return p.getName() == null ? p.getUniqueId().toString() : p.getName();
					}
					
					@Override
					public String toVariableNameString(OfflinePlayer p) {
						if (SkriptConfig.usePlayerUUIDsInVariableNames.value() || p.getName() == null)
							return "" + p.getUniqueId();
						else
							return "" + p.getName();
					}

					@Override
					public String getDebugMessage(OfflinePlayer p) {
						if (p.isOnline())
							return Classes.getDebugMessage(p.getPlayer());
						return toString(p, 0);
					}
				}).serializer(new Serializer<OfflinePlayer>() {
					@Override
					public Fields serialize(final OfflinePlayer p) {
						final Fields f = new Fields();
						f.putObject("uuid", p.getUniqueId());
						return f;
					}
					
					@Override
					public void deserialize(final OfflinePlayer o, final Fields f) {
						assert false;
					}
					
					@Override
					public boolean canBeInstantiated() {
						return false;
					}
					
					@SuppressWarnings("deprecation")
					@Override
					protected OfflinePlayer deserialize(final Fields fields) throws StreamCorruptedException {
						if (fields.contains("uuid")) {
							final UUID uuid = fields.getObject("uuid", UUID.class);
							if (uuid == null)
								throw new StreamCorruptedException();
							return Bukkit.getOfflinePlayer(uuid);
						} else {
							final String name = fields.getObject("name", String.class);
							if (name == null)
								throw new StreamCorruptedException();
							return Bukkit.getOfflinePlayer(name);
						}
					}
					
					// return p.getName();
					@SuppressWarnings("deprecation")
					@Override
					@Nullable
					public OfflinePlayer deserialize(final String s) {
						return Bukkit.getOfflinePlayer(s);
					}
					
					@Override
					public boolean mustSyncDeserialization() {
						return true;
					}
				}));
		
		Classes.registerClass(new ClassInfo<>(CommandSender.class, "commandsender")
				.user("((commands?)? ?)?(sender|executor)s?")
				.name("Command Sender")
				.description("A player or the console.")
				.usage("use <a href='expressions.html#LitConsole'>the console</a> for the console",
						"see <a href='#player'>player</a> for players.")
				.examples("command /push [&lt;player&gt;]:",
						"\ttrigger:",
						"\t\tif arg-1 is not set:",
						"\t\t\tif command sender is console:",
						"\t\t\t\tsend \"You can't push yourself as a console :\\\" to sender",
						"\t\t\t\tstop",
						"\t\t\tpush sender upwards with force 2",
						"\t\t\tsend \"Yay!\"",
						"\t\telse:",
						"\t\t\tpush arg-1 upwards with force 2",
						"\t\t\tsend \"Yay!\" to sender and arg-1")
				.since("1.0")
				.defaultExpression(new EventValueExpression<>(CommandSender.class))
				.parser(new Parser<CommandSender>() {
					@Override
					@Nullable
					public CommandSender parse(final String s, final ParseContext context) {
						return null;
					}
					
					@Override
					public boolean canParse(final ParseContext context) {
						return false;
					}
					
					@Override
					public String toString(final CommandSender s, final int flags) {
						return "" + s.getName();
					}
					
					@Override
					public String toVariableNameString(final CommandSender s) {
						return "" + s.getName();
					}
				}));
		
		Classes.registerClass(new ClassInfo<>(InventoryHolder.class, "inventoryholder")
				.name(ClassInfo.NO_DOC)
				.defaultExpression(new EventValueExpression<>(InventoryHolder.class))
				.after("entity", "block")
				.parser(new Parser<InventoryHolder>() {
					@Override
					public boolean canParse(ParseContext context) {
						return false;
					}
					
					@Override
					public String toString(InventoryHolder holder, int flags) {
						if (holder instanceof BlockState) {
							return Classes.toString(((BlockState) holder).getBlock());
						} else if (holder instanceof DoubleChest) {
							return Classes.toString(holder.getInventory().getLocation().getBlock());
						} else {
							return Classes.toString(holder);
						}
					}
					
					@Override
					public String toVariableNameString(InventoryHolder holder) {
						return toString(holder, 0);
					}
				}));
		Classes.registerClass(new EnumClassInfo<>(GameMode.class, "gamemode", "game modes", new SimpleLiteral<>(GameMode.SURVIVAL, true))
				.user("game ?modes?")
				.name("Game Mode")
				.description("The game modes survival, creative, adventure and spectator.")
				.examples("player's gamemode is survival",
						"set the player argument's game mode to creative")
				.since("1.0"));
		
		Classes.registerClass(new ClassInfo<>(ItemStack.class, "itemstack")
				.user("items?")
				.name("Item")
				.description("An item, e.g. a stack of torches, a furnace, or a wooden sword of sharpness 2. " +
								"Unlike <a href='#itemtype'>item type</a> an item can only represent exactly one item (e.g. an upside-down cobblestone stair facing west), " +
								"while an item type can represent a whole range of items (e.g. any cobble stone stairs regardless of direction).",
						"You don't usually need this type except when you want to make a command that only accepts an exact item.",
						"Please note that currently 'material' is exactly the same as 'item', i.e. can have an amount & enchantments.")
				.usage("<code>[&lt;number&gt; [of]] &lt;alias&gt; [of &lt;enchantment&gt; &lt;level&gt;]</code>, Where &lt;alias&gt; must be an alias that represents exactly one item " +
						"(i.e cannot be a general alias like 'sword' or 'plant')")
				.examples("set {_item} to type of the targeted block",
						"{_item} is a torch")
				.since("1.0")
				.after("number")
				.supplier(() -> Arrays.stream(Material.values())
					.map(ItemStack::new)
					.iterator())
				.parser(new Parser<ItemStack>() {
					@Override
					@Nullable
					public ItemStack parse(final String s, final ParseContext context) {
						ItemType t = Aliases.parseItemType(s);
						if (t == null)
							return null;
						t = t.getItem();
						if (t.numTypes() != 1) {
							Skript.error("'" + s + "' represents multiple materials");
							return null;
						}
						
						final ItemStack i = t.getRandom();
						assert i != null;
						return i;
					}
					
					@Override
					public String toString(final ItemStack i, final int flags) {
						return ItemType.toString(i, flags);
					}
					
					@Override
					public String toVariableNameString(final ItemStack i) {
						final StringBuilder b = new StringBuilder("item:");
						b.append(i.getType().name());
						b.append(":" + ItemUtils.getDamage(i));
						b.append("*" + i.getAmount());
						
						for (Entry<Enchantment, Integer> entry : i.getEnchantments().entrySet())
							b.append("#" + EnchantmentUtils.getKey(entry.getKey()))
									.append(":" + entry.getValue());

						return b.toString();
					}
				})
				.cloner(ItemStack::clone)
				.serializer(new ConfigurationSerializer<>()));
		
		Classes.registerClass(new ClassInfo<>(Item.class, "itementity")
				.name(ClassInfo.NO_DOC)
				.since("2.0")
				.changer(DefaultChangers.itemChanger));
		
		Classes.registerClass(new EnumClassInfo<>(Biome.class, "biome", "biomes")
				.user("biomes?")
				.name("Biome")
				.description("All possible biomes Minecraft uses to generate a world.")
				.examples("biome at the player is desert")
				.since("1.4.4")
				.after("damagecause"));
		
		Classes.registerClass(new ClassInfo<>(PotionEffect.class, "potioneffect")
			.user("potion ?effects?")
			.name("Potion Effect")
			.description("A potion effect, including the potion effect type, tier and duration.")
			.usage("speed of tier 1 for 10 seconds")
			.since("2.5.2")
			.parser(new Parser<PotionEffect>() {
				
				@Override
				public boolean canParse(ParseContext context) {
					return false;
				}
				
				@Override
				public String toString(PotionEffect potionEffect, int flags) {
					return PotionEffectUtils.toString(potionEffect);
				}
				
				@Override
				public String toVariableNameString(PotionEffect o) {
					return "potion_effect:" + o.getType().getName();
				}

			})
			.serializer(new Serializer<PotionEffect>() {
				@Override
				public Fields serialize(PotionEffect o) {
					Fields fields = new Fields();
					fields.putObject("type", o.getType().getName());
					fields.putPrimitive("amplifier", o.getAmplifier());
					fields.putPrimitive("duration", o.getDuration());
					fields.putPrimitive("particles", o.hasParticles());
					fields.putPrimitive("ambient", o.isAmbient());
					return fields;
				}
				
				@Override
				public void deserialize(PotionEffect o, Fields f) {
					assert false;
				}
				
				@Override
				protected PotionEffect deserialize(Fields fields) throws StreamCorruptedException {
					String typeName = fields.getObject("type", String.class);
					assert typeName != null;
					PotionEffectType type = PotionEffectType.getByName(typeName);
					if (type == null)
						throw new StreamCorruptedException("Invalid PotionEffectType " + typeName);
					int amplifier = fields.getPrimitive("amplifier", int.class);
					int duration = fields.getPrimitive("duration", int.class);
					boolean particles = fields.getPrimitive("particles", boolean.class);
					boolean ambient = fields.getPrimitive("ambient", boolean.class);
					return new PotionEffect(type, duration, amplifier, ambient, particles);
				}
				
				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}
				
				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		
		Classes.registerClass(new ClassInfo<>(PotionEffectType.class, "potioneffecttype")
				.user("potion( ?effect)? ?types?") // "type" had to be made non-optional to prevent clashing with potion effects
				.name("Potion Effect Type")
				.description("A potion effect type, e.g. 'strength' or 'swiftness'.")
				.usage(StringUtils.join(PotionEffectUtils.getNames(), ", "))
				.examples("apply swiftness 5 to the player",
						"apply potion of speed 2 to the player for 60 seconds",
						"remove invisibility from the victim")
				.since("")
				.supplier(PotionEffectType.values())
				.parser(new Parser<PotionEffectType>() {
					@Override
					@Nullable
					public PotionEffectType parse(final String s, final ParseContext context) {
						return PotionEffectUtils.parseType(s);
					}
					
					@Override
					public String toString(final PotionEffectType p, final int flags) {
						return PotionEffectUtils.toString(p, flags);
					}
					
					@Override
					public String toVariableNameString(final PotionEffectType p) {
						return "" + p.getName();
					}
				})
				.serializer(new Serializer<PotionEffectType>() {
					@Override
					public Fields serialize(final PotionEffectType o) {
						final Fields f = new Fields();
						f.putObject("name", o.getName());
						return f;
					}
					
					@Override
					public boolean canBeInstantiated() {
						return false;
					}
					
					@Override
					public void deserialize(final PotionEffectType o, final Fields f) {
						assert false;
					}
					
					@Override
					protected PotionEffectType deserialize(final Fields fields) throws StreamCorruptedException {
						final String name = fields.getObject("name", String.class);
						assert name != null;
						final PotionEffectType t = PotionEffectType.getByName(name);
						if (t == null)
							throw new StreamCorruptedException("Invalid PotionEffectType " + name);
						return t;
					}
					
					// return o.getName();
					@Override
					@Nullable
					public PotionEffectType deserialize(final String s) {
						return PotionEffectType.getByName(s);
					}
					
					@Override
					public boolean mustSyncDeserialization() {
						return false;
					}
				}));
		
		// REMIND make my own damage cause class (that e.g. stores the attacker entity, the projectile, or the attacking block)
		Classes.registerClass(new EnumClassInfo<>(DamageCause.class, "damagecause", "damage causes", new ExprDamageCause())
				.user("damage ?causes?")
				.name("Damage Cause")
				.description("The cause/type of a <a href='events.html#damage'>damage event</a>, e.g. lava, fall, fire, drowning, explosion, poison, etc.",
						"Please note that support for this type is very rudimentary, e.g. lava, fire and burning, " +
								"as well as projectile and attack are considered different types.")
				.examples("")
				.since("2.0")
				.after("itemtype", "itemstack", "entitydata", "entitytype"));
		
		Classes.registerClass(new ClassInfo<>(Chunk.class, "chunk")
				.user("chunks?")
				.name("Chunk")
				.description("A chunk is a cuboid of 16×16×128 (x×z×y) blocks. Chunks are spread on a fixed rectangular grid in their world.")
				.usage("")
				.examples("")
				.since("2.0")
				.parser(new Parser<Chunk>() {
					@Override
					@Nullable
					public Chunk parse(final String s, final ParseContext context) {
						return null;
					}
					
					@Override
					public boolean canParse(final ParseContext context) {
						return false;
					}
					
					@Override
					public String toString(final Chunk c, final int flags) {
						return "chunk (" + c.getX() + "," + c.getZ() + ") of " + c.getWorld().getName();
					}
					
					@Override
					public String toVariableNameString(final Chunk c) {
						return c.getWorld().getName() + ":" + c.getX() + "," + c.getZ();
					}
				})
				.serializer(new Serializer<Chunk>() {
					@Override
					public Fields serialize(final Chunk c) {
						final Fields f = new Fields();
						f.putObject("world", c.getWorld());
						f.putPrimitive("x", c.getX());
						f.putPrimitive("z", c.getZ());
						return f;
					}
					
					@Override
					public void deserialize(final Chunk o, final Fields f) {
						assert false;
					}
					
					@Override
					public boolean canBeInstantiated() {
						return false;
					}
					
					@Override
					protected Chunk deserialize(final Fields fields) throws StreamCorruptedException {
						final World w = fields.getObject("world", World.class);
						final int x = fields.getPrimitive("x", int.class), z = fields.getPrimitive("z", int.class);
						if (w == null)
							throw new StreamCorruptedException();
						return w.getChunkAt(x, z);
					}
					
					// return c.getWorld().getName() + ":" + c.getX() + "," + c.getZ();
					@Override
					@Nullable
					public Chunk deserialize(final String s) {
						final String[] split = s.split("[:,]");
						if (split.length != 3)
							return null;
						final World w = Bukkit.getWorld(split[0]);
						if (w == null)
							return null;
						try {
							final int x = Integer.parseInt(split[1]);
							final int z = Integer.parseInt(split[1]);
							return w.getChunkAt(x, z);
						} catch (final NumberFormatException e) {
							return null;
						}
					}
					
					@Override
					public boolean mustSyncDeserialization() {
						return true;
					}
				}));
		
		Classes.registerClass(new ClassInfo<>(Enchantment.class, "enchantment")
				.user("enchantments?")
				.name("Enchantment")
				.description("An enchantment, e.g. 'sharpness' or 'fortune'. Unlike <a href='#enchantmenttype'>enchantment type</a> " +
						"this type has no level, but you usually don't need to use this type anyway.")
				.usage(StringUtils.join(EnchantmentType.getNames(), ", "))
				.examples("")
				.since("1.4.6")
				.before("enchantmenttype")
				.supplier(Enchantment.values())
				.parser(new Parser<Enchantment>() {
					@Override
					@Nullable
					public Enchantment parse(final String s, final ParseContext context) {
						return EnchantmentType.parseEnchantment(s);
					}
					
					@Override
					public String toString(final Enchantment e, final int flags) {
						return EnchantmentType.toString(e, flags);
					}
					
					@Override
					public String toVariableNameString(final Enchantment e) {
						return "" + EnchantmentUtils.getKey(e);
					}
				})
				.serializer(new Serializer<Enchantment>() {
					@Override
					public Fields serialize(final Enchantment ench) {
						final Fields f = new Fields();
						f.putObject("key", EnchantmentUtils.getKey(ench));
						return f;
					}
					
					@Override
					public boolean canBeInstantiated() {
						return false;
					}
					
					@Override
					public void deserialize(final Enchantment o, final Fields f) {
						assert false;
					}
					
					@Override
					protected Enchantment deserialize(final Fields fields) throws StreamCorruptedException {
						final String key = fields.getObject("key", String.class);
						assert key != null; // If a key happens to be null, something went really wrong...
						final Enchantment e = EnchantmentUtils.getByKey(key);
						if (e == null)
							throw new StreamCorruptedException("Invalid enchantment " + key);
						return e;
					}
					
					// return "" + e.getId();
					@Override
					@Nullable
					public Enchantment deserialize(String s) {
						return Enchantment.getByName(s);
					}
					
					@Override
					public boolean mustSyncDeserialization() {
						return false;
					}
				}));
		
		Material[] allMaterials = Material.values();
		Classes.registerClass(new ClassInfo<>(Material.class, "material")
				.name(ClassInfo.NO_DOC)
				.since("aliases-rework")
				.serializer(new Serializer<Material>() {
					@Override
					public Fields serialize(Material o) {
						Fields f = new Fields();
						f.putObject("i", o.ordinal());
						return f;
					}
					
					@Override
					public void deserialize(Material o, Fields f) {
						assert false;
					}
					
					@Override
					public Material deserialize(Fields f) throws StreamCorruptedException {
						Material mat = allMaterials[(int) f.getPrimitive("i")];
						assert mat != null; // Hope server owner didn't mod too much...
						return mat;
					}
					
					@Override
					public boolean mustSyncDeserialization() {
						return false;
					}
					
					@Override
					protected boolean canBeInstantiated() {
						return false; // It is an enum, come on
					}
				}));
		
		Classes.registerClass(new ClassInfo<>(Metadatable.class, "metadataholder")
				.user("metadata ?holders?")
				.name("Metadata Holder")
				.description("Something that can hold metadata (e.g. an entity or block)")
				.examples("set metadata value \"super cool\" of player to true")
				.since("2.2-dev36"));
		
		Classes.registerClass(new EnumClassInfo<>(TeleportCause.class, "teleportcause", "teleport causes")
				.user("teleport ?(cause|reason|type)s?")
				.name("Teleport Cause")
				.description("The teleport cause in a <a href='events.html#teleport'>teleport</a> event.")
				.since("2.2-dev35"));
		
		Classes.registerClass(new EnumClassInfo<>(SpawnReason.class, "spawnreason", "spawn reasons")
				.user("spawn(ing)? ?reasons?")
				.name("Spawn Reason")
				.description("The spawn reason in a <a href='events.html#spawn'>spawn</a> event.")
				.since("2.3"));
		
		if (Skript.classExists("com.destroystokyo.paper.event.server.PaperServerListPingEvent")) {
			Classes.registerClass(new ClassInfo<>(CachedServerIcon.class, "cachedservericon")
					.user("server ?icons?")
					.name("Server Icon")
					.description("A server icon that was loaded using the <a href='effects.html#EffLoadServerIcon'>load server icon</a> effect.")
					.examples("")
					.since("2.3")
					.parser(new Parser<CachedServerIcon>() {
						@Override
						@Nullable
						public CachedServerIcon parse(final String s, final ParseContext context) {
							return null;
						}
						
						@Override
						public boolean canParse(final ParseContext context) {
							return false;
						}
						
						@Override
						public String toString(final CachedServerIcon o, int flags) {
							return "server icon";
						}
						
						@Override
						public String toVariableNameString(final CachedServerIcon o) {
							return "server icon";
						}
					}));
		}
		
		Classes.registerClass(new EnumClassInfo<>(FireworkEffect.Type.class, "fireworktype", "firework types")
				.user("firework ?types?")
				.name("Firework Type")
				.description("The type of a <a href='#fireworkeffect'>fireworkeffect</a>.")
				.since("2.4")
				.documentationId("FireworkType"));
		
		Classes.registerClass(new ClassInfo<>(FireworkEffect.class, "fireworkeffect")
				.user("firework ?effects?")
				.name("Firework Effect")
				.usage("See <a href='/classes.html#FireworkType'>Firework Types</a>")
				.description(
					"A configuration of effects that defines the firework when exploded",
					"which can be used in the <a href='effects.html#EffFireworkLaunch'>launch firework</a> effect.",
					"See the <a href='expressions.html#ExprFireworkEffect'>firework effect</a> expression for detailed patterns."
				).defaultExpression(new EventValueExpression<>(FireworkEffect.class))
				.examples(
					"launch flickering trailing burst firework colored blue and green at player",
					"launch trailing flickering star colored purple, yellow, blue, green and red fading to pink at target entity",
					"launch ball large colored red, purple and white fading to light green and black at player's location with duration 1"
				).since("2.4")
				.parser(new Parser<FireworkEffect>() {
					@Override
					@Nullable
					public FireworkEffect parse(String input, ParseContext context) {
						return null;
					}
					
					@Override
					public boolean canParse(ParseContext context) {
						return false;
					}
					
					@Override
					public String toString(FireworkEffect effect, int flags) {
						return "Firework effect " + effect.toString();
					}
					
					@Override
					public String toVariableNameString(FireworkEffect effect) {
						return "firework effect " + effect.toString();
					}
				}));
		
		Classes.registerClass(new EnumClassInfo<>(Difficulty.class, "difficulty", "difficulties")
				.user("difficult(y|ies)")
				.name("Difficulty")
				.description("The difficulty of a <a href='#world'>world</a>.")
				.since("2.3"));

		Classes.registerClass(new EnumClassInfo<>(Status.class, "resourcepackstate", "resource pack states")
				.user("resource ?pack ?states?")
				.name("Resource Pack State")
				.description("The state in a <a href='events.html#resource_pack_request_action'>resource pack request response</a> event.")
				.since("2.4"));

		Classes.registerClass(new EnumClassInfo<>(SoundCategory.class, "soundcategory", "sound categories")
				.user("sound ?categor(y|ies)")
				.name("Sound Category")
				.description("The category of a sound, they are used for sound options of Minecraft. " +
						"See the <a href='effects.html#EffPlaySound'>play sound</a> and <a href='effects.html#EffStopSound'>stop sound</a> effects.")
				.since("2.4"));

		if (Skript.classExists("org.bukkit.entity.Panda$Gene")) {
			Classes.registerClass(new EnumClassInfo<>(Gene.class, "gene", "genes")
					.user("(panda )?genes?")
					.name("Gene")
					.description("Represents a Panda's main or hidden gene. " +
							"See <a href='https://minecraft.gamepedia.com/Panda#Genetics'>genetics</a> for more info.")
					.since("2.4")
					.requiredPlugins("Minecraft 1.14 or newer"));
		}
		Classes.registerClass(new EnumClassInfo<>(RegainReason.class, "healreason", "heal reasons")
			.user("(regen|heal) (reason|cause)")
			.name("Heal Reason")
			.description("The heal reason in a heal event.")
			.examples("")
			.since("2.5"));
		if (Skript.classExists("org.bukkit.entity.Cat$Type")) {
			Classes.registerClass(new EnumClassInfo<>(Cat.Type.class, "cattype", "cat types")
					.user("cat ?(type|race)s?")
					.name("Cat Type")
					.description("Represents the race/type of a cat entity.")
					.since("2.4")
					.requiredPlugins("Minecraft 1.14 or newer")
					.documentationId("CatType"));
		}

		Classes.registerClass(new ClassInfo<>(GameRule.class, "gamerule")
			.user("gamerules?")
			.name("Gamerule")
			.description("A gamerule")
			.usage(Arrays.stream(GameRule.values()).map(GameRule::getName).collect(Collectors.joining(", ")))
			.since("2.5")
			.requiredPlugins("Minecraft 1.13 or newer")
			.supplier(GameRule.values())
			.parser(new Parser<GameRule>() {
				@Override
				@Nullable
				public GameRule parse(final String input, final ParseContext context) {
					return GameRule.getByName(input);
				}

				@Override
				public String toString(GameRule o, int flags) {
					return o.getName();
				}

				@Override
				public String toVariableNameString(GameRule o) {
					return o.getName();
				}
			})
		);

		Classes.registerClass(new ClassInfo<>(EnchantmentOffer.class, "enchantmentoffer")
				.user("enchant[ment][ ]offers?")
				.name("Enchantment Offer")
				.description("The enchantmentoffer in an enchant prepare event.")
				.examples("on enchant prepare:",
					"\tset enchant offer 1 to sharpness 1",
					"\tset the cost of enchant offer 1 to 10 levels")
				.since("2.5")
				.parser(new Parser<EnchantmentOffer>() {
					@Override
					public boolean canParse(ParseContext context) {
						return false;
					}
	
					@Override
					public String toString(EnchantmentOffer eo, int flags) {
						return EnchantmentType.toString(eo.getEnchantment(), flags) + " " + eo.getEnchantmentLevel();
					}
	
					@Override
					public String toVariableNameString(EnchantmentOffer eo) {
						return "offer:" + EnchantmentType.toString(eo.getEnchantment()) + "=" + eo.getEnchantmentLevel();
					}
				}));

		Classes.registerClass(new EnumClassInfo<>(Attribute.class, "attributetype", "attribute types")
				.user("attribute ?types?")
				.name("Attribute Type")
				.description("Represents the type of an attribute. Note that this type does not contain any numerical values."
						+ "See <a href='https://minecraft.gamepedia.com/Attribute#Attributes'>attribute types</a> for more info.")
				.since("2.5"));

		Classes.registerClass(new EnumClassInfo<>(Environment.class, "environment", "environments")
				.user("(world ?)?environments?")
				.name("World Environment")
				.description("Represents the environment of a world.")
				.since("2.7"));

		if (Skript.classExists("io.papermc.paper.world.MoonPhase"))
			Classes.registerClass(new EnumClassInfo<>(MoonPhase.class, "moonphase", "moon phases")
					.user("(lunar|moon) ?phases?")
					.name("Moon Phase")
					.description("Represents the phase of a moon.")
					.requiredPlugins("Paper 1.16+")
					.since("2.7"));

		if (Skript.classExists("org.bukkit.event.player.PlayerQuitEvent$QuitReason"))
			Classes.registerClass(new EnumClassInfo<>(QuitReason.class, "quitreason", "quit reasons")
					.user("(quit|disconnect) ?(reason|cause)s?")
					.name("Quit Reason")
					.description("Represents a quit reason from a player quit server event.")
					.requiredPlugins("Paper 1.16.5+")
					.since("INSERT VERSION"));
	}
}
