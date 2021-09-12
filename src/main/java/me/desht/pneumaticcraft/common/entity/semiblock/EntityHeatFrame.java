package me.desht.pneumaticcraft.common.entity.semiblock;

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.crafting.ingredient.FluidIngredient;
import me.desht.pneumaticcraft.api.crafting.recipe.HeatFrameCoolingRecipe;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.heat.SyncedTemperature;
import me.desht.pneumaticcraft.common.recipes.PneumaticCraftRecipeType;
import me.desht.pneumaticcraft.common.recipes.machine.HeatFrameCoolingRecipeImpl;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class EntityHeatFrame extends EntitySemiblockBase {
    private static final DataParameter<Byte> STATUS = EntityDataManager.defineId(EntityHeatFrame.class, DataSerializers.BYTE);
    private static final DataParameter<Integer> TEMPERATURE = EntityDataManager.defineId(EntityHeatFrame.class, DataSerializers.INT);

    private static final int MIN_COOKING_TEMP = 373;

    private static final byte IDLE = 0;
    private static final byte COOKING = 1;
    private static final byte COOLING = 2;

    private final IHeatExchangerLogic logic = PneumaticRegistry.getInstance().getHeatRegistry().makeHeatExchangerLogic();

    private final LazyOptional<IHeatExchangerLogic> heatCap;
    private int lastValidSlot; // cache the current cooking slot for performance boost
    private int cookingProgress;

    private int coolingProgress;

    private final SyncedTemperature syncedTemperature = new SyncedTemperature(logic);

    public EntityHeatFrame(EntityType<?> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);

        heatCap = LazyOptional.of(() -> logic);
    }

    public IHeatExchangerLogic getHeatExchangerLogic() {
        return logic;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(STATUS, IDLE);
        this.entityData.define(TEMPERATURE, 0);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == PNCCapabilities.HEAT_EXCHANGER_CAPABILITY) {
            return PNCCapabilities.HEAT_EXCHANGER_CAPABILITY.orEmpty(cap, heatCap);
        }
        return super.getCapability(cap, side);
    }

    @Override
    public boolean canPlace(Direction facing) {
        return getCachedTileEntity() != null
                && getCachedTileEntity().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent()
                && !getCachedTileEntity().getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).isPresent();
    }

    private void setStatus(byte status) {
        getEntityData().set(STATUS, status);
    }

    private byte getStatus() {
        return getEntityData().get(STATUS);
    }

    private void setSyncedTemperature(int temperature) {
        getEntityData().set(TEMPERATURE, temperature);
    }

    public int getSyncedTemperature() {
        return getEntityData().get(TEMPERATURE);
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount == 1) {
            logic.initializeAmbientTemperature(level, getBlockPos());
        }

        if (!getWorld().isClientSide) {
            byte newStatus = IDLE;
            if (logic.getTemperature() > MIN_COOKING_TEMP) {
                newStatus = doCooking();
            } else if (logic.getTemperature() < HeatFrameCoolingRecipeImpl.getMaxThresholdTemp(getWorld())) {
                newStatus = doCooling();
            }
            setStatus(newStatus);
            if (newStatus == IDLE) {
                double delta = logic.getTemperature() - logic.getAmbientTemperature();
                if (delta > 1) {
                    logic.addHeat(-0.1);
                } else if (delta < -1) {
                    logic.addHeat(0.1);
                }
            }
            syncedTemperature.tick();
            setSyncedTemperature(syncedTemperature.getSyncedTemp());
        } else {
            // client
            if ((tickCount & 0x3) == 0) {
                byte status = getStatus();
                switch (status) {
                    case COOKING:
                        ClientUtils.emitParticles(level, getBlockPos(), level.random.nextInt(4) == 0 ? ParticleTypes.FLAME : ParticleTypes.SMOKE);
                        break;
                    case COOLING:
                        ClientUtils.emitParticles(level, getBlockPos(), ParticleTypes.SPIT);
                        break;
                }
            }
        }
    }

    private byte doCooking() {
        byte newStatus = IDLE;
        if (cookingProgress < 100) {
            int progress = Math.max(0, ((int) logic.getTemperature() - 343) / 30);
            progress = Math.min(5, progress);
            logic.addHeat(-progress);
            cookingProgress += progress;
            newStatus = COOKING;
        }
        if (cookingProgress >= 100) {
            IOHelper.getInventoryForTE(getCachedTileEntity()).ifPresent(handler -> {
                if (!tryCookSlot(handler, lastValidSlot)) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        if (tryCookSlot(handler, i)) {
                            cookingProgress -= 100;
                            break;
                        }
                    }
                } else {
                    cookingProgress -= 100;
                }
            });
        }
        return newStatus;
    }

    private boolean tryCookSlot(IItemHandler handler, int slot) {
        ItemStack stack = handler.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Inventory inv = new Inventory(1);
            inv.setItem(0, stack);
            return level.getRecipeManager().getRecipeFor(IRecipeType.SMELTING, inv, this.level).map(recipe -> {
                ItemStack result = recipe.getResultItem().copy();
                if (!result.isEmpty()) {
                    ItemStack remainder = ItemHandlerHelper.insertItem(handler, result, true);
                    if (remainder.isEmpty()) {
                        handler.extractItem(slot, 1, false);
                        ItemHandlerHelper.insertItem(handler, result, false);
                        lastValidSlot = slot;
                        return true;
                    }
                }
                return false;
            }).orElse(false);
        }
        return false;
    }

    private byte doCooling() {
        byte newStatus = IDLE;
        if (coolingProgress < 100) {
            int progress = Math.max(0, ((int) logic.getTemperature() - 243) / 30);
            progress = 6 - Math.min(5, progress);
            logic.addHeat(progress);
            coolingProgress += progress;
            newStatus = COOLING;
        }
        if (coolingProgress >= 100) {
            IOHelper.getInventoryForTE(getCachedTileEntity()).ifPresent(handler -> {
                if (!tryCoolSlot(handler, lastValidSlot)) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        if (tryCoolSlot(handler, i)) {
                            coolingProgress -= 100;
                            break;
                        }
                    }
                } else {
                    coolingProgress -= 100;
                }
            });
        }
        return newStatus;
    }

    private boolean tryCoolSlot(IItemHandler handler, int slot) {
        ItemStack stack = handler.getStackInSlot(slot);
        if (stack.isEmpty()) return false;

        HeatFrameCoolingRecipe recipe = PneumaticCraftRecipeType.HEAT_FRAME_COOLING.findFirst(level, r -> r.matches(stack));

        if (recipe != null) {
            boolean extractedOK;
            if (recipe.getInput() instanceof FluidIngredient) {
                if (stack.getCount() != 1) return false;  // fluid-containing items must not be stacked!
                extractedOK = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).map(fluidHandler -> {
                    int toDrain = ((FluidIngredient) recipe.getInput()).getAmount();
                    if (fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE).getAmount() == toDrain) {
                        ItemStack containerStack = fluidHandler.getContainer().copy();
                        handler.extractItem(slot, 1, false);
                        handler.insertItem(slot, containerStack, false);
                        return true;
                    }
                    return false;
                }).orElse(false);
            } else {
                extractedOK = handler.extractItem(slot, 1, false).getCount() == 1;
            }
            if (extractedOK) {
                ItemStack result = ItemHandlerHelper.copyStackWithSize(recipe.getOutput(), recipe.calculateOutputQuantity(logic.getTemperature()));
                ItemHandlerHelper.insertItem(handler, result, false);
                lastValidSlot = slot;
                return true;
            }
            return extractedOK;
        }
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {
        super.readAdditionalSaveData(tag);

        logic.deserializeNBT(tag.getCompound("heatExchanger"));
        cookingProgress = tag.getInt("cookingProgress");
        coolingProgress = tag.getInt("coolingProgress");
    }

    @Override
    public CompoundNBT serializeNBT(CompoundNBT tag) {
        tag.put("heatExchanger", logic.serializeNBT());
        tag.putInt("cookingProgress", cookingProgress);
        tag.putInt("coolingProgress", coolingProgress);

        return super.serializeNBT(tag);
    }

    @Override
    public void addTooltip(List<ITextComponent> curInfo, PlayerEntity player, CompoundNBT tag, boolean extended) {
        int cook, cool;
        if (!level.isClientSide) {
            // TOP
            cook = cookingProgress;
            cool = coolingProgress;
        } else {
            // Waila
            logic.deserializeNBT(tag.getCompound("heatExchanger"));
            cook = tag.getInt("cookingProgress");
            cool = tag.getInt("coolingProgress");
        }

        if (getStatus() != COOKING && cook >= 100) cook = 0;
        if (getStatus() != COOLING && cool >= 100) cool = 0;
        curInfo.add(HeatUtil.formatHeatString(logic.getTemperatureAsInt()));
        curInfo.add(PneumaticCraftUtils.xlate("pneumaticcraft.waila.heatFrame.cooking", cook).withStyle(TextFormatting.GRAY));
        curInfo.add(PneumaticCraftUtils.xlate("pneumaticcraft.waila.heatFrame.cooling", cool).withStyle(TextFormatting.GRAY));
    }
}
