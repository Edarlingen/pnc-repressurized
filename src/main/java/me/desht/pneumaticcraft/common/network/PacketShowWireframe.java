package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketShowWireframe extends LocationIntPacket {
    private final int entityId;

    public PacketShowWireframe(EntityDrone entity, BlockPos pos) {
        super(pos);
        entityId = entity.getId();
    }

    public PacketShowWireframe(PacketBuffer buffer) {
        super(buffer);
        entityId = buffer.readInt();
    }

    @Override
    public void toBytes(PacketBuffer buffer) {
        super.toBytes(buffer);
        buffer.writeInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity ent = ctx.get().getSender().level.getEntity(entityId);
            if (ent instanceof EntityDrone) {
                ClientUtils.addDroneToHudHandler((EntityDrone) ent, pos);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
