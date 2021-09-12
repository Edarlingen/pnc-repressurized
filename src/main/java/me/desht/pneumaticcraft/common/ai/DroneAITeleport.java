package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidget;

public class DroneAITeleport extends DroneEntityAIGoToLocation {

    public DroneAITeleport(EntityDrone drone, ProgWidget gotoWidget) {
        super(drone, gotoWidget);
    }

    @Override
    public boolean canUse() {
        EntityPathNavigateDrone navigator = (EntityPathNavigateDrone) drone.getPathNavigator();
        navigator.setForceTeleport(true);
        boolean result = super.canUse();
        navigator.setForceTeleport(false);
        return result;
    }
}
