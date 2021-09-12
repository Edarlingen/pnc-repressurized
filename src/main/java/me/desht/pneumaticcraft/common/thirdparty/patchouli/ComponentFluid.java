package me.desht.pneumaticcraft.common.thirdparty.patchouli;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTank;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.FluidStack;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ComponentFluid implements ICustomComponent {
    private transient List<FluidStack> fluidStacks;
    private transient WidgetTank tankWidget;
    private transient int scaleParsed = 16000;
    public IVariable fluid;
    public IVariable scale;

    @Override
    public void build(int componentX, int componentY, int pageNum) {
        tankWidget = new WidgetTank(componentX, componentY, 16, 64, fluidStacks.isEmpty() ? FluidStack.EMPTY : fluidStacks.get(0));
        tankWidget.getTank().setCapacity(scaleParsed);
    }

    @Override
    public void render(MatrixStack matrixStack, IComponentRenderContext ctx, float pticks, int mouseX, int mouseY) {
        if (!fluidStacks.isEmpty()) {
            tankWidget.getTank().setFluid(fluidStacks.get(ctx.getTicksInBook() / 20 % fluidStacks.size()));
        }
        if (tankWidget.getTank().getCapacity() > 0 && !tankWidget.getTank().getFluid().isEmpty()) {
            tankWidget.renderButton(matrixStack, mouseX, mouseY, pticks);
            if (ctx.isAreaHovered(mouseX, mouseY, tankWidget.x, tankWidget.y, tankWidget.getWidth(), tankWidget.getHeight())) {
                List<ITextComponent> tooltip = new ArrayList<>();
                tankWidget.addTooltip(mouseX, mouseY, tooltip, Screen.hasShiftDown());
                ctx.setHoverTooltipComponents(tooltip);
            }
        }
    }

    @Override
    public void onVariablesAvailable(UnaryOperator<IVariable> lookup) {
        fluidStacks = lookup.apply(this.fluid).asStreamOrSingleton()
                .map((x) -> x.as(FluidStack.class))
                .collect(Collectors.toList());
        scaleParsed = Integer.parseInt(lookup.apply(scale).asString());
    }
}
