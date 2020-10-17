package mortar.api.fulcrum.resourcepack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;

import com.google.common.io.Files;
import com.googlecode.pngtastic.core.PngImage;
import com.googlecode.pngtastic.core.PngOptimizer;

import mortar.api.fulcrum.FulcrumInstance;
import mortar.api.fulcrum.ResourceCache;
import mortar.api.sched.S;
import mortar.compute.math.M;
import mortar.compute.math.Profiler;
import mortar.lang.collection.GList;
import mortar.lang.collection.GMap;
import org.json.JSONObject;
import mortar.logic.format.F;
import mortar.logic.io.Hasher;
import mortar.logic.io.VIO;
import mortar.util.text.C;
import mortar.util.text.TXT;

public class ResourcePack
{
	private int deduper = 0;
	private long dedupSaved = 0;
	private long totalSaved = 0;
	private final PackMeta meta;
	private GMap<String, URL> copyResources;
	private GMap<String, String> writeResources;
	GList<String> oc = new GList<String>();
	GList<String> ow = new GList<String>();
	private boolean optimizePngs;
	private boolean minifyJSON = false;
	private boolean overbose = false;
	private boolean obfuscate = false;
	private boolean deduplicate = false;
	private ResourceCache rc;
	private GList<File> ignore = new GList<>();
	private GMap<String, String> summary = new GMap<>();
	private long changeJSONMinify;
	private long folder = 0;

	public ResourcePack()
	{
		optimizePngs = false;
		meta = new PackMeta();
		copyResources = new GMap<String, URL>();
		writeResources = new GMap<String, String>();
		oc = new GList<String>();
		ow = new GList<String>();
		rc = FulcrumInstance.instance.getResources();
	}

	public void o(String s)
	{
		if(!overbose)
		{
			return;
		}

		new S()
		{
			@Override
			public void run()
			{
				Bukkit.getConsoleSender().sendMessage(TXT.makeTag(C.RED, C.WHITE, C.GRAY, "FU OVERBOSE") + s);
			}
		};
	}

	public void f(String s)
	{
		new S()
		{
			@Override
			public void run()
			{
				Bukkit.getConsoleSender().sendMessage(TXT.makeTag(C.RED, C.WHITE, C.RED, "FU ERROR") + s);
			}
		};
	}

	public boolean isOptimizedPngs()
	{
		return optimizePngs;
	}

	public void setOptimizePngs(boolean optimize)
	{
		this.optimizePngs = optimize;
	}

	public int size()
	{
		return copyResources.size() + writeResources.size() + 3;
	}

	public void removeResource(String path)
	{
		oc.remove(path);
		copyResources.remove(path);
		writeResources.remove(path);
	}

	public void setResource(String path, URL url)
	{
		if(path == null && url == null)
		{
			return;
		}

		if(path == null)
		{
			f("PATH IS NULL: " + url.toString());
			return;
		}

		if(url == null)
		{
			f("URL IS NULL: " + path);
			return;
		}

		oc.add(path);
		copyResources.put(path, url);

		try
		{
			o("Adding Resource: " + C.WHITE + path + C.GRAY + " from url " + C.WHITE + url.getFile().split("\\Q!\\E")[1]);
		}

		catch(Exception e)
		{
			o("Adding Resource: " + C.WHITE + path + C.GRAY + " from url " + C.WHITE + url.getFile());
		}
	}

	public void setResource(String path, String content)
	{
		ow.add(path);
		writeResources.put(path, content);

		o("Adding Resource: " + C.WHITE + path + C.GRAY + " from TEXT " + C.WHITE + (content.length() > 40 ? content.substring(0, 40) + "..." : content).replaceAll("\n", ""));
	}

	public PackMeta getMeta()
	{
		return meta;
	}

