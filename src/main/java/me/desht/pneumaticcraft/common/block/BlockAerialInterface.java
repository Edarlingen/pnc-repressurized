package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.common.core.ModBlocks;
import me.desht.pneumaticcraft.common.tileentity.TileEntityAerialInterface;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

public class BlockAerialInterface extends BlockPneumaticCraft {
    public BlockAerialInterface() {
        super(ModBlocks.defaultProps());
    }

    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityAerialInterface.class;
    }

    @Override
    public boolean isRotatable() {
        return true;
    }

    @Override
    protected boolean reversePlacementRotation() {
        return true;
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        PneumaticCraftUtils.getTileEntityAt(world, pos, TileEntityAerialInterface.class).ifPresent(teAI -> {
            if (entity instanceof PlayerEntity && !(entity instanceof FakePlayer)) {
                teAI.setPlayerId(entity.getUUID());
            }
        });

        super.setPlacedBy(world, pos, state, entity, stack);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side) {
        return PneumaticCraftUtils.getTileEntityAt(blockAccess, pos, TileEntityAerialInterface.class)
                .map(teAI -> teAI.getRedstoneController().shouldEmit() ? 15 : 0).orElse(0);
    }
}
