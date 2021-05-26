package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableMap;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.crafting.recipe.ThermoPlantRecipe;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.core.ModTileEntities;
import me.desht.pneumaticcraft.common.inventory.ContainerThermopneumaticProcessingPlant;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketPlaySound;
import me.desht.pneumaticcraft.common.recipes.PneumaticCraftRecipeType;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TileEntityThermopneumaticProcessingPlant extends TileEntityPneumaticBase implements
        IMinWorkingPressure, IRedstoneControl<TileEntityThermopneumaticProcessingPlant>, ISerializableTanks,
        IAutoFluidEjecting, INamedContainerProvider, IComparatorSupport, IHeatExchangingTE {

    private static final int INVENTORY_SIZE = 1;
    private static final int CRAFTING_TIME = 60 * 100;  // 60 ticks base crafting time
    private static final double MAX_SPEED_UP = 2.5;  // speed-up due to temperature

    private static final Set<Item> acceptedItemCache = new HashSet<>();
    private static final Set<Fluid> acceptedFluidCache = new HashSet<>();

    @GuiSynced
    @DescSynced
    private final ThermopneumaticFluidTankInput inputTank = new ThermopneumaticFluidTankInput(PneumaticValues.NORMAL_TANK_CAPACITY);
    @GuiSynced
    @DescSynced
    private final SmartSyncTank outputTank = new SmartSyncTank(this, PneumaticValues.NORMAL_TANK_CAPACITY);
    @GuiSynced
    private final IHeatExchangerLogic heatExchanger = PneumaticRegistry.getInstance().getHeatRegistry().makeHeatExchangerLogic();
    private final LazyOptional<IHeatExchangerLogic> heatCap = LazyOptional.of(() -> heatExchanger);
    @GuiSynced
    public final RedstoneController<TileEntityThermopneumaticProcessingPlant> rsController = new RedstoneController<>(this);
    @GuiSynced
    private int craftingProgress;
    @GuiSynced
    private float requiredPressure;
    @GuiSynced
    public int minTemperature;
    @GuiSynced
    public int maxTemperature;
    @GuiSynced
    public TPProblem problem = TPProblem.OK;
    @DescSynced
    private boolean didWork;
    @GuiSynced
    private String currentRecipeIdSynced = "";
    private ThermoPlantRecipe currentRecipe;
    private boolean searchForRecipe = true;

    private final ItemStackHandler inputItemHandler = new InputItemHandler(this);
    private final ItemStackHandler outputItemHandler = new BaseItemStackHandler(this, INVENTORY_SIZE);
    private final ThermopneumaticInvWrapper invWrapper = new ThermopneumaticInvWrapper(inputItemHandler, outputItemHandler);
    private final LazyOptional<IItemHandler> invCap = LazyOptional.of(() -> invWrapper);

    private final ThermopneumaticFluidHandler fluidHandler = new ThermopneumaticFluidHandler();
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluidHandler);
    private double airUsage;

    public TileEntityThermopneumaticProcessingPlant() {
        super(ModTileEntities.THERMOPNEUMATIC_PROCESSING_PLANT.get(), 5, 7, 3000, 4);
        heatExchanger.setThermalResistance(10);
    }

    @Override
    public boolean canConnectPneumatic(Direction dir) {
        return getRotation().getOpposite() != dir && dir != Direction.UP;
    }

    @Override
    public void tick() {
        super.tick();

        inputTank.tick();
        outputTank.tick();

        if (!getWorld().isRemote) {
            problem = TPProblem.OK;

            ThermoPlantRecipe prevRecipe = currentRecipe;
            if (searchForRecipe) {
                currentRecipe = findApplicableRecipe();
                currentRecipeIdSynced = currentRecipe == null ? "" : currentRecipe.getId().toString();
                searchForRecipe = false;
            }
            if (prevRecipe != currentRecipe) {
                getWorld().updateComparatorOutputLevel(getPos(), getBlockState().getBlock());
            }

            didWork = false;
            if (currentRecipe != null) {
                if (getInputTank().getFluidAmount() < currentRecipe.getInputFluid().getAmount()) {
                    problem = TPProblem.NOT_ENOUGH_FLUID;
                } else if (heatExchanger.getTemperature() > currentRecipe.getOperatingTemperature().getMax()) {
                    problem = TPProblem.TOO_HOT;
                } else if (heatExchanger.getTemperature() < currentRecipe.getOperatingTemperature().getMin()) {
                    problem = TPProblem.TOO_COLD;
                } else {
                    if (rsController.shouldRun() && hasEnoughPressure()) {
                        double speedBoost = minTemperature > 0 ? Math.min(MAX_SPEED_UP, heatExchanger.getTemperature() / minTemperature) : 1.0;
                        if (craftingProgress < CRAFTING_TIME) {
                            double progressInc = speedBoost * currentRecipe.getRecipeSpeed() * 100;
                            craftingProgress += progressInc;
                            double progressDivider = progressInc / CRAFTING_TIME;
                            airUsage += currentRecipe.airUsed() * progressDivider * speedBoost;
                            if (airUsage > 1) {
                                int i = (int) airUsage;
                                addAir(-i);
                                airUsage -= i;
                            }
                            heatExchanger.addHeat(-currentRecipe.heatUsed(heatExchanger.getAmbientTemperature()) * speedBoost * 0.75 * progressDivider);
                        }
                        if (craftingProgress >= CRAFTING_TIME) {
                            int filled = outputTank.fill(currentRecipe.getOutputFluid().copy(), FluidAction.SIMULATE);
                            ItemStack excess = outputItemHandler.insertItem(0, currentRecipe.getOutputItem().copy(), true);
                            if (filled == currentRecipe.getOutputFluid().getAmount() && excess.isEmpty()) {
                                outputTank.fill(currentRecipe.getOutputFluid().copy(), FluidAction.EXECUTE);
                                outputItemHandler.insertItem(0, currentRecipe.getOutputItem().copy(), false);
                                inputTank.drain(currentRecipe.getInputFluid().getAmount(), FluidAction.EXECUTE);
                                inputItemHandler.extractItem(0, 1, false);
                                craftingProgress -= CRAFTING_TIME;
                            } else {
                                problem = TPProblem.OUTPUT_BLOCKED;
                            }
                        }
                        didWork = problem == TPProblem.OK;
                    }
                }
            } else {
                problem = TPProblem.NO_RECIPE;
                craftingProgress = 0;
                minTemperature = 0;
                maxTemperature = 0;
                requiredPressure = 0;
            }
        } else {
            if (didWork && getWorld().rand.nextBoolean()) {
                ClientUtils.emitParticles(getWorld(), getPos(), ParticleTypes.SMOKE, 0.9);
            }
        }
    }

    private boolean hasEnoughPressure() {
        if (getMinWorkingPressure() == 0) {
            return true;
        } else if (getMinWorkingPressure() > 0) {
            return getPressure() >= getMinWorkingPressure();
        } else {
            return getPressure() <= getMinWorkingPressure();
        }
    }

    /**
     * Find a recipe which matches the input fluid and/or item
     *
     * @return a recipe, or null for no matching recipe
     */
    private ThermoPlantRecipe findApplicableRecipe() {
        for (ThermoPlantRecipe recipe : PneumaticCraftRecipeType.THERMO_PLANT.getRecipes(world).values()) {
            if (recipe.matches(inputTank.getFluid(), inputItemHandler.getStackInSlot(0))) {
                requiredPressure = recipe.getRequiredPressure();
                minTemperature = recipe.getOperatingTemperature().getMin();
                maxTemperature = recipe.getOperatingTemperature().getMax();
                return recipe;
            }
        }
        return null;
    }

    @Override
    protected LazyOptional<IItemHandler> getInventoryCap() {
        return invCap;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return fluidCap.cast();
        } else {
            return super.getCapability(cap, side);
        }
    }

    public FluidTank getInputTank() {
        return inputTank;
    }

    public FluidTank getOutputTank() {
        return outputTank;
    }

    public double getCraftingPercentage() {
        return (double) craftingProgress / CRAFTING_TIME;
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        super.write(tag);

        tag.put("Items", inputItemHandler.serializeNBT());
        tag.put("Output", outputItemHandler.serializeNBT());
        tag.putInt("craftingProgress", craftingProgress);
        return tag;
    }

    @Override
    public void read(BlockState state, CompoundNBT tag) {
        super.read(state, tag);

        inputItemHandler.deserializeNBT(tag.getCompound("Items"));
        outputItemHandler.deserializeNBT(tag.getCompound("Output"));
        craftingProgress = tag.getInt("craftingProgress");
    }

    @Override
    public LazyOptional<IHeatExchangerLogic> getHeatCap(Direction side) {
        return heatCap;
    }

    @Override
    public void handleGUIButtonPress(String tag, boolean shiftHeld, ServerPlayerEntity player) {
        if (rsController.parseRedstoneMode(tag))
            return;
        if (tag.equals("dump")) {
            FluidStack moved;
            if (shiftHeld) {
                moved = inputTank.drain(inputTank.getCapacity(), FluidAction.EXECUTE);
            } else {
                moved = FluidUtil.tryFluidTransfer(outputTank, inputTank, inputTank.getFluidAmount(), true);
            }
            if (!moved.isEmpty()) {
                NetworkHandler.sendToPlayer(new PacketPlaySound(SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, pos, 1f, 1f, false), player);
            }
        }
    }

    @Override
    public IItemHandler getPrimaryInventory() {
        return inputItemHandler;
    }

    @Override
    public RedstoneController<TileEntityThermopneumaticProcessingPlant> getRedstoneController() {
        return rsController;
    }

    @Override
    public float getMinWorkingPressure() {
        return requiredPressure;
    }

    @Nonnull
    @Override
    public Map<String, FluidTank> getSerializableTanks() {
        return ImmutableMap.of("InputTank", inputTank, "OutputTank", outputTank);
    }

    @Nullable
    @Override
    public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new ContainerThermopneumaticProcessingPlant(i, playerInventory, getPos());
    }

    public IItemHandler getOutputInventory() {
        return outputItemHandler;
    }

    @Override
    public int getComparatorValue() {
        return currentRecipe != null ? 15 : 0;
    }

    public static void clearCachedItemsAndFluids() {
        acceptedItemCache.clear();
        acceptedFluidCache.clear();
    }

    @Override
    public IHeatExchangerLogic getHeatExchanger(Direction dir) {
        return heatExchanger;
    }

    @Override
    public String getCurrentRecipeIdSynced() {
        return currentRecipeIdSynced;
    }

    private class ThermopneumaticFluidTankInput extends SmartSyncTank {
        private FluidStack prevFluid = FluidStack.EMPTY;

        ThermopneumaticFluidTankInput(int capacity){
            super(TileEntityThermopneumaticProcessingPlant.this, capacity);
        }
        
        @Override
        public boolean isFluidValid(FluidStack fluid) {
            if (fluid.isEmpty() || acceptedFluidCache.contains(fluid.getFluid())) return true;
            boolean accepted = PneumaticCraftRecipeType.THERMO_PLANT.stream(world).anyMatch(r -> r.getInputFluid().testFluid(fluid.getFluid()));
            if (accepted) acceptedFluidCache.add(fluid.getFluid());
            return accepted;
        }

        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            FluidStack newFluid = getFluid();
            if (prevFluid.getFluid() != newFluid.getFluid() || prevFluid.isEmpty() && !newFluid.isEmpty() || newFluid.isEmpty() && !prevFluid.isEmpty()) {
                searchForRecipe = true;
                prevFluid = getFluid().copy();
            }
        }
    }

    private class InputItemHandler extends BaseItemStackHandler {
        private Item prev = null;

        public InputItemHandler(TileEntity te) {
            super(te, INVENTORY_SIZE);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty() || acceptedItemCache.contains(stack.getItem())) return true;
            boolean accepted = PneumaticCraftRecipeType.THERMO_PLANT.stream(world).anyMatch(r -> r.getInputItem().test(stack));
            if (accepted) acceptedItemCache.add(stack.getItem());
            return accepted;
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (getStackInSlot(0).getItem() != prev) {
                searchForRecipe = true;
            }
            prev = getStackInSlot(0).getItem();
        }
    }

    private class ThermopneumaticFluidHandler implements IFluidHandler {
        final FluidTank[] tanks;

        ThermopneumaticFluidHandler() {
            tanks = new FluidTank[]{ inputTank, outputTank };
        }

        @Override
        public int getTanks() {
            return tanks.length;
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return tanks[tank].getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tanks[tank].getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return tanks[tank].isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction doFill) {
            return inputTank.fill(resource, doFill);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction doDrain) {
            return outputTank.getFluid().isFluidEqual(resource) ? outputTank.drain(resource.getAmount(), doDrain) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction doDrain) {
            return outputTank.drain(maxDrain, doDrain);
        }
    }

    private class ThermopneumaticInvWrapper implements IItemHandler {
        private final IItemHandler input;
        private final IItemHandler output;

        ThermopneumaticInvWrapper(IItemHandler input, IItemHandler output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public int getSlots() {
            return 2;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? input.getStackInSlot(0) : output.getStackInSlot(0);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return slot == 0 ? inputItemHandler.insertItem(0, stack, simulate) : stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? outputItemHandler.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? input.getSlotLimit(0) : output.getSlotLimit(0);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 ? input.isItemValid(0, stack) : output.isItemValid(0, stack);
        }
    }

    public enum TPProblem {
        OK(""),
        NO_RECIPE("pneumaticcraft.gui.tab.problems.noRecipe"),
        NOT_ENOUGH_FLUID("pneumaticcraft.gui.tab.problems.notEnoughFluid"),
        TOO_HOT("pneumaticcraft.gui.tab.problems.tooMuchHeat"),
        TOO_COLD("pneumaticcraft.gui.tab.problems.notEnoughHeat"),
        OUTPUT_BLOCKED("pneumaticcraft.gui.tab.problems.outputBlocked");

        public final String key;

        TPProblem(String key) {
            this.key = key;
        }
    }
}
