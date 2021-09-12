package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.api.pneumatic_armor.IArmorUpgradeHandler;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.pneumatic_armor.ArmorUpgradeRegistry;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import me.desht.pneumaticcraft.common.util.NBTUtils;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

/**
 * Received on: SERVER
 * General packet for updating various pneumatic armor settings from client GUI
 */
public class PacketUpdateArmorExtraData {
    private static final List<Map<String, Integer>> VALID_KEYS = new ArrayList<>();
    private final ResourceLocation upgradeID;

    private static void addKey(EquipmentSlotType slot, String key, int nbtType) {
        VALID_KEYS.get(slot.getIndex()).put(key, nbtType);
    }

    static {
        Arrays.stream(ArmorUpgradeRegistry.ARMOR_SLOTS)
                .<Map<String, Integer>>map(slot -> new HashMap<>())
                .forEach(VALID_KEYS::add);
        addKey(EquipmentSlotType.HEAD, ItemPneumaticArmor.NBT_ENTITY_FILTER, NBT.TAG_STRING);
        addKey(EquipmentSlotType.HEAD, ItemPneumaticArmor.NBT_COORD_TRACKER, NBT.TAG_COMPOUND);
        addKey(EquipmentSlotType.LEGS, ItemPneumaticArmor.NBT_SPEED_BOOST, NBT.TAG_INT);
        addKey(EquipmentSlotType.LEGS, ItemPneumaticArmor.NBT_JUMP_BOOST, NBT.TAG_INT);
        addKey(EquipmentSlotType.FEET, ItemPneumaticArmor.NBT_BUILDER_MODE, NBT.TAG_BYTE);
        addKey(EquipmentSlotType.FEET, ItemPneumaticArmor.NBT_JET_BOOTS_POWER, NBT.TAG_INT);
        addKey(EquipmentSlotType.FEET, ItemPneumaticArmor.NBT_FLIGHT_STABILIZERS, NBT.TAG_BYTE);
        addKey(EquipmentSlotType.FEET, ItemPneumaticArmor.NBT_SMART_HOVER, NBT.TAG_BYTE);
    }

    private final EquipmentSlotType slot;
    private final CompoundNBT data;

    public PacketUpdateArmorExtraData(EquipmentSlotType slot, CompoundNBT data, ResourceLocation upgradeID) {
        this.slot = slot;
        this.data = data;
        this.upgradeID = upgradeID;
    }

    PacketUpdateArmorExtraData(PacketBuffer buffer) {
        slot = EquipmentSlotType.values()[buffer.readByte()];
        data = buffer.readNbt();
        upgradeID = buffer.readResourceLocation();
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeByte(slot.ordinal());
        buf.writeNbt(data);
        buf.writeResourceLocation(upgradeID);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof ItemPneumaticArmor) {
                CommonArmorHandler handler = CommonArmorHandler.getHandlerForPlayer(player);
                NBTUtils.initNBTTagCompound(stack);
                for (String key : data.getAllKeys()) {
                    INBT dataTag = data.get(key);
                    if (isKeyOKForSlot(key, slot, dataTag.getId())) {
                        stack.getTag().put(key, dataTag);
                        IArmorUpgradeHandler<?> upgradeHandler = ArmorUpgradeRegistry.getInstance().getUpgradeEntry(upgradeID);
                        if (upgradeHandler != null) {
                            upgradeHandler.onDataFieldUpdated(handler, key, dataTag);
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean isKeyOKForSlot(String key, EquipmentSlotType slot, int nbtType) {
        return VALID_KEYS.get(slot.getIndex()).get(key) == nbtType;
    }
}
