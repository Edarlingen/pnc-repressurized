package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.common.inventory.ContainerAmadron;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Received on: SERVER
 * Sent from client when an offer widget is clicked in the Amadron GUI to update the server-side order amount
 */
public class PacketAmadronOrderUpdate {
    private final int orderId;
    private final int mouseButton;
    private final boolean sneaking;

    public PacketAmadronOrderUpdate(int orderId, int mouseButton, boolean sneaking) {
        this.orderId = orderId;
        this.mouseButton = mouseButton;
        this.sneaking = sneaking;
    }

    public PacketAmadronOrderUpdate(PacketBuffer buffer) {
        orderId = buffer.readVarInt();
        mouseButton = buffer.readByte();
        sneaking = buffer.readBoolean();
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeVarInt(orderId);
        buf.writeByte(mouseButton);
        buf.writeBoolean(sneaking);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof ContainerAmadron) {
                ((ContainerAmadron) player.containerMenu).clickOffer(orderId, mouseButton, sneaking, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
