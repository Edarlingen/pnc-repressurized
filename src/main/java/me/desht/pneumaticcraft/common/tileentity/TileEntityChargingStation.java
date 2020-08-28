package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.common.block.BlockChargingStation;
import me.desht.pneumaticcraft.common.core.ModTileEntities;
import me.desht.pneumaticcraft.common.inventory.ContainerChargingStation;
import me.desht.pneumaticcraft.common.inventory.ContainerChargingStationItemInventory;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.inventory.handler.ChargeableItemHandler;
import me.desht.pneumaticcraft.common.item.IChargeableContainerProvider;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.util.GlobalTileEntityCacheManager;
import me.desht.pneumaticcraft.lib.NBTKeys;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileEntityChargingStation extends TileEntityPneumaticBase implements IRedstoneControl, ICamouflageableTE, INamedContainerProvider {
    private static final List<String> REDSTONE_LABELS = ImmutableList.of(
            "pneumaticcraft.gui.tab.redstoneBehaviour.button.never",
            "pneumaticcraft.gui.tab.redstoneBehaviour.chargingStation.button.doneDischarging",
            "pneumaticcraft.gui.tab.redstoneBehaviour.chargingStation.button.charging",
            "pneumaticcraft.gui.tab.redstoneBehaviour.chargingStation.button.discharging"
    );
    private static final int INVENTORY_SIZE = 1;
    public static final int CHARGE_INVENTORY_INDEX = 0;
    private static final int MAX_REDSTONE_UPDATE_FREQ = 10;  // in ticks; used to reduce lag from rapid updates

    @DescSynced
    private ItemStack chargingStackSynced = ItemStack.EMPTY;  // the item being charged, minus any nbt - for client display purposes

    private ChargingStationHandler itemHandler;  // holds the item being charged
    private final LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> itemHandler);

    private ChargeableItemHandler chargeableInventory;  // inventory of the item being charged

    @GuiSynced
    public float chargingItemPressure;
    @GuiSynced
    public boolean charging;
    @GuiSynced
    public boolean discharging;
    @GuiSynced
    public int redstoneMode;
    private boolean oldRedstoneStatus;
    private BlockState camoState;
    private long lastRedstoneUpdate;
    private int pendingRedstoneStatus = -1;

    public TileEntityChargingStation() {
        super(ModTileEntities.CHARGING_STATION.get(), PneumaticValues.DANGER_PRESSURE_CHARGING_STATION, PneumaticValues.MAX_PRESSURE_CHARGING_STATION, PneumaticValues.VOLUME_CHARGING_STATION, 4);
        itemHandler = new ChargingStationHandler();
    }

    @Nonnull
    public ItemStack getChargingStack() {
        return itemHandler.getStackInSlot(CHARGE_INVENTORY_INDEX);
    }

    @Nonnull
    public ItemStack getChargingStackSynced() { return chargingStackSynced; }

    @Override
    public void tick() {
        super.tick();

        if (!world.isRemote) {
            discharging = false;
            charging = false;

            chargingStackSynced = itemHandler.getStackInSlot(CHARGE_INVENTORY_INDEX);

            int airToTransfer = (int) (PneumaticValues.CHARGING_STATION_CHARGE_RATE * getSpeedMultiplierFromUpgrades());

            for (IAirHandler itemAirHandler : findChargeable()) {
                float itemPressure = itemAirHandler.getPressure();
                float itemVolume = itemAirHandler.getVolume();
                float delta = Math.abs(getPressure() - itemPressure) / 2.0F;
                int airInItem = (int) (itemPressure * itemVolume);

                if (itemPressure > getPressure() + 0.01F && itemPressure > 0F) {
                    // move air from item to charger
                    int airToMove = Math.min(Math.min(airToTransfer, airInItem), (int) (delta * airHandler.getVolume()));
                    itemAirHandler.addAir(-airToMove);
                    this.addAir(airToMove);
                    discharging = true;
                } else if (itemPressure < getPressure() - 0.01F && itemPressure < itemAirHandler.maxPressure()) {
                    // move air from charger to item
                    int maxAirInItem = (int) (itemAirHandler.maxPressure() * itemVolume);
                    int airToMove = Math.min(Math.min(airToTransfer, airHandler.getAir()), maxAirInItem - airInItem);
                    airToMove = Math.min((int) (delta * itemVolume), airToMove);
                    itemAirHandler.addAir(airToMove);
                    this.addAir(-airToMove);
                    charging = true;
                }
            }

            if (oldRedstoneStatus != shouldEmitRedstone()) {
                if (world.getGameTime() - lastRedstoneUpdate > MAX_REDSTONE_UPDATE_FREQ) {
                    updateRedstoneOutput();
                } else {
                    pendingRedstoneStatus = shouldEmitRedstone() ? 1: 0;
                }
            } else if (pendingRedstoneStatus != -1 && world.getGameTime() - lastRedstoneUpdate > MAX_REDSTONE_UPDATE_FREQ) {
                updateRedstoneOutput();
            }

            airHandler.setSideLeaking(hasNoConnectedAirHandlers() ? getRotation() : null);
        }
    }

    private void updateRedstoneOutput() {
        oldRedstoneStatus = shouldEmitRedstone();
        updateNeighbours();
        pendingRedstoneStatus = -1;
        lastRedstoneUpdate = world.getGameTime();
    }

    private List<IAirHandler> findChargeable() {
        List<IAirHandler> res = new ArrayList<>();

        if (!getChargingStack().isEmpty()) {
            getChargingStack().getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).ifPresent(h -> {
                res.add(h);
                chargingItemPressure = h.getPressure();
            });
        }

        if (getUpgrades(EnumUpgrade.DISPENSER) > 0) {
            List<Entity> entitiesOnPad = getWorld().getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(getPos().up()));
            for (Entity entity : entitiesOnPad) {
                if (entity instanceof ItemEntity) {
                    ((ItemEntity) entity).getItem().getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).ifPresent(res::add);
                } else if (entity instanceof PlayerEntity) {
                    PlayerInventory inv = ((PlayerEntity) entity).inventory;
                    for (int i = 0; i < inv.getSizeInventory(); i++) {
                        ItemStack stack = inv.getStackInSlot(i);
                        stack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).ifPresent(res::add);
                    }
                } else {
                    entity.getCapability(PNCCapabilities.AIR_HANDLER_CAPABILITY).ifPresent(res::add);
                }
            }
        }
        return res;
    }

    @Override
    public boolean canConnectPneumatic(Direction side) {
        return getRotation() == side;
    }

    @Override
    public void handleGUIButtonPress(String tag, boolean shiftHeld, PlayerEntity player) {
        switch (tag) {
            case IGUIButtonSensitive.REDSTONE_TAG:
                redstoneMode++;
                if (redstoneMode > 3) redstoneMode = 0;
                updateNeighbours();
                break;
            case "open_upgrades":
                INamedContainerProvider provider = ((IChargeableContainerProvider) getChargingStack().getItem()).getContainerProvider(this);
                NetworkHooks.openGui((ServerPlayerEntity) player, provider, getPos());
                break;
            case "close_upgrades":
                NetworkHooks.openGui((ServerPlayerEntity) player, this, getPos());
                break;
        }
    }

    @Override
    public IItemHandler getPrimaryInventory() {
        return itemHandler;
    }

    public boolean shouldEmitRedstone() {
        switch (redstoneMode) {
            case 0:
                return false;
            case 1:
                return !charging && !discharging && getChargingStack().getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).isPresent();
            case 2:
                return charging;
            case 3:
                return discharging;

        }
        return false;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(getPos().getX(), getPos().getY(), getPos().getZ(), getPos().getX() + 1, getPos().getY() + 1, getPos().getZ() + 1);
    }

    public ChargeableItemHandler getChargeableInventory() {
        return getWorld().isRemote ? new ChargeableItemHandler(this) : chargeableInventory;
    }

    @Override
    protected LazyOptional<IItemHandler> getInventoryCap() {
        return inventoryCap;
    }

    @Override
    public void read(CompoundNBT tag) {
        super.read(tag);
        redstoneMode = tag.getInt(NBTKeys.NBT_REDSTONE_MODE);
        itemHandler = new ChargingStationHandler();
        itemHandler.deserializeNBT(tag.getCompound("Items"));

        ItemStack chargeSlot = getChargingStack();
        if (chargeSlot.getItem() instanceof IChargeableContainerProvider) {
            chargeableInventory = new ChargeableItemHandler(this);
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        super.write(tag);
        if (chargeableInventory != null) {
            chargeableInventory.writeToNBT();
        }
        tag.putInt(NBTKeys.NBT_REDSTONE_MODE, redstoneMode);
        tag.put("Items", itemHandler.serializeNBT());
        return tag;
    }

    @Override
    public void writeToPacket(CompoundNBT tag) {
        super.writeToPacket(tag);

        ICamouflageableTE.writeCamo(tag, camoState);
    }

    @Override
    public void readFromPacket(CompoundNBT tag) {
        super.readFromPacket(tag);

        camoState = ICamouflageableTE.readCamo(tag);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (chargeableInventory != null) {
            chargeableInventory.writeToNBT();
        }
    }

    @Override
    public void onUpgradesChanged() {
        super.onUpgradesChanged();

        if (world != null && !world.isRemote) {
            BlockState state = world.getBlockState(pos);
            world.setBlockState(pos, state.with(BlockChargingStation.CHARGE_PAD, getUpgrades(EnumUpgrade.DISPENSER) > 0));
        }
    }

    @Override
    public int getRedstoneMode() {
        return redstoneMode;
    }

    @Override
    protected List<String> getRedstoneButtonLabels() {
        return REDSTONE_LABELS;
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    @Override
    public BlockState getCamouflage() {
        return camoState;
    }

    @Override
    public void setCamouflage(BlockState state) {
        camoState = state;
        ICamouflageableTE.syncToClient(this);
    }
    
    @Override
    public void remove(){
        super.remove();
        GlobalTileEntityCacheManager.getInstance().chargingStations.remove(this);
    }
    
    @Override
    public void validate(){
        super.validate();
        GlobalTileEntityCacheManager.getInstance().chargingStations.add(this);
    }

    @Nullable
    @Override
    public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new ContainerChargingStation(i, playerInventory, getPos());
    }

    @Override
    public ITextComponent getDisplayName() {
        return getDisplayNameInternal();
    }

    private class ChargingStationHandler extends BaseItemStackHandler {
        ChargingStationHandler() {
            super(TileEntityChargingStation.this, INVENTORY_SIZE);
        }
        
        @Override
        public int getSlotLimit(int slot){
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return slot == CHARGE_INVENTORY_INDEX
                    && (itemStack.isEmpty() || itemStack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).isPresent());
        }

        @Override
        protected void onContentsChanged(int slot) {
            TileEntityChargingStation teCS = TileEntityChargingStation.this;

            ItemStack newStack = getStackInSlot(slot);
            if (!ItemStack.areItemsEqual(chargingStackSynced, newStack)) {
                chargingStackSynced = new ItemStack(newStack.getItem());
            }

            if (teCS.getWorld().isRemote || slot != CHARGE_INVENTORY_INDEX) return;

            teCS.chargeableInventory = newStack.getItem() instanceof IChargeableContainerProvider ?
                    new ChargeableItemHandler(teCS) :
                    null;

            // if any other player has a gui open for the previous item, force a reopen of the charging station gui
            for (PlayerEntity player : teCS.getWorld().getPlayers()) {
                if (player instanceof ServerPlayerEntity
                        && player.openContainer instanceof ContainerChargingStationItemInventory
                        && ((ContainerChargingStationItemInventory) player.openContainer).te == te) {
                    NetworkHooks.openGui((ServerPlayerEntity) player, TileEntityChargingStation.this, getPos());
                }
            }
        }
    }
}
