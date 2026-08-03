package org.com.epicawaken_grappling_hook.util;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.com.epicawaken_grappling_hook.compat.CombatEvolutionCompat;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.damagesource.StunType;

public final class GrapplingShieldDisarmHandler {
    private static final int VANILLA_SHIELD_DISABLE_TICKS = 100;

    public static boolean tryDisarm(ServerPlayer attacker, LivingEntity target) {
        LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
        boolean vanillaBlocking = target.isBlocking();
        boolean epicFightGuarding = isEpicFightGuarding(targetPatch);
        boolean combatEvolutionGuarding = CombatEvolutionCompat.isGuarding(targetPatch);
        if (!vanillaBlocking && !epicFightGuarding && !combatEvolutionGuarding) {
            return false;
        }

        ItemStack shieldStack = findShieldStack(target);
        target.stopUsingItem();
        cancelEpicFightGuard(targetPatch);
        if (target instanceof Player player && !shieldStack.isEmpty()) {
            player.getCooldowns().addCooldown(shieldStack.getItem(), VANILLA_SHIELD_DISABLE_TICKS);
        }

        if (CombatEvolutionCompat.applyGuardBreak(attacker, target, targetPatch)) {
            return true;
        }
        return targetPatch != null && targetPatch.applyStun(StunType.NEUTRALIZE, 0.0F);
    }

    private static boolean isEpicFightGuarding(LivingEntityPatch<?> targetPatch) {
        if (!(targetPatch instanceof PlayerPatch<?> playerPatch)) {
            return false;
        }
        SkillContainer guardContainer = playerPatch.getSkill(SkillSlots.GUARD);
        return guardContainer != null
                && guardContainer.getSkill() instanceof GuardSkill
                && guardContainer.isActivated();
    }

    private static void cancelEpicFightGuard(LivingEntityPatch<?> targetPatch) {
        if (!(targetPatch instanceof PlayerPatch<?> playerPatch)) {
            return;
        }
        SkillContainer guardContainer = playerPatch.getSkill(SkillSlots.GUARD);
        if (guardContainer == null
                || !(guardContainer.getSkill() instanceof GuardSkill guardSkill)
                || !guardContainer.isActivated()) {
            return;
        }

        FriendlyByteBuf arguments = new FriendlyByteBuf(Unpooled.buffer(0));
        try {
            guardSkill.cancelOnServer(guardContainer, arguments);
        } finally {
            arguments.release();
        }
    }

    private static ItemStack findShieldStack(LivingEntity target) {
        ItemStack useItem = target.getUseItem();
        if (useItem.getItem() instanceof ShieldItem) {
            return useItem;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack heldItem = target.getItemInHand(hand);
            if (heldItem.getItem() instanceof ShieldItem) {
                return heldItem;
            }
        }
        return ItemStack.EMPTY;
    }

    private GrapplingShieldDisarmHandler() {
    }
}
