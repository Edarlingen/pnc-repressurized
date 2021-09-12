package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.tileentity.IGUIButtonSensitive;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Received on: SERVER
 * Sent when a GUI button is clicked.
 */
public class PacketGuiButton {
    private final String tag;
    private final boolean shiftHeld;

    public PacketGuiButton(String tag) {
        this.tag = tag;
        this.shiftHeld = ClientUtils.hasShiftDown();
    }

    public PacketGuiButton(PacketBuffer buffer) {
        tag = buffer.readUtf(1024);
        shiftHeld = buffer.readBoolean();
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeUtf(tag);
        buffer.writeBoolean(shiftHeld);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof IGUIButtonSensitive) {
                ((IGUIButtonSensitive) player.containerMenu).handleGUIButtonPress(tag, shiftHeld, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
