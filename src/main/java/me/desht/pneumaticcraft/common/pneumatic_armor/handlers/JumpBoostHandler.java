package me.desht.pneumaticcraft.common.pneumatic_armor.handlers;

import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.api.pneumatic_armor.BaseArmorUpgradeHandler;
import me.desht.pneumaticcraft.api.pneumatic_armor.IArmorExtensionData;
import me.desht.pneumaticcraft.api.pneumatic_armor.ICommonArmorHandler;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.ResourceLocation;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;

public class JumpBoostHandler extends BaseArmorUpgradeHandler<IArmorExtensionData> {
    @Override
    public ResourceLocation getID() {
        return RL("jump_boost");
    }

    @Override
    public EnumUpgrade[] getRequiredUpgrades() {
        return new EnumUpgrade[] { EnumUpgrade.JUMPING };
    }

    @Override
    public float getIdleAirUsage(ICommonArmorHandler armorHandler) {
        return 0;
    }

    @Override
    public EquipmentSlotType getEquipmentSlot() {
        return EquipmentSlotType.LEGS;
    }
}
