package mortar.bukkit.plugin;

import mortar.api.rift.Rift;
import mortar.bukkit.command.MortarCommand;
import mortar.bukkit.command.MortarSender;
import mortar.lib.control.RiftController;

public class CommandRiftList extends MortarCommand
{
	public CommandRiftList()
	{
		super("list");
		requiresPermission(MortarAPIPlugin.perm);
	}

	@Override
	public boolean handle(MortarSender sender, String[] args)
	{
		RiftController rc = Mortar.getController(RiftController.class);
		for(Rift i : rc.getRifts())
		{
			sender.sendMessage("- " + i.getName() + " " + (i.isLoaded() ? "[Active]" : "[Hybernating]"));
		}

		sender.sendMessage("There are " + rc.getRifts().size() + " Rifts");

		return true;
	}
}
