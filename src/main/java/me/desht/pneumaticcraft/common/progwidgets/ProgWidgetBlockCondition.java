package me.desht.pneumaticcraft.common.progwidgets;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.drone.ProgWidgetType;
import me.desht.pneumaticcraft.common.ai.DroneAIBlockCondition;
import me.desht.pneumaticcraft.common.ai.DroneAIDig;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.common.core.ModProgWidgets;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ProgWidgetBlockCondition extends ProgWidgetCondition {
    public boolean checkingForAir;
    public boolean checkingForLiquids;

    public ProgWidgetBlockCondition() {
        super(ModProgWidgets.CONDITION_BLOCK.get());
    }

    @Override
    public List<ProgWidgetType<?>> getParameters() {
        return ImmutableList.of(ModProgWidgets.AREA.get(), ModProgWidgets.ITEM_FILTER.get(), ModProgWidgets.TEXT.get());
    }

    @Override
    protected DroneAIBlockCondition getEvaluator(IDroneBase drone, IProgWidget widget) {
        return new DroneAIBlockCondition(drone, (ProgWidgetAreaItemBase) widget) {
            @Override
            protected boolean evaluate(BlockPos pos) {
                boolean ret = false;
                if (checkingForAir && drone.world().isEmptyBlock(pos)) {
                    ret = true;
                } else if (checkingForLiquids && PneumaticCraftUtils.isBlockLiquid(drone.world().getBlockState(pos).getBlock())) {
                    ret = true;
                } else if (!checkingForAir && !checkingForLiquids || getConnectedParameters()[1] != null) {
                    ret = DroneAIDig.isBlockValidForFilter(drone.world(), pos, drone, progWidget);
                }
                maybeRecordMeasuredVal(drone, ret ? 1 : 0);
                return ret;
            }
        };
    }

    @Override
    public ResourceLocation getTexture() {
        return Textures.PROG_WIDGET_CONDITION_BLOCK;
    }

    @Override
    public void writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        if (checkingForAir) tag.putBoolean("checkingForAir", true);
        if (checkingForLiquids) tag.putBoolean("checkingForLiquids", true);
    }

    @Override
    public void readFromNBT(CompoundNBT tag) {
        super.readFromNBT(tag);
        checkingForAir = tag.getBoolean("checkingForAir");
        checkingForLiquids = tag.getBoolean("checkingForLiquids");
    }

    @Override
    public void writeToPacket(PacketBuffer buf) {
        super.writeToPacket(buf);
        buf.writeBoolean(checkingForAir);
        buf.writeBoolean(checkingForLiquids);
    }

    @Override
    public void readFromPacket(PacketBuffer buf) {
        super.readFromPacket(buf);
        checkingForAir = buf.readBoolean();
        checkingForLiquids = buf.readBoolean();
    }
}
