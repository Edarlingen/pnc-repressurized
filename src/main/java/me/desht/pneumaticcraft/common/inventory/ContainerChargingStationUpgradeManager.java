package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.common.core.ModContainers;
import me.desht.pneumaticcraft.common.inventory.handler.ChargeableItemHandler;
import me.desht.pneumaticcraft.common.tileentity.TileEntityChargingStation;
import me.desht.pneumaticcraft.common.util.upgrade.ApplicableUpgradesDB;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ContainerChargingStationUpgradeManager extends ContainerPneumaticBase<TileEntityChargingStation> {

    private ContainerChargingStationUpgradeManager(ContainerType type, int windowId, PlayerInventory inv, PacketBuffer data) {
        this(type, windowId, inv, getTilePos(data));
    }

    public ContainerChargingStationUpgradeManager(ContainerType type, int windowId, PlayerInventory inventoryPlayer, BlockPos pos) {
        super(type, windowId, inventoryPlayer, pos);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new UpgradeSlot(te, i * 3 + j, 44 + j * 18, 34 + i * 18));
            }
        }

        addPlayerSlots(inventoryPlayer, 100);
    }

    public static ContainerChargingStationUpgradeManager createMinigunContainer(int windowId, PlayerInventory inv, PacketBuffer data) {
        return new ContainerChargingStationUpgradeManager(ModContainers.CHARGING_MINIGUN.get(), windowId, inv, data);
    }

    public static ContainerChargingStationUpgradeManager createDroneContainer(int windowId, PlayerInventory inv, PacketBuffer data) {
        return new ContainerChargingStationUpgradeManager(ModContainers.CHARGING_DRONE.get(), windowId, inv, data);
    }

    public static ContainerChargingStationUpgradeManager createArmorContainer(int windowId, PlayerInventory inv, PacketBuffer data) {
        return new ContainerChargingStationUpgradeManager(ModContainers.CHARGING_ARMOR.get(), windowId, inv, data);
    }

    public static ContainerChargingStationUpgradeManager createJackhammerContainer(int windowId, PlayerInventory inv, PacketBuffer data) {
        return new ContainerChargingStationUpgradeManager(ModContainers.CHARGING_JACKHAMMER.get(), windowId, inv, data);
    }

    private class UpgradeSlot extends SlotItemHandler {
        UpgradeSlot(TileEntityChargingStation te, int slotIndex, int posX, int posY) {
            super(te.getChargeableInventory(), slotIndex, posX, posY);
        }

        @Override
        public int getMaxStackSize(@Nonnull ItemStack stack) {
            return ApplicableUpgradesDB.getInstance().getMaxUpgrades(te.getChargingStack().getItem(), EnumUpgrade.from(stack));
        }

        @Override
        public void setChanged() {
            super.setChanged();
            ((ChargeableItemHandler) getItemHandler()).writeToNBT();
        }
    }
}
