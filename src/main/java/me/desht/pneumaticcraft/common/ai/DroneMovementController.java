package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.entity.living.EntityDroneBase;
import net.minecraft.entity.ai.controller.MovementController;

public class DroneMovementController extends MovementController {
    private final EntityDroneBase entity;
    private double x, y, z, speed;
    private int timeoutTimer;
    private int timeoutCounter;//counts the times the drone timed out.

    public DroneMovementController(EntityDroneBase par1EntityLiving) {
        super(par1EntityLiving);
        entity = par1EntityLiving;
        x = entity.getPosX();
        y = entity.getPosY();
        z = entity.getPosZ();
    }

    @Override
    public void setMoveTo(double x, double y, double z, double speed) {
        double newY = y + 0.5 - 0.17;
        if (x != this.x || newY != this.y || z != this.z) {
            this.x = x;
            this.y = newY;
            this.z = z;
            timeoutTimer = 0;
        } else {
            timeoutCounter = 0;
        }
        this.speed = speed;
    }

    @Override
    public void tick() {
        if (!(entity.getNavigator() instanceof EntityPathNavigateDrone)) {
            // this could be the case if the drone's path navigator has been replaced, e.g. if it's been picked
            // up by something, in which case just bail - nothing else to do here
            // https://github.com/TeamPneumatic/pnc-repressurized/issues/794
            return;
        }

        if (entity.isAccelerating()) {
            entity.setMotion(
                    Math.max(-speed, Math.min(speed, x - entity.getPosX())),
                    Math.max(-speed, Math.min(speed, y - entity.getPosY())),
                    Math.max(-speed, Math.min(speed, z - entity.getPosZ()))
            );

            EntityPathNavigateDrone navigator = (EntityPathNavigateDrone)entity.getNavigator();
            
            // When teleporting already, the drone stands still for a bit, so don't expect movement in this case.
            if (!navigator.isGoingToTeleport() && timeoutTimer++ > 40) {
                entity.getNavigator().clearPath();
                timeoutTimer = 0;
                timeoutCounter++;
                if (timeoutCounter > 1 && entity.hasPath()) {
                    // Teleport when after re-acquiring a new path, the drone still doesn't move.
                    navigator.teleport();
                }
            }
        }
    }

}
