package mortar.api.fulcrum;

import java.io.IOException;

import mortar.api.fulcrum.object.CustomBlock;
import mortar.api.fulcrum.object.CustomCollective;
import mortar.api.fulcrum.object.CustomInventory;
import mortar.api.fulcrum.object.CustomItem;
import mortar.api.fulcrum.object.CustomLang;
import mortar.api.fulcrum.object.CustomModel;
import mortar.api.fulcrum.object.CustomSound;
import mortar.api.fulcrum.object.CustomTexture;
import mortar.api.fulcrum.object.CustomVorbis;
import mortar.api.fulcrum.registry.FCURegistrar;
import mortar.api.fulcrum.registry.Registered;
import mortar.api.fulcrum.resourcepack.ModelType;
import mortar.api.fulcrum.resourcepack.ResourcePack;
import mortar.api.fulcrum.util.AllocationBlock;
import mortar.api.fulcrum.util.CustomSkin;
import mortar.api.fulcrum.util.DefaultBlockModel;
import mortar.api.fulcrum.util.DefaultSkinModel;
import mortar.bukkit.plugin.Mortar;
import mortar.lang.collection.GMap;
import org.json.JSONException;
import org.json.JSONObject;
import mortar.logic.io.VIO;

public class FulcrumRegistry
{
	private final FCURegistrar<CustomCollective> collectives;
	private final FCURegistrar<CustomInventory> inventories;
	private final FCURegistrar<CustomSkin> skins;
	private final FCURegistrar<CustomItem> items;
	private final FCURegistrar<CustomBlock> blocks;
	private final FCURegistrar<CustomTexture> textures;
	private final FCURegistrar<CustomModel> models;
	private final FCURegistrar<CustomSound> sounds;
	private final FCURegistrar<CustomVorbis> vorbis;
	private final FCURegistrar<CustomLang> lang;
	private final StringBuilder langBuild;
	private final AllocationBlock allocator;
	private final JSONObject soundsJSON;

	public FulcrumRegistry() throws JSONException, IOException
	{
		allocator = new AllocationBlock(Fulcrum.allocationStrategy);
		allocator.addDefaultUnits();
		langBuild = new StringBuilder();
		soundsJSON = new JSONObject(VIO.readAll(getClass().getResourceAsStream("/assets/sounds-default.json")));

		inventories = new FCURegistrar<CustomInventory>()
		{
			@Override
			public void onRegister(CustomInventory r)
			{
				r.registerResources(FulcrumRegistry.this);
			}
		};

		lang = new FCURegistrar<CustomLang>()
		{
			@Override
			public void onRegister(CustomLang r)
			{
				langBuild.append(r.toLine() + "\n");
			}
		};

		items = new FCURegistrar<CustomItem>()
		{
			@Override
			public void onRegister(CustomItem r)
			{
				allocator.allocate(r);
				r.registerResources(FulcrumRegistry.this);
				collective().register(r);
			}
		};

		collectives = new FCURegistrar<CustomCollective>()
		{
			@Override
			public void onRegister(CustomCollective r)
			{

			}
		};

		blocks = new FCURegistrar<CustomBlock>()
		{
			@Override
			public void onRegister(CustomBlock r)
			{
				allocator.allocate(r);
				r.registerResources(FulcrumRegistry.this);
				collective().register(r);
			}
		};

		skins = new FCURegistrar<CustomSkin>()
		{
			@Override
			public void onRegister(CustomSkin r)
			{
				allocator.allocate(r);
				r.registerResources(FulcrumRegistry.this);
				collective().register(r);
			}
		};

		textures = new FCURegistrar<CustomTexture>()
		{
			@Override
			public void onRegister(CustomTexture r)
			{
				getPack().setResource(r.toPackPath(), r.toURL());
			}
		};

		vorbis = new FCURegistrar<CustomVorbis>()
		{
			@Override
			public void onRegister(CustomVorbis r)
			{
				getPack().setResource(r.toPackPath(), r.toURL());
			}
		};

		models = new FCURegistrar<CustomModel>()
		{
			@Override
			public void onRegister(CustomModel r)
			{
				getPack().setResource(r.toPackPath(), r.toURL());
			}
		};

		sounds = new FCURegistrar<CustomSound>()
		{
			@Override
			public void onRegister(CustomSound r)
			{
				r.registerResources(FulcrumRegistry.this);
				r.addToJSON(soundsJSON);
			}
		};
	}

	public void complete()
	{
		allocator.registerAll(this);

		for(DefaultBlockModel i : DefaultBlockModel.values())
		{
			depend(i);
		}

		for(DefaultSkinModel i : DefaultSkinModel.values())
		{
			depend(i);
		}

		getPack().setResource("sounds.json", soundsJSON.toString(Fulcrum.minifyJSON ? 0 : 4));
		getPack().setResource("lang/en_us.lang", langBuild.toString());
	}

	private void depend(DefaultBlockModel model)
	{
		model().register(new CustomModel(model.getName(), f().getResource(model.getFulcrumPath()), ModelType.BLOCK));
	}

	private void depend(DefaultSkinModel model)
	{
		model().register(new CustomModel(model.getName(), f().getResource(model.getFulcrumPath()), ModelType.INVENTORY));
	}

	public GMap<String, FCURegistrar<? extends Registered>> getRegistries()
	{
		//@builder
		return new GMap<String, FCURegistrar<? extends Registered>>()
				.qput("item", item())
				.qput("block", block())
				.qput("skin", skin())
				.qput("inventory", inventory())
				.qput("model", model())
				.qput("texture", texture())
				.qput("sound", sound())
				.qput("vorbis", vorbis())
				.qput("lang", lang());
		//@done
	}

	public FCURegistrar<CustomItem> item()
	{
		return items;
	}

	public FCURegistrar<CustomCollective> collective()
	{
		return collectives;
	}

	public FCURegistrar<CustomBlock> block()
	{
		return blocks;
	}

	public FCURegistrar<CustomSkin> skin()
	{
		return skins;
	}

	public FCURegistrar<CustomInventory> inventory()
	{
		return inventories;
	}

	public FCURegistrar<CustomTexture> texture()
	{
		return textures;
	}

	public FCURegistrar<CustomSound> sound()
	{
		return sounds;
	}

	public FCURegistrar<CustomVorbis> vorbis()
	{
		return vorbis;
	}

	public FCURegistrar<CustomModel> model()
	{
		return models;
	}

	public FCURegistrar<CustomLang> lang()
	{
		return lang;
	}

	private ResourcePack getPack()
	{
		return f().getPack();
	}

	private FulcrumInstance f()
	{
		return FulcrumInstance.instance;
	}

	public FulcrumRegistry begin()
	{
		Mortar.callEvent(new FulcrumRegistryEvent());
		return this;
	}

	public AllocationBlock allocator()
	{
		return allocator;
	}
}
