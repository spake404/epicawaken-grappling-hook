package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class GrapplingHookCurioRenderer implements ICurioRenderer {
    public static final ResourceLocation ARM_MODEL = GrapplingHookArmModelRenderer.ARM_MODEL;

    private static final float ARM_MOUNT_X = 0.0F;
    private static final float ARM_MOUNT_Y = 0.625F;
    private static final float ARM_MOUNT_Z = 0.0F;
    private static final float ARM_MOUNT_SCALE = 1.0F;

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource bufferSource,
            int packedLight,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if (stack.isEmpty()) {
            return;
        }

        boolean suppressedLayerCall = EpicFightCuriosFallbackGuard.isSuppressedLayerCall();
        if (suppressedLayerCall) {
            GrapplingHookRenderPathDebug.logRenderPath(
                    "CURIO_SUPPRESSED_BY_EPIC_FIGHT",
                    slotContext.entity(),
                    GrapplingHookRenderDebugControls.shouldUsePullModel(slotContext.entity()),
                    "Curios fallback call suppressed while Epic Fight animated model is active");
            return;
        }

        EntityModel<T> model = renderLayerParent.getModel();
        if (!(model instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        poseStack.pushPose();
        humanoidModel.leftArm.translateAndRotate(poseStack);
        poseStack.translate(ARM_MOUNT_X, ARM_MOUNT_Y, ARM_MOUNT_Z);
        poseStack.scale(ARM_MOUNT_SCALE, ARM_MOUNT_SCALE, ARM_MOUNT_SCALE);
        boolean pullModel = GrapplingHookRenderDebugControls.shouldUsePullModel(slotContext.entity());
        GrapplingHookRenderPathDebug.logRenderPath(
                "CURIO_DEFAULT",
                slotContext.entity(),
                pullModel,
                "using DEFAULT transform on HumanoidModel.leftArm");
        if (pullModel) {
            GrapplingHookRenderDebugControls.applyDefaultPullTransform(poseStack);
            GrapplingHookArmModelRenderer.render(GrapplingHookArmModelRenderer.ARM_PULL_MODEL, stack, poseStack, bufferSource, packedLight);
        } else {
            GrapplingHookRenderDebugControls.applyDefaultNormalTransform(poseStack);
            GrapplingHookArmModelRenderer.render(stack, poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }
}
