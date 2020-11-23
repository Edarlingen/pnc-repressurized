package me.desht.pneumaticcraft.common.entity;

import me.desht.pneumaticcraft.common.entity.living.EntityDroneBase;
import me.desht.pneumaticcraft.common.tileentity.TileEntityProgrammableController;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Special client-only entity used for rendering the programmable controller's "minidrone".
 */
public class EntityProgrammableController extends EntityDroneBase {
    private TileEntityProgrammableController controller;
    private float propSpeed = 0f;

    public EntityProgrammableController(EntityType<EntityProgrammableController> type, World world) {
        super(type, world);

        this.preventEntitySpawning = false;
    }

    public void setController(TileEntityProgrammableController controller) {
        this.controller = controller;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public void tick() {
        if (world.isRemote && controller != null) {
            TileEntity te = world.getTileEntity(controller.getPos());
            if (te != controller) {
                // expire stale minidrones
                remove();
            } else {
                if (controller.isIdle) {
                    propSpeed = Math.max(0, propSpeed - 0.04F);
                } else {
                    propSpeed = Math.min(1, propSpeed + 0.04F);
                }
                oldPropRotation = propRotation;
                propRotation += propSpeed;
            }
        }
    }

    @Override
    public double getLaserOffsetY() {
        return 0.45;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    @Override
    public BlockPos getDugBlock() {
        return controller == null ? null : controller.getDugPosition();
    }

    @Override
    public ItemStack getDroneHeldItem() {
        return controller == null ? ItemStack.EMPTY : controller.getFakePlayer().getHeldItemMainhand();
    }
}
