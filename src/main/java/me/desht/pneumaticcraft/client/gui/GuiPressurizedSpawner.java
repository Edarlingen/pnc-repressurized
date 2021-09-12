package me.desht.pneumaticcraft.client.gui;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.client.gui.widget.WidgetAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetButtonExtended;
import me.desht.pneumaticcraft.client.gui.widget.WidgetRangeToggleButton;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.client.util.PointXY;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import me.desht.pneumaticcraft.common.inventory.ContainerPressurizedSpawner;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPressurizedSpawner;
import me.desht.pneumaticcraft.common.tileentity.TileEntityVacuumTrap;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class GuiPressurizedSpawner extends GuiPneumaticContainerBase<ContainerPressurizedSpawner, TileEntityPressurizedSpawner> {
    WidgetAnimatedStat infoStat;
    WidgetButtonExtended rangeButton;

    public GuiPressurizedSpawner(ContainerPressurizedSpawner container, PlayerInventory inv, ITextComponent displayString) {
        super(container, inv, displayString);
    }

    @Override
    public void init() {
        super.init();

        addButton(rangeButton = new WidgetRangeToggleButton(leftPos + 152, topPos + 66, te));

        infoStat = addAnimatedStat(xlate("pneumaticcraft.gui.tab.status"), new ItemStack(ModBlocks.PRESSURIZED_SPAWNER.get()), 0xFF4E4066, false);
    }

    @Override
    public void tick() {
        super.tick();

        infoStat.setText(ImmutableList.of(
            xlate("pneumaticcraft.gui.tab.status.pressurizedSpawner.spawnRate", te.getSpawnInterval()),
            xlate("pneumaticcraft.gui.tab.status.pressurizedSpawner.airUsage", te.getAirUsage())
        ));
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return Textures.GUI_PRESSURIZED_SPAWNER;
    }

    @Override
    protected PointXY getGaugeLocation() {
        int xStart = (width - imageWidth) / 2;
        int yStart = (height - imageHeight) / 2;
        return new PointXY(xStart + (int)(imageWidth * 0.82), yStart + imageHeight / 4);
    }

    @Override
    protected void addProblems(List<ITextComponent> curInfo) {
        super.addProblems(curInfo);
        if (te.problem == TileEntityVacuumTrap.Problems.NO_CORE) {
            curInfo.addAll(GuiUtils.xlateAndSplit("pneumaticcraft.gui.tab.problems.pressurized_spawner.no_core"));
        }
    }
}
