package me.desht.pneumaticcraft.api.client.pneumatic_helmet;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.api.lib.Names;
import me.desht.pneumaticcraft.api.pneumatic_armor.IArmorUpgradeHandler;
import me.desht.pneumaticcraft.api.pneumatic_armor.ICommonArmorHandler;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Represents the client-specific part of an armor upgrade handler; provides methods for rendering, getting the
 * configuration GUI page, reading/writing client-side configuration, and handling keybinds. It's recommended to extend
 * {@link AbstractHandler} or {@link SimpleToggleableHandler} rather than implement this interface directly.
 */
public interface IArmorUpgradeClientHandler<T extends IArmorUpgradeHandler<?>> {
    /**
     * Get the common handler corresponding to this client handler. There is always a one-to-mapping between common
     * and client handlers.
     */
    T getCommonHandler();

    /**
     * This is called when a {@link net.minecraftforge.fml.config.ModConfig.ModConfigEvent} is received for the mod.
     */
    default void initConfig() {}

    /**
     * When called this should save the settings to config.
     */
    default void saveToConfig() {}

    /**
     * This method is called every client tick, and should be used to update clientside logic for armor upgrades.
     * Unlike {@link IArmorUpgradeHandler#tick(ICommonArmorHandler, boolean)}, this method is only called for upgrades
     * which are actually enabled (or not toggleable).
     *
     * @param armorHandler common armor handler for the player wearing this armor piece
     */
    void tickClient(ICommonArmorHandler armorHandler);

    /**
     * Called in the 3D render stage (via {@link net.minecraftforge.client.event.RenderWorldLastEvent})
     *
     * @param matrixStack the matrix stack
     * @param buffer the render type buffer
     * @param partialTicks partial ticks since last world tick
     */
    void render3D(MatrixStack matrixStack, IRenderTypeBuffer buffer, float partialTicks);

    /**
     * Called in the 2D render stage (via {@link net.minecraftforge.client.event.RenderGameOverlayEvent.Post})
     *
     * @param matrixStack the matrix stack
     * @param partialTicks partial ticks since last world tick
     * @param armorPieceHasPressure true if the armor piece actually has any pressure
     */
    void render2D(MatrixStack matrixStack, float partialTicks, boolean armorPieceHasPressure);

    /**
     * You can return a {@link IGuiAnimatedStat} here, which the HUD Handler will pick up and render. It also
     * automatically opens and closes the stat window as necessary.
     *
     * @return the animated stat, or null if this upgrade doesn't use/require a stat window
     */
    default IGuiAnimatedStat getAnimatedStat() {
        return null;
    }

    /**
     * Called when (re-)equipping the armor piece.  Use this to clear any client-side state information held by the
     * upgrade handler and initialise it to a known state.
     */
    void reset();

    /**
     * When you have some configurable options for your upgrade handler, return a new instance of an {@link IOptionPage}.
     * When you do so, it will automatically get picked up by the armor GUI handler, and a button for the upgrade
     * will be displayed in the main armor GUI.
     *
     * @param screen an instance of the gui Screen object
     * @return an options page, or null if the upgrade does not have an options page
     */
    IOptionPage getGuiOptionsPage(IGuiScreen screen);

    /**
     * Called when the screen resolution has changed. Primarily intended to allow render handlers to recalculate
     * stat positions.
     */
    default void onResolutionChanged() {
    }

    /**
     * Is this upgrade toggleable, i.e. can it be switched on & off?  Toggleable upgrades will have a checkbox in their
     * GUI page with a possible associated keybinding. Non-toggleable upgrades generally have a bindable hotkey to
     * trigger a one-off action (e.g. hacking, chestplate launcher...).  The default return value for this method is
     * true, which is the most common case.  Override to return false for non-toggleable upgrades.
     *
     * @return true if the upgrade is toggleable, false otherwise
     */
    default boolean isToggleable() {
        return true;
    }

