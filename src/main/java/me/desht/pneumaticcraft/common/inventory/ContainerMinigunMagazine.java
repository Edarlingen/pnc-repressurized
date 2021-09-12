package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.core.ModContainers;
import me.desht.pneumaticcraft.common.item.ItemMinigun;
import me.desht.pneumaticcraft.common.tileentity.TileEntityBase;
import me.desht.pneumaticcraft.common.util.NBTUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ContainerMinigunMagazine extends ContainerPneumaticBase<TileEntityBase> {
    private final ItemMinigun.MagazineHandler gunInv;
    private final Hand hand;

    public ContainerMinigunMagazine(int i, PlayerInventory playerInventory, @SuppressWarnings("unused") PacketBuffer buffer) {
        this(i, playerInventory, getHand(buffer));
    }

    public ContainerMinigunMagazine(int windowId, PlayerInventory playerInventory, Hand hand) {
        super(ModContainers.MINIGUN_MAGAZINE.get(), windowId, playerInventory);
        this.hand = hand;

        ItemMinigun minigun = (ItemMinigun) playerInventory.player.getItemInHand(hand).getItem();
        gunInv = minigun.getMagazine(playerInventory.player.getItemInHand(hand));
        for (int i = 0; i < gunInv.getSlots(); i++) {
            addSlot(new SlotItemHandler(gunInv, i, 26 + (i % 2) * 18, 26 + (i / 2) * 18));
        }

        addPlayerSlots(playerInventory, 84);
    }

    @Override
    public void removed(PlayerEntity playerIn) {
        super.removed(playerIn);

        gunInv.save();
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return true;
    }

    @Nonnull
    @Override
    public ItemStack clicked(int slotId, int dragType, ClickType clickType, PlayerEntity player) {
        if (clickType == ClickType.CLONE && dragType == 2 && slotId >= 0 && slotId < ItemMinigun.MAGAZINE_SIZE) {
            // middle-click to lock a slot
            ItemStack gunStack = player.getItemInHand(hand);
            if (gunStack.getItem() instanceof ItemMinigun) {
                int slot = ItemMinigun.getLockedSlot(gunStack);
                if (slot == slotId) {
                    NBTUtils.removeTag(gunStack, ItemMinigun.NBT_LOCKED_SLOT);
                } else {
                    NBTUtils.setInteger(gunStack, ItemMinigun.NBT_LOCKED_SLOT, slotId);
                }
                if (player.level.isClientSide) {
                    player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
            }
            return ItemStack.EMPTY;
        } else {
            return super.clicked(slotId, dragType, clickType, player);
        }
    }

    public Hand getHand() {
        return hand;
    }
}
