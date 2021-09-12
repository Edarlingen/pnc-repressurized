package me.desht.pneumaticcraft.common.worldgen;

import me.desht.pneumaticcraft.common.config.ConfigHelper;
import me.desht.pneumaticcraft.common.config.PNCConfig.Common.General;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import me.desht.pneumaticcraft.common.core.ModDecorators;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.BlockStateFeatureConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.placement.ChanceConfig;
import net.minecraftforge.event.world.BiomeLoadingEvent;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;

public class ModWorldGen {
    public static ConfiguredFeature<?,?> OIL_LAKES;

    public static void registerConfiguredFeatures() {
        Registry<ConfiguredFeature<?, ?>> registry = WorldGenRegistries.CONFIGURED_FEATURE;

        OIL_LAKES = Feature.LAKE
                .configured(new BlockStateFeatureConfig(ModBlocks.OIL.get().defaultBlockState()))
                .decorated(ModDecorators.OIL_LAKE.get().configured(new ChanceConfig(ConfigHelper.getOilLakeChance())));
        Registry.register(registry, RL("oil_lakes"), OIL_LAKES);
    }

    public static void onBiomeLoading(BiomeLoadingEvent event) {
        if (!General.oilWorldGenBlacklist.contains(event.getName()) && !General.oilWorldGenCategoryBlacklist.contains(event.getCategory().getName())) {
            event.getGeneration().addFeature(GenerationStage.Decoration.LAKES, OIL_LAKES);
        }
    }
}
