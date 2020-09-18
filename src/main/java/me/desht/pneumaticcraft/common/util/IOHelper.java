package me.desht.pneumaticcraft.common.util;

import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.List;

/*
 * This file is part of Blue Power.
 *
 *     Blue Power is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Blue Power is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Blue Power.  If not, see <http://www.gnu.org/licenses/>
 */

/**
 * @author MineMaarten
 * @author Dynious
 * @author desht
 */
public class IOHelper {
    public enum ExtractCount {
        /**
         * Extract exactly the items specified, including amount.  If the exact number isn't available, extract nothing.
         */
        EXACT,
        /**
         * Extract the first matching ItemStack in the inventory, but not more than the given amount.
         */
        FIRST_MATCHING,
        /**
         * Extract up to the number of items specified but not more.
         */
        UP_TO
    }

    public static LazyOptional<IItemHandler> getInventoryForTE(TileEntity te, Direction facing) {
        return te == null ? LazyOptional.empty() : te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
    }

    public static LazyOptional<IItemHandler> getInventoryForTE(TileEntity te) {
        return getInventoryForTE(te, null);
    }

    public static LazyOptional<IFluidHandler> getFluidHandlerForTE(TileEntity te, Direction facing) {
        return te == null ? LazyOptional.empty() : te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing);
    }

    public static LazyOptional<IFluidHandler> getFluidHandlerForTE(TileEntity te) {
        return getFluidHandlerForTE(te, null);
    }

    /**
     * Extract a specific number of the given item from the given item handler
     *
     * @param handler the item handler
     * @param requestedStack the item to search for, including the number of items; this stack is not modified
     * @param countType how to interpret the item count of requestedStack
     * @param simulate true if extraction should only be simulated
     * @param matchNBT if true, require an exact match of item NBT
     *
     * @return the extracted item stack, or ItemStack.EMPTY if nothing was extracted
     */
    public static ItemStack extract(IItemHandler handler, ItemStack requestedStack, ExtractCount countType, boolean simulate, boolean matchNBT) {
        if (requestedStack.isEmpty()) return requestedStack;

        if (handler != null) {
            int itemsFound = 0;
            List<Integer> slotsOfInterest = Lists.newArrayList();
            for (int slot = 0; slot < handler.getSlots() && itemsFound < requestedStack.getCount(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && matchStacks(stack, requestedStack, matchNBT)) {
                    if (countType == ExtractCount.FIRST_MATCHING) {
                        return handler.extractItem(slot, Math.min(requestedStack.getCount(), stack.getCount()), simulate);
                    }
                    itemsFound += stack.getCount();
                    slotsOfInterest.add(slot);
                }
            }
            if (countType == ExtractCount.UP_TO || itemsFound >= requestedStack.getCount()) {
                ItemStack exportedStack = ItemStack.EMPTY;
                int itemsNeeded = requestedStack.getCount();
                int totalExtracted = 0;
                for (int slot : slotsOfInterest) {
                    ItemStack stack = handler.getStackInSlot(slot);
                    if (matchStacks(stack, requestedStack, matchNBT)) {
                        int itemsSubtracted = Math.min(itemsNeeded, stack.getCount());
                        if (itemsSubtracted > 0) {
                            exportedStack = stack;
                        }
                        itemsNeeded -= itemsSubtracted;
                        ItemStack extracted = handler.extractItem(slot, itemsSubtracted, simulate);
                        totalExtracted += extracted.getCount();
                    }
                }
                exportedStack = exportedStack.copy();
                exportedStack.setCount(totalExtracted);
                return exportedStack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean matchStacks(ItemStack stack1, ItemStack stack2, boolean matchNBT) {
        return ItemStack.areItemsEqual(stack1, stack2) && (!matchNBT || ItemStack.areItemStackTagsEqual(stack1, stack2));
    }

    @Nonnull
    public static ItemStack insert(ICapabilityProvider provider, ItemStack itemStack, Direction side, boolean simulate) {
        return provider.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)
                .map(handler -> ItemHandlerHelper.insertItem(handler, itemStack, simulate))
                .orElse(itemStack);
    }

    @Nonnull
    public static ItemStack insertStacked(ICapabilityProvider provider, ItemStack itemStack, Direction side, boolean simulate) {
        return provider.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)
                .map(handler -> ItemHandlerHelper.insertItemStacked(handler, itemStack, simulate))
                .orElse(itemStack);
    }

    /**
     * Try to transfer a single item between two item handlers
     *
     * @param input the input handler
     * @param output the output handler
     * @return true if an item was transferred
     */
    public static boolean transferOneItem(IItemHandler input, IItemHandler output) {
        if (input == null || output == null) return false;

        for (int i = 0; i < input.getSlots(); i++) {
            ItemStack extracted = input.extractItem(i, 1, true);
            if (!extracted.isEmpty()) {
                if (ItemHandlerHelper.insertItemStacked(output, extracted, false).isEmpty()) {
                    input.extractItem(i, 1, false);
                    return true;
                }
            }
        }

        return false;
    }

}
