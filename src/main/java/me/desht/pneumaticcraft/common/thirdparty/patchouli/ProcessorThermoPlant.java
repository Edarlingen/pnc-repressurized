package me.desht.pneumaticcraft.common.thirdparty.patchouli;

import me.desht.pneumaticcraft.api.crafting.TemperatureRange;
import me.desht.pneumaticcraft.api.crafting.recipe.ThermoPlantRecipe;
import me.desht.pneumaticcraft.common.recipes.PneumaticCraftRecipeType;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

@SuppressWarnings("unused")
public class ProcessorThermoPlant implements IComponentProcessor {
    private ThermoPlantRecipe recipe = null;
    private String header = null;

    @Override
    public void setup(IVariableProvider iVariableProvider) {
        ResourceLocation recipeId = new ResourceLocation(iVariableProvider.get("recipe").asString());
        this.recipe = PneumaticCraftRecipeType.THERMO_PLANT.getRecipe(Minecraft.getInstance().level, recipeId);
        this.header = iVariableProvider.has("header") ? iVariableProvider.get("header").asString() : "";
    }

    @Override
    public IVariable process(String s) {
        if (recipe == null) return null;

        switch (s) {
            case "header":
                return IVariable.wrap(header.isEmpty() ? defaultHeader() : header);
            case "item_input":
                return Patchouli.Util.getStacks(recipe.getInputItem());
            case "fluid_input":
                return Patchouli.Util.getFluidStacks(recipe.getInputFluid());
            case "item_output":
                return IVariable.from(recipe.getOutputItem());
            case "fluid_output":
                return IVariable.from(recipe.getOutputFluid());
            case "text":
                String pr = PneumaticCraftUtils.roundNumberTo(recipe.getRequiredPressure(), 1);
                String temp = recipe.getOperatingTemperature().asString(TemperatureRange.TemperatureScale.CELSIUS);
                return IVariable.wrap(I18n.get("pneumaticcraft.patchouli.processor.thermoPlant.desc", pr, temp));
            case "scale":
                return IVariable.wrap(getScale(recipe));
        }
        return null;
    }

    private int getScale(ThermoPlantRecipe recipe) {
        int in = recipe.getInputFluid().getAmount();
        int out = recipe.getOutputFluid().getAmount();
        if (in >= 4000 || out >= 4000) {
            return 16000;
        } else {
            return 2 * Math.max(in, out);
        }
    }

    private String defaultHeader() {
        if (!recipe.getOutputFluid().isEmpty()) {
            return recipe.getOutputFluid().getDisplayName().getString();
        } else if (!recipe.getOutputItem().isEmpty()) {
            return recipe.getOutputItem().getHoverName().getString();
        } else {
            return "";
        }
    }
}
