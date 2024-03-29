package mortar.api.config;

import java.io.File;
import java.util.List;

import mortar.lang.collection.GList;
import org.json.JSONArray;
import org.json.JSONObject;
import mortar.logic.io.VIO;
import mortar.util.text.D;

public class WrappedJSONConfiguration implements ConfigWrapper
{
	private JSONObject wrapped;

	public WrappedJSONConfiguration()
	{
		wrapped = new JSONObject();
	}

	@Override
	public void load(File f) throws Exception
	{
		load(VIO.readAll(f));
	}

	@Override
	public void save(File f) throws Exception
	{
		VIO.writeAll(f, wrapped.toString(2));
	}

	@Override
	public String save()
	{
		return wrapped.toString(2);
	}

	@Override
	public void load(String s) throws Exception
	{
		JSONObject j = new JSONObject(s);

		for(String i : j.keySet())
		{
			wrapped.put(i, j.get(i));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void set(String key, Object oo)
	{
		Object o = null;

		if(oo instanceof List)
		{
			o = GList.asStringList((List<?>) oo);
		}

		else
		{
			o = oo;
		}

		if(o instanceof GList)
		{
			try
			{
				o = ((GList<String>) o).toJSONStringArray();
			}

			catch(Throwable e)
			{
				D.as("ID10T Advisor").w("GLists in Object-Configs only support String Lists for json providers.");
			}
		}

		if(!key.contains("."))
		{
			wrapped.put(key, o);
		}

		else
		{
			JSONObject cursor = wrapped;
			GList<String> splitkey = new GList<String>(key.split("\\.")).removeLast();

			for(String i : splitkey)
			{
				if(!cursor.has(i) || !(cursor.get(i) instanceof JSONObject))
				{
					cursor.put(i, new JSONObject());
				}

				cursor = cursor.getJSONObject(i);
			}

			cursor.put(key.split("\\.")[key.split("\\.").length - 1], o);
		}
	}

	@Override
	public Object get(String key)
	{
		if(!key.contains("."))
		{
			Object o = wrapped.get(key);

			if(o instanceof JSONArray)
			{
				o = GList.fromJSONAny((JSONArray) o);
			}

			return o;
		}

		else
		{
			JSONObject cursor = wrapped;
			GList<String> splitkey = new GList<String>(key.split("\\.")).removeLast();

			for(String i : splitkey)
			{
				if(!cursor.has(i) || !(cursor.get(i) instanceof JSONObject))
				{
					return null;
				}

				cursor = cursor.getJSONObject(i);
			}

			Object o = cursor.get(key.split("\\.")[key.split("\\.").length - 1]);

			if(o instanceof JSONArray)
			{
				return GList.fromJSONAny((JSONArray) o);
			}

			return o;
		}
	}

	@Override
	public GList<String> keys()
	{
		return keys("", wrapped);
	}

	public GList<String> keys(String prefix, JSONObject start)
	{
		GList<String> keys = new GList<>();

		for(String i : start.keySet())
		{
			if(start.get(i) instanceof JSONObject)
			{
				keys.addAll(keys(i + ".", start.getJSONObject(i)));
			}

			else
			{
				keys.add(prefix + i);
			}
		}

		return keys;
	}

	@Override
	public boolean contains(String key)
	{
		return keys().contains(key);
	}
}
