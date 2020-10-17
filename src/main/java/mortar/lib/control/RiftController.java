package mortar.lib.control;

import java.io.File;
import java.io.IOException;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.World.Environment;

import mortar.api.generator.FlatGenerator;
import mortar.api.rift.PhantomRift;
import mortar.api.rift.Rift;
import mortar.api.rift.RiftException;
import mortar.bukkit.plugin.Controller;
import mortar.lang.collection.GList;
import org.json.JSONException;
import org.json.JSONObject;
import mortar.logic.io.VIO;
import mortar.util.text.D;

public class RiftController extends Controller
{
	private static final GList<Rift> rifts = new GList<>();
	private static final GList<String> usedNames = new GList<>();

	@Override
	public void start()
	{
		usedNames.clear();
		rifts.clear();
		searchForRifts(new File("rifts"));
	}

	private void searchForRifts(File folder)
	{
		try
		{
			File scfg = new File(folder, "rift.json");

			if(scfg.exists())
			{
				try
				{
					JSONObject j = new JSONObject(VIO.readAll(scfg));

					if(j.has("temporary") && j.getBoolean("temporary"))
					{
						D.as("Rift Service").w("Found temporary rift " + folder.getName() + " deleting.");
						VIO.delete(folder);

						if(folder.exists())
						{
							VIO.deleteOnExit(folder);
							usedNames.add(j.getString("name"));
						}
					}

					else
					{
						try
						{
							Rift r = new PhantomRift(j.getString("name"));
							rifts.add(r);
							D.as("Rift Service").l("Identified Rift " + folder.getName() + ".");
						}

						catch(Throwable e)
						{
							e.printStackTrace();
						}
					}
				}

				catch(JSONException | IOException e)
				{
					D.as("Rift Service").f("Failed to identify rift " + folder.getName() + " deleting.");
					VIO.delete(folder);

					if(folder.exists())
					{
						VIO.deleteOnExit(folder);
					}

					e.printStackTrace();
				}
			}

			else
			{
				for(File i : folder.listFiles())
				{
					if(i.isDirectory())
					{
						searchForRifts(i);
					}
				}
			}
		}

		catch(Throwable e)
		{

		}
	}

	public void deleteRift(String name)
	{
		if(hasRift(riftName(name)))
		{
			Rift r = getRift(riftName(name));

			if(r.isLoaded())
			{
				return;
			}

			rifts.remove(r);
			VIO.delete(r.getWorldFolder());

			if(r.getWorldFolder().exists())
			{
				D.as("Rift Service").w("Deleting world " + riftName(name) + " was partially successful.");
				D.as("Rift Service").w("The world was successfully unloaded but did not delete all of its files.");
				D.as("Rift Service").w("You cannot create a rift with this name until you reboot this server.");
				VIO.deleteOnExit(r.getWorldFolder());
				usedNames.add(riftName(name));
			}
		}
	}

	public boolean hasRift(String name)
	{
		return getRift(riftName(name)) != null;
	}

	public boolean hasRiftFile(String name)
	{
		return new File(riftName(name)).exists();
	}

	public Rift getOrCreate(String name) throws RiftException
	{
		if(hasRift(riftName(name)))
		{
			return getRift(riftName(name));
		}

		return createRift(riftName(name));
	}

	public Rift getRift(String name)
	{
		for(Rift i : rifts)
		{
			if(i.getName().equals(riftName(name)))
			{
				return i;
			}
		}

		return null;
	}

	public boolean isInRift(World world)
	{
		return getRift(world) != null;
	}

	public Rift getRift(World world)
	{
		try
		{
			for(Rift i : rifts)
			{
				if(i.isLoaded())
				{
					if(i.getWorld().equals(world))
					{
						return i;
					}
				}
			}
		}

		catch(Throwable e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public Rift createRift(String v) throws RiftException
	{
		String name = riftName(v);

		if(usedNames.contains(name))
		{
			throw new RiftException("Cannot create rift. A rift with the name " + name + " is already loaded, or already exists, or has been deleted during this server's runtime.");
		}

		if(new File(name).exists())
		{
			throw new RiftException("Cannot create rift. A rift with the name " + name + " is already loaded, or already exists, or has been deleted during this server's runtime.");
		}

		Rift rift = new PhantomRift(name);
		rifts.add(rift);

		return rift;
	}

	public Rift openTemporaryRift(String prefix)
	{
		int m = 1;
		String name = "temp/" + prefix + m;

		while(hasRiftFile(name))
		{
			m++;
			name = "temp/" + prefix + m;
		}

		try
		{
			//@builder
			return createRift(name)
					.setTemporary(true)
					.setDifficulty(Difficulty.PEACEFUL)
					.setForcedGameMode(GameMode.CREATIVE)
					.setEnvironment(Environment.THE_END)
					.setGenerator(FlatGenerator.class)
					.setMaxTNTUpdatesPerTick(1)
					.setAllowBosses(false)
					.setEntityTickLimit(0.001)
					.setTileTickLimit(0.001)
					.setPhysicsThrottle(100)
					.setHopperCheckRate(200)
					.setHopperTransferAmount(200)
					.setHopperTransferAmount(64)
					.setRandomLightUpdates(false)
					.setUnloadWhenEmpty(15)
					.setRule("doMobSpawning", "false")
					.setRule("doEntityDrops", "false")
					.setRule("doFireTick", "false")
					.setRule("doMobLoot", "false")
					.setRule("doTileDrops", "false")
					.setRule("doWeatherCycle", "false")
					.setRule("doDaylightCycle", "false")
					.setWorldBorderEnabled(true)
					.setWorldBorderCenter(0, 0)
					.setWorldBorderSize(256)
					.load();
			//@done
		}

		catch(RiftException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	private String riftName(String v)
	{
		if(v.startsWith("rifts/"))
		{
			return v;
		}

		return "rifts/" + v;
	}

	@Override
	public void stop()
	{
		for(Rift i : rifts)
		{
			if(i.isLoaded())
			{
				D.as("Rift Service").l("Unloading " + i.getName());
				i.unload();
			}
		}

		D.as("Rift Service").l("Unloaded " + rifts.size() + " Rifts");
	}

	public GList<Rift> getRifts()
	{
		return rifts.copy();
	}

	@Override
	public void tick()
	{

	}
}
