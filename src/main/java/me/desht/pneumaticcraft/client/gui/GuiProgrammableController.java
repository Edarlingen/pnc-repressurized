package me.desht.pneumaticcraft.client.gui;

import com.google.common.collect.ImmutableMap;
import me.desht.pneumaticcraft.client.gui.widget.WidgetAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.WidgetEnergy;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.inventory.ContainerProgrammableController;
import me.desht.pneumaticcraft.common.tileentity.TileEntityProgrammableController;
import me.desht.pneumaticcraft.lib.GuiConstants;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.energy.CapabilityEnergy;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class GuiProgrammableController extends GuiPneumaticContainerBase<ContainerProgrammableController,TileEntityProgrammableController>
        implements IGuiDrone
{
    private static final ItemStack EYE_OFF = new ItemStack(Items.ENDER_EYE);
    private static final ItemStack EYE_ON = new ItemStack(Items.ENDER_EYE);
    static {
        EnchantmentHelper.setEnchantments(ImmutableMap.of(Enchantments.SILK_TOUCH, 1), EYE_ON);
    }

    private WidgetAnimatedStat chunkTab;
    private WidgetCheckBox shouldCharge;
    private WidgetCheckBox chunkloadSelf;
    private WidgetCheckBox chunkloadWork;
    private WidgetCheckBox chunkloadWork3x3;

    public GuiProgrammableController(ContainerProgrammableController container, PlayerInventory inv, ITextComponent displayString) {
        super(container, inv, displayString);
    }

    @Override
    public void init() {
        super.init();

        te.getCapability(CapabilityEnergy.ENERGY).ifPresent(handler -> addButton(new WidgetEnergy(leftPos + 12, topPos + 20, handler)));

        List<ITextComponent> exc = TileEntityProgrammableController.BLACKLISTED_WIDGETS.stream()
                .map(s -> GuiConstants.BULLET + " " + I18n.get("programmingPuzzle." + s.getNamespace() + "." + s.getPath() + ".name"))
                .sorted()
                .map(StringTextComponent::new)
                .collect(Collectors.toList());
        addAnimatedStat(xlate("pneumaticcraft.gui.tab.info.programmable_controller.excluded"),
                new ItemStack(ModItems.DRONE.get()), 0xFFFF5050, true).setText(exc);

        WidgetAnimatedStat chargeTab = addAnimatedStat(xlate("pneumaticcraft.gui.tab.info.programmable_controller.charging"),
                new ItemStack(ModItems.CHARGING_MODULE.get()), 0xFFA0A0A0, false);
        chargeTab.addSubWidget(shouldCharge = new WidgetCheckBox(5, 15, 0x404040, xlate("pneumaticcraft.gui.tab.info.programmable_controller.chargeHeld")).withTag("charging"));
        chargeTab.setMinimumExpandedDimensions(shouldCharge.getWidth(), 35);
        shouldCharge.setTooltipKey("pneumaticcraft.gui.tab.info.programmable_controller.chargeHeld.tooltip");

        chunkTab = addAnimatedStat(xlate("pneumaticcraft.gui.tab.info.programmable_controller.chunkLoading"),
                new ItemStack(Items.ENDER_EYE), 0xFF804080, false);
        chunkTab.addSubWidget(chunkloadSelf = new WidgetCheckBox(5, 15, 0x303030, xlate("pneumaticcraft.gui.tab.info.programmable_controller.chunkLoading.self")).setChecked(te.chunkloadSelf()).withTag("chunkload_self"));
        chunkTab.addSubWidget(chunkloadWork = new WidgetCheckBox(5, 27, 0x303030, xlate("pneumaticcraft.gui.tab.info.programmable_controller.chunkLoading.work")).setChecked(te.chunkloadWorkingChunk()).withTag("chunkload_work"));
        chunkTab.addSubWidget(chunkloadWork3x3 = new WidgetCheckBox(10, 39, 0x303030, xlate("pneumaticcraft.gui.tab.info.programmable_controller.chunkLoading.work_3x3")).setChecked(te.chunkloadWorkingChunk3x3()).withTag("chunkload_work_3x3"));
        chunkTab.setReservedLines(5);
        int w = Math.max(chunkloadSelf.getWidth(), Math.max(chunkloadWork.getWidth(), chunkloadWork3x3.getWidth() + 5));
        chunkTab.setMinimumExpandedDimensions(w, 70);
    }

    @Override
    public void tick() {
        super.tick();

        shouldCharge.checked = te.shouldChargeHeldItem;
        chunkloadWork3x3.active = chunkloadWork.checked;

        int usage = chunkloadSelf.checked ? PneumaticValues.USAGE_PROGRAMMABLE_CONTROLLER_CHUNKLOAD_SELF : 0;
        if (chunkloadWork.checked) usage += chunkloadWork3x3.checked ?
                PneumaticValues.USAGE_PROGRAMMABLE_CONTROLLER_CHUNKLOAD_WORK3 :
                PneumaticValues.USAGE_PROGRAMMABLE_CONTROLLER_CHUNKLOAD_WORK;
        usage += PneumaticValues.USAGE_PROGRAMMABLE_CONTROLLER;
        chunkTab.setText(Collections.singletonList(xlate("pneumaticcraft.gui.tab.info.pneumatic_armor.usage")
                .append(" " + usage + "mL/t")));
        chunkTab.setTexture(chunkloadSelf.checked || chunkloadWork.checked ? EYE_ON : EYE_OFF);
    }

    @Override
    public IDroneBase getDrone() {
        return te;
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return Textures.GUI_PROGRAMMABLE_CONTROLLER;
    }

    @Override
    protected void addProblems(List<ITextComponent> curInfo) {
        super.addProblems(curInfo);

        if (te.getPrimaryInventory().getStackInSlot(0).isEmpty()) {
            curInfo.addAll(GuiUtils.xlateAndSplit("pneumaticcraft.gui.tab.problems.programmableController.noProgram"));
        }
    }
}
