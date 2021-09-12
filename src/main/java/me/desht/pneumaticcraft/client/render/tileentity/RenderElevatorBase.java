package me.desht.pneumaticcraft.client.render.tileentity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.desht.pneumaticcraft.client.render.ModRenderTypes;
import me.desht.pneumaticcraft.common.tileentity.TileEntityElevatorBase;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;

public class RenderElevatorBase extends AbstractTileModelRenderer<TileEntityElevatorBase> {
    private static final float FACTOR = 9F / 16;
    private static final float[] SHADE = new float[] { 1f, 0.85f, 0.7f, 0.55f };

    private final ModelRenderer pole1;
    private final ModelRenderer pole2;
    private final ModelRenderer pole3;
    private final ModelRenderer pole4;
    private final ModelRenderer floor;

    public RenderElevatorBase(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);

        pole1 = new ModelRenderer(64, 64, 0, 17);
        pole1.addBox(0F, 0F, 0F, 2, 14, 2);
        pole1.setPos(-1F, 9F, -1F);
        pole1.mirror = true;
        pole2 = new ModelRenderer(64, 64, 0, 17);
        pole2.addBox(0F, 0F, 0F, 4, 14, 4);
        pole2.setPos(-2F, 9F, -2F);
        pole2.mirror = true;
        pole3 = new ModelRenderer(64, 64, 0, 17);
        pole3.addBox(0F, 0F, 0F, 6, 14, 6);
        pole3.setPos(-3F, 9F, -3F);
        pole3.mirror = true;
        pole4 = new ModelRenderer(64, 64, 0, 17);
        pole4.addBox(0F, 0F, 0F, 8, 14, 8);
        pole4.setPos(-4F, 9F, -4F);
        pole4.mirror = true;

        floor = new ModelRenderer(64, 64, 0, 0);
        floor.addBox(0F, 0F, 0F, 16, 1, 16);
        floor.setPos(-8F, 8F, -8F);
        floor.mirror = true;
    }

    @Override
    public void renderModel(TileEntityElevatorBase te, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        if (te.extension == 0) return;

        IVertexBuilder builder = bufferIn.getBuffer(RenderType.entityCutout(Textures.MODEL_ELEVATOR));

        double extension = MathHelper.lerp(partialTicks, te.oldExtension, te.extension);
        renderPole(matrixStackIn, builder, te.lightAbove, combinedOverlayIn, pole4, 0, extension);
        renderPole(matrixStackIn, builder, te.lightAbove, combinedOverlayIn, pole3, 1, extension);
        renderPole(matrixStackIn, builder, te.lightAbove, combinedOverlayIn, pole2, 2, extension);
        renderPole(matrixStackIn, builder, te.lightAbove, combinedOverlayIn, pole1, 3, extension);

        floor.render(matrixStackIn, builder, te.lightAbove, combinedOverlayIn);
    }

    @Override
    protected void renderExtras(TileEntityElevatorBase te, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer iRenderTypeBuffer, int combinedLightIn, int combinedOverlayIn) {
        if (te.fakeFloorTextureUV != null && te.fakeFloorTextureUV.length == 4) {
            matrixStack.pushPose();
            double extension = MathHelper.lerp(partialTicks, te.oldExtension, te.extension);
            matrixStack.translate(0, extension + 1.0005f, 0);
            IVertexBuilder builder = iRenderTypeBuffer.getBuffer(ModRenderTypes.getTextureRender(AtlasTexture.LOCATION_BLOCKS));
            float uMin = te.fakeFloorTextureUV[0];
            float vMin = te.fakeFloorTextureUV[1];
            float uMax = te.fakeFloorTextureUV[2];
            float vMax = te.fakeFloorTextureUV[3];
            Matrix4f posMat = matrixStack.last().pose();
            builder.vertex(posMat,0, 0, 1).color(1f, 1f, 1f, 1f).uv(uMin, vMax).uv2(te.lightAbove).endVertex();
            builder.vertex(posMat,1, 0, 1).color(1f, 1f, 1f, 1f).uv(uMax, vMax).uv2(te.lightAbove).endVertex();
            builder.vertex(posMat,1, 0, 0).color(1f, 1f, 1f, 1f).uv(uMax, vMin).uv2(te.lightAbove).endVertex();
            builder.vertex(posMat,0, 0, 0).color(1f, 1f, 1f, 1f).uv(uMin, vMin).uv2(te.lightAbove).endVertex();
            matrixStack.popPose();
        }
    }

    private void renderPole(MatrixStack matrixStackIn, IVertexBuilder builder, int combinedLightIn, int combinedOverlayIn, ModelRenderer pole, int idx, double extension) {
        matrixStackIn.translate(0, -extension / 4, 0);
        matrixStackIn.pushPose();
        matrixStackIn.translate(0, FACTOR, 0);
        matrixStackIn.scale(1, (float) (extension * 16 / 14 / 4), 1);
        matrixStackIn.translate(0, -FACTOR, 0);
        pole.render(matrixStackIn, builder, combinedLightIn, combinedOverlayIn, SHADE[idx], SHADE[idx], SHADE[idx], 1);
        matrixStackIn.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(TileEntityElevatorBase te) {
        return true;  // since this can get very tall
    }
}
