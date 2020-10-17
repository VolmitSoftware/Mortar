package mortar.api.blockmeta;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.World;

import mortar.api.sched.J;
import mortar.api.sql.UniversalParser;
import org.json.JSONObject;
import mortar.logic.io.Hasher;
import mortar.logic.io.VIO;
import mortar.util.gross.BlackMagicException;
import mortar.util.gross.ObjectWitchcraft;

public class BlockMetaIO
{
	public static final File getFile(BlockMeta m)
	{
		int cx = m.x >> 4;
		int cz = m.z >> 4;
		int my = m.y >> 5;
		int mx = cx >> 5;
		int mz = cz >> 5;
		World world = Bukkit.getWorld(m.world);

		if(world != null)
		{
			return new File(world.getWorldFolder(), "blockmeta/" + mx + "." + mz + "/" + cx + "." + cz + "/" + my + "/" + m.x + "." + m.y + "." + m.z + ".bkm.json");
		}

		return null;
	}

	public static final void delete(BlockMeta m)
	{
		VIO.deleteUp(getFile(m));
	}

	public static final boolean exists(BlockMeta m)
	{
		return getFile(m).exists();
	}

	public static final boolean load(BlockMeta m)
	{
		File f = getFile(m);

		if(f.exists())
		{
			try
			{
				BlockMeta bc = UniversalParser.fromJSON(new JSONObject(VIO.readAll(f)), m.getClass());
				ObjectWitchcraft.shove(bc, m);
			}

			catch(BlackMagicException e)
			{
				e.printStackTrace();
				return false;
			}

			catch(Throwable e)
			{
				e.printStackTrace();
				VIO.deleteUp(f);
				return false;
			}

			return true;
		}

		return false;
	}

	public static final void save(BlockMeta m)
	{
		J.a(() ->
		{
			File f = getFile(m);
			f.getParentFile().mkdirs();
			try
			{
				VIO.writeAll(f, Hasher.compress(UniversalParser.toJSON(m).toString(0)));
			}

			catch(Throwable e)
			{
				e.printStackTrace();
			}
		});
	}
}
