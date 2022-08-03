/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.common.thirdparty.cofhcore;

import cofh.core.init.CoreEnchantments;
import cofh.lib.capability.IEnchantableItem;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.item.ItemVolumeModifier;
import me.desht.pneumaticcraft.api.misc.Symbols;
import me.desht.pneumaticcraft.common.config.ConfigHelper;
import me.desht.pneumaticcraft.lib.ModIds;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class HoldingEnchantableProvider implements ICapabilityProvider {
    public static final Capability<IEnchantableItem> CAPABILITY_ENCHANTABLE_ITEM = CapabilityManager.get(new CapabilityToken<>() {});
    static Enchantment holdingEnchantment = null;

    private final AllowHoldingEnchant ench = new AllowHoldingEnchant();
    private final LazyOptional<IEnchantableItem> lazy = LazyOptional.of(() -> ench);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return CAPABILITY_ENCHANTABLE_ITEM.orEmpty(cap, lazy);
    }

    static void registerVolumeModifier() {
        holdingEnchantment = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(ModIds.COFH_CORE, "holding"));
        if (holdingEnchantment != null) {
            PneumaticRegistry.getInstance().getItemRegistry().registerPneumaticVolumeModifier(new COFHVolumeModifier(holdingEnchantment));
        }
    }

    static void registerEnchantment() {
        CoreEnchantments.registerHoldingEnchantment();
    }

    public static class AllowHoldingEnchant implements IEnchantableItem {
        @Override
        public boolean supportsEnchantment(Enchantment enchantment) {
            return ConfigHelper.common().integration.cofhHoldingMultiplier.get() > 0 && enchantment == holdingEnchantment;
        }
    }

    public record COFHVolumeModifier(Enchantment holding) implements ItemVolumeModifier {
        @Override
        public int getNewVolume(ItemStack stack, int oldVolume) {
            return oldVolume * (1 + EnchantmentHelper.getItemEnchantmentLevel(holding, stack));
        }

        @Override
        public void addInfo(ItemStack stack, List<Component> text) {
            int nHolding = EnchantmentHelper.getItemEnchantmentLevel(holding, stack);
            if (nHolding > 0) {
                text.add(new TextComponent(Symbols.TRIANGLE_RIGHT + " ").append(holding.getFullname(nHolding)));
            }
        }
    }
}
