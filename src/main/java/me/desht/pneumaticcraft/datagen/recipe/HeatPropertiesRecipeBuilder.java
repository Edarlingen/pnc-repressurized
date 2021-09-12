package me.desht.pneumaticcraft.datagen.recipe;

import com.google.gson.JsonObject;
import me.desht.pneumaticcraft.api.crafting.PneumaticCraftRecipeTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;

/**
 * Currently unused.  Would require a lot of extra mods in the dev workspace to use properly.  Maybe this should
 * just take string block & blockstate names?  For now though the heat properties files remain as non-generated
 * JSONs while I decide how best to handle it.
 */
@SuppressWarnings("unused")
public class HeatPropertiesRecipeBuilder extends PneumaticCraftRecipeBuilder<HeatPropertiesRecipeBuilder> {
    private final Block block;
    private final int temperature;
    private final double thermalResistance;
    private final Map<String, String> predicates;
    private final int heatCapacity;
    private final BlockState transformHot;
    private final BlockState transformCold;
    private final BlockState transformHotFlowing;
    private final BlockState transformColdFlowing;

    public HeatPropertiesRecipeBuilder(Block block, int temperature, double thermalResistance, Map<String,String> predicates) {
        this(block, temperature, thermalResistance, predicates, 0, null, null, null, null);
    }

    public HeatPropertiesRecipeBuilder(Block block, int temperature, double thermalResistance, Map<String,String> predicates, int heatCapacity,
                                       BlockState transformHot, BlockState transformCold, BlockState transformHotFlowing, BlockState transformColdFlowing) {
        super(RL(PneumaticCraftRecipeTypes.HEAT_PROPERTIES));
        this.block = block;
        this.temperature = temperature;
        this.thermalResistance = thermalResistance;
        this.predicates = predicates;
        this.heatCapacity = heatCapacity;
        this.transformHot = transformHot;
        this.transformCold = transformCold;
        this.transformHotFlowing = transformHotFlowing;
        this.transformColdFlowing = transformColdFlowing;
    }

    @Override
    protected RecipeResult getResult(ResourceLocation id) {
        return new HeatPropertiesRecipeResult(id);
    }

    public class HeatPropertiesRecipeResult extends RecipeResult {
        HeatPropertiesRecipeResult(ResourceLocation id) {
            super(id);
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            json.addProperty("block", block.getRegistryName().toString());
            json.addProperty("temperature", temperature);
            json.addProperty("thermalResistance", thermalResistance);
            if (!predicates.isEmpty()) {
                JsonObject obj = new JsonObject();
                predicates.forEach(obj::addProperty);
                json.add("statePredicate", obj);
            }
            if (heatCapacity > 0) json.addProperty("heatCapacity", heatCapacity);
            maybeAddBlockstateProp(json, "transformHot", transformHot);
            maybeAddBlockstateProp(json, "transformCold", transformCold);
            maybeAddBlockstateProp(json, "transformHotFlowing", transformHotFlowing);
            maybeAddBlockstateProp(json, "transformColdFlowing", transformColdFlowing);
        }

        private void maybeAddBlockstateProp(JsonObject json, String key, BlockState state) {
            if (state != null) {
                JsonObject obj = new JsonObject();
                if (state.getBlock() instanceof FlowingFluidBlock) {
                    obj.addProperty("fluid", ((FlowingFluidBlock) state.getBlock()).getFluid().getRegistryName().toString());
                } else {
                    obj.addProperty("block", state.toString());
                }
                json.add(key, obj);
            }
        }
    }
}
