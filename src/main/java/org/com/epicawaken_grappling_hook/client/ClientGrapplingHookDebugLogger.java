package org.com.epicawaken_grappling_hook.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.animation.ModHookAnimations;
import org.com.epicawaken_grappling_hook.util.GrapplingHookArrivalTracker;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

@OnlyIn(Dist.CLIENT)
public class ClientGrapplingHookDebugLogger {
    private static long nextLogTick;
    private static boolean wasTerrainHook;

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null) {
            wasTerrainHook = false;
            return;
        }

        var playerPatch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (playerPatch == null) {
            wasTerrainHook = false;
            return;
        }

        AnimationPlayer animationPlayer = playerPatch.getClientAnimator().baseLayer.animationPlayer;
        boolean isHookPull = animationPlayer.getRealAnimation() == ModHookAnimations.HOOK_PULL;
        boolean isHookAir = animationPlayer.getRealAnimation() == ModHookAnimations.HOOK_AIR;
        boolean isHookGround = animationPlayer.getRealAnimation() == ModHookAnimations.HOOK_GROUND;
        if (!isHookPull && !isHookAir && !isHookGround) {
            if (Config.debugLogging && wasTerrainHook) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][CLIENT] hook animation ended at gameTime={}", minecraft.level.getGameTime());
            }
            wasTerrainHook = false;
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        if (Config.debugLogging && (!wasTerrainHook || gameTime >= nextLogTick)) {
            EntityState state = playerPatch.getEntityState();
            Epicawaken_grappling_hook.LOGGER.info(
                    "[GrapplingHookDebug][CLIENT] {} tick gameTime={} elapsed={} arrived={} movementLocked={} inaction={} canBasicAttack={} canUseSkill={} updateLivingMotion={}",
                    currentHookAnimationName(isHookPull, isHookAir, isHookGround),
                    gameTime,
                    animationPlayer.getElapsedTime(),
                    GrapplingHookArrivalTracker.hasArrived(player),
                    state.movementLocked(),
                    state.inaction(),
                    state.canBasicAttack(),
                    state.canUseSkill(),
                    state.updateLivingMotion());
            nextLogTick = gameTime + 5L;
        }

        wasTerrainHook = true;
    }

    private static String currentHookAnimationName(boolean isHookPull, boolean isHookAir, boolean isHookGround) {
        if (isHookPull) {
            return "HOOK_PULL";
        }
        if (isHookAir) {
            return "HOOK_AIR";
        }
        if (isHookGround) {
            return "HOOK_GROUND";
        }
        return "UNKNOWN";
    }

    private ClientGrapplingHookDebugLogger() {
    }
}
