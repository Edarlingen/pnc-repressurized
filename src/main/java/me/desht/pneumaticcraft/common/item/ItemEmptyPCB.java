package me.desht.pneumaticcraft.common.item;

import me.desht.pneumaticcraft.api.item.ICustomDurabilityBar;
import me.desht.pneumaticcraft.common.PneumaticCraftTags;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.tileentity.TileEntityUVLightBox;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class ItemEmptyPCB extends ItemNonDespawning implements ICustomDurabilityBar {
    private static final String NBT_ETCH_PROGRESS = "pneumaticcraft:etch_progress";

    @Override
    public void appendHoverText(ItemStack stack, World player, List<ITextComponent> infoList, ITooltipFlag par4) {
        super.appendHoverText(stack, player, infoList, par4);
        int uvProgress = TileEntityUVLightBox.getExposureProgress(stack);
        int etchProgress = getEtchProgress(stack);

        if (etchProgress > 0) {
            infoList.add(xlate("pneumaticcraft.gui.tooltip.item.uvLightBox.etchProgress", etchProgress));
        }
        infoList.add(xlate("pneumaticcraft.gui.tooltip.item.uvLightBox.successChance", uvProgress));
        if (uvProgress < 100 && etchProgress == 0) {
            infoList.add(xlate("pneumaticcraft.gui.tooltip.item.uvLightBox.putInLightBox").withStyle(TextFormatting.GRAY));
        }
        if (uvProgress > 0) {
            infoList.add(xlate("pneumaticcraft.gui.tooltip.item.uvLightBox.putInAcid").withStyle(TextFormatting.GRAY));
        }
    }

    public static int getEtchProgress(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(NBT_ETCH_PROGRESS) : 0;
    }

    public static void setEtchProgress(ItemStack stack, int progress) {
        Validate.isTrue(progress >= 0 && progress <= 100);
        stack.getOrCreateTag().putInt(NBT_ETCH_PROGRESS, progress);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        int progress = TileEntityUVLightBox.getExposureProgress(stack);
        return 1 - progress / 100D;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        int progress = TileEntityUVLightBox.getExposureProgress(stack);
        return progress * 2 << 16 | 0xFF;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
        super.onEntityItemUpdate(stack, entityItem);

        if (entityItem.level.getFluidState(entityItem.blockPosition()).getType().is(PneumaticCraftTags.Fluids.ETCHING_ACID)) {
            if (!stack.hasTag()) {
                stack.setTag(new CompoundNBT());
            }
            int etchProgress = getEtchProgress(stack);
            if (etchProgress < 100) {
                if (entityItem.tickCount % (TileEntityConstants.PCB_ETCH_TIME / 5) == 0) {
                    setEtchProgress(stack, etchProgress + 1);
                }
                World world = entityItem.getCommandSenderWorld();
                if (world.random.nextInt(15) == 0) {
                    double x = entityItem.getX() + world.random.nextDouble() * 0.3 - 0.15;
                    double y = entityItem.getY() - 0.15;
                    double z = entityItem.getZ() + world.random.nextDouble() * 0.3 - 0.15;
                    world.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.05, 0.0);
                }
            } else if (!entityItem.level.isClientSide) {
                int successCount = 0;
                int failedCount = 0;
                int uvProgress = TileEntityUVLightBox.getExposureProgress(stack);
                for (int i = 0; i < stack.getCount(); i++) {
                    if (entityItem.level.random.nextInt(100) <= uvProgress) {
                        successCount++;
                    } else {
                        failedCount++;
                    }
                }

                ItemStack successStack = new ItemStack(successCount == 0 ? ModItems.FAILED_PCB.get() : ModItems.UNASSEMBLED_PCB.get(),
                        successCount == 0 ? failedCount : successCount);
                entityItem.setItem(successStack);

                // Only when we have failed items and the existing item entity wasn't reused already for the failed items.
                if (successCount > 0 && failedCount > 0) {
                    ItemStack failedStack = new ItemStack(ModItems.FAILED_PCB.get(), failedCount);
                    entityItem.level.addFreshEntity(new ItemEntity(entityItem.level, entityItem.getX(), entityItem.getY(), entityItem.getZ(), failedStack));
                }
            }
        }
        return false;
    }

    @Override
    public void fillItemCategory(ItemGroup group, NonNullList<ItemStack> items) {
        if (this.allowdedIn(group)) {
            items.add(new ItemStack(this));

            ItemStack stack = new ItemStack(this);
            TileEntityUVLightBox.setExposureProgress(stack, 100);
            items.add(stack);
        }
    }

    @Override
    public boolean shouldShowCustomDurabilityBar(ItemStack stack) {
        return ItemEmptyPCB.getEtchProgress(stack) > 0;
    }

    @Override
    public int getCustomDurabilityColour(ItemStack stack) {
        return MaterialColor.EMERALD.col;
    }

    @Override
    public float getCustomDurability(ItemStack stack) {
        return ItemEmptyPCB.getEtchProgress(stack) / 100f;
    }

    @Override
    public boolean isShowingOtherBar(ItemStack stack) {
        return true;
    }
}
