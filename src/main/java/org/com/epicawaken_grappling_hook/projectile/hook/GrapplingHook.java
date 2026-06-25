package org.com.epicawaken_grappling_hook.projectile.hook;

import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.animation.ModHookAnimations;
import org.com.epicawaken_grappling_hook.network.GrapplingHookFovType;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.StartGrapplingHookFovPacket;
import org.com.epicawaken_grappling_hook.network.StopGrapplingHookFovPacket;
import org.com.epicawaken_grappling_hook.network.SyncGrapplingHookArrivalPacket;
import org.com.epicawaken_grappling_hook.network.SyncGrapplingHookMissedPacket;
import org.com.epicawaken_grappling_hook.util.AirHookArrivalJumpTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookArrivalTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookMissedTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookParcoolBlocker;
import org.com.epicawaken_grappling_hook.util.GroundHookSlideTracker;
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.network.PacketDistributor;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class GrapplingHook extends AbstractArrow {
    public static final float PULL_TARGET = 0.22F;
    private static final double AIR_HOOK_MIN_ANGLE_DEGREES = 3.0D;
    private static final double AIR_HOOK_MIN_HEIGHT_ABOVE_EYES = 1.0D;
    private static final double TERRAIN_PULL_CONTACT_DISTANCE = 0.08D;
    private static final double TERRAIN_PULL_HORIZONTAL_ARRIVAL_DISTANCE = 0.12D;
    private static final double TERRAIN_PULL_VERTICAL_ARRIVAL_DISTANCE = 0.45D;
    private static final double TERRAIN_PULL_OVERSHOOT_DISTANCE = 0.75D;
    private static final double WALL_TARGET_SURFACE_GAP = 0.0D;
    private static final double[] COLLISION_FREE_TARGET_VERTICAL_OFFSETS = {0.0D, 0.25D, 0.5D, 1.0D, -0.25D};
    private static final int MISSED_HOOK_MIN_VISUAL_RETRACT_TICKS = 18;

    private int life;
    private boolean hooked;
    private HookType hookType = HookType.AIR;
    private Entity hookedEntity;
    private Vec3 terrainTarget;
    private Vec3 groundSlideDirection;
    private Vec3 lastTerrainPullVelocity;
    private boolean terrainPullArrived;
    private boolean fovEffectActive;
    private int missedHookAnimationStartLife = -1;
    private double previousTerrainTargetDistance = Double.MAX_VALUE;

    public GrapplingHook(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected boolean tryPickup(@NotNull Player player) {
        return false;
    }

    @Override
    @NotNull
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);

        int lockDelayTicks = Config.getHookLockDelayTicks();
        int missedHookVisualTicks = Math.max(Config.missedHookGroundAnimationDurationTicks, MISSED_HOOK_MIN_VISUAL_RETRACT_TICKS);
        int maxLifeTicks = Math.max(Config.maxLifeTicks, lockDelayTicks + missedHookVisualTicks + 2);
        Entity owner = this.getOwner();
        if (++this.life > maxLifeTicks || owner == null) {
            this.discardHook();
            return;
        }
        GrapplingHookParcoolBlocker.block(owner, 2);

        if (this.hookedEntity != null) {
            this.setPos(this.hookedEntity.getX(), this.hookedEntity.getY(0.8D), this.hookedEntity.getZ());
        }

        if (this.life < lockDelayTicks) {
            return;
        }

        if (this.life == lockDelayTicks) {
            if (!this.hooked) {
                this.startMissedHookGroundAnimation(owner, lockDelayTicks);
                return;
            }
            this.lockHookType();
        }

        if (!this.hooked) {
            return;
        }

        switch (this.hookType) {
            case ENTITY -> this.tickEntityHook(lockDelayTicks);
            case AIR -> this.tickAirHook(lockDelayTicks);
            case GROUND -> this.tickGroundHook(lockDelayTicks);
            case MISSED -> this.tickMissedHook(lockDelayTicks);
        }
    }

    private void startMissedHookGroundAnimation(Entity owner, int lockDelayTicks) {
        this.hooked = true;
        this.hookType = HookType.MISSED;
        this.missedHookAnimationStartLife = this.life;
        this.setDeltaMovement(Vec3.ZERO);
        GrapplingHookMissedTracker.markMissed(owner);
        if (!this.level().isClientSide) {
            ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> owner), new SyncGrapplingHookMissedPacket(owner.getId()));
        }
        this.blockMissedHookForwardMovement(owner);
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][SERVER] started missed hook cleanup owner={} hookLife={} lockDelayTicks={} cleanupTicks={} hookPos={} ownerPos={} hookVelocity={} ownerVelocity={}",
                    owner.getId(),
                    this.life,
                    lockDelayTicks,
                    Config.missedHookGroundAnimationDurationTicks,
                    this.position(),
                    owner.position(),
                    this.getDeltaMovement(),
                    owner.getDeltaMovement());
        }
    }

    private void tickMissedHook(int lockDelayTicks) {
        Entity owner = this.getOwner();
        this.blockMissedHookForwardMovement(owner);
        int elapsedTicks = this.missedHookAnimationStartLife >= 0 ? this.life - this.missedHookAnimationStartLife : this.life - lockDelayTicks;
        int cleanupTicks = Math.max(Config.missedHookGroundAnimationDurationTicks, MISSED_HOOK_MIN_VISUAL_RETRACT_TICKS);
        if (elapsedTicks >= cleanupTicks) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][SERVER] cleaned up missed hook projectile owner={} hookLife={} elapsedTicks={} cleanupTicks={} hookPos={} ownerPos={}",
                        owner != null ? owner.getId() : -1,
                        this.life,
                        elapsedTicks,
                        cleanupTicks,
                        this.position(),
                        owner != null ? owner.position() : null);
            }
            this.discardHook();
        }
    }

    private void blockMissedHookForwardMovement(Entity owner) {
        if (owner == null) {
            return;
        }

        Vec3 velocity = owner.getDeltaMovement();
        Vec3 forward = horizontalLookDirection(owner);
        double forwardSpeed = velocity.x * forward.x + velocity.z * forward.z;
        if (forwardSpeed <= 0.0D) {
            return;
        }

        owner.setDeltaMovement(
                velocity.x - forward.x * forwardSpeed,
                velocity.y,
                velocity.z - forward.z * forwardSpeed);
        owner.hurtMarked = true;
    }

    private void lockHookType() {
        this.hooked = true;
        this.setDeltaMovement(Vec3.ZERO);

        Entity owner = this.getOwner();
        if (owner == null) {
            return;
        }

        if (this.hookType == HookType.ENTITY && this.hookedEntity instanceof LivingEntity livingEntity && hasTooMuchKnockbackResistance(livingEntity)) {
            this.hookType = HookType.AIR;
        }

        if (this.hookType != HookType.ENTITY) {
            HookType previousHookType = this.hookType;
            Vec3 hookVec = this.position().subtract(owner.getEyePosition());
            double deltaY = this.getY() - (owner.getY() + owner.getEyeHeight());
            double angleDeg = Math.toDegrees(Math.asin(hookVec.normalize().y));
            this.hookType = angleDeg >= AIR_HOOK_MIN_ANGLE_DEGREES && deltaY > AIR_HOOK_MIN_HEIGHT_ABOVE_EYES ? HookType.AIR : HookType.GROUND;
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] hook type locked owner={} previousHookType={} finalHookType={} angleDeg={} minAirAngle={} deltaY={} minAirDeltaY={} hookPos={} ownerEye={} terrainTarget={} hookVec={}",
                        owner.getId(),
                        previousHookType,
                        this.hookType,
                        angleDeg,
                        AIR_HOOK_MIN_ANGLE_DEGREES,
                        deltaY,
                        AIR_HOOK_MIN_HEIGHT_ABOVE_EYES,
                        this.position(),
                        owner.getEyePosition(),
                        this.terrainTarget,
                        hookVec);
            }
        }
    }

    private boolean hasTooMuchKnockbackResistance(LivingEntity livingEntity) {
        if (!Config.respectKnockbackResistance) {
            return false;
        }

        AttributeInstance instance = livingEntity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        return instance != null && (1.0D - instance.getValue()) * PULL_TARGET <= 0.05D;
    }

    private void tickEntityHook(int lockDelayTicks) {
        if (this.hookedEntity != null && this.life == lockDelayTicks + 2) {
            float pullTargetStrength = this.getEntityPullStrength();
            if (Config.respectKnockbackResistance && pullTargetStrength <= 0.05F) {
                this.hookType = HookType.AIR;
            }
            this.pullHookedEntityToOwner(pullTargetStrength);
            return;
        }

        if (this.life == lockDelayTicks + 6) {
            this.discardHook();
        }
    }

    private float getEntityPullStrength() {
        float strength = PULL_TARGET;
        if (Config.respectKnockbackResistance && this.hookedEntity instanceof LivingEntity livingEntity) {
            AttributeInstance instance = livingEntity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (instance != null) {
                strength = Mth.clamp((float) ((1.0D - instance.getValue()) * PULL_TARGET * 2.0F), 0.0F, PULL_TARGET);
            }
        }

        return strength * (float) Config.entityPullStrengthMultiplier;
    }

    private void tickAirHook(int lockDelayTicks) {
        if (this.life == lockDelayTicks) {
            this.playAnimation(this.getOwner(), ModHookAnimations.HOOK_AIR);
            return;
        }

        if (this.terrainTarget != null && this.tickTerrainTargetPull(lockDelayTicks + 6)) {
            return;
        }

        if (this.life == lockDelayTicks + 6) {
            this.hookPull(this.getOwner(), (float) Config.airPullStrength);
            return;
        }

        if (this.life == lockDelayTicks + 10) {
            this.discardHook();
        }
    }

    private void tickGroundHook(int lockDelayTicks) {
        if (this.life == lockDelayTicks) {
            this.playAnimation(this.getOwner(), ModHookAnimations.HOOK_GROUND);
            return;
        }

        if (this.terrainTarget != null && this.tickGroundTerrainTargetPull(lockDelayTicks + 5)) {
            return;
        }

        if (this.life == lockDelayTicks + 5) {
            Entity owner = this.getOwner();
            this.hookPull(owner, (float) Config.groundHookPullStrength);
            if (owner != null) {
                this.groundSlideDirection = this.position().subtract(owner.position());
                if (Config.debugLogging) {
                    Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] ground fallback pull applied owner={} hookLife={} ownerPos={} hookPos={} terrainTarget={} slideDirection={} ownerVelocity={}",
                            owner.getId(),
                            this.life,
                            owner.position(),
                            this.position(),
                            this.terrainTarget,
                            this.groundSlideDirection,
                            owner.getDeltaMovement());
                }
            }
            return;
        }

        if (this.life == lockDelayTicks + 10) {
            this.discardHook();
        }
    }

    private boolean tickGroundTerrainTargetPull(int pullStartTick) {
        int pullEndTick = pullStartTick + Config.groundHookTargetPullDurationTicks;
        Entity owner = this.getOwner();

        if (this.life == pullStartTick && owner != null) {
            this.groundSlideDirection = this.terrainTarget.subtract(owner.position());
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] ground target pull started owner={} hookLife={} pullStartTick={} pullEndTick={} ownerPos={} hookPos={} terrainTarget={} slideDirection={} ownerVelocity={}",
                        owner.getId(),
                        this.life,
                        pullStartTick,
                        pullEndTick,
                        owner.position(),
                        this.position(),
                        this.terrainTarget,
                        this.groundSlideDirection,
                        owner.getDeltaMovement());
            }
        }

        if (this.life >= pullStartTick && this.life <= pullEndTick) {
            if (this.terrainPullArrived) {
                if (Config.debugLogging) {
                    Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] ground target pull already arrived owner={} hookLife={} ownerPos={} terrainTarget={} ownerVelocity={}",
                            owner != null ? owner.getId() : -1,
                            this.life,
                            owner != null ? owner.position() : null,
                            this.terrainTarget,
                            owner != null ? owner.getDeltaMovement() : null);
                }
                this.startGroundHookSlide(owner);
                this.discardHook();
                return true;
            }

            boolean finishedPull = this.pullOwnerToTerrainTarget(owner);
            if (this.terrainPullArrived) {
                if (Config.debugLogging) {
                    Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] ground target pull arrived after tick owner={} hookLife={} ownerPos={} terrainTarget={} ownerVelocity={}",
                            owner != null ? owner.getId() : -1,
                            this.life,
                            owner != null ? owner.position() : null,
                            this.terrainTarget,
                            owner != null ? owner.getDeltaMovement() : null);
                }
                this.startGroundHookSlide(owner);
                this.discardHook();
                return true;
            }

            if (finishedPull || this.life == pullEndTick) {
                if (Config.debugLogging) {
                    Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] ground target pull finished owner={} hookLife={} reason={} ownerPos={} terrainTarget={} ownerVelocity={}",
                            owner != null ? owner.getId() : -1,
                            this.life,
                            finishedPull ? "finished" : "timeout",
                            owner != null ? owner.position() : null,
                            this.terrainTarget,
                            owner != null ? owner.getDeltaMovement() : null);
                }
                this.finishTerrainPull(owner, finishedPull);
                this.startGroundHookSlide(owner);
                this.discardHook();
            }
            return true;
        }

        if (this.life > pullEndTick) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] ground target pull expired owner={} hookLife={} pullEndTick={} ownerPos={} terrainTarget={} ownerVelocity={}",
                        owner != null ? owner.getId() : -1,
                        this.life,
                        pullEndTick,
                        owner != null ? owner.position() : null,
                        this.terrainTarget,
                        owner != null ? owner.getDeltaMovement() : null);
            }
            this.discardHook();
            return true;
        }

        return false;
    }

    private void startGroundHookSlide(Entity owner) {
        if (owner == null || this.hookType != HookType.GROUND) {
            return;
        }

        Vec3 direction = this.lastTerrainPullVelocity != null ? this.lastTerrainPullVelocity
                : this.groundSlideDirection != null ? this.groundSlideDirection
                : this.position().subtract(owner.position());
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] starting slide from projectile owner={} hookLife={} ownerPos={} hookPos={} terrainTarget={} direction={} ownerVelocity={}",
                    owner.getId(),
                    this.life,
                    owner.position(),
                    this.position(),
                    this.terrainTarget,
                    direction,
                    owner.getDeltaMovement());
        }
        if (GroundHookSlideTracker.start(owner, direction)) {
            this.fovEffectActive = false;
        }
    }

    private boolean tickTerrainTargetPull(int pullStartTick) {
        int pullEndTick = pullStartTick + Config.airHookTargetPullDurationTicks;
        if (this.life >= pullStartTick && this.life <= pullEndTick) {
            if (this.terrainPullArrived) {
                this.discardHook();
                return true;
            }

            boolean finishedPull = this.pullOwnerToTerrainTarget(this.getOwner());
            if (this.terrainPullArrived) {
                this.discardHook();
                return true;
            }

            if (finishedPull || this.life == pullEndTick) {
                this.finishTerrainPull(this.getOwner(), finishedPull);
                this.discardHook();
            }
            return true;
        }

        if (this.life > pullEndTick) {
            this.discardHook();
            return true;
        }

        return false;
    }

    public void playAnimation(Entity entity, AnimationManager.AnimationAccessor<? extends StaticAnimation> animationAccessor) {
        LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        if (entityPatch != null) {
            entityPatch.playAnimationSynchronized(animationAccessor, 0.0F);
        }
    }

    private void hookPull(Entity owner, float speedScale) {
        if (owner == null) {
            return;
        }

        Vec3 acceleration = this.position().subtract(owner.position()).scale(speedScale);
        Vec3 currentVel = owner.getDeltaMovement().add(acceleration);
        if (this.hookType == HookType.AIR) {
            if (currentVel.y < 0.5D) {
                currentVel = new Vec3(currentVel.x, 0.5D, currentVel.z);
            } else if (currentVel.y > 1.25D) {
                currentVel = new Vec3(currentVel.x, 1.25D, currentVel.z);
            }
        } else if (this.hookType == HookType.GROUND && currentVel.y < 0.12D) {
            currentVel = new Vec3(currentVel.x, 0.12D, currentVel.z);
        }

        this.startPullFovEffect(owner);
        owner.addDeltaMovement(currentVel);
        owner.hurtMarked = true;
    }

    private void pullHookedEntityToOwner(float speedScale) {
        Entity owner = this.getOwner();
        if (owner != null && this.hookedEntity != null && this.hookedEntity.isAlive()) {
            Vec3 targetVec = owner.position().subtract(this.hookedEntity.position()).scale(speedScale);
            this.hookedEntity.addDeltaMovement(targetVec.add(0.0D, Config.entityPullUpBoost, 0.0D));
            this.hookedEntity.hurtMarked = true;
        }
    }

    private boolean pullOwnerToTerrainTarget(Entity owner) {
        if (owner == null || this.terrainTarget == null) {
            return true;
        }

        Vec3 delta = this.terrainTarget.subtract(owner.position());
        double distance = delta.length();
        if (this.hookType == HookType.GROUND && Config.groundHookSlideEnabled && distance <= Config.groundHookSlideStartDistance) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] ground target pull pre-start slide owner={} hookLife={} distance={} startDistance={} horizontalDistance={} verticalDistance={} ownerPos={} target={} ownerVelocity={}",
                        owner.getId(),
                        this.life,
                        distance,
                        Config.groundHookSlideStartDistance,
                        horizontalDistance(delta),
                        Math.abs(delta.y),
                        owner.position(),
                        this.terrainTarget,
                        owner.getDeltaMovement());
            }
            this.markTerrainPullArrived(owner);
            return false;
        }

        if ((this.hookType == HookType.AIR || this.hookType == HookType.GROUND) && this.hasReachedTerrainTarget(delta, distance)) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][SERVER] {} terrain target reached life={} distance={} horizontalDistance={} verticalDistance={} previousDistance={} target={} ownerPos={}",
                        this.hookType,
                        this.life,
                        distance,
                        horizontalDistance(delta),
                        Math.abs(delta.y),
                        this.previousTerrainTargetDistance,
                        this.terrainTarget,
                        owner.position());
            }
            this.markTerrainPullArrived(owner);
            return false;
        }

        double arrivalDistance = this.getTargetPullArrivalDistance();
        if (distance <= arrivalDistance) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] terrain target pull finished by config arrival hookType={} owner={} hookLife={} distance={} arrivalDistance={} ownerPos={} target={} ownerVelocity={}",
                        this.hookType,
                        owner.getId(),
                        this.life,
                        distance,
                        arrivalDistance,
                        owner.position(),
                        this.terrainTarget,
                        owner.getDeltaMovement());
            }
            return true;
        }

        double minSpeed = this.getTargetPullMinSpeed();
        double maxSpeed = this.getTargetPullMaxSpeed();
        double speed = Math.min(maxSpeed, Math.max(minSpeed, distance * 0.35D));
        Vec3 newVelocity = delta.normalize().scale(speed);
        this.lastTerrainPullVelocity = newVelocity;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] terrain target pull tick hookType={} owner={} hookLife={} distance={} horizontalDistance={} verticalDistance={} speed={} minSpeed={} maxSpeed={} ownerPos={} target={} oldVelocity={} newVelocity={}",
                    this.hookType,
                    owner.getId(),
                    this.life,
                    distance,
                    horizontalDistance(delta),
                    Math.abs(delta.y),
                    speed,
                    minSpeed,
                    maxSpeed,
                    owner.position(),
                    this.terrainTarget,
                    owner.getDeltaMovement(),
                    newVelocity);
        }
        this.startPullFovEffect(owner);
        owner.setDeltaMovement(newVelocity);
        owner.fallDistance = 0.0F;
        owner.hurtMarked = true;
        return false;
    }

    private void startPullFovEffect(Entity owner) {
        if (this.fovEffectActive || !(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }

        this.fovEffectActive = true;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookFovDebug][SERVER] start owner={} hookType={} hookLife={}",
                    serverPlayer.getId(),
                    this.hookType,
                    this.life);
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new StartGrapplingHookFovPacket(GrapplingHookFovType.fromHookType(this.hookType)));
    }

    private void stopPullFovEffect() {
        if (!this.fovEffectActive || !(this.getOwner() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        this.fovEffectActive = false;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookFovDebug][SERVER] stop owner={} hookType={} hookLife={}",
                    serverPlayer.getId(),
                    this.hookType,
                    this.life);
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new StopGrapplingHookFovPacket(GrapplingHookFovType.fromHookType(this.hookType)));
    }

    private void discardHook() {
        this.stopPullFovEffect();
        this.discard();
    }

    private boolean hasReachedTerrainTarget(Vec3 delta, double distance) {
        double contactDistance = Math.max(TERRAIN_PULL_CONTACT_DISTANCE, this.getTargetPullArrivalDistance());
        double horizontalDistance = horizontalDistance(delta);
        boolean positionReached = distance <= contactDistance
                || (horizontalDistance <= TERRAIN_PULL_HORIZONTAL_ARRIVAL_DISTANCE
                && Math.abs(delta.y) <= TERRAIN_PULL_VERTICAL_ARRIVAL_DISTANCE);
        boolean reached = distance <= contactDistance
                || positionReached
                || (this.previousTerrainTargetDistance <= TERRAIN_PULL_OVERSHOOT_DISTANCE && distance > this.previousTerrainTargetDistance);
        this.previousTerrainTargetDistance = distance;
        return reached;
    }

    private static double horizontalDistance(Vec3 delta) {
        return Math.sqrt(delta.x * delta.x + delta.z * delta.z);
    }

    private void markTerrainPullArrived(Entity owner) {
        this.terrainPullArrived = true;
        this.stopOwnerTerrainPull(owner, this.terrainTarget);
        this.openAirArrivalJumpWindow(owner, "mark_arrived");
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][SERVER] markTerrainPullArrived hookType={} hookLife={} owner={} ownerPos={} target={}",
                    this.hookType,
                    this.life,
                    owner.getId(),
                    owner.position(),
                    this.terrainTarget);
        }
        this.syncTerrainPullArrival(owner);
    }

    private void finishTerrainPull(Entity owner, boolean allowArrivalBoost) {
        if (owner == null) {
            return;
        }

        if (this.shouldSnapToTarget() && this.terrainTarget != null) {
            owner.teleportTo(this.terrainTarget.x, this.terrainTarget.y, this.terrainTarget.z);
        }

        this.stopOwnerTerrainPull(owner, this.terrainTarget);
        if (allowArrivalBoost) {
            this.openAirArrivalJumpWindow(owner, "finish_pull");
        }

        this.syncTerrainPullArrival(owner);
    }

    private void stopOwnerTerrainPull(Entity owner, Vec3 target) {
        owner.fallDistance = 0.0F;
        owner.hurtMarked = true;
    }

    private void openAirArrivalJumpWindow(Entity owner, String reason) {
        if (!Config.airHookArrivalJumpEnabled || this.hookType != HookType.AIR || owner == null) {
            return;
        }

        AirHookArrivalJumpTracker.openWindow(owner);
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookArrivalJumpDebug][SERVER] opened air arrival jump window owner={} hookLife={} reason={} windowTicks={} initialSpeed={} terrainTarget={} lastPullVelocity={} ownerVelocity={}",
                    owner.getId(),
                    this.life,
                    reason,
                    Config.airHookArrivalJumpWindowTicks,
                    Config.airHookArrivalJumpInitialSpeed,
                    this.terrainTarget,
                    this.lastTerrainPullVelocity,
                    owner.getDeltaMovement());
        }
    }

    private double getTargetPullMinSpeed() {
        return this.hookType == HookType.GROUND ? Config.groundHookTargetPullMinSpeed : Config.airHookTargetPullMinSpeed;
    }

    private double getTargetPullMaxSpeed() {
        return this.hookType == HookType.GROUND ? Config.groundHookTargetPullMaxSpeed : Config.airHookTargetPullMaxSpeed;
    }

    private double getTargetPullArrivalDistance() {
        return this.hookType == HookType.GROUND ? Config.groundHookTargetPullArrivalDistance : Config.airHookTargetPullArrivalDistance;
    }

    private boolean shouldSnapToTarget() {
        return this.hookType == HookType.GROUND ? Config.groundHookTargetPullSnapToTarget : Config.airHookTargetPullSnapToTarget;
    }

    private void syncTerrainPullArrival(Entity owner) {
        GrapplingHookArrivalTracker.markArrived(owner);
        if (owner instanceof ServerPlayer serverPlayer) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][SERVER] sending arrival packet owner={} hookType={} hookLife={} target={}",
                        serverPlayer.getId(),
                        this.hookType,
                        this.life,
                        this.terrainTarget);
            }
            ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer), new SyncGrapplingHookArrivalPacket(serverPlayer.getId()));
        } else if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][SERVER] owner arrived but is not ServerPlayer owner={}", owner.getId());
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        if (result.getEntity() != this.getOwner()) {
            this.hooked = true;
            this.hookType = HookType.ENTITY;
            this.hookedEntity = result.getEntity();
            this.moveTo(this.hookedEntity.position());
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        this.hooked = true;
        this.hookType = HookType.GROUND;
        this.terrainTarget = this.findSafeTerrainTarget(result);
        if (Config.debugLogging) {
            Entity owner = this.getOwner();
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] block hit owner={} direction={} horizontalFace={} hit={} block={} projectilePos={} ownerPos={} ownerYaw={} ownerLook={} terrainTarget={} retargetEnabled={} adaptiveEnabled={} allowedAngleWidthDegrees={} halfAngleDegrees={} requiredDot={}",
                    owner != null ? owner.getId() : -1,
                    result.getDirection(),
                    isHorizontalWallFace(result.getDirection()),
                    result.getLocation(),
                    result.getBlockPos(),
                    this.position(),
                    owner != null ? owner.position() : null,
                    owner != null ? owner.getYRot() : null,
                    owner != null ? horizontalLookDirection(owner) : null,
                    this.terrainTarget,
                    Config.wallHookFacingRetargetEnabled,
                    Config.wallHookFacingRetargetAdaptiveEnabled,
                    Config.wallHookFacingRetargetAngleWidthDegrees,
                    Config.wallHookFacingRetargetHalfAngleDegrees,
                    Config.wallHookFacingRetargetDot);
        }
        Vec3 vec3 = result.getLocation().subtract(this.getX(), this.getY(), this.getZ());
        this.setDeltaMovement(vec3);
        Vec3 vec31 = vec3.normalize().scale(0.05F);
        this.setPosRaw(this.getX() - vec31.x, this.getY() - vec31.y, this.getZ() - vec31.z);
        this.inGround = true;
    }

    private Vec3 findSafeTerrainTarget(BlockHitResult result) {
        Entity owner = this.getOwner();
        if (owner == null) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] using raw hit target reason=no_owner hit={}",
                        result.getLocation());
            }
            return result.getLocation();
        }

        Vec3 retarget = this.getFacingWallRetarget(result, owner);
        if (retarget != null) {
            return retarget;
        }

        Vec3 target = this.getRawTerrainTarget(result, owner);
        Vec3 safeTarget = this.findCollisionFreeTarget(owner, target);
        if (safeTarget != null) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] using normal terrain safe target owner={} rawTarget={} safeTarget={} direction={} hit={}",
                        owner.getId(),
                        target,
                        safeTarget,
                        result.getDirection(),
                        result.getLocation());
            }
            return safeTarget;
        }

        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] using normal terrain raw target owner={} rawTarget={} direction={} hit={} reason=no_collision_free_candidate",
                    owner.getId(),
                    target,
                    result.getDirection(),
                    result.getLocation());
        }
        return target;
    }

    private Vec3 findCollisionFreeTarget(Entity owner, Vec3 target) {
        Vec3 ownerPosition = owner.position();
        Vec3 targetOffset = target.subtract(ownerPosition);
        AABB ownerBox = owner.getBoundingBox();
        Level level = this.level();

        for (double verticalOffset : COLLISION_FREE_TARGET_VERTICAL_OFFSETS) {
            Vec3 candidate = target.add(0.0D, verticalOffset, 0.0D);
            if (level.noCollision(owner, ownerBox.move(targetOffset.add(0.0D, verticalOffset, 0.0D)))) {
                return candidate;
            }
        }

        return null;
    }

    private Vec3 getFacingWallRetarget(BlockHitResult result, Entity owner) {
        Direction hitDirection = result.getDirection();
        if (!Config.wallHookFacingRetargetEnabled) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] skipped wall retarget owner={} reason=disabled direction={} hit={} ownerYaw={}",
                        owner.getId(),
                        hitDirection,
                        result.getLocation(),
                        owner.getYRot());
            }
            return null;
        }
        if (!isHorizontalWallFace(hitDirection)) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] skipped wall retarget owner={} reason=not_horizontal_wall_face direction={} hit={} ownerYaw={}",
                        owner.getId(),
                        hitDirection,
                        result.getLocation(),
                        owner.getYRot());
            }
            return null;
        }

        Vec3 normal = Vec3.atLowerCornerOf(hitDirection.getNormal());
        Vec3 look = horizontalLookDirection(owner);
        if (look.lengthSqr() <= 1.0E-6D) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] skipped wall retarget owner={} reason=no_horizontal_look direction={} hit={} ownerYaw={} look={}",
                        owner.getId(),
                        hitDirection,
                        result.getLocation(),
                        owner.getYRot(),
                        look);
            }
            return null;
        }

        double facingDot = -look.dot(normal);
        if (facingDot < Config.wallHookFacingRetargetDot) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] skipped wall retarget owner={} direction={} facingDot={} requiredDot={} allowedAngleWidthDegrees={} halfAngleDegrees={} ownerYaw={} hit={} ownerPos={}",
                        owner.getId(),
                        hitDirection,
                        facingDot,
                        Config.wallHookFacingRetargetDot,
                        Config.wallHookFacingRetargetAngleWidthDegrees,
                        Config.wallHookFacingRetargetHalfAngleDegrees,
                        owner.getYRot(),
                        result.getLocation(),
                        owner.position());
            }
            return null;
        }

        Vec3 base = result.getLocation().add(normal.scale(Config.wallHookFacingRetargetForwardOffset));
        String retargetSource = "adaptive";
        Vec3 retarget = Config.wallHookFacingRetargetAdaptiveEnabled
                ? this.findAdaptiveFacingWallRetarget(owner, base, result, facingDot)
                : null;
        if (retarget == null) {
            retargetSource = Config.wallHookFacingRetargetAdaptiveEnabled ? "fallback_after_adaptive" : "fallback_adaptive_disabled";
            Vec3 fallback = base.add(0.0D, Config.wallHookFacingRetargetUpOffset, 0.0D);
            retarget = this.findCollisionFreeTarget(owner, fallback);
            if (retarget == null) {
                if (Config.debugLogging) {
                    Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] wall retarget fallback failed owner={} direction={} facingDot={} fallback={} base={} source={} reason=no_collision_free_candidate ownerPos={}",
                            owner.getId(),
                            hitDirection,
                            facingDot,
                            fallback,
                            base,
                            retargetSource,
                            owner.position());
                }
                return null;
            }
        }

        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] applied wall retarget owner={} direction={} facingDot={} requiredDot={} allowedAngleWidthDegrees={} halfAngleDegrees={} hit={} base={} retarget={} source={} upOffset={} forwardOffset={} ownerPos={} ownerVelocity={}",
                    owner.getId(),
                    hitDirection,
                    facingDot,
                    Config.wallHookFacingRetargetDot,
                    Config.wallHookFacingRetargetAngleWidthDegrees,
                    Config.wallHookFacingRetargetHalfAngleDegrees,
                    result.getLocation(),
                    base,
                    retarget,
                    retargetSource,
                    Config.wallHookFacingRetargetUpOffset,
                    Config.wallHookFacingRetargetForwardOffset,
                    owner.position(),
                    owner.getDeltaMovement());
        }
        return retarget;
    }

    private Vec3 findAdaptiveFacingWallRetarget(Entity owner, Vec3 base, BlockHitResult result, double facingDot) {
        double minOffset = Math.min(Config.wallHookFacingRetargetMinUpOffset, Config.wallHookFacingRetargetUpOffset);
        double maxOffset = Math.max(Config.wallHookFacingRetargetMinUpOffset, Config.wallHookFacingRetargetUpOffset);
        double step = Math.max(0.05D, Config.wallHookFacingRetargetSearchStep);

        for (double upOffset = minOffset; upOffset <= maxOffset + 1.0E-6D; upOffset += step) {
            Vec3 target = base.add(0.0D, upOffset, 0.0D);
            if (!this.hasWallTopClearance(result, target)) {
                if (Config.debugLogging) {
                    Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] adaptive candidate rejected owner={} reason=no_wall_top_clearance direction={} facingDot={} hit={} target={} upOffset={}",
                            owner.getId(),
                            result.getDirection(),
                            facingDot,
                            result.getLocation(),
                            target,
                            upOffset);
                }
                continue;
            }

            Vec3 candidate = this.findCollisionFreeTarget(owner, target);
            if (candidate != null) {
                if (Config.debugLogging) {
                    Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] adaptive wall retarget found owner={} direction={} facingDot={} hit={} candidate={} upOffset={} minOffset={} maxOffset={} step={} ownerPos={}",
                            owner.getId(),
                            result.getDirection(),
                            facingDot,
                            result.getLocation(),
                            candidate,
                            upOffset,
                            minOffset,
                            maxOffset,
                            step,
                            owner.position());
                }
                return candidate;
            }
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] adaptive candidate rejected owner={} reason=no_collision_free_candidate direction={} facingDot={} hit={} target={} upOffset={} ownerPos={}",
                        owner.getId(),
                        result.getDirection(),
                        facingDot,
                        result.getLocation(),
                        target,
                        upOffset,
                        owner.position());
            }
        }

        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] adaptive wall retarget not found owner={} direction={} facingDot={} hit={} minOffset={} maxOffset={} step={} ownerPos={}",
                    owner.getId(),
                    result.getDirection(),
                    facingDot,
                    result.getLocation(),
                    minOffset,
                    maxOffset,
                    step,
                    owner.position());
        }
        return null;
    }

    private boolean hasWallTopClearance(BlockHitResult result, Vec3 target) {
        BlockPos hitBlock = result.getBlockPos();
        int targetBlockY = Mth.floor(target.y);
        int yOffset = targetBlockY - hitBlock.getY();
        if (yOffset < 0) {
            return false;
        }

        BlockPos wallColumnAtFeet = hitBlock.above(yOffset);
        BlockPos supportBelow = wallColumnAtFeet.below();
        Level level = this.level();
        boolean feetSpaceClear = level.getBlockState(wallColumnAtFeet).getCollisionShape(level, wallColumnAtFeet).isEmpty();
        boolean supportExists = !level.getBlockState(supportBelow).getCollisionShape(level, supportBelow).isEmpty();
        return feetSpaceClear && supportExists;
    }

    private Vec3 getRawTerrainTarget(BlockHitResult result, Entity owner) {
        Vec3 hit = result.getLocation();
        Direction direction = result.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());

        if (direction == Direction.UP) {
            return hit.add(0.0D, 0.05D, 0.0D);
        }

        if (direction == Direction.DOWN) {
            return hit.add(0.0D, -owner.getBbHeight() - 0.05D, 0.0D);
        }

        double wallOffset = owner.getBbWidth() * 0.5D + WALL_TARGET_SURFACE_GAP;
        return hit.add(normal.scale(wallOffset)).add(0.0D, -owner.getEyeHeight(), 0.0D);
    }

    private static boolean isHorizontalWallFace(Direction direction) {
        return direction.getAxis().isHorizontal();
    }

    private static Vec3 horizontalLookDirection(Entity entity) {
        return Vec3.directionFromRotation(0.0F, entity.getYRot());
    }

    public HookType getHookType() {
        return this.hookType;
    }

    public int getLifeForDebug() {
        return this.life;
    }

    public boolean isHookedForDebug() {
        return this.hooked;
    }

    public boolean isInGroundForDebug() {
        return this.inGround;
    }

    public Vec3 getTerrainTargetForDebug() {
        return this.terrainTarget;
    }

    public enum HookType {
        AIR,
        GROUND,
        ENTITY,
        MISSED
    }

}
