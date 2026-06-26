package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import top.theillusivec4.curios.client.render.CuriosLayer;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.layer.UniqueLayer;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Optional;

public class EpicFightGrapplingHookArmLayer extends UniqueLayer<LivingEntity, LivingEntityPatch<LivingEntity>, EntityModel<LivingEntity>> {
    private static final float ARM_MOUNT_X = 0.0F;
    private static final float ARM_MOUNT_Y = 0.625F;
    private static final float ARM_MOUNT_Z = 0.0F;
    private static final float ARM_MOUNT_SCALE = 1.0F;
    private static int lastLoggedTick = -1;
    private static boolean lastLoggedZeroMatrix;
    private static String lastLoggedAnimation = "";

    public static void onModifyPatchedRenderers(PatchedRenderersEvent.Modify event) {
        PatchedEntityRenderer renderer = event.get(EntityType.PLAYER);
        if (renderer instanceof PatchedLivingEntityRenderer<?, ?, ?, ?, ?> livingRenderer) {
            addLayer(livingRenderer);
        }
        addFirstPersonLayer();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLayer(PatchedLivingEntityRenderer renderer) {
        renderer.addCustomLayer(new EpicFightGrapplingHookArmLayer());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addFirstPersonLayer() {
        yesman.epicfight.client.ClientEngine clientEngine = yesman.epicfight.client.ClientEngine.getInstance();
        if (clientEngine == null || clientEngine.renderEngine == null || clientEngine.renderEngine.getFirstPersonRenderer() == null) {
            return;
        }
        clientEngine.renderEngine.getFirstPersonRenderer().addPatchedLayer(CuriosLayer.class, new EpicFightGrapplingHookFirstPersonLayer());
    }

    @Override
    protected void renderLayer(
            LivingEntityPatch<LivingEntity> entityPatch,
            LivingEntity entity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            OpenMatrix4f[] poses,
            float bob,
            float yRot,
            float xRot,
            float partialTicks) {
        if (!ClientConfig.enableAnimatedFirstPersonModel || !(entity instanceof Player player)) {
            return;
        }

        Optional<GrapplingHookCurioLookup.Entry> entry = GrapplingHookCurioLookup.findVisible(player);
        if (entry.isEmpty()) {
            return;
        }

        Armature armature = entityPatch.getArmature();
        Joint handJoint = findLeftHandJoint(armature);
        if (!isValidJoint(handJoint, poses)) {
            return;
        }

        OpenMatrix4f jointPose = poses[handJoint.getId()];
        if (Config.debugLogging) {
            logRenderDebug(player, entityPatch, handJoint, jointPose, partialTicks);
        }

        poseStack.pushPose();
        MathUtils.mulStack(poseStack, jointPose);
        poseStack.translate(ARM_MOUNT_X, ARM_MOUNT_Y, ARM_MOUNT_Z);
        poseStack.scale(ARM_MOUNT_SCALE, ARM_MOUNT_SCALE, ARM_MOUNT_SCALE);
        if (GrapplingHookRenderDebugControls.shouldUsePullModel(player)) {
            GrapplingHookRenderDebugControls.applyEpicFightPullTransform(poseStack);
            GrapplingHookArmModelRenderer.render(GrapplingHookArmModelRenderer.ARM_PULL_MODEL, entry.get().stack(), poseStack, bufferSource, packedLight);
        } else {
            GrapplingHookRenderDebugControls.applyEpicFightNormalTransform(poseStack);
            GrapplingHookArmModelRenderer.render(entry.get().stack(), poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }

    private static Joint findLeftHandJoint(Armature armature) {
        return armature.searchJointByName("Hand_L");
    }

    private static boolean isValidJoint(Joint joint, OpenMatrix4f[] poses) {
        return joint != null && joint.getId() >= 0 && joint.getId() < poses.length;
    }

    private static void logRenderDebug(
            Player player,
            LivingEntityPatch<LivingEntity> entityPatch,
            Joint handJoint,
            OpenMatrix4f matrix,
            float partialTicks) {
        AnimationPlayer animationPlayer = entityPatch.getAnimator().getPlayerFor(null);
        boolean zeroMatrix = isZeroRotationScale(matrix);
        String realAnimation = animationPlayer == null ? "<no-player>" : animationName(animationPlayer.getRealAnimation());
        String rawAnimation = animationPlayer == null ? "<no-player>" : animationName(animationPlayer.getAnimation());
        String animationKey = realAnimation + "|" + rawAnimation;
        boolean shouldLog = player.tickCount != lastLoggedTick
                && (zeroMatrix
                || zeroMatrix != lastLoggedZeroMatrix
                || !animationKey.equals(lastLoggedAnimation)
                || player.tickCount % 20 == 0);

        if (!shouldLog) {
            return;
        }

        lastLoggedTick = player.tickCount;
        lastLoggedZeroMatrix = zeroMatrix;
        lastLoggedAnimation = animationKey;

        Epicawaken_grappling_hook.LOGGER.info(
                "[GrapplingHookRenderDebug] player={} tick={} partial={} realAnimation={} rawAnimation={} elapsed={} prevElapsed={} handJoint={} id={} zeroRotationScale={} matrix3x3=[{},{},{}; {},{},{}; {},{},{}]",
                player.getScoreboardName(),
                player.tickCount,
                partialTicks,
                realAnimation,
                rawAnimation,
                animationPlayer == null ? -1.0F : animationPlayer.getElapsedTime(),
                animationPlayer == null ? -1.0F : animationPlayer.getPrevElapsedTime(),
                handJoint.getName(),
                handJoint.getId(),
                zeroMatrix,
                matrix.m00,
                matrix.m01,
                matrix.m02,
                matrix.m10,
                matrix.m11,
                matrix.m12,
                matrix.m20,
                matrix.m21,
                matrix.m22);
    }

    private static String animationName(AssetAccessor<?> animation) {
        if (animation == null) {
            return "<null>";
        }
        return String.valueOf(animation.registryName());
    }

    private static boolean isZeroRotationScale(OpenMatrix4f matrix) {
        float sum = Math.abs(matrix.m00) + Math.abs(matrix.m01) + Math.abs(matrix.m02)
                + Math.abs(matrix.m10) + Math.abs(matrix.m11) + Math.abs(matrix.m12)
                + Math.abs(matrix.m20) + Math.abs(matrix.m21) + Math.abs(matrix.m22);
        return sum < 0.0001F;
    }
}
