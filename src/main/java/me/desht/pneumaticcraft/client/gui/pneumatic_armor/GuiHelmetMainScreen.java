package me.desht.pneumaticcraft.client.gui.pneumatic_armor;

import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IGuiScreen;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IOptionPage;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.client.gui.GuiPneumaticScreenBase;
import me.desht.pneumaticcraft.client.gui.widget.WidgetButtonExtended;
import me.desht.pneumaticcraft.client.gui.widget.WidgetKeybindCheckBox;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.UpgradeRenderHandlerList;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler.MainHelmetHandler;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiHelmetMainScreen extends GuiPneumaticScreenBase implements IGuiScreen {
    private static final String TITLE_PREFIX = TextFormatting.AQUA + "" + TextFormatting.UNDERLINE;

    public static final ItemStack[] ARMOR_STACKS = new ItemStack[]{
            new ItemStack(ModItems.PNEUMATIC_BOOTS.get()),
            new ItemStack(ModItems.PNEUMATIC_LEGGINGS.get()),
            new ItemStack(ModItems.PNEUMATIC_CHESTPLATE.get()),
            new ItemStack(ModItems.PNEUMATIC_HELMET.get())
    };
    private final List<UpgradeOption> upgradeOptions = new ArrayList<>();
    private static int pageNumber;
    private boolean inInitPhase = true;

    // A static instance which can handle keybinds when the GUI is closed.
    private static GuiHelmetMainScreen instance;

    private GuiHelmetMainScreen() {
        super(new StringTextComponent("Main Screen"));
    }

    public static GuiHelmetMainScreen getInstance() {
        return instance;
    }

    public static void initHelmetMainScreen() {
        if (instance == null) {
            instance = new GuiHelmetMainScreen();
            MainWindow mw = Minecraft.getInstance().getMainWindow();
            int width = mw.getScaledWidth();
            int height = mw.getScaledHeight();
            instance.init(Minecraft.getInstance(), width, height);  // causes init() to be called

            for (int i = 1; i < instance.upgradeOptions.size(); i++) {
                pageNumber = i;
                instance.init();
            }
            pageNumber = 0;
            instance.inInitPhase = false;
        }
    }

    @Override
    public void init() {
        super.init();

        buttons.clear();
        children.clear();
        upgradeOptions.clear();
        addPages();
        for (int i = 0; i < upgradeOptions.size(); i++) {
            final int idx = i;
            WidgetButtonExtended button = new WidgetButtonExtended(210, 20 + i * 22, 120, 20,
                    upgradeOptions.get(i).page.getPageName(), b -> setPage(idx));
            button.setRenderStacks(upgradeOptions.get(i).icons);
            button.setIconPosition(WidgetButtonExtended.IconPosition.RIGHT);
            if (pageNumber == i) button.active = false;
            addButton(button);
        }
        if (pageNumber > upgradeOptions.size() - 1) {
            pageNumber = upgradeOptions.size() - 1;
        }
        String keybindName = upgradeOptions.get(pageNumber).upgradeName;
        WidgetKeybindCheckBox checkBox = new WidgetKeybindCheckBox(40, 25, 0xFFFFFFFF, keybindName, null);
        if (upgradeOptions.get(pageNumber).page.isToggleable()) {
            addButton(checkBox);
        }
        upgradeOptions.get(pageNumber).page.populateGui(this);
    }

    private void setPage(int newPage) {
        pageNumber = newPage;
        init();
    }

    @Override
    protected ResourceLocation getTexture() {
        return null;
    }

    private void addPages() {
        for (EquipmentSlotType slot : UpgradeRenderHandlerList.ARMOR_SLOTS) {
            List<IUpgradeRenderHandler> renderHandlers = UpgradeRenderHandlerList.instance().getHandlersForSlot(slot);
            for (int i = 0; i < renderHandlers.size(); i++) {
                if (inInitPhase || CommonArmorHandler.getHandlerForPlayer().isUpgradeRendererInserted(slot, i)) {
                    IUpgradeRenderHandler upgradeRenderHandler = renderHandlers.get(i);
                    if (inInitPhase
                            || ItemPneumaticArmor.isPneumaticArmorPiece(Minecraft.getInstance().player, slot)
                            || upgradeRenderHandler instanceof MainHelmetHandler) {
                        IOptionPage optionPage = upgradeRenderHandler.getGuiOptionsPage(this);
                        if (optionPage != null) {
                            List<ItemStack> stacks = new ArrayList<>();
                            stacks.add(ARMOR_STACKS[upgradeRenderHandler.getEquipmentSlot().getIndex()]);
                            Arrays.stream(upgradeRenderHandler.getRequiredUpgrades()).map(EnumUpgrade::getItemStack).forEach(stacks::add);
                            upgradeOptions.add(new UpgradeOption(optionPage, upgradeRenderHandler.getUpgradeID(), stacks.toArray(new ItemStack[0])));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void render(int x, int y, float partialTicks) {
        renderBackground();
        IOptionPage optionPage = upgradeOptions.get(pageNumber).page;
        optionPage.renderPre(x, y, partialTicks);
        drawCenteredString(font, TITLE_PREFIX + upgradeOptions.get(pageNumber).page.getPageName(), 100, 12, 0xFFFFFFFF);
        if (optionPage.displaySettingsHeader()) {
            drawCenteredString(font, "Settings", 100, optionPage.settingsYposition(), 0xFFFFFFFF);
        }
        super.render(x, y, partialTicks);
        optionPage.renderPost(x, y, partialTicks);
    }

    @Override
    public void tick() {
        super.tick();

        IOptionPage optionPage = upgradeOptions.get(pageNumber).page;
        optionPage.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return upgradeOptions.get(pageNumber).page.keyPressed(keyCode, scanCode, modifiers)
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return upgradeOptions.get(pageNumber).page.mouseClicked(mouseX, mouseY, mouseButton)
                || super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dir) {
        return upgradeOptions.get(pageNumber).page.mouseScrolled(mouseX, mouseY, dir)
                || super.mouseScrolled(mouseX, mouseY, dir);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return upgradeOptions.get(pageNumber).page.mouseDragged(mouseX, mouseY, button, dragX, dragY)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public <T extends Widget> T addWidget(T w) {
        return addButton(w);
    }

    @Override
    public List<Widget> getWidgetList() {
        return buttons;
    }

    @Override
    public FontRenderer getFontRenderer() {
        return font;
    }

    @Override
    public void setFocusedWidget(Widget w) {
        setFocused(w);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class UpgradeOption {
        private final IOptionPage page;
        private final String upgradeName;
        private final ItemStack[] icons;

        UpgradeOption(IOptionPage page, String upgradeName, ItemStack... icons) {
            this.page = page;
            this.upgradeName = upgradeName;
            this.icons = icons;
        }
    }
}
