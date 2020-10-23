package mortar.bukkit.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import mortar.api.sched.J;
import mortar.bukkit.command.MortarSender;
import mortar.lang.collection.GSet;
import mortar.logic.io.VIO;
import mortar.util.text.C;
import mortar.util.text.D;
import mortar.util.text.TXT;
import mortar.util.text.VersionCodes;

public class Mortar
{
	public static final int API_VERSION = readAPIVersion();
	public static boolean STARTUP_LOAD = false;

	public static boolean isMainThread()
	{
		return Bukkit.isPrimaryThread();
	}

	public static File getJar(Class<?> src)
	{
		try
		{
			return new File(new File(src.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
		}

		catch(URISyntaxException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public static GSet<Class<?>> getClassesInPackage(String p, Class<?> src)
	{
		JarScannerSpecial s = new JarScannerSpecial(getJar(src), p);

		try
		{
			s.scan();
		}

		catch(IOException e)
		{
			e.printStackTrace();
		}

		return s.getClasses();
	}

	public static String tag(String s)
	{
		return TXT.makeTag(C.BLUE, C.DARK_GRAY, C.GRAY, s);
	}

	@SuppressWarnings("unchecked")
	public static <T extends IController> T getController(Class<? extends T> t, Plugin p)
	{
		return (T) ((MortarPlugin) p).getController(t);
	}

	public static <T extends IController> T getController(Class<? extends T> t)
	{
		return getController(t, MortarAPIPlugin.p);
	}

	public static World getDefaultWorld()
	{
		for(World i : Bukkit.getWorlds())
		{
			if(i.getName().equals("world"))
			{
				return i;
			}
		}

		for(World i : Bukkit.getWorlds())
		{
			if(i.getEnvironment().equals(Environment.NORMAL))
			{
				return i;
			}
		}

		return Bukkit.getWorlds().get(0);
	}

	public static void callEvent(Event e)
	{
		Bukkit.getServer().getPluginManager().callEvent(e);
	}

	private static int readAPIVersion()
	{
		try
		{
			Integer.valueOf(VIO.readAll(Mortar.class.getResourceAsStream("/apiversion.info")).replaceAll("\\Q\n\\E", "").replaceAll("\\Q\"\\E", "").trim());
		}

		catch(Throwable e)
		{
			e.printStackTrace();
		}

		return -1;
	}

	public static void checkForUpdates(MortarSender sender)
	{
		J.a(() ->
		{
			try
			{
				try
				{
					D.as("Mortar Updater").l("Checking for Updates");
					URL url = new URL("https://raw.githubusercontent.com/VolmitSoftware/Mortar/master/version.txt");
					InputStream in = url.openStream();
					BufferedReader bu = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
					String version = bu.readLine().trim().toLowerCase();
					in.close();
					bu.close();
					J.s(() ->
					{
						int grv = VersionCodes.getVersionCode(version);
						int cv = VersionCodes.getVersionCode(MortarAPIPlugin.p.getDescription().getVersion());

						if(grv == cv)
						{
							sender.sendMessage("Mortar is up to date.");
						}

						else if(grv < cv)
						{
							sender.sendMessage("Mortar is ahead of date...");
						}

						else
						{
							sender.sendMessage("There is an update for mortar: " + C.WHITE + version);
							sender.sendMessage("Use /mortar update");
						}
					});
				}

				catch(Throwable e)
				{

				}
			}

			catch(Throwable e)
			{

			}
		});
	}
}
