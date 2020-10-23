package mortar.bukkit.plugin;

import mortar.bukkit.command.Command;
import mortar.bukkit.command.MortarCommand;
import mortar.bukkit.command.MortarSender;

public class CommandRift extends MortarCommand
{
	@Command
	private CommandRiftOpen open;
	
	@Command
	private CommandRiftClose close;
	
	@Command
	private CommandRiftList list;
	
	@Command
	private CommandRiftVisit visit;

	public CommandRift()
	{
		super("rift");
		requiresPermission(MortarAPIPlugin.perm);
	}

	@Override
	public boolean handle(MortarSender sender, String[] args)
	{
		sender.sendMessage("/rift list - List all active rifts");
		sender.sendMessage("/rift close [name] - Close the rift you are currently in or the name of the rift.");
		sender.sendMessage("/rift open <name> - Open a new blank rift (temporary)");
		sender.sendMessage("/rift visit <name> - The name of the rift");

		return true;
	}
}
