package org.com.epicawaken_grappling_hook.animation;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.entity.ModEntities;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHook;
import org.com.epicawaken_grappling_hook.util.GrapplingHookArrivalTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookMissedTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookUse;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class ModHookAnimations {
    public static AnimationManager.AnimationAccessor<ActionAnimation> HOOK_PULL;
    public static AnimationManager.AnimationAccessor<ActionAnimation> HOOK_AIR;
    public static AnimationManager.AnimationAccessor<ActionAnimation> HOOK_GROUND;

    // Movement input can interrupt hook recovery after these animation elapsed times.
    public static final float HOOK_PULL_MOVEMENT_INTERRUPT_AT = 1.0F;
    public static final float HOOK_AIR_MOVEMENT_INTERRUPT_AT = 0.9F;
    public static final float HOOK_GROUND_MOVEMENT_INTERRUPT_AT = 0.9F;
    private static final float HOOK_PULL_FALLBACK_TOTAL_SECONDS = 2.1667F;
    private static final float HOOK_PULL_SLOW_SEGMENT_START_RATIO = 0.30F;
    private static final float HOOK_PULL_SLOW_SEGMENT_SPEED_MULTIPLIER = 0.45F;
    private static final float HOOK_PULL_RECOVERY_SEGMENT_START_AT = 0.9F;
    private static final float HOOK_PULL_RECOVERY_SEGMENT_SPEED_MULTIPLIER = 1.5F;
    private static long nextHookPullSpeedClientDebugLogTick;
    private static long nextHookPullSpeedServerDebugLogTick;

    public static void registerAnimations(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder(Epicawaken_grappling_hook.MODID, ModHookAnimations::build);
    }

    private static void build(AnimationManager.AnimationBuilder builder) {
        HOOK_PULL = builder.nextAccessor("biped/weapon/darknight_pursuiters/hook_pull",
                accessor -> limitMovementLock(
                        new HookPullActionAnimation(0.15F, accessor, Armatures.BIPED)
                                .newTimePair(0.05F, Float.MAX_VALUE)
                                .addStateRemoveOld(EntityState.TURNING_LOCKED, true)
                                .newTimePair(0.0F, 1.0F)
                                .addStateRemoveOld(EntityState.CAN_BASIC_ATTACK, false)
                                .addStateRemoveOld(EntityState.CAN_SKILL_EXECUTION, false)
                                .addEvents(AnimationEvent.InTimeEvent.create(0.05F, (entityPatch, animation, params) -> {
                                    if (entityPatch instanceof PlayerPatch<?> playerPatch) {
                                        playerPatch.playSound(SoundEvents.FISHING_BOBBER_THROW, 0.0F, 0.0F);
                                        Player player = playerPatch.getOriginal();
                                        playerPatch.setModelYRot(player.getYRot(), true);

                                        GrapplingHook hook = new GrapplingHook(ModEntities.GRAPPLING_HOOK.get(), player.level());
                                        hook.setOwner(player);
                                        hook.setPos(player.getX(), player.getEyeY(), player.getZ());
                                        hook.shootFromRotation(player, player.getXRot(), entityPatch.getYRot(), 0.0F, (float) Config.getProjectileSpeed(), (float) Config.projectileInaccuracy);
                                        player.level().addFreshEntity(hook);
                                    }
                                }, AnimationEvent.Side.SERVER)),
                        HOOK_PULL_MOVEMENT_INTERRUPT_AT));

        HOOK_AIR = builder.nextAccessor("biped/weapon/darknight_pursuiters/hook_air",
                accessor -> limitArrivalMovementLock(
                        new ActionAnimation(0.15F, accessor, Armatures.BIPED)
                                .addProperty(AnimationProperty.AttackAnimationProperty.STOP_MOVEMENT, false)
                                .newTimePair(0.0F, 0.25F)
                                .addStateRemoveOld(EntityState.CAN_BASIC_ATTACK, false)
                                .addStateRemoveOld(EntityState.CAN_SKILL_EXECUTION, false),
                        HOOK_AIR_MOVEMENT_INTERRUPT_AT));

        HOOK_GROUND = builder.nextAccessor("biped/weapon/darknight_pursuiters/hook_ground",
                accessor -> limitMovementLock(
                        new ActionAnimation(0.15F, accessor, Armatures.BIPED)
                                .addProperty(AnimationProperty.AttackAnimationProperty.STOP_MOVEMENT, false)
                                .newTimePair(0.0F, 0.25F)
                                .addStateRemoveOld(EntityState.CAN_BASIC_ATTACK, false)
                                .addStateRemoveOld(EntityState.CAN_SKILL_EXECUTION, false),
                        HOOK_GROUND_MOVEMENT_INTERRUPT_AT));
    }

    private static <T extends StaticAnimation> T limitMovementLock(T animation, float movementInterruptAt) {
        animation.newTimePair(0.0F, movementInterruptAt);
        animation.addStateRemoveOld(EntityState.MOVEMENT_LOCKED, true);
        animation.addStateRemoveOld(EntityState.UPDATE_LIVING_MOTION, false);
        return animation;
    }

    private static <T extends StaticAnimation> T limitArrivalMovementLock(T animation, float fallbackMovementInterruptAt) {
        animation.removeState(EntityState.MOVEMENT_LOCKED);
        animation.removeState(EntityState.UPDATE_LIVING_MOTION);
        animation.removeState(EntityState.INACTION);
        animation.newConditionalTimePair(
                entityPatch -> GrapplingHookArrivalTracker.hasArrived(entityPatch.getOriginal()) ? 1 : 0,
                0.0F,
                fallbackMovementInterruptAt);
        animation.addConditionalState(0, EntityState.MOVEMENT_LOCKED, true);
        animation.addConditionalState(0, EntityState.UPDATE_LIVING_MOTION, false);
        animation.addConditionalState(0, EntityState.INACTION, true);
        animation.addConditionalState(1, EntityState.MOVEMENT_LOCKED, false);
        animation.addConditionalState(1, EntityState.UPDATE_LIVING_MOTION, true);
        animation.addConditionalState(1, EntityState.INACTION, false);
        return animation;
    }

    private static float modifyHookPullSpeed(DynamicAnimation self, LivingEntityPatch<?> entityPatch, float speed) {
        boolean activeConfiguredUse = GrapplingHookUse.hasActiveConfiguredUse(entityPatch.getOriginal());
        boolean missedHook = GrapplingHookMissedTracker.hasMissed(entityPatch.getOriginal());
        float totalTime = self.getTotalTime();
        AnimationPlayer animationPlayer = entityPatch.getAnimator().getPlayerFor(self.getAccessor());
        float prevElapsedTime = animationPlayer.getPrevElapsedTime();
        float elapsedTime = animationPlayer.getElapsedTime();
        float phaseMultiplier = missedHook ? getHookPullPhaseSpeedMultiplier(totalTime, elapsedTime) : 1.0F;
        boolean slowSegment = phaseMultiplier == HOOK_PULL_SLOW_SEGMENT_SPEED_MULTIPLIER;

        float modifiedSpeed = speed * (float) Config.hookPullAnimationSpeed;
        modifiedSpeed *= phaseMultiplier;
        logHookPullSpeedDebug(entityPatch, self, speed, modifiedSpeed, phaseMultiplier, prevElapsedTime, elapsedTime, totalTime, activeConfiguredUse, missedHook, slowSegment);
        return modifiedSpeed;
    }

    private static float getHookPullPhaseSpeedMultiplier(float totalTime, float elapsedTime) {
        float animationTotalTime = totalTime > 0.0F ? totalTime : HOOK_PULL_FALLBACK_TOTAL_SECONDS;
        float slowSegmentStart = animationTotalTime * HOOK_PULL_SLOW_SEGMENT_START_RATIO;
        if (elapsedTime >= HOOK_PULL_RECOVERY_SEGMENT_START_AT) {
            return HOOK_PULL_RECOVERY_SEGMENT_SPEED_MULTIPLIER;
        }
        if (elapsedTime >= slowSegmentStart) {
            return HOOK_PULL_SLOW_SEGMENT_SPEED_MULTIPLIER;
        }
        return 1.0F;
    }

    private static void logHookPullSpeedDebug(
            LivingEntityPatch<?> entityPatch,
            DynamicAnimation self,
            float baseSpeed,
            float finalSpeed,
            float phaseMultiplier,
            float prevElapsedTime,
            float elapsedTime,
            float totalTime,
            boolean activeConfiguredUse,
            boolean missedHook,
            boolean slowSegment) {
        if (!Config.debugLogging) {
            return;
        }

        long gameTime = entityPatch.getOriginal().level().getGameTime();
        boolean clientSide = entityPatch.getOriginal().level().isClientSide;
        long nextLogTick = clientSide ? nextHookPullSpeedClientDebugLogTick : nextHookPullSpeedServerDebugLogTick;
        if (gameTime < nextLogTick) {
            return;
        }

        if (clientSide) {
            nextHookPullSpeedClientDebugLogTick = gameTime + 5L;
        } else {
            nextHookPullSpeedServerDebugLogTick = gameTime + 5L;
        }
        Epicawaken_grappling_hook.LOGGER.info(
                "[GrapplingHookSpeedDebug][{}] hook_pull speed modifier owner={} gameTime={} animation={} activeUse={} missedHook={} prevElapsed={} elapsed={} total={} threshold={} recoveryAt={} slowSegment={} baseSpeed={} configSpeed={} phaseMultiplier={} finalSpeed={}",
                clientSide ? "CLIENT" : "SERVER",
                entityPatch.getOriginal().getId(),
                gameTime,
                self.getRegistryName(),
                activeConfiguredUse,
                missedHook,
                prevElapsedTime,
                elapsedTime,
                totalTime,
                (totalTime > 0.0F ? totalTime : HOOK_PULL_FALLBACK_TOTAL_SECONDS) * HOOK_PULL_SLOW_SEGMENT_START_RATIO,
                HOOK_PULL_RECOVERY_SEGMENT_START_AT,
                slowSegment,
                baseSpeed,
                Config.hookPullAnimationSpeed,
                phaseMultiplier,
                finalSpeed);
    }

    private static class HookPullActionAnimation extends ActionAnimation {
        private HookPullActionAnimation(float transitionTime, AnimationManager.AnimationAccessor<? extends ActionAnimation> accessor, AssetAccessor<? extends Armature> armature) {
            super(transitionTime, accessor, armature);
        }

        @Override
        public float getPlaySpeed(LivingEntityPatch<?> entityPatch, DynamicAnimation animation) {
            return modifyHookPullSpeed(this, entityPatch, super.getPlaySpeed(entityPatch, animation));
        }
    }

    private ModHookAnimations() {
    }
}
