package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.api.item.IProgrammable;
import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.core.ModContainers;
import me.desht.pneumaticcraft.common.network.PacketSendNBTPacket;
import me.desht.pneumaticcraft.common.tileentity.TileEntityProgrammer;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ContainerProgrammer extends ContainerPneumaticBase<TileEntityProgrammer> {

    private final boolean hiRes;

    public ContainerProgrammer(int i, PlayerInventory playerInventory, BlockPos pos) {
        super(ModContainers.PROGRAMMER.get(), i, playerInventory, pos);

        // server side doesn't care about slot positioning, so doesn't care about screen res either
        this.hiRes = playerInventory.player.level.isClientSide && ClientUtils.isScreenHiRes();
        int xBase = hiRes ? 270 : 95;
        int yBase = hiRes ? 430 : 174;

        addSlot(new SlotItemHandler(te.getPrimaryInventory(), 0, hiRes ? 676 : 326, 15) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return isProgrammableItem(stack);
            }
        });

        // Add the player's inventory slots to the container
        addPlayerSlots(playerInventory, xBase, yBase);
    }

    public ContainerProgrammer(int i, PlayerInventory playerInventory, PacketBuffer buffer) {
        this(i, playerInventory, getTilePos(buffer));
    }

    public boolean isHiRes() {
        return hiRes;
    }

    private static boolean isProgrammableItem(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof IProgrammable && ((IProgrammable) stack.getItem()).canProgram(stack);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // update the client about contents of adjacent inventories so the programmer GUI knows what
        // puzzle pieces are available
        if (te.getLevel().getGameTime() % 20 == 0) {
            for (Direction d : DirectionUtil.VALUES) {
                TileEntity neighbor = te.getCachedNeighbor(d);
                if (neighbor != null && neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, d.getOpposite()).isPresent()) {
                    sendToContainerListeners(new PacketSendNBTPacket(neighbor));
                }
            }
        }
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(PlayerEntity par1EntityPlayer, int slotIndex) {
        ItemStack stack = ItemStack.EMPTY;
        Slot srcSlot = slots.get(slotIndex);

        if (srcSlot != null && srcSlot.hasItem()) {
            ItemStack stackInSlot = srcSlot.getItem();
            stack = stackInSlot.copy();

            if (slotIndex == 0) {
                if (!moveItemStackTo(stackInSlot, 1, 36, false)) return ItemStack.EMPTY;
                srcSlot.onQuickCraft(stackInSlot, stack);
            } else if (isProgrammableItem(stack)) {
                if (!moveItemStackTo(stackInSlot, 0, 1, false)) return ItemStack.EMPTY;
                srcSlot.onQuickCraft(stackInSlot, stack);
            }
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

    @Override
    public void removed(PlayerEntity playerIn) {
        super.removed(playerIn);

        if (playerIn.level.isClientSide) GuiProgrammer.onCloseFromContainer();
    }
}
