package me.desht.pneumaticcraft.common.thirdparty.waila;

import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerDataProvider;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.semiblock.ISemiBlock;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.thirdparty.ModNameCache;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Names;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.util.List;

public class EntityProvider {

    public static class Data implements IServerDataProvider<Entity> {
        @Override
        public void appendServerData(CompoundNBT compoundNBT, ServerPlayerEntity serverPlayerEntity, World world, Entity entity) {
            entity.getCapability(PNCCapabilities.AIR_HANDLER_CAPABILITY)
                    .ifPresent(h -> compoundNBT.putFloat("Pressure", h.getPressure()));
            entity.getCapability(PNCCapabilities.HEAT_EXCHANGER_CAPABILITY)
                    .ifPresent(h -> compoundNBT.putFloat("Temperature", h.getTemperatureAsInt()));
            if (entity instanceof ISemiBlock) {
                ((ISemiBlock) entity).serializeNBT(compoundNBT);
            }
        }
    }

    public static class Component implements IEntityComponentProvider {
        @Override
        public void appendHead(List<ITextComponent> tooltip, IEntityAccessor accessor, IPluginConfig config) {
            tooltip.add(accessor.getEntity().getDisplayName().deepCopy().mergeStyle(TextFormatting.WHITE));
        }

        @Override
        public void appendBody(List<ITextComponent> tooltip, IEntityAccessor accessor, IPluginConfig config) {
            if (accessor.getServerData().contains("Pressure")) {
                float pressure = accessor.getServerData().getFloat("Pressure");
                tooltip.add(new TranslationTextComponent("pneumaticcraft.gui.tooltip.pressure", PneumaticCraftUtils.roundNumberTo(pressure, 1)));
            }
            if (accessor.getServerData().contains("Temperature")) {
                tooltip.add(HeatUtil.formatHeatString(accessor.getServerData().getInt("Temperature")));
            }
            if (accessor.getEntity() instanceof ISemiBlock) {
                ISemiBlock semiBlock = (ISemiBlock) accessor.getEntity();
                semiBlock.addTooltip(tooltip, accessor.getPlayer(), accessor.getServerData(), accessor.getPlayer().isSneaking());
                BlockPos pos = semiBlock.getBlockPos();
                BlockState state = accessor.getWorld().getBlockState(pos);
                tooltip.add(state.getBlock().getTranslatedName().mergeStyle(TextFormatting.YELLOW));
            }
        }

        @Override
        public void appendTail(List<ITextComponent> tooltip, IEntityAccessor accessor, IPluginConfig config) {
            String modName = ModNameCache.getModName(Names.MOD_ID);
            tooltip.add(new StringTextComponent(modName).mergeStyle(TextFormatting.BLUE, TextFormatting.ITALIC));
        }
    }
}
