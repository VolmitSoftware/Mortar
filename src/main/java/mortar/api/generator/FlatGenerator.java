package mortar.api.generator;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import mortar.bukkit.compatibility.MaterialEnum;

public class FlatGenerator extends ChunkGenerator
{
	@SuppressWarnings("deprecation")
	@Override
	public short[][] generateExtBlockSections(World world, Random random, int x, int z, BiomeGrid biomes)
	{
		int maxY = world.getMaxHeight();
		short[][] result = new short[maxY / 16][];

		for(int i = 0; i < 16; i++)
		{
			for(int j = 0; j < 16; j++)
			{
				setBlock(result, i, 0, j, (short) MaterialEnum.BEDROCK.bukkitMaterial().getId());

				if(x % 2 == 0 ^ z % 2 == 0)
				{
					setBlock(result, i, 1, j, (short) MaterialEnum.PURPUR_BLOCK.bukkitMaterial().getId());
				}

				else
				{
					setBlock(result, i, 1, j, (short) MaterialEnum.END_BRICKS.bukkitMaterial().getId());
				}
			}
		}

		return result;
	}

	private void setBlock(short[][] result, int x, int y, int z, short blkid)
	{
		if(result[y >> 4] == null)
		{
			result[y >> 4] = new short[4096];
		}

		result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
	}
}
