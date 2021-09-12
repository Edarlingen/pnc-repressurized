package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.IAreaProvider;
import me.desht.pneumaticcraft.common.progwidgets.IGotoWidget;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidget;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import java.util.*;

public class DroneEntityAIGoToLocation extends Goal {
    protected final IDroneBase drone;
    private final ProgWidget gotoWidget;
    private final ChunkPositionSorter positionSorter;
    private final List<BlockPos> validArea;

    public DroneEntityAIGoToLocation(IDroneBase drone, ProgWidget gotoWidget) {
        this.drone = drone;
        setFlags(EnumSet.allOf(Flag.class)); // so it won't run along with other AI tasks.
        this.gotoWidget = gotoWidget;
        Set<BlockPos> set = new HashSet<>();
        ((IAreaProvider) gotoWidget).getArea(set);
        validArea = new ArrayList<>(set);
        positionSorter = new ChunkPositionSorter(drone);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
    public boolean canUse() {
        validArea.sort(positionSorter);
        for (BlockPos c : validArea) {
            // 0.75 is the squared dist from a block corner to its center (0.5^2 + 0.5^2 + 0.5^2)
            if (drone.getDronePos().distanceToSqr(new Vector3d(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5)) < 0.75)
                return false;
            if (drone.getPathNavigator().moveToXYZ(c.getX(), c.getY(), c.getZ())) {
                return !((IGotoWidget) gotoWidget).doneWhenDeparting();
            }
        }
        boolean teleport = drone.getPathNavigator().isGoingToTeleport();
        if (teleport) {
            return true;
        } else {
            for (BlockPos c : validArea) {
                drone.getDebugger().addEntry("pneumaticcraft.gui.progWidget.goto.debug.cantNavigate", c);
            }
            return false;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean canContinueToUse() {
        return !drone.getPathNavigator().hasNoPath();
    }
}
