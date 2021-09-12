package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.client.pneumatic_armor.ArmorUpgradeClientRegistry;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.RenderBlockTarget;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler.BlockTrackerClientHandler;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.hacking.WorldAndCoord;
import me.desht.pneumaticcraft.common.pneumatic_armor.ArmorUpgradeRegistry;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Received on: BOTH
 * Sent by client when player initiates a hack, and from server back to client to confirm initiation
 */
public class PacketHackingBlockStart extends LocationIntPacket {
    public PacketHackingBlockStart(BlockPos pos) {
        super(pos);
    }

    public PacketHackingBlockStart(PacketBuffer buffer) {
        super(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) {
                // client
                PlayerEntity cPlayer = ClientUtils.getClientPlayer();
                CommonArmorHandler.getHandlerForPlayer()
                        .getExtensionData(ArmorUpgradeRegistry.getInstance().hackHandler)
                        .setHackedBlockPos(new WorldAndCoord(cPlayer.level, pos));

                RenderBlockTarget target = ArmorUpgradeClientRegistry.getInstance()
                        .getClientHandler(ArmorUpgradeRegistry.getInstance().blockTrackerHandler, BlockTrackerClientHandler.class)
                        .getTargetForCoord(pos);
                if (target != null) target.onHackConfirmServer();
            } else {
                // server
                CommonArmorHandler handler = CommonArmorHandler.getHandlerForPlayer(player);
                if (handler.upgradeUsable(ArmorUpgradeRegistry.getInstance().blockTrackerHandler, true)) {
                    handler.getExtensionData(ArmorUpgradeRegistry.getInstance().hackHandler)
                            .setHackedBlockPos(new WorldAndCoord(player.level, pos));
                    NetworkHandler.sendToAllTracking(this, player.level, player.blockPosition());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
