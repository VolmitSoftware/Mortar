package mortar.lib.control;

import java.util.UUID;

import mortar.bukkit.plugin.Controller;
import mortar.lang.collection.CacheMap;
import mortar.lang.collection.GList;
import org.json.JSONArray;
import org.json.JSONObject;
import mortar.logic.io.VIO;

public class MojangProfileController extends Controller
{
	private static final CacheMap<String, UUID> NAME_UUID_CACHE = new CacheMap<String, UUID>(512);
	private static final CacheMap<UUID, String> UUID_NAME_CACHE = new CacheMap<UUID, String>(512);
	private static final CacheMap<UUID, GList<String>> UUID_NAMES_CACHE = new CacheMap<UUID, GList<String>>(512);
	private static final String PROFILE_USERNAME = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String PROFILE_NAMES = "https://api.mojang.com/user/profiles/";

	@Override
	public void start()
	{

	}

	@Override
	public void stop()
	{

	}

	@Override
	public void tick()
	{

	}

	public UUID withoutDashes(String id)
	{
		return UUID.fromString(id.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
	}

	public String getOnlineNameFor(UUID uuid)
	{
		getOnlineNamesFor(uuid);

		return UUID_NAME_CACHE.get(uuid);
	}

	public GList<String> getOnlineNamesFor(UUID uuid)
	{
		if(UUID_NAMES_CACHE.has(uuid))
		{
			return UUID_NAMES_CACHE.get(uuid);
		}

		GList<String> vx = new GList<String>();

		try
		{
			JSONArray j = new JSONArray(VIO.downloadToString(PROFILE_NAMES + uuid.toString().replaceAll("-", "") + "/names").trim());

			for(int i = 0; i < j.length(); i++)
			{
				JSONObject v = j.getJSONObject(i);

				if(!v.has("changedToAt"))
				{
					UUID_NAME_CACHE.put(uuid, v.getString("name"));
				}

				vx.add(v.getString("name"));
			}

			UUID_NAMES_CACHE.put(uuid, vx.copy());
		}

		catch(Throwable e)
		{
			e.printStackTrace();
		}

		return vx;
	}

	public UUID getOnlineUUID(String name)
	{
		if(NAME_UUID_CACHE.has(name))
		{
			return NAME_UUID_CACHE.get(name);
		}

		try
		{
			JSONObject j = new JSONObject(VIO.downloadToString(PROFILE_USERNAME + name));
			UUID s = withoutDashes(j.getString("id"));

			if(s != null)
			{
				NAME_UUID_CACHE.put(name, s);
				UUID_NAME_CACHE.put(s, j.getString("name"));
			}

			return s;
		}

		catch(Throwable e)
		{
			e.printStackTrace();
		}

		return null;
	}
}