	public byte[] writeToArchive(File f) throws IOException, NoSuchAlgorithmException
	{
		Profiler px = new Profiler();
		px.begin();
		File fx = new File(f.getParentFile(), f.getName() + "-gen");
		if(fx.exists())
		{
			VIO.delete(fx);
		}

		fx.mkdirs();
		writeToFolder(fx);
		f.createNewFile();

		MessageDigest d = MessageDigest.getInstance("MD5");
		FileOutputStream fos = new FileOutputStream(f);
		ZipOutputStream zos = new ZipOutputStream(fos);

		for(File i : fx.listFiles())
		{
			if(ignore.contains(i))
			{
				o("Skipping " + i.getPath() + " since it isnt supposed to be here.");
				continue;
			}

			addToZip(d, i, fx, zos);
		}

		zos.close();
		f.setLastModified(M.ms());
		px.end();

		o("Wrote Pack in " + F.time(px.getMilliseconds(), 2));
		o("------------------------------------------------------");

		long zip = VIO.size(f);
		long zr = folder - zip;
		double savings = (double) zr / (double) folder;

		if(optimizePngs)
		{
			summary.put("PNG Optimizer", (totalSaved > 0 ? (C.GREEN + "-") : (C.RED + "+")) + F.fileSize(Math.abs(totalSaved)) + "b");
		}

		if(deduplicate)
		{
			summary.put("Asset Deduplication", (dedupSaved > 0 ? (C.GREEN + "-") : (C.RED + "+")) + F.fileSize(Math.abs(dedupSaved)) + "b" + C.AQUA + " (" + F.f(deduper) + " assets trimmed)");
		}

		summary.put("JSON Post-Processor", (changeJSONMinify < 0 ? (C.GREEN + "-") : (C.RED + "+")) + F.fileSize(Math.abs(changeJSONMinify)) + "b");
		summary.put("Total Savings", (zr > 0 ? (C.GREEN + "-") : (C.RED + "+")) + F.fileSize(Math.abs(zr)) + "b" + C.AQUA + " (" + F.pc(savings, 1) + ")");

		for(String i : summary.k())
		{
			o(i + ": " + C.WHITE + summary.get(i));
		}

		return d.digest();
	}

	private void addToZip(MessageDigest d, File file, File root, ZipOutputStream s) throws IOException
	{
		if(file.isDirectory())
		{
			for(File i : file.listFiles())
			{
				if(ignore.contains(i))
				{
					o("Skipping " + i.getPath() + " since it isnt supposed to be here.");
					continue;
				}

				addToZip(d, i, root, s);
			}
		}

		else
		{
			String path = file.getAbsolutePath();
			String base = root.getAbsolutePath();
			String relative = new File(base).toURI().relativize(new File(path).toURI()).getPath();
			ZipEntry ze = new ZipEntry(relative);
			FileInputStream fin = new FileInputStream(file);
			o("Zipping " + C.WHITE + file.getPath());
			s.putNextEntry(ze);
			byte[] buf = new byte[1024];
			int read = 0;

			while((read = fin.read(buf)) != -1)
			{
				d.update(buf, 0, read);
				s.write(buf, 0, read);
			}

			s.closeEntry();
			fin.close();
		}
	}

	public void writeToFolder(File f) throws IOException
	{
		getMeta().getProperties().add("uses-zlibcompression");

		if(deduplicate)
		{
			getMeta().getProperties().add("uses-deduplication");
		}

		if(minifyJSON)
		{
			getMeta().getProperties().add("uses-minification");
		}

		if(obfuscate)
		{
			getMeta().getProperties().add("uses-obfuscation");
		}

		if(optimizePngs)
		{
			getMeta().getProperties().add("uses-png-opts");
		}

		f.mkdirs();
		totalSaved = 0;
		writePackContent(new File(f, "pack.mcmeta"), getMeta().toString());
		writeResourceToFile(getMeta().getPackIcon(), new File(f, "pack.png"));
		GList<File> jsonFiles = new GList<>();
		GList<File> allFiles = new GList<>();

		for(String i : oc.copy())
		{
			File destination = new File(f, "assets" + File.separator + "minecraft" + File.separator + i.replaceAll("/", Matcher.quoteReplacement(File.separator)));
			destination.getParentFile().mkdirs();
			writeResourceToFile(copyResources.get(i), destination);

			if(destination.getName().endsWith(".json"))
			{
				jsonFiles.add(destination);
			}

			allFiles.add(destination);
		}

		for(String i : ow.copy())
		{
			File destination = new File(f, "assets" + File.separator + "minecraft" + File.separator + i.replaceAll("/", Matcher.quoteReplacement(File.separator)));
			destination.getParentFile().mkdirs();
			writePackContent(destination, writeResources.get(i));

			if(destination.getName().endsWith(".json"))
			{
				jsonFiles.add(destination);
			}

			allFiles.add(destination);
		}
		folder = VIO.size(f);

		changeJSONMinify = 0;

		for(File i : jsonFiles)
		{
			try
			{
				long size = i.length();
				JSONObject o = new JSONObject(VIO.readAll(i));
				VIO.writeAll(i, o.toString(minifyJSON ? 0 : 4));
				changeJSONMinify += i.length() - size;
			}

			catch(Throwable e)
			{

			}
		}

		if(deduplicate)
		{
			deduplicate(allFiles.copy().qdel(new File(f, "pack.png")), jsonFiles);
		}

		if(obfuscate)
		{
			obfuscate(allFiles.copy().qdel(new File(f, "pack.png")).qdel(new File(f, "pack.mcmeta")).qdel(new File(f, "assets/minecraft/sounds.json")).qdel(new File(f, "assets/minecraft/lang/en_us.lang")), jsonFiles.copy().qdel(new File(f, "assets/minecraft/sounds.json")));
		}

		if(isOptimizedPngs())
		{
			o("Saved a total of " + F.ofSize(totalSaved, 1024, 2));
		}
	}

