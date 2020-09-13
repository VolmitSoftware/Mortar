package mortar.api.scm;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import mortar.bukkit.compatibility.MaterialEnum;
import mortar.bukkit.compatibility.SoundEnum;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import mortar.api.fx.ParticleRedstone;
import mortar.api.nms.NMSVersion;
import mortar.api.particle.ParticleEffect;
import mortar.api.sched.A;
import mortar.api.sched.S;
import mortar.api.sound.Audio;
import mortar.api.world.P;
import mortar.api.world.W;
import mortar.bukkit.plugin.Controller;
import mortar.bukkit.plugin.Mortar;
import mortar.bukkit.plugin.MortarAPIPlugin;
import mortar.compute.math.M;
import mortar.event.VolumeConstructEvent;
import mortar.lang.collection.GBiset;
import mortar.lang.collection.GList;
import mortar.lang.collection.GMap;
import mortar.util.text.C;

public class SCMController extends Controller
{
	public static final String WAND_NAME = C.YELLOW + "SCM Wand";

	private GMap<String, IVolume> volumes;

	public SCMController()
	{
		setTickRate(0);
	}

	@Override
	public void start()
	{
		if(NMSVersion.current().equals(NMSVersion.v1_8_R3))
		{
			return;
		}

		volumes = new GMap<String, IVolume>();

		File gf = getSCMFolder();

		new S(5)
		{
			@Override
			public void run()
			{
				if(gf.exists())
				{
					for(File i : gf.listFiles())
					{
						try
						{
							IVolume v = new SCMVolume(i);
							volumes.put(i.getName().replace(".scmv", ""), v);
						}

						catch(IOException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		};
	}

	@Override
	public void stop()
	{

	}

	public File getSCMFolder()
	{
		return new File(new File(MortarAPIPlugin.p.getDataFolder(), "scm"), "volumes");
	}

	public File getSCMRawFolder()
	{
		return new File(MortarAPIPlugin.p.getDataFolder(), "scm");
	}

	public File getSCMFile(String name)
	{
		return new File(getSCMFolder(), name + ".scmv");
	}

	public File getSCMRawFile(String name)
	{
		return new File(getSCMRawFolder(), name + ".scmr");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on(BlockPlaceEvent e)
	{
		new A()
		{
			@Override
			public void run()
			{
				GBiset<String, IMappedVolume> s = doMatch(e.getBlock().getLocation());

				if(s != null)
				{
					new S()
					{
						@Override
						public void run()
						{
							Mortar.callEvent(new VolumeConstructEvent(e, volumes.get(s.getA()), s.getB(), s.getA()));
						}
					};
				}
			}
		};
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on(BlockBreakEvent e)
	{
		new A()
		{
			@Override
			public void run()
			{
				for(Block i : W.blockFaces(e.getBlock()))
				{
					GBiset<String, IMappedVolume> s = doMatch(i.getLocation());

					if(s != null)
					{
						new S()
						{
							@Override
							public void run()
							{
								Mortar.callEvent(new VolumeConstructEvent(e, volumes.get(s.getA()), s.getB(), s.getA()));
							}
						};

						return;
					}
				}

				GBiset<String, IMappedVolume> s = doMatch(e.getBlock().getLocation());

				if(s != null)
				{
					new S()
					{
						@Override
						public void run()
						{
							Mortar.callEvent(new VolumeConstructEvent(e, volumes.get(s.getA()), s.getB(), s.getA()));
						}
					};
				}
			}
		};
	}

	public GBiset<String, IMappedVolume> doMatch(Location at)
	{
		if (volumes == null || volumes.size() == 0) return null;
		GBiset<String, IMappedVolume> s = null;

		for(String i : volumes.k())
		{
			IVolume v = volumes.get(i);
			IMappedVolume m = v.match(at);

			if(m != null)
			{
				s = new GBiset<String, IMappedVolume>(i, m);
				break;
			}
		}

		return s;
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void on(PlayerInteractEvent e)
	{
		final ItemStack i = e.getItem();
		
		if(i == null)
		{
			return;
		}

		if(i.getType().equals(MaterialEnum.IRON_AXE.bukkitMaterial()) && i.hasItemMeta() && i.getItemMeta().getDisplayName().equalsIgnoreCase(WAND_NAME))
		{
			e.setCancelled(true);

			if(e.getAction().equals(Action.LEFT_CLICK_BLOCK))
			{
				new Audio().s(SoundEnum.ENTITY_ENDEREYE_DEATH.bukkitSound()).vp(1f, 1.5f).play(e.getPlayer());
				ParticleEffect.ENCHANTMENT_TABLE.display(2.15f, 40, e.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), 32);
				ParticleEffect.SWEEP_ATTACK.display(2.15f, 1, P.getHand(e.getPlayer()), 32);

				ItemStack is = i.clone();
				ItemMeta im = is.getItemMeta();
				im.setDisplayName(WAND_NAME);
				Location ll = e.getClickedBlock().getLocation();
				GList<String> lx = new GList<>(im.hasLore() ? im.getLore() : new GList<>());
				if (lx.size() == 0) lx.add("");
				lx.set(0, C.AQUA + "A: " + ll.getWorld().getName() + "@" + ll.getBlockX() + "." + ll.getBlockY() + "." + ll.getBlockZ());
				im.setLore(lx);
				is.setItemMeta(im);
				e.getPlayer().getInventory().setItemInHand(is);
			}

			else if(e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
			{
				new Audio().s(SoundEnum.ENTITY_ENDEREYE_DEATH.bukkitSound()).vp(1f, 1.2f).play(e.getPlayer());
				ParticleEffect.ENCHANTMENT_TABLE.display(2.15f, 40, e.getClickedBlock().getLocation().clone().add(0.5, 0.5, 0.5), 32);
				ParticleEffect.SWEEP_ATTACK.display(2.15f, 1, P.getHand(e.getPlayer()), 32);
				ItemStack is = i.clone();
				ItemMeta im = is.getItemMeta();
				im.setDisplayName(WAND_NAME);
				Location ll = e.getClickedBlock().getLocation();
				GList<String> lx = new GList<String>(im.hasLore() ? im.getLore() : new GList<>());
				if (lx.size() == 0) lx.add("");
				lx.set(0, C.AQUA + "B: " + ll.getWorld().getName() + "@" + ll.getBlockX() + "." + ll.getBlockY() + "." + ll.getBlockZ());
				im.setLore(lx);
				is.setItemMeta(im);
				e.getPlayer().getInventory().setItemInHand(is);
			}
		}
	}

	public Location[] getSelection(Player p)
	{
		Location[] l = new Location[2];
		@SuppressWarnings("deprecation")
		ItemStack is = p.getInventory().getItemInHand();
		if(is == null)
		{
			return null;
		}

		if(is.getType().equals(MaterialEnum.IRON_AXE.bukkitMaterial()))
		{
			ItemMeta im = is.getItemMeta();

			if(im.getDisplayName().equals(WAND_NAME))
			{
				String a = im.getLore().get(0).split(":")[1].trim();
				String b = im.getLore().get(1).split(":")[1].trim();
				l[0] = new Location(Bukkit.getWorld(a.split("@")[0]), Integer.valueOf(a.split("@")[1].split("\\.")[0]), Integer.valueOf(a.split("@")[1].split("\\.")[1]), Integer.valueOf(a.split("@")[1].split("\\.")[2]));
				l[1] = new Location(Bukkit.getWorld(b.split("@")[0]), Integer.valueOf(b.split("@")[1].split("\\.")[0]), Integer.valueOf(b.split("@")[1].split("\\.")[1]), Integer.valueOf(b.split("@")[1].split("\\.")[2]));
				return l;
			}
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void tick()
	{
		try
		{
			for(Player i : P.onlinePlayers())
			{
				if(i.getInventory().getItemInHand().getType().equals(MaterialEnum.IRON_AXE.bukkitMaterial()))
				{
					Location[] d = getSelection(i);

					if(d != null)
					{
						if(M.interval(3))
						{
							ParticleEffect.CRIT_MAGIC.display(0.1f, 1, d[0].clone().add(0.5, 0.5, 0.5).clone().add(Vector.getRandom().subtract(Vector.getRandom()).normalize().clone().multiply(0.65)), i);
							ParticleEffect.CRIT.display(0.1f, 1, d[1].clone().add(0.5, 0.5, 0.5).clone().add(Vector.getRandom().subtract(Vector.getRandom()).normalize().clone().multiply(0.65)), i);

							if(!d[0].getWorld().equals(d[1].getWorld()))
							{
								return;
							}

							if(d[0].distanceSquared(d[1]) > 64 * 64)
							{
								return;
							}

							int minx = Math.min(d[0].getBlockX(), d[1].getBlockX());
							int miny = Math.min(d[0].getBlockY(), d[1].getBlockY());
							int minz = Math.min(d[0].getBlockZ(), d[1].getBlockZ());
							int maxx = Math.max(d[0].getBlockX(), d[1].getBlockX());
							int maxy = Math.max(d[0].getBlockY(), d[1].getBlockY());
							int maxz = Math.max(d[0].getBlockZ(), d[1].getBlockZ());

							for(double j = minx - 1; j < maxx + 1; j += 0.25)
							{
								for(double k = miny - 1; k < maxy + 1; k += 0.25)
								{
									for(double l = minz - 1; l < maxz + 1; l += 0.25)
									{
										boolean jj = j == minx || j == maxx;
										boolean kk = k == miny || k == maxy;
										boolean ll = l == minz || l == maxz;
										double aa = j;
										double bb = k;
										double cc = l;

										if((jj && kk) || (jj && ll) || (ll && kk))
										{
											Vector push = new Vector(0, 0, 0);

											if(j == minx)
											{
												push.add(new Vector(-0.55, 0, 0));
											}

											if(k == miny)
											{
												push.add(new Vector(0, -0.55, 0));
											}

											if(l == minz)
											{
												push.add(new Vector(0, 0, -0.55));
											}

											if(j == maxx)
											{
												push.add(new Vector(0.55, 0, 0));
											}

											if(k == maxy)
											{
												push.add(new Vector(0, 0.55, 0));
											}

											if(l == maxz)
											{
												push.add(new Vector(0, 0, 0.55));
											}

											Location lv = new Location(d[0].getWorld(), aa, bb, cc).clone().add(0.5, 0.5, 0.5).clone().add(push);
											Color color = Color.getHSBColor((float) (0.5f + (Math.sin((aa + bb + cc + (M.ticksOnline() / 2)) / 20f) / 2)), 1, 1);
											new ParticleRedstone(color).play(lv, i);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		catch(Throwable e)
		{

		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on(VolumeConstructEvent e)
	{
		if(e.getCause() instanceof BlockPlaceEvent)
		{
			((BlockPlaceEvent) e.getCause()).getPlayer().sendMessage("Constructed " + e.getVolumeName());
		}

		if(e.getCause() instanceof BlockBreakEvent)
		{
			((BlockBreakEvent) e.getCause()).getPlayer().sendMessage("Constructed " + e.getVolumeName());
		}

		new Audio().s(SoundEnum.BLOCK_ENCHANTMENT_TABLE_USE.bukkitSound()).vp(5f, 1.5f).play(e.getMappedVolume().getReverseRealizedMapping().k().pickRandom());
	}

	public GMap<String, IVolume> getVolumes()
	{
		return volumes;
	}
}
