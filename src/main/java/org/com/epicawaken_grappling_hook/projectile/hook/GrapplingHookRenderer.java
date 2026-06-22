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
import org.com.epicawaken_grappling_hook.client.GrapplingHookLineDebugControls;
import org.com.epicawaken_grappling_hook.item.ModItems;
import org.com.epicawaken_grappling_hook.util.ArmatureUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class GrapplingHookRenderer extends EntityRenderer<GrapplingHook> {
    private static final ResourceLocation HOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "textures/item/grappling_hook.png");
    public static final ResourceLocation PROJECTILE_MODEL = ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "item/grappling_hook_projectile");
    private static final float PROJECTILE_MODEL_SCALE = 0.85F;
    private static final float PROJECTILE_MODEL_CENTER_X = 0.0F;
    private static final float PROJECTILE_MODEL_CENTER_Y = 1.9877725F;
    private static final float PROJECTILE_MODEL_CENTER_Z = 0.34375F;
    private static final int LEASH_RENDER_STEPS = 24;
    private static final float LEASH_WIDTH = 0.08F;
    private static final float LEASH_SAG = 0.18F;

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
            this.renderConnectionLine(grapplingHook, player, partialTicks, poseStack, bufferSource, packedLight);
            poseStack.popPose();
            this.renderHook(grapplingHook, partialTicks, poseStack, bufferSource, packedLight);
            super.render(grapplingHook, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        }
    }

    private void renderHook(GrapplingHook grapplingHook, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, grapplingHook.yRotO, grapplingHook.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, grapplingHook.xRotO, grapplingHook.getXRot())));
        poseStack.scale(PROJECTILE_MODEL_SCALE, PROJECTILE_MODEL_SCALE, PROJECTILE_MODEL_SCALE);
        poseStack.translate(-PROJECTILE_MODEL_CENTER_X, -PROJECTILE_MODEL_CENTER_Y, -PROJECTILE_MODEL_CENTER_Z);
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

    private void renderConnectionLine(GrapplingHook grapplingHook, Player player, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
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
        float x = (float) vector.x;
        float y = (float) vector.y;
        float z = (float) vector.z;
        float horizontalLengthSqr = x * x + z * z;
        float sideX;
        float sideZ;
        if (horizontalLengthSqr < 1.0E-6F) {
            sideX = LEASH_WIDTH * 0.5F;
            sideZ = 0.0F;
        } else {
            float invHorizontalLength = Mth.invSqrt(horizontalLengthSqr) * LEASH_WIDTH * 0.5F;
            sideX = z * invHorizontalLength;
            sideZ = -x * invHorizontalLength;
        }

        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();
        for (int step = 0; step <= LEASH_RENDER_STEPS; step++) {
            addLeashVertexPair(lineConsumer, matrix, x, y, z, sideX, sideZ, packedLight, step, false);
        }
        for (int step = LEASH_RENDER_STEPS; step >= 0; step--) {
            addLeashVertexPair(lineConsumer, matrix, x, y, z, sideX, sideZ, packedLight, step, true);
        }
    }

    private static void addLeashVertexPair(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float sideX,
            float sideZ,
            int packedLight,
            int step,
            boolean reverse) {
        float t = (float) step / LEASH_RENDER_STEPS;
        float shade = (step % 2 == (reverse ? 1 : 0)) ? 0.7F : 1.0F;
        float px = x * t;
        float py = y * t - LEASH_SAG * t * (1.0F - t);
        float pz = z * t;
        float firstYOffset = reverse ? 0.0F : LEASH_WIDTH;
        float secondYOffset = LEASH_WIDTH - firstYOffset;
        float red = GrapplingHookLineDebugControls.red(step) * shade;
        float green = GrapplingHookLineDebugControls.green(step) * shade;
        float blue = GrapplingHookLineDebugControls.blue(step) * shade;

        consumer.vertex(matrix, px + sideX, py + firstYOffset, pz + sideZ)
                .color(red, green, blue, 1.0F)
                .uv2(packedLight)
                .endVertex();
        consumer.vertex(matrix, px - sideX, py + secondYOffset, pz - sideZ)
                .color(red, green, blue, 1.0F)
                .uv2(packedLight)
                .endVertex();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull GrapplingHook grapplingHook) {
        return HOOK_TEXTURE;
    }
}
