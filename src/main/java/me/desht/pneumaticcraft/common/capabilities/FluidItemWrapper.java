package me.desht.pneumaticcraft.common.capabilities;

import me.desht.pneumaticcraft.lib.NBTKeys;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

public class FluidItemWrapper implements ICapabilityProvider {
    private ItemStack stack;
    private final String tankName;
    private final int capacity;
    private final Predicate<Fluid> fluidPredicate;
    private final LazyOptional<IFluidHandlerItem> holder = LazyOptional.of(Handler::new);

    public FluidItemWrapper(ItemStack stack, String tankName, int capacity, Predicate<Fluid> fluidPredicate) {
        this.stack = stack;
        this.tankName = tankName;
        this.capacity = capacity;
        this.fluidPredicate = fluidPredicate;
    }

    public FluidItemWrapper(ItemStack stack, String tankName, int capacity) {
        this(stack, tankName, capacity, fluid -> true);
    }

    /**
     * Serialize some tank data onto the handler's ItemStack.  Used by the item's fluid handler capability.  If the
     * tank is empty, it will be removed from the stack's NBT to keep it clean (helps with stackability)
     * <p>
     * Data is serialized under the "BlockEntityTag" sub-tag, so will
     * be automatically deserialized back into the tile entity when this item (assuming it's from a block,
     * of course) is placed back down.
     *
     * @param tank the fluid tank
     * @param tagName name of the subtag in the itemstack's NBT to store the tank data
     */
     private void serializeTank(FluidTank tank, String tagName) {
         ItemStack newStack = stack.copy();
         CompoundNBT tag = newStack.getOrCreateChildTag(NBTKeys.BLOCK_ENTITY_TAG);
         CompoundNBT subTag = tag.getCompound(NBTKeys.NBT_SAVED_TANKS);
         if (!tank.getFluid().isEmpty()) {
             subTag.put(tagName, tank.writeToNBT(new CompoundNBT()));
         } else {
             subTag.remove(tagName);
         }
         if (!subTag.isEmpty()) {
             tag.put(NBTKeys.NBT_SAVED_TANKS, subTag);
         } else {
             tag.remove(NBTKeys.NBT_SAVED_TANKS);
             if (tag.isEmpty()) {
                 newStack.getTag().remove(NBTKeys.BLOCK_ENTITY_TAG);
                 if (newStack.getTag().isEmpty()) {
                     newStack.setTag(null);
                 }
             }
         }
         stack = newStack;
    }

    /**
     * Deserialize some fluid tank data from an ItemStack into a fluid tank.  Used by the
     * item's fluid handler capability.
     *
     * @param stack the itemstack to load from
     * @param tagName name of the subtag in the itemstack's NBT which holds the saved tank data
     * @param capacity capacity of the created tank
     * @return the deserialized tank, or null
     */
    static FluidTank deserializeTank(ItemStack stack, String tagName, int capacity) {
        CompoundNBT tag = stack.getChildTag(NBTKeys.BLOCK_ENTITY_TAG);
        if (tag != null && tag.contains(NBTKeys.NBT_SAVED_TANKS)) {
            FluidTank tank = new FluidTank(capacity);
            CompoundNBT subTag = tag.getCompound(NBTKeys.NBT_SAVED_TANKS);
            return tank.readFromNBT(subTag.getCompound(tagName));
        }
        return null;
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing)
    {
        return CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY.orEmpty(capability, holder);
    }

    class Handler implements IFluidHandlerItem {
        private final FluidTank fluidTank;

        Handler() {
            FluidTank tank = deserializeTank(stack, tankName, capacity);
            fluidTank = tank == null ? new FluidTank(capacity) : tank;
        }

        @Nonnull
        @Override
        public ItemStack getContainer() {
            return stack;
        }

        @Override
        public int getTanks() {
            return fluidTank == null ? 0 : fluidTank.getTanks();
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluidTank == null ? FluidStack.EMPTY : fluidTank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidTank == null ? 0 : fluidTank.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return fluidTank != null && fluidPredicate.test(stack.getFluid()) && fluidTank.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction doFill) {
            if (fluidTank == null) return 0;
            int filled = fluidTank.fill(resource, doFill);
            if (filled > 0 && doFill == FluidAction.EXECUTE) {
                serializeTank(fluidTank, tankName);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction doDrain) {
            if (fluidTank == null) return FluidStack.EMPTY;
            FluidStack drained = fluidTank.drain(resource, doDrain);
            if (!drained.isEmpty() && doDrain == FluidAction.EXECUTE) {
                serializeTank(fluidTank, tankName);
            }
            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction doDrain) {
            if (fluidTank == null) return FluidStack.EMPTY;
            FluidStack drained = fluidTank.drain(maxDrain, doDrain);
            if (!drained.isEmpty() && doDrain == FluidAction.EXECUTE) {
                serializeTank(fluidTank, tankName);
            }
            return drained;
        }
    }
}
