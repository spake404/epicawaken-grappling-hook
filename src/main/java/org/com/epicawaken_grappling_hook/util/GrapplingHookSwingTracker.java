package org.com.epicawaken_grappling_hook.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.network.GrapplingHookFovType;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.StartGrapplingHookFovPacket;
import org.com.epicawaken_grappling_hook.network.StopGrapplingHookFovPacket;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHook;

@Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID)
public final class GrapplingHookSwingTracker {
    private static final Set<UUID> HOLDING = new HashSet<>();
    private static final Map<UUID, SwingState> SWINGS = new HashMap<>();
    private static final Map<UUID, Long> HOLD_STARTED_AT = new HashMap<>();
    private static final Map<UUID, Long> HOLD_RELEASED_AT = new HashMap<>();
    private static final int HOLD_RELEASE_GRACE_TICKS = 4;
    private static final double MIN_ROPE_LENGTH = 1.5D;
    private static final double MAX_ROPE_LENGTH = 32.0D;
    private static final double TANGENTIAL_ACCELERATION = 0.055D;
    private static final double PRE_SWING_PULL_ACCELERATION = 0.018D;
    private static final double PRE_SWING_MAX_PULL_SPEED = 0.35D;
    private static final double PRE_SWING_MAX_FALL_SPEED = -0.12D;
    private static final double GRAVITY = 0.0D;
    private static final double SWING_TARGET_SPEED = 1.15D;
    private static final double SWING_ACCELERATION = 0.18D;
    private static final double SWING_MIN_HORIZONTAL_SPEED = 0.65D;
    private static final double SWING_BOTTOM_HORIZONTAL_DISTANCE = 1.35D;
    private static final double SWING_BOTTOM_UPWARD_BOOST = 0.16D;
    private static final double SWING_MIN_Y_SPEED = -0.02D;
    private static final double MAX_SPEED = 1.75D;

    private GrapplingHookSwingTracker() {
    }

    public static void setHolding(ServerPlayer player, boolean holding) {
        UUID uuid = player.getUUID();
        if (holding) {
            HOLDING.add(uuid);
            HOLD_RELEASED_AT.remove(uuid);
            HOLD_STARTED_AT.putIfAbsent(uuid, player.serverLevel().getGameTime());
        } else {
            HOLDING.remove(uuid);
            HOLD_RELEASED_AT.put(uuid, player.serverLevel().getGameTime());
        }
    }

    public static boolean isHolding(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }

        UUID uuid = player.getUUID();
        if (HOLDING.contains(uuid)) {
            return true;
        }

        Long releasedAt = HOLD_RELEASED_AT.get(uuid);
        if (releasedAt == null) {
            return false;
        }

