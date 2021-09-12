package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.api.crafting.recipe.RefineryRecipe;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import me.desht.pneumaticcraft.common.tileentity.TileEntityRefineryController;
import me.desht.pneumaticcraft.common.tileentity.TileEntityRefineryOutput;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.util.VoxelShapeUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.ItemHandlerHelper;

public class BlockRefineryOutput extends BlockPneumaticCraft {
    private static final VoxelShape SHAPE1 = box(0, 0, 4, 16, 16, 12);
    private static final VoxelShape SHAPE2 = box(3, 0, 0, 13, 16, 16);
    private static final VoxelShape SHAPE_EW = VoxelShapes.or(SHAPE1, SHAPE2);
    private static final VoxelShape SHAPE_NS = VoxelShapeUtils.rotateY(SHAPE_EW, 90);

    public BlockRefineryOutput() {
        super(ModBlocks.defaultProps());
    }

    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityRefineryOutput.class;
    }

    @Override
    public boolean isRotatable() {
        return true;
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult brtr) {
        return PneumaticCraftUtils.getTileEntityAt(world, pos, TileEntityRefineryOutput.class).map(te -> {
            // normally, activating any refinery block would open the controller TE's gui, but if we
            // activate with a fluid tank in hand (which can actually transfer fluid out),
            // then we must activate the actual refinery output that was clicked
            boolean canTransferFluid = FluidUtil.getFluidHandler(ItemHandlerHelper.copyStackWithSize(player.getItemInHand(hand), 1))
                    .map(heldHandler -> FluidUtil.getFluidHandler(world, pos, brtr.getDirection())
                            .map(refineryHandler -> couldTransferFluidOut(heldHandler, refineryHandler))
                            .orElse(false))
                    .orElse(false);
            if (canTransferFluid) {
                return super.use(state, world, pos, player, hand, brtr);
            } else if (!world.isClientSide) {
                TileEntityRefineryController master = te.getRefineryController();
                if (master != null) {
                    NetworkHooks.openGui((ServerPlayerEntity) player, master, master.getBlockPos());
                }
            }
            return ActionResultType.SUCCESS;
        }).orElse(ActionResultType.PASS);
    }

    private boolean couldTransferFluidOut(IFluidHandler h1, IFluidHandler h2) {
        FluidStack f = FluidUtil.tryFluidTransfer(h1, h2, 1000, false);
        return !f.isEmpty();
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return getRotation(state).getAxis() == Direction.Axis.X ? SHAPE_EW : SHAPE_NS;
    }

    @Override
    public boolean canSurvive(BlockState state, IWorldReader worldIn, BlockPos pos) {
        int nOutputs = 0;
        int up = 1, down = 1;
        while (worldIn.getBlockState(pos.above(up++)).getBlock() instanceof BlockRefineryOutput) {
            nOutputs++;
        }
        while (worldIn.getBlockState(pos.below(down++)).getBlock() instanceof BlockRefineryOutput) {
            nOutputs++;
        }
        return nOutputs < RefineryRecipe.MAX_OUTPUTS  && super.canSurvive(state, worldIn, pos);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (!worldIn.isClientSide() && facingState.getBlock() == ModBlocks.REFINERY_OUTPUT.get()) {
            recache(worldIn, currentPos);
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    private void recache(IWorld world, BlockPos pos) {
        PneumaticCraftUtils.getTileEntityAt(world, pos, TileEntityRefineryOutput.class).ifPresent(te -> {
            TileEntityRefineryController teC = te.getRefineryController();
            if (teC != null) teC.cacheRefineryOutputs();
        });
    }
}
