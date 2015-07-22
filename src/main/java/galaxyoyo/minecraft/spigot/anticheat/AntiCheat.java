package galaxyoyo.minecraft.spigot.anticheat;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiCheat extends JavaPlugin
{
	private boolean antiXRay;
	private int antiXRayDistance;
	private boolean antiFly;
	private double flyDistance;
	private List<Location[]> disabledLocations = new ArrayList<Location[]>();
	
	@Override
	public void onEnable()
	{
		YamlConfiguration config = (YamlConfiguration) getConfig();
		antiXRay = config.getBoolean("Anti X-Ray", true);
		antiXRayDistance = config.getInt("Distance Anti X-Ray", 16);
		antiFly = config.getBoolean("Anti Fly", true);
		flyDistance = config.getDouble("Hauteur de fly", 1.5D);
		String[] disabledZones = config.getStringList("disabled-fly-protection").toArray(new String[0]);
		for (String disabledZone : disabledZones)
		{
			String[] split = disabledZone.split(":");
			if (split.length != 7)
				continue;
			World world = Bukkit.getWorld(split[0]);
			int x1, y1, z1, x2, y2, z2;
			try
			{
				x1 = Double.valueOf(split[1]).intValue();
				y1 = Double.valueOf(split[2]).intValue();
				z1 = Double.valueOf(split[3]).intValue();
				x2 = Double.valueOf(split[4]).intValue();
				y2 = Double.valueOf(split[5]).intValue();
				z2 = Double.valueOf(split[6]).intValue();
			}
			catch (NumberFormatException ex)
			{
				continue;
			}
			Location a = new Location(world, x1, y1, z1);
			Location b = new Location(world, x2, y2, z2);
			Location[] locs = {a, b};
			disabledLocations.add(locs);
		}
		config.set("Anti X-Ray", antiXRay);
		config.set("Distance Anti X-Ray", antiXRayDistance);
		config.set("Ant-Fly", antiFly);
		config.set("Hauteur de fly", flyDistance);
		List<String> disabledZonesString = new ArrayList<String>();
		for (Location[] locs : disabledLocations)
		{
			String str = locs[0].getWorld().getName() + ":";
			str += locs[0].getBlockX() + ":";
			str += locs[0].getBlockY() + ":";
			str += locs[0].getBlockZ() + ":";
			str += locs[1].getBlockX() + ":";
			str += locs[1].getBlockY() + ":";
			str += locs[1].getBlockZ();
			disabledZonesString.add(str);
		}
		config.set("disabled-fly-protection", disabledZonesString);
		saveConfig();
		
		getServer().getPluginManager().registerEvents(new AntiCheatListener(this), this);
	}
	
	@Override
	public void onDisable()
	{
	}
	
	public boolean isAntiXRayEnabled()
	{
		return antiXRay;
	}
	
	public int getAntiXRayDistance()
	{
		return antiXRayDistance;
	}
	
	public boolean isAntiFlyEnabled()
	{
		return antiFly;
	}
	
	public double getFlyDistance()
	{
		return flyDistance;
	}
	
	public List<Location[]> getDisabledFlyProtectionZones()
	{
		return disabledLocations;
	}
}
