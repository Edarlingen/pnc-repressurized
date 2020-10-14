package me.desht.pneumaticcraft.common.heat;

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.heat.HeatBehaviour;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.heat.IHeatRegistry;
import me.desht.pneumaticcraft.api.semiblock.ISemiBlock;
import me.desht.pneumaticcraft.common.heat.BlockHeatProperties.CustomHeatEntry;
import me.desht.pneumaticcraft.common.heat.behaviour.HeatBehaviourManager;
import me.desht.pneumaticcraft.common.semiblock.SemiblockTracker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public enum HeatExchangerManager implements IHeatRegistry {
    INSTANCE;

    public static HeatExchangerManager getInstance() {
        return INSTANCE;
    }

    @Nonnull
    public LazyOptional<IHeatExchangerLogic> getLogic(IWorld world, BlockPos pos, Direction side) {
        return getLogic(world, pos, side, IHeatExchangerLogic.ALL_BLOCKS);
    }

    @Nonnull
    public LazyOptional<IHeatExchangerLogic> getLogic(IWorld world, BlockPos pos, Direction side, BiPredicate<IWorld,BlockPos> blockFilter) {
        if (!world.isAreaLoaded(pos, 0)) return LazyOptional.empty();
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te.getCapability(PNCCapabilities.HEAT_EXCHANGER_CAPABILITY, side).isPresent()) {
            return te.getCapability(PNCCapabilities.HEAT_EXCHANGER_CAPABILITY, side);
        } else {
            if (!blockFilter.test(world, pos)) {
                return LazyOptional.empty();
            }
            List<ISemiBlock> l = SemiblockTracker.getInstance().getAllSemiblocks(world, pos)
                    .filter(s -> s.getCapability(PNCCapabilities.HEAT_EXCHANGER_CAPABILITY).isPresent())
                    .collect(Collectors.toList());
            if (!l.isEmpty()) {
                return l.get(0).getCapability(PNCCapabilities.HEAT_EXCHANGER_CAPABILITY);
            }
            if (world.isAirBlock(pos)) {
                return LazyOptional.of(() -> HeatExchangerLogicAmbient.atPosition(world, pos));
            }
            BlockState state = world.getBlockState(pos);
            CustomHeatEntry entry = BlockHeatProperties.getInstance().getCustomHeatEntry(state.getBlock());
            if (entry != null && entry.testPredicates(state)) {
                return LazyOptional.of(entry::getLogic);
            } else {
                return LazyOptional.empty();
            }
        }
    }

    @Override
    public void registerBlockExchanger(Block block, double temperature, double thermalResistance) {
        BlockHeatProperties.getInstance().register(block.getRegistryName(), new CustomHeatEntry(block, (int) temperature, thermalResistance));
    }

    @Override
    public void registerHeatBehaviour(ResourceLocation id, Supplier<? extends HeatBehaviour<?>> heatBehaviour) {
        HeatBehaviourManager.getInstance().registerBehaviour(id, heatBehaviour);
    }

    @Override
    public IHeatExchangerLogic makeHeatExchangerLogic() {
        return new HeatExchangerLogicTicking();
    }
}
