package me.desht.pneumaticcraft.common.thirdparty.immersiveengineering;

import me.desht.pneumaticcraft.api.harvesting.HarvestHandler;
import me.desht.pneumaticcraft.api.lib.Names;
import me.desht.pneumaticcraft.common.harvesting.HarvestHandlerCactusLike;
import me.desht.pneumaticcraft.common.thirdparty.IThirdParty;
import me.desht.pneumaticcraft.lib.Log;
import me.desht.pneumaticcraft.lib.ModIds;
import net.minecraft.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ObjectHolder;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;

public class ImmersiveEngineering implements IThirdParty {

    @ObjectHolder("immersiveengineering:hemp")
    private static Block HEMP_BLOCK = null;

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(ElectricAttackHandler.class);
        IEHeatHandler.registerHeatHandler();
        IEIntegration.registerFuels();
    }

    @Mod.EventBusSubscriber(modid = Names.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Listener {
        @SubscribeEvent
        public static void registerHarvestHandler(RegistryEvent.Register<HarvestHandler> event) {
            if (HEMP_BLOCK == null && ModList.get().isLoaded(ModIds.IMMERSIVE_ENGINEERING)) {
                Log.error("block 'immersiveengineering:hemp' did not get registered? PneumaticCraft drone harvesting won't work!");
            }
            event.getRegistry().register(new HarvestHandlerCactusLike(state -> HEMP_BLOCK != null && state.getBlock() == HEMP_BLOCK)
                    .setRegistryName(RL("ie_hemp")));
        }
    }
}
