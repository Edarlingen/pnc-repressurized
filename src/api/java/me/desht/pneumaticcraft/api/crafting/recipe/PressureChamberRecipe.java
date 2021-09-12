package me.desht.pneumaticcraft.api.crafting.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.*;

public abstract class PressureChamberRecipe extends PneumaticCraftRecipe {
    protected PressureChamberRecipe(ResourceLocation id) {
        super(id);
    }

    /**
     * Returns the minimum pressure required to craft the recipe. Negative pressures are also acceptable; in this
     * case the pressure chamber's pressure must be <strong>lower</strong> than the required pressure.
     *
     * @deprecated don't override this; override {@link #getCraftingPressure(IItemHandler, List)} instead
     * @return threshold pressure
     */
    @Deprecated
    public float getCraftingPressure() {
        return 0f;
    }

    /**
     * Returns the minimum pressure required to craft the recipe. Negative pressures are also acceptable; in this
     * case the pressure chamber's pressure must be <strong>lower</strong> than the required pressure.
     *
     * @param chamberHandler what's currently in the pressure chamber
     * @param ingredientSlots a list of slots in {@code chamberHandler} (as returned by {@link #findIngredients(IItemHandler)})
     *                        where the ingredients can be found
     * @return threshold pressure
     */
    public float getCraftingPressure(IItemHandler chamberHandler, List<Integer> ingredientSlots) {
        return getCraftingPressure();
    }

    /**
     * Get the required crafting pressure for the items specified by {@link #getInputsForDisplay()}, for display
     * purposes only (e.g. for JEI)
     */
    public abstract float getCraftingPressureForDisplay();

    /**
     * When called (by the pressure chamber TE when it detects a change in the chamber contents), try to find the
     * ingredients for this recipe in the given item handler, which represents all of the items currently in the
     * pressure chamber. You must return a collection of slot indices into the item handler which contain the matching
     * ingredients; those indices will be passed promptly to {@link #getCraftingPressure(IItemHandler, List)} and
     * {@link #craftRecipe(IItemHandler, List, boolean)} by the pressure chamber.
     * <p><strong>Do not cache this list across ticks</strong>, since the chamber contents are quite likely to
     * change in the meantime.</p>
     *
     * @param chamberHandler what's currently in the pressure chamber
     * @return if this recipe is valid, a list of slots in the item handler where the ingredients can be found; otherwise, an empty list
     */
    public abstract Collection<Integer> findIngredients(@Nonnull IItemHandler chamberHandler);

    /**
     * Get the input items for this recipe. This is primarily intended for recipe display purposes by
     * JEI or any other recipe display mod.
     */
    public abstract List<Ingredient> getInputsForDisplay();

    /**
     * Implement if no output slots display more than one stack.
     *
     * @see PressureChamberRecipe#getResultsForDisplay()
     */
    protected List<ItemStack> getSingleResultsForDisplay() {
        return ImmutableList.of();
    }

