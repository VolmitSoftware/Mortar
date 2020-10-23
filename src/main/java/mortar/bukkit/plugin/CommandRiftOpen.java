package mortar.bukkit.plugin;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World.Environment;

import mortar.api.generator.FlatGenerator;
import mortar.api.rift.Rift;
import mortar.api.rift.RiftException;
import mortar.bukkit.command.MortarCommand;
import mortar.bukkit.command.MortarSender;
import mortar.lib.control.RiftController;

public class CommandRiftOpen extends MortarCommand
{
	public CommandRiftOpen()
	{
		super("open");
		requiresPermission(MortarAPIPlugin.perm);
	}

	@Override
	public boolean handle(MortarSender sender, String[] args)
	{
		if(args.length == 0)
		{
			sender.sendMessage("/rift open <NAME>");
			return true;
		}

		if(!sender.isPlayer())
		{
			sender.sendMessage("You cannot see rifts.");
			return true;
		}

		String name = args[0];
		try
		{
			RiftController rc = Mortar.getController(RiftController.class);
			Rift rift = rc.createRift(name);
			rift.setGenerator(FlatGenerator.class);
			rift.setTemporary(true);
			rift.setRandomLightUpdates(false);
			rift.setDifficulty(Difficulty.PEACEFUL);
			rift.setPhysicsThrottle(10);
			rift.setTileTickLimit(0.1);
			rift.setEntityTickLimit(0.1);
			rift.setForcedGameMode(GameMode.CREATIVE);
			rift.setEnvironment(Environment.THE_END);
			rift.setWorldBorderSize(512);
			rift.setWorldBorderWarningDistance(128);
			rift.setWorldBorderEnabled(true);
			rift.load();
			rift.send(sender.player());
		}

		catch(RiftException e)
		{
			sender.sendMessage("Failed. Check the console.");
			e.printStackTrace();
		}

		return true;
	}
}
