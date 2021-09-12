package me.desht.pneumaticcraft.client.render.tileentity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.desht.pneumaticcraft.client.render.ModRenderTypes;
import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.tileentity.TileEntityElevatorCaller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.math.vector.Matrix4f;

public class RenderElevatorCaller extends TileEntityRenderer<TileEntityElevatorCaller> {

    private static final float Z_OFFSET = 0.499F;

    public RenderElevatorCaller(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(TileEntityElevatorCaller te, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        matrixStackIn.pushPose();
        matrixStackIn.translate(0.5, 1.5, 0.5);
        matrixStackIn.scale(1f, -1f, -1f);
        RenderUtils.rotateMatrixForDirection(matrixStackIn, te.getRotation());
        matrixStackIn.translate(-1, 0, -1);

        FontRenderer fontRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getFont();
        Matrix4f posMat = matrixStackIn.last().pose();

        for (TileEntityElevatorCaller.ElevatorButton button : te.getFloors()) {
            // button background
            IVertexBuilder builder = bufferIn.getBuffer(ModRenderTypes.getUntexturedQuad(false));
            builder.vertex(posMat, button.posX + 0.5F, button.posY + 0.5F, Z_OFFSET)
                    .color(button.red, button.green, button.blue, 1F)
                    .uv2(0x00F000A0)
                    .endVertex();
            builder.vertex(posMat, button.posX + 0.5F, button.posY + button.height + 0.5F, Z_OFFSET)
                    .color(button.red, button.green, button.blue, 1F)
                    .uv2(0x00F000A0)
                    .endVertex();
            builder.vertex(posMat, button.posX + button.width + 0.5F, button.posY + button.height + 0.5F, Z_OFFSET)
                    .color(button.red, button.green, button.blue, 1F)
                    .uv2(0x00F000A0)
                    .endVertex();
            builder.vertex(posMat, button.posX + button.width + 0.5F, button.posY + 0.5F, Z_OFFSET)
                    .color(button.red, button.green, button.blue, 1F)
                    .uv2(0x00F000A0)
                    .endVertex();

            // button text
            matrixStackIn.pushPose();
            matrixStackIn.translate(button.posX + 0.5D, button.posY + 0.5D, 0.498);
            matrixStackIn.translate(button.width / 2, button.height / 2, 0);
            float textScale = Math.min(button.width / 10F, button.height / 10F);
            matrixStackIn.scale(textScale, textScale, textScale);
            fontRenderer.drawInBatch(button.buttonText, -fontRenderer.width(button.buttonText) / 2f, -fontRenderer.lineHeight / 2f, 0xFF000000, false, matrixStackIn.last().pose(), bufferIn, false, 0, combinedLightIn);
            matrixStackIn.popPose();
        }

        matrixStackIn.popPose();
    }
}
