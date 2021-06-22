package me.desht.pneumaticcraft.api;

import me.desht.pneumaticcraft.api.client.IClientRegistry;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IPneumaticHelmetRegistry;
import me.desht.pneumaticcraft.api.crafting.IPneumaticRecipeRegistry;
import me.desht.pneumaticcraft.api.crafting.ingredient.FluidIngredient;
import me.desht.pneumaticcraft.api.drone.IDroneRegistry;
import me.desht.pneumaticcraft.api.fuel.IFuelRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatRegistry;
import me.desht.pneumaticcraft.api.item.IItemRegistry;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachineFactory;
import me.desht.pneumaticcraft.api.universal_sensor.ISensorRegistry;
import me.desht.pneumaticcraft.lib.Names;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * This class can be used to register and access various things to and from the mod.  All access is via
 * {@link PneumaticRegistry#getInstance()}
 */
public final class PneumaticRegistry {
    public static final String MOD_ID = "pneumaticcraft";

    private static IPneumaticCraftInterface instance;

    /**
     * Get an instance of the top-level API interface.
     *
     * @return the top-level API interface
     */
    public static IPneumaticCraftInterface getInstance() {
        return instance;
    }

    /**
     * Do not call this method yourself.  For PneumaticCraft internal usage only!
     * @param inter reference to the API interface object
     */
    public static void init(IPneumaticCraftInterface inter) {
        if (instance == null && ModLoadingContext.get().getActiveContainer().getModId().equals(Names.MOD_ID))
            instance = inter;//only allow initialization once; by PneumaticCraft
        else throw new IllegalStateException("Only pneumaticcraft is allowed to call this method!");
    }

    /**
     * Retrieve an instance of this via {@link PneumaticRegistry#getInstance()}
     */
    public interface IPneumaticCraftInterface {

        IPneumaticRecipeRegistry getRecipeRegistry();

        IAirHandlerMachineFactory getAirHandlerMachineFactory();

        IPneumaticHelmetRegistry getHelmetRegistry();

        IDroneRegistry getDroneRegistry();

        IHeatRegistry getHeatRegistry();

        IClientRegistry getGuiRegistry();

        ISensorRegistry getSensorRegistry();

        IItemRegistry getItemRegistry();

        IFuelRegistry getFuelRegistry();

        /**
         * Returns the number of Security Stations that disallow interaction with the given coordinate for the given
         * player. Usually you'd disallow interaction when this returns > 0.
         *
         * @param player the player who is trying to access the block
         * @param pos blockpos of the block being tested
         * @param showRangeLines this is ignored and will disappear in a future release
         * @return the number of Security Stations that disallow interaction for the given player.
         * @throws IllegalArgumentException when called from the client side
         * @deprecated use {@link #getProtectingSecurityStations(PlayerEntity, BlockPos)}
         */
        @Deprecated
        int getProtectingSecurityStations(PlayerEntity player, BlockPos pos, boolean showRangeLines);

        /**
         * Returns the number of Security Stations that disallow interaction with the given coordinate for the given
         * player. Usually you'd disallow interaction when this returns > 0.
         *
         * @param player the player who is trying to access the block
         * @param pos blockpos of the block being tested
         * @return the number of Security Stations that disallow interaction for the given player.
         * @throws IllegalArgumentException when called from the client side
         */
        int getProtectingSecurityStations(PlayerEntity player, BlockPos pos);

        /**
         * Register a fluid that represents liquid XP (e.g. PneumaticCraft Memory Essence, CoFH Essence of
         * Knowledge, or OpenBlocks Liquid XP). This is used in the Aerial Interface to transfer experience to/from
         * the player. See also {@link #registerXPFluid(FluidIngredient, int)}.
         *
         * @param fluid the fluid to register
         * @param liquidToPointRatio the amount of fluid (in mB) for one XP point; use a value of 0 or less to
         *                          unregister this fluid
         * @deprecated use {@link #registerXPFluid(FluidIngredient, int)}
         */
        @Deprecated
        void registerXPFluid(Fluid fluid, int liquidToPointRatio);

        /**
         * Register a fluid ingredient that represents liquid XP. This ingredient could be a fluid, or a fluid tag,
         * or even a stream of fluid ingredients.
         *
         * Note that a fluid ingredient of the "forge:experience" fluid tag is registered by default with a ratio of
         * 20mb per XP; this tag includes PneumaticCraft Memory Essence, and possibly other modded XP fluids too.
         *
         * @param fluid the fluid tag to register; all fluids in this tag will have the given XP value
         * @param liquidToPointRatio the amount of fluid (in mB) for one XP point; use a value of 0 or less to
         *                          unregister all fluids matching this fluid ingredient
         */
        void registerXPFluid(FluidIngredient fluid, int liquidToPointRatio);

        /**
         * Convenience method to get a resource location in PneumaticCraft: Repressurized's namespace.
         *
         * @param path a path
         * @return a resource location
         */
        ResourceLocation RL(String path);

        /**
         * Sync a global variable from server to client for the given player. Primarily intended for use by
         * {@link me.desht.pneumaticcraft.api.item.IPositionProvider#syncVariables(ServerPlayerEntity, ItemStack)}
         *
         * @param player the player to sync to
         * @param varName the global variable name (with or without the leading '#')
         */
        void syncGlobalVariable(ServerPlayerEntity player, String varName);
    }

}