        boolean inGrace = player.serverLevel().getGameTime() - releasedAt <= HOLD_RELEASE_GRACE_TICKS;
        if (!inGrace) {
            HOLD_RELEASED_AT.remove(uuid);
            HOLD_STARTED_AT.remove(uuid);
        }
        return inGrace;
    }

    public static boolean hasHeldFor(Entity entity, int ticks) {
        if (!(entity instanceof ServerPlayer player) || !isHolding(player)) {
            return false;
        }

        Long startedAt = HOLD_STARTED_AT.get(player.getUUID());
        return startedAt != null && player.serverLevel().getGameTime() - startedAt >= ticks;
    }

    public static boolean start(ServerPlayer player, GrapplingHook hook, Vec3 anchor) {
        if (!isHolding(player)) {
            return false;
        }

        Vec3 radius = player.position().subtract(anchor);
        double ropeLength = radius.length();
        if (ropeLength < MIN_ROPE_LENGTH) {
            return false;
        }

        SwingState state = new SwingState(hook.getUUID(), anchor, Math.min(ropeLength, MAX_ROPE_LENGTH));
        SWINGS.put(player.getUUID(), state);
        GrapplingHookParcoolBlocker.block(player, 2);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StartGrapplingHookFovPacket(GrapplingHookFovType.AIR));
        applySwingVelocity(player, state);

        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSwingDebug][SERVER] swing started owner={} hook={} anchor={} ropeLength={} ownerPos={} ownerVelocity={}",
                    player.getId(),
                    hook.getId(),
                    anchor,
                    state.ropeLength,
                    player.position(),
                    player.getDeltaMovement());
        }
        return true;
    }

    public static void applyPreSwingDrag(ServerPlayer player, Vec3 anchor) {
        if (!isHolding(player)) {
            return;
        }

        Vec3 toAnchor = anchor.subtract(player.position());
        double distance = toAnchor.length();
        if (distance <= MIN_ROPE_LENGTH) {
            return;
        }

        Vec3 direction = toAnchor.scale(1.0D / distance);
        Vec3 current = player.getDeltaMovement();
        double pullSpeed = current.dot(direction);
        double targetPullSpeed = Math.min(PRE_SWING_MAX_PULL_SPEED, distance * 0.08D);
        Vec3 velocity = current;
        if (pullSpeed < targetPullSpeed) {
            velocity = velocity.add(direction.scale(Math.min(PRE_SWING_PULL_ACCELERATION, targetPullSpeed - pullSpeed)));
        }
        if (velocity.y < PRE_SWING_MAX_FALL_SPEED) {
            velocity = new Vec3(velocity.x, PRE_SWING_MAX_FALL_SPEED, velocity.z);
        }

        player.setDeltaMovement(velocity);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        GrapplingHookParcoolBlocker.block(player, 2);
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSwingDebug][SERVER] pre swing drag owner={} anchor={} distance={} oldVelocity={} newVelocity={} ownerPos={}",
                    player.getId(),
                    anchor,
                    distance,
                    current,
                    player.getDeltaMovement(),
                    player.position());
        }
    }

    public static boolean isSwinging(Entity entity, GrapplingHook hook) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }

        SwingState state = SWINGS.get(player.getUUID());
        return state != null && state.hookUuid.equals(hook.getUUID());
    }

    public static void stop(Entity entity, GrapplingHook hook) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        SwingState state = SWINGS.get(player.getUUID());
        if (state != null && state.hookUuid.equals(hook.getUUID())) {
            stop(player, state, "hook_removed");
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || SWINGS.isEmpty() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        SwingState state = SWINGS.get(player.getUUID());
        if (state != null) {
            tickSwing(player, state);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        HOLDING.remove(uuid);
        HOLD_STARTED_AT.remove(uuid);
        HOLD_RELEASED_AT.remove(uuid);
        SWINGS.remove(uuid);
    }

    private static void tickSwing(ServerPlayer player, SwingState state) {
        if (!isHolding(player) || player.isShiftKeyDown()) {
            stop(player, state, !isHolding(player) ? "released" : "sneaking");
            return;
        }

        state.elapsedTicks++;
        applySwingVelocity(player, state);
        GrapplingHookParcoolBlocker.block(player, 2);
    }

    private static void applySwingVelocity(ServerPlayer player, SwingState state) {
        Vec3 radius = player.position().subtract(state.anchor);
        double distance = radius.length();
        if (distance <= 1.0E-6D) {
            return;
        }

        Vec3 radialDir = radius.scale(1.0D / distance);
        Vec3 current = player.getDeltaMovement();
        double radialSpeed = current.dot(radialDir);
        Vec3 velocity = current.subtract(radialDir.scale(Math.max(0.0D, radialSpeed)));
        Vec3 yawForward = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 tangentInput = yawForward.subtract(radialDir.scale(yawForward.dot(radialDir)));
        if (tangentInput.lengthSqr() <= 1.0E-6D) {
            tangentInput = new Vec3(-radialDir.z, 0.0D, radialDir.x);
        }

        double horizontalDistance = Math.sqrt(radius.x * radius.x + radius.z * radius.z);
        boolean belowAnchor = radius.y < 0.0D;
        boolean nearBottom = belowAnchor && horizontalDistance <= SWING_BOTTOM_HORIZONTAL_DISTANCE;
        if (tangentInput.lengthSqr() > 1.0E-6D) {
            Vec3 tangentDir = tangentInput.normalize();
            if (nearBottom && tangentDir.y < 0.0D) {
                tangentDir = tangentDir.scale(-1.0D);
            }

            Vec3 targetVelocity = tangentDir.scale(SWING_TARGET_SPEED);
            Vec3 velocityDelta = targetVelocity.subtract(velocity);
            double acceleration = Math.min(SWING_ACCELERATION, velocityDelta.length());
            if (acceleration > 1.0E-6D) {
                velocity = velocity.add(velocityDelta.normalize().scale(acceleration));
            }

            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontalSpeed < SWING_MIN_HORIZONTAL_SPEED) {
                Vec3 horizontalTangent = new Vec3(tangentDir.x, 0.0D, tangentDir.z);
                if (horizontalTangent.lengthSqr() > 1.0E-6D) {
                    Vec3 horizontalDir = horizontalTangent.normalize();
                    velocity = new Vec3(
                            horizontalDir.x * SWING_MIN_HORIZONTAL_SPEED,
                            velocity.y,
                            horizontalDir.z * SWING_MIN_HORIZONTAL_SPEED);
                }
            }

            if (nearBottom && velocity.y < SWING_BOTTOM_UPWARD_BOOST) {
                velocity = new Vec3(velocity.x, SWING_BOTTOM_UPWARD_BOOST, velocity.z);
            } else if (velocity.y < SWING_MIN_Y_SPEED) {
                velocity = new Vec3(velocity.x, SWING_MIN_Y_SPEED, velocity.z);
            }
        }

        velocity = velocity.add(0.0D, GRAVITY, 0.0D);
        velocity = velocity.subtract(radialDir.scale(velocity.dot(radialDir)));
        if (velocity.length() > MAX_SPEED) {
            velocity = velocity.normalize().scale(MAX_SPEED);
        }

        if (distance > state.ropeLength) {
            Vec3 constrainedPos = state.anchor.add(radialDir.scale(state.ropeLength));
            player.teleportTo(constrainedPos.x, constrainedPos.y, constrainedPos.z);
        }

        player.setDeltaMovement(velocity);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSwingDebug][SERVER] swing tick owner={} elapsedTicks={} anchor={} ropeLength={} distance={} horizontalDistance={} belowAnchor={} nearBottom={} oldVelocity={} newVelocity={} ownerPos={}",
                    player.getId(),
                    state.elapsedTicks,
                    state.anchor,
                    state.ropeLength,
                    distance,
                    horizontalDistance,
                    belowAnchor,
                    nearBottom,
                    current,
                    player.getDeltaMovement(),
                    player.position());
        }
    }

    private static void stop(ServerPlayer player, SwingState state, String reason) {
        SWINGS.remove(player.getUUID());
        GrapplingHookParcoolBlocker.clear(player);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StopGrapplingHookFovPacket(GrapplingHookFovType.AIR));
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSwingDebug][SERVER] swing stopped owner={} reason={} elapsedTicks={} ownerPos={} ownerVelocity={}",
                    player.getId(),
                    reason,
                    state.elapsedTicks,
                    player.position(),
                    player.getDeltaMovement());
        }
    }

    private static final class SwingState {
        private final UUID hookUuid;
        private final Vec3 anchor;
        private final double ropeLength;
        private int elapsedTicks;

        private SwingState(UUID hookUuid, Vec3 anchor, double ropeLength) {
            this.hookUuid = hookUuid;
            this.anchor = anchor;
            this.ropeLength = ropeLength;
        }
    }
}
