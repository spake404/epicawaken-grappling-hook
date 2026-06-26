package org.com.epicawaken_grappling_hook.client;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.animation.ModHookAnimations;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHook;
import org.com.epicawaken_grappling_hook.util.GrapplingHookMissedTracker;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class ClientMissedHookRopeRetractTracker {
    private static final float P2_ARRIVAL_PROGRESS = 0.65F;
    private static final Map<Integer, RetractState> RETRACTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> OWNER_RETRACT_START_ELAPSED = new ConcurrentHashMap<>();
    private static final Set<UUID> OWNER_RETRACT_COMPLETED = ConcurrentHashMap.newKeySet();
    private static long nextDebugLogTick;

    public static RopeRenderState getRopeRenderState(GrapplingHook hook, Player owner, PlayerPatch<?> ownerPatch, Vec3 hookPos, Vec3 handPos, Vec3 p2Pos) {
        if (!GrapplingHookMissedTracker.hasMissed(owner)) {
            RETRACTS.remove(hook.getId());
            clearOwner(owner);
            return RopeRenderState.single(hookPos);
        }

        if (OWNER_RETRACT_COMPLETED.contains(owner.getUUID())) {
            return RopeRenderState.afterP2(p2Pos);
        }

        AnimationPlayer animationPlayer = ownerPatch.getClientAnimator().baseLayer.animationPlayer;
        float elapsedTime = animationPlayer.getElapsedTime();
        if (!Float.isFinite(elapsedTime)) {
            return RopeRenderState.single(hookPos);
        }

        RetractState state = RETRACTS.computeIfAbsent(hook.getId(), id -> new RetractState(hookPos, elapsedTime));
        OWNER_RETRACT_START_ELAPSED.putIfAbsent(owner.getUUID(), state.startElapsedTime);
        float progress = retractProgress(state.startElapsedTime, elapsedTime);
        if (progress >= P2_ARRIVAL_PROGRESS) {
            OWNER_RETRACT_COMPLETED.add(owner.getUUID());
        }
        RopeRenderState renderState = retractState(state.startHookPos, handPos, p2Pos, progress);
        logRetractDebug(owner, hook, state, elapsedTime, progress, renderState, handPos, p2Pos);
        return renderState;
    }

    public static void markNewMissed(Entity owner) {
        if (owner == null) {
            return;
        }

        OWNER_RETRACT_START_ELAPSED.remove(owner.getUUID());
        OWNER_RETRACT_COMPLETED.remove(owner.getUUID());
    }

    public static boolean isBeforeP2Arrival(LivingEntity owner, float elapsedTime) {
        if (owner == null || !Float.isFinite(elapsedTime)) {
            return false;
        }

        if (!GrapplingHookMissedTracker.hasMissed(owner)) {
            clearOwner(owner);
            return false;
        }

        if (OWNER_RETRACT_COMPLETED.contains(owner.getUUID())) {
            return false;
        }

        Float startElapsedTime = OWNER_RETRACT_START_ELAPSED.get(owner.getUUID());
        if (startElapsedTime == null) {
            startElapsedTime = elapsedTime;
            OWNER_RETRACT_START_ELAPSED.put(owner.getUUID(), startElapsedTime);
        }

        boolean beforeP2Arrival = retractProgress(startElapsedTime, elapsedTime) < P2_ARRIVAL_PROGRESS;
        if (!beforeP2Arrival) {
            OWNER_RETRACT_COMPLETED.add(owner.getUUID());
        }
        return beforeP2Arrival;
    }

    private static void clearOwner(Entity owner) {
        if (owner == null) {
            return;
        }

        OWNER_RETRACT_START_ELAPSED.remove(owner.getUUID());
        OWNER_RETRACT_COMPLETED.remove(owner.getUUID());
    }

    private static RopeRenderState retractState(Vec3 startHookPos, Vec3 handPos, Vec3 p2Pos, float progress) {
        if (progress < P2_ARRIVAL_PROGRESS) {
            float p2Progress = Mth.clamp(progress / P2_ARRIVAL_PROGRESS, 0.0F, 1.0F);
            return new RopeRenderState(lerp(startHookPos, p2Pos, p2Progress), p2Pos, true, true);
        }

        return RopeRenderState.afterP2(p2Pos);
    }

    private static float retractProgress(float startElapsedTime, float elapsedTime) {
        float endElapsedTime = ModHookAnimations.HOOK_PULL_MOVEMENT_INTERRUPT_AT;
        if (startElapsedTime >= endElapsedTime) {
            return 1.0F;
        }

        float duration = Math.max(0.001F, endElapsedTime - startElapsedTime);
        return Mth.clamp((elapsedTime - startElapsedTime) / duration, 0.0F, 1.0F);
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, float progress) {
        return new Vec3(
                Mth.lerp(progress, from.x, to.x),
                Mth.lerp(progress, from.y, to.y),
                Mth.lerp(progress, from.z, to.z));
    }

    private static void logRetractDebug(Player owner, GrapplingHook hook, RetractState state, float elapsedTime, float progress, RopeRenderState renderState, Vec3 handPos, Vec3 p2Pos) {
        if (!Config.debugLogging || owner.level().getGameTime() < nextDebugLogTick) {
            return;
        }

        nextDebugLogTick = owner.level().getGameTime() + 5L;
        Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookRopeRetractDebug][CLIENT] owner={} hook={} gameTime={} startElapsed={} elapsed={} endElapsed={} progress={} p2ArrivalProgress={} startHookPos={} visualHookPos={} handPos={} p2Pos={} hasBend={} hookVisible={}",
                owner.getId(),
                hook.getId(),
                owner.level().getGameTime(),
                state.startElapsedTime,
                elapsedTime,
                ModHookAnimations.HOOK_PULL_MOVEMENT_INTERRUPT_AT,
                progress,
                P2_ARRIVAL_PROGRESS,
                state.startHookPos,
                renderState.visualHookPos(),
                handPos,
                p2Pos,
                renderState.hasBend(),
                renderState.hookVisible());
    }

    public record RopeRenderState(Vec3 visualHookPos, Vec3 bendPos, boolean hasBend, boolean hookVisible) {
        private static RopeRenderState single(Vec3 visualHookPos) {
            return new RopeRenderState(visualHookPos, Vec3.ZERO, false, true);
        }

        private static RopeRenderState afterP2(Vec3 p2Pos) {
            return new RopeRenderState(p2Pos, p2Pos, true, false);
        }
    }

    private record RetractState(Vec3 startHookPos, float startElapsedTime) {
    }

    private ClientMissedHookRopeRetractTracker() {
    }
}
