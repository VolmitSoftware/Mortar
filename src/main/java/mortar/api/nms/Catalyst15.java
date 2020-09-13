package mortar.api.nms;

import mortar.api.sched.J;
import mortar.api.world.Area;
import mortar.api.world.MaterialBlock;
import mortar.bukkit.plugin.MortarAPIPlugin;
import mortar.lang.collection.GList;
import mortar.util.reflection.V;
import mortar.util.text.C;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_15_R1.*;
import net.minecraft.server.v1_15_R1.DataWatcher.Item;
import net.minecraft.server.v1_15_R1.IScoreboardCriteria.EnumScoreboardHealthDisplay;
import net.minecraft.server.v1_15_R1.PacketPlayOutTitle.EnumTitleAction;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftMetaBook;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.util.Vector;

import java.awt.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;

public class Catalyst15 extends CatalystPacketListener implements CatalystHost {
    private final Map<Player, PlayerSettings> playerSettings = new HashMap<>();
    private MethodHandle nextTickListGetter;

    public Catalyst15() {
        try {
            Field nextTickListField = TickListServer.class.getDeclaredField("nextTickList");
            nextTickListField.setAccessible(true);
            nextTickListGetter = MethodHandles.publicLookup().unreflectGetter(nextTickListField);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }

    @Override
    public void sendAdvancement(Player p, FrameType type, ItemStack is, String text) {
        AdvancementHolder15 a = new AdvancementHolder15(UUID.randomUUID().toString(), MortarAPIPlugin.p);
        a.withToast(true);
        a.withDescription("?");
        a.withFrame(type);
        a.withAnnouncement(false);
        a.withTitle(text);
        a.withTrigger("minecraft:impossible");
        a.withIcon(is.getData());
        a.withBackground("minecraft:textures/blocks/bedrock.png");
        a.loadAdvancement();
        a.sendPlayer(p);
        J.s(() -> a.delete(p), 5);
    }

    @Override
    public MaterialBlock getBlock(Location l) {
        return null;
    }

    @Override
    public MaterialBlock getBlock(World w, int x, int y, int z) {
        return null;
    }

    @Override
    public Object packetTime(long full, long day) {
        return new PacketPlayOutUpdateTime(full, day, true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlock(Location l, MaterialBlock m) {
        int x = l.getBlockX();
        int y = l.getBlockY();
        int z = l.getBlockZ();
        net.minecraft.server.v1_15_R1.World w = ((CraftWorld) l.getWorld()).getHandle();
        net.minecraft.server.v1_15_R1.Chunk chunk = w.getChunkAt(x >> 4, z >> 4);
        int combined = m.getMaterial().getId() + (m.getData() << 12);
        IBlockData ibd = net.minecraft.server.v1_15_R1.Block.getByCombinedId(combined);

        if (chunk.getSections()[y >> 4] == null) {
            chunk.getSections()[y >> 4] = new net.minecraft.server.v1_15_R1.ChunkSection(y >> 4 << 4);
        }

        net.minecraft.server.v1_15_R1.ChunkSection sec = chunk.getSections()[y >> 4];
        sec.setType(x & 15, y & 15, z & 15, ibd);
    }

    @Override
    public Object packetChunkUnload(int x, int z) {
        return new PacketPlayOutUnloadChunk(x, z);
    }

    @Override
    public Object packetChunkFullSend(Chunk chunk) {
        return new PacketPlayOutMapChunk(((CraftChunk) chunk).getHandle(), 65535);
    }

    @Override
    public Object packetBlockChange(Location block, int blockId, byte blockData) {
        PacketPlayOutBlockChange p = new PacketPlayOutBlockChange();
        V v = new V(p);
        v.set("a", toBlockPos(block));
        v.set("b", Block.getByCombinedId(blockId << 4 | (blockData & 15)));

        return p;
    }

    @Override
    public Object packetBlockAction(Location block, int action, int param, int blocktype) {
        return new PacketPlayOutBlockAction(toBlockPos(block), Block.getByCombinedId(blocktype).getBlock(), action, param);
    }

    @Override
    public Object packetAnimation(int eid, int animation) {
        PacketPlayOutAnimation p = new PacketPlayOutAnimation();
        V v = new V(p);
        v.set("a", eid);
        v.set("b", animation);

        return p;
    }

    @Override
    public Object packetBlockBreakAnimation(int eid, Location location, byte damage) {
        return new PacketPlayOutBlockBreakAnimation(eid, toBlockPos(location), damage);
    }

    @Override
    public Object packetGameState(int mode, float value) {
        return new PacketPlayOutGameStateChange(mode, value);
    }

    @Override
    public Object packetTitleMessage(String title) {
        return new PacketPlayOutTitle(EnumTitleAction.TITLE, s(title));
    }

    @Override
    public Object packetSubtitleMessage(String subtitle) {
        return new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, s(subtitle));
    }

    @Override
    public Object packetActionBarMessage(String subtitle) {
        return new PacketPlayOutTitle(EnumTitleAction.ACTIONBAR, s(subtitle));
    }

    @Override
    public Object packetResetTitle() {
        return new PacketPlayOutTitle(EnumTitleAction.RESET, null);
    }

    @Override
    public Object packetClearTitle() {
        return new PacketPlayOutTitle(EnumTitleAction.CLEAR, null);
    }

    @Override
    public Object packetTimes(int in, int stay, int out) {
        return new PacketPlayOutTitle(in, stay, out);
    }

    private BlockPosition toBlockPos(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public String getServerVersion() {
        return "1_15_R1";
    }

    @Override
    public String getVersion() {
        return "1.15.X";
    }

    @Override
    public void start() {
        openListener();
        Bukkit.getPluginManager().registerEvents(this, MortarAPIPlugin.p);
    }

    @Override
    public void stop() {
        closeListener();
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onOpened() {
        addGlobalIncomingListener((player, packet) -> {
            if (packet instanceof PacketPlayInSettings) {
                PacketPlayInSettings s = (PacketPlayInSettings) packet;
                playerSettings.put(player, new PlayerSettings(s.b(), s.viewDistance, ChatMode.values()[s.d().ordinal()], s.e(), s.f(), s.getMainHand().equals(EnumMainHand.RIGHT)));
            }

            return packet;
        });
    }

    @Override
    public void sendPacket(Player p, Object o) {
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket((Packet<?>) o);
    }

    @Override
    public void sendRangedPacket(double radius, Location l, Object o) {
        for (Player i : l.getWorld().getPlayers()) {
            if (canSee(l, i) && l.distanceSquared(i.getLocation()) <= radius * radius) {
                sendPacket(i, o);
            }
        }
    }

    @Override
    public void sendGlobalPacket(World w, Object o) {
        for (Player i : w.getPlayers()) {
            sendPacket(i, o);
        }
    }

    @Override
    public void sendUniversalPacket(Object o) {
        for (Player i : Bukkit.getOnlinePlayers()) {
            sendPacket(i, o);
        }
    }

    @Override
    public void sendViewDistancedPacket(Chunk c, Object o) {
        for (Player i : getObservers(c)) {
            sendPacket(i, o);
        }
    }

    @Override
    public boolean canSee(Chunk c, Player p) {
        return isWithin(p.getLocation().getChunk(), c, getViewDistance(p));
    }

    @Override
    public boolean canSee(Location l, Player p) {
        return canSee(l.getChunk(), p);
    }

    @Override
    public int getViewDistance(Player p) {
        PlayerSettings settings = getSettings(p);
        if (settings != null) {
            return settings.getViewDistance();
        } else return Bukkit.getServer().getViewDistance();
    }

    public boolean isWithin(Chunk center, Chunk check, int viewDistance) {
        return Math.abs(center.getX() - check.getX()) <= viewDistance && Math.abs(center.getZ() - check.getZ()) <= viewDistance;
    }

    @Override
    public List<Player> getObservers(Chunk c) {
        List<Player> p = new ArrayList<>();

        for (Player i : c.getWorld().getPlayers()) {
            if (canSee(c, i)) {
                p.add(i);
            }
        }

        return p;
    }

    @Override
    public List<Player> getObservers(Location l) {
        return getObservers(l.getChunk());
    }

    @EventHandler
    public void on(PlayerQuitEvent e) {
        playerSettings.remove(e.getPlayer());
    }

    @Override
    public PlayerSettings getSettings(Player p) {
        return playerSettings.get(p);
    }

    @Override
    public ShadowChunk shadowCopy(Chunk at) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Object> getTickList(World world) {
        TickListServer<Block> blockTickList = ((CraftWorld) world).getHandle().getBlockTickList();
        try {
            return (Set<Object>) nextTickListGetter.invoke(blockTickList);
        } catch (Throwable ignored) {
        }
        return Collections.emptySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Object> getTickListFluid(World world) {
        TickListServer<FluidType> fluidTickList = ((CraftWorld) world).getHandle().getFluidTickList();
        try {
            return (Set<Object>) nextTickListGetter.invoke(fluidTickList);
        } catch (Throwable ignored) {
        }
        return Collections.emptySet();
    }

    @Override
    public org.bukkit.block.Block getBlock(World world, Object tickListEntry) {
        BlockPosition pos = ((NextTickListEntry<?>) tickListEntry).a;
        return world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Object packetTabHeaderFooter(String h, String f) {
        PacketPlayOutPlayerListHeaderFooter p = new PacketPlayOutPlayerListHeaderFooter();
        V v = new V(p);
        v.set("header", s(h));
        v.set("footer", s(f));
        return p;
    }

    @Override
    public void scroll(Player sender, int previous) {
        sendPacket(sender, new PacketPlayOutHeldItemSlot(previous));
    }

    @Override
    public int getAction(Object packetIn) {
        return ((PacketPlayInEntityAction) packetIn).c().ordinal();
    }

    @Override
    public Vector getDirection(Object packet) {
        PacketPlayInFlying p = (PacketPlayInFlying) packet;
        float yaw = p.a(0);
        float pitch = p.b(0);
        double pitchRadians = Math.toRadians(-pitch);
        double yawRadians = Math.toRadians(-yaw);
        double sinPitch = Math.sin(pitchRadians);
        double cosPitch = Math.cos(pitchRadians);
        double sinYaw = Math.sin(yawRadians);
        double cosYaw = Math.cos(yawRadians);
        Vector v = new Vector(-cosPitch * sinYaw, sinPitch, -cosPitch * cosYaw);
        return new Vector(-v.getX(), v.getY(), -v.getZ());
    }

    @Override
    public void spawnFallingBlock(int eid, UUID id, Location l, Player player, MaterialBlock mb) {
        @SuppressWarnings("deprecation")
        int bid = mb.getMaterial().getId() + (mb.getData() << 12);
        PacketPlayOutSpawnEntity m = new PacketPlayOutSpawnEntity();
        V v = new V(m);
        v.set("a", eid);
        v.set("b", id);
        v.set("c", l.getX());
        v.set("d", l.getY());
        v.set("e", l.getZ());
        v.set("f", 0);
        v.set("g", 0);
        v.set("h", 0);
        v.set("i", 0);
        v.set("j", 0);
        v.set("k", EntityTypes.FALLING_BLOCK);
        v.set("l", bid);
        sendPacket(player, m);
    }

    @Override
    public void removeEntity(int eid, Player p) {
        PacketPlayOutEntityDestroy d = new PacketPlayOutEntityDestroy(eid);
        sendPacket(p, d);
    }

    @Override
    public void moveEntityRelative(int eid, Player p, double x, double y, double z, boolean onGround) {
        PacketPlayOutEntity.PacketPlayOutRelEntityMove r = new PacketPlayOutEntity.PacketPlayOutRelEntityMove();
        V v = new V(r);
        v.set("a", eid);
        v.set("b", (int) (x * 4096));
        v.set("c", (int) (y * 4096));
        v.set("d", (int) (z * 4096));
        v.set("e", (byte) 0);
        v.set("f", (byte) 0);
        v.set("g", onGround);
        v.set("h", onGround);
        sendPacket(p, r);
    }

    @Override
    public void teleportEntity(int eid, Player p, Location l, boolean onGround) {
        PacketPlayOutEntityTeleport t = new PacketPlayOutEntityTeleport();
        V v = new V(t);
        v.set("a", eid);
        v.set("b", l.getX());
        v.set("c", l.getY());
        v.set("d", l.getZ());
        v.set("e", (byte) 0);
        v.set("f", (byte) 0);
        v.set("g", onGround);
        sendPacket(p, t);
    }

    @Override
    public void spawnArmorStand(int eid, UUID id, Location l, int data, Player player) {
        PacketPlayOutSpawnEntity m = new PacketPlayOutSpawnEntity();
        V v = new V(m);
        v.set("a", eid);
        v.set("b", id);
        v.set("c", l.getX());
        v.set("d", l.getY());
        v.set("e", l.getZ());
        v.set("f", 0);
        v.set("g", 0);
        v.set("h", 0);
        v.set("i", 0);
        v.set("j", 0);
        v.set("k", EntityTypes.ARMOR_STAND);
        v.set("l", 0);
        sendPacket(player, m);
    }

    private IChatBaseComponent s(String s) {
        return new ChatComponentText(s);
    }

    @Override
    public void sendTeam(Player p, String id, String name, String prefix, String suffix, C color, int mode) {
        PacketPlayOutScoreboardTeam k = new PacketPlayOutScoreboardTeam();
        V v = new V(k);
        v.set("a", id);
        v.set("b", s(name));
        v.set("i", mode); // 0 = new, 1 = remove, 2 = update, 3 = addplayer, 4 = removeplayer
        v.set("c", s(prefix));
        v.set("d", s(suffix));
        v.set("j", 0);
        v.set("f", "never");
        v.set("e", "always");
        v.set("g", EnumChatFormat.valueOf(color.name().replaceAll("MAGIC", "OBFUSCATED")));
        sendPacket(p, k);
    }

    @Override
    public void addTeam(Player p, String id, String name, String prefix, String suffix, C color) {
        sendTeam(p, id, name, prefix, suffix, color, 0);
    }

    @Override
    public void updateTeam(Player p, String id, String name, String prefix, String suffix, C color) {
        sendTeam(p, id, name, prefix, suffix, color, 2);
    }

    @Override
    public void removeTeam(Player p, String id) {
        sendTeam(p, id, "", "", "", C.WHITE, 1);
    }

    @Override
    public void addToTeam(Player p, String id, String... entities) {
        PacketPlayOutScoreboardTeam k = new PacketPlayOutScoreboardTeam();
        V v = new V(k);
        v.set("a", id);
        v.set("i", 3);
        v.set("h", Arrays.asList(entities));
        sendPacket(p, k);
    }

    @Override
    public void removeFromTeam(Player p, String id, String... entities) {
        PacketPlayOutScoreboardTeam k = new PacketPlayOutScoreboardTeam();
        V v = new V(k);
        v.set("a", id);
        v.set("i", 4);
        v.set("h", Arrays.asList(entities));
        sendPacket(p, k);
    }

    @Override
    public void displayScoreboard(Player p, int slot, String id) {
        PacketPlayOutScoreboardDisplayObjective k = new PacketPlayOutScoreboardDisplayObjective();
        V v = new V(k);
        v.set("a", slot);
        v.set("b", id);
        sendPacket(p, k);
    }

    @Override
    public void displayScoreboard(Player p, C slot, String id) {
        displayScoreboard(p, 3 + slot.getMeta(), id);
    }

    @Override
    public void sendNewObjective(Player p, String id, String name) {
        PacketPlayOutScoreboardObjective k = new PacketPlayOutScoreboardObjective();
        V v = new V(k);
        v.set("d", 0);
        v.set("a", id);
        v.set("b", s(name));
        v.set("c", EnumScoreboardHealthDisplay.INTEGER);
        sendPacket(p, k);
    }

    @Override
    public void sendDeleteObjective(Player p, String id) {
        PacketPlayOutScoreboardObjective k = new PacketPlayOutScoreboardObjective();
        V v = new V(k);
        v.set("d", 1);
        v.set("a", id);
        v.set("b", s("memes"));
        v.set("c", EnumScoreboardHealthDisplay.INTEGER);
        sendPacket(p, k);
    }

    @Override
    public void sendEditObjective(Player p, String id, String name) {
        PacketPlayOutScoreboardObjective k = new PacketPlayOutScoreboardObjective();
        V v = new V(k);
        v.set("d", 2);
        v.set("a", id);
        v.set("b", s(name));
        v.set("c", EnumScoreboardHealthDisplay.INTEGER);
        sendPacket(p, k);
    }

    @Override
    public void sendScoreUpdate(Player p, String name, String objective, int score) {
        PacketPlayOutScoreboardScore k = new PacketPlayOutScoreboardScore();
        V v = new V(k);
        v.set("a", name);
        v.set("b", objective);
        v.set("c", score);
        v.set("d", ScoreboardServer.Action.CHANGE);
        sendPacket(p, k);
    }

    @Override
    public void sendScoreRemove(Player p, String name, String objective) {
        PacketPlayOutScoreboardScore k = new PacketPlayOutScoreboardScore();
        V v = new V(k);
        v.set("a", name);
        v.set("b", objective);
        v.set("c", 0);
        v.set("d", ScoreboardServer.Action.REMOVE);
        sendPacket(p, k);
    }

    @Override
    public void sendRemoveGlowingColorMetaEntity(Player p, UUID glowing) {
        String c = teamCache.get(p.getUniqueId() + "-" + glowing);

        if (c != null) {
            teamCache.remove(p.getUniqueId() + "-" + glowing);
            removeFromTeam(p, c, glowing.toString());
            removeTeam(p, c);
        }
    }

    @Override
    public void sendRemoveGlowingColorMetaPlayer(Player p, UUID glowing, String name) {
        String c = teamCache.get(p.getUniqueId() + "-" + glowing);

        if (c != null) {
            teamCache.remove(p.getUniqueId() + "-" + glowing);
            removeFromTeam(p, c, name);
            removeTeam(p, c);
        }
    }

    @Override
    public void sendGlowingColorMeta(Player p, Entity glowing, C color) {
        if (glowing instanceof Player) {
            sendGlowingColorMetaName(p, p.getName(), color);
        } else {
            sendGlowingColorMetaEntity(p, glowing.getUniqueId(), color);
        }
    }

    @Override
    public void sendGlowingColorMetaEntity(Player p, UUID euid, C color) {
        sendGlowingColorMetaName(p, euid.toString(), color);
    }

    @Override
    public void sendGlowingColorMetaName(Player p, String euid, C color) {
        String c = teamCache.get(p.getUniqueId() + "-" + euid);

        if (c != null) {
            updateTeam(p, c, c, color.toString(), C.RESET.toString(), color);
            sendEditObjective(p, c, c);
        } else {
            c = "v" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15);
            teamCache.put(p.getUniqueId() + "-" + euid, c);

            addTeam(p, c, c, color.toString(), C.RESET.toString(), color);
            updateTeam(p, c, c, color.toString(), C.RESET.toString(), color);

            addToTeam(p, c, euid);
        }
    }

    @Override
    public void sendRemoveGlowingColorMeta(Player p, Entity glowing) {
        String c = teamCache.get(p.getUniqueId() + "-" + glowing.getUniqueId());

        if (c != null) {
            teamCache.remove(p.getUniqueId() + "-" + glowing.getUniqueId());
            removeFromTeam(p, c, glowing instanceof Player ? glowing.getName() : glowing.getUniqueId().toString());
            removeTeam(p, c);
        }
    }

    @Override
    public void updatePassengers(Player p, int vehicle, int... passengers) {
        PacketPlayOutMount mount = new PacketPlayOutMount();
        V v = new V(mount);
        v.set("a", vehicle);
        v.set("b", passengers);
        sendPacket(p, mount);
    }

    @Override
    public void sendEntityMetadata(Player p, int eid, Object... objects) {
        PacketPlayOutEntityMetadata md = new PacketPlayOutEntityMetadata();
        V v = new V(md);
        v.set("a", eid);
        v.set("b", Arrays.asList(objects));
        sendPacket(p, md);
    }

    @Override
    public void sendEntityMetadata(Player p, int eid, List<Object> objects) {
        sendEntityMetadata(p, eid, objects.toArray(new Object[0]));
    }

    @Override
    public Object getMetaEntityRemainingAir(int airTicksLeft) {
        return new Item<>(new DataWatcherObject<>(1, DataWatcherRegistry.b), airTicksLeft);
    }

    @Override
    public Object getMetaEntityCustomName(String name) {
        Optional<IChatBaseComponent> c = Optional.of(s(name));
        return new Item<>(new DataWatcherObject<>(2, DataWatcherRegistry.f), c);
    }

    @Override
    public Object getMetaEntityProperties(boolean onFire, boolean crouched, boolean sprinting, boolean swimming, boolean invisible, boolean glowing, boolean flyingElytra) {
        byte bits = 0;
        bits += onFire ? 1 : 0;
        bits += crouched ? 2 : 0;
        bits += sprinting ? 8 : 0;
        bits += swimming ? 10 : 0;
        bits += invisible ? 20 : 0;
        bits += glowing ? 40 : 0;
        bits += flyingElytra ? 80 : 0;

        return new Item<>(new DataWatcherObject<>(0, DataWatcherRegistry.a), bits);
    }

    @Override
    public Object getMetaEntityGravity(boolean gravity) {
        return new Item<>(new DataWatcherObject<>(5, DataWatcherRegistry.i), gravity);
    }

    @Override
    public Object getMetaEntitySilenced(boolean silenced) {
        return new Item<>(new DataWatcherObject<>(4, DataWatcherRegistry.i), silenced);
    }

    @Override
    public Object getMetaEntityCustomNameVisible(boolean visible) {
        return new Item<>(new DataWatcherObject<>(3, DataWatcherRegistry.i), visible);
    }

    @Override
    public Object getMetaArmorStandProperties(boolean isSmall, boolean hasArms, boolean noBasePlate, boolean marker) {
        byte bits = 0;
        bits += isSmall ? 1 : 0;
        bits += hasArms ? 2 : 0;
        bits += noBasePlate ? 8 : 0;
        bits += marker ? 10 : 0;

        return new Item<>(new DataWatcherObject<>(13, DataWatcherRegistry.a), bits);
    }

    @Override
    public void sendItemStack(Player p, ItemStack is, int slot) {
        sendPacket(p, new PacketPlayOutSetSlot(((CraftPlayer) p).getHandle().activeContainer.windowId, slot, CraftItemStack.asNMSCopy(is)));
    }

    @Override
    public void resendChunkSection(Player p, int x, int y, int z) {
        ShadowChunk sc = shadowCopy(p.getWorld().getChunkAt(x, z));
        sc.modifySection(y);
        new PacketBuffer().q(sc.flush()).flush(p);
    }

    @Override
    public void redstoneParticle(Player p, Color c, Location l, float size) {
        ParticleParam param = new ParticleParamRedstone(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, size);
        Packet<?> packet = new PacketPlayOutWorldParticles(param, p.getLocation().distanceSquared(l) > 64 * 64, (float) l.getX(), (float) l.getY(), (float) l.getZ(), 0f, 0f, 0f, 0f, 1);
        sendPacket(p, packet);
    }

    @Override
    public void redstoneParticle(double range, Color c, Location l, float size) {
        ParticleParam param = new ParticleParamRedstone(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, size);
        Packet<?> particle = new PacketPlayOutWorldParticles(param, range > 64, (float) l.getX(), (float) l.getY(), (float) l.getZ(), 0f, 0f, 0f, 0f, 1);

        for (Player player : new Area(l, range).getNearbyPlayers()) {
            sendPacket(player, particle);
        }
    }

    @Override
    public Object getIChatBaseComponent(BaseComponent bc) {
        return IChatBaseComponent.ChatSerializer.a(ComponentSerializer.toString(bc));
    }

    @Override
    public void add(BookMeta bm, GList<BaseComponent> pages) {
        ((CraftMetaBook) bm).pages = pages.convert((bc) -> IChatBaseComponent.ChatSerializer.a(ComponentSerializer.toString(bc)));
    }
}