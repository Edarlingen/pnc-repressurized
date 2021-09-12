package me.desht.pneumaticcraft.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.thirdparty.ModNameCache;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Consumer;

public class WidgetFluidFilter extends Widget implements ITooltipProvider {
    final Consumer<WidgetFluidFilter> pressable;
    protected FluidStack fluidStack;

    public WidgetFluidFilter(int x, int y, Fluid fluid) {
        this(x, y, fluid, null);
    }

    public WidgetFluidFilter(int x, int y, Fluid fluid, Consumer<WidgetFluidFilter> pressable) {
        this(x, y, new FluidStack(fluid, 1000), pressable);
    }

    WidgetFluidFilter(int x, int y, FluidStack fluidStack, Consumer<WidgetFluidFilter> pressable) {
        super(x, y, 16, 16, StringTextComponent.EMPTY);
        this.pressable = pressable;
        this.fluidStack = fluidStack;
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        if (!fluidStack.isEmpty()) {
            GuiUtils.drawFluid(matrixStack, new Rectangle2d(x, y, 16, 16), new FluidStack(fluidStack, 1000), null);
        }
    }

    @Override
    public void addTooltip(double mouseX, double mouseY, List<ITextComponent> curTip, boolean shiftPressed) {
        if (!fluidStack.isEmpty()) {
            curTip.add(new FluidStack(fluidStack, 1).getDisplayName());
            curTip.add(new StringTextComponent(ModNameCache.getModName(fluidStack.getFluid()))
                    .withStyle(TextFormatting.BLUE, TextFormatting.ITALIC));
        }
    }

    public Fluid getFluid() {
        return fluidStack.getFluid();
    }

    public WidgetFluidFilter setFluid(Fluid fluid) {
        this.fluidStack = new FluidStack(fluid, 1000);
        return this;
    }


    @Override
    public void onClick(double x, double y) {
        super.onClick(x, y);

        if (pressable != null) pressable.accept(this);
    }
}
