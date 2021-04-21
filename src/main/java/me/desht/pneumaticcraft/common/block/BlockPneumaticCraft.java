package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.block.IPneumaticWrenchable;
import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.api.item.IUpgradeAcceptor;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.advancements.AdvancementTriggers;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.core.ModSounds;
import me.desht.pneumaticcraft.common.heat.TemperatureCategory;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketBlockDestroyed;
import me.desht.pneumaticcraft.common.network.PacketSpawnParticle;
import me.desht.pneumaticcraft.common.particle.AirParticleData;
import me.desht.pneumaticcraft.common.thirdparty.ModdedWrenchUtils;
import me.desht.pneumaticcraft.common.tileentity.IComparatorSupport;
import me.desht.pneumaticcraft.common.tileentity.IHeatExchangingTE;
import me.desht.pneumaticcraft.common.tileentity.TileEntityBase;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPneumaticBase;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import me.desht.pneumaticcraft.common.util.FluidUtils;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.util.UpgradableItemUtils;
import me.desht.pneumaticcraft.lib.NBTKeys;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;
import static net.minecraft.state.properties.BlockStateProperties.FACING;
import static net.minecraft.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public abstract class BlockPneumaticCraft extends Block implements IPneumaticWrenchable, IUpgradeAcceptor {
    static final VoxelShape ALMOST_FULL_SHAPE = Block.makeCuboidShape(0.5, 0, 0.5, 15.5, 16, 15.5);

    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty[] CONNECTION_PROPERTIES = new BooleanProperty[]{DOWN, UP, NORTH, SOUTH, WEST, EAST};
    public static final EnumProperty<TemperatureCategory> TEMPERATURE = EnumProperty.create("temperature", TemperatureCategory.class);

    protected BlockPneumaticCraft(Properties props) {
        super(props);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return getTileEntityClass() != null;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        try {
            return getTileEntityClass().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected abstract Class<? extends TileEntity> getTileEntityClass();

    @Override
    public String getUpgradeAcceptorTranslationKey() {
        return getTranslationKey();
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult brtr) {
        ItemStack heldItem = player.getHeldItem(hand);
        TileEntity te = world.getTileEntity(pos);
        if (player.isSneaking()
                || !(te instanceof INamedContainerProvider)
                || isRotatable() && (heldItem.getItem() == ModItems.MANOMETER.get() || ModdedWrenchUtils.getInstance().isModdedWrench(heldItem))
                || hand == Hand.OFF_HAND && ModdedWrenchUtils.getInstance().isModdedWrench(player.getHeldItemMainhand())) {
            return ActionResultType.PASS;
        } else {
            if (!world.isRemote) {
                if (te instanceof TileEntityBase) {
                    if (FluidUtils.tryFluidInsertion(te, null, player, hand)) {
                        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        return ActionResultType.SUCCESS;
                    } else if (FluidUtils.tryFluidExtraction(te, null, player, hand)) {
                        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        return ActionResultType.SUCCESS;
                    }
                    if (te instanceof INamedContainerProvider) {
                        NetworkHooks.openGui((ServerPlayerEntity) player, (INamedContainerProvider) te, pos);
                    }
                }
            }

            return ActionResultType.SUCCESS;
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state != null) {
            for (Direction facing : DirectionUtil.VALUES) {
                if (state.hasProperty(connectionProperty(facing))) {
                    // handle pneumatic connections to neighbouring air handlers
                    TileEntity te = ctx.getWorld().getTileEntity(ctx.getPos().offset(facing));
                    boolean b = te != null && te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, facing.getOpposite()).isPresent();
                    state = state.with(connectionProperty(facing), b);
                }
            }
            if (isRotatable()) {
                Direction f = canRotateToTopOrBottom() ? ctx.getNearestLookingDirection() : ctx.getPlacementHorizontalFacing();
                state = state.with(directionProperty(), reversePlacementRotation() ? f.getOpposite() : f);
            }
        }
        return state;
    }

    /**
     * Does the block face the same way as the player when placed, or opposite?
     * @return whether or not the block should be rotated 180 degrees on placement
     */
    protected boolean reversePlacementRotation() {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, entity, stack);

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityBase && stack.hasDisplayName()) {
            ((TileEntityBase) te).setCustomName(stack.getDisplayName());
        }
        if (te instanceof IHeatExchangingTE) {
            ((IHeatExchangingTE) te).initHeatExchangersOnPlacement(world, pos);
        }
    }

    public static BooleanProperty connectionProperty(Direction dir) {
        return CONNECTION_PROPERTIES[dir.getIndex()];
    }

    DirectionProperty directionProperty() { return canRotateToTopOrBottom() ? FACING : HORIZONTAL_FACING; }

    protected Direction getRotation(IBlockReader world, BlockPos pos) {
        return getRotation(world.getBlockState(pos));
    }

    public Direction getRotation(BlockState state) {
        return state.get(directionProperty());
    }

    protected void setRotation(World world, BlockPos pos, Direction rotation) {
        setRotation(world, pos, rotation, world.getBlockState(pos));
    }

    private void setRotation(World world, BlockPos pos, Direction rotation, BlockState state) {
        world.setBlockState(pos, state.with(directionProperty(), rotation));
    }

    public boolean isRotatable() {
        return false;
    }

    protected boolean canRotateToTopOrBottom() {
        return false;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        if (isRotatable()) {
            builder.add(directionProperty());
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        // this is here to support rotation when creating blocks from templates (jigsaw system)
        // - rotation by the pneumatic wrench is handled by onBlockWrenched() below
        // - rotation by 3rd party wrenches is captured by a client-side event handler, which sends
        //   a PacketModWrenchBlock to the server, also leading to onBlockWrenched()
        if (isRotatable()) {
            state = state.with(directionProperty(), rotation.rotate(state.get(directionProperty())));
        }

        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        // see above
        return isRotatable() ? state.rotate(mirrorIn.toRotation(state.get(directionProperty()))) : state;
    }

    @Override
    public boolean onWrenched(World world, PlayerEntity player, BlockPos pos, Direction side, Hand hand) {
        if (player != null && player.isSneaking()) {
            TileEntity te = world.getTileEntity(pos);
            boolean preserve = false;
            if (te instanceof TileEntityBase) {
                preserve = true;
                ((TileEntityBase) te).setPreserveStateOnBreak(true);
            }
            if (!player.isCreative() || preserve) {
                Block.spawnDrops(world.getBlockState(pos), world, pos, te);
            }
            removeBlockSneakWrenched(world, pos);
            return true;
        } else {
            if (isRotatable()) {
                BlockState state = world.getBlockState(pos);
                if (!rotateCustom(world, pos, state, side)) {
                    if (rotateForgeWay()) {
                        if (!canRotateToTopOrBottom()) side = Direction.UP;
                        if (getRotation(world, pos).getAxis() != side.getAxis()) {
                            setRotation(world, pos, DirectionUtil.rotateAround(getRotation(world, pos), side.getAxis()));
                        }
                    } else {
                        Direction f = getRotation(world, pos);
                        do {
                            f = Direction.byIndex(f.getIndex() + 1);
                        } while (!canRotateToTopOrBottom() && f.getAxis() == Axis.Y);
                        setRotation(world, pos, f);
                    }
                    TileEntity te = world.getTileEntity(pos);
                    if (te instanceof TileEntityBase) ((TileEntityBase) te).onBlockRotated();
                }
                return true;
            } else {
                return false;
            }
        }
    }

    protected boolean rotateForgeWay() {
        return true;
    }

    /**
     * Can be overridden to implement custom rotation behaviour for a block.
     *
     * @param world the world
     * @param pos block position
     * @param state block state
     * @param side the side clicked
     * @return true when the method is overridden, to disable default rotation behaviour
     */
    protected boolean rotateCustom(World world, BlockPos pos, BlockState state, Direction side) {
        return false;
    }

    @Override
    public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos tilePos) {
        if (world instanceof World && !((World) world).isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityBase) {
                ((TileEntityBase) te).onNeighborTileUpdate(tilePos);
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityBase) {
                ((TileEntityBase) te).onNeighborBlockUpdate(fromPos);
            }
        }
    }

    private int getSavedAir(ItemStack stack) {
        CompoundNBT tag = stack.getChildTag(NBTKeys.BLOCK_ENTITY_TAG);
        if (tag != null && tag.contains(NBTKeys.NBT_AIR_AMOUNT)) {
            return tag.getInt(NBTKeys.NBT_AIR_AMOUNT);
        } else {
            return 0;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, IBlockReader world, List<ITextComponent> curInfo, ITooltipFlag flag) {
        if (stack.hasTag()) {
            int savedAir = getSavedAir(stack);
            if (savedAir != 0) {
                curInfo.add(xlate("pneumaticcraft.gui.tooltip.air", Integer.toString(savedAir)).mergeStyle(TextFormatting.GREEN));
            }
            if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof IUpgradeAcceptor) {
                UpgradableItemUtils.addUpgradeInformation(stack, curInfo, flag);
            }
            CompoundNBT subTag = stack.getChildTag(NBTKeys.BLOCK_ENTITY_TAG);
            if (subTag != null && subTag.contains(NBTKeys.NBT_SAVED_TANKS, Constants.NBT.TAG_COMPOUND)) {
                CompoundNBT tag = subTag.getCompound(NBTKeys.NBT_SAVED_TANKS);
                for (String s : tag.keySet()) {
                    CompoundNBT tankTag = tag.getCompound(s);
                    FluidTank tank = new FluidTank(tankTag.getInt("Amount"));
                    tank.readFromNBT(tankTag);
                    FluidStack fluidStack = tank.getFluid();
                    if (!fluidStack.isEmpty()) {
                        curInfo.add(xlate("pneumaticcraft.gui.tooltip.fluid")
                                .appendString(fluidStack.getAmount() + "mB ")
                                .append(fluidStack.getDisplayName()).mergeStyle(TextFormatting.GREEN));
                    }
                }
            }
            addExtraInformation(stack, world, curInfo, flag);
        }
        if (ClientUtils.hasShiftDown() && hasTileEntity(getDefaultState())) {
            TileEntity te = createTileEntity(getDefaultState(), world);
            if (te instanceof TileEntityPneumaticBase) {
                float pressure = ((TileEntityPneumaticBase) te).dangerPressure;
                curInfo.add(xlate("pneumaticcraft.gui.tooltip.maxPressure", pressure).mergeStyle(TextFormatting.YELLOW));
            }
        }
    }

    protected void addExtraInformation(ItemStack stack, IBlockReader world, List<ITextComponent> curInfo, ITooltipFlag flag) {
        // override in subclasses
    }

    /**
     * If this returns true, then comparators facing away from this block will use the value from
     * getComparatorInputOverride instead of the actual redstone signal strength.
     */
    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        Class<? extends TileEntity> teClass = getTileEntityClass();
        return teClass != null && IComparatorSupport.class.isAssignableFrom(teClass);
    }

    /**
     * If hasComparatorInputOverride returns true, the return value from this is used instead of the redstone signal
     * strength when this block inputs to a comparator.
     */
    @Override
    public int getComparatorInputOverride(BlockState state, World world, BlockPos pos) {
        return ((IComparatorSupport) world.getTileEntity(pos)).getComparatorValue();
    }

    @Override
    public Map<EnumUpgrade, Integer> getApplicableUpgrades() {
        if (hasTileEntity(getDefaultState())) {
            TileEntity te = createTileEntity(getDefaultState(), null);
            if (te instanceof IUpgradeAcceptor) return ((IUpgradeAcceptor) te).getApplicableUpgrades();
        }
        return Collections.emptyMap();
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityBase) {
                NonNullList<ItemStack> drops = NonNullList.create();
                ((TileEntityBase) te).getContentsToDrop(drops);
                drops.forEach(stack -> PneumaticCraftUtils.dropItemOnGround(stack, world, pos));

                if (!((TileEntityBase) te).shouldPreserveStateOnBreak()) {
                    te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY).ifPresent(handler -> {
                        if (handler.getAir() > 0) {
                            NetworkHandler.sendToAllTracking(new PacketSpawnParticle(AirParticleData.DENSE, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 0, (int) (5 * handler.getPressure()), 1, 1, 1), world, pos);
                            world.playSound(null, pos, ModSounds.SHORT_HISS.get(), SoundCategory.BLOCKS, 0.3f, 0.8f);
                        }
                    });
                }

            }
            super.onReplaced(state, world, pos, newState, isMoving);
        }
    }

    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.hasProperty(connectionProperty(facing))) {
            TileEntity ourTE = worldIn.getTileEntity(currentPos);
            if (ourTE != null && ourTE.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, facing).isPresent()) {
                // handle pneumatic connections to neighbouring air handlers
                TileEntity te = worldIn.getTileEntity(currentPos.offset(facing));
                boolean b = te != null && te.getCapability (PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, facing.getOpposite()).isPresent();
                stateIn = stateIn.with(connectionProperty(facing), b);
                return stateIn;
            }
        }
        return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
        return getCollisionShape(state, worldIn, pos, ISelectionContext.dummy()).isEmpty();
    }

    static void removeBlockSneakWrenched(World world, BlockPos pos) {
        if (!world.isRemote()) {
            world.removeBlock(pos, false);
            // this only gets called server-side, but the client needs to be informed too, to update neighbour states
            NetworkHandler.sendToAllTracking(new PacketBlockDestroyed(pos), world, pos);
        }
    }

    @Override
    public void harvestBlock(World worldIn, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack stack) {
        if (player instanceof ServerPlayerEntity && !(player instanceof FakePlayer)
                && te instanceof TileEntityBase && !(((TileEntityBase) te).shouldPreserveStateOnBreak())) {
            AdvancementTriggers.MACHINE_VANDAL.trigger((ServerPlayerEntity) player);
        }
        super.harvestBlock(worldIn, player, pos, state, te, stack);
    }
}
