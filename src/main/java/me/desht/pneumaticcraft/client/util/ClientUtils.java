package me.desht.pneumaticcraft.client.util;

import me.desht.pneumaticcraft.client.gui.GuiPneumaticContainerBase;
import me.desht.pneumaticcraft.client.gui.programmer.GuiProgWidgetOptionBase;
import me.desht.pneumaticcraft.client.pneumatic_armor.ArmorUpgradeClientRegistry;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler.EntityTrackerClientHandler;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.pneumatic_armor.ArmorUpgradeRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.IParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Miscellaneous client-side utilities.  Used to wrap client-only code in methods safe to call from classes that could
 * be loaded on dedicated server (mainly packet handlers & event handlers, but could be anywhere...)
 */
public class ClientUtils {
    /**
     * Emit particles from just above the given blockpos, which is generally a machine or similar.
     * Only call this clientside.
     *
     * @param world the world
     * @param pos the block pos
     * @param particle the particle type
     */
    public static void emitParticles(World world, BlockPos pos, IParticleData particle, double yOffset) {
        float xOff = world.random.nextFloat() * 0.6F + 0.2F;
        float zOff = world.random.nextFloat() * 0.6F + 0.2F;
        getClientWorld().addParticle(particle,
                pos.getX() + xOff, pos.getY() + yOffset, pos.getZ() + zOff,
                0, 0, 0);
    }

    public static void emitParticles(World world, BlockPos pos, IParticleData particle) {
        emitParticles(world, pos, particle, 1.2);
    }

    @Nonnull
    public static ItemStack getWornArmor(EquipmentSlotType slot) {
        return Minecraft.getInstance().player.getItemBySlot(slot);
    }

    public static void addDroneToHudHandler(EntityDrone drone, BlockPos pos) {
        ArmorUpgradeClientRegistry.getInstance()
                .getClientHandler(ArmorUpgradeRegistry.getInstance().entityTrackerHandler, EntityTrackerClientHandler.class)
                .getTargetsStream()
                .filter(target -> target.entity == drone)
                .forEach(target -> target.getDroneAIRenderer().addBlackListEntry(drone.level, pos));
    }

