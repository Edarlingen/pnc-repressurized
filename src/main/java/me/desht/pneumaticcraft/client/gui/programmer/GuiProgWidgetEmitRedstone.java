package me.desht.pneumaticcraft.client.gui.programmer;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.WidgetCheckBox;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetEmitRedstone;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;

public class GuiProgWidgetEmitRedstone extends GuiProgWidgetOptionBase<ProgWidgetEmitRedstone> {

    public GuiProgWidgetEmitRedstone(ProgWidgetEmitRedstone widget, GuiProgrammer guiProgrammer) {
        super(widget, guiProgrammer);
    }

    @Override
    public void init() {
        super.init();

        for (Direction dir : DirectionUtil.VALUES) {
            ITextComponent sideName = ClientUtils.translateDirectionComponent(dir);
            WidgetCheckBox checkBox = new WidgetCheckBox(guiLeft + 8, guiTop + 30 + dir.get3DDataValue() * 12, 0xFF404040, sideName,
                    b -> progWidget.getSides()[dir.get3DDataValue()] = b.checked);
            checkBox.checked = progWidget.getSides()[dir.get3DDataValue()];
            addButton(checkBox);
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        font.draw(matrixStack, I18n.get("pneumaticcraft.gui.progWidget.general.affectingSides"), guiLeft + 8, guiTop + 20, 0xFF604040);
    }
}
