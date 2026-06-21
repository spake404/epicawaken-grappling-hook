package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;
import top.theillusivec4.curios.client.render.CuriosLayer;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.compat.CuriosCompat;
import yesman.epicfight.model.armature.types.ToolHolderArmature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class GrapplingHookCurioRenderer implements ICurioRenderer, CuriosCompat.EpicFightCurioRenderer {
    public static final ResourceLocation ARM_MODEL = ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "item/grappling_hook_arm");
    public static final ItemDisplayContext WORN = ItemDisplayContext.create(
            "EPICAWAKEN_GRAPPLING_HOOK_WORN",
            ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "worn"),
            ItemDisplayContext.FIXED);

    private static final float ARM_MOUNT_X = 0.0F;
    private static final float ARM_MOUNT_Y = 0.625F;
    private static final float ARM_MOUNT_Z = 0.0F;
    private static final float ARM_MOUNT_SCALE = 1.0F;

    private static final OpenMatrix4f RIGHT_HAND_CORRECTION = new OpenMatrix4f().unmodifiable();

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
        LivingEntity livingEntity = slotContext.entity();
        if (stack.isEmpty()) {
            return;
        }

        EntityModel<T> model = renderLayerParent.getModel();
        if (!(model instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        poseStack.pushPose();
        humanoidModel.rightArm.translateAndRotate(poseStack);
        poseStack.translate(ARM_MOUNT_X, ARM_MOUNT_Y, ARM_MOUNT_Z);
        poseStack.scale(ARM_MOUNT_SCALE, ARM_MOUNT_SCALE, ARM_MOUNT_SCALE);
        renderModel(livingEntity, stack, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public void draw(
            ItemStack stack,
            SlotContext slotContext,
            LivingEntityPatch<LivingEntity> entityPatch,
            LivingEntity livingEntity,
            CuriosLayer<LivingEntity, EntityModel<LivingEntity>> curiosLayer,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            OpenMatrix4f[] poses,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks) {
        if (stack.isEmpty()) {
            return;
        }

        Armature armature = entityPatch.getArmature();
        if (!(armature instanceof ToolHolderArmature toolHolderArmature)) {
            return;
        }

        Joint rightToolJoint = toolHolderArmature.rightToolJoint();
        if (rightToolJoint == null || rightToolJoint.getId() < 0 || rightToolJoint.getId() >= poses.length) {
            return;
        }

        OpenMatrix4f transform = new OpenMatrix4f(RIGHT_HAND_CORRECTION).mulFront(poses[rightToolJoint.getId()]);

        poseStack.pushPose();
        MathUtils.mulStack(poseStack, transform);
        poseStack.scale(ARM_MOUNT_SCALE, ARM_MOUNT_SCALE, ARM_MOUNT_SCALE);
        renderModel(livingEntity, stack, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    private static void renderModel(
            LivingEntity livingEntity,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(ARM_MODEL);
        minecraft.getItemRenderer().render(
                stack,
                WORN,
                false,
                poseStack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                model);
    }
}
