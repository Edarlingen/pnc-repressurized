package me.desht.pneumaticcraft.common.thirdparty.waila;

import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import me.desht.pneumaticcraft.api.misc.IPneumaticCraftProbeable;
import me.desht.pneumaticcraft.common.entity.semiblock.EntitySemiblockBase;
import me.desht.pneumaticcraft.common.tileentity.ICamouflageableTE;
import me.desht.pneumaticcraft.common.tileentity.IRedstoneControl;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPressureTube;
import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.tileentity.TileEntity;

@WailaPlugin
public class WailaRegistration implements IWailaPlugin {
    @Override
    public void register(IRegistrar iRegistrar) {
        iRegistrar.registerBlockDataProvider(new PneumaticProvider.Data(), IPneumaticCraftProbeable.class);
        iRegistrar.registerBlockDataProvider(new SemiblockProvider.Data(), Block.class);
        iRegistrar.registerBlockDataProvider(new RedstoneControlProvider.Data(), IRedstoneControl.class);
        iRegistrar.registerBlockDataProvider(new TubeModuleProvider.Data(), TileEntityPressureTube.class);
        iRegistrar.registerEntityDataProvider(new EntityProvider.Data(), LivingEntity.class);
        iRegistrar.registerEntityDataProvider(new EntityProvider.Data(), EntitySemiblockBase.class);

        iRegistrar.registerComponentProvider(new PneumaticProvider.Component(), TooltipPosition.BODY, TileEntity.class);
        iRegistrar.registerComponentProvider(new SemiblockProvider.Component(), TooltipPosition.BODY, Block.class);
        iRegistrar.registerComponentProvider(new RedstoneControlProvider.Component(), TooltipPosition.BODY, IRedstoneControl.class);
        iRegistrar.registerComponentProvider(new TubeModuleProvider.Component(), TooltipPosition.BODY, TileEntityPressureTube.class);
        iRegistrar.registerComponentProvider(new EntityProvider.Component(), TooltipPosition.BODY, LivingEntity.class);
        iRegistrar.registerComponentProvider(new EntityProvider.Component(), TooltipPosition.HEAD, EntitySemiblockBase.class);
        iRegistrar.registerComponentProvider(new EntityProvider.Component(), TooltipPosition.BODY, EntitySemiblockBase.class);
        iRegistrar.registerComponentProvider(new EntityProvider.Component(), TooltipPosition.TAIL, EntitySemiblockBase.class);
        iRegistrar.registerComponentProvider(new CamoProvider.Component(), TooltipPosition.BODY, ICamouflageableTE.class);
    }
}
