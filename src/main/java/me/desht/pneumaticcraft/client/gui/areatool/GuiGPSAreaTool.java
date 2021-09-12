package me.desht.pneumaticcraft.client.gui.areatool;

import me.desht.pneumaticcraft.client.gui.GuiGPSTool;
import me.desht.pneumaticcraft.common.item.ItemGPSAreaTool;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketChangeGPSToolCoordinate;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class GuiGPSAreaTool extends GuiGPSTool {

    private static final int CHANGE_AREA_BUTTON_WIDTH = 4 * 22 + 40 + 5 * 2;
    private static final int P1P2_BUTTON_WIDTH = 30;

    private final BlockPos[] p1p2Pos = new BlockPos[2];
    private final String[] vars = new String[2];
    private int index;

    private GuiGPSAreaTool(ItemStack stack, Hand hand, int index) {
        super(stack.getHoverName(), hand,
                ItemGPSAreaTool.getGPSLocation(Minecraft.getInstance().level, stack, index),
                ItemGPSAreaTool.getVariable(stack, index));

        this.index = index;
        for (int i = 0; i <= 1; i++) {
            p1p2Pos[i] = ItemGPSAreaTool.getGPSLocation(Minecraft.getInstance().level, stack, i);
            vars[i] = ItemGPSAreaTool.getVariable(stack, i);
        }
    }

    public static void showGUI(ItemStack stack, Hand hand, int index) {
        Minecraft.getInstance().setScreen(new GuiGPSAreaTool(stack, hand, index));
    }

    @Override
    public void init() {
        super.init();

        int xMiddle = width / 2;
        int yMiddle = height / 2;

        int x = xMiddle - CHANGE_AREA_BUTTON_WIDTH / 2;
        int y = yMiddle + 100;
        addButton(new Button(x, y, CHANGE_AREA_BUTTON_WIDTH, 20, xlate("pneumaticcraft.gui.gps_area_tool.changeAreaType"), b -> {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            ProgWidgetArea area = ItemGPSAreaTool.getArea(stack);
            minecraft.setScreen(new GuiProgWidgetAreaTool(area, hand, () -> minecraft.setScreen(new GuiGPSAreaTool(stack, hand, index))));
        }));

        addButton(new Button(xMiddle - P1P2_BUTTON_WIDTH / 2, yMiddle - 45, P1P2_BUTTON_WIDTH, 20, getToggleLabel(),
                this::toggle));
    }

    @Override
    protected int getIndex() {
        return index;
    }

    @Override
    protected void syncToServer() {
        p1p2Pos[index] = new BlockPos(textFields[0].getIntValue(), textFields[1].getIntValue(), textFields[2].getIntValue());
        vars[index] = variableField.getValue();
        for (int i = 0; i <= 1; i++) {
            if (changed(i)) NetworkHandler.sendToServer(new PacketChangeGPSToolCoordinate(p1p2Pos[i], hand, vars[i], i));
        }
    }

    private boolean changed(int index) {
        ItemStack stack = minecraft.player.getItemInHand(hand);
        BlockPos p = ItemGPSAreaTool.getGPSLocation(Minecraft.getInstance().level, stack, index);
        String var = ItemGPSAreaTool.getVariable(stack, index);
        return !p.equals(p1p2Pos[index]) || !var.equals(vars[index]);
    }

    private void toggle(Button b) {
        ItemStack stack = Minecraft.getInstance().player.getItemInHand(hand);
        if (stack.getItem() instanceof ItemGPSAreaTool) {
            p1p2Pos[index] = new BlockPos(textFields[0].getIntValue(), textFields[1].getIntValue(), textFields[2].getIntValue());
            vars[index] = variableField.getValue();

            index = 1 - index;

            b.setMessage(getToggleLabel());
            textFields[0].setValue(p1p2Pos[index].getX());
            textFields[1].setValue(p1p2Pos[index].getY());
            textFields[2].setValue(p1p2Pos[index].getZ());
            variableField.setValue(vars[index]);
        }
    }

    private ITextComponent getToggleLabel() {
        TextFormatting color = index == 0 ? TextFormatting.RED : TextFormatting.GREEN;
        return new StringTextComponent("P" + (index + 1)).withStyle(color);
    }
}
