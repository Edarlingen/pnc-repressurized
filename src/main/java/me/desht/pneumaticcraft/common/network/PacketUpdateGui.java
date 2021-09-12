package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.client.gui.GuiPneumaticContainerBase;
import me.desht.pneumaticcraft.common.inventory.ContainerPneumaticBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Received on: CLIENT
 *
 * The primary mechanism for sync'ing TE fields to an open GUI.  TE fields annotated with @GuiSynced will be synced
 * in this packet, via {@link ContainerPneumaticBase#detectAndSendChanges()}.
 */
public class PacketUpdateGui {
    private final int syncId;
    private final Object value;
    private final byte type;

    public PacketUpdateGui(int syncId, SyncedField<?> syncField) {
        this.syncId = syncId;
        value = syncField.getValue();
        type = SyncedField.getType(syncField);
    }

    public PacketUpdateGui(PacketBuffer buf) {
        syncId = buf.readVarInt();
        type = buf.readByte();
        value = SyncedField.fromBytes(buf, type);
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeVarInt(syncId);
        buf.writeByte(type);
        SyncedField.toBytes(buf, value, type);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof ContainerScreen) {
                Container container = ((ContainerScreen<?>) Minecraft.getInstance().screen).getMenu();
                if (container instanceof ContainerPneumaticBase) {
                    ((ContainerPneumaticBase<?>) container).updateField(syncId, value);
                }
                if (Minecraft.getInstance().screen instanceof GuiPneumaticContainerBase) {
                    ((GuiPneumaticContainerBase<?,?>) Minecraft.getInstance().screen).onGuiUpdate();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
