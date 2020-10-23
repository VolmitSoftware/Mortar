package mortar.bukkit.plugin;

import mortar.api.rift.Rift;
import mortar.bukkit.command.MortarCommand;
import mortar.bukkit.command.MortarSender;
import mortar.lib.control.RiftController;

public class CommandRiftVisit extends MortarCommand
{
	public CommandRiftVisit()
	{
		super("visit");
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
			sender.sendMessage("/rift visit <NAME>");

			return true;
		}

		String name = args[0];
		RiftController rc = Mortar.getController(RiftController.class);
		Rift rift = rc.getRift(name);
		rift.send(sender.player());

		return true;
	}
}
