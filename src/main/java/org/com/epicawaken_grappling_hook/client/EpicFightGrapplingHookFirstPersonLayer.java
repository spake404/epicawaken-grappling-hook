package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import top.theillusivec4.curios.client.render.CuriosLayer;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.config.ClientConfig;

import java.util.Optional;

public class EpicFightGrapplingHookFirstPersonLayer extends PatchedLayer<LocalPlayer, LocalPlayerPatch, PlayerModel<LocalPlayer>, CuriosLayer<LocalPlayer, PlayerModel<LocalPlayer>>> {
    private static final float ARM_MOUNT_X = 0.0F;
    private static final float ARM_MOUNT_Y = 0.625F;
    private static final float ARM_MOUNT_Z = 0.0F;
    private static final float ARM_MOUNT_SCALE = 1.0F;

    @Override
    protected void renderLayer(
            LocalPlayerPatch entityPatch,
            LocalPlayer player,
            CuriosLayer<LocalPlayer, PlayerModel<LocalPlayer>> originalRenderer,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            OpenMatrix4f[] poses,
            float bob,
            float yRot,
            float xRot,
            float partialTicks) {
        if (!ClientConfig.enableAnimatedFirstPersonModel) {
            return;
        }

        Optional<GrapplingHookCurioLookup.Entry> entry = GrapplingHookCurioLookup.findVisible(player);
        if (entry.isEmpty()) {
            return;
        }

        Armature armature = entityPatch.getArmature();
        Joint handJoint = armature.searchJointByName("Hand_L");
        if (handJoint == null || handJoint.getId() < 0 || handJoint.getId() >= poses.length) {
            return;
        }

        poseStack.pushPose();
        OpenMatrix4f jointPose = poses[handJoint.getId()];
        MathUtils.mulStack(poseStack, jointPose);
        poseStack.translate(ARM_MOUNT_X, ARM_MOUNT_Y, ARM_MOUNT_Z);
        poseStack.scale(ARM_MOUNT_SCALE, ARM_MOUNT_SCALE, ARM_MOUNT_SCALE);
        boolean pullModel = GrapplingHookRenderDebugControls.shouldUsePullModel(player);
        GrapplingHookRenderPathDebug.logJointMatrix(
                "EPIC_FIGHT_FIRST_PERSON",
                player,
                pullModel,
                handJoint.getName(),
                handJoint.getId(),
                jointPose);
        GrapplingHookRenderPathDebug.logPoseTop("first_after_joint_mount", player, pullModel, poseStack);
        GrapplingHookRenderPathDebug.logRenderPath(
                "EPIC_FIGHT_FIRST_PERSON",
                player,
                pullModel,
                "layerIdentity=" + System.identityHashCode(this) + " using EPIC_FIGHT transform joint=" + handJoint.getName() + " id=" + handJoint.getId());
        if (pullModel) {
            GrapplingHookRenderDebugControls.applyEpicFightPullTransform(poseStack);
            GrapplingHookRenderPathDebug.logPoseTop("first_after_mod_pull_transform", player, true, poseStack);
            GrapplingHookArmModelRenderer.render(GrapplingHookArmModelRenderer.ARM_PULL_MODEL, entry.get().stack(), poseStack, bufferSource, packedLight);
        } else {
            GrapplingHookRenderDebugControls.applyEpicFightNormalTransform(poseStack);
            GrapplingHookRenderPathDebug.logPoseTop("first_after_mod_normal_transform", player, false, poseStack);
            GrapplingHookArmModelRenderer.render(entry.get().stack(), poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }
}
