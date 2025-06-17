package ch.njol.skript.hooks.regions;

import ch.njol.skript.Skript;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.util.AABB;
import ch.njol.skript.variables.Variables;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilID;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.vector.FastVector;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.*;

public class WorldGuardHook extends RegionsPlugin<WorldGuardPlugin> {
	
	public WorldGuardHook() throws IOException {}
	
	@Override
	protected boolean init() {
		if (!Skript.classExists("com.sk89q.worldedit.math.BlockVector3")) {
			Skript.error("WorldEdit you're using is not compatible with Skript. Disabling WorldGuard support!");
			return false;
		}
		return super.init();
	}
	
	@Override
	public String getName() {
		return "WorldGuard";
	}
	
	@Override
	public boolean canBuild_i(Player p, Location l) {
		if (p.hasPermission("worldguard.region.bypass." + l.getWorld().getName()))
			return true; // Build access always granted by permission
		WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
		RegionQuery query = platform.getRegionContainer().createQuery();
		return query.testBuild(BukkitAdapter.adapt(l), plugin.wrapPlayer(p));
	}
	
	static {
		Variables.yggdrasil.registerSingleClass(WorldGuardRegion.class);
	}
	
	@YggdrasilID("WorldGuardRegion")
	public final class WorldGuardRegion extends Region {
		
		final World world;
		private transient ProtectedRegion region;
		
		@SuppressWarnings({"null", "unused"})
		private WorldGuardRegion() {
			world = null;
		}
		
		public WorldGuardRegion(final World w, final ProtectedRegion r) {
			world = w;
			region = r;
		}
		
		@Override
		public boolean contains(final Location l) {
			return l.getWorld().equals(world) && region.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ());
		}
		
		@Override
		public boolean isMember(final OfflinePlayer p) {
			return region.isMember(plugin.wrapOfflinePlayer(p));
		}
		
		@Override
		public Collection<OfflinePlayer> getMembers() {
			final Collection<UUID> ids = region.getMembers().getUniqueIds();
			final Collection<OfflinePlayer> r = new ArrayList<>(ids.size());
			for (final UUID id : ids)
				r.add(Bukkit.getOfflinePlayer(id));
			return r;
		}
		
		@Override
		public boolean isOwner(final OfflinePlayer p) {
			return region.isOwner(plugin.wrapOfflinePlayer(p));
		}
		
		@Override
		public Collection<OfflinePlayer> getOwners() {
			final Collection<UUID> ids = region.getOwners().getUniqueIds();
			final Collection<OfflinePlayer> r = new ArrayList<>(ids.size());
			for (final UUID id : ids)
				r.add(Bukkit.getOfflinePlayer(id));
			return r;
		}
		
		@Override
		public Iterator<Block> getBlocks() {
			final BlockVector3 min = region.getMinimumPoint(), max = region.getMaximumPoint();
			return new AABB(world, new FastVector(min.getBlockX(), min.getBlockY(), min.getBlockZ()),
					new FastVector(max.getBlockX(), max.getBlockY(), max.getBlockZ())).iterator();
		}
		
		@Override
		public Fields serialize() throws NotSerializableException {
			final Fields f = new Fields(this);
			f.putObject("region", region.getId());
			return f;
		}
		
		@Override
		public void deserialize(final Fields fields) throws StreamCorruptedException, NotSerializableException {
			final String r = fields.getAndRemoveObject("region", String.class);
			fields.setFields(this);
			
			WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
			ProtectedRegion region = platform.getRegionContainer().get(BukkitAdapter.adapt(world)).getRegion(r);
			if (region == null)
				throw new StreamCorruptedException("Invalid region " + r + " in world " + world);
			this.region = region;
		}
		
		@Override
		public String toString() {
			return region.getId() + " in world " + world.getName();
		}
		
		@Override
		public RegionsPlugin<?> getPlugin() {
			return WorldGuardHook.this;
		}
		
		@Override
		public boolean equals(final @Nullable Object o) {
			if (o == this)
				return true;
			if (o == null)
				return false;
			if (!(o instanceof WorldGuardRegion))
				return false;
			return world.equals(((WorldGuardRegion) o).world) && region.equals(((WorldGuardRegion) o).region);
		}
		
		@Override
		public int hashCode() {
			return world.hashCode() * 31 + region.hashCode();
		}
		
	}
	
	@SuppressWarnings("null")
	@Override
	public Collection<? extends Region> getRegionsAt_i(@Nullable final Location l) {
		final ArrayList<Region> r = new ArrayList<>();
		
		if (l == null) // Working around possible cause of issue #280
			return Collections.emptyList();
		if (l.getWorld() == null)
			return Collections.emptyList();
		
		WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
		RegionManager manager = platform.getRegionContainer().get(BukkitAdapter.adapt(l.getWorld()));
		if (manager == null)
			return r;
		ApplicableRegionSet applicable = manager.getApplicableRegions(BukkitAdapter.asBlockVector(l));
		if (applicable == null)
			return r;
		for (ProtectedRegion region : applicable)
			r.add(new WorldGuardRegion(l.getWorld(), region));
		return r;
	}
	
	@Override
	@Nullable
	public Region getRegion_i(final World world, final String name) {
		WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
		ProtectedRegion region = platform.getRegionContainer().get(BukkitAdapter.adapt(world)).getRegion(name);
		if (region != null)
			return new WorldGuardRegion(world, region);
		return null;
	}
	
	@Override
	public boolean hasMultipleOwners_i() {
		return true;
	}
	
	@Override
	protected Class<? extends Region> getRegionClass() {
		return WorldGuardRegion.class;
	}
	
}
