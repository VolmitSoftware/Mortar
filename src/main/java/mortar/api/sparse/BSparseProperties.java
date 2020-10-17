package mortar.api.sparse;

import java.io.IOException;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import mortar.api.sql.UniversalParser;
import mortar.lang.collection.GList;
import mortar.lang.collection.GMap;
import org.json.JSONException;
import org.json.JSONObject;
import mortar.logic.io.Hasher;

public class BSparseProperties implements SparseProperties
{
	private final GMap<String, JSONObject> data;
	private boolean modified;

	protected BSparseProperties(JSONObject o)
	{
		data = new GMap<>();
		modified = false;

		for(String i : o.keySet())
		{
			try
			{
				setRaw(i, o.getJSONObject(i));
			}

			catch(Throwable e)
			{

			}
		}

		syncronize();
	}

	@Override
	public BSparseProperties remove(String key)
	{
		if(contains(key))
		{
			data.remove(key);
			modified = true;
		}

		return this;
	}

	@Override
	public boolean contains(String key)
	{
		return data.containsKey(key);
	}

	@Override
	public BSparseProperties clear()
	{
		data.clear();
		modified = true;
		return this;
	}

	@Override
	public BSparseProperties setRaw(String key, JSONObject data)
	{
		if(data == null)
		{
			if(contains(key))
			{
				remove(key);
				modified = true;
				return this;
			}
		}

		this.data.put(key, data);
		modified = true;
		return this;
	}

	@Override
	public BSparseProperties set(String key, Object data)
	{
		if(data == null)
		{
			if(contains(key))
			{
				remove(key);
				modified = true;
				return this;
			}
		}

		try
		{
			setRaw(key, UniversalParser.toJSON(data));
			modified = true;
		}

		catch(Throwable e)
		{
			e.printStackTrace();
			throw new RuntimeException("Failed to write data from " + data.getClass().getSimpleName());
		}

		return this;
	}

	@Override
	public JSONObject getRaw(String key)
	{
		if(!contains(key))
		{
			return null;
		}

		return data.get(key);
	}

	@Override
	public <T> T get(String key, Class<? extends T> data)
	{
		JSONObject j = getRaw(key);

		if(j != null)
		{
			try
			{
				return UniversalParser.fromJSON(j, data);
			}

			catch(Throwable e)
			{
				e.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public int size()
	{
		return data.size();
	}

	@Override
	public boolean isEmpty()
	{
		return size() == 0;
	}

	@Override
	public boolean isModified()
	{
		return modified;
	}

	@Override
	public GList<String> getKeys()
	{
		return data.k();
	}

	@Override
	public GList<JSONObject> getValues()
	{
		return data.v();
	}

	@Override
	public BSparseProperties syncronize()
	{
		modified = false;
		return this;
	}

	@Override
	public JSONObject toJSON()
	{
		JSONObject j = new JSONObject();

		for(String i : data.k())
		{
			j.put(i, getRaw(i));
		}

		return j;
	}

	@Override
	public String toCJSON()
	{
		try
		{
			return Hasher.compress(toJSON().toString(0));
		}

		catch(JSONException | IOException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public SparseProperties toItem(ItemStack is)
	{
		ItemMeta im = is.getItemMeta();
		String data = "sparse://" + toCJSON();
		GList<String> lore = new GList<String>();
		if(im.hasLore())
		{
			lore.addAll(im.getLore());
		}

		if(!lore.isEmpty() && lore.get(0).startsWith("sparse://"))
		{
			lore.remove(0);
		}

		lore.add(0, data);
		im.setLore(lore);
		is.setItemMeta(im);
		return this;
	}
}
