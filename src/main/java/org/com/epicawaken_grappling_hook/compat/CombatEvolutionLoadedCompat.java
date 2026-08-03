package org.com.epicawaken_grappling_hook.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

final class CombatEvolutionLoadedCompat {
    static boolean isGuarding(LivingEntityPatch<?> targetPatch) {
        return CEPatchUtils.isGuard(targetPatch);
    }

    static boolean applyGuardBreak(
            ServerPlayer attacker,
            LivingEntity target,
            LivingEntityPatch<?> targetPatch) {
        if (targetPatch instanceof CEHumanoidPatch<?> humanoidPatch) {
            humanoidPatch.onBreak(target.damageSources().playerAttack(attacker));
            return true;
        }

        CEPatchUtils.setGuard(targetPatch, false);
        return targetPatch.applyStun(StunType.NEUTRALIZE, 0.0F);
    }

    private CombatEvolutionLoadedCompat() {
    }
}
