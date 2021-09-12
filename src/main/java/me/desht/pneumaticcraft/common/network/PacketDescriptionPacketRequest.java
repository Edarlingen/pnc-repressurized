package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.common.pneumatic_armor.ArmorUpgradeRegistry;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Received on: SERVER
 * Sent by client to ask the server for more info about a block, for Pneumatic Helmet purposes
 * TODO: replace with a more formal data request protocol
 */
public class PacketDescriptionPacketRequest extends LocationIntPacket {
    public PacketDescriptionPacketRequest(BlockPos pos) {
        super(pos);
    }

    public PacketDescriptionPacketRequest(PacketBuffer buffer) {
        super(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CommonArmorHandler handler = CommonArmorHandler.getHandlerForPlayer(ctx.get().getSender());
            if (handler.upgradeUsable(ArmorUpgradeRegistry.getInstance().blockTrackerHandler, true)) {
                TileEntity te = ctx.get().getSender().level.getBlockEntity(pos);
                if (te != null) {
                    forceLootGeneration(te);
                    NetworkHandler.sendToPlayer(new PacketSendNBTPacket(te), ctx.get().getSender());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * Force loot generation, as this is required on the client side to peek inside inventories.
     * The client is not able to generate the loot.
     * @param te the tile entity
     */
    private void forceLootGeneration(TileEntity te){
        if(te instanceof LockableLootTileEntity){
            LockableLootTileEntity teLoot = (LockableLootTileEntity)te;
            teLoot.unpackLootTable(null);
        }
    }
}