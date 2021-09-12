package me.desht.pneumaticcraft.api.lib;

public class NBTKeys {
    public static final String PNEUMATIC_HELMET_DEBUGGING_DRONE = "debuggingDrone";
    public static final String PNEUMATIC_HELMET_DEBUGGING_PC = "debuggingPC";

    // Saved on tile entities and also serialized to itemstacks
    public static final String NBT_UPGRADE_INVENTORY = "UpgradeInventory";
    public static final String NBT_AIR_AMOUNT = "AirAmount";
    public static final String NBT_SAVED_TANKS = "SavedTanks";
    public static final String NBT_HEAT_EXCHANGER = "HeatExchanger";
    public static final String NBT_AIR_HANDLER = "AirHandler";
    public static final String NBT_SIDE_CONFIG = "SideConfiguration";
    public static final String NBT_EXTRA = "ExtraData";
    public static final String NBT_REDSTONE_MODE = "redstoneMode";

    // Standard tag for saving an item inventory
    public static final String NBT_ITEM_INV = "Items";

    // this is the tag vanilla uses to serialize TE data onto dropped items
    public static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
    // this is the tag vanilla uses to serialize entity data onto items
    public static final String ENTITY_TAG = "EntityTag";
}
