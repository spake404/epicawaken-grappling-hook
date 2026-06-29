package org.com.epicawaken_grappling_hook.client;

import java.util.HashMap;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

public final class GrapplingHookRenderPathDebug {
    private static final Map<String, Integer> LAST_RENDER_LOG_TICKS = new HashMap<>();
    private static final Map<String, RenderCallCounter> RENDER_CALLS = new HashMap<>();
    private static final Map<String, Long> LAST_MODEL_LOG_TICKS = new HashMap<>();
    private static final Map<String, Long> LAST_POSE_LOG_TICKS = new HashMap<>();

    private GrapplingHookRenderPathDebug() {
    }

    public static void logLifecycle(String message, Object... args) {
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookRenderPathDebug] " + message, args);
        }
    }

    public static void logRenderPath(String path, LivingEntity entity, boolean pullModel, String detail) {
        if (!Config.debugLogging || entity == null) {
            return;
        }

        String key = path + ":" + entity.getId() + ":" + pullModel;
        int tick = entity.tickCount;
        int callIndex = nextRenderCallIndex(key, tick);
        Integer lastTick = LAST_RENDER_LOG_TICKS.get(key);
        if (lastTick != null && tick - lastTick < 20) {
            return;
        }

        LAST_RENDER_LOG_TICKS.put(key, tick);
        Epicawaken_grappling_hook.LOGGER.info(
                "[GrapplingHookRenderPathDebug] path={} entity={} name={} tick={} callIndex={} pullModel={} detail={}",
                path,
                entity.getId(),
                entity.getScoreboardName(),
                tick,
                callIndex,
                pullModel,
                detail);
    }

    public static void logModelRender(Object modelLocation, BakedModel model, ItemDisplayContext displayContext, ItemTransform transform) {
        if (!Config.debugLogging || model == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        long tick = minecraft.level != null ? minecraft.level.getGameTime() : -1L;
        String key = String.valueOf(modelLocation) + ":" + displayContext;
        Long lastTick = LAST_MODEL_LOG_TICKS.get(key);
        if (lastTick != null && tick >= 0L && tick - lastTick < 40L) {
            return;
        }

        LAST_MODEL_LOG_TICKS.put(key, tick);
        Epicawaken_grappling_hook.LOGGER.info(
                "[GrapplingHookRenderPathDebug] modelRender model={} modelClass={} modelIdentity={} displayContext={} transformIdentity={} transformValues={}",
                modelLocation,
                model.getClass().getName(),
                System.identityHashCode(model),
                displayContext,
                transform == null ? 0 : System.identityHashCode(transform),
                formatItemTransform(transform));
    }

    public static void logJointMatrix(String path, LivingEntity entity, boolean pullModel, String jointName, int jointId, OpenMatrix4f matrix) {
        if (!Config.debugLogging || entity == null || matrix == null) {
            return;
        }

        Epicawaken_grappling_hook.LOGGER.info(
                "[GrapplingHookRenderPathDebug] jointMatrix path={} entity={} name={} tick={} pullModel={} joint={} id={} matrix4x4=[{},{},{},{}; {},{},{},{}; {},{},{},{}; {},{},{},{}] translationCandidates=row=({},{},{}) column=({},{},{})",
                path,
                entity.getId(),
                entity.getScoreboardName(),
                entity.tickCount,
                pullModel,
                jointName,
                jointId,
                matrix.m00,
                matrix.m01,
                matrix.m02,
                matrix.m03,
                matrix.m10,
                matrix.m11,
                matrix.m12,
                matrix.m13,
                matrix.m20,
                matrix.m21,
                matrix.m22,
                matrix.m23,
                matrix.m30,
                matrix.m31,
                matrix.m32,
                matrix.m33,
                matrix.m30,
                matrix.m31,
                matrix.m32,
                matrix.m03,
                matrix.m13,
                matrix.m23);
    }

    public static void logPoseTop(String stage, LivingEntity entity, boolean pullModel, PoseStack poseStack) {
        if (!Config.debugLogging || entity == null || poseStack == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        long tick = minecraft.level != null ? minecraft.level.getGameTime() : entity.tickCount;
        String key = stage + ":" + entity.getId() + ":" + pullModel;
        Long lastTick = LAST_POSE_LOG_TICKS.get(key);
        if (lastTick != null && tick - lastTick < 20L) {
            return;
        }

        LAST_POSE_LOG_TICKS.put(key, tick);
        Matrix4f matrix = poseStack.last().pose();
        Epicawaken_grappling_hook.LOGGER.info(
                "[GrapplingHookRenderPathDebug] poseTop stage={} entity={} name={} entityTick={} gameTime={} pullModel={} matrix4x4=[{},{},{},{}; {},{},{},{}; {},{},{},{}; {},{},{},{}] translation=({},{},{})",
                stage,
                entity.getId(),
                entity.getScoreboardName(),
                entity.tickCount,
                tick,
                pullModel,
                matrix.m00(),
                matrix.m01(),
                matrix.m02(),
                matrix.m03(),
                matrix.m10(),
                matrix.m11(),
                matrix.m12(),
                matrix.m13(),
                matrix.m20(),
                matrix.m21(),
                matrix.m22(),
                matrix.m23(),
                matrix.m30(),
                matrix.m31(),
                matrix.m32(),
                matrix.m33(),
                matrix.m30(),
                matrix.m31(),
                matrix.m32());
    }

    private static String formatItemTransform(ItemTransform transform) {
        if (transform == null) {
            return "<null>";
        }

        return "rotation=" + formatVector(transform.rotation)
                + " translation=" + formatVector(transform.translation)
                + " scale=" + formatVector(transform.scale);
    }

    private static String formatVector(Vector3f vector) {
        if (vector == null) {
            return "<null>";
        }

        return "(" + vector.x() + "," + vector.y() + "," + vector.z() + ")";
    }

    private static int nextRenderCallIndex(String key, int tick) {
        RenderCallCounter counter = RENDER_CALLS.get(key);
        if (counter == null || counter.tick != tick) {
            counter = new RenderCallCounter(tick, 1);
            RENDER_CALLS.put(key, counter);
            return 1;
        }

        counter.count++;
        return counter.count;
    }

    private static final class RenderCallCounter {
        private final int tick;
        private int count;

        private RenderCallCounter(int tick, int count) {
            this.tick = tick;
            this.count = count;
        }
    }
}
