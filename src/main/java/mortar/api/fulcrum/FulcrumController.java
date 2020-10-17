package mortar.api.fulcrum;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import mortar.api.fulcrum.util.EntityNMS12;
import mortar.api.fulcrum.util.EntityNMS12.Type;
import mortar.api.sched.AR;
import mortar.api.sched.J;
import mortar.bukkit.plugin.Controller;
import org.json.JSONException;

public class FulcrumController extends Controller
{
	private boolean active;

	@Override
	public void start()
	{
		File folder = new File("fulcrum");

		if(folder.exists())
		{
			EntityNMS12.registerEntity("block_stand", Type.ARMOR_STAND, BlockStand12.class);

			J.s(() ->
			{
				try
				{
					new FulcrumInstance().reigsterPack();
				}

				catch(JSONException | IOException e)
				{
					e.printStackTrace();
				}
			});

			new AR(20)
			{
				@Override
				public void run()
				{
					if(FulcrumInstance.ready)
					{
						cancel();
						J.s(() -> dump());
					}
				}
			};
		}
	}

	private void dump()
	{
		if(!active || !FulcrumInstance.ready)
		{
			return;
		}

		for(Player i : Bukkit.getOnlinePlayers())
		{
			FulcrumInstance.instance.on(new PlayerJoinEvent(i, "this isnt real!"));
		}

		for(World i : Bukkit.getWorlds())
		{
			for(Chunk j : i.getLoadedChunks())
			{
				FulcrumInstance.instance.on(new ChunkLoadEvent(j, false));
			}
		}
	}

	@Override
	public void stop()
	{
		try
		{
			FulcrumInstance.instance.stop();
		}

		catch(Throwable e)
		{

		}
	}

	@Override
	public void tick()
	{

	}
}