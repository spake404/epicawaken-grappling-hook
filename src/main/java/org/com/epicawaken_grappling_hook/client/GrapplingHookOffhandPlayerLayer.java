package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class GrapplingHookOffhandPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float ARM_MOUNT_X = 0.0F;
    private static final float ARM_MOUNT_Y = 0.625F;
    private static final float ARM_MOUNT_Z = 0.0F;
    private static final float ARM_MOUNT_SCALE = 1.0F;

    public GrapplingHookOffhandPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if (GrapplingHookEquipmentLookup.hasVisibleCurio(player) || shouldLetEpicFightLayerRender(player)) {
            return;
        }

        ItemStack stack = GrapplingHookOffhandRenderState.getRenderableOffhandStack(player);
        if (!GrapplingHookEquipmentLookup.isGrapplingHookStack(stack)) {
            return;
        }

        poseStack.pushPose();
        this.getParentModel().leftArm.translateAndRotate(poseStack);
        poseStack.translate(ARM_MOUNT_X, ARM_MOUNT_Y, ARM_MOUNT_Z);
        poseStack.scale(ARM_MOUNT_SCALE, ARM_MOUNT_SCALE, ARM_MOUNT_SCALE);
        GrapplingHookRenderPathDebug.logRenderPath(
                "OFFHAND_DEFAULT_PLAYER",
                player,
                GrapplingHookRenderDebugControls.shouldUsePullModel(player),
                "using DEFAULT transform on PlayerModel.leftArm for offhand stack");
        if (GrapplingHookRenderDebugControls.shouldUsePullModel(player)) {
            GrapplingHookRenderDebugControls.applyDefaultPullTransform(poseStack);
            GrapplingHookArmModelRenderer.render(GrapplingHookArmModelRenderer.ARM_PULL_MODEL, stack, poseStack, bufferSource, packedLight);
        } else {
            GrapplingHookRenderDebugControls.applyDefaultNormalTransform(poseStack);
            GrapplingHookArmModelRenderer.render(stack, poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }

    private static boolean shouldLetEpicFightLayerRender(AbstractClientPlayer player) {
        return ClientConfig.enableAnimatedFirstPersonModel
                && EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class) != null;
    }
}
