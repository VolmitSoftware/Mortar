package mortar.api.fulcrum.util;

import org.bukkit.Material;

import mortar.api.fulcrum.Fulcrum;
import mortar.lang.collection.GList;
import org.json.JSONArray;
import org.json.JSONObject;

public class AllocationUnit
{
	private final Material material;
	private final String model;
	private final String texture;
	private final String parent;
	private final GList<IAllocation> allocations;

	public AllocationUnit(Material material, String model, String texture, String parent)
	{
		this.material = material;
		this.texture = texture;
		this.model = model;
		this.parent = parent;
		allocations = new GList<>();

		if(material.getMaxDurability() < 1)
		{
			throw new RuntimeException("Cannot create allocation unit with items without durability");
		}
	}

	public String getModelNormal()
	{
		return getModel() + "_normal";
	}

	public JSONObject generateNormal()
	{
		JSONObject model = new JSONObject();
		JSONObject textures = new JSONObject();
		textures.put("layer0", getTexture());
		model.put("parent", getParent());
		model.put("textures", textures);
		model.put("##", "required");

		return model;
	}

	public JSONObject generateModel()
	{
		JSONObject model = generateNormal();
		model.put("overrides", generateOverrides());

		return model;
	}

	private JSONObject generatePredicate(String model, double damage, int damaged)
	{
		// https://minecraft.gamepedia.com/index.php?title=Model&oldid=1084824#Item_tags
		JSONObject pred = new JSONObject();
		JSONObject brx = new JSONObject();
		brx.put("damaged", damaged);
		brx.put("damage", damage);
		pred.put("predicate", brx);
		pred.put("model", model);

		return pred;
	}

	private JSONArray generateOverrides()
	{
		JSONArray overrides = new JSONArray();
		short maximum = getMaterial().getMaxDurability();

		for(IAllocation i : getAllocations())
		{
			overrides.put(generatePredicate(i.getAllocatedModel(), (double) i.getAllocationID() / (double) maximum, 0));

			if(Fulcrum.generateModelNormals)
			{
				overrides.put(generatePredicate("item/" + getModelNormal(), (double) i.getAllocationID() / (double) maximum, 1));
			}
		}

		return overrides;
	}

	public GList<IAllocation> getAllocations()
	{
		return allocations.copy();
	}

	public IAllocation getAllocation(short id)
	{
		if(!isAllocated(id))
		{
			return null;
		}

		return allocations.get(id - 1);
	}

	public boolean isAllocated(short id)
	{
		return getAllocated() >= id;
	}

	public synchronized void allocate(IAllocation a)
	{
		if(!hasNextID())
		{
			throw new RuntimeException("Out of allocation capacity!");
		}

		short id = getNextID();
		allocations.add(a);
		a.onAllocated(this, id);
		System.out.println("Allocated " + a.getAllocationID() + " to " + getMaterial() + " id " + id);
	}

	public boolean hasNextID()
	{
		return getCapacity() > getAllocated() + 1; // Dont use most damaged (cap)
	}

	public short getNextID()
	{
		return (short) (getAllocated() + 1);
	}

	public short getCapacity()
	{
		return (short) (material.getMaxDurability() - 2); // Dont use undamaged (0) or most damaged (cap)
	}

	public short getAllocated()
	{
		return (short) allocations.size();
	}

	public short getFreeSpace()
	{
		return (short) (getCapacity() - getAllocated());
	}

	public Material getMaterial()
	{
		return material;
	}

	public String getTexture()
	{
		return texture;
	}

	public String getParent()
	{
		return parent;
	}

	public String getModel()
	{
		return model;
	}
}
