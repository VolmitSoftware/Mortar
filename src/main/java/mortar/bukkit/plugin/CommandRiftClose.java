package mortar.bukkit.plugin;

import mortar.api.rift.Rift;
import mortar.bukkit.command.MortarCommand;
import mortar.bukkit.command.MortarSender;
import mortar.lib.control.RiftController;

public class CommandRiftClose extends MortarCommand
{
	public CommandRiftClose()
	{
		super("close");
		requiresPermission(MortarAPIPlugin.perm);
	}

	@Override
	public boolean handle(MortarSender sender, String[] args)
	{
		if(!sender.isPlayer())
		{
			sender.sendMessage("You cannot see rifts.");
			return true;
		}

		if(args.length == 0)
		{
			RiftController rc = Mortar.getController(RiftController.class);
			Rift rift = rc.getRift(sender.player().getWorld());

			if(rift != null)
			{
				rift.unload();
				rc.getRifts().remove(rift);
				rc.deleteRift(rift.getName());
			}

			return true;
		}

		String name = args[0];
		RiftController rc = Mortar.getController(RiftController.class);
		Rift rift = rc.getRift(name);
		rift.unload();
		rc.getRifts().remove(rift);
		rc.deleteRift(rift.getName());
		sender.sendMessage("Colapsed Rift");

		return true;
	}
}
