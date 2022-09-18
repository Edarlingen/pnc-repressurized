/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.common.block.entity.PressureTubeBlockEntity;
import me.desht.pneumaticcraft.common.tubemodules.AbstractTubeModule;
import me.desht.pneumaticcraft.common.tubemodules.INetworkedModule;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Received on: SERVER
 * Sent by client when logistics module colour is updated via GUI
 */
public class PacketTubeModuleColor extends LocationIntPacket {
    private final int ourColor;
    private final Direction side;

    public PacketTubeModuleColor(AbstractTubeModule module) {
        super(module.getTube().getBlockPos());

        this.ourColor = ((INetworkedModule) module).getColorChannel();
        this.side = module.getDirection();
    }

    PacketTubeModuleColor(FriendlyByteBuf buffer) {
        super(buffer);

        this.ourColor = buffer.readByte();
        this.side = Direction.from3DDataValue(buffer.readByte());
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);

        buf.writeByte(ourColor);
        buf.writeByte(side.get3DDataValue());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PneumaticCraftUtils.getTileEntityAt(player.level, pos, PressureTubeBlockEntity.class).ifPresent(tube -> {
                    if (tube.getModule(side) instanceof INetworkedModule net) {
                        net.setColorChannel(ourColor);
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
