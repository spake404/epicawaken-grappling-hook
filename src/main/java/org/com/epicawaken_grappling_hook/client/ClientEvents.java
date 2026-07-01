package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.entity.ModEntities;
import org.com.epicawaken_grappling_hook.item.ModItems;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.UseGrapplingHookPacket;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHook;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHookRenderer;
import org.com.epicawaken_grappling_hook.util.ArmatureUtil;
import org.com.epicawaken_grappling_hook.util.GrapplingHookParcoolBlocker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookMissedTracker;
import org.lwjgl.glfw.GLFW;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.config.ClientConfig;
import net.minecraft.world.phys.Vec3;

public class ClientEvents {
    public static final KeyMapping USE_GRAPPLING_HOOK = new KeyMapping(
            "key.epicawaken_grappling_hook.use_grappling_hook",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories.epicawaken_grappling_hook");

    private ClientEvents() {
    }

    @Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(USE_GRAPPLING_HOOK);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.GRAPPLING_HOOK.get(), GrapplingHookRenderer::new);
        }

        @SubscribeEvent
        public static void addEntityRendererLayers(EntityRenderersEvent.AddLayers event) {
            for (String skinName : event.getSkins()) {
                PlayerRenderer renderer = event.getPlayerSkin(skinName);
                if (renderer != null) {
                    renderer.addLayer(new GrapplingHookOffhandPlayerLayer(renderer));
                }
            }
        }

        @SubscribeEvent
        public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
            event.register(GrapplingHookArmModelRenderer.ARM_MODEL);
            event.register(GrapplingHookArmModelRenderer.ARM_PULL_MODEL);
            event.register(GrapplingHookRenderer.PROJECTILE_MODEL);
        }

        @SubscribeEvent
        public static void onModelBakingCompleted(ModelEvent.BakingCompleted event) {
            GrapplingHookRenderPathDebug.logLifecycle("model baking completed; resource reload may have rebuilt client renderers");
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> CuriosRendererRegistry.register(ModItems.GRAPPLING_HOOK.get(), GrapplingHookCurioRenderer::new));
        }

        @SubscribeEvent
        public static void onModifyPatchedRenderers(PatchedRenderersEvent.Modify event) {
            GrapplingHookRenderPathDebug.logLifecycle("PatchedRenderersEvent.Modify fired; installing Epic Fight grappling hook layers");
            EpicFightGrapplingHookArmLayer.onModifyPatchedRenderers(event);
        }
    }

    @Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
        private static boolean previewYawLocked;
        private static float lockedPreviewYaw;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            GrapplingHookOffhandRenderState.restoreAll();
            ClientGrapplingHookJumpInputSync.tick();
            while (USE_GRAPPLING_HOOK.consumeClick()) {
                GrapplingHookParcoolBlocker.block(net.minecraft.client.Minecraft.getInstance().player, 8);
                ClientGrapplingHookSprintRestore.recordUseAttempt();
                ClientGrapplingHookFovEffect.recordUseAttempt();
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new UseGrapplingHookPacket());
            }
            if (ClientGrapplingHookSprintRestore.hasWork()) {
                ClientGrapplingHookSprintRestore.tick();
            }
            if (ClientGrapplingHookWallRunBridge.hasOpenWindow()) {
                ClientGrapplingHookWallRunBridge.tick();
            }
            if (Config.debugLogging) {
                ClientGrapplingHookDebugLogger.tick();
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onComputeFovModifier(ComputeFovModifierEvent event) {
            ClientGrapplingHookFovEffect.onComputeFovModifier(event);
        }

        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (event.getHand() == InteractionHand.OFF_HAND
                    && minecraft.player != null
                    && shouldHideFirstPersonOffhandHook(minecraft.player)
                    && GrapplingHookEquipmentLookup.isGrapplingHookStack(event.getItemStack())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
            GrapplingHookOffhandRenderState.hideForPlayerRender(event.getEntity());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
            GrapplingHookOffhandRenderState.restoreAfterPlayerRender(event.getEntity());
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }

            float partialTicks = event.getPartialTick();
            Vec3 cameraPos = event.getCamera().getPosition();
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
            boolean renderedLocalActualRope = renderActualHookRopes(minecraft, partialTicks, cameraPos, poseStack, bufferSource);

            PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(minecraft.player, PlayerPatch.class);
            if (playerPatch == null) {
                if (renderedLocalActualRope) {
                    bufferSource.endBatch();
                }
                return;
            }

            boolean manualPreview = GrapplingHookLineDebugControls.isPreviewEnabled();
            boolean shouldRenderPreview = manualPreview;
            if (!shouldRenderPreview) {
                previewYawLocked = false;
                if (renderedLocalActualRope) {
                    bufferSource.endBatch();
                }
                return;
            }

            Joint handJoint = playerPatch.getArmature().searchJointByName("Hand_L");
            if (handJoint == null) {
                if (renderedLocalActualRope) {
                    bufferSource.endBatch();
                }
                return;
            }

            Vec3 handPos = ArmatureUtil.getJointWorldPos(
                    playerPatch,
                    handJoint,
                    GrapplingHookRenderDebugControls.ropeHandLocalOffset(playerPatch),
                    partialTicks);
            Vec3 missedP2Pos = ArmatureUtil.getJointWorldPos(
                    playerPatch,
                    handJoint,
                    GrapplingHookRenderDebugControls.ropeMissedP2LocalOffset(),
                    partialTicks);
            float previewYaw = previewYaw(minecraft.player, partialTicks, manualPreview);
            Vec3 previewHookPos = handPos.add(forwardFromYaw(previewYaw).scale(3.0D));
            renderWorldShortRope(missedP2Pos, handPos, cameraPos, poseStack, bufferSource);
            renderWorldShortRope(previewHookPos, missedP2Pos, cameraPos, poseStack, bufferSource);
            renderWorldProjectile(previewHookPos, previewYaw, 0.0F, cameraPos, poseStack, bufferSource);
            bufferSource.endBatch();
        }

        private static boolean renderActualHookRopes(Minecraft minecraft, float partialTicks, Vec3 cameraPos, PoseStack poseStack, MultiBufferSource bufferSource) {
            boolean renderedLocalPlayerRope = false;
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (!(entity instanceof GrapplingHook grapplingHook) || !(grapplingHook.getOwner() instanceof Player player)) {
                    continue;
                }

                PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
                if (playerPatch == null) {
                    continue;
                }

                Joint handJoint = playerPatch.getArmature().searchJointByName("Hand_L");
                if (handJoint == null) {
                    continue;
                }

                Vec3 handPos = ArmatureUtil.getJointWorldPos(
                        playerPatch,
                        handJoint,
                        GrapplingHookRenderDebugControls.ropeHandLocalOffset(playerPatch),
                        partialTicks);
                Vec3 hookPos = new Vec3(
                        Mth.lerp(partialTicks, grapplingHook.xo, grapplingHook.getX()),
                        Mth.lerp(partialTicks, grapplingHook.yo, grapplingHook.getY()),
                        Mth.lerp(partialTicks, grapplingHook.zo, grapplingHook.getZ()));
                if (!GrapplingHookMissedTracker.hasMissed(player)) {
                    renderWorldRope(hookPos, handPos, cameraPos, poseStack, bufferSource);
                    if (player == minecraft.player) {
                        renderedLocalPlayerRope = true;
                    }
                    continue;
                }

                Vec3 missedP2Pos = ArmatureUtil.getJointWorldPos(
                        playerPatch,
                        handJoint,
                        GrapplingHookRenderDebugControls.ropeMissedP2LocalOffset(),
                        partialTicks);
                ClientMissedHookRopeRetractTracker.RopeRenderState ropeRenderState =
                        ClientMissedHookRopeRetractTracker.getRopeRenderState(grapplingHook, player, playerPatch, hookPos, handPos, missedP2Pos);
                if (ropeRenderState.hasBend()) {
                    renderWorldShortRope(ropeRenderState.bendPos(), handPos, cameraPos, poseStack, bufferSource);
                    renderWorldShortRope(ropeRenderState.visualHookPos(), ropeRenderState.bendPos(), cameraPos, poseStack, bufferSource);
                } else {
                    renderWorldShortRope(ropeRenderState.visualHookPos(), handPos, cameraPos, poseStack, bufferSource);
                }
                if (player == minecraft.player) {
                    renderedLocalPlayerRope = true;
                }
            }
            return renderedLocalPlayerRope;
        }

        private static void renderWorldRope(Vec3 ropeOrigin, Vec3 ropeTarget, Vec3 cameraPos, PoseStack poseStack, MultiBufferSource bufferSource) {
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            poseStack.translate(ropeOrigin.x, ropeOrigin.y, ropeOrigin.z);
            GrapplingHookRopeRenderUtil.renderLocal(ropeTarget.subtract(ropeOrigin), poseStack, bufferSource, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }

        private static void renderWorldShortRope(Vec3 ropeOrigin, Vec3 ropeTarget, Vec3 cameraPos, PoseStack poseStack, MultiBufferSource bufferSource) {
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            poseStack.translate(ropeOrigin.x, ropeOrigin.y, ropeOrigin.z);
            GrapplingHookRopeRenderUtil.renderShortLocal(ropeTarget.subtract(ropeOrigin), poseStack, bufferSource, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }

        private static void renderWorldProjectile(Vec3 projectilePos, float yaw, float pitch, Vec3 cameraPos, PoseStack poseStack, MultiBufferSource bufferSource) {
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            poseStack.translate(projectilePos.x, projectilePos.y, projectilePos.z);
            GrapplingHookRenderer.orientLikeProjectile(poseStack, yaw, pitch);
            GrapplingHookRenderer.renderProjectileModel(poseStack, bufferSource, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }

        private static float previewYaw(Player player, float partialTicks, boolean manualPreview) {
            if (!manualPreview) {
                previewYawLocked = false;
                return bodyYaw(player, partialTicks);
            }
            if (!previewYawLocked) {
                lockedPreviewYaw = bodyYaw(player, partialTicks);
                previewYawLocked = true;
            }
            return lockedPreviewYaw;
        }

        private static Vec3 forwardFromYaw(float yawDegrees) {
            float yaw = yawDegrees * Mth.DEG_TO_RAD;
            return new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw)).normalize();
        }

        private static float bodyYaw(Player player, float partialTicks) {
            return Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot);
        }

        private static boolean shouldHideFirstPersonOffhandHook(Player player) {
            return ClientConfig.enableAnimatedFirstPersonModel
                    && EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class) != null;
        }
    }
}