    /**
     * Get the output of this recipe, without crafting it. This is intended for recipe display purposes by
     * JEI, Patchouli, or any other recipe display mod.
     * <p>
     * If overriding and no output slots display more than one stack then can override
     * {@link PressureChamberRecipe#getSingleResultsForDisplay()} instead.
     */
    public List<List<ItemStack>> getResultsForDisplay() {
        return getSingleResultsForDisplay().stream()
                .map(ImmutableList::of)
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Get the slots that are synchronized with each other.
     * They must have the same cycle length and not intersect.
     * <p>
     * A sync group (represented by a set of {@link RecipeSlot}s) will have all its member's cycles synchronized
     * should one of the members be the focus for a recipe lookup.
     * <p>
     * If you need more functionality, see {@link PressureChamberRecipe#getSyncForDisplay(SlotCycle)}.
     *
     * @return List of sync groups represented by a set of {@link RecipeSlot}s
     */
    protected List<Set<RecipeSlot>> getSyncGroupsForDisplay() {
        return ImmutableList.of();
    }

    /**
     * Get the slots and associated cycles that are synchronized with the given focused slot and matched cycle indices.
     * Prefer overriding {@link PressureChamberRecipe#getSyncGroupsForDisplay()} unless you need special handling.
     * <p>
     * This method is called when a recipe lookup is done through JEI that has a focus.
     *
     * @param focusedSlotCycle Slot and indices of the slot's cycle that matches with the focus
     * @return Synchronizations for the given slot cycle
     */
    public Map<RecipeSlot, List<Integer>> getSyncForDisplay(SlotCycle focusedSlotCycle) {
        RecipeSlot focusedSlot = focusedSlotCycle.getSlot();
        return getSyncGroupsForDisplay().stream()
                // Find group that contains the focused slot
                .filter(set -> set.contains(focusedSlot))
                .findAny()
                // Create a mapping from slot to cycle, using the focused slot's cycle
                .map(set -> set.stream()
                        .collect(ImmutableMap.toImmutableMap(slot -> slot, slot -> (List<Integer>) focusedSlotCycle.getCycle()))
                )
                .orElseGet(ImmutableMap::of);
    }

    /**
     * Check if the given item is a valid input item for this recipe.  This should also be true even if the number of
     * items in the passed item stack is smaller than the number required by the recipe; this is testing for item type,
     * not item count.
     *
     * @param stack item stack to check
     * @return true if this is a valid item, false otherwise
     */
    public abstract boolean isValidInputItem(ItemStack stack);

    /**
     * This method is called when the Pressure Chamber is ready to craft with this recipe, and will only be called when
     * {@link #findIngredients(IItemHandler)} returns a non-empty list of slot numbers, i.e. the necessary
     * items are definitely in the chamber.
     * The implementation is responsible for removing the items that have been used from the {@code chamberHandler}.
     * The implementation must also return the list of crafted items, for the Pressure Chamber to insert into its
     * output item handler.
     *
     * @param chamberHandler  items in the pressure chamber; should be modified to remove recipe input items.
     * @param ingredientSlots slots in the chamber handler where the ingredients can be found, as returned from {@link #findIngredients(IItemHandler)}
     * @param simulate        pass on to uses of {@code chamberHandler}
     * @return the resulting items; these do not have to be copies, since the Pressure Chamber itself will insert copies of these items
     */
    @Nonnull
    public abstract NonNullList<ItemStack> craftRecipe(@Nonnull IItemHandler chamberHandler, List<Integer> ingredientSlots, boolean simulate);

    /**
     * Return a translation key for a supplementary tooltip to be displayed on the ingredient or resulting item.  For
     * use in recipe display systems such as JEI.
     *
     * @param input true if this is an input item, false if an output item
     * @param slot the slot number
     * @return a tooltip translation key, or "" for no tooltip
     */
    public String getTooltipKey(boolean input, int slot) {
        return "";
    }

    /**
     * Data object for type of slot and the index of the slot.
     */
    public static final class RecipeSlot {
        private final boolean input;
        private final int index;

        /**
         * Create a data object to store the type of slot and the index of the slot.
         *
         * @param input true iff this is an input slot
         * @param index index of the slot in the recipe
         */
        public RecipeSlot(boolean input, int index) {
            this.input = input;
            this.index = index;
        }

        /**
         * Checks if this is an input slot.
         */
        public boolean isInput() {
            return input;
        }

        /**
         * Get the index of the slot.
         */
        public int getIndex() {
            return index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecipeSlot that = (RecipeSlot) o;
            return input == that.input && index == that.index;
        }

        @Override
        public int hashCode() {
            return Objects.hash(input, index);
        }
    }

    /**
     * Data object for storing a {@link RecipeSlot} and a cycle represented as a list indices for the slot.
     */
    public static final class SlotCycle {
        private final RecipeSlot slot;
        private final ImmutableList<Integer> cycle;

        /**
         * Create a data object to store a {@link RecipeSlot} and a cycle represented as a list of indices for the slot.
         *
         * @param slot  Type of slot and the index of the slot
         * @param cycle A cycle represented as a list of indices
         */
        public SlotCycle(RecipeSlot slot, ImmutableList<Integer> cycle) {
            this.slot = slot;
            this.cycle = cycle;
        }

        /**
         * Get the {@link RecipeSlot} that this cycle belongs to.
         */
        public RecipeSlot getSlot() {
            return slot;
        }

        /**
         * Get the cycle represented as a list of indices.
         */
        public ImmutableList<Integer> getCycle() {
            return cycle;
        }
    }
}
