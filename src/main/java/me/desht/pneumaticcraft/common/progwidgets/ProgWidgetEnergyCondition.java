package me.desht.pneumaticcraft.common.progwidgets;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.drone.ProgWidgetType;
import me.desht.pneumaticcraft.common.ai.DroneAIBlockCondition;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.common.core.ModProgWidgets;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.List;

public class ProgWidgetEnergyCondition extends ProgWidgetCondition {
    public ProgWidgetEnergyCondition() {
        super(ModProgWidgets.CONDITION_RF.get());
    }

    @Override
    public ResourceLocation getTexture() {
        return Textures.PROG_WIDGET_CONDITION_ENERGY;
    }

    @Override
    public List<ProgWidgetType<?>> getParameters() {
        return ImmutableList.of(ModProgWidgets.AREA.get(), ModProgWidgets.TEXT.get());
    }

    @Override
    protected DroneAIBlockCondition getEvaluator(IDroneBase drone, IProgWidget widget) {
        return new DroneAIBlockCondition(drone, (ProgWidgetAreaItemBase) widget) {
            @Override
            protected boolean evaluate(BlockPos pos) {
                TileEntity te = drone.world().getBlockEntity(pos);
                if (te == null) return false;
                int energy = 0;
                for (Direction face : DirectionUtil.VALUES) {
                    if (getSides()[face.get3DDataValue()]) {
                        energy = Math.max(energy, getEnergy(te, face));
                    }
                }
                maybeRecordMeasuredVal(drone, energy);
                return ((ICondition) progWidget).getOperator().evaluate(energy,((ICondition) progWidget).getRequiredCount());
            }

            private int getEnergy(TileEntity te, Direction side) {
                return te.getCapability(CapabilityEnergy.ENERGY, side).map(IEnergyStorage::getEnergyStored).orElse(0);
            }
        };
    }
}
