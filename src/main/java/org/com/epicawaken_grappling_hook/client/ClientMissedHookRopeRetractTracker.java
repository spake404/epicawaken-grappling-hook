package org.com.epicawaken_grappling_hook.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;
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
    private static final Map<Integer, RetractState> RETRACTS = new ConcurrentHashMap<>();
    private static long nextDebugLogTick;

    public static Vec3 getVisualRopeOrigin(GrapplingHook hook, Player owner, PlayerPatch<?> ownerPatch, Vec3 hookPos, Vec3 handPos) {
        return getVisualHookPos(hook, owner, ownerPatch, hookPos, handPos);
    }

    public static Vec3 getVisualHookPos(GrapplingHook hook, Player owner, PlayerPatch<?> ownerPatch, Vec3 hookPos, Vec3 handPos) {
        if (!GrapplingHookMissedTracker.hasMissed(owner)) {
            RETRACTS.remove(hook.getId());
            return hookPos;
        }

        AnimationPlayer animationPlayer = ownerPatch.getClientAnimator().baseLayer.animationPlayer;
        float elapsedTime = animationPlayer.getElapsedTime();
        if (!Float.isFinite(elapsedTime)) {
            return hookPos;
        }

        RetractState state = RETRACTS.computeIfAbsent(hook.getId(), id -> new RetractState(hookPos, elapsedTime));
        float progress = retractProgress(state.startElapsedTime, elapsedTime);
        Vec3 visualOrigin = lerp(state.startHookPos, handPos, progress);
        logRetractDebug(owner, hook, state, elapsedTime, progress, visualOrigin, handPos);
        return visualOrigin;
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

    private static void logRetractDebug(Player owner, GrapplingHook hook, RetractState state, float elapsedTime, float progress, Vec3 visualOrigin, Vec3 handPos) {
        if (!Config.debugLogging || owner.level().getGameTime() < nextDebugLogTick) {
            return;
        }

        nextDebugLogTick = owner.level().getGameTime() + 5L;
        Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookRopeRetractDebug][CLIENT] owner={} hook={} gameTime={} startElapsed={} elapsed={} endElapsed={} progress={} startHookPos={} visualOrigin={} handPos={}",
                owner.getId(),
                hook.getId(),
                owner.level().getGameTime(),
                state.startElapsedTime,
                elapsedTime,
                ModHookAnimations.HOOK_PULL_MOVEMENT_INTERRUPT_AT,
                progress,
                state.startHookPos,
                visualOrigin,
                handPos);
    }

    private record RetractState(Vec3 startHookPos, float startElapsedTime) {
    }

    private ClientMissedHookRopeRetractTracker() {
    }
}
