package mortar.api.rift;

import java.io.File;

import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import mortar.lang.collection.GList;
import org.json.JSONObject;

public interface Rift
{
	public Environment getEnvironment();

	public JSONObject toJSON();

	public boolean isLockingTime();

	public long getLockedTime();

	public Rift setForceLoadX(int x);

	public Rift setForceLoadZ(int z);

	public int getForceLoadX();

	public int getForceLoadZ();

	public boolean shouldKeepLoaded(Chunk c);

	public Rift setLockTime(long time);

	public Rift send(Player p);

	public Rift reload();

	public Rift destroy();

	public Rift setEnvironment(Environment environment);

	public String getName();

	public Rift setName(String name);

	public File getWorldFolder();

	public World getWorld();

	public boolean isLoaded();

	public Rift load();

	public Rift unload();

	public Rift save();

	public Rift saveConfiguration();

	public Rift slowlyPreload();

	public Rift colapse();

	public Rift setTemporary(boolean temp);

	public boolean isTemporary();

	public Rift setGenerator(Class<? extends ChunkGenerator> generator);

	public Class<? extends ChunkGenerator> getGenerator();

	public Rift setRule(String key, String value);

	public Rift removeRule(String key);

	public String getRule(String key);

	public GList<String> getRules();

	public long getSeed();

	public Rift setSeed(long seed);

	public Rift setPhysicsThrottle(int delay);

	public int getPhysicsThrottle();

	public Rift setEntityTickLimit(double ms);

	public Rift setTileTickLimit(double ms);

	public double getEntityTickLimit();

	public double getTileTickLimit();

	public double getEntityTickTime();

	public double getTileTickTime();

	public PhysicsEngine getPhysicsEngine();

	public int getMaxTNTUpdatesPerTick();

	public Rift setMaxTNTUpdatesPerTick(int max);

	public int getAnimalActivationRange();

	public Rift setAnimalActivationRange(int blocks);

	public int getMiscActivationRange();

	public Rift setMiscActivationRange(int blocks);

	public int getMonsterActivationRange();

	public Rift setMonsterActivationRange(int blocks);

	public int getArrowDespawnRate();

	public Rift setArrowDespawnRate(int ticks);

	public int getItemDespawnRate();

	public Rift setItemDespawnRate(int ticks);

	public double getXPMergeRadius();

	public Rift setXPMergeRadius(double radius);

	public double getItemMergeRadius();

	public Rift setItemMergeRadius(double radius);

	public int getViewDistance();

	public Rift setViewDistance(int viewDistance);

	public int getHopperTransferAmount();

	public Rift setHopperTransferAmount(int amt);

	public int getHopperTransferRate();

	public Rift setHopperTransferRate(int ticks);

	public int getHopperCheckRate();

	public Rift setHopperCheckRate(int ticks);

	public int getHangingTickRate();

	public Rift setHangingTickRate(int ticks);

	public boolean isNerfSpawnerMobs();

	public Rift setNerfSpawnerMobs(boolean nerf);

	public int getPlayerTrackingRange();

	public Rift setPlayerTrackingRange(int range);

	public boolean isRandomLightUpdates();

	public Rift setRandomLightUpdates(boolean b);

	public Rift setSpawn(Location location);

	public Location getSpawn();

	public Rift setAllowBosses(boolean allowBosses);

	public boolean areBossesAllowed();

	public Rift setForcedGameMode(GameMode gm);

	public boolean isForcingGameMode();

	public GameMode getForcedGameMode();

	public Difficulty getDifficulty();

	public Rift setDifficulty(Difficulty difficulty);

	public Rift setUnloadWhenEmpty(int seconds);

	public int getTicksWhenEmpty();

	public Rift setWorldBorderSize(double size);

	public double getWorldBorderSize();

	public Rift setWorldBorderCenter(double x, double z);

	public double getWorldBorderX();

	public double getWorldBorderZ();

	public Rift setWorldBorderAnimationTime(int seconds);

	public double getWorldBorderAnimationTime();

	public Rift setWorldBorderWarningDistance(int blocks);

	public int getWorldBorderWarningDistance();

	public Rift setWorldBorderWarningTime(int seconds);

	public int getWorldBorderWarningTime();

	public Rift setWorldBorderEnabled(boolean t);

	public boolean isWorldBorderEnabled();
}
