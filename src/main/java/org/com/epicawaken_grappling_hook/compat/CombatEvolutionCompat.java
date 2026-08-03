package org.com.epicawaken_grappling_hook.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class CombatEvolutionCompat {
    private static final String MOD_ID = "combat_evolution";

    public static boolean isGuarding(LivingEntityPatch<?> targetPatch) {
        return targetPatch != null
                && ModList.get().isLoaded(MOD_ID)
                && CombatEvolutionLoadedCompat.isGuarding(targetPatch);
    }

    public static boolean applyGuardBreak(
            ServerPlayer attacker,
            LivingEntity target,
            LivingEntityPatch<?> targetPatch) {
        return targetPatch != null
                && ModList.get().isLoaded(MOD_ID)
                && CombatEvolutionLoadedCompat.applyGuardBreak(attacker, target, targetPatch);
    }

    private CombatEvolutionCompat() {
    }
}
