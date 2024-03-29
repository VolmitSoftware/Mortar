package mortar.bukkit.plugin;

import mortar.api.scm.PermissionSCM;
import mortar.bukkit.command.MortarPermission;
import mortar.bukkit.command.Permission;

public class PermissionMortar extends MortarPermission
{
	@Permission
	public PermissionSCM scm;

	@Override
	protected String getNode()
	{
		return "mortar";
	}

	@Override
	public String getDescription()
	{
		return "Mortar permissions";
	}

	@Override
	public boolean isDefault()
	{
		return false;
	}
}
