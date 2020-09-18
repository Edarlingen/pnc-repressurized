package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetInventoryBase;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class DroneEntityAIInventoryExport extends DroneAIImExBase<ProgWidgetInventoryBase> {

    public DroneEntityAIInventoryExport(IDroneBase drone, ProgWidgetInventoryBase widget) {
        super(drone, widget);
    }

    @Override
    protected boolean isValidPosition(BlockPos pos) {
        return export(pos, true);
    }

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
        return export(pos, false) && super.doBlockInteraction(pos, distToBlock);
    }

    private boolean export(BlockPos pos, boolean simulate) {
        TileEntity te = drone.world().getTileEntity(pos);
        if (te != null) {
            for (int i = 0; i < drone.getInv().getSlots(); i++) {
                ItemStack droneStack = drone.getInv().getStackInSlot(i);
                if (!droneStack.isEmpty()) {
                    if (progWidget.isItemValidForFilters(droneStack)) {
                        for (int side = 0; side < 6; side++) {
                            if (progWidget.getSides()[side]) {
                                droneStack = droneStack.copy();
                                int oldCount = droneStack.getCount();
                                if (progWidget.useCount()) {
                                    droneStack.setCount(Math.min(droneStack.getCount(), getRemainingCount()));
                                }
                                ItemStack remainder = IOHelper.insertStacked(te, droneStack.copy(), Direction.byIndex(side), simulate);
                                int stackSize = drone.getInv().getStackInSlot(i).getCount() - (remainder.isEmpty() ? droneStack.getCount() : droneStack.getCount() - remainder.getCount());
                                droneStack.setCount(stackSize);
                                int exportedItems = oldCount - stackSize;
                                if (!simulate) {
                                    drone.getInv().setStackInSlot(i, stackSize > 0 ? droneStack : ItemStack.EMPTY);
                                    decreaseCount(exportedItems);
                                }
                                if (simulate && exportedItems > 0) return true;
                            }
                        }
                        if (droneStack.isEmpty() && !simulate) {
                            drone.getCapability(PNCCapabilities.AIR_HANDLER_CAPABILITY)
                                    .ifPresent(h -> h.addAir(-PneumaticValues.DRONE_USAGE_INV));
                        }
                        else drone.addDebugEntry("pneumaticcraft.gui.progWidget.inventoryExport.debug.filledToMax", pos);
                    } else {
                        drone.addDebugEntry("pneumaticcraft.gui.progWidget.inventoryExport.debug.stackdoesntPassFilter", pos);
                    }
                }
            }
        } else {
            drone.addDebugEntry("pneumaticcraft.gui.progWidget.inventory.debug.noInventory", pos);
        }
        return false;
    }
}
