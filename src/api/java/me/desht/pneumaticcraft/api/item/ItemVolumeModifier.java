package me.desht.pneumaticcraft.api.item;

import net.minecraft.item.ItemStack;

/**
 * A functional interface to modify a given Pneumatic item's volume based on attributes of the item stack
 * (generally values in its NBT, e.g. upgrades or enchantments). Instances of this can be registered
 * with {@link IItemRegistry#registerPneumaticVolumeModifier(ItemVolumeModifier)}.
 */
@FunctionalInterface
public interface ItemVolumeModifier {
    /**
     * Given an item stack, which is a pneumatic item, and its current volume, return a new, modified volume
     * @param stack the item
     * @param oldVolume the initial volume
     * @return the modified volume
     */
    int getNewVolume(ItemStack stack, int oldVolume);
}
