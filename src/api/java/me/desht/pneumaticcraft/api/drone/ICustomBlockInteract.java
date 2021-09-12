package me.desht.pneumaticcraft.api.drone;

import net.minecraft.item.DyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.RegistryEvent;

/**
 * Implement this and register it with {@link IDroneRegistry#registerCustomBlockInteractor(RegistryEvent.Register, ICustomBlockInteract)}.
 * This will add a puzzle piece that has only an Area white- and blacklist parameter (similar to a Goto piece).
 * This could be used to create energy import/export widgets, for example.
 */
public interface ICustomBlockInteract {

    /**
     * Get a unique name for this puzzle piece. This is a Forge Registry entry so make it unique, and namespaced with
     * your mod's ID.
     *
     * @return a unique ID
     */
    ResourceLocation getID();

    /**
     * Get the puzzle piece texture. Should be a multiple of 80x64 (width x height). I'd recommend starting
     * out by copying the
     * <a href="https://github.com/TeamPneumatic/pnc-repressurized/blob/master/src/main/resources/assets/pneumaticcraft/textures/items/progwidgets/goto_piece.png">Go To widget texture</a>
     *
     * @return a resource location for the texture to be used
     */
    ResourceLocation getTexture();

    /**
     * The actual interaction.
     * <p>
     * For each blockpos in the specified area, the drone will visit that block (ordered from closest to furthest). It
     * will call this method with {@code simulate} = true. If this method returns true, the drone will navigate to this
     * location, and call this method again with {@code simulate} = false. It will keep doing this until this method
     * returns false.
     * <p>
     * In the puzzle piece GUI, players can specify a 'use count' and fill in the maximum count they want
     * to use. When {@link IBlockInteractHandler#useCount()} returns true, and {@code simulate} is false, you must only
     * import/export up to {@link IBlockInteractHandler#getRemainingCount()}, and you must notify the transferred amount
     * by doing {@link IBlockInteractHandler#decreaseCount(int)}.
     *
     * @param pos current visited location
     * @param drone a reference to the drone object
     * @param interactHandler object you can use to use to get accessible sides and give feedback about counts.
     * @param simulate  true when trying to figure out whether or not the drone should navigate to this block,
     *                  false when next to this block.
     * @return true if the interaction was (would be) successful
     */
    boolean doInteract(BlockPos pos, IDrone drone, IBlockInteractHandler interactHandler, boolean simulate);

    /**
     * Used for crafting, categorizes the puzzle piece.
     *
     * @return a color
     */
    DyeColor getColor();
}
