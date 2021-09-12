package me.desht.pneumaticcraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer2D;
import me.desht.pneumaticcraft.client.util.PointXY;
import me.desht.pneumaticcraft.common.inventory.ContainerVacuumPump;
import me.desht.pneumaticcraft.common.tileentity.TileEntityVacuumPump;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.GuiConstants;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class GuiVacuumPump extends GuiPneumaticContainerBase<ContainerVacuumPump,TileEntityVacuumPump> {

    public GuiVacuumPump(ContainerVacuumPump container, PlayerInventory inv, ITextComponent displayString) {
        super(container, inv, displayString);
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return Textures.GUI_VACUUM_PUMP;
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int x, int y) {
        super.renderLabels(matrixStack, x, y);

        font.draw(matrixStack, "+", 32, 47, 0xFF00AA00);
        font.draw(matrixStack, "-", 138, 47, 0xFFFF0000);

        float pressure = te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, te.getInputSide())
                .orElseThrow(RuntimeException::new).getPressure();
        PressureGaugeRenderer2D.drawPressureGauge(matrixStack, font, -1, PneumaticValues.MAX_PRESSURE_VACUUM_PUMP,
                PneumaticValues.DANGER_PRESSURE_VACUUM_PUMP, PneumaticValues.MIN_PRESSURE_VACUUM_PUMP, pressure,
                imageWidth / 5, imageHeight / 5 + 4);

        float vacPressure = te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, te.getVacuumSide())
                .orElseThrow(RuntimeException::new).getPressure();
        PressureGaugeRenderer2D.drawPressureGauge(matrixStack, font, -1, PneumaticValues.MAX_PRESSURE_VACUUM_PUMP,
                PneumaticValues.DANGER_PRESSURE_VACUUM_PUMP, -1, vacPressure,
                imageWidth * 4 / 5, imageHeight / 5 + 4);
    }

    @Override
    protected PointXY getGaugeLocation() {
        return null;
    }

    @Override
    protected void addPressureStatInfo(List<ITextComponent> pressureStatText) {
        IAirHandlerMachine inputAirHandler = te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, te.getInputSide())
                .orElseThrow(RuntimeException::new);
        IAirHandlerMachine vacuumHandler = te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, te.getVacuumSide())
                .orElseThrow(RuntimeException::new);

        pressureStatText.add(xlate("pneumaticcraft.gui.tab.status.vacuumPump.inputPressure",
                PneumaticCraftUtils.roundNumberTo(inputAirHandler.getPressure(), 2)));
        pressureStatText.add(xlate("pneumaticcraft.gui.tab.status.vacuumPump.vacuumPressure",
                PneumaticCraftUtils.roundNumberTo(vacuumHandler.getPressure(), 2)));
        pressureStatText.add(xlate("pneumaticcraft.gui.tab.status.vacuumPump.inputAir",
                String.format("%,d", inputAirHandler.getAir())));
        pressureStatText.add(xlate("pneumaticcraft.gui.tab.status.vacuumPump.vacuumAir",
                String.format("%,d", vacuumHandler.getAir())));

        int volume = inputAirHandler.getVolume();
        int upgrades = te.getUpgrades(EnumUpgrade.VOLUME);
        pressureStatText.add(xlate("pneumaticcraft.gui.tooltip.baseVolume",
                String.format("%,d", PneumaticValues.VOLUME_VACUUM_PUMP)));
        if (volume > inputAirHandler.getBaseVolume()) {
            pressureStatText.add(new StringTextComponent(GuiConstants.TRIANGLE_RIGHT + " " + upgrades + " x ")
                    .append(EnumUpgrade.VOLUME.getItemStack().getHoverName())
            );
            pressureStatText.add(xlate("pneumaticcraft.gui.tooltip.effectiveVolume", String.format("%,d",volume)));
        }

        if (te.turning) {
            int suction = Math.round(PneumaticValues.PRODUCTION_VACUUM_PUMP * te.getSpeedMultiplierFromUpgrades());
            pressureStatText.add(xlate("pneumaticcraft.gui.tooltip.suction", String.format("%,d", suction)));
        }
    }

    @Override
    protected void addProblems(List<ITextComponent> textList) {
        super.addProblems(textList);
        float pressure = te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, te.getInputSide())
                .map(IAirHandlerMachine::getPressure).orElseThrow(RuntimeException::new);
        if (pressure < PneumaticValues.MIN_PRESSURE_VACUUM_PUMP) {
            textList.add(xlate("pneumaticcraft.gui.tab.problems.notEnoughPressure"));
            textList.add(xlate("pneumaticcraft.gui.tab.problems.applyPressure", PneumaticValues.MIN_PRESSURE_VACUUM_PUMP).withStyle(TextFormatting.BLACK));
        }
    }

}
