package me.desht.pneumaticcraft.common.fluid;

import me.desht.pneumaticcraft.api.crafting.recipe.FuelQualityRecipe;
import me.desht.pneumaticcraft.api.fuel.IFuelRegistry;
import me.desht.pneumaticcraft.common.recipes.PneumaticCraftRecipeType;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.fluid.Fluid;
import net.minecraft.tags.ITag;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public enum FuelRegistry implements IFuelRegistry {
    INSTANCE;

    private static final FuelRecord MISSING_FUEL_ENTRY = new FuelRecord(0, 1f);

    // values which have been registered in code (could be accessed from off-thread via API)
    private final Map<ITag<Fluid>, FuelRecord> fuelTags = new ConcurrentHashMap<>();

    private final Map<Fluid, FuelRecord> cachedFuels = new HashMap<>();  // cleared on a /reload
    private final Map<Fluid, FuelRecord> hotFluids = new HashMap<>();

    public static FuelRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public void registerFuel(ITag<Fluid> fluidTag, int mLPerBucket, float burnRateMultiplier) {
        Validate.notNull(fluidTag);
        Validate.isTrue(mLPerBucket >= 0, "mlPerBucket can't be < 0!");
        Validate.isTrue(burnRateMultiplier > 0f, "burnRate can't be <= 0!");

        if (fuelTags.containsKey(fluidTag)) {
            Log.info("Overriding liquid fuel tag entry %s with a fuel value of %d (previous value %d)",
                    fluidTag, mLPerBucket, fuelTags.get(fluidTag).mLperBucket);
        }
        fuelTags.put(fluidTag, new FuelRecord(mLPerBucket, burnRateMultiplier));
        Log.info("Registering liquid fuel tag entry '%s': %d mL air/bucket, burn rate %f",
                fluidTag, mLPerBucket, burnRateMultiplier);
    }

    // non-API!
    public void registerHotFluid(Fluid fluid, int mLPerBucket, float burnRateMultiplier) {
        hotFluids.put(fluid, new FuelRecord(mLPerBucket, burnRateMultiplier));
    }

    @Override
    public int getFuelValue(World world, Fluid fluid) {
        return cachedFuels.computeIfAbsent(fluid, k -> findEntry(world, fluid)).mLperBucket;
    }

    @Override
    public float getBurnRateMultiplier(World world, Fluid fluid) {
        return cachedFuels.computeIfAbsent(fluid, k -> findEntry(world, fluid)).burnRateMultiplier;
    }

    @Override
    public Collection<Fluid> registeredFuels(World world) {
        Set<Fluid> res = new HashSet<>(hotFluids.keySet());

        // recipes, from datapacks
        for (FuelQualityRecipe recipe : PneumaticCraftRecipeType.FUEL_QUALITY.getRecipes(world).values()) {
            res.addAll(recipe.getFuel().getFluidStacks().stream()
                    .map(FluidStack::getFluid)
                    .filter(f -> f.isSource(f.defaultFluidState()))
                    .collect(Collectors.toList()));
        }

        // fluids tags added by code
        fuelTags.forEach((tag, entry) -> {
            if (entry.mLperBucket > 0) {
                List<Fluid> l = tag.getValues().stream().filter(f -> f.isSource(f.defaultFluidState())).collect(Collectors.toList());
                res.addAll(l);
            }
        });

        return res;
    }

    public void clearCachedFuelFluids() {
        // called when tags are reloaded
        cachedFuels.clear();
    }

    private FuelRecord findEntry(World world, Fluid fluid) {
        // special case for high-temperature fluids
        FuelRecord fe = hotFluids.get(fluid);
        if (fe != null) return fe;

        // stuff from datapacks (override default registered stuff)
        for (FuelQualityRecipe recipe : PneumaticCraftRecipeType.FUEL_QUALITY.getRecipes(world).values()) {
            if (recipe.matchesFluid(fluid)) {
                return new FuelRecord(recipe.getAirPerBucket(), recipe.getBurnRate());
            }
        }

        // fluid tags registered in code
        for (Map.Entry<ITag<Fluid>, FuelRecord> entry : fuelTags.entrySet()) {
            if (entry.getKey().contains(fluid)) {
                return entry.getValue();
            }
        }

        return MISSING_FUEL_ENTRY;
    }

    private static class FuelRecord {
        final int mLperBucket;
        final float burnRateMultiplier;

        private FuelRecord(int mLperBucket, float burnRateMultiplier) {
            this.mLperBucket = mLperBucket;
            this.burnRateMultiplier = burnRateMultiplier;
        }
    }
}
