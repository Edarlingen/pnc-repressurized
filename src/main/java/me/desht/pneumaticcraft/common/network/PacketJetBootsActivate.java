package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.pneumatic_armor.ArmorUpgradeRegistry;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import me.desht.pneumaticcraft.common.pneumatic_armor.JetBootsStateTracker;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Received on: SERVER
 * Sent from client to tell the server the player is activating/deactivating jet boots.  Toggled when
 * Jump key is pressed or released.
 */
public class PacketJetBootsActivate {
    private final boolean state;

    public PacketJetBootsActivate(boolean state) {
        this.state = state;
    }

    PacketJetBootsActivate(PacketBuffer buffer) {
        state = buffer.readBoolean();
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeBoolean(state);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayerEntity player = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (ItemPneumaticArmor.isPneumaticArmorPiece(player, EquipmentSlotType.FEET)) {
                CommonArmorHandler handler = CommonArmorHandler.getHandlerForPlayer(player);
                JetBootsStateTracker.JetBootsState jbState = JetBootsStateTracker.getTracker(player).getJetBootsState(player);
                if (handler.getUpgradeCount(EquipmentSlotType.FEET, EnumUpgrade.JET_BOOTS) > 0
                        && (!state || jbState.isEnabled())) {
                    ArmorUpgradeRegistry.getInstance().jetBootsHandler.setJetBootsActive(handler, state);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
