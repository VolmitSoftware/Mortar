package mortar.api.nms;

import com.google.gson.stream.MalformedJsonException;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import mortar.api.sched.S;
import mortar.lang.collection.GList;

public class NMP
{
	public static CatalystHost host;

	/**
	 * Player information
	 *
	 * @author cyberpwn
	 */
	public static class PLAYER
	{
		public static void time(Player p, long full, long day)
		{
			new PacketBuffer().q(host.packetTime(full, day));
		}

		public static boolean canSee(Player p, Chunk c)
		{
			return host.canSee(c, p);
		}

		public static void sendTablist(Player p, String h, String f)
		{
			try
			{
				new PacketBuffer().q(host.packetTabHeaderFooter(h, f)).flush(p);
			}
			catch(Exception ex)
			{
			}
		}

		/**
		 * Can the player see the chunk holding this location?
		 *
		 * @param p
		 *            the player
		 * @param l
		 *            the location
		 * @return true if the chunk is visible
		 */
		public static boolean canSee(Player p, Location l)
		{
			return host.canSee(l, p);
		}

		/**
		 * Get the players last known view distance
		 *
		 * @param p
		 *            the player
		 * @return the view distance (maxxed by the server distance)
		 */
		public static int viewDistance(Player p)
		{
			return settings(p).getViewDistance();
		}

		/**
		 * Get player settings
		 *
		 * @param p
		 *            the player
		 * @return the settings
		 */
		public static PlayerSettings settings(Player p)
		{
			return host.getSettings(p);
		}
	}

	/**
	 * Messaging
	 *
	 * @author cyberpwn
	 *
	 */
	public static class MESSAGE
	{
		/**
		 * Send an advancement
		 *
		 * @param player
		 *            the player
		 * @param item
		 *            the item
		 * @param msg
		 *            the message
		 * @param type
		 *            the type
		 */
		public static void advance(Player player, ItemStack item, String msg, FrameType type)
		{
			new S()
			{
				@Override
				public void run()
				{
					host.sendAdvancement(player, type, item, msg);
				}
			};
		}

		/**
		 * Send a player action bar
		 *
		 * @param p
		 *            the player
		 * @param msg
		 *            the msg
		 */
		public static void action(Player p, String msg)
		{
			new PacketBuffer().q(host.packetActionBarMessage(msg)).flush(p);
		}

		/**
		 * Reset title times
		 *
		 * @param p
		 *            the player
		 */
		public static void reset(Player p)
		{
			new PacketBuffer().q(host.packetResetTitle()).flush(p);
		}

		/**
		 * Send a title message
		 *
		 * @param p
		 *            the player
		 * @param title
		 *            the title
		 * @param subtitle
		 *            the subtitle
		 * @param in
		 *            the in
		 * @param stay
		 *            the stay
		 * @param out
		 *            the out
		 */
		public static void title(Player p, String title, String subtitle, int in, int stay, int out)
		{
			new PacketBuffer().q(host.packetTitleMessage(title)).q(host.packetSubtitleMessage(subtitle)).q(host.packetTimes(in, stay, out)).flush(p);
		}
	}

	/**
	 * Chunk tools
	 *
	 * @author cyberpwn
	 *
	 */
	public static class CHUNK
	{
		/**
		 * Reload (fix) the specified chunk
		 *
		 * @param p
		 *            the player
		 * @param at
		 *            the chunk
		 */
		public static void refresh(Player p, Chunk at)
		{
			unload(p, at);

			if(host.canSee(at, p))
			{
				host.sendPacket(p, host.packetChunkFullSend(at));
			}
		}

		public static void refreshIgnorePosition(Player p, Chunk at)
		{
			unloadIgnorePosition(p, at);

			host.sendPacket(p, host.packetChunkFullSend(at));
		}

		/**
		 * Unload the specified chunk
		 *
		 * @param p
		 *            the player
		 * @param at
		 *            the chunk
		 */
		public static void unload(Player p, Chunk at)
		{
			if(host.canSee(at, p))
			{
				host.sendPacket(p, host.packetChunkUnload(at.getX(), at.getZ()));
			}
		}

		public static void unloadIgnorePosition(Player p, Chunk at)
		{
			host.sendPacket(p, host.packetChunkUnload(at.getX(), at.getZ()));
		}

		/**
		 * Flush shadow chunk data to the player
		 *
		 * @param p
		 *            the player
		 * @param chunk
		 *            the chunk
		 */
		public static void data(Player p, ShadowChunk chunk)
		{
			if(host.canSee(chunk.getSource(), p))
			{
				for(Object i : chunk.flush())
				{
					host.sendPacket(p, i);
				}
			}

			else
			{
				chunk.flush();
			}
		}

		/**
		 * Get a shadow clone of the given chunk
		 *
		 * @param at
		 *            the chunk
		 * @return the shadow clone chunk
		 */
		public static ShadowChunk shadow(Chunk at)
		{
			return host.shadowCopy(at);
		}

		public static void resend(Player p, int x, int y, int z)
		{
			host.resendChunkSection(p, x, y, z);
		}

		public static void resend(Location c)
		{
			resend(c.getChunk(), c.getBlockY() >> 4);
		}

		public static void resend(Chunk c, int y)
		{
			resend(c.getWorld(), c.getX(), y, c.getZ());
		}

		public static void resend(World w, int x, int y, int z)
		{
			Chunk c = w.getChunkAt(x, z);
			ShadowChunk s = shadow(c);
			s.modifySection(y);
			PacketBuffer pb = new PacketBuffer().q(s.flush());

			for(Player i : nearbyPlayers(c))
			{
				pb.flush(i);
			}
		}

		public static GList<Player> nearbyPlayers(Chunk i)
		{
			GList<Player> px = new GList<>();

			for(Player x : i.getWorld().getPlayers())
			{
				if(NMP.host.canSee(i, x))
				{
					px.add(x);
				}
			}

			return px;
		}
	}
}
