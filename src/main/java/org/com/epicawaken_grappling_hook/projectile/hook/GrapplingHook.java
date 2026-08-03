package org.com.epicawaken_grappling_hook.projectile.hook;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.animation.ModHookAnimations;
import org.com.epicawaken_grappling_hook.enchantment.RopeExtensionEnchantment;
import org.com.epicawaken_grappling_hook.network.GrapplingHookFovType;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.OpenEntityHookChoicePacket;
import org.com.epicawaken_grappling_hook.network.StartGrapplingHookFovPacket;
import org.com.epicawaken_grappling_hook.network.StopGrapplingHookFovPacket;
import org.com.epicawaken_grappling_hook.network.SyncGrapplingHookArrivalPacket;
import org.com.epicawaken_grappling_hook.network.SyncGrapplingHookMissedPacket;
import org.com.epicawaken_grappling_hook.network.SyncGrapplingPullVelocityPacket;
import org.com.epicawaken_grappling_hook.util.AirHookArrivalJumpTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookArrivalTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookMissedTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookParcoolBlocker;
import org.com.epicawaken_grappling_hook.util.GrapplingShieldDisarmHandler;
import org.com.epicawaken_grappling_hook.util.GrapplingHookUse;
import org.com.epicawaken_grappling_hook.util.GrapplingSwingPhysics;
import org.com.epicawaken_grappling_hook.util.GroundHookSlideTracker;
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.network.PacketDistributor;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class GrapplingHook extends AbstractArrow {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(GrapplingHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_USE_MODE = SynchedEntityData.defineId(GrapplingHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SWINGING = SynchedEntityData.defineId(GrapplingHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SWING_DIRECTION = SynchedEntityData.defineId(GrapplingHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWING_PHASE_DURATION_TICKS = SynchedEntityData.defineId(GrapplingHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROPE_EXTENSION_LEVEL = SynchedEntityData.defineId(GrapplingHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ITEM_RETRIEVAL_RETURNING = SynchedEntityData.defineId(GrapplingHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ENTITY_HOOK_CHOICE_WAITING = SynchedEntityData.defineId(GrapplingHook.class, EntityDataSerializers.BOOLEAN);
    public static final float PULL_TARGET = 0.22F;
    private static final double AIR_HOOK_MIN_ANGLE_DEGREES = 3.0D;
    private static final double AIR_HOOK_MIN_HEIGHT_ABOVE_EYES = 1.0D;
    private static final double TERRAIN_PULL_CONTACT_DISTANCE = 0.08D;
    private static final double TERRAIN_PULL_HORIZONTAL_ARRIVAL_DISTANCE = 0.12D;
    private static final double TERRAIN_PULL_VERTICAL_ARRIVAL_DISTANCE = 0.45D;
    private static final double TERRAIN_PULL_OVERSHOOT_DISTANCE = 0.75D;
    private static final double WALL_TARGET_SURFACE_GAP = 0.0D;
    private static final double WALL_TOP_SURFACE_EPSILON = 0.02D;
    private static final double WALL_TOP_EDGE_GAP = 0.02D;
    private static final double WALL_TOP_APPROACH_CLEARANCE = 0.55D;
    private static final double WALL_TOP_CROSSING_CLEARANCE = 0.06D;
    private static final double WALL_TOP_APPROACH_HORIZONTAL_DISTANCE = 0.18D;
    private static final double WALL_TOP_ARRIVAL_HORIZONTAL_DISTANCE = 0.14D;
    private static final double WALL_TOP_ARRIVAL_VERTICAL_DISTANCE = 0.12D;
    private static final double SWING_REEL_MAX_DISTANCE_SLACK = 0.75D;
    private static final int SWING_PHASE_FALLBACK_DURATION_TICKS = 28;
    private static final int SWING_PHASE_MIN_DURATION_TICKS = 8;
    private static final int SWING_PHASE_MAX_DURATION_TICKS = 100;
    private static final double ITEM_RETRIEVAL_SEARCH_RADIUS = 1.5D;
    private static final double ITEM_RETRIEVAL_ATTRACTION_MIN_SPEED = 0.12D;
    private static final double ITEM_RETRIEVAL_ATTRACTION_MAX_SPEED = 0.55D;
    private static final double ITEM_RETRIEVAL_ATTRACTION_DISTANCE_SCALE = 0.35D;
    private static final double ITEM_RETRIEVAL_ATTACH_DISTANCE = 0.15D;
    private static final int ITEM_RETRIEVAL_ATTRACTION_TIMEOUT_TICKS = 10;
    private static final double ITEM_RETRIEVAL_MIN_RETURN_SPEED = 0.1D;
    private static final double ITEM_RETRIEVAL_ARRIVAL_DISTANCE = 1.25D;
    private static final int ENTITY_HOOK_CHOICE_WINDOW_TICKS = 10;
    private static final int ENTITY_TARGET_PULL_DELAY_TICKS = 2;
    private static final int ENTITY_HOOK_RELEASE_DELAY_TICKS = 4;
    private static final int AIR_HOOK_PULL_DELAY_TICKS = 6;
    private static final int AIR_HOOK_RELEASE_DELAY_TICKS = 10;
    private static final double[] COLLISION_FREE_TARGET_VERTICAL_OFFSETS = {0.0D, 0.25D, 0.5D, 1.0D, -0.25D};

    private int life;
    private boolean hooked;
    private HookType hookType = HookType.AIR;
    private Entity hookedEntity;
    private Vec3 terrainTarget;
    private Vec3 wallTopApproachTarget;
    private BlockPos wallTopSupportBlock;
    private double wallTopSupportSurfaceY;
    private boolean wallTopApproachComplete;
    private Vec3 groundSlideDirection;
    private Vec3 lastTerrainPullVelocity;
    private boolean terrainPullArrived;
    private boolean fovEffectActive;
    private int missedHookAnimationStartLife = -1;
    private boolean phantomMissedHookPull;
    private double previousTerrainTargetDistance = Double.MAX_VALUE;
    private int useSequenceId = -1;
    private boolean lockResolved;
    private Vec3 swingAnchor;
    private BlockPos swingAnchorBlock;
    private double swingRopeLength;
    private double swingTargetRopeLength;
    private int swingObstructedTicks;
    private boolean useFinished;
    private Vec3 swingForwardDirection;
    private Vec3 swingPlaneNormal;
    private int swingTravelDirection = 1;
    private double swingEnergy;
    private long swingPhaseStartGameTime;
    private int swingPhaseDurationTicks = SWING_PHASE_FALLBACK_DURATION_TICKS;
    private int lastForwardSwingPhaseDurationTicks = -1;
    private int lastBackwardSwingPhaseDurationTicks = -1;
    private boolean swingTransitionAnimationStopped;
    private boolean initialSwingHoldPlayed;
    private boolean swingAnimationDirectionCalibrated;
    private int swingAnimationForwardDirection;
    private AnimationManager.AnimationAccessor<? extends StaticAnimation> activeSwingAnimation;
    private final Map<UUID, Boolean> retrievedItemGravityStates = new LinkedHashMap<>();
    private final Set<UUID> attachedRetrievedItemIds = new HashSet<>();
    private boolean itemRetrievalAttracting;
    private int itemRetrievalAttractionTicks;
    private double retrievalReturnSpeed;
    private boolean grapplingShieldDisarmEnabled;
    private boolean entityHookChoiceStarted;
    private int entityHookChoiceDeadlineLife;
    private boolean entityPullToOwnerRequested;
    private boolean entityHookPullExecuted;
    private int entityHookPullExecutionLife;
    private int entityOwnerPullStartLife = -1;
    private int entityTargetPullStartLife = -1;
    private float entityTargetPullStrength;

    public GrapplingHook(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, GrapplingHookVariant.NORMAL.ordinal());
        this.entityData.define(DATA_USE_MODE, UseMode.NORMAL.ordinal());
        this.entityData.define(DATA_SWINGING, false);
        this.entityData.define(DATA_SWING_DIRECTION, 1);
        this.entityData.define(DATA_SWING_PHASE_DURATION_TICKS, SWING_PHASE_FALLBACK_DURATION_TICKS);
        this.entityData.define(DATA_ROPE_EXTENSION_LEVEL, 0);
        this.entityData.define(DATA_ITEM_RETRIEVAL_RETURNING, false);
        this.entityData.define(DATA_ENTITY_HOOK_CHOICE_WAITING, false);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("HookVariant", this.getVariant().name());
        tag.putInt("RopeExtensionLevel", this.getRopeExtensionLevel());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HookVariant")) {
            try {
                this.setVariant(GrapplingHookVariant.valueOf(tag.getString("HookVariant")));
            } catch (IllegalArgumentException ignored) {
                this.setVariant(GrapplingHookVariant.NORMAL);
            }
        }
        this.setRopeExtensionLevel(tag.getInt("RopeExtensionLevel"));
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
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            this.releaseRetrievedItems(null, false);
        }
        super.remove(reason);
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        if (this.getUseMode() == UseMode.ITEM_RETRIEVAL && this.isItemRetrievalReturning()) {
            this.inGround = false;
            this.noPhysics = true;
        }
        super.tick();

        int lockDelayTicks = Config.getHookLockDelayTicks();
        int maxLifeTicks = Config.getNormalHookMaxLifeTicks();
        Entity owner = this.getOwner();
        this.life++;
        if (owner == null) {
            this.discardHook();
            return;
        }

        if (!this.level().isClientSide && owner instanceof ServerPlayer serverPlayer && this.updateUseMode(serverPlayer)) {
            return;
        }

        if (this.getUseMode() == UseMode.ITEM_RETRIEVAL) {
            if (!this.level().isClientSide) {
                this.tickItemRetrieval(owner, maxLifeTicks);
            }
            return;
        }

        if (this.isSwinging()) {
            if (!this.level().isClientSide) {
                this.tickSwing(owner);
            }
            return;
        }

        if (this.life > maxLifeTicks && !this.entityHookChoiceStarted && this.hookedEntity == null) {
            this.discardHook();
            return;
        }
        GrapplingHookParcoolBlocker.block(owner, 2);

        if (this.hookedEntity != null) {
            this.setPos(this.hookedEntity.getX(), this.hookedEntity.getY(0.8D), this.hookedEntity.getZ());
        }

        if (!this.level().isClientSide
                && this.life < lockDelayTicks
                && this.getVariant().isPhantom()) {
            this.applyPhantomUseFallBrake(owner);
        }

        if (!this.level().isClientSide && this.hookedEntity != null && this.entityHookChoiceStarted) {
            this.tickEntityHook();
            return;
        }

        if (this.life < lockDelayTicks || this.getUseMode() == UseMode.PHANTOM_PENDING) {
            return;
        }

        if (!this.lockResolved) {
            if (this.getUseMode() == UseMode.PHANTOM_SWING) {
                if (!this.hooked) {
                    this.hooked = true;
                    this.swingAnchor = this.position();
                    this.swingAnchorBlock = null;
                    this.setDeltaMovement(Vec3.ZERO);
                }
                if (this.hookedEntity == null && this.swingAnchor != null) {
                    if (!this.level().isClientSide) {
                        this.startSwing(owner);
                    }
                    return;
                }
                this.setUseMode(UseMode.NORMAL);
            }

            this.lockResolved = true;
            if (!this.hooked) {
                if (this.getVariant().isPhantom()) {
                    this.startPhantomMissedHookPull(owner, lockDelayTicks);
                } else {
                    this.startMissedHookGroundAnimation(owner, lockDelayTicks);
                    return;
                }
            } else {
                this.lockHookType();
            }
        }
        if (!this.hooked) {
            return;
        }

        switch (this.hookType) {
            case ENTITY -> this.tickEntityHook();
            case AIR -> this.tickAirHook(lockDelayTicks);
            case GROUND -> this.tickGroundHook(lockDelayTicks);
            case MISSED -> this.tickMissedHook(lockDelayTicks);
        }
    }

    private void applyPhantomUseFallBrake(Entity owner) {
        if (!(owner instanceof Player player)
                || player.onGround()
                || player.isInWaterOrBubble()
                || player.isFallFlying()
                || player.isPassenger()) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y >= 0.0D) {
            return;
        }

        double slowedFallSpeed = Math.max(
                velocity.y * Config.phantomSwingPreholdFallMultiplier,
                -Config.phantomSwingPreholdMaxFallSpeed);
        if (slowedFallSpeed >= velocity.y) {
            player.setDeltaMovement(velocity.x, slowedFallSpeed, velocity.z);
            player.hurtMarked = true;
        }
    }

    private boolean updateUseMode(ServerPlayer player) {
        if (this.useSequenceId < 0
                || this.getUseMode() == UseMode.NORMAL
                || this.getUseMode() == UseMode.ITEM_RETRIEVAL) {
            return false;
        }

        GrapplingHookUse.HoldDecision decision = GrapplingHookUse.getHoldDecision(player, this.useSequenceId);
        return switch (decision) {
            case NORMAL -> {
                this.setUseMode(UseMode.NORMAL);
                yield false;
            }
            case PENDING -> false;
            case SWING_HELD -> {
                this.setUseMode(UseMode.PHANTOM_SWING);
                this.startInitialSwingHoldAnimation(player);
                yield false;
            }
            case SWING_RELEASED -> {
                this.setUseMode(UseMode.PHANTOM_SWING);
                this.discardHook();
                yield true;
            }
        };
    }

    private void stopSwingTransitionAnimation(ServerPlayer player) {
        LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
        if (entityPatch != null) {
            entityPatch.stopPlaying(ModHookAnimations.HOOK_PULL);
        }
    }

    private void startInitialSwingHoldAnimation(ServerPlayer player) {
        if (this.initialSwingHoldPlayed) {
            return;
        }

        if (this.playSwingAnimation(player, ModHookAnimations.HOOK_HOLD)) {
            this.initialSwingHoldPlayed = true;
        }
    }

    private void startForwardSwingAnimation(ServerPlayer player) {
        AnimationManager.AnimationAccessor<? extends StaticAnimation> animation = this.initialSwingHoldPlayed
                ? ModHookAnimations.HOOK_HOLD_FORWARD
                : ModHookAnimations.HOOK_HOLD;
        if (this.playSwingAnimation(player, animation)) {
            this.initialSwingHoldPlayed = true;
        }
    }

    private void startBackwardSwingAnimation(ServerPlayer player) {
        this.playSwingAnimation(player, ModHookAnimations.HOOK_HOLD_BACK);
    }

    private static String swingAnimationName(
            AnimationManager.AnimationAccessor<? extends StaticAnimation> animation) {
        if (animation == ModHookAnimations.HOOK_HOLD_FORWARD) {
            return "hook_hold_forward";
        }
        if (animation == ModHookAnimations.HOOK_HOLD_BACK) {
            return "hook_hold_back";
        }
        return "hook_hold";
    }

    private boolean playSwingAnimation(
            ServerPlayer player,
            AnimationManager.AnimationAccessor<? extends StaticAnimation> animation) {
        LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
        if (entityPatch != null) {
            entityPatch.playAnimationSynchronized(animation, 0.0F);
            this.activeSwingAnimation = animation;
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info(
                        "[GrapplingHookSwingAnimationDebug][SERVER] animation played owner={} hook={} animation={} swingDirection={} calibratedForwardDirection={} initialHoldPlayed={}",
                        player.getId(),
                        this.getId(),
                        swingAnimationName(animation),
                        this.swingTravelDirection,
                        this.swingAnimationForwardDirection,
                        this.initialSwingHoldPlayed);
            }
            return true;
        }
        return false;
    }

    private void stopSwingAnimation(ServerPlayer player) {
        if (this.activeSwingAnimation == null) {
            return;
        }

        AnimationManager.AnimationAccessor<? extends StaticAnimation> animation = this.activeSwingAnimation;
        this.activeSwingAnimation = null;
        LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
        if (entityPatch != null) {
            entityPatch.stopPlaying(animation);
        }
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info(
                    "[GrapplingHookSwingAnimationDebug][SERVER] animation stopped owner={} hook={} animation={} swingDirection={} calibratedForwardDirection={}",
                    player.getId(),
                    this.getId(),
                    swingAnimationName(animation),
                    this.swingTravelDirection,
                    this.swingAnimationForwardDirection);
        }
    }

    private void updateSwingDirectionAnimation(ServerPlayer player, int direction) {
        if (!this.swingAnimationDirectionCalibrated) {
            return;
        }
        if (direction == this.swingAnimationForwardDirection) {
            this.startForwardSwingAnimation(player);
        } else {
            this.startBackwardSwingAnimation(player);
        }
    }

    private void recordCompletedSwingPhase(int direction, int durationTicks) {
        if (direction == this.swingAnimationForwardDirection) {
            this.lastForwardSwingPhaseDurationTicks = durationTicks;
        } else {
            this.lastBackwardSwingPhaseDurationTicks = durationTicks;
        }
    }

    private int predictSwingPhaseDuration(int direction, int fallbackDurationTicks) {
        int sameDirectionDuration = direction == this.swingAnimationForwardDirection
                ? this.lastForwardSwingPhaseDurationTicks
                : this.lastBackwardSwingPhaseDurationTicks;
        return sameDirectionDuration > 0 ? sameDirectionDuration : fallbackDurationTicks;
    }

    private int defaultSwingPhaseDurationTicks() {
        if (ModHookAnimations.HOOK_HOLD == null || !ModHookAnimations.HOOK_HOLD.isPresent()) {
            return SWING_PHASE_FALLBACK_DURATION_TICKS;
        }
        return Mth.clamp(
                Mth.ceil(ModHookAnimations.HOOK_HOLD.get().getTotalTime() * 20.0F),
                SWING_PHASE_MIN_DURATION_TICKS,
                SWING_PHASE_MAX_DURATION_TICKS);
    }

    private void startSwing(Entity owner) {
        if (this.swingAnchor == null || !(owner instanceof ServerPlayer serverPlayer)) {
            this.discardHook();
            return;
        }
        if (!GrapplingHookUse.hasEquippedVariant(serverPlayer, GrapplingHookVariant.PHANTOM)) {
            this.discardHook();
            return;
        }

        this.lockResolved = true;
        this.hooked = true;
        this.hookType = HookType.AIR;
        this.terrainTarget = null;
        this.clearWallTopTarget();
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.swingAnchor.x, this.swingAnchor.y, this.swingAnchor.z);
        this.inGround = true;
        this.swingRopeLength = GrapplingSwingPhysics.calculateInitialRopeLength(
                owner,
                this.swingAnchor,
                this.getPhantomLaunchRange());
        this.swingTargetRopeLength = Math.min(this.swingRopeLength, this.getPhantomSwingTargetRopeLength());
        if (this.swingForwardDirection == null || this.swingForwardDirection.lengthSqr() < 1.0E-8D) {
            this.swingForwardDirection = horizontalLookDirection(owner);
        }
        this.swingPlaneNormal = GrapplingSwingPhysics.calculateSwingPlaneNormal(
                owner,
                this.swingAnchor,
                this.swingForwardDirection);
        this.entityData.set(DATA_SWINGING, true);
        GrapplingHookParcoolBlocker.block(owner, 2);
        this.syncTerrainPullArrival(owner);
        this.startPullFovEffect(owner);
        this.swingTravelDirection = GrapplingSwingPhysics.calculateInitialTravelDirection(
                owner,
                this.swingAnchor,
                this.swingPlaneNormal);
        this.entityData.set(DATA_SWING_DIRECTION, this.swingTravelDirection);
        this.swingPhaseStartGameTime = this.level().getGameTime();
        this.swingPhaseDurationTicks = this.defaultSwingPhaseDurationTicks();
        this.entityData.set(DATA_SWING_PHASE_DURATION_TICKS, this.swingPhaseDurationTicks);
        this.startInitialSwingHoldAnimation(serverPlayer);
        GrapplingSwingPhysics.applyInitialBoost(
                owner,
                this.swingAnchor,
                this.swingTargetRopeLength,
                this.swingPlaneNormal,
                this.swingTravelDirection);
        this.swingEnergy = GrapplingSwingPhysics.calculateInitialEnergy(
                owner,
                this.swingAnchor,
                this.swingRopeLength,
                this.swingTargetRopeLength,
                this.swingPlaneNormal);

        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSwingDebug][SERVER] swing started owner={} hook={} sequence={} anchor={} ropeLength={} targetRopeLength={} configuredTargetRopeLength={} ownerPos={} ownerVelocity={}",
                    owner.getId(),
                    this.getId(),
                    this.useSequenceId,
                    this.swingAnchor,
                    this.swingRopeLength,
                    this.swingTargetRopeLength,
                    this.getPhantomSwingTargetRopeLength(),
                    owner.position(),
                    owner.getDeltaMovement());
        }
    }

    private void tickSwing(Entity owner) {
        if (!(owner instanceof ServerPlayer serverPlayer)
                || !serverPlayer.isAlive()
                || serverPlayer.isSpectator()
                || this.swingAnchor == null
                || !GrapplingHookUse.hasEquippedVariant(serverPlayer, GrapplingHookVariant.PHANTOM)) {
            this.discardHook();
            return;
        }

        if (this.swingAnchorBlock != null
                && this.level().getBlockState(this.swingAnchorBlock).getCollisionShape(this.level(), this.swingAnchorBlock).isEmpty()) {
            this.discardHook();
            return;
        }

        GrapplingHookParcoolBlocker.block(owner, 2);
        this.setPos(this.swingAnchor.x, this.swingAnchor.y, this.swingAnchor.z);
        if (this.swingRopeLength > this.swingTargetRopeLength) {
            double actualDistance = GrapplingSwingPhysics.attachmentPosition(owner).distanceTo(this.swingAnchor);
            double desiredRopeLength = Math.max(
                    this.swingTargetRopeLength,
                    this.swingRopeLength - Config.phantomSwingReelInSpeed);
            double minimumRopeLengthForCurrentDistance = Math.max(
                    this.swingTargetRopeLength,
                    actualDistance - SWING_REEL_MAX_DISTANCE_SLACK);
            this.swingRopeLength = Math.min(
                    this.swingRopeLength,
                    Math.max(desiredRopeLength, minimumRopeLengthForCurrentDistance));
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info(
                        "[GrapplingHookSwingDebug][SERVER] reeling owner={} hook={} ropeLength={} targetRopeLength={} actualDistance={}",
                        owner.getId(),
                        this.getId(),
                        this.swingRopeLength,
                        this.swingTargetRopeLength,
                        actualDistance);
            }
        }
        GrapplingSwingPhysics.SwingResult swingResult = GrapplingSwingPhysics.tick(
                owner,
                this.swingAnchor,
                this.swingRopeLength,
                this.swingTargetRopeLength,
                this.swingPlaneNormal,
                this.swingEnergy,
                this.swingTravelDirection);
        this.swingEnergy = swingResult.energy();
        int previousTravelDirection = this.swingTravelDirection;
        this.swingTravelDirection = swingResult.travelDirection();
        long currentGameTime = this.level().getGameTime();
        if (!this.swingAnimationDirectionCalibrated) {
            this.swingAnimationDirectionCalibrated = true;
            this.swingAnimationForwardDirection = this.swingTravelDirection;
            this.swingPhaseStartGameTime = currentGameTime;
            this.entityData.set(DATA_SWING_DIRECTION, this.swingTravelDirection);
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info(
                        "[GrapplingHookSwingAnimationDebug][SERVER] direction calibrated owner={} hook={} forwardDirection={} initialDirection={} ropeLength={} velocity={}",
                        owner.getId(),
                        this.getId(),
                        this.swingAnimationForwardDirection,
                        previousTravelDirection,
                        this.swingRopeLength,
                        swingResult.velocity());
            }
        } else if (this.swingTravelDirection != previousTravelDirection) {
            int completedPhaseDuration = Mth.clamp(
                    (int) Math.max(1L, currentGameTime - this.swingPhaseStartGameTime),
                    SWING_PHASE_MIN_DURATION_TICKS,
                    SWING_PHASE_MAX_DURATION_TICKS);
            this.recordCompletedSwingPhase(previousTravelDirection, completedPhaseDuration);
            int predictedPhaseDuration = this.predictSwingPhaseDuration(
                    this.swingTravelDirection,
                    completedPhaseDuration);
            this.swingPhaseStartGameTime = currentGameTime;
            this.swingPhaseDurationTicks = predictedPhaseDuration;
            this.entityData.set(DATA_SWING_PHASE_DURATION_TICKS, predictedPhaseDuration);
            this.entityData.set(DATA_SWING_DIRECTION, this.swingTravelDirection);
            this.updateSwingDirectionAnimation(serverPlayer, this.swingTravelDirection);
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info(
                        "[GrapplingHookSwingAnimationDebug][SERVER] direction changed owner={} hook={} direction={} forwardDirection={} completedPhaseDurationTicks={} predictedPhaseDurationTicks={} lastForwardPhaseDurationTicks={} lastBackwardPhaseDurationTicks={} ropeLength={} energy={}",
                        owner.getId(),
                        this.getId(),
                        this.swingTravelDirection,
                        this.swingAnimationForwardDirection,
                        completedPhaseDuration,
                        predictedPhaseDuration,
                        this.lastForwardSwingPhaseDurationTicks,
                        this.lastBackwardSwingPhaseDurationTicks,
                        this.swingRopeLength,
                        this.swingEnergy);
            }
        }
        owner.setDeltaMovement(swingResult.velocity());
    }

    private boolean isSwingPathObstructed(Entity owner) {
        BlockHitResult hitResult = this.level().clip(new ClipContext(
                GrapplingSwingPhysics.attachmentPosition(owner),
                this.swingAnchor,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                owner));
        return hitResult.getType() == HitResult.Type.BLOCK && !hitResult.getBlockPos().equals(this.swingAnchorBlock);
    }

    private void startPhantomMissedHookPull(Entity owner, int lockDelayTicks) {
        this.hooked = true;
        this.phantomMissedHookPull = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.lockHookType();
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][SERVER] started phantom missed hook pull owner={} hookLife={} lockDelayTicks={} hookType={} hookPos={} ownerPos={} hookVelocity={} ownerVelocity={}",
                    owner.getId(),
                    this.life,
                    lockDelayTicks,
                    this.hookType,
                    this.position(),
                    owner.position(),
                    this.getDeltaMovement(),
                    owner.getDeltaMovement());
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
        int cleanupTicks = Config.getMissedHookVisualTicks();
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

    private void tickEntityHook() {
        if (this.level().isClientSide) {
            return;
        }

        if (this.hookedEntity == null || !this.hookedEntity.isAlive()) {
            this.discardHook();
            return;
        }

        if (!this.entityHookChoiceStarted) {
            this.startEntityHookChoice();
            return;
        }

        if (this.entityOwnerPullStartLife >= 0) {
            this.tickAirHookFromStart(this.entityOwnerPullStartLife);
            return;
        }

        if (this.entityTargetPullStartLife >= 0) {
            this.tickTargetPullToOwner();
            return;
        }

        if (!this.entityHookPullExecuted) {
            if (this.entityPullToOwnerRequested) {
                this.executeEntityHookPull(true);
            } else if (this.life >= this.entityHookChoiceDeadlineLife) {
                this.executeEntityHookPull(false);
            }
            return;
        }

        if (this.life >= this.entityHookPullExecutionLife + ENTITY_HOOK_RELEASE_DELAY_TICKS) {
            this.discardHook();
        }
    }

    private void startEntityHookChoice() {
        if (this.level().isClientSide || this.entityHookChoiceStarted) {
            return;
        }

        this.entityHookChoiceStarted = true;
        this.entityData.set(DATA_ENTITY_HOOK_CHOICE_WAITING, true);
        this.entityHookChoiceDeadlineLife = this.life + ENTITY_HOOK_CHOICE_WINDOW_TICKS;
        this.setDeltaMovement(Vec3.ZERO);
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info(
                    "[GrapplingHookEntityChoiceDebug][SERVER] choice started owner={} hook={} target={} life={} deadline={} windowTicks={}",
                    this.getOwner() == null ? -1 : this.getOwner().getId(),
                    this.getId(),
                    this.hookedEntity == null ? -1 : this.hookedEntity.getId(),
                    this.life,
                    this.entityHookChoiceDeadlineLife,
                    ENTITY_HOOK_CHOICE_WINDOW_TICKS);
        }
        if (this.getOwner() instanceof ServerPlayer serverPlayer) {
            serverPlayer.playNotifySound(
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new OpenEntityHookChoicePacket(this.useSequenceId, ENTITY_HOOK_CHOICE_WINDOW_TICKS));
        }
    }

    private void executeEntityHookPull(boolean pullTargetToOwner) {
        Entity owner = this.getOwner();
        if (owner == null || this.hookedEntity == null) {
            this.discardHook();
            return;
        }

        this.entityData.set(DATA_ENTITY_HOOK_CHOICE_WAITING, false);

        if (pullTargetToOwner) {
            if (this.grapplingShieldDisarmEnabled
                    && owner instanceof ServerPlayer serverPlayer
                    && this.hookedEntity instanceof LivingEntity livingEntity) {
                GrapplingShieldDisarmHandler.tryDisarm(serverPlayer, livingEntity);
            }

            float pullTargetStrength = this.getEntityPullStrength();
            if (Config.respectKnockbackResistance && pullTargetStrength <= 0.05F) {
                this.startOwnerPullToHookedEntity();
            } else {
                this.startTargetPullToOwner(pullTargetStrength);
            }
        } else {
            this.startOwnerPullToHookedEntity();
        }
    }

    private void startOwnerPullToHookedEntity() {
        if (this.entityOwnerPullStartLife >= 0) {
            return;
        }

        this.hookType = HookType.AIR;
        this.entityOwnerPullStartLife = this.life;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info(
                    "[GrapplingHookEntityChoiceDebug][SERVER] owner pull phase started owner={} hook={} target={} life={}",
                    this.getOwner() == null ? -1 : this.getOwner().getId(),
                    this.getId(),
                    this.hookedEntity == null ? -1 : this.hookedEntity.getId(),
                    this.life);
        }
        this.tickAirHookFromStart(this.entityOwnerPullStartLife);
    }

    private void startTargetPullToOwner(float pullTargetStrength) {
        if (this.entityTargetPullStartLife >= 0) {
            return;
        }

        this.entityTargetPullStartLife = this.life;
        this.entityTargetPullStrength = pullTargetStrength;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info(
                    "[GrapplingHookEntityChoiceDebug][SERVER] target pull phase started owner={} hook={} target={} life={} strength={}",
                    this.getOwner() == null ? -1 : this.getOwner().getId(),
                    this.getId(),
                    this.hookedEntity == null ? -1 : this.hookedEntity.getId(),
                    this.life,
                    pullTargetStrength);
        }
    }

    private void tickTargetPullToOwner() {
        if (!this.entityHookPullExecuted
                && this.life >= this.entityTargetPullStartLife + ENTITY_TARGET_PULL_DELAY_TICKS) {
            this.pullHookedEntityToOwner(this.entityTargetPullStrength);
            this.syncPullVelocity(this.hookedEntity);
            this.entityHookPullExecuted = true;
            this.entityHookPullExecutionLife = this.life;
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info(
                        "[GrapplingHookEntityChoiceDebug][SERVER] target pull impulse applied owner={} hook={} target={} life={} phaseStartLife={} strength={}",
                        this.getOwner() == null ? -1 : this.getOwner().getId(),
                        this.getId(),
                        this.hookedEntity == null ? -1 : this.hookedEntity.getId(),
                        this.life,
                        this.entityTargetPullStartLife,
                        this.entityTargetPullStrength);
            }
        }

        if (this.entityHookPullExecuted
                && this.life >= this.entityHookPullExecutionLife + ENTITY_HOOK_RELEASE_DELAY_TICKS) {
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
        this.tickAirHookFromStart(lockDelayTicks);
    }

    private void tickAirHookFromStart(int animationStartLife) {
        if (this.life == animationStartLife) {
            this.playAnimation(this.getOwner(), ModHookAnimations.HOOK_AIR);
            return;
        }

        if (this.terrainTarget != null && this.tickTerrainTargetPull(animationStartLife + AIR_HOOK_PULL_DELAY_TICKS)) {
            return;
        }

        if (this.life == animationStartLife + AIR_HOOK_PULL_DELAY_TICKS) {
            if (this.phantomMissedHookPull && this.getVariant().isPhantom()) {
                this.applyPhantomMissedAirPull(this.getOwner());
            } else {
                this.hookPull(this.getOwner(), (float) Config.airPullStrength);
                this.syncPullVelocity(this.getOwner());
            }
            if (Config.debugLogging && this.entityOwnerPullStartLife >= 0) {
                Epicawaken_grappling_hook.LOGGER.info(
                        "[GrapplingHookEntityChoiceDebug][SERVER] owner pull impulse applied owner={} hook={} target={} life={} animationStartLife={}",
                        this.getOwner() == null ? -1 : this.getOwner().getId(),
                        this.getId(),
                        this.hookedEntity == null ? -1 : this.hookedEntity.getId(),
                        this.life,
                        animationStartLife);
            }
            return;
        }

        if (this.life == animationStartLife + AIR_HOOK_RELEASE_DELAY_TICKS) {
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
        if (owner == null || this.hookType != HookType.GROUND || this.isWallTopTerrainTarget()) {
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

    private void applyPhantomMissedAirPull(Entity owner) {
        if (owner == null) {
            return;
        }

        Vec3 oldVelocity = owner.getDeltaMovement();
        Vec3 toAnchor = this.position().subtract(owner.position());
        Vec3 horizontalToAnchor = new Vec3(toAnchor.x, 0.0D, toAnchor.z);
        double horizontalDistance = horizontalToAnchor.length();
        double horizontalPull = Math.min(
                Config.phantomMissedAirMaxHorizontalSpeed,
                horizontalDistance * Config.phantomMissedAirHorizontalPullStrength);

        Vec3 horizontalVelocity = new Vec3(oldVelocity.x, 0.0D, oldVelocity.z);
        if (horizontalDistance > 1.0E-6D && horizontalPull > 0.0D) {
            horizontalVelocity = horizontalVelocity.add(horizontalToAnchor.scale(horizontalPull / horizontalDistance));
        }
        if (horizontalVelocity.lengthSqr() > Config.phantomMissedAirMaxHorizontalSpeed * Config.phantomMissedAirMaxHorizontalSpeed) {
            horizontalVelocity = horizontalVelocity.normalize().scale(Config.phantomMissedAirMaxHorizontalSpeed);
        }

        double maxTotalSpeed = Math.max(0.1D, Config.phantomMissedAirMaxTotalSpeed);
        double maxUpSpeed = Math.min(Config.phantomMissedAirMaxUpSpeed, maxTotalSpeed);
        double anchorHeight = Math.max(0.0D, toAnchor.y);
        double targetUpSpeed = Mth.clamp(
                Config.phantomMissedAirBaseUpSpeed + anchorHeight * Config.phantomMissedAirHeightUpScale,
                0.0D,
                maxUpSpeed);
        double verticalVelocity = Mth.clamp(Math.max(oldVelocity.y, targetUpSpeed), -maxTotalSpeed, maxUpSpeed);
        double horizontalAllowance = Math.sqrt(Math.max(0.0D, maxTotalSpeed * maxTotalSpeed - verticalVelocity * verticalVelocity));
        if (horizontalVelocity.lengthSqr() > horizontalAllowance * horizontalAllowance) {
            horizontalVelocity = horizontalAllowance > 1.0E-6D
                    ? horizontalVelocity.normalize().scale(horizontalAllowance)
                    : Vec3.ZERO;
        }

        Vec3 newVelocity = new Vec3(horizontalVelocity.x, verticalVelocity, horizontalVelocity.z);
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookPhantomAirPullDebug][{}] applied owner={} hookLife={} hookPos={} ownerPos={} toAnchor={} anchorHeight={} horizontalDistance={} horizontalPull={} targetUpSpeed={} oldVelocity={} newVelocity={} maxHorizontalSpeed={} maxUpSpeed={} maxTotalSpeed={}",
                    this.level().isClientSide ? "CLIENT" : "SERVER",
                    owner.getId(),
                    this.life,
                    this.position(),
                    owner.position(),
                    toAnchor,
                    anchorHeight,
                    horizontalDistance,
                    horizontalPull,
                    targetUpSpeed,
                    oldVelocity,
                    newVelocity,
                    Config.phantomMissedAirMaxHorizontalSpeed,
                    maxUpSpeed,
                    maxTotalSpeed);
        }

        this.startPullFovEffect(owner);
        owner.setDeltaMovement(newVelocity);
        owner.fallDistance = 0.0F;
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

    private void syncPullVelocity(Entity entity) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new SyncGrapplingPullVelocityPacket(entity.getId(), entity.getDeltaMovement()));
    }

    private boolean pullOwnerToTerrainTarget(Entity owner) {
        if (owner == null || this.terrainTarget == null) {
            return true;
        }

        Vec3 activeTarget = this.getActiveTerrainPullTarget(owner);
        Vec3 delta = activeTarget.subtract(owner.position());
        double distance = delta.length();
        if (!this.isWallTopTerrainTarget()
                && this.hookType == HookType.GROUND
                && Config.groundHookSlideEnabled
                && distance <= Config.groundHookSlideStartDistance) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] ground target pull pre-start slide owner={} hookLife={} distance={} startDistance={} horizontalDistance={} verticalDistance={} ownerPos={} target={} ownerVelocity={}",
                        owner.getId(),
                        this.life,
                        distance,
                        Config.groundHookSlideStartDistance,
                        horizontalDistance(delta),
                        Math.abs(delta.y),
                        owner.position(),
                        activeTarget,
                        owner.getDeltaMovement());
            }
            this.markTerrainPullArrived(owner);
            return false;
        }

        boolean reachedTarget = this.isWallTopTerrainTarget()
                ? this.wallTopApproachComplete && this.hasReachedWallTopTerrainTarget(owner, delta)
                : (this.hookType == HookType.AIR || this.hookType == HookType.GROUND)
                && this.hasReachedTerrainTarget(delta, distance);
        if (reachedTarget) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][SERVER] {} terrain target reached life={} distance={} horizontalDistance={} verticalDistance={} previousDistance={} target={} ownerPos={}",
                        this.hookType,
                        this.life,
                        distance,
                        horizontalDistance(delta),
                        Math.abs(delta.y),
                        this.previousTerrainTargetDistance,
                        activeTarget,
                        owner.position());
            }
            this.markTerrainPullArrived(owner);
            return false;
        }

        double arrivalDistance = this.getTargetPullArrivalDistance();
        if (!this.isWallTopTerrainTarget() && distance <= arrivalDistance) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] terrain target pull finished by config arrival hookType={} owner={} hookLife={} distance={} arrivalDistance={} ownerPos={} target={} ownerVelocity={}",
                        this.hookType,
                        owner.getId(),
                        this.life,
                        distance,
                        arrivalDistance,
                        owner.position(),
                        activeTarget,
                        owner.getDeltaMovement());
            }
            return true;
        }

        double minSpeed = this.getTargetPullMinSpeed();
        double maxSpeed = this.getTargetPullMaxSpeed();
        double speed = Math.min(maxSpeed, Math.max(minSpeed, distance * 0.35D));
        if (this.isWallTopTerrainTarget()) {
            speed = Math.min(speed, distance);
        }
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
                    activeTarget,
                    owner.getDeltaMovement(),
                    newVelocity);
        }
        this.startPullFovEffect(owner);
        owner.setDeltaMovement(newVelocity);
        owner.fallDistance = 0.0F;
        owner.hurtMarked = true;
        return false;
    }

    private Vec3 getActiveTerrainPullTarget(Entity owner) {
        if (!this.isWallTopTerrainTarget() || this.wallTopApproachComplete) {
            return this.terrainTarget;
        }

        Vec3 approachDelta = this.wallTopApproachTarget.subtract(owner.position());
        if (horizontalDistance(approachDelta) <= WALL_TOP_APPROACH_HORIZONTAL_DISTANCE
                && owner.getY() >= this.wallTopSupportSurfaceY + WALL_TOP_CROSSING_CLEARANCE) {
            this.wallTopApproachComplete = true;
            this.previousTerrainTargetDistance = Double.MAX_VALUE;
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] wall top approach completed owner={} hookLife={} ownerPos={} approachTarget={} landingTarget={} supportBlock={} supportSurfaceY={}",
                        owner.getId(),
                        this.life,
                        owner.position(),
                        this.wallTopApproachTarget,
                        this.terrainTarget,
                        this.wallTopSupportBlock,
                        this.wallTopSupportSurfaceY);
            }
            return this.terrainTarget;
        }

        return this.wallTopApproachTarget;
    }

    private boolean hasReachedWallTopTerrainTarget(Entity owner, Vec3 delta) {
        if (horizontalDistance(delta) > WALL_TOP_ARRIVAL_HORIZONTAL_DISTANCE
                || Math.abs(delta.y) > WALL_TOP_ARRIVAL_VERTICAL_DISTANCE
                || owner.getY() < this.wallTopSupportSurfaceY - WALL_TOP_ARRIVAL_VERTICAL_DISTANCE) {
            return false;
        }

        VoxelShape supportShape = this.level().getBlockState(this.wallTopSupportBlock)
                .getCollisionShape(this.level(), this.wallTopSupportBlock);
        if (supportShape.isEmpty()) {
            return false;
        }

        double supportSurfaceY = this.wallTopSupportBlock.getY() + supportShape.max(Direction.Axis.Y);
        return Math.abs(supportSurfaceY - this.wallTopSupportSurfaceY) <= WALL_TOP_SURFACE_EPSILON;
    }

    private boolean isWallTopTerrainTarget() {
        return this.wallTopApproachTarget != null && this.wallTopSupportBlock != null;
    }

    private void clearWallTopTarget() {
        this.wallTopApproachTarget = null;
        this.wallTopSupportBlock = null;
        this.wallTopSupportSurfaceY = 0.0D;
        this.wallTopApproachComplete = false;
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
        this.entityData.set(DATA_ENTITY_HOOK_CHOICE_WAITING, false);
        if (!this.level().isClientSide) {
            this.releaseRetrievedItems(null, false);
        }
        if (this.useFinished) {
            this.discard();
            return;
        }

        this.useFinished = true;
        this.stopPullFovEffect();
        if (!this.level().isClientSide && this.getOwner() instanceof ServerPlayer serverPlayer && this.useSequenceId >= 0) {
            if (!this.swingTransitionAnimationStopped && this.getVariant().isPhantom()) {
                this.stopSwingTransitionAnimation(serverPlayer);
                this.swingTransitionAnimationStopped = true;
            }
            this.stopSwingAnimation(serverPlayer);
            GrapplingHookUse.finishUse(
                    serverPlayer,
                    this.useSequenceId,
                    this.getId(),
                    this.getUseMode() == UseMode.PHANTOM_SWING);
        }
        this.discard();
    }

    private void tickItemRetrieval(Entity owner, int maxLifeTicks) {
        if (!(owner instanceof ServerPlayer player) || !player.isAlive()) {
            this.discardHook();
            return;
        }

        if (!this.isItemRetrievalReturning()) {
            if (this.itemRetrievalAttracting) {
                this.tickItemRetrievalAttraction(player);
            } else if (this.hooked) {
                if (this.hookedEntity != null && this.hookedEntity.isAlive()) {
                    this.setPos(
                            this.hookedEntity.getX(),
                            this.hookedEntity.getY(0.8D),
                            this.hookedEntity.getZ());
                }
                this.beginItemRetrieval(player);
            } else if (this.life > maxLifeTicks) {
                this.discardHook();
            }
            return;
        }

        Vec3 target = player.getEyePosition().add(0.0D, -0.35D, 0.0D);
        Vec3 toPlayer = target.subtract(this.position());
        double distance = toPlayer.length();
        double returnSpeed = Math.max(ITEM_RETRIEVAL_MIN_RETURN_SPEED, this.retrievalReturnSpeed);
        if (distance <= Math.max(ITEM_RETRIEVAL_ARRIVAL_DISTANCE, returnSpeed)) {
            this.setPos(target.x, target.y, target.z);
            this.releaseRetrievedItems(player, true);
            this.discardHook();
            return;
        }

        Vec3 returnVelocity = toPlayer.scale(returnSpeed / distance);
        this.setDeltaMovement(returnVelocity);
        this.moveRetrievedItemsWithHook();
    }

    private void beginItemRetrieval(ServerPlayer player) {
        this.captureNearbyItems();
        this.hooked = false;
        this.hookedEntity = null;
        this.terrainTarget = null;
        this.swingAnchor = null;
        this.swingAnchorBlock = null;
        this.inGround = false;
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        if (this.retrievedItemGravityStates.isEmpty()) {
            this.startItemRetrievalReturn(player);
            return;
        }

        this.itemRetrievalAttracting = true;
        this.itemRetrievalAttractionTicks = 0;
    }

    private void tickItemRetrievalAttraction(ServerPlayer player) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            this.discardHook();
            return;
        }

        this.setDeltaMovement(Vec3.ZERO);
        this.itemRetrievalAttractionTicks++;
        Iterator<Map.Entry<UUID, Boolean>> iterator = this.retrievedItemGravityStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Boolean> entry = iterator.next();
            Entity entity = serverLevel.getEntity(entry.getKey());
            if (!(entity instanceof ItemEntity item) || !item.isAlive() || item.getItem().isEmpty()) {
                this.attachedRetrievedItemIds.remove(entry.getKey());
                iterator.remove();
                continue;
            }

            item.setNoGravity(true);
            item.setPickUpDelay(20);
            item.setDeltaMovement(Vec3.ZERO);
            if (this.attachedRetrievedItemIds.contains(entry.getKey())) {
                item.setPos(this.getX(), this.getY(), this.getZ());
                item.hurtMarked = true;
                continue;
            }

            Vec3 toHook = this.position().subtract(item.position());
            double distance = toHook.length();
            if (distance <= ITEM_RETRIEVAL_ATTACH_DISTANCE) {
                this.attachedRetrievedItemIds.add(entry.getKey());
                item.setPos(this.getX(), this.getY(), this.getZ());
                item.hurtMarked = true;
                continue;
            }

            double attractionSpeed = Mth.clamp(
                    distance * ITEM_RETRIEVAL_ATTRACTION_DISTANCE_SCALE,
                    ITEM_RETRIEVAL_ATTRACTION_MIN_SPEED,
                    ITEM_RETRIEVAL_ATTRACTION_MAX_SPEED);
            Vec3 attractionVelocity = toHook.scale(Math.min(distance, attractionSpeed) / distance);
            item.setDeltaMovement(attractionVelocity);
            item.hurtMarked = true;
        }

        if (this.retrievedItemGravityStates.isEmpty()
                || this.attachedRetrievedItemIds.size() >= this.retrievedItemGravityStates.size()) {
            this.startItemRetrievalReturn(player);
            return;
        }

        if (this.itemRetrievalAttractionTicks >= ITEM_RETRIEVAL_ATTRACTION_TIMEOUT_TICKS) {
            this.releaseUnattachedRetrievedItems(serverLevel);
            this.startItemRetrievalReturn(player);
        }
    }

    private void startItemRetrievalReturn(ServerPlayer player) {
        this.itemRetrievalAttracting = false;
        this.itemRetrievalAttractionTicks = 0;
        this.entityData.set(DATA_ITEM_RETRIEVAL_RETURNING, true);

        Vec3 target = player.getEyePosition().add(0.0D, -0.35D, 0.0D);
        Vec3 toPlayer = target.subtract(this.position());
        double distance = toPlayer.length();
        if (distance > 1.0E-6D) {
            double returnSpeed = Math.max(ITEM_RETRIEVAL_MIN_RETURN_SPEED, this.retrievalReturnSpeed);
            this.setDeltaMovement(toPlayer.scale(returnSpeed / distance));
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
        this.moveRetrievedItemsWithHook();
    }

    private void releaseUnattachedRetrievedItems(ServerLevel serverLevel) {
        Iterator<Map.Entry<UUID, Boolean>> iterator = this.retrievedItemGravityStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Boolean> entry = iterator.next();
            if (this.attachedRetrievedItemIds.contains(entry.getKey())) {
                continue;
            }

            Entity entity = serverLevel.getEntity(entry.getKey());
            if (entity instanceof ItemEntity item && item.isAlive() && !item.getItem().isEmpty()) {
                item.setNoGravity(entry.getValue());
                item.setDeltaMovement(Vec3.ZERO);
                item.setNoPickUpDelay();
                item.hurtMarked = true;
            }
            iterator.remove();
        }
    }

    private void captureNearbyItems() {
        AABB searchBox = new AABB(this.position(), this.position()).inflate(ITEM_RETRIEVAL_SEARCH_RADIUS);
        List<ItemEntity> nearbyItems = this.level().getEntitiesOfClass(
                ItemEntity.class,
                searchBox,
                item -> item.isAlive() && !item.getItem().isEmpty());
        for (ItemEntity item : nearbyItems) {
            this.retrievedItemGravityStates.put(item.getUUID(), item.isNoGravity());
            item.setNoGravity(true);
            item.setPickUpDelay(20);
            item.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void moveRetrievedItemsWithHook() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Iterator<Map.Entry<UUID, Boolean>> iterator = this.retrievedItemGravityStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Boolean> entry = iterator.next();
            Entity entity = serverLevel.getEntity(entry.getKey());
            if (!(entity instanceof ItemEntity item) || !item.isAlive() || item.getItem().isEmpty()) {
                iterator.remove();
                continue;
            }

            item.setNoGravity(true);
            item.setPickUpDelay(20);
            item.setDeltaMovement(Vec3.ZERO);
            item.setPos(this.getX(), this.getY(), this.getZ());
            item.hurtMarked = true;
        }
    }

    private void releaseRetrievedItems(ServerPlayer player, boolean attemptPickup) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 releasePosition = player != null
                ? player.position().add(0.0D, 0.25D, 0.0D)
                : this.position();
        for (Map.Entry<UUID, Boolean> entry : this.retrievedItemGravityStates.entrySet()) {
            Entity entity = serverLevel.getEntity(entry.getKey());
            if (!(entity instanceof ItemEntity item) || !item.isAlive() || item.getItem().isEmpty()) {
                continue;
            }

            item.setNoGravity(entry.getValue());
            item.setDeltaMovement(Vec3.ZERO);
            item.setPos(releasePosition.x, releasePosition.y, releasePosition.z);
            item.setNoPickUpDelay();
            item.hurtMarked = true;
            if (attemptPickup && player != null) {
                item.playerTouch(player);
            }
        }
        this.retrievedItemGravityStates.clear();
        this.attachedRetrievedItemIds.clear();
        this.itemRetrievalAttracting = false;
        this.itemRetrievalAttractionTicks = 0;
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

        if (!this.isWallTopTerrainTarget() && this.shouldSnapToTarget() && this.terrainTarget != null) {
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
            if (!this.level().isClientSide && this.getUseMode() != UseMode.ITEM_RETRIEVAL) {
                this.startEntityHookChoice();
            }
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        this.hooked = true;
        this.hookType = HookType.GROUND;
        this.swingAnchor = result.getLocation();
        this.swingAnchorBlock = result.getBlockPos();
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
        this.clearWallTopTarget();
        Entity owner = this.getOwner();
        if (owner == null) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] using raw hit target reason=no_owner hit={}",
                        result.getLocation());
            }
            return result.getLocation();
        }

        WallTopTarget retarget = this.getFacingWallRetarget(result, owner);
        if (retarget != null) {
            this.wallTopApproachTarget = retarget.approachTarget();
            this.wallTopSupportBlock = retarget.supportBlock();
            this.wallTopSupportSurfaceY = retarget.supportSurfaceY();
            return retarget.landingTarget();
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

    private WallTopTarget getFacingWallRetarget(BlockHitResult result, Entity owner) {
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
        WallTopTarget retarget = Config.wallHookFacingRetargetAdaptiveEnabled
                ? this.findAdaptiveFacingWallRetarget(owner, base, result, facingDot)
                : null;
        if (retarget == null) {
            retargetSource = Config.wallHookFacingRetargetAdaptiveEnabled ? "fallback_after_adaptive" : "fallback_adaptive_disabled";
            Vec3 fallback = base.add(0.0D, Config.wallHookFacingRetargetUpOffset, 0.0D);
            retarget = this.createWallTopTarget(owner, result, fallback.y);
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
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] applied wall retarget owner={} direction={} facingDot={} requiredDot={} allowedAngleWidthDegrees={} halfAngleDegrees={} hit={} base={} approachTarget={} landingTarget={} supportBlock={} supportSurfaceY={} source={} upOffset={} forwardOffset={} ownerPos={} ownerVelocity={}",
                    owner.getId(),
                    hitDirection,
                    facingDot,
                    Config.wallHookFacingRetargetDot,
                    Config.wallHookFacingRetargetAngleWidthDegrees,
                    Config.wallHookFacingRetargetHalfAngleDegrees,
                    result.getLocation(),
                    base,
                    retarget.approachTarget(),
                    retarget.landingTarget(),
                    retarget.supportBlock(),
                    retarget.supportSurfaceY(),
                    retargetSource,
                    Config.wallHookFacingRetargetUpOffset,
                    Config.wallHookFacingRetargetForwardOffset,
                    owner.position(),
                    owner.getDeltaMovement());
        }
        return retarget;
    }

    private WallTopTarget findAdaptiveFacingWallRetarget(Entity owner, Vec3 base, BlockHitResult result, double facingDot) {
        double minOffset = Math.min(Config.wallHookFacingRetargetMinUpOffset, Config.wallHookFacingRetargetUpOffset);
        double maxOffset = Math.max(Config.wallHookFacingRetargetMinUpOffset, Config.wallHookFacingRetargetUpOffset);
        double step = Math.max(0.05D, Config.wallHookFacingRetargetSearchStep);

        for (double upOffset = minOffset; upOffset <= maxOffset + 1.0E-6D; upOffset += step) {
            Vec3 target = base.add(0.0D, upOffset, 0.0D);
            WallTopTarget candidate = this.createWallTopTarget(owner, result, target.y);
            if (candidate == null) {
                if (Config.debugLogging) {
                    Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] adaptive candidate rejected owner={} reason=no_stable_wall_top_target direction={} facingDot={} hit={} target={} upOffset={}",
                            owner.getId(),
                            result.getDirection(),
                            facingDot,
                            result.getLocation(),
                            target,
                            upOffset);
                }
                continue;
            }

            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRetargetDebug][SERVER] adaptive wall retarget found owner={} direction={} facingDot={} hit={} approachTarget={} landingTarget={} supportBlock={} supportSurfaceY={} upOffset={} minOffset={} maxOffset={} step={} ownerPos={}",
                        owner.getId(),
                        result.getDirection(),
                        facingDot,
                        result.getLocation(),
                        candidate.approachTarget(),
                        candidate.landingTarget(),
                        candidate.supportBlock(),
                        candidate.supportSurfaceY(),
                        upOffset,
                        minOffset,
                        maxOffset,
                        step,
                        owner.position());
            }
            return candidate;
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

    private WallTopTarget createWallTopTarget(Entity owner, BlockHitResult result, double candidateY) {
        BlockPos hitBlock = result.getBlockPos();
        int feetBlockY = Mth.floor(candidateY);
        if (feetBlockY < hitBlock.getY()) {
            return null;
        }

        BlockPos supportBlock = new BlockPos(hitBlock.getX(), feetBlockY - 1, hitBlock.getZ());
        Level level = this.level();
        VoxelShape supportShape = level.getBlockState(supportBlock).getCollisionShape(level, supportBlock);
        if (supportShape.isEmpty()) {
            return null;
        }

        double minX = supportBlock.getX() + supportShape.min(Direction.Axis.X);
        double maxX = supportBlock.getX() + supportShape.max(Direction.Axis.X);
        double minZ = supportBlock.getZ() + supportShape.min(Direction.Axis.Z);
        double maxZ = supportBlock.getZ() + supportShape.max(Direction.Axis.Z);
        double supportSurfaceY = supportBlock.getY() + supportShape.max(Direction.Axis.Y);
        double halfWidth = owner.getBbWidth() * 0.5D + WALL_TOP_EDGE_GAP;
        if (maxX - minX < halfWidth * 2.0D || maxZ - minZ < halfWidth * 2.0D) {
            return null;
        }

        Vec3 hit = result.getLocation();
        double landingX = Mth.clamp(hit.x, minX + halfWidth, maxX - halfWidth);
        double landingZ = Mth.clamp(hit.z, minZ + halfWidth, maxZ - halfWidth);
        Direction direction = result.getDirection();
        switch (direction) {
            case EAST -> landingX = maxX - halfWidth;
            case WEST -> landingX = minX + halfWidth;
            case SOUTH -> landingZ = maxZ - halfWidth;
            case NORTH -> landingZ = minZ + halfWidth;
            default -> {
                return null;
            }
        }

        double landingY = supportSurfaceY + WALL_TOP_SURFACE_EPSILON;
        Vec3 landingTarget = new Vec3(landingX, landingY, landingZ);
        double outsideDistance = Math.max(
                Config.wallHookFacingRetargetForwardOffset,
                owner.getBbWidth() * 0.5D + WALL_TOP_EDGE_GAP);
        double approachY = supportSurfaceY + WALL_TOP_APPROACH_CLEARANCE;
        Vec3 approachTarget = switch (direction) {
            case EAST -> new Vec3(maxX + outsideDistance, approachY, landingZ);
            case WEST -> new Vec3(minX - outsideDistance, approachY, landingZ);
            case SOUTH -> new Vec3(landingX, approachY, maxZ + outsideDistance);
            case NORTH -> new Vec3(landingX, approachY, minZ - outsideDistance);
            default -> landingTarget;
        };

        if (!this.isCollisionFreeAt(owner, approachTarget) || !this.isCollisionFreeAt(owner, landingTarget)) {
            return null;
        }

        return new WallTopTarget(approachTarget, landingTarget, supportBlock.immutable(), supportSurfaceY);
    }

    private boolean isCollisionFreeAt(Entity owner, Vec3 target) {
        Vec3 offset = target.subtract(owner.position());
        return this.level().noCollision(owner, owner.getBoundingBox().move(offset));
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

    public GrapplingHookVariant getVariant() {
        return GrapplingHookVariant.fromId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(GrapplingHookVariant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public void configureUse(int sequenceId, boolean phantomPending, int ropeExtensionLevel) {
        this.useSequenceId = sequenceId;
        this.setRopeExtensionLevel(ropeExtensionLevel);
        this.setUseMode(phantomPending ? UseMode.PHANTOM_PENDING : UseMode.NORMAL);
    }

    public boolean enableItemRetrieval() {
        if (this.useFinished
                || this.isRemoved()
                || this.isSwinging()
                || this.getUseMode() == UseMode.PHANTOM_SWING) {
            return false;
        }
        this.setUseMode(UseMode.ITEM_RETRIEVAL);
        return true;
    }

    public boolean requestEntityPullToOwner() {
        if (this.useFinished
                || this.isRemoved()
                || this.getUseMode() == UseMode.ITEM_RETRIEVAL
                || this.isSwinging()
                || this.hookType != HookType.ENTITY
                || !this.entityHookChoiceStarted
                || this.entityHookPullExecuted
                || this.entityOwnerPullStartLife >= 0
                || this.entityTargetPullStartLife >= 0
                || this.life > this.entityHookChoiceDeadlineLife) {
            return false;
        }
        this.entityPullToOwnerRequested = true;
        return true;
    }

    public void setGrapplingShieldDisarmEnabled(boolean enabled) {
        this.grapplingShieldDisarmEnabled = enabled;
    }

    public void setRetrievalReturnSpeed(double speed) {
        if (Double.isFinite(speed) && speed > 0.0D) {
            this.retrievalReturnSpeed = speed;
        }
    }

    private boolean isItemRetrievalReturning() {
        return this.entityData.get(DATA_ITEM_RETRIEVAL_RETURNING);
    }

    public int getRopeExtensionLevel() {
        return this.entityData.get(DATA_ROPE_EXTENSION_LEVEL);
    }

    public void setRopeExtensionLevel(int level) {
        this.entityData.set(DATA_ROPE_EXTENSION_LEVEL, Mth.clamp(level, 0, 3));
    }

    public int getRopeExtensionBonusBlocks() {
        return RopeExtensionEnchantment.getBonusBlocks(this.getRopeExtensionLevel());
    }

    private double getPhantomLaunchRange() {
        return Config.PHANTOM_HOOK_LAUNCH_RANGE + this.getRopeExtensionBonusBlocks();
    }

    private double getPhantomSwingTargetRopeLength() {
        return Config.PHANTOM_SWING_TARGET_ROPE_LENGTH + this.getRopeExtensionBonusBlocks();
    }

    public void captureSwingForwardDirectionFromVelocity() {
        Vec3 velocity = this.getDeltaMovement();
        Vec3 horizontal = new Vec3(velocity.x, 0.0D, velocity.z);
        if (horizontal.lengthSqr() >= 1.0E-8D) {
            this.swingForwardDirection = horizontal.normalize();
        }
    }

    public int getUseSequenceId() {
        return this.useSequenceId;
    }

    public UseMode getUseMode() {
        return UseMode.fromId(this.entityData.get(DATA_USE_MODE));
    }

    private void setUseMode(UseMode useMode) {
        this.entityData.set(DATA_USE_MODE, useMode.ordinal());
    }

    public boolean isSwinging() {
        return this.entityData.get(DATA_SWINGING);
    }

    public boolean isEntityHookChoiceWaiting() {
        return this.entityData.get(DATA_ENTITY_HOOK_CHOICE_WAITING);
    }

    public static GrapplingHook findActiveEntityChoiceHook(Entity owner) {
        if (owner == null || owner.level() == null) {
            return null;
        }

        return owner.level().getEntitiesOfClass(
                        GrapplingHook.class,
                        owner.getBoundingBox().inflate(32.0D),
                        hook -> hook.isEntityHookChoiceWaiting() && hook.getOwner() == owner)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static GrapplingHook findActiveSwingHook(Entity owner) {
        if (owner == null || owner.level() == null) {
            return null;
        }

        return owner.level().getEntitiesOfClass(
                        GrapplingHook.class,
                        owner.getBoundingBox().inflate(32.0D),
                        hook -> hook.isSwinging() && hook.getOwner() == owner)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public float getSwingAnimationSpeedMultiplier(float animationTotalTimeSeconds, int reservedPhaseTicks) {
        int phaseDurationTicks = Math.max(
                SWING_PHASE_MIN_DURATION_TICKS,
                this.entityData.get(DATA_SWING_PHASE_DURATION_TICKS));
        int animationPlaybackTicks = Math.max(1, phaseDurationTicks - Math.max(0, reservedPhaseTicks));
        float animationDurationTicks = Math.max(1.0F, animationTotalTimeSeconds * 20.0F);
        return Mth.clamp(animationDurationTicks / animationPlaybackTicks, 0.25F, 3.0F);
    }

    private record WallTopTarget(
            Vec3 approachTarget,
            Vec3 landingTarget,
            BlockPos supportBlock,
            double supportSurfaceY) {
    }

    public enum HookType {
        AIR,
        GROUND,
        ENTITY,
        MISSED
    }

    public enum UseMode {
        NORMAL,
        PHANTOM_PENDING,
        PHANTOM_SWING,
        ITEM_RETRIEVAL;

        private static UseMode fromId(int id) {
            UseMode[] values = values();
            return id >= 0 && id < values.length ? values[id] : NORMAL;
        }
    }

}
