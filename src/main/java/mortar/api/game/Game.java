package mortar.api.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface Game
{
	public String getName();

	public boolean inGame(World world);

	public boolean inGame(Location location);

	public boolean inGame(Entity entity);

	public boolean inGame(Player player);
}
