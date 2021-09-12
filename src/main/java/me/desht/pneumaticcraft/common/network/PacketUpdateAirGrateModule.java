package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.common.block.tubes.ModuleAirGrate;
import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;

/**
 * Received on: SERVER
 * Update the entity filter of an air grate module
 */
public class PacketUpdateAirGrateModule extends PacketUpdateTubeModule {
    private final String entityFilter;

    public PacketUpdateAirGrateModule(TubeModule module, String entityFilter) {
        super(module);
        this.entityFilter = entityFilter;
    }

    public PacketUpdateAirGrateModule(PacketBuffer buffer) {
        super(buffer);
        entityFilter = buffer.readUtf(32767);
    }

    @Override
    public void toBytes(PacketBuffer buffer) {
        super.toBytes(buffer);
        buffer.writeUtf(entityFilter);
    }

    @Override
    protected void onModuleUpdate(TubeModule module, PlayerEntity player) {
        if (module instanceof ModuleAirGrate && module.isUpgraded()) {
            ((ModuleAirGrate) module).setEntityFilter(entityFilter);
        }
    }
}
