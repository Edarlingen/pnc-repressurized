/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.common.item;

import me.desht.pneumaticcraft.api.item.IProgrammable;
import me.desht.pneumaticcraft.client.gui.GuiSecurityStationHacking;
import me.desht.pneumaticcraft.common.core.ModItems;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import java.util.List;

public class ItemNetworkComponent extends Item implements IProgrammable {
    private final NetworkComponentType type;

    public enum NetworkComponentType {
        DIAGNOSTIC_SUBROUTINE("diagnostic_subroutine", true),
        NETWORK_API("network_api", false),
        NETWORK_DATA_STORAGE("network_data_storage", false),
        NETWORK_IO_PORT("network_io_port", true),
        NETWORK_REGISTRY("network_registry", true),
        NETWORK_NODE("network_node", true);

        private final String name;
        private final boolean secStationComponent;

        NetworkComponentType(String name, boolean secStationComponent) {
            this.name = name;
            this.secStationComponent = secStationComponent;
        }

        public boolean isSecStationComponent() {
            return secStationComponent;
        }

        public String getRegistryName() {
            return name;
        }
    }

    public ItemNetworkComponent(NetworkComponentType type) {
        super(ModItems.defaultProps());
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, World worldIn, List<ITextComponent> curInfo, ITooltipFlag extraInfo) {
        super.appendHoverText(stack, worldIn, curInfo, extraInfo);

        if (worldIn != null && worldIn.isClientSide) {
            GuiSecurityStationHacking.addExtraHackInfoStatic(curInfo);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.getCommandSenderWorld().isClientSide && canProgram(stack) && stack.hasTag() && stack.getTag().contains(IProgrammable.NBT_WIDGETS)) entity.setExtendedLifetime();
        return false;
    }

    @Override
    public boolean canProgram(ItemStack stack) {
        return type == NetworkComponentType.NETWORK_API || type == NetworkComponentType.NETWORK_DATA_STORAGE;
    }

    @Override
    public boolean usesPieces(ItemStack stack) {
        return type == NetworkComponentType.NETWORK_API;
    }

    @Override
    public boolean showProgramTooltip() {
        return true;
    }

    public static NetworkComponentType getType(ItemStack stack) {
        return stack.getItem() instanceof ItemNetworkComponent ? ((ItemNetworkComponent) stack.getItem()).type : null;
    }
}