	private void obfuscate(GList<File> allFiles, GList<File> jsonFiles)
	{

	}

	private void deduplicate(GList<File> allFiles, GList<File> jsonFiles)
	{
		int dedup = 1;
		int dedupd = 1;
		long saved = 0;
		GMap<File, JSONObject> jsonObjects = new GMap<>();
		GMap<File, String> imageHashes = new GMap<>();

		for(File i : allFiles)
		{
			if(i.getName().endsWith(".png"))
			{
				imageHashes.put(i, Hasher.hash(i));
			}
		}

		GMap<String, GList<File>> deduplications = imageHashes.flip();
		GMap<GList<File>, File> reroutes = new GMap<>();

		for(String i : deduplications.k())
		{
			if(deduplications.get(i).size() <= 1)
			{
				deduplications.remove(i);
				dedupd++;
			}
		}

		if(deduplications.isEmpty())
		{
			o("There are no duplicated assets! Nothing to deduplicate.");
			return;
		}

		for(String i : deduplications.k())
		{
			File c = deduplications.get(i).get(1);
			File parent = c.getParentFile();
			File fix = new File(parent, "merger_" + (dedup++) + ".png");

			try
			{
				Files.copy(c, fix);

				for(File j : deduplications.get(i))
				{
					if(j.exists())
					{
						saved += j.length();

						if(!j.delete())
						{
							ignore.add(j);
							f("UNABLE TO DELETE: This file will be ignored when creating the zip. " + j.getPath());
						}
					}
				}

				reroutes.put(deduplications.get(i).copy(), fix);
			}

			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

		for(File i : jsonFiles.copy())
		{
			try
			{
				jsonObjects.put(i, new JSONObject(VIO.readAll(i)));
			}

			catch(Throwable e)
			{
				jsonFiles.remove(i);
			}
		}

		for(File i : jsonObjects.k())
		{
			boolean rewrite = false;
			JSONObject textures = null;
			JSONObject o = jsonObjects.get(i);

			if(o.has("textures"))
			{
				try
				{
					textures = o.getJSONObject("textures");

					for(String j : textures.keySet())
					{
						String m = textures.getString(j);
						if(m.contains("$"))
						{
							continue;
						}

						routing: for(GList<File> k : reroutes.k())
						{
							for(File l : k)
							{
								String map = l.getParentFile().getName() + "/" + l.getName().replaceAll("\\Q.png\\E", "");

								if(m.equals(map))
								{
									textures.put(j, reroutes.get(k).getParentFile().getName() + "/" + reroutes.get(k).getName().replaceAll("\\Q.png\\E", ""));
									rewrite = true;
									break routing;
								}
							}
						}
					}
				}

				catch(Throwable e)
				{

				}
			}

			if(rewrite)
			{
				o.put("textures", textures);

				try
				{
					VIO.writeAll(i, o.toString(minifyJSON ? 0 : 4));
				}

				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		dedupSaved = saved;
		deduper = dedupd;

		o("Deduplicated " + dedupd + " Assets! Saved " + F.fileSize(saved));
	}

	private void writeResourceToFile(URL url, File f) throws IOException
	{
		if(url == null)
		{
			f("WARNING! Resource is null: " + f.getAbsolutePath());
			return;
		}

		try
		{
			try
			{
				o("Writing " + C.WHITE + new File(url.getFile().split("\\Q!\\E")[1]).getName() + C.GRAY + " to " + C.WHITE + f.getName());
			}

			catch(Exception e)
			{
				o("Writing " + C.WHITE + new File(url.getFile()).getName() + C.GRAY + " to " + C.WHITE + f.getName());
			}

			FileOutputStream fos = new FileOutputStream(f);
			InputStream in = url.openStream();
			byte[] buffer = new byte[1024];
			int read = 0;

			while((read = in.read(buffer)) != -1)
			{
				fos.write(buffer, 0, read);
			}

			fos.close();
			in.close();

			if(f.getName().endsWith(".png") && isOptimizedPngs())
			{
				optimizePNG(f);
			}
		}

		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("FAILED TO PACK RESOURCE: " + e.getMessage());
		}
	}

	private void optimizePNG(File f) throws IOException
	{
		String hash = Hasher.hash(f);
		File fk = new File(rc.getBase(), "optimized/" + hash + ".png");
		fk.getParentFile().mkdirs();

		if(fk.exists())
		{
			totalSaved += f.length() - fk.length();
			f.delete();
			Files.copy(fk, f);
			o("Optimized (cached) " + f.getName());
			return;
		}

		PngOptimizer o = new PngOptimizer();
		PngImage img = new PngImage(f.getPath(), "NONE");
		o.setCompressor("zopfli", 32);
		o.optimize(img, f.getPath(), true, 9);
		long sa = o.getTotalSavings();
		o("Optimized " + f.getName() + " (saved " + F.fileSize(sa) + ")");
		totalSaved += sa;
		Files.copy(f, fk);
	}

	private void writePackContent(File m, String content) throws IOException
	{
		o("Writing " + C.WHITE + (content.length() > 40 ? content.substring(0, 40) + "..." : content).replaceAll("\n", "") + C.GRAY + " to " + C.WHITE + m.getPath());
		m.createNewFile();
		PrintWriter pw = new PrintWriter(m);
		pw.println(content);
		pw.close();
	}

	public void setOverbose(boolean hasFlag)
	{
		overbose = hasFlag;
	}

	public void setMinifyJSON(boolean minifyJSON)
	{
		this.minifyJSON = minifyJSON;
	}

	public void setObfuscate(boolean obfuscate)
	{
		this.obfuscate = obfuscate;
	}

	public long getTotalSaved()
	{
		return totalSaved;
	}

	public void setTotalSaved(long totalSaved)
	{
		this.totalSaved = totalSaved;
	}

	public GMap<String, URL> getCopyResources()
	{
		return copyResources;
	}

	public void setCopyResources(GMap<String, URL> copyResources)
	{
		this.copyResources = copyResources;
	}

	public GMap<String, String> getWriteResources()
	{
		return writeResources;
	}

	public void setWriteResources(GMap<String, String> writeResources)
	{
		this.writeResources = writeResources;
	}

	public GList<String> getOc()
	{
		return oc;
	}

	public void setOc(GList<String> oc)
	{
		this.oc = oc;
	}

	public GList<String> getOw()
	{
		return ow;
	}

	public void setOw(GList<String> ow)
	{
		this.ow = ow;
	}

	public ResourceCache getRc()
	{
		return rc;
	}

	public void setRc(ResourceCache rc)
	{
		this.rc = rc;
	}

	public boolean isOptimizePngs()
	{
		return optimizePngs;
	}

	public boolean isMinifyJSON()
	{
		return minifyJSON;
	}

	public boolean isOverbose()
	{
		return overbose;
	}

	public boolean isObfuscate()
	{
		return obfuscate;
	}

	public void setDeduplicate(boolean deduplicate)
	{
		this.deduplicate = deduplicate;
	}
}
