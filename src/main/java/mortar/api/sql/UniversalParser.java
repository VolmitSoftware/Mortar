package mortar.api.sql;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

public class UniversalParser
{
	private static final List<Class<?>> PRIMATIVES;

	public static byte[] compress(String s) throws IOException
	{
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		GZIPOutputStream gzo = new CustomOutputStream(boas, 1);
		DataOutputStream dos = new DataOutputStream(gzo);
		dos.writeUTF(s);
		dos.close();
		return boas.toByteArray();
	}

	public static String decompress(byte[] s) throws IOException
	{
		ByteArrayInputStream bin = new ByteArrayInputStream(s);
		GZIPInputStream in = new GZIPInputStream(bin);
		DataInputStream din = new DataInputStream(in);
		return din.readUTF();
	}

	public static String nept(String d)
	{
		return d == null ? "" : d;
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromJSON(JSONObject o, Class<T> type) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		Set<Field> fields = new HashSet<Field>();

		for(Field i : type.getSuperclass().getDeclaredFields())
		{
			fields.add(i);
		}

		for(Field i : type.getFields())
		{
			fields.add(i);
		}

		for(Field i : type.getDeclaredFields())
		{
			fields.add(i);
		}
		Object m = null;
		try
		{
			m = type.getConstructor().newInstance();
		}

		catch(Throwable e)
		{
			System.out.println("ERROR CANNOT FIND DEFAULT CONSTRUCTOR FOR CLASS " + type.getCanonicalName());
			e.printStackTrace();
		}

		if(m == null)
		{
			return null;
		}

		for(String i : o.keySet())
		{
			Object v = o.get(i);

			for(Field j : fields)
			{
				j.setAccessible(true);

				if(i.equals(j.getName()))
				{
					if(v.getClass().equals(String.class))
					{
						j.set(m, stringToValue((String) v, j.getType()));
					}

					if(v.getClass().equals(JSONArray.class))
					{
						UniversalType dtype = j.getAnnotation(UniversalType.class);

						if(dtype != null)
						{
							JSONArray arr = (JSONArray) v;

							for(int k = 0; k < arr.length(); k++)
							{
								Object d = arr.get(k);
								Object list = j.get(m);

								if(d.getClass().equals(String.class))
								{
									list.getClass().getMethod("add", Object.class).invoke(list, dtype.value().cast(stringToValue((String) d, dtype.value())));
								}

								if(d.getClass().equals(JSONObject.class))
								{
									list.getClass().getMethod("add", Object.class).invoke(list, dtype.value().cast(fromJSON((JSONObject) d, dtype.value())));
								}
							}
						}
					}

					if(v.getClass().equals(JSONObject.class))
					{
						j.set(m, j.getType().cast(fromJSON((JSONObject) v, j.getType())));
					}
				}
			}
		}

		return (T) m;
	}

	public static JSONObject toJSON(Object o) throws IllegalArgumentException, IllegalAccessException
	{
		if(o.getClass().equals(Object.class) || o.getClass().equals(Class.class))
		{
			throw new RuntimeException("Cannot parse class or object class.");
		}

		JSONObject j = new JSONObject();
		Set<Field> fields = new HashSet<Field>();

		if(!o.getClass().getSuperclass().equals(Object.class) || o.getClass().getSuperclass().equals(Class.class))
		{
			for(Field i : o.getClass().getSuperclass().getDeclaredFields())
			{
				fields.add(i);
			}
		}

		for(Field i : o.getClass().getFields())
		{
			fields.add(i);
		}

		for(Field i : o.getClass().getDeclaredFields())
		{
			fields.add(i);
		}

		for(Field i : fields)
		{
			if(Modifier.isStatic(i.getModifiers()))
			{
				continue;
			}

			if(Modifier.isFinal(i.getModifiers()))
			{
				continue;
			}

			i.setAccessible(true);
			Object value = i.get(o);

			if(value == null)
			{
				continue;
			}

			if(i.getType().equals(List.class))
			{
				UniversalType type = i.getAnnotation(UniversalType.class);

				if(type != null)
				{
					JSONArray a = new JSONArray();

					for(Object k : (List<?>) value)
					{
						a.put(valueToString(k));
					}

					j.put(i.getName(), a);
				}
			}

			else
			{
				j.put(i.getName(), valueToString(value));
			}
		}

		return j;
	}

	private static Object stringToValue(String value, Class<?> type)
	{
		if(type.equals(int.class))
		{
			return Integer.valueOf(value).intValue();
		}

		if(type.equals(long.class))
		{
			return Long.valueOf(value).longValue();
		}

		if(type.equals(double.class))
		{
			return Double.valueOf(value).doubleValue();
		}

		if(type.equals(boolean.class))
		{
			return Boolean.valueOf(value).booleanValue();
		}

		if(type.equals(Integer.class))
		{
			return Integer.valueOf(value);
		}

		if(type.equals(Long.class))
		{
			return Long.valueOf(value);
		}

		if(type.equals(Double.class))
		{
			return Double.valueOf(value);
		}

		if(type.equals(Boolean.class))
		{
			return Boolean.valueOf(value);
		}

		if(type.equals(UUID.class))
		{
			return UUID.fromString(value);
		}

		if(type.equals(String.class))
		{
			return value;
		}

		System.out.println("Warning: Unknown type " + type.getName());

		return null;
	}

	private static Object valueToString(Object value) throws IllegalArgumentException, IllegalAccessException
	{
		if(value == null)
		{
			return "";
		}

		if(PRIMATIVES.contains(value.getClass()))
		{
			return value.toString();
		}

		if(value.getClass().equals(UUID.class))
		{
			return value.toString();
		}

		else
		{
			return toJSON(value);
		}
	}

	static
	{
		PRIMATIVES = new ArrayList<Class<?>>();
		PRIMATIVES.add(int.class);
		PRIMATIVES.add(long.class);
		PRIMATIVES.add(double.class);
		PRIMATIVES.add(boolean.class);
		PRIMATIVES.add(String.class);
		PRIMATIVES.add(Integer.class);
		PRIMATIVES.add(Long.class);
		PRIMATIVES.add(Double.class);
		PRIMATIVES.add(Boolean.class);
	}
}
