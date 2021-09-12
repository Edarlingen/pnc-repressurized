package me.desht.pneumaticcraft.client.gui.pneumatic_armor.option_screens;

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.ICheckboxWidget;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IGuiScreen;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IKeybindingButton;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IPneumaticHelmetRegistry;
import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.client.KeyHandler;
import me.desht.pneumaticcraft.client.gui.pneumatic_armor.GuiMoveStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetButtonExtended;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.HUDHandler;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler.JetBootsClientHandler;
import me.desht.pneumaticcraft.client.util.PointXY;
import me.desht.pneumaticcraft.common.config.subconfig.ArmorHUDLayout;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdateArmorExtraData;
import me.desht.pneumaticcraft.common.pneumatic_armor.ArmorUpgradeRegistry;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import me.desht.pneumaticcraft.common.pneumatic_armor.handlers.JetBootsHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.Optional;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;
import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class JetBootsOptions extends AbstractSliderOptions<JetBootsClientHandler> {
    private ICheckboxWidget checkBoxBuilderMode;
    private ICheckboxWidget checkBoxStabilizers;
    private IKeybindingButton changeKeybindingButton;

    public JetBootsOptions(IGuiScreen screen, JetBootsClientHandler upgradeHandler) {
        super(screen, upgradeHandler);
    }

    @Override
    public void populateGui(IGuiScreen gui) {
        super.populateGui(gui);

        IPneumaticHelmetRegistry registry = PneumaticRegistry.getInstance().getHelmetRegistry();
        ResourceLocation ownerID = getClientUpgradeHandler().getCommonHandler().getID();
        checkBoxBuilderMode = registry.makeKeybindingCheckBox(RL("jet_boots.module.builder_mode"), 5, 45, 0xFFFFFFFF,
                b -> setFlag(ItemPneumaticArmor.NBT_BUILDER_MODE, JetBootsHandler.BUILDER_MODE_LEVEL, b))
                .withOwnerUpgradeID(ownerID);
        gui.addWidget(checkBoxBuilderMode.asWidget());
        checkBoxStabilizers = registry.makeKeybindingCheckBox(RL("jet_boots.module.flight_stabilizers"), 5, 65, 0xFFFFFFFF,
                b -> setFlag(ItemPneumaticArmor.NBT_FLIGHT_STABILIZERS, JetBootsHandler.STABLIZERS_LEVEL, b))
                .withOwnerUpgradeID(ownerID);
        gui.addWidget(checkBoxStabilizers.asWidget());
        ICheckboxWidget hoverControl = registry.makeKeybindingCheckBox(RL("jet_boots.module.smart_hover"), 5, 85, 0xFFFFFFFF,
                b -> setFlag(ItemPneumaticArmor.NBT_SMART_HOVER, 1, b))
                .withOwnerUpgradeID(ownerID);
        gui.addWidget(hoverControl.asWidget());

        changeKeybindingButton = registry.makeKeybindingButton(135, KeyHandler.getInstance().keybindJetBoots);
        gui.addWidget(changeKeybindingButton.asWidget());

        gui.addWidget(new WidgetButtonExtended(30, 157, 150, 20,
                xlate("pneumaticcraft.armor.gui.misc.moveStatScreen"), b -> {
            Minecraft.getInstance().player.closeContainer();
            Minecraft.getInstance().setScreen(new GuiMoveStat(getClientUpgradeHandler(), ArmorHUDLayout.LayoutType.JET_BOOTS));
        }));
    }

    @Override
    protected PointXY getSliderPos() {
        return new PointXY(30, 105);
    }

    private void setFlag(String flagName, int minTier, ICheckboxWidget cb) {
        CommonArmorHandler commonArmorHandler = CommonArmorHandler.getHandlerForPlayer();
        if (commonArmorHandler.getUpgradeCount(EquipmentSlotType.FEET, EnumUpgrade.JET_BOOTS) >= minTier) {
            CompoundNBT tag = new CompoundNBT();
            tag.putBoolean(flagName, cb.isChecked());
            JetBootsHandler upgradeHandler = getClientUpgradeHandler().getCommonHandler();
            NetworkHandler.sendToServer(new PacketUpdateArmorExtraData(EquipmentSlotType.FEET, tag, upgradeHandler.getID()));
            upgradeHandler.onDataFieldUpdated(CommonArmorHandler.getHandlerForPlayer(), flagName, tag.get(flagName));
            ResourceLocation ownerId = upgradeHandler.getID();
            HUDHandler.getInstance().addFeatureToggleMessage(ArmorUpgradeRegistry.getStringKey(ownerId), ArmorUpgradeRegistry.getStringKey(cb.getUpgradeId()), cb.isChecked());
        }
    }

    @Override
    public void tick() {
        super.tick();

        int nUpgrades = CommonArmorHandler.getHandlerForPlayer().getUpgradeCount(EquipmentSlotType.FEET, EnumUpgrade.JET_BOOTS);
        checkBoxBuilderMode.asWidget().active = nUpgrades >= JetBootsHandler.BUILDER_MODE_LEVEL;
        checkBoxStabilizers.asWidget().active = nUpgrades >= JetBootsHandler.STABLIZERS_LEVEL;
    }

    @Override
    protected String getTagName() {
        return ItemPneumaticArmor.NBT_JET_BOOTS_POWER;
    }

    @Override
    protected ITextComponent getPrefix() {
        return new StringTextComponent("Power: ");
    }

    @Override
    protected ITextComponent getSuffix() {
        return new StringTextComponent("%");
    }

    @Override
    public Optional<IKeybindingButton> getKeybindingButton() {
        return Optional.of(changeKeybindingButton);
    }
}
