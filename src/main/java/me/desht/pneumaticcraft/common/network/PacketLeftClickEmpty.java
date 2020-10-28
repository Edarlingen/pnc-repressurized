package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.common.item.ILeftClickableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketLeftClickEmpty {
    public PacketLeftClickEmpty() {
    }

    public PacketLeftClickEmpty(PacketBuffer buf) {
        // nothing
    }

    public void toBytes(PacketBuffer buf) {
        // nothing
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getSender() != null) {
                ItemStack stack = ctx.get().getSender().getHeldItemMainhand();
                if (stack.getItem() instanceof ILeftClickableItem) {
                    ((ILeftClickableItem) stack.getItem()).onLeftClickEmpty(ctx.get().getSender());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
