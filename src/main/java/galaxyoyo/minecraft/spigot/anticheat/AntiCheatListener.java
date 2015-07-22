package galaxyoyo.minecraft.spigot.anticheat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.v1_8_R3.Blocks;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.packetwrapper.WrapperPlayServerBlockChange;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;

public class AntiCheatListener implements Listener
{
	private final AntiCheat plugin;
	private static final Random random = new Random();
	
	public AntiCheatListener(AntiCheat plugin)
	{
		this.plugin = plugin;
	}
	
	private Map<Player, Double> playersJumping = new HashMap<Player, Double>();
	@EventHandler
	public void onMove(final PlayerMoveEvent event)
	{
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				event.getPlayer().setFireTicks(0);
				if (event.getPlayer().getLocation().getBlock().getType() != Material.FIRE)
					cancel();
			}
		}.runTaskTimer(plugin, 0L, 1L);
		
		updateAntiXRay(event.getTo(), event.getPlayer());
		
		Player player = event.getPlayer();
		if (plugin.isAntiFlyEnabled())
		{
			for (Location[] locs : plugin.getDisabledFlyProtectionZones())
			{
				Location a = locs[0];
				Location b = locs[1];
				Location c = event.getTo();
				
				if (!(a.getX() <= c.getX() && c.getX() <= b.getX() && a.getY() <= c.getY() && c.getY() <= b.getY()
								&& a.getZ() <= c.getZ() && c.getZ() <= b.getZ()))
				{
					return;
				}
			}
			
			if (!playersJumping.containsKey(player))
				playersJumping.put(player, 0.0D);
			
			if ((player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
							&& !player.isSneaking())
			{
				Location a = event.getFrom();
				Location b = event.getTo().clone();
				playersJumping.put(player, playersJumping.get(player) + b.getY() - a.getY());
				while (b.getBlock().getType() == Material.AIR)
					b.add(0.0D, -1.0D, 0.0D);
				if (b.distance(event.getTo()) <= 1.0D)
					playersJumping.put(player, 0.0D);
				if (playersJumping.get(player) >= plugin.getFlyDistance() && event.getTo().distance(b) >= plugin.getFlyDistance())
				{
					player.sendMessage(ChatColor.RED + "Et bien non ! On ne vole pas ! Ou alors prenez du RedBull ...");
					player.damage(event.getTo().distance(b));
					player.teleport(b.add(0.0D, 1.0D, 0.0D));
					playersJumping.put(player, 0.0D);
				}
			}
		}
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event)
	{
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event)
	{
		updateAntiXRay(event.getPlayer().getLocation(), event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		updateAntiXRay(event.getPlayer().getLocation(), event.getPlayer());
	}
	
	@EventHandler
	public void onFlight(PlayerToggleFlightEvent event)
	{
		event.setCancelled(event.isFlying() && event.getPlayer().getGameMode() != GameMode.CREATIVE);
	}

	private Map<Player, List<Location>> toReplace = new HashMap<Player, List<Location>>();
	public void updateAntiXRay(Location loc, final Player player)
	{
		if (plugin.isAntiXRayEnabled())
		{
			if (!toReplace.containsKey(player))
				toReplace.put(player, new ArrayList<Location>());
			
			int distance = plugin.getAntiXRayDistance();
			
			if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
			{
				for (int x = -distance; x <= distance; ++x)
				{
					for (int y = -distance; y <= distance; ++y)
					{
						for (int z = -distance; z <= distance; ++z)
						{
							Location _loc = loc.clone().add(x, y, z);
							replaceStone(_loc, player);
						}
					}
				}
			}
			else if (player.getGameMode() == GameMode.SPECTATOR)
			{
				for (int x = -distance; x <= distance; ++x)
				{
					for (int y = -distance; y <= distance; ++y)
					{
						for (int z = -distance; z <= distance; ++z)
						{
							final Location _loc = loc.clone().add(x, y, z);
							_loc.setX(_loc.getBlockX());
							_loc.setY(_loc.getBlockY());
							_loc.setZ(_loc.getBlockZ());
							_loc.setPitch(0.0F);
							_loc.setYaw(0.0F);
							if (isStone(_loc.getBlock()) && _loc.getBlock().getType() != Material.STONE
											&& !toReplace.get(player).contains(_loc))
							{
								final WrapperPlayServerBlockChange pkt = new WrapperPlayServerBlockChange();
								pkt.setLocation(new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
								WrappedBlockData data = new WrappedBlockData(Blocks.STONE.getBlockData());
								pkt.setBlockData(data);
								new BukkitRunnable()
								{
									@Override
									public void run()
									{
										pkt.sendPacket(player);
										toReplace.get(player).add(_loc);
									}
								}.runTaskAsynchronously(plugin);
							}
						}
					}
				}
			}
			else if (player.getGameMode() == GameMode.CREATIVE)
			{
				for (Location l : toReplace.get(player))
				{
					final WrapperPlayServerBlockChange pkt = new WrapperPlayServerBlockChange();
					pkt.setLocation(new BlockPosition(l.getBlockX(), l.getBlockY(), l.getBlockZ()));
					WrappedBlockData data = new WrappedBlockData(CraftMagicNumbers.getBlock(l.getBlock()).getBlockData());
					pkt.setBlockData(data);
					new BukkitRunnable()
					{
						@Override
						public void run()
						{
							pkt.sendPacket(player);
						}
					}.runTaskAsynchronously(plugin);
				}
			}
		}
	}
	
	public void replaceStone(final Location loc, final Player player)
	{
		loc.setX(loc.getBlockX());
		loc.setY(loc.getBlockY());
		loc.setZ(loc.getBlockZ());
		loc.setPitch(0.0F);
		loc.setYaw(0.0F);
		
		List<BlockFace> faces = new ArrayList<BlockFace>(Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST, BlockFace.SELF));
		
		Block block = loc.getBlock();
		if (!isStone(block))
			return;
		
		boolean ok = true;
		for (BlockFace face : new ArrayList<BlockFace>(faces))
		{
			Block relative = block.getRelative(face);
			if (!isStone(relative))
			{
				ok = false;
				break;
			}
		}
		
		if (player.getLocation().distance(loc) <= 4.0D)
			ok = false;
		
		if (ok && !toReplace.get(player).contains(loc))
		{
			List<net.minecraft.server.v1_8_R3.Block> blocks = Arrays.asList(Blocks.COAL_ORE, Blocks.IRON_ORE,
							Blocks.GOLD_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE);
			final WrapperPlayServerBlockChange pkt = new WrapperPlayServerBlockChange();
			pkt.setLocation(new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
			WrappedBlockData data = new WrappedBlockData(blocks.get(random.nextInt(blocks.size())).getBlockData());
			pkt.setBlockData(data);
			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					pkt.sendPacket(player);
					toReplace.get(player).add(loc);
				}
			}.runTaskAsynchronously(plugin);
		}
		else if (!ok && isStone(block) && toReplace.get(player).contains(loc))
		{
			final WrapperPlayServerBlockChange pkt = new WrapperPlayServerBlockChange();
			pkt.setLocation(new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
			WrappedBlockData data = new WrappedBlockData(CraftMagicNumbers.getBlock(block).getBlockData());
			pkt.setBlockData(data);
			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					pkt.sendPacket(player);
					toReplace.get(player).remove(loc);
				}
			}.runTaskAsynchronously(plugin);
		}
	}
	
	public boolean isStone(Block block)
	{
		Material mat = block.getType();
		return mat == Material.STONE || mat == Material.COBBLESTONE || mat == Material.COAL_ORE
						|| mat == Material.IRON_ORE || mat == Material.GOLD_ORE || mat == Material.DIAMOND_ORE
						|| mat == Material.EMERALD_ORE;
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent event)
	{
		event.setCancelled(true);
	}
}
