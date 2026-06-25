package org.com.epicawaken_grappling_hook.projectile.hook;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.client.ClientMissedHookRopeRetractTracker;
import org.com.epicawaken_grappling_hook.client.GrapplingHookRenderDebugControls;
import org.com.epicawaken_grappling_hook.item.ModItems;
import org.com.epicawaken_grappling_hook.util.ArmatureUtil;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import java.util.HashMap;
import java.util.Map;

public class GrapplingHookRenderer extends EntityRenderer<GrapplingHook> {
    private static final ResourceLocation HOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "textures/item/grappling_hook.png");
    public static final ResourceLocation PROJECTILE_MODEL = ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "item/grappling_hook_projectile");
    private static final float PROJECTILE_MODEL_CENTER_X = 0.0F;
    private static final float PROJECTILE_MODEL_CENTER_Y = 1.9877725F;
    private static final float PROJECTILE_MODEL_CENTER_Z = 0.34375F;
    private static final float ROTATION_LOG_THRESHOLD = 5.0F;
    private static final Map<Integer, RotationSample> ROTATION_SAMPLES = new HashMap<>();
    private static final Map<Integer, RenderRotation> RENDER_ROTATIONS = new HashMap<>();

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
            this.renderHook(grapplingHook, player, partialTicks, poseStack, bufferSource, packedLight);
            super.render(grapplingHook, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        }
    }

    private void renderHook(GrapplingHook grapplingHook, Player owner, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float entityYaw = Mth.lerp(partialTicks, grapplingHook.yRotO, grapplingHook.getYRot());
        float entityPitch = Mth.lerp(partialTicks, grapplingHook.xRotO, grapplingHook.getXRot());
        RenderRotation renderRotation = renderRotation(grapplingHook, entityYaw, entityPitch);
        logRotationChange(grapplingHook, owner, partialTicks, entityYaw, entityPitch);

        poseStack.pushPose();
        Vec3 visualOffset = visualHookOffset(grapplingHook, owner, partialTicks);
        poseStack.translate(visualOffset.x, visualOffset.y, visualOffset.z);
        orientLikeProjectile(poseStack, renderRotation.yaw, renderRotation.pitch);
        renderProjectileModel(poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    private static Vec3 visualHookOffset(GrapplingHook grapplingHook, Player owner, float partialTicks) {
        PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(owner, PlayerPatch.class);
        if (playerPatch == null) {
            return Vec3.ZERO;
        }

        Joint handJoint = playerPatch.getArmature().searchJointByName("Hand_L");
        if (handJoint == null) {
            return Vec3.ZERO;
        }

        Vec3 actualHookPos = new Vec3(
                Mth.lerp(partialTicks, grapplingHook.xo, grapplingHook.getX()),
                Mth.lerp(partialTicks, grapplingHook.yo, grapplingHook.getY()),
                Mth.lerp(partialTicks, grapplingHook.zo, grapplingHook.getZ()));
        Vec3 handPos = ArmatureUtil.getJointWorldPos(
                playerPatch,
                handJoint,
                GrapplingHookRenderDebugControls.ropeHandLocalOffset(playerPatch),
                partialTicks);
        Vec3 visualHookPos = ClientMissedHookRopeRetractTracker.getVisualHookPos(grapplingHook, owner, playerPatch, actualHookPos, handPos);
        return visualHookPos.subtract(actualHookPos);
    }

    private static RenderRotation renderRotation(GrapplingHook grapplingHook, float entityYaw, float entityPitch) {
        int id = grapplingHook.getId();
        RenderRotation cached = RENDER_ROTATIONS.get(id);
        if (shouldFreezeUnhitRenderRotation(grapplingHook)) {
            if (cached == null) {
                cached = new RenderRotation(entityYaw, entityPitch);
                RENDER_ROTATIONS.put(id, cached);
            }
            return cached;
        }

        RenderRotation current = new RenderRotation(entityYaw, entityPitch);
        RENDER_ROTATIONS.put(id, current);
        return current;
    }

    private static boolean shouldFreezeUnhitRenderRotation(GrapplingHook grapplingHook) {
        return grapplingHook.isHookedForDebug()
                && !grapplingHook.isInGroundForDebug()
                && grapplingHook.getHookType() != GrapplingHook.HookType.ENTITY
                && grapplingHook.getDeltaMovement().lengthSqr() <= 1.0E-6D;
    }

    private static void logRotationChange(GrapplingHook grapplingHook, Player owner, float partialTicks, float yaw, float pitch) {
        if (!Config.debugLogging) {
            return;
        }

        int id = grapplingHook.getId();
        RotationSample previous = ROTATION_SAMPLES.get(id);
        boolean hooked = grapplingHook.isHookedForDebug();
        boolean inGround = grapplingHook.isInGroundForDebug();
        GrapplingHook.HookType hookType = grapplingHook.getHookType();
        if (previous == null) {
            ROTATION_SAMPLES.put(id, new RotationSample(yaw, pitch, hooked, inGround, hookType));
            logRotationSample("initial", grapplingHook, owner, partialTicks, yaw, pitch, 0.0F, 0.0F);
            return;
        }

        float yawDelta = Mth.wrapDegrees(yaw - previous.yaw);
        float pitchDelta = Mth.wrapDegrees(pitch - previous.pitch);
        boolean rotationChanged = Math.abs(yawDelta) >= ROTATION_LOG_THRESHOLD || Math.abs(pitchDelta) >= ROTATION_LOG_THRESHOLD;
        boolean stateChanged = previous.hooked != hooked || previous.inGround != inGround || previous.hookType != hookType;
        if (rotationChanged || stateChanged) {
            logRotationSample(stateChanged ? "state_changed" : "rotation_changed", grapplingHook, owner, partialTicks, yaw, pitch, yawDelta, pitchDelta);
            ROTATION_SAMPLES.put(id, new RotationSample(yaw, pitch, hooked, inGround, hookType));
        }
    }

    private static void logRotationSample(String reason, GrapplingHook grapplingHook, Player owner, float partialTicks, float yaw, float pitch, float yawDelta, float pitchDelta) {
        Vec3 movement = grapplingHook.getDeltaMovement();
        PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(owner, PlayerPatch.class);
        String realAnimation = "none";
        String rawAnimation = "none";
        if (playerPatch != null) {
            AnimationPlayer animationPlayer = playerPatch.getAnimator().getPlayerFor(null);
            if (animationPlayer != null) {
                realAnimation = animationName(animationPlayer.getRealAnimation());
                rawAnimation = animationName(animationPlayer.getAnimation());
            }
        }

        Epicawaken_grappling_hook.LOGGER.info(
                "[GrapplingHookProjectileRotationDebug] reason={} entity={} life={} partial={} yaw={} pitch={} yawDelta={} pitchDelta={} yRotO={} yRot={} xRotO={} xRot={} movement={} movementLenSqr={} hooked={} inGround={} hookType={} terrainTarget={} ownerDistance={} ownerYaw={} ownerPitch={} ownerBodyYaw={} realAnimation={} rawAnimation={}",
                reason,
                grapplingHook.getId(),
                grapplingHook.getLifeForDebug(),
                partialTicks,
                yaw,
                pitch,
                yawDelta,
                pitchDelta,
                grapplingHook.yRotO,
                grapplingHook.getYRot(),
                grapplingHook.xRotO,
                grapplingHook.getXRot(),
                movement,
                movement.lengthSqr(),
                grapplingHook.isHookedForDebug(),
                grapplingHook.isInGroundForDebug(),
                grapplingHook.getHookType(),
                grapplingHook.getTerrainTargetForDebug(),
                owner.distanceTo(grapplingHook),
                owner.getYRot(),
                owner.getXRot(),
                owner.yBodyRot,
                realAnimation,
                rawAnimation);
    }

    private static String animationName(AssetAccessor<?> animation) {
        return animation == null ? "null" : String.valueOf(animation.registryName());
    }

    public static void orientLikeProjectile(PoseStack poseStack, float yaw, float pitch) {
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
    }

    public static void orientToDirection(PoseStack poseStack, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 normalized = direction.normalize();
        double horizontalLength = Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z);
        float yaw = (float) (Mth.atan2(normalized.z, normalized.x) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(normalized.y, horizontalLength) * Mth.RAD_TO_DEG);
        orientLikeProjectile(poseStack, yaw, pitch);
    }

    public static void renderProjectileModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        GrapplingHookRenderDebugControls.applyProjectileArrowTransform(poseStack);
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

    private record RotationSample(float yaw, float pitch, boolean hooked, boolean inGround, GrapplingHook.HookType hookType) {
    }

    private record RenderRotation(float yaw, float pitch) {
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull GrapplingHook grapplingHook) {
        return HOOK_TEXTURE;
    }
}
