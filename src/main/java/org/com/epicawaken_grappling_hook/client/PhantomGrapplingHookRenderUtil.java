package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Phantom;
import org.com.epicawaken_grappling_hook.Config;

public final class PhantomGrapplingHookRenderUtil {
    private static final ResourceLocation PHANTOM_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/phantom.png");
    private static final float PROJECTILE_PREVIEW_HOLD_TICKS = 10.0F;
    private static final float PROJECTILE_INITIAL_SCALE = 0.50F;
    private static final float PROJECTILE_TARGET_SCALE = 1.00F;
    private static final float VANILLA_FLAP_DEGREES_PER_TICK = 7.448451F;
    private static final float VANILLA_WING_ROTATION_DEGREES = 16.0F;
    private static final float VANILLA_TAIL_BASE_ROTATION_DEGREES = 5.0F;
    private static PhantomModel<Phantom> phantomModel;

    private PhantomGrapplingHookRenderUtil() {
    }

    public static void clearModelCache() {
        phantomModel = null;
    }

    public static void renderMountedPhantom(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        GrapplingHookRenderDebugControls.applyPhantomMountedTransform(poseStack);
        renderPhantomModel(poseStack, bufferSource, packedLight, false, 0.0F);
        poseStack.popPose();
    }

    public static void renderProjectilePhantom(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float ageInTicks) {
        poseStack.pushPose();
        GrapplingHookRenderDebugControls.applyPhantomProjectileTransform(poseStack);
        float growthScale = projectileGrowthScale(ageInTicks);
        poseStack.scale(growthScale, growthScale, growthScale);
        renderPhantomModel(poseStack, bufferSource, packedLight, true, ageInTicks);
        poseStack.popPose();
    }

    public static float loopingProjectilePreviewAge(float ageInTicks) {
        float loopTicks = projectileGrowthTicks() + PROJECTILE_PREVIEW_HOLD_TICKS;
        return ageInTicks % loopTicks;
    }

    private static void renderPhantomModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, boolean flying, float ageInTicks) {
        PhantomModel<Phantom> model = getPhantomModel();
        model.root().getAllParts().forEach(ModelPart::resetPose);
        if (flying) {
            applyVanillaFlightPose(model.root(), ageInTicks);
        } else {
            GrapplingHookRenderDebugControls.applyPhantomWingPose(model.root());
        }
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(PHANTOM_TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float projectileGrowthScale(float ageInTicks) {
        float progress = Mth.clamp(ageInTicks / projectileGrowthTicks(), 0.0F, 1.0F);
        float targetMultiplier = PROJECTILE_TARGET_SCALE / PROJECTILE_INITIAL_SCALE;
        return Mth.lerp(progress, 1.0F, targetMultiplier);
    }

    private static float projectileGrowthTicks() {
        return Math.max(1.0F, Config.getHookLockDelayTicks());
    }

    private static void applyVanillaFlightPose(ModelPart root, float ageInTicks) {
        ModelPart body = root.getChild("body");
        ModelPart leftWingBase = body.getChild("left_wing_base");
        ModelPart leftWingTip = leftWingBase.getChild("left_wing_tip");
        ModelPart rightWingBase = body.getChild("right_wing_base");
        ModelPart rightWingTip = rightWingBase.getChild("right_wing_tip");
        ModelPart tailBase = body.getChild("tail_base");
        ModelPart tailTip = tailBase.getChild("tail_tip");

        float flap = ageInTicks * VANILLA_FLAP_DEGREES_PER_TICK * Mth.DEG_TO_RAD;
        float wingRotation = Mth.cos(flap) * VANILLA_WING_ROTATION_DEGREES * Mth.DEG_TO_RAD;
        float tailRotation = -(VANILLA_TAIL_BASE_ROTATION_DEGREES
                + Mth.cos(flap * 2.0F) * VANILLA_TAIL_BASE_ROTATION_DEGREES) * Mth.DEG_TO_RAD;
        leftWingBase.zRot = wingRotation;
        leftWingTip.zRot = wingRotation;
        rightWingBase.zRot = -wingRotation;
        rightWingTip.zRot = -wingRotation;
        tailBase.xRot = tailRotation;
        tailTip.xRot = tailRotation;
    }

    private static PhantomModel<Phantom> getPhantomModel() {
        if (phantomModel == null) {
            phantomModel = new PhantomModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PHANTOM));
        }
        return phantomModel;
    }
}
