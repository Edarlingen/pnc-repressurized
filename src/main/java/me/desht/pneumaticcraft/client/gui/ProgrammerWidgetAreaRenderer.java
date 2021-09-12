package me.desht.pneumaticcraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.desht.pneumaticcraft.client.gui.programmer.ProgWidgetGuiManager;
import me.desht.pneumaticcraft.client.gui.widget.WidgetVerticalScrollbar;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.client.util.ProgWidgetRenderer;
import me.desht.pneumaticcraft.common.progwidgets.IJump;
import me.desht.pneumaticcraft.common.progwidgets.ILabel;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.common.thirdparty.ThirdPartyManager;
import me.desht.pneumaticcraft.lib.GuiConstants;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import java.util.*;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class ProgrammerWidgetAreaRenderer {
    private static final float SCALE_PER_STEP = 0.2F;

    private final Screen parent;
    private final List<IProgWidget> progWidgets;
    private final int guiLeft, guiTop;
    private final int startX, startY, areaWidth, areaHeight;
    private final WidgetVerticalScrollbar scaleScroll;
    private double translatedX, translatedY;
    private int lastZoom;
    private final List<List<ITextComponent>> widgetErrors = new ArrayList<>();
    private final List<List<ITextComponent>> widgetWarnings = new ArrayList<>();
    private int totalErrors = 0;
    private int totalWarnings = 0;

    public ProgrammerWidgetAreaRenderer(Screen parent, List<IProgWidget> progWidgets, int guiLeft, int guiTop,
                                        Rectangle2d bounds, double translatedX, double translatedY, int lastZoom) {
        this.parent = parent;
        this.progWidgets = progWidgets;
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
        this.startX = bounds.getX();
        this.startY = bounds.getY();
        this.areaWidth = bounds.getWidth();
        this.areaHeight = bounds.getHeight();
        this.translatedX = translatedX;
        this.translatedY = translatedY;
        this.lastZoom = lastZoom;

        scaleScroll = new WidgetVerticalScrollbar(guiLeft + areaWidth + 8, guiTop + 40, areaHeight - 25)
                .setStates((int)((2.0F / SCALE_PER_STEP) - 1))
                .setCurrentState(lastZoom)
                .setListening(true);
    }

    public WidgetVerticalScrollbar getScrollBar() {
        return scaleScroll;
    }

    int getLastZoom() {
        return lastZoom;
    }

    double getTranslatedX() {
        return translatedX;
    }

    double getTranslatedY() {
        return translatedY;
    }

    private void addMessages(List<ITextComponent> tooltip, List<ITextComponent> msgList, String key, TextFormatting color) {
        if (!msgList.isEmpty()) {
            tooltip.add(xlate(key).withStyle(color, TextFormatting.UNDERLINE));
            for (ITextComponent msg : msgList) {
                tooltip.add(new StringTextComponent(GuiConstants.TRIANGLE_RIGHT + " ").append(msg).withStyle(color));
            }
        }
    }

    public void renderForeground(MatrixStack matrixStack, int x, int y, IProgWidget tooltipExcludingWidget, FontRenderer font) {
        int idx = getHoveredWidgetIndex(x, y);
        if (idx >= 0) {
            IProgWidget progWidget = progWidgets.get(idx);
            if (progWidget != null && progWidget != tooltipExcludingWidget) {
                List<ITextComponent> tooltip = new ArrayList<>();
                progWidget.getTooltip(tooltip);
                if (widgetErrors.size() == progWidgets.size())
                    addMessages(tooltip, widgetErrors.get(idx), "pneumaticcraft.gui.programmer.errors", TextFormatting.RED);
                if (widgetWarnings.size() == progWidgets.size())
                    addMessages(tooltip, widgetWarnings.get(idx), "pneumaticcraft.gui.programmer.warnings", TextFormatting.YELLOW);
                addAdditionalInfoToTooltip(progWidget, tooltip);
                if (!tooltip.isEmpty()) {
                    parent.renderTooltip(matrixStack, GuiUtils.wrapTextComponentList(tooltip, areaWidth * 2 / 3, font), x - guiLeft, y - guiTop);
                }
            }
        }
    }

    private int getHoveredWidgetIndex(int mouseX, int mouseY) {
        float scale = getScale();
        for (int i = 0; i < progWidgets.size(); i++) {
            IProgWidget widget = progWidgets.get(i);
            if (!isOutsideProgrammingArea(widget)
                    && (mouseX - translatedX) / scale - guiLeft >= widget.getX()
                    && (mouseY - translatedY) / scale - guiTop >= widget.getY()
                    && (mouseX - translatedX) / scale - guiLeft <= widget.getX() + widget.getWidth() / 2f
                    && (mouseY - translatedY) / scale - guiTop <= widget.getY() + widget.getHeight() / 2f) {
                return i;
            }
        }
        return -1;
    }

    public IProgWidget getHoveredWidget(int mouseX, int mouseY) {
        int i = getHoveredWidgetIndex(mouseX, mouseY);
        return i >= 0 ? progWidgets.get(i) : null;
    }

    protected void addAdditionalInfoToTooltip(IProgWidget widget, List<ITextComponent> tooltip) {
        if (ProgWidgetGuiManager.hasGui(widget)) {
            tooltip.add(xlate("pneumaticcraft.gui.programmer.rightClickForOptions").withStyle(TextFormatting.GOLD));
        }
        ThirdPartyManager.instance().getDocsProvider().addTooltip(tooltip, false);
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            tooltip.add(new StringTextComponent(widget.getType().getRegistryName().toString()).withStyle(TextFormatting.DARK_GRAY));
        }
    }

    public void tick() {
        if ((Minecraft.getInstance().level.getGameTime() & 0xf) == 0 || widgetErrors.size() != progWidgets.size() || widgetWarnings.size() != progWidgets.size()) {
            widgetErrors.clear();
            widgetWarnings.clear();
            totalErrors = totalWarnings = 0;
            for (IProgWidget widget : progWidgets) {
                List<ITextComponent> e = new ArrayList<>();
                widget.addErrors(e, progWidgets);
                widgetErrors.add(e.isEmpty() ? Collections.emptyList() : e);
                totalErrors += e.size();
                List<ITextComponent> w = new ArrayList<>();
                widget.addWarnings(w, progWidgets);
                widgetWarnings.add(w.isEmpty() ? Collections.emptyList() : w);
                totalWarnings += w.size();
            }
        }
    }

    public void render(MatrixStack matrixStack, int x, int y, boolean showFlow, boolean showInfo) {
        if (scaleScroll.getState() != lastZoom) {
            float shift = SCALE_PER_STEP * (scaleScroll.getState() - lastZoom);
            float prevScale = 2.0F - lastZoom * SCALE_PER_STEP;
            translatedX += shift * (x - translatedX) / prevScale;
            translatedY += shift * (y - translatedY) / prevScale;
        }
        lastZoom = scaleScroll.getState();

        MainWindow mw = Minecraft.getInstance().getWindow();
        double sf = mw.getGuiScale();
        GL11.glScissor((int)((guiLeft + startX) * mw.getGuiScale()), (int)(mw.getGuiScaledHeight() * sf - areaHeight * sf - (guiTop + startY) * sf), (int)(areaWidth * sf), (int)(areaHeight * sf));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        matrixStack.pushPose();
        matrixStack.translate(translatedX, translatedY, 0);

        float scale = getScale();
        matrixStack.scale(scale, scale, 1);

        if (showFlow) showFlow(matrixStack);

        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        for (IProgWidget widget : progWidgets) {
            matrixStack.pushPose();
            matrixStack.translate(widget.getX() + guiLeft, widget.getY() + guiTop, 0);
            matrixStack.scale(0.5f, 0.5f, 1.0f);
            ProgWidgetRenderer.renderProgWidget2d(matrixStack, widget);
            matrixStack.popPose();
        }

        if (widgetErrors.size() == progWidgets.size() && widgetWarnings.size() == progWidgets.size()) {
            for (int i = 0; i < progWidgets.size(); i++) {
                if (!widgetErrors.get(i).isEmpty()) {
                    drawBorder(matrixStack, progWidgets.get(i), 0xFFFF0000);
                } else if (!widgetWarnings.get(i).isEmpty()) {
                    drawBorder(matrixStack, progWidgets.get(i), 0xFFFFFF00);
                }
            }
        }

        renderAdditionally(matrixStack);

        RenderSystem.disableBlend();

        if (showInfo) {
            for (IProgWidget widget : progWidgets) {
                matrixStack.pushPose();
                matrixStack.translate(widget.getX() + guiLeft, widget.getY() + guiTop, 0);
                matrixStack.scale(0.5f, 0.5f, 1.0f);
                ProgWidgetRenderer.doExtraRendering2d(matrixStack, widget);
                matrixStack.popPose();
            }
        }

        matrixStack.popPose();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
        if (mouseButton == 0 && !scaleScroll.isDragging() && new Rectangle2d(guiLeft + startX, guiTop + startY, areaWidth, areaHeight).contains((int)mouseX, (int)mouseY)) {
            translatedX += dx;
            translatedY += dy;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double dir) {
        return scaleScroll.mouseScrolled(mouseX, mouseY, dir);
    }

    protected void renderAdditionally(MatrixStack matrixStack) {
        // nothing; to be overridden
    }

    protected void drawBorder(MatrixStack matrixStack, IProgWidget widget, int color) {
        drawBorder(matrixStack, widget, color, 0);
    }

    protected void drawBorder(MatrixStack matrixStack, IProgWidget widget, int color, int inset) {
        matrixStack.pushPose();
        matrixStack.translate(widget.getX() + guiLeft, widget.getY() + guiTop, 0);
        matrixStack.scale(0.5f, 0.5f, 1f);
        vLine(matrixStack, inset, inset, widget.getHeight() - inset, color);
        vLine(matrixStack, widget.getWidth() - inset, inset, widget.getHeight() - inset, color);
        hLine(matrixStack, widget.getWidth() - inset, inset, inset, color);
        hLine(matrixStack, widget.getWidth() - inset, inset, widget.getHeight() - inset, color);
        matrixStack.popPose();
    }

    private void hLine(MatrixStack matrixStack, int minX, int maxX, int y, int color) {
        if (maxX < minX) {
            int i = minX; minX = maxX; maxX = i;
        }
        AbstractGui.fill(matrixStack, minX, y, maxX + 1, y + 1, color);
    }

    private void vLine(MatrixStack matrixStack, int x, int minY, int maxY, int color) {
        if (maxY < minY) {
            int i = minY; minY = maxY; maxY = i;
        }
        AbstractGui.fill(matrixStack, x, minY + 1, x + 1, maxY, color);
    }

    private static final float ARROW_ANGLE = (float) Math.toRadians(30);
    private static final float ARROW_SIZE = 5;

    private void showFlow(MatrixStack matrixStack) {
        RenderSystem.lineWidth(1);
        RenderSystem.disableTexture();

        BufferBuilder wr = Tessellator.getInstance().getBuilder();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);

        Map<String, List<IProgWidget>> labelWidgets = new HashMap<>();
        for (IProgWidget w : progWidgets) {
            if (w instanceof ILabel) {
                labelWidgets.computeIfAbsent(((ILabel) w).getLabel(), k -> new ArrayList<>()).add(w);
            }
        }

        Matrix4f posMat = matrixStack.last().pose();
        for (IProgWidget widget : progWidgets) {
            if (widget instanceof IJump) {
                for (String jumpLocation : ((IJump) widget).getPossibleJumpLocations()) {
                    for (IProgWidget labelWidget : labelWidgets.getOrDefault(jumpLocation, Collections.emptyList())) {
                        int x1 = widget.getX() + widget.getWidth() / 4;
                        int y1 = widget.getY() + widget.getHeight() / 4;
                        int x2 = labelWidget.getX() + labelWidget.getWidth() / 4;
                        int y2 = labelWidget.getY() + labelWidget.getHeight() / 4;
                        float midX = (x2 + x1) / 2F;
                        float midY = (y2 + y1) / 2F;
                        wr.vertex(posMat,guiLeft + x1, guiTop + y1, 0.0f).endVertex();
                        wr.vertex(posMat,guiLeft + x2, guiTop + y2, 0.0f).endVertex();
                        Vector3d arrowVec = new Vector3d(x1 - x2, y1 - y2, 0).normalize();
                        arrowVec = new Vector3d(arrowVec.x * ARROW_SIZE, 0, arrowVec.y * ARROW_SIZE);
                        arrowVec = arrowVec.yRot(ARROW_ANGLE);
                        wr.vertex(posMat,guiLeft + midX, guiTop + midY, 0.0f).endVertex();
                        wr.vertex(posMat,guiLeft + midX + (float)arrowVec.x, guiTop + midY + (float)arrowVec.z, 0.0f).endVertex();
                        arrowVec = arrowVec.yRot(-2 * ARROW_ANGLE);
                        wr.vertex(posMat,guiLeft + midX, guiTop + midY, 0.0f).endVertex();
                        wr.vertex(posMat,guiLeft + midX + (float)arrowVec.x, guiTop + midY + (float)arrowVec.z, 0.0f).endVertex();
                    }
                }
            }
        }

        Tessellator.getInstance().end();

        RenderSystem.enableTexture();
    }

    public float getScale() {
        return 2.0F - scaleScroll.getState() * SCALE_PER_STEP;
    }

    boolean isOutsideProgrammingArea(IProgWidget widget) {
        float scale = getScale();
        int x = (int) ((widget.getX() + guiLeft) * scale);
        int y = (int) ((widget.getY() + guiTop) * scale);
        x += translatedX - guiLeft;
        y += translatedY - guiTop;

        return x < startX || x + widget.getWidth() * scale / 2 > startX + areaWidth
                || y < startY || y + widget.getHeight() * scale / 2 > startY + areaHeight;
    }

    public void gotoPiece(IProgWidget widget) {
        if (widget != null) {
            scaleScroll.currentScroll = 0;
            lastZoom = 0;
            translatedX = -widget.getX() * 2d + areaWidth / 2d - guiLeft;
            translatedY = -widget.getY() * 2d + areaHeight / 2d - guiTop;
        }
    }

    public int getTotalErrors() {
        return totalErrors;
    }

    public int getTotalWarnings() {
        return totalWarnings;
    }
}
