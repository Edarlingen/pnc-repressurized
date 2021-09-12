package me.desht.pneumaticcraft.common.block.tubes;

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.common.item.ItemTubeModule;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdatePressureBlock;

public class ModulePressureGauge extends TubeModuleRedstoneEmitting {
    public ModulePressureGauge(ItemTubeModule item) {
        super(item);
        lowerBound = 0;
        higherBound = 7.5F;
    }

    @Override
    public void update() {
        super.update();

        if (!pressureTube.getLevel().isClientSide) {
            pressureTube.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY).ifPresent(h -> {
                if (pressureTube.getLevel().getGameTime() % 20 == 0)
                    NetworkHandler.sendToAllTracking(new PacketUpdatePressureBlock(getTube(), null, h.getSideLeaking(), h.getAir()), getTube());
                if (setRedstone(getRedstone(h.getPressure()))) {
                    // force a recalc on next tick
                    pressureTube.tubeModules()
                            .filter(tm -> tm instanceof ModuleRedstone)
                            .forEach(tm -> ((ModuleRedstone) tm).setInputLevel(-1));
                }
            });
        }
    }

    private int getRedstone(float pressure) {
        return (int) ((pressure - lowerBound) / (higherBound - lowerBound) * 15);
    }

    @Override
    public double getWidth() {
        return 8D;
    }

    @Override
    protected double getHeight() {
        return 4D;
    }

    @Override
    public boolean hasGui() {
        return upgraded;
    }
}
