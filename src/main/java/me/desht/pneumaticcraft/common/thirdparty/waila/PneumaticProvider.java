package me.desht.pneumaticcraft.common.thirdparty.waila;

import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerDataProvider;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.common.capabilities.MachineAirHandler;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.heat.TemperatureData;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PneumaticProvider {
    public static class Data implements IServerDataProvider<TileEntity> {
        @Override
        public void appendServerData(CompoundNBT compoundNBT, ServerPlayerEntity serverPlayerEntity, World world, TileEntity te) {
            TileEntity teInfo;
            if (te instanceof IInfoForwarder) {
                teInfo = ((IInfoForwarder) te).getInfoTileEntity();
                if (teInfo != null) {
                    compoundNBT.putInt("infoX", teInfo.getPos().getX());
                    compoundNBT.putInt("infoY", teInfo.getPos().getY());
                    compoundNBT.putInt("infoZ", teInfo.getPos().getZ());
                }
            } else {
                teInfo = te;
            }
            if (teInfo != null) {
                teInfo.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY)
                        .ifPresent(h -> compoundNBT.putFloat("pressure", h.getPressure()));

                if (teInfo.getCapability(PNCCapabilities.HEAT_EXCHANGER_CAPABILITY).isPresent()) {
                    compoundNBT.put("heatData", new TemperatureData(teInfo).toNBT());
                }
            }
        }
    }

    public static class Component implements IComponentProvider {
        @Override
        public void appendBody(List<ITextComponent> tooltip, IDataAccessor accessor, IPluginConfig config) {
            CompoundNBT tag = accessor.getServerData();
            TileEntity te = accessor.getTileEntity();
            if (te instanceof IInfoForwarder){
                BlockPos infoPos = new BlockPos(tag.getInt("infoX"), tag.getInt("infoY"), tag.getInt("infoZ"));
                te = accessor.getWorld().getTileEntity(infoPos);
            }
            if (te != null) {
                te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY).ifPresent(h -> {
                    if (h instanceof MachineAirHandler) {
                        addTipToMachine(tooltip, (MachineAirHandler) h, tag.getFloat("pressure"));
                    }
                });
                handleHeatData(tooltip, tag);
            }
        }

        private void handleHeatData(List<ITextComponent> tooltip, CompoundNBT tag) {
            if (tag.contains("heatData")) {
                TemperatureData tempData = TemperatureData.fromNBT(tag.getCompound("heatData"));
                if (tempData.isMultisided()) {
                    for (Direction face : Direction.VALUES) {
                        if (tempData.hasData(face)) {
                            tooltip.add(HeatUtil.formatHeatString(face, (int) tempData.getTemperature(face)));
                        }
                    }
                } else if (tempData.hasData(null)) {
                    tooltip.add(HeatUtil.formatHeatString((int) tempData.getTemperature(null)));
                }
            }
        }

        private void addTipToMachine(List<ITextComponent> tooltip, MachineAirHandler airHandler, float pressure) {
            Map<String, String> values = new HashMap<>();

            values.put("pneumaticcraft.gui.tooltip.pressure", PneumaticCraftUtils.roundNumberTo(pressure, 2));
            values.put("pneumaticcraft.gui.tooltip.maxPressure", PneumaticCraftUtils.roundNumberTo(airHandler.getDangerPressure(), 1));

            for (Map.Entry<String, String> entry : values.entrySet()) {
                tooltip.add(new TranslationTextComponent(entry.getKey(), entry.getValue()));
            }
        }
    }
}
