package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.ICraftingWidget;
import me.desht.pneumaticcraft.common.util.DummyContainer;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.common.util.ItemTagMatcher;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.fml.hooks.BasicEventHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DroneAICrafting extends Goal {
    private final ICraftingWidget widget;
    private final IDroneBase drone;

    public DroneAICrafting(IDroneBase drone, ICraftingWidget widget) {
        this.drone = drone;
        this.widget = widget;
    }

    @Override
    public boolean canUse() {
        CraftingInventory craftingGrid = widget.getCraftingGrid();
        return widget.getRecipe(drone.world(), craftingGrid).map(recipe -> {
            List<List<ItemStack>> equivalentsList = buildEquivalentsList(craftingGrid);
            if (equivalentsList.isEmpty()) return false;
            int[] equivIndices = new int[9];
            CraftingInventory craftMatrix = new CraftingInventory(new DummyContainer(), 3, 3);
            do {
                for (int i = 0; i < equivalentsList.size(); i++) {
                    ItemStack stack = equivalentsList.get(i).isEmpty() ? ItemStack.EMPTY : equivalentsList.get(i).get(equivIndices[i]);
                    craftMatrix.setItem(i, stack);
                }
                if (recipe.matches(craftMatrix, drone.world()) && doCrafting(recipe.assemble(craftMatrix), craftMatrix)) {
                    return true;
                }
            } while (count(equivIndices, equivalentsList));
            return false;
        }).orElse(false);
    }

    /**
     * Get a list of 9 lists of item from the drone's inventory.  Each element of the list corresponds to a slot in the
     * crafting inventory that is passed.  Each sub-list contains the itemstacks from the drone's inventory
     * which match the crafting grid (either direct item match or via item tag).  The elements of each sub-list are direct
     * references to itemstacks in the drone's inventory; shrinking those stacks (as is done in
     * {@link #doCrafting(ItemStack, CraftingInventory)} will remove items from the drone.
     *
     * @param craftingGrid the crafting grid, set up from the item filter widgets attached to the crafting widget
     * @return a list of 9 lists of itemstack
     */
    private List<List<ItemStack>> buildEquivalentsList(CraftingInventory craftingGrid) {
        List<List<ItemStack>> equivalentsList = new ArrayList<>();
        for (int i = 0; i < craftingGrid.getContainerSize(); i++) {
            equivalentsList.add(new ArrayList<>());
            ItemStack recipeStack = craftingGrid.getItem(i);
            if (!recipeStack.isEmpty()) {
                List<ItemStack> equivalents = new ArrayList<>();
                for (int j = 0; j < drone.getInv().getSlots(); j++) {
                    ItemStack droneStack = drone.getInv().getStackInSlot(j);
                    if (!droneStack.isEmpty() && (droneStack.getItem() == recipeStack.getItem() || ItemTagMatcher.matchTags(droneStack, recipeStack))) {
                        equivalents.add(droneStack);
                    }
                }
                if (equivalents.isEmpty()) return Collections.emptyList();
                equivalentsList.get(i).addAll(equivalents);
            }
        }
        return equivalentsList;
    }

    private boolean count(int[] curIndexes, List<List<ItemStack>> equivalentsList) {
        for (int i = 0; i < equivalentsList.size(); i++) {
            List<ItemStack> list = equivalentsList.get(i);
            curIndexes[i]++;
            if (list.isEmpty() || curIndexes[i] >= list.size()) {
                curIndexes[i] = 0;
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean doCrafting(ItemStack craftedStack, CraftingInventory craftMatrix) {
        for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
            int requiredCount = 0;
            ItemStack stack = craftMatrix.getItem(i);
            if (!stack.isEmpty()) {
                for (int j = 0; j < craftMatrix.getContainerSize(); j++) {
                    if (stack == craftMatrix.getItem(j)) {
                        requiredCount++;
                    }
                }
                if (requiredCount > stack.getCount()) return false;
            }
        }

        BasicEventHooks.firePlayerCraftingEvent(drone.getFakePlayer(), craftedStack, craftMatrix);

        for (int i = 0; i < craftMatrix.getContainerSize(); ++i) {
            ItemStack stack = craftMatrix.getItem(i);

            if (!stack.isEmpty()) {
                if (stack.getItem().hasContainerItem(stack)) {
                    ItemStack containerItem = stack.getItem().getContainerItem(stack);
                    if (!containerItem.isEmpty() && containerItem.isDamageableItem() && containerItem.getDamageValue() > containerItem.getMaxDamage()) {
                        MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(drone.getFakePlayer(), containerItem, Hand.MAIN_HAND));
                        continue;
                    }
                    IOHelper.insertOrDrop(drone.world(), containerItem, drone.getInv(), drone.getDronePos(), false);
                }
                stack.shrink(1); // As this stack references to the Drones stacks in its inventory, we can do this.
            }
        }

        for (int i = 0; i < drone.getInv().getSlots(); i++) {
            ItemStack stack = drone.getInv().getStackInSlot(i);
            if (stack.getCount() <= 0) {
                drone.getInv().setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        IOHelper.insertOrDrop(drone.world(), craftedStack, drone.getInv(), drone.getDronePos(), false);

        return true;
    }
}
