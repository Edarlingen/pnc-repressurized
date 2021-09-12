package me.desht.pneumaticcraft.common.thirdparty.jei;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.crafting.ingredient.FluidIngredient;
import me.desht.pneumaticcraft.common.core.ModFluids;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JEIYeastCraftingCategory implements IRecipeCategory<JEIYeastCraftingCategory.YeastCraftingRecipe> {
    private final String localizedName;
    private final IDrawable background;
    private final IDrawable icon;

    public JEIYeastCraftingCategory() {
        localizedName = I18n.get("pneumaticcraft.gui.jei.title.yeastCrafting");
        background = JEIPlugin.jeiHelpers.getGuiHelper().createDrawable(Textures.GUI_JEI_YEAST_CRAFTING, 0, 0, 128, 40);
        icon = JEIPlugin.jeiHelpers.getGuiHelper().createDrawableIngredient(new ItemStack(ModItems.YEAST_CULTURE_BUCKET.get()));
    }

    @Override
    public ResourceLocation getUid() {
        return ModCategoryUid.YEAST_CRAFTING;
    }

    @Override
    public Class<? extends YeastCraftingRecipe> getRecipeClass() {
        return YeastCraftingRecipe.class;
    }

    @Override
    public String getTitle() {
        return localizedName;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setIngredients(YeastCraftingRecipe recipe, IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, recipe.itemInput);
        ingredients.setInputs(VanillaTypes.FLUID, ImmutableList.of(
                new FluidStack(ModFluids.YEAST_CULTURE.get(), 1000),
                new FluidStack(Fluids.WATER, 1000)
        ));
        ingredients.setOutput(VanillaTypes.FLUID, recipe.output);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, YeastCraftingRecipe recipe, IIngredients ingredients) {
        recipeLayout.getItemStacks().init(0, true, 0, 0);
        recipeLayout.getItemStacks().set(0, ingredients.getInputs(VanillaTypes.ITEM).get(0));

        recipeLayout.getFluidStacks().init(0, true, 16, 16);
        recipeLayout.getFluidStacks().set(0, ingredients.getInputs(VanillaTypes.FLUID).get(0));

        recipeLayout.getFluidStacks().init(1, true, 32, 16);
        recipeLayout.getFluidStacks().set(1, ingredients.getInputs(VanillaTypes.FLUID).get(1));

        recipeLayout.getFluidStacks().init(2, false, 80, 16);
        recipeLayout.getFluidStacks().set(2, ingredients.getOutputs(VanillaTypes.FLUID).get(0));

        recipeLayout.getFluidStacks().init(3, false, 96, 16);
        recipeLayout.getFluidStacks().set(3, ingredients.getOutputs(VanillaTypes.FLUID).get(0));
    }

    public static Collection<?> getAllRecipes() {
        return Collections.singletonList(new YeastCraftingRecipe(
                        new ItemStack(Items.SUGAR),
                        FluidIngredient.of(1000, ModFluids.YEAST_CULTURE.get()),
                        new FluidStack(ModFluids.YEAST_CULTURE.get(), 1000)
                )
        );
    }

    @Override
    public List<ITextComponent> getTooltipStrings(YeastCraftingRecipe recipe, double mouseX, double mouseY) {
        List<ITextComponent> res = new ArrayList<>();
        if (mouseX >= 48 && mouseX <= 80) {
            res.addAll(PneumaticCraftUtils.splitStringComponent(I18n.get("pneumaticcraft.gui.jei.tooltip.yeastCrafting")));
        }
        return res;
    }

    static class YeastCraftingRecipe {
        final ItemStack itemInput;
        final FluidIngredient fluidInput;
        final FluidStack output;

        YeastCraftingRecipe(ItemStack itemInput, FluidIngredient fluidInput, FluidStack output) {
            this.itemInput = itemInput;
            this.fluidInput = fluidInput;
            this.output = output;
        }
    }
}
