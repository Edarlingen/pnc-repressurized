package me.desht.pneumaticcraft.common.event;

import me.desht.pneumaticcraft.common.tileentity.TileEntityUniversalSensor;
import me.desht.pneumaticcraft.common.util.GlobalTileEntityCacheManager;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandlerUniversalSensor {
    @SubscribeEvent
    public void onInteraction(PlayerInteractEvent event) {
        sendEventToSensors(event.getEntity().level, event);
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        sendEventToSensors(event.getEntity().level, event);
    }

    @SubscribeEvent
    public void onItemPickUp(EntityItemPickupEvent event) {
        sendEventToSensors(event.getEntity().level, event);
    }

    private void sendEventToSensors(World world, Event event) {
        if (!world.isClientSide) {
            for (TileEntityUniversalSensor sensor : GlobalTileEntityCacheManager.getInstance().universalSensors) {
                sensor.onEvent(event);
            }
        }
    }
}
