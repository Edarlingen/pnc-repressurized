package me.desht.pneumaticcraft.common.amadron;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import me.desht.pneumaticcraft.api.crafting.recipe.AmadronRecipe;
import me.desht.pneumaticcraft.common.inventory.ContainerAmadron;
import me.desht.pneumaticcraft.common.inventory.ContainerAmadron.EnumProblemState;
import me.desht.pneumaticcraft.common.item.ItemAmadronTablet;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketAmadronOrderResponse;
import me.desht.pneumaticcraft.common.util.CountedItemStacks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ShoppingBasket implements Iterable<ResourceLocation> {
    private final Map<ResourceLocation, Integer> basket;

    public ShoppingBasket() {
        basket = new Object2IntLinkedOpenHashMap<>();
    }

    public static ShoppingBasket fromNBT(CompoundNBT subTag) {
        ShoppingBasket res = new ShoppingBasket();
        if (subTag != null) {
            for (String key : subTag.getAllKeys()) {
                int count = subTag.getInt(key);
                if (count > 0) res.setOffer(new ResourceLocation(key), count);
            }
        }
        return res;
    }

    public CompoundNBT toNBT() {
        CompoundNBT subTag = new CompoundNBT();
        basket.forEach((key, value) -> {
            if (value > 0) subTag.putInt(key.toString(), value);
        });
        return subTag;
    }

    public int getUnits(ResourceLocation offerId) {
        return basket.getOrDefault(offerId, 0);
    }

    public void setOffer(ResourceLocation offerId, int units) {
        basket.put(offerId, units);
    }

    public void addUnitsToOffer(ResourceLocation offerId, int toAdd) {
        basket.put(offerId, Math.max(0, getUnits(offerId) + toAdd));
        removeIfEmpty(offerId);
    }

    public void remove(ResourceLocation offerId) {
        basket.remove(offerId);
    }

    public void halve(ResourceLocation offerId) {
        basket.put(offerId, getUnits(offerId) / 2);
        removeIfEmpty(offerId);
    }

    private void removeIfEmpty(ResourceLocation offerId) {
        if (getUnits(offerId) == 0) basket.remove(offerId);
    }

    public void clear() {
        basket.clear();
    }

    @Override
    public Iterator<ResourceLocation> iterator() {
        return basket.keySet().iterator();
    }

    /**
     * Go through all items & fluids in the basket and ensure that the providing inventory/tank contains enough
     * resources to fund all of the offers.
     *
     * @param tablet    the Amadron tablet, to get the inventory/tank locations
     * @param allOffers true to check all offers as one, false to check each individually
     */
    public EnumProblemState cap(ItemStack tablet, boolean allOffers) {
        if (basket.isEmpty()) return EnumProblemState.NO_PROBLEMS;  // simple case

        EnumProblemState problem = EnumProblemState.NO_PROBLEMS;

        LazyOptional<IItemHandler> itemCap = ItemAmadronTablet.getItemCapability(tablet);
        LazyOptional<IFluidHandler> fluidCap = ItemAmadronTablet.getFluidCapability(tablet);

        CountedItemStacks itemAmounts = itemCap.map(CountedItemStacks::new).orElse(new CountedItemStacks());
        Map<Fluid, Integer> fluidAmounts = countFluids(fluidCap);

        // make sure the inventory and/or tank are actually present for each available offer
        if (basket.keySet().removeIf(offerId -> {
            AmadronRecipe offer = AmadronOfferManager.getInstance().getOffer(offerId);
            boolean inputOk = offer.getInput().apply(itemStack -> itemCap.isPresent(), fluidStack -> fluidCap.isPresent());
            boolean outputOk = offer.getOutput().apply(itemStack -> itemCap.isPresent(), fluidStack -> fluidCap.isPresent());
            return !inputOk || !outputOk;
        })) problem = EnumProblemState.NO_INVENTORY;

        for (ResourceLocation offerId : basket.keySet()) {
            AmadronRecipe offer = AmadronOfferManager.getInstance().getOffer(offerId);

            // check there's enough in stock, if the order has limited stock
            int units0 = basket.get(offerId);
            int units;
            if (offer.getMaxStock() >= 0 && units0 > offer.getStock()) {
                units = offer.getStock();
                basket.put(offerId, units);
                problem = offer.getStock() == 0 ? EnumProblemState.OUT_OF_STOCK : EnumProblemState.NOT_ENOUGH_STOCK;
            } else {
                units = units0;
            }

            // check there's enough items or fluid in the input
            problem = problem.addProblem(offer.getInput().apply(
                    itemStack -> {
                        int available = itemAmounts.getOrDefault(itemStack, 0);
                        int needed = itemStack.getCount() * units;
                        if (allOffers) itemAmounts.put(itemStack, available - needed);
                        if (available < needed) {
                            basket.put(offerId, available / itemStack.getCount());
                            return EnumProblemState.NOT_ENOUGH_ITEMS;
                        }
                        return EnumProblemState.NO_PROBLEMS;
                    },
                    fluidStack -> {
                        int available = fluidAmounts.getOrDefault(fluidStack.getFluid(), 0);
                        int needed = fluidStack.getAmount() * units;
                        if (allOffers) fluidAmounts.put(fluidStack.getFluid(), available / fluidStack.getAmount());
                        if (available < needed) {
                            basket.put(offerId, available / fluidStack.getAmount());
                            return EnumProblemState.NOT_ENOUGH_FLUID;
                        }
                        return EnumProblemState.NO_PROBLEMS;
                    }
            ));

            // check there's enough space for the returned item/fluid in the output inventory/tank
            problem = problem.addProblem(offer.getOutput().apply(
                    itemStack -> {
                        int availableSpace = offer.getOutput().findSpaceInItemOutput(itemCap, units);
                        if (availableSpace < units) {
                            basket.put(offerId, availableSpace);
                            return EnumProblemState.NOT_ENOUGH_ITEM_SPACE;
                        }
                        return EnumProblemState.NO_PROBLEMS;
                    },
                    fluidStack -> {
                        int availableTrades = Math.min(
                                ContainerAmadron.HARD_MAX_MB / fluidStack.getAmount(),
                                offer.getOutput().findSpaceInFluidOutput(fluidCap, units)
                        );
                        if (availableTrades < units) {
                            basket.put(offerId, availableTrades);
                            return EnumProblemState.NOT_ENOUGH_FLUID_SPACE;
                        }
                        return EnumProblemState.NO_PROBLEMS;
                    }
            ));
        }

        basket.keySet().removeIf(offerId -> basket.get(offerId) == 0);

        return problem;
    }

    public void syncToPlayer(ServerPlayerEntity player) {
        basket.forEach((offerId, units) -> NetworkHandler.sendToPlayer(new PacketAmadronOrderResponse(offerId, units), player));
    }

    public boolean isEmpty() {
        return basket.values().stream().noneMatch(amount -> amount > 0);
    }

    private static Map<Fluid, Integer> countFluids(LazyOptional<IFluidHandler> fluidCap) {
        return fluidCap.map(handler -> {
            Map<Fluid,Integer> result = new HashMap<>();
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack stack = handler.getFluidInTank(i);
                result.merge(stack.getFluid(), stack.getAmount(), Integer::sum);
            }
            return result;
        }).orElse(Collections.emptyMap());
    }
}
