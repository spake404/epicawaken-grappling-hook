package org.com.epicawaken_grappling_hook.projectile.hook;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.item.ModItems;
import org.com.epicawaken_grappling_hook.util.ArmatureUtil;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class GrapplingHookRenderer extends EntityRenderer<GrapplingHook> {
    private static final ResourceLocation HOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "textures/item/grappling_hook.png");
    public static final ResourceLocation PROJECTILE_MODEL = ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "item/grappling_hook_projectile");

    public GrapplingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull GrapplingHook grapplingHook, @NotNull Frustum cameraFrustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public void render(GrapplingHook grapplingHook, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        Entity entity = grapplingHook.getOwner();
        if (entity instanceof Player player) {
            poseStack.pushPose();
            this.renderConnectionLine(grapplingHook, player, partialTicks, poseStack, bufferSource);
            poseStack.popPose();
            this.renderHook(grapplingHook, partialTicks, poseStack, bufferSource, packedLight);
            super.render(grapplingHook, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        }
    }

    private void renderHook(GrapplingHook grapplingHook, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, grapplingHook.yRotO, grapplingHook.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, grapplingHook.xRotO, grapplingHook.getXRot())));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(PROJECTILE_MODEL);
        minecraft.getItemRenderer().render(
                new ItemStack(ModItems.GRAPPLING_HOOK.get()),
                ItemDisplayContext.NONE,
                false,
                poseStack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                model);
        poseStack.popPose();
    }

    private void renderConnectionLine(GrapplingHook grapplingHook, Player player, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource) {
        PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        if (playerPatch == null) {
            return;
        }

        Vec3 leftHandPos = ArmatureUtil.getJointWorldPos(playerPatch, ((HumanoidArmature) Armatures.BIPED.get()).toolL, partialTicks);
        Vec3 hookPos = new Vec3(
                Mth.lerp(partialTicks, grapplingHook.xo, grapplingHook.getX()),
                Mth.lerp(partialTicks, grapplingHook.yo, grapplingHook.getY()),
                Mth.lerp(partialTicks, grapplingHook.zo, grapplingHook.getZ()));
        Vec3 vector = leftHandPos.subtract(hookPos);
        Vec3 vectorNormal = vector.normalize();
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();
        lineConsumer.vertex(pose.pose(), 0.0F, 0.0F, 0.0F)
                .color(0, 1, 1, 255)
                .normal(pose.normal(), (float) vectorNormal.x, (float) vectorNormal.y, (float) vectorNormal.z)
                .endVertex();
        lineConsumer.vertex(pose.pose(), (float) vector.x, (float) vector.y, (float) vector.z)
                .color(0, 1, 1, 255)
                .normal(pose.normal(), (float) vectorNormal.x, (float) vectorNormal.y, (float) vectorNormal.z)
                .endVertex();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull GrapplingHook grapplingHook) {
        return HOOK_TEXTURE;
    }
}
