package org.com.epicawaken_grappling_hook.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class ArmatureUtil {
    public static Vec3 getJointWorldPos(LivingEntityPatch<?> entityPatch, Joint joint) {
        return getJointWorldPos(entityPatch, joint, 0.5F);
    }

    public static Vec3 getJointWorldPos(LivingEntityPatch<?> entityPatch, Joint joint, float partialTicks) {
        return getJointWorldPos(entityPatch, joint, Vec3.ZERO, partialTicks);
    }

    public static Vec3 getJointWorldPos(LivingEntityPatch<?> entityPatch, Joint joint, Vec3 localOffset, float partialTicks) {
        Pose pose = entityPatch.getAnimator().getPose(partialTicks);
        return getVec3(entityPatch, joint, localOffset, partialTicks, pose);
    }

    @NotNull
    private static Vec3 getVec3(LivingEntityPatch<?> entityPatch, Joint joint, Vec3 localOffset, float partialTicks, Pose pose) {
        OpenMatrix4f jointTransform = getM4f(entityPatch, joint, partialTicks, pose);
        return OpenMatrix4f.transform(jointTransform, localOffset);
    }

    private static OpenMatrix4f getM4f(LivingEntityPatch<?> entityPatch, Joint joint, float partialTicks, Pose pose) {
        LivingEntity entity = (LivingEntity) entityPatch.getOriginal();
        Vec3 pos = new Vec3(
                Mth.lerp(partialTicks, entity.xo, entity.getX()),
                Mth.lerp(partialTicks, entity.yo, entity.getY()),
                Mth.lerp(partialTicks, entity.zo, entity.getZ()));
        OpenMatrix4f modelTransform = OpenMatrix4f.createTranslation((float) pos.x, (float) pos.y, (float) pos.z)
                .mulBack(OpenMatrix4f.createRotatorDeg(180.0F, Vec3f.Y_AXIS)
                        .mulBack(entityPatch.getModelMatrix(partialTicks)));
        return new OpenMatrix4f(entityPatch.getArmature().getBoundTransformFor(pose, joint)).mulFront(modelTransform);
    }

    private ArmatureUtil() {
    }
}