    public static boolean isKeyDown(int keyCode) {
        return InputMappings.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keyCode);
    }

    /**
     * Open a container-based GUI client-side. This is a cheeky hack, but appears to work. However, it is important
     * to call {@link ClientUtils#closeContainerGui(Screen)} from the opened GUI's {@code onClose()} method to restore
     * the player's openContainer to the correct container. Therefore the GUI being opened should remember the previous
     * open GUI, and call {@link ClientUtils#closeContainerGui(Screen)} with that GUI as an argument.
     *
     * @param type the container type to open
     * @param displayString container's display name
     */
    public static void openContainerGui(ContainerType<? extends Container> type, ITextComponent displayString) {
        ScreenManager.create(type, Minecraft.getInstance(), -1, displayString);
    }

    /**
     * Close a container-based GUI, and restore the player's openContainer. See {@link ClientUtils#openContainerGui(ContainerType, ITextComponent)}
     *
     * @param parentScreen the previously-opened GUI, which will be re-opened
     */
    public static void closeContainerGui(Screen parentScreen) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(parentScreen);
        if (parentScreen instanceof ContainerScreen) {
            mc.player.containerMenu = ((ContainerScreen<?>) parentScreen).getMenu();
        } else if (parentScreen instanceof GuiProgWidgetOptionBase) {
            mc.player.containerMenu = ((GuiProgWidgetOptionBase<?>) parentScreen).getProgrammerContainer();
        }
    }

    /**
     * For use where we can't reference Minecraft directly, e.g. packet handling code.
     * @return the client world
     */
    public static World getClientWorld() {
        return Minecraft.getInstance().level;
    }

    public static PlayerEntity getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    public static boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }

    /**
     * Get a TE client-side.  Convenience method for packet handling code, primarily.
     * @return a tile entity or null
     */
    public static TileEntity getClientTE(BlockPos pos) {
        return Minecraft.getInstance().level.getBlockEntity(pos);
    }

    /**
     * Same as AWT Rectangle's intersects() method, but we don't have access to AWT...
     *
     * @param rect a rectangle
     * @param x x coord
     * @param y y coord
     * @param w width
     * @param h height
     * @return true if intersection, false otherwise
     */
    public static boolean intersects(Rectangle2d rect, double x, double y, double w, double h) {
        if (rect.getWidth() <= 0 || rect.getHeight() <= 0 || w <= 0 || h <= 0) {
            return false;
        }
        double x0 = rect.getX();
        double y0 = rect.getY();
        return (x + w > x0 &&
                y + h > y0 &&
                x < x0 + rect.getWidth() &&
                y < y0 + rect.getHeight());
    }

    /**
     * For the programmer GUI
     *
     * @return true if the screen res > 700x512
     */
    public static boolean isScreenHiRes() {
        MainWindow mw = Minecraft.getInstance().getWindow();
        return mw.getGuiScaledWidth() > 700 && mw.getGuiScaledHeight() > 512;
    }

    public static float getBrightnessAtWorldHeight() {
        PlayerEntity player = getClientPlayer();
        BlockPos pos = new BlockPos.Mutable(player.getX(), getClientWorld().getMaxBuildHeight(), player.getZ());
        if (player.level.hasChunkAt(pos)) {
            return player.level.dimensionType().brightness(player.level.getMaxLocalRawBrightness(pos));
        } else {
            return 0.0F;
        }
    }

    public static int getLightAt(BlockPos pos) {
        return WorldRenderer.getLightColor(Minecraft.getInstance().level, pos);
    }

    public static int getStringWidth(String line) {
        return Minecraft.getInstance().getEntityRenderDispatcher().getFont().width(line);
    }

    public static boolean isGuiOpen(TileEntity te) {
        if (Minecraft.getInstance().screen instanceof GuiPneumaticContainerBase) {
            return ((GuiPneumaticContainerBase<?,?>) Minecraft.getInstance().screen).te == te;
        } else {
            return false;
        }
    }

    public static float[] getTextureUV(BlockState state, Direction face) {
        if (state == null) return null;
        IBakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        List<BakedQuad> quads = model.getQuads(state, face, Minecraft.getInstance().level.random, EmptyModelData.INSTANCE);
        if (!quads.isEmpty()) {
            TextureAtlasSprite sprite = quads.get(0).getSprite();
            return new float[] { sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1() };
        } else {
            return null;
        }
    }

    public static void spawnEntityClientside(Entity e) {
        ((ClientWorld) getClientWorld()).putNonPlayerEntity(e.getId(), e);
    }

    public static String translateDirection(Direction d) {
        return I18n.get("pneumaticcraft.gui.tooltip.direction." + d.toString());
    }

    public static ITextComponent translateDirectionComponent(Direction d) {
        return new TranslationTextComponent("pneumaticcraft.gui.tooltip.direction." + d.toString());
    }

    /**
     * Because keyBinding.getTranslationKey() doesn't work that well...
     *
     * @param keyBinding the keybinding
     * @return a human-friendly string representation
     */
    public static ITextComponent translateKeyBind(KeyBinding keyBinding) {
        return keyBinding.getKeyModifier().getCombinedName(keyBinding.getKey(), () -> {
            ITextComponent s = keyBinding.getKey().getDisplayName();
            // small kludge to clearly distinguish keypad from non-keypad keys
            if (keyBinding.getKey().getType() == InputMappings.Type.KEYSYM
                    && keyBinding.getKey().getValue() >= GLFW.GLFW_KEY_KP_0
                    && keyBinding.getKey().getValue() <= GLFW.GLFW_KEY_KP_EQUAL) {
                return new StringTextComponent("KP_").append(s);
            } else {
                return s;
            }
        }).copy().withStyle(TextFormatting.YELLOW);
    }

    /**
     * Add some context-sensitive info to an item's tooltip, based on the currently-open GUI.
     *
     * @param stack the item stack
     * @param tooltip tooltip to add data to
     */
    public static void addGuiContextSensitiveTooltip(ItemStack stack, List<ITextComponent> tooltip) {
        Screen screen = Minecraft.getInstance().screen;

        if (screen != null) {
            String subKey = screen.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            Item item = stack.getItem();
            String base = item instanceof BlockItem ? "gui.tooltip.block" : "gui.tooltip.item";
            String k = String.join(".", base, item.getRegistryName().getNamespace(), item.getRegistryName().getPath(), subKey);
            if (I18n.exists(k)) {
                tooltip.addAll(GuiUtils.xlateAndSplit(k).stream().map(s -> s.copy().withStyle(TextFormatting.GRAY)).collect(Collectors.toList()));
            }
        }
    }

    /**
     * Get the render distance based on current game settings
     *
     * @return the squared render distance, in blocks
     */
    public static int getRenderDistanceThresholdSq() {
        int d = Minecraft.getInstance().options.renderDistance * 16;
        return d * d;
    }

    public static boolean isFirstPersonCamera() {
        return Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

}
