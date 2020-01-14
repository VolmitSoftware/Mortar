package mortar.api.nms;

import java.util.Objects;

import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import net.minecraft.server.v1_15_R1.IRegistry;
import net.minecraft.server.v1_15_R1.Item;
import net.minecraft.server.v1_15_R1.MinecraftKey;

public class AdvancementHolder15 extends AdvancementHolder
{
	public AdvancementHolder15(String id, Plugin p)
	{
		super(id, p);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void removeAdvancement(NamespacedKey k)
	{
		CraftMagicNumbers.INSTANCE.removeAdvancement(getID());
	}

	@Override
	public String getMinecraftIDFrom(ItemStack stack)
	{
		final int check = Item.getId(CraftItemStack.asNMSCopy(stack).getItem());
		final MinecraftKey matching = IRegistry.ITEM.keySet().stream().filter(key -> Item.getId(IRegistry.ITEM.get(key)) == check).findFirst().orElse(null);
		return Objects.toString(matching, null);
	}
}