package me.desht.pneumaticcraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.desht.pneumaticcraft.api.crafting.TemperatureRange;
import me.desht.pneumaticcraft.api.crafting.recipe.RefineryRecipe;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTank;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.inventory.ContainerRefinery;
import me.desht.pneumaticcraft.common.recipes.PneumaticCraftRecipeType;
import me.desht.pneumaticcraft.common.tileentity.TileEntityRefineryController;
import me.desht.pneumaticcraft.common.tileentity.TileEntityRefineryOutput;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GuiRefineryController extends GuiPneumaticContainerBase<ContainerRefinery, TileEntityRefineryController> {
    private List<TileEntityRefineryOutput> outputs;
    private WidgetTemperature widgetTemperature;
    private int nExposedFaces;

    public GuiRefineryController(ContainerRefinery container, PlayerInventory inv, ITextComponent displayString) {
        super(container, inv, displayString);

        imageHeight = 189;
    }

    @Override
    public void init() {
        super.init();

        widgetTemperature = new WidgetTemperature(leftPos + 32, topPos + 32, TemperatureRange.of(273, 673), 273, 50);
        addButton(widgetTemperature);

        addButton(new WidgetTank(leftPos + 8, topPos + 25, te.getInputTank()));

        int x = leftPos + 95;
        int y = topPos + 29;

        // "te" always refers to the master refinery; the bottom block of the stack
        outputs = new ArrayList<>();
        TileEntity te1 = te.findAdjacentOutput();
        if (te1 != null) {
            int i = 0;
            do {
                TileEntityRefineryOutput teRO = (TileEntityRefineryOutput) te1;
                if (outputs.size() < 4) addButton(new WidgetTank(x, y, te.outputsSynced[i++]));
                x += 20;
                y -= 4;
                outputs.add(teRO);
                te1 = te1.getLevel().getBlockEntity(te1.getBlockPos().above());
            } while (te1 instanceof TileEntityRefineryOutput);
        }

        if (outputs.size() < 2 || outputs.size() > 4) {
            problemTab.openStat();
        }

        nExposedFaces = HeatUtil.countExposedFaces(outputs);
    }

    @Override
    public void tick() {
        super.tick();

        if (te.maxTemp > te.minTemp && !te.getCurrentRecipeIdSynced().isEmpty()) {
            widgetTemperature.setOperatingRange(TemperatureRange.of(te.minTemp, te.maxTemp));
        } else {
            widgetTemperature.setOperatingRange(null);
        }
        widgetTemperature.setTemperature(te.getHeatExchanger().getTemperatureAsInt());
        widgetTemperature.autoScaleForTemperature();
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float f, int x, int y) {
        super.renderBg(matrixStack, f, x, y);
        if (outputs.size() < 4) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            fill(matrixStack, leftPos + 155, topPos + 17, leftPos + 171, topPos + 81, 0x40FF0000);
            if (outputs.size() < 3) {
                fill(matrixStack, leftPos + 135, topPos + 21, leftPos + 151, topPos + 85, 0x40FF0000);
            }
            if (outputs.size() < 2) {
                fill(matrixStack, leftPos + 115, topPos + 25, leftPos + 131, topPos + 89, 0x40FF0000);
            }
            if (outputs.size() < 1) {
                fill(matrixStack, leftPos + 95, topPos + 29, leftPos + 111, topPos + 93, 0x40FF0000);
            }
            RenderSystem.disableBlend();
        }
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return Textures.GUI_REFINERY;
    }

    @Override
    public void addProblems(List<ITextComponent> curInfo) {
        super.addProblems(curInfo);

        if (te.getHeatExchanger().getTemperatureAsInt() < te.minTemp) {
            curInfo.addAll(GuiUtils.xlateAndSplit("pneumaticcraft.gui.tab.problems.notEnoughHeat"));
        }
        if (te.getInputTank().getFluidAmount() < 10) {
            curInfo.addAll(GuiUtils.xlateAndSplit("pneumaticcraft.gui.tab.problems.refinery.noOil"));
        }
        if (outputs.size() < 2) {
            curInfo.addAll(GuiUtils.xlateAndSplit("pneumaticcraft.gui.tab.problems.refinery.notEnoughRefineries"));
        } else if (outputs.size() > 4) {
            curInfo.addAll(GuiUtils.xlateAndSplit("pneumaticcraft.gui.tab.problems.refinery.tooManyRefineries"));
        }
    }

    @Override
    protected void addWarnings(List<ITextComponent> curInfo) {
        super.addWarnings(curInfo);

        if (te.isBlocked()) {
            curInfo.addAll(GuiUtils.xlateAndSplit("pneumaticcraft.gui.tab.problems.refinery.outputBlocked"));
        }
        if (nExposedFaces > 0) {
            curInfo.addAll(GuiUtils.xlateAndSplit("pneumaticcraft.gui.tab.problems.exposedFaces", nExposedFaces, outputs.size() * 6));
        }
    }

    @Override
    protected boolean shouldAddUpgradeTab() {
        return false;
    }

    @Override
    public Collection<FluidStack> getTargetFluids() {
        return getCurrentRecipe(PneumaticCraftRecipeType.REFINERY)
                .map(RefineryRecipe::getOutputs)
                .orElse(Collections.emptyList());
    }
}
