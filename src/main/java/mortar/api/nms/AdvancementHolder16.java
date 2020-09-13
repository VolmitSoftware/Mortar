package mortar.api.nms;

import java.util.Objects;

import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R2.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import net.minecraft.server.v1_16_R2.IRegistry;
import net.minecraft.server.v1_16_R2.Item;
import net.minecraft.server.v1_16_R2.MinecraftKey;

public class AdvancementHolder16 extends AdvancementHolder
{
    public AdvancementHolder16(String id, Plugin p)
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