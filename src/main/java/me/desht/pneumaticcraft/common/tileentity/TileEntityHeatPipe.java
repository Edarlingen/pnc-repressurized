package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.block.BlockPneumaticCraft;
import me.desht.pneumaticcraft.common.core.ModTileEntities;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import java.util.function.BiPredicate;

public class TileEntityHeatPipe extends TileEntityTickableBase implements ICamouflageableTE, IHeatExchangingTE {
    private final IHeatExchangerLogic heatExchanger = PneumaticRegistry.getInstance().getHeatRegistry().makeHeatExchangerLogic();
    private final LazyOptional<IHeatExchangerLogic> heatCap = LazyOptional.of(() -> heatExchanger);

    private BlockState camoState;

    public TileEntityHeatPipe() {
        super(ModTileEntities.HEAT_PIPE.get());
    }

    @Override
    public IItemHandler getPrimaryInventory() {
        return null;
    }

    @Override
    public LazyOptional<IHeatExchangerLogic> getHeatCap(Direction side) {
        return heatCap;
    }

    @Override
    public BiPredicate<IWorld, BlockPos> heatExchangerBlockFilter() {
        // heat pipes don't connect to air or fluids
        return (world, pos) -> !world.isEmptyBlock(pos) && !(world.getBlockState(pos).getBlock() instanceof FlowingFluidBlock);
    }

    @Override
    protected void onFirstServerTick() {
        super.onFirstServerTick();

        updateConnections();
    }

    @Override
    public void onNeighborBlockUpdate(BlockPos fromPos) {
        super.onNeighborBlockUpdate(fromPos);

        updateConnections();
    }

    public void updateConnections() {
        BlockState state = getBlockState();
        boolean changed = false;
        for (Direction dir : DirectionUtil.VALUES) {
            BooleanProperty prop = BlockPneumaticCraft.connectionProperty(dir);
            boolean connected = heatExchanger.isSideConnected(dir);
            if (state.getValue(prop) != connected) {
                state = state.setValue(prop, connected);
                changed = true;
            }
        }
        if (changed) level.setBlockAndUpdate(worldPosition, state);
    }

    @Override
    public void writeToPacket(CompoundNBT tag) {
        super.writeToPacket(tag);

        ICamouflageableTE.writeCamo(tag, camoState);
    }

    @Override
    public void readFromPacket(CompoundNBT tag) {
        super.readFromPacket(tag);

        camoState = ICamouflageableTE.readCamo(tag);
    }

    @Override
    public BlockState getCamouflage() {
        return camoState;
    }

    @Override
    public void setCamouflage(BlockState state) {
        camoState = state;
        ICamouflageableTE.syncToClient(this);
    }

    @Override
    public IHeatExchangerLogic getHeatExchanger(Direction dir) {
        return heatExchanger;
    }
}
