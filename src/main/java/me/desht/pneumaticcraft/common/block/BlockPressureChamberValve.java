package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;
import me.desht.pneumaticcraft.common.advancements.AdvancementTriggers;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPressureChamberValve;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockPressureChamberValve extends BlockPneumaticCraft implements IBlockPressureChamber {

    public static final PropertyBool FORMED = PropertyBool.create("formed");

    public BlockPressureChamberValve() {
        super(Material.IRON, "pressure_chamber_valve");
        setResistance(2000.0f);
    }

    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityPressureChamberValve.class;
    }

    /**
     * Called when the block is placed in the world.
     */
    @Override
    public void onBlockPlacedBy(World par1World, BlockPos pos, BlockState state, LivingEntity par5EntityLiving, ItemStack iStack) {
        super.onBlockPlacedBy(par1World, pos, state, par5EntityLiving, iStack);
        if (TileEntityPressureChamberValve.checkIfProperlyFormed(par1World, pos) && par5EntityLiving instanceof ServerPlayerEntity) {
            AdvancementTriggers.PRESSURE_CHAMBER.trigger((ServerPlayerEntity) par5EntityLiving);
        }
    }

    @Override
    public boolean isRotatable() {
        return true;
    }

    @Override
    protected boolean canRotateToTopOrBottom() {
        return true;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ROTATION, FORMED);
    }

    @Override
    public BlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta).withProperty(FORMED, meta >= 6);
    }

    @Override
    public int getMetaFromState(BlockState state) {
        return super.getMetaFromState(state) + (state.getValue(FORMED) ? 6 : 0);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, BlockState state, PlayerEntity player, Hand hand, Direction side, float par7, float par8, float par9) {
        if (player.isSneaking()) return false;
        TileEntity te = world.getTileEntity(pos);
        if (!world.isRemote && te instanceof TileEntityPressureChamberValve) {
            if (((TileEntityPressureChamberValve) te).multiBlockSize > 0) {
                player.openGui(PneumaticCraftRepressurized.instance, EnumGuiId.PRESSURE_CHAMBER.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
            } else if (((TileEntityPressureChamberValve) te).accessoryValves.size() > 0) {
                // when this isn't the core valve, track down the core valve
                //  System.out.println("size: " + ((TileEntityPressureChamberValve)te).accessoryValves.size());
                for (TileEntityPressureChamberValve valve : ((TileEntityPressureChamberValve) te).accessoryValves) {
                    if (valve.multiBlockSize > 0) {
                        player.openGui(PneumaticCraftRepressurized.instance, EnumGuiId.PRESSURE_CHAMBER.ordinal(), world, valve.getPos().getX(), valve.getPos().getY(), valve.getPos().getZ());
                        break;
                    }
                }
            } else {
                return false;
            }
            return true;
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, BlockState state) {
        invalidateMultiBlock(world, pos);
        super.breakBlock(world, pos, state);
    }

    private void invalidateMultiBlock(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityPressureChamberValve && !world.isRemote) {
            if (((TileEntityPressureChamberValve) te).multiBlockSize > 0) {
                ((TileEntityPressureChamberValve) te).onMultiBlockBreak();
            } else if (((TileEntityPressureChamberValve) te).accessoryValves.size() > 0) {
                for (TileEntityPressureChamberValve valve : ((TileEntityPressureChamberValve) te).accessoryValves) {
                    if (valve.multiBlockSize > 0) {
                        valve.onMultiBlockBreak();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean isOpaqueCube(BlockState state) {
        return false;
    }
}
