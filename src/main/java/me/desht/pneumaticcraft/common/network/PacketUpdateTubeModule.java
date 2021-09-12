package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPressureTube;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Received on: SERVER
 * Sent by client when tube module settings are updated via GUI.
 */
public abstract class PacketUpdateTubeModule extends LocationIntPacket {
    private final Direction moduleSide;

    public PacketUpdateTubeModule(TubeModule module) {
        super(module.getTube().getBlockPos());
        this.moduleSide = module.getDirection();
    }

    public PacketUpdateTubeModule(PacketBuffer buffer) {
        super(buffer);
        this.moduleSide = Direction.from3DDataValue(buffer.readByte());
    }

    @Override
    public void toBytes(PacketBuffer buffer) {
        super.toBytes(buffer);
        buffer.writeByte((byte) moduleSide.get3DDataValue());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEntity player = ctx.get().getSender();
            if (player != null) {
                PneumaticCraftUtils.getTileEntityAt(player.getCommandSenderWorld(), pos, TileEntityPressureTube.class).ifPresent(te -> {
                    TubeModule tm = te.getModule(moduleSide);
                    if (tm != null && PneumaticCraftUtils.canPlayerReach(player, te.getBlockPos())) {
                        onModuleUpdate(tm, player);
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }

    protected abstract void onModuleUpdate(TubeModule module, PlayerEntity player);

}
