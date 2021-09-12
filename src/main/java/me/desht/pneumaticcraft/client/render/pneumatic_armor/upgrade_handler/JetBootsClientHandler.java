package me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IArmorUpgradeClientHandler;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IGuiScreen;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IOptionPage;
import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.api.pneumatic_armor.ICommonArmorHandler;
import me.desht.pneumaticcraft.client.gui.pneumatic_armor.option_screens.JetBootsOptions;
import me.desht.pneumaticcraft.client.gui.widget.WidgetAnimatedStat;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.HUDHandler;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.config.subconfig.ArmorHUDLayout;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.pneumatic_armor.ArmorUpgradeRegistry;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import me.desht.pneumaticcraft.common.pneumatic_armor.JetBootsStateTracker;
import me.desht.pneumaticcraft.common.pneumatic_armor.handlers.JetBootsHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.gen.Heightmap;

import java.util.Collection;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;
import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class JetBootsClientHandler extends IArmorUpgradeClientHandler.SimpleToggleableHandler<JetBootsHandler> {
    private static final ItemStack PICK = new ItemStack(Items.DIAMOND_PICKAXE);
    private static final ItemStack ROTOR = new ItemStack(ModItems.TURBINE_ROTOR.get());
    private static final ItemStack ELYTRA = new ItemStack(Items.ELYTRA);
    private static final ItemStack FEATHER = new ItemStack(Items.FEATHER);

    private static final String[] HEADINGS = new String[] { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };

    private String l1, l2, l3, r1, r2, r3;
    private int widestR;
    private boolean builderMode;
    private boolean flightStabilizers;
    private boolean smartHover;
    private double prevX, prevY, prevZ;

    private IGuiAnimatedStat jbStat;

    public JetBootsClientHandler() {
        super(ArmorUpgradeRegistry.getInstance().jetBootsHandler);
    }

    @Override
    public IOptionPage getGuiOptionsPage(IGuiScreen screen) {
        return new JetBootsOptions(screen,this);
    }

    @Override
    public void tickClient(ICommonArmorHandler armorHandler) {
        super.tickClient(armorHandler);

        String g1 = TextFormatting.WHITE.toString();
        String g2 = TextFormatting.GREEN.toString();

        PlayerEntity player = armorHandler.getPlayer();
        if (jbStat.isStatOpen()) {
            double mx = player.getX() - prevX;
            double my = player.getY() - prevY;
            double mz = player.getZ() - prevZ;
            prevX = player.getX();
            prevY = player.getY();
            prevZ = player.getZ();
            double v = Math.sqrt(mx * mx + my * my + mz * mz);
            double vg = Math.sqrt(mx * mx + mz * mz);
            int heading = MathHelper.floor((double)(player.yRot * 8.0F / 360.0F) + 0.5D) & 0x7;
            int yaw = ((int) player.yRot + 180) % 360;
            if (yaw < 0) yaw += 360;
            BlockPos pos = player.blockPosition();

            l1 = String.format(" %sSpd: %s%05.2fm/s", g1, g2, v * 20);
            l2 = String.format("  %sAlt: %s%03dm", g1, g2, pos.getY());
            l3 = String.format("%sHead: %s%d° (%s)", g1, g2, yaw, HEADINGS[heading]);
            r1 = String.format("%sGnd: %s%05.2f", g1, g2, vg * 20);
            r2 = String.format("%sGnd: %s%dm", g1, g2, pos.getY() - player.level.getHeight(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ()));
            r3 = String.format("%sPch: %s%d°", g1, g2, (int)-player.xRot);
            FontRenderer fr = Minecraft.getInstance().font;
            widestR = Math.max(fr.width(r1), Math.max(fr.width(r2), fr.width(r3)));

            CommonArmorHandler handler = CommonArmorHandler.getHandlerForPlayer();
            JetBootsStateTracker.JetBootsState jbState = JetBootsStateTracker.getClientTracker().getJetBootsState(player);
            builderMode = jbState.isBuilderMode();

            JetBootsHandler.JetBootsLocalState jbLocal = handler.getExtensionData(getCommonHandler());
            flightStabilizers = jbLocal.isFlightStabilizers();
            smartHover = jbLocal.isSmartHover();
        }
    }

    @Override
    public void render2D(MatrixStack matrixStack, float partialTicks, boolean armorPieceHasPressure) {
        if (armorPieceHasPressure && jbStat.isStatOpen()) {
            FontRenderer fr = Minecraft.getInstance().font;
            int xl = jbStat.getBaseX() + 5;
            int y = jbStat.getBaseY() + fr.lineHeight + 8;
            int xr = jbStat.getBaseX() + jbStat.getStatWidth() - 5;
            if (jbStat.isLeftSided()) {
                xl -= jbStat.getStatWidth();
                xr -= jbStat.getStatWidth();
            }
            fr.drawShadow(matrixStack, l1, xl, y, 0x404040);
            fr.drawShadow(matrixStack, l2, xl, y + fr.lineHeight, 0x404040);
            fr.drawShadow(matrixStack, l3, xl, y + fr.lineHeight * 2, 0x404040);
            fr.drawShadow(matrixStack, r1, xr - widestR, y, 0x404040);
            fr.drawShadow(matrixStack, r2, xr - widestR, y + fr.lineHeight, 0x404040);
            fr.drawShadow(matrixStack, r3, xr - widestR, y + fr.lineHeight * 2, 0x404040);

            int iconX = xr - 30;
            if (builderMode) {
                GuiUtils.renderItemStack(matrixStack, PICK, iconX, jbStat.getBaseY());
                iconX -= 16;
            }
            if (flightStabilizers) {
                GuiUtils.renderItemStack(matrixStack, ROTOR, iconX, jbStat.getBaseY());
                iconX -= 16;
            }
            if (Minecraft.getInstance().player.isFallFlying()) {
                GuiUtils.renderItemStack(matrixStack, ELYTRA, iconX, jbStat.getBaseY());
                iconX -= 16;
            }
            if (smartHover) {
                GuiUtils.renderItemStack(matrixStack, FEATHER, iconX, jbStat.getBaseY());
//                iconX -= 16;
            }
        }
    }

    @Override
    public IGuiAnimatedStat getAnimatedStat() {
        if (jbStat == null) {
            PlayerEntity player = Minecraft.getInstance().player;
            if (player != null) {
                prevX = player.getX();
                prevY = player.getY();
                prevZ = player.getZ();
            }
            CommonArmorHandler handler = CommonArmorHandler.getHandlerForPlayer();
            int n = Math.max(1, handler.getUpgradeCount(EquipmentSlotType.FEET, EnumUpgrade.JET_BOOTS));
            ItemStack stack = new ItemStack(EnumUpgrade.JET_BOOTS.getItem(n));
            jbStat = new WidgetAnimatedStat(null, xlate(ArmorUpgradeRegistry.getStringKey(getCommonHandler().getID())),
                    WidgetAnimatedStat.StatIcon.of(stack),
                    HUDHandler.getInstance().getStatOverlayColor(), null, ArmorHUDLayout.INSTANCE.jetBootsStat);
            jbStat.setMinimumContractedDimensions(0, 0);
            jbStat.setMinimumExpandedDimensions(120, 42);
        }
        return jbStat;
    }

    @Override
    public void onResolutionChanged() {
        jbStat = null;
    }

    @Override
    public Collection<ResourceLocation> getSubKeybinds() {
        return ImmutableList.of(RL("jet_boots.module.builder_mode"), RL("jet_boots.module.flight_stabilizers"), RL("jet_boots.module.smart_hover"));
    }
}
