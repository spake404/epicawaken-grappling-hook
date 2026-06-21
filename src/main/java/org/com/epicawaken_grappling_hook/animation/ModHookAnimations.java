package org.com.epicawaken_grappling_hook.animation;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.entity.ModEntities;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHook;
import org.com.epicawaken_grappling_hook.util.GrapplingHookArrivalTracker;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class ModHookAnimations {
    public static AnimationManager.AnimationAccessor<ActionAnimation> HOOK_PULL;
    public static AnimationManager.AnimationAccessor<ActionAnimation> HOOK_AIR;
    public static AnimationManager.AnimationAccessor<ActionAnimation> HOOK_GROUND;

    // Movement input can interrupt hook recovery after these animation elapsed times.
    public static final float HOOK_PULL_MOVEMENT_INTERRUPT_AT = 1.0F;
    public static final float HOOK_AIR_MOVEMENT_INTERRUPT_AT = 0.9F;
    public static final float HOOK_GROUND_MOVEMENT_INTERRUPT_AT = 0.9F;

    public static void registerAnimations(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder(Epicawaken_grappling_hook.MODID, ModHookAnimations::build);
    }

    private static void build(AnimationManager.AnimationBuilder builder) {
        HOOK_PULL = builder.nextAccessor("biped/weapon/darknight_pursuiters/hook_pull",
                accessor -> limitMovementLock(
                        new ActionAnimation(0.15F, accessor, Armatures.BIPED)
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

    private ModHookAnimations() {
    }
}
