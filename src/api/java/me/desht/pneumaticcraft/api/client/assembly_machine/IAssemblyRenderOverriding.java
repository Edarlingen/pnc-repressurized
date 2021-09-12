package me.desht.pneumaticcraft.api.client.assembly_machine;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * Provides methods to customize how items are rendered when held by Assembly machines.  You can create an instance
 * of this class and register it via
 * {@link me.desht.pneumaticcraft.api.client.IClientRegistry#registerRenderOverride(IForgeRegistryEntry, IAssemblyRenderOverriding)}
 */
public interface IAssemblyRenderOverriding {
    /**
     * This method is called just before the IO Unit's held stack is rendered. You can modify the passed
     * matrix stack here to rotate the model, for example. You don't need to call .push() or .pop() on the matrix;
     * that is done automatically before and after this method is called.
     * <p>
     * You can also choose to do the whole rendering yourself; in this case, return false to indicate that
     * PneumaticCraft shouldn't render the item at all.
     *
     * @param matrixStack the matrix; only apply transformations to this, <strong>never directly via OpenGL</strong>
     * @param renderedStack itemStack that is being rendered
     * @return true if PneumaticCraft should render the item (after your changes), or false to cancel rendering.
     */
    boolean applyRenderChangeIOUnit(MatrixStack matrixStack, ItemStack renderedStack);

    /**
     * See {@link #applyRenderChangeIOUnit(MatrixStack, ItemStack)}, but for the Assembly Platform.
     *
     * @param matrixStack the matrix; only apply transformations to this, <strong>never directly via OpenGL</strong>
     * @param renderedStack itemStack that is being rendered
     * @return true if PneumaticCraft should render the item (after your changes), or false to cancel rendering.
     */
    boolean applyRenderChangePlatform(MatrixStack matrixStack, ItemStack renderedStack);

    /**
     * Should return the distance the IO Units' claw travels before it grips the stack.
     * By default it's 0.0875F for items and 0.00625F for blocks, 0.09375 when the claw is completely closed.
     *
     * @param renderedStack the ItemStack being rendered
     * @return the claw distance
     */
    float getIOUnitClawShift(ItemStack renderedStack);

    /**
     * Should return the distance the Assembly Platform's claw travels before it grips the stack.
     * By default it's 0.0875F for items and 0.00625F for blocks, 0.09375 when the claw is completely closed.
     *
     * @param renderedStack the ItemStack being rendered
     * @return the claw shift
     */
    float getPlatformClawShift(ItemStack renderedStack);
}
