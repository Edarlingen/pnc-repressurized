package me.desht.pneumaticcraft.api.client.pneumatic_helmet;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.desht.pneumaticcraft.api.pneumatic_armor.IArmorUpgradeHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Optional;

/**
 * An Option Page is the GUI object which holds the control widgets for a particular Pneumatic Armor upgrade. Create
 * and return an instance of this class in {@link IArmorUpgradeClientHandler#getGuiOptionsPage(IGuiScreen)}.
 * <p>
 * It is strongly recommended to extend the {@link SimpleOptionPage} class rather than implement this interface directly.
 */
public interface IOptionPage {
    /**
     * Get a reference to the IGuiScreen object.  You can use this to get the font renderer, for example.
     *
     * @return the screen
     */
    IGuiScreen getGuiScreen();

    /**
     * This text is used in the GUI button for this page.
     *
     * @return the page name
     */
    IFormattableTextComponent getPageName();

    /**
     * Here you can initialize your buttons and stuff like with a {@link Screen}.
     *
     * @param gui the holding GUI
     */
    void populateGui(IGuiScreen gui);

    /**
     * Called immediately before {@link Screen#render(MatrixStack, int, int, float)}
     *
     * @param matrixStack the matrix stack
     * @param x mouse X
     * @param y mouse Y
     * @param partialTicks partial ticks since last world ticks
     */
    void renderPre(MatrixStack matrixStack, int x, int y, float partialTicks);

    /**
     * Called immediately after {@link Screen#render(MatrixStack, int, int, float)}
     * Here you can render additional things like text.
     *
     * @param matrixStack the matrix stack
     * @param x mouse X
     * @param y mouse Y
     * @param partialTicks partial ticks since last world ticks
     */
    void renderPost(MatrixStack matrixStack, int x, int y, float partialTicks);

    /**
     * Called by {@link Screen#keyPressed(int, int, int)} when a key is pressed.
     *
     * @param keyCode typed keycode
     * @param scanCode the scan code (rarely useful)
     * @param modifiers key modifiers
     * @return true if the event has been handled, false otherwise
     */
    boolean keyPressed(int keyCode, int scanCode, int modifiers);

    /**
     * Called by {@link Screen#keyReleased(int, int, int)} when a key is released.
     *
     * @param keyCode typed keycode
     * @param scanCode the scan code (rarely useful)
     * @param modifiers key modifiers
     * @return true if the event has been handled, false otherwise
     */
    boolean keyReleased(int keyCode, int scanCode, int modifiers);

    /**
     * Called when mouse is clicked via {@link Screen#mouseClicked(double, double, int)}
     * @param x mouse X
     * @param y mouse Y
     * @param button mouse button
     * @return true if the event has been handled, false otherwise
     */
    boolean mouseClicked(double x, double y, int button);

    /**
     * Called when the mouse wheel is rolled.
     *
     * @param x mouse X
     * @param y mouse Y
     * @param dir scroll direction
     * @return true if the event has been handled, false otherwise
     */
    boolean mouseScrolled(double x, double y, double dir);

    /**
     * Called when the mouse is dragged across the GUI
     * @param mouseX mouse X
     * @param mouseY mouse Y
     * @param button mouse button
     * @param dragX drag X
     * @param dragY drag Y
     * @return true if the event has been handled, false otherwise
     */
    boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY);

    /**
     * Can this upgrade be toggled off & on?  If true, a checkbox (with the ability to bind a key) will be
     * automatically displayed in this upgrade's GUI.
     *
     * @return true if the upgrade is toggleable, false otherwise
     */
    boolean isToggleable();

    /**
     * Should the "Settings" header be displayed?
     *
     * @return true if the header should be displayed, false otherwise
     */
    boolean displaySettingsHeader();

    /**
     * Y position from the "Setting" header.  The default is fine in most cases, but if your options page has
     * many buttons (e.g. like the Block Tracker), you may wish to adjust this.
     *
     * @return Y position, default 115
     */
    default int settingsYposition() { return 115; }

    /**
     * Called immediately after {@link Screen#tick()}
     */
    default void tick() { }

    /**
     * Get the keybinding button for this page, if any.  You can create a keybinding button with
     * {@link IPneumaticHelmetRegistry#makeKeybindingButton(int, KeyBinding)}.
     *
     * @return the keybinding button, or {@code Optional.empty()} if there isn't one
     */
    default Optional<IKeybindingButton> getKeybindingButton() { return Optional.empty(); }

    /**
     * Convenience class for simple armor features with no additional settings.
     */
    class SimpleOptionPage<T extends IArmorUpgradeClientHandler<?>> implements IOptionPage {
        private final IGuiScreen screen;
        private final IFormattableTextComponent name;
        private final T clientUpgradeHandler;

        public SimpleOptionPage(IGuiScreen screen, T clientUpgradeHandler) {
            this.screen = screen;
            this.name = new TranslationTextComponent(IArmorUpgradeHandler.getStringKey(clientUpgradeHandler.getCommonHandler().getID()));
            this.clientUpgradeHandler = clientUpgradeHandler;
        }

        protected T getClientUpgradeHandler() {
            return clientUpgradeHandler;
        }

        @Override
        public IGuiScreen getGuiScreen() {
            return screen;
        }

        @Override
        public IFormattableTextComponent getPageName() {
            return name;
        }

        @Override
        public void populateGui(IGuiScreen gui) {
        }

        @Override
        public void renderPre(MatrixStack matrixStack, int x, int y, float partialTicks) {
        }

        @Override
        public void renderPost(MatrixStack matrixStack, int x, int y, float partialTicks) {
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return getKeybindingButton().map(b -> b.receiveKey(InputMappings.Type.KEYSYM, keyCode)).orElse(false);
        }

        @Override
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            return getKeybindingButton().map(b -> { b.receiveKeyReleased(); return true; }).orElse(false);
        }

        @Override
        public boolean mouseClicked(double x, double y, int button) {
            return getKeybindingButton().map(b -> b.receiveKey(InputMappings.Type.MOUSE, button)).orElse(false);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return false;
        }

        @Override
        public boolean mouseScrolled(double x, double y, double dir) {
            return false;
        }


        @Override
        public boolean isToggleable() {
            return getClientUpgradeHandler().isToggleable();
        }

        @Override
        public boolean displaySettingsHeader() {
            return false;
        }
    }
}
