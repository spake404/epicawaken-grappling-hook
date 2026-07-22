package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PhantomGrapplingHookItemRenderer extends BlockEntityWithoutLevelRenderer {
    public PhantomGrapplingHookItemRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel hookModel = minecraft.getModelManager().getModel(GrapplingHookArmModelRenderer.PHANTOM_ARM_MODEL);
        minecraft.getItemRenderer().render(
                stack,
                ItemDisplayContext.NONE,
                false,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                hookModel);
        if (displayContext == ItemDisplayContext.GUI) {
            PhantomGrapplingHookRenderUtil.renderMountedPhantom(poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }
}
