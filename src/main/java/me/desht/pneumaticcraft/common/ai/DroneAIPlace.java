package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetAreaItemBase;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.pathfinding.PathType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;

public class DroneAIPlace<W extends ProgWidgetAreaItemBase /*& IBlockOrdered & ISidedWidget*/> extends DroneAIBlockInteraction<W> {
    /**
     * @param drone the drone
     * @param widget needs to implement IBlockOrdered as well as ProgWidgetAreaItemBase
     */
    public DroneAIPlace(IDroneBase drone, W widget) {
        super(drone, widget);
    }

    @Override
    protected boolean respectClaims() {
        return true;
    }

    @Override
    protected boolean isValidPosition(BlockPos pos) {
        if (drone.world().getBlockState(pos).getMaterial().isReplaceable()) {
            if (Vector3d.atCenterOf(pos).distanceToSqr(drone.getDronePos()) < 1.2) {
                // too close - placement could be blocked by the drone
                return false;
            }
            boolean failedOnPlacement = false;
            for (int i = 0; i < drone.getInv().getSlots(); i++) {
                ItemStack droneStack = drone.getInv().getStackInSlot(i);
                if (droneStack.getItem() instanceof BlockItem && progWidget.isItemValidForFilters(droneStack)) {
                    BlockPos placerPos = findClearSide(pos);
                    if (placerPos == null) {
                        drone.getDebugger().addEntry("pneumaticcraft.gui.progWidget.place.debug.noClearSides", pos);
                        failedOnPlacement = true;
                        break;
                    }
                    Block placingBlock = ((BlockItem) droneStack.getItem()).getBlock();
                    BlockState state = placingBlock.getStateForPlacement(getPlacementContext(placerPos, pos, droneStack));
                    if (state == null) {
                        drone.getDebugger().addEntry("pneumaticcraft.gui.progWidget.place.debug.cantPlaceBlock", pos);
                        failedOnPlacement = true;
                    } else if (worldCache.isUnobstructed(null, state.getShape(drone.world(), pos))) {
                        if (state.canSurvive(drone.world(), pos)) {
                            return true;
                        } else {
                            drone.getDebugger().addEntry("pneumaticcraft.gui.progWidget.place.debug.cantPlaceBlock", pos);
                            failedOnPlacement = true;
                        }
                    } else {
                        drone.getDebugger().addEntry("pneumaticcraft.gui.progWidget.place.debug.entityInWay", pos);
                        failedOnPlacement = true;
                    }
                }
            }
            if (!failedOnPlacement) abort();
        }
        return false;
    }

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double squareDistToBlock) {
        if (squareDistToBlock < 2 * 2) {
            for (int slot = 0; slot < drone.getInv().getSlots(); slot++) {
                ItemStack droneStack = drone.getInv().getStackInSlot(slot);
                if (droneStack.getItem() instanceof BlockItem && progWidget.isItemValidForFilters(droneStack) && worldCache.getBlockState(pos).getMaterial().isReplaceable()) {
                    BlockItem blockItem = (BlockItem) droneStack.getItem();
                    BlockItemUseContext ctx = getPlacementContext(pos, pos, droneStack);
                    if (progWidget.getCachedAreaSet().contains(ctx.getClickedPos())) {
                        ActionResultType res = blockItem.place(ctx);
                        if (res.consumesAction()) {
                            drone.addAirToDrone(-PneumaticValues.DRONE_USAGE_PLACE);
                            if (slot == 0 && drone.getInv().getStackInSlot(slot).isEmpty()) {
                                // kludge to force update of visible held item
                                drone.getInv().setStackInSlot(slot, ItemStack.EMPTY);
                            }
                            return false;
                        }
                    }
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private BlockPos findClearSide(BlockPos pos) {
        for (Direction side : DirectionUtil.VALUES) {
            BlockPos pos2 = pos.relative(side);
            if (drone.world().getBlockState(pos.relative(side)).isPathfindable(drone.world(), pos2, PathType.AIR)) {
                return pos2;
            }
        }
        return null;
    }

    private BlockItemUseContext getPlacementContext(BlockPos placerPos, BlockPos targetPos, ItemStack droneStack) {
        BlockRayTraceResult brtr = drone.world().clip(new RayTraceContext(
                Vector3d.atCenterOf(placerPos),
                Vector3d.atCenterOf(targetPos),
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE,
                drone.getFakePlayer()
        ));
        return new BlockItemUseContext(new DroneBlockItemUseContext(drone.getFakePlayer(), droneStack, brtr));
    }

    private static class DroneBlockItemUseContext extends ItemUseContext {
        protected DroneBlockItemUseContext(@Nullable PlayerEntity droneFakePlayer, ItemStack heldItem, BlockRayTraceResult rayTraceResultIn) {
            super(droneFakePlayer.level, droneFakePlayer, Hand.MAIN_HAND, heldItem, rayTraceResultIn);
        }
    }
}
