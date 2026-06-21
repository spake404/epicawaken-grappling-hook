package org.com.epicawaken_grappling_hook.util;

import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.animation.ModHookAnimations;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;

public class AwakenAnimationHooks {
    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }

        ActionAnimation hookPull = ModHookAnimations.HOOK_PULL.get();
        if (hookPull == null) {
            Epicawaken_grappling_hook.LOGGER.warn("Failed to configure custom hook_pull speed: animation is not available yet.");
            return;
        }

        hookPull.addProperty(
                AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER,
                (self, entityPatch, speed, prevElapsedTime, elapsedTime) -> {
                    if (GrapplingHookUse.hasActiveConfiguredUse(entityPatch.getOriginal())) {
                        return speed * (float) Config.hookPullAnimationSpeed;
                    }
                    return speed;
                });
        registered = true;
    }

    private AwakenAnimationHooks() {
    }
}
