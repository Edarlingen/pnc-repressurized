package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.core.ModContainers;
import me.desht.pneumaticcraft.common.tileentity.TileEntityKeroseneLamp;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public class ContainerKeroseneLamp extends ContainerPneumaticBase<TileEntityKeroseneLamp> {

    public ContainerKeroseneLamp(int i, PlayerInventory playerInventory, PacketBuffer buffer) {
        this(i, playerInventory, getTilePos(buffer));
    }

    public ContainerKeroseneLamp(int i, PlayerInventory playerInventory, BlockPos pos) {
        super(ModContainers.KEROSENE_LAMP.get(), i, playerInventory, pos);

        addSlot(new SlotFluidContainer(te.getPrimaryInventory(), 0, 132, 22));
        addSlot(new SlotOutput(te.getPrimaryInventory(), 1, 132, 55));

        addPlayerSlots(playerInventory, 84);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return te.isGuiUseableByPlayer(player);
    }
}