    /**
     * Get the default keybinding for toggling this upgrade on/off. By default, an unbound key binding will be
     * registered for the upgrade, so it appears in the vanilla Config -> Controls screen with no binding. Note that only
     * toggles are added here; keybinds for non-toggleable upgrade which trigger specific actions (e.g. the
     * Chestplate Launcher or Drone Debugging key) need to be registered explicitly.
     * <p>
     * You should not override this default implementation. Non-toggleable upgrades return {@code Optional.empty()}
     * by default.
     *
     * @return the default key binding for this upgrade
     */
    default Optional<KeyBinding> getInitialKeyBinding() {
        return isToggleable() ?
                Optional.of(new KeyBinding(IArmorUpgradeHandler.getStringKey(getCommonHandler().getID()),
                        KeyConflictContext.IN_GAME, KeyModifier.NONE,
                        InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, getKeybindCategory())) :
                Optional.empty();
    }

    /**
     * Get all the sub-keybinds for this upgrade handler. Any checkboxes which toggle a sub-feature of this upgrade
     * (e.g. the various Block Tracker categories, or the Jet Boots builder mode) need to be returned here so a key
     * binding can be registered for them.
     *
     * @return a collection of ID's
     */
    default Collection<ResourceLocation> getSubKeybinds() {
        return Collections.emptyList();
    }

    /**
     * Get the keybind used to trigger this upgrade's action, if any. This is distinct from the toggle keybind (which
     * switches an upgrade on or off); the trigger keybind triggers an action, e.g. Hacking, Pneumatic Kick...
     *
     * @return an optional keybinding name
     */
    default Optional<KeyBinding> getTriggerKeyBinding() {
        return Optional.empty();
    }

    /**
     * Called when the registered triggered keybind (if any) is pressed.
     * @param armorHandler the client-side common armor handler object for the player
     */
    default void onTriggered(ICommonArmorHandler armorHandler) {
    }

    /**
     * Get the keybind category for this upgrade.  By default, this is the same as the default category for all
     * PneumaticCraft keybinds.
     *
     * @return a keybind category ID
     */
    default String getKeybindCategory() {
        return Names.PNEUMATIC_KEYBINDING_CATEGORY_UPGRADE_TOGGLES;
    }

    /**
     * Get the keybind category for any sub-keybinds.  By default, this is the same as the default category for all
     * PneumaticCraft keybinds.
     *
     * @return a keybind category ID
     */
    default String getSubKeybindCategory() {
        return Names.PNEUMATIC_KEYBINDING_CATEGORY_UPGRADE_TOGGLES;
    }

    /**
     * Called when the player alters their eyepiece color in the Pneumatic Armor GUI "Colors..." screen to re-color any
     * stat this client handler displays.  The default implementation works for most cases, but if your handler displays
     * extra stats (like the Entity or Block tracker does), override this method to re-color them too.
     *
     * @param color the new color for the stat display, as chosen by the player
     */
    default void setOverlayColor(int color) {
        IGuiAnimatedStat stat = getAnimatedStat();
        if (stat != null) stat.setBackgroundColor(color);
    }

    /**
     * Convenience class which allows a reference to the common upgrade handler to be passed in and retrieved.
     */
    abstract class AbstractHandler<T extends IArmorUpgradeHandler<?>> implements IArmorUpgradeClientHandler<T> {
        private final T commonHandler;

        public AbstractHandler(T commonHandler) {
            this.commonHandler = commonHandler;
        }

        @Override
        public T getCommonHandler() {
            return commonHandler;
        }
    }

    /**
     * Convenience class for simple toggleable armor features with no additional settings.
     */
    abstract class SimpleToggleableHandler<T extends IArmorUpgradeHandler<?>> extends AbstractHandler<T> {
        public SimpleToggleableHandler(T commonHandler) {
            super(commonHandler);
        }

        @Override
        public void tickClient(ICommonArmorHandler armorHandler) {
        }

        @Override
        public void render3D(MatrixStack matrixStack, IRenderTypeBuffer buffer, float partialTicks) {
        }

        @Override
        public void render2D(MatrixStack matrixStack, float partialTicks, boolean armorPieceHasPressure) {
        }

        @Override
        public void reset() {
        }

        @Override
        public IOptionPage getGuiOptionsPage(IGuiScreen screen) {
            return new IOptionPage.SimpleOptionPage<>(screen, this);
        }
    }
}
