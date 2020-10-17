package mortar.api.fulcrum.resourcepack;

import java.net.URL;

import mortar.bukkit.plugin.Mortar;
import mortar.bukkit.plugin.MortarAPIPlugin;
import mortar.lang.collection.GList;
import org.json.JSONObject;

public class PackMeta
{
	private String packDescription;
	private int packFormat;
	private URL packIcon;
	private GList<String> properties;
	private String revision;
	private String vendorName;
	private String vendorURL;
	private String providerVersion;
	private boolean production;

	public PackMeta(String packDescription, int packFormat, URL packIcon)
	{
		this.packDescription = packDescription;
		this.packFormat = packFormat;
		this.packIcon = packIcon;
		this.properties = new GList<>();
		this.revision = "1.0";
		this.vendorName = "Mortar";
		this.production = false;
		this.vendorURL = "volmit.com";
		this.providerVersion = MortarAPIPlugin.p.getDescription().getVersion();
	}

	public PackMeta(String packDescription, int packFormat)
	{
		this(packDescription, packFormat, Mortar.class.getResource("/assets/textures/blocks/unknown.png"));
	}

	public PackMeta(String packDescription)
	{
		this(packDescription, 3);
	}

	public PackMeta(int packFormat)
	{
		this("No Description", packFormat);
	}

	public PackMeta()
	{
		this(3);
	}

	public String getPackDescription()
	{
		return packDescription;
	}

	public void setPackDescription(String packDescription)
	{
		this.packDescription = packDescription;
	}

	public int getPackFormat()
	{
		return packFormat;
	}

	public void setPackFormat(int packFormat)
	{
		this.packFormat = packFormat;
	}

	public URL getPackIcon()
	{
		return packIcon;
	}

	public void setPackIcon(URL packIcon)
	{
		this.packIcon = packIcon;
	}

	public void setRevision(String revision)
	{
		this.revision = revision;
	}

	public void setVendorName(String vendorName)
	{
		this.vendorName = vendorName;
	}

	public void setVendorURL(String vendorURL)
	{
		this.vendorURL = vendorURL;
	}

	public GList<String> getProperties()
	{
		return properties;
	}

	public String getRevision()
	{
		return revision;
	}

	public String getVendorName()
	{
		return vendorName;
	}

	public String getVendorURL()
	{
		return vendorURL;
	}

	public String getProviderVersion()
	{
		return providerVersion;
	}

	public boolean isProduction()
	{
		return production;
	}

	public void setProduction(boolean production)
	{
		this.production = production;
	}

	@Override
	public String toString()
	{
		JSONObject j = new JSONObject();
		JSONObject pack = new JSONObject();
		pack.put("pack_format", getPackFormat());
		pack.put("description", getPackDescription());
		j.put("pack", pack);
		pack.put("properties", getProperties().toJSONStringArray());
		pack.put("vendor-name", getVendorName());
		pack.put("vendor-url", getVendorURL());
		pack.put("production", isProduction());
		pack.put("provider-version", getProviderVersion());
		pack.put("revision", getRevision());
		pack.put("properties", properties.toJSONStringArray());

		return j.toString();
	}
}
