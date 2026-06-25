package org.com.epicawaken_grappling_hook.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID)
public final class AirHookArrivalJumpTracker {
    private static final Map<UUID, Long> JUMP_WINDOWS = new HashMap<>();
    private static final Map<UUID, BoostState> BOOSTS = new HashMap<>();

    private AirHookArrivalJumpTracker() {
    }

    public static void openWindow(Entity entity) {
        if (!(entity instanceof ServerPlayer player) || !Config.airHookArrivalJumpEnabled) {
            return;
        }

        long expiresAt = player.serverLevel().getGameTime() + Config.airHookArrivalJumpWindowTicks;
        JUMP_WINDOWS.put(player.getUUID(), expiresAt);
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookArrivalJumpDebug][SERVER] jump window opened owner={} expiresAt={} duration={} ownerPos={} ownerVelocity={}",
                    player.getId(),
                    expiresAt,
                    Config.airHookArrivalJumpWindowTicks,
                    player.position(),
                    player.getDeltaMovement());
        }
    }

    public static boolean tryStartFromJump(ServerPlayer player) {
        if (!Config.airHookArrivalJumpEnabled || !hasOpenWindow(player)) {
            return false;
        }

        JUMP_WINDOWS.remove(player.getUUID());
        return start(player);
    }

    private static boolean hasOpenWindow(ServerPlayer player) {
        Long expiresAt = JUMP_WINDOWS.get(player.getUUID());
        if (expiresAt == null) {
            return false;
        }

        if (player.serverLevel().getGameTime() > expiresAt) {
            JUMP_WINDOWS.remove(player.getUUID(), expiresAt);
            return false;
        }

        return true;
    }

    private static boolean start(ServerPlayer player) {
        double speed = Config.airHookArrivalJumpInitialSpeed;
        if (speed <= 1.0E-6D || Config.airHookArrivalJumpDurationTicks <= 0) {
            return false;
        }

        BoostState state = new BoostState(speed, Config.airHookArrivalJumpDurationTicks);
        BOOSTS.put(player.getUUID(), state);
        GrapplingHookParcoolBlocker.block(player, Config.airHookArrivalJumpDurationTicks + 2);
        applyBoostVelocity(player, state);
        playEpicFightJumpAnimation(player);

        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookArrivalJumpDebug][SERVER] jump boost started owner={} boostSpeed={} duration={} ownerPos={} ownerVelocity={}",
                    player.getId(),
                    speed,
                    Config.airHookArrivalJumpDurationTicks,
                    player.position(),
                    player.getDeltaMovement());
        }
        return true;
    }

    private static void playEpicFightJumpAnimation(ServerPlayer player) {
        LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
        if (entityPatch != null && Animations.BIPED_JUMP != null) {
            entityPatch.playAnimationSynchronized(Animations.BIPED_JUMP, 0.0F);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || BOOSTS.isEmpty() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        BoostState state = BOOSTS.get(player.getUUID());
        if (state != null) {
            tickBoost(player, state);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        JUMP_WINDOWS.remove(uuid);
        BOOSTS.remove(uuid);
    }

    private static void tickBoost(ServerPlayer player, BoostState state) {
        if (state.remainingTicks-- <= 0
                || state.speed < Config.airHookArrivalJumpMinSpeed
                || player.onGround()
                || player.isShiftKeyDown()) {
            BOOSTS.remove(player.getUUID());
            GrapplingHookParcoolBlocker.clear(player);
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookArrivalJumpDebug][SERVER] jump boost stopped owner={} remainingTicks={} speed={} minSpeed={} onGround={} shift={} ownerPos={} ownerVelocity={}",
                        player.getId(),
                        state.remainingTicks,
                        state.speed,
                        Config.airHookArrivalJumpMinSpeed,
                        player.onGround(),
                        player.isShiftKeyDown(),
                        player.position(),
                        player.getDeltaMovement());
            }
            return;
        }

        applyBoostVelocity(player, state);
        GrapplingHookParcoolBlocker.block(player, 2);
        state.speed *= Config.airHookArrivalJumpFriction;
    }

    private static void applyBoostVelocity(ServerPlayer player, BoostState state) {
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(current.x, state.speed, current.z);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookArrivalJumpDebug][SERVER] jump boost tick owner={} remainingTicks={} speed={} oldVelocity={} newVelocity={} ownerPos={}",
                    player.getId(),
                    state.remainingTicks,
                    state.speed,
                    current,
                    player.getDeltaMovement(),
                    player.position());
        }
    }

    private static final class BoostState {
        private double speed;
        private int remainingTicks;

        private BoostState(double speed, int remainingTicks) {
            this.speed = speed;
            this.remainingTicks = remainingTicks;
        }
    }
}
