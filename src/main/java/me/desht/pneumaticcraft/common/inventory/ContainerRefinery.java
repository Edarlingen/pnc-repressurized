package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.core.ModContainers;
import me.desht.pneumaticcraft.common.tileentity.TileEntityRefineryController;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public class ContainerRefinery extends ContainerPneumaticBase<TileEntityRefineryController> {

    public ContainerRefinery(int i, PlayerInventory playerInventory, PacketBuffer buffer) {
        this(i, playerInventory, getTilePos(buffer));
    }

    public ContainerRefinery(int i, PlayerInventory playerInventory, BlockPos pos) {
        super(ModContainers.REFINERY.get(), i, playerInventory, pos);

        TileEntityRefineryController refinery = te;
        refinery.onNeighborTileUpdate(null);
        while (refinery.getCachedNeighbor(Direction.UP) instanceof TileEntityRefineryController) {
            refinery = (TileEntityRefineryController) refinery.getCachedNeighbor(Direction.UP);
            addSyncedFields(refinery);
            refinery.onNeighborTileUpdate(null);
        }

        addPlayerSlots(playerInventory, 108);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return te.isGuiUseableByPlayer(player);
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(PlayerEntity par1EntityPlayer, int slotIndex) {
        // Refinery itself has no item slots, but this allows shift-clicking items between player's inventory & hotbar
        ItemStack stack = ItemStack.EMPTY;
        Slot srcSlot = slots.get(slotIndex);

        if (srcSlot != null && srcSlot.hasItem()) {
            ItemStack stackInSlot = srcSlot.getItem();
            stack = stackInSlot.copy();

            if (slotIndex < 27) {
                if (!moveItemStackTo(stackInSlot, 27, 36, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stackInSlot, 0, 27, false)) return ItemStack.EMPTY;
            }
            srcSlot.onQuickCraft(stackInSlot, stack);

            if (stackInSlot.isEmpty()) {
                srcSlot.set(ItemStack.EMPTY);
            } else {
                srcSlot.setChanged();
            }

            if (stackInSlot.getCount() == stack.getCount()) return ItemStack.EMPTY;

            srcSlot.onTake(par1EntityPlayer, stackInSlot);
        }

        return stack;
    }
}
