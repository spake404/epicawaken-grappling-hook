package org.com.epicawaken_grappling_hook;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.com.epicawaken_grappling_hook.util.GrapplingHookDebugMode;

@Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final int BASE_HOOK_LOCK_DELAY_TICKS = 11;
    private static final int MIN_MISSED_HOOK_VISUAL_TICKS = 18;
    private static final double BASE_PROJECTILE_SPEED = 2.0D;
    public static final double PHANTOM_SWING_MIN_ROPE_LENGTH = 5.0D;
    public static final double PHANTOM_HOOK_LAUNCH_RANGE = 15.0D;
    public static final double PHANTOM_SWING_TARGET_ROPE_LENGTH = 10.0D;

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue GRAPPLING_HOOK_COOLDOWN;
    private static final ForgeConfigSpec.DoubleValue HOOK_PULL_ANIMATION_SPEED;
    private static final ForgeConfigSpec.DoubleValue PROJECTILE_INACCURACY;
    private static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;
    private static final ForgeConfigSpec.BooleanValue DISABLE_PARCOOL_CRAWL_AND_SLIDE_DURING_HOOK;
    private static final ForgeConfigSpec.IntValue MISSED_HOOK_GROUND_ANIMATION_DURATION_TICKS;

    private static final ForgeConfigSpec.DoubleValue GROUND_HOOK_PULL_STRENGTH;
    private static final ForgeConfigSpec.BooleanValue GROUND_HOOK_SLIDE_ENABLED;
    private static final ForgeConfigSpec.IntValue GROUND_HOOK_SLIDE_DURATION_TICKS;
    private static final ForgeConfigSpec.DoubleValue GROUND_HOOK_SLIDE_START_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue GROUND_HOOK_SLIDE_INITIAL_SPEED;
    private static final ForgeConfigSpec.DoubleValue GROUND_HOOK_SLIDE_FRICTION;
    private static final ForgeConfigSpec.DoubleValue GROUND_HOOK_SLIDE_MIN_SPEED;
    private static final ForgeConfigSpec.IntValue GROUND_HOOK_TARGET_PULL_DURATION_TICKS;
    private static final ForgeConfigSpec.DoubleValue GROUND_HOOK_TARGET_PULL_MIN_SPEED;
    private static final ForgeConfigSpec.DoubleValue GROUND_HOOK_TARGET_PULL_MAX_SPEED;
    private static final ForgeConfigSpec.DoubleValue GROUND_HOOK_TARGET_PULL_ARRIVAL_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue GROUND_HOOK_TARGET_PULL_SNAP_TO_TARGET;

    private static final ForgeConfigSpec.DoubleValue AIR_PULL_STRENGTH;
    private static final ForgeConfigSpec.IntValue AIR_HOOK_TARGET_PULL_DURATION_TICKS;
    private static final ForgeConfigSpec.DoubleValue AIR_HOOK_TARGET_PULL_MIN_SPEED;
    private static final ForgeConfigSpec.DoubleValue AIR_HOOK_TARGET_PULL_MAX_SPEED;
    private static final ForgeConfigSpec.DoubleValue AIR_HOOK_TARGET_PULL_ARRIVAL_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue AIR_HOOK_TARGET_PULL_SNAP_TO_TARGET;
    private static final ForgeConfigSpec.IntValue AIR_HOOK_FOV_HOLD_TAIL_TICKS;

    private static final ForgeConfigSpec.BooleanValue WALL_HOOK_FACING_RETARGET_ENABLED;
    private static final ForgeConfigSpec.BooleanValue WALL_HOOK_FACING_RETARGET_ADAPTIVE_ENABLED;
    private static final ForgeConfigSpec.DoubleValue WALL_HOOK_FACING_RETARGET_ANGLE_WIDTH_DEGREES;
    private static final ForgeConfigSpec.DoubleValue WALL_HOOK_FACING_RETARGET_MIN_UP_OFFSET;
    private static final ForgeConfigSpec.DoubleValue WALL_HOOK_FACING_RETARGET_UP_OFFSET;
    private static final ForgeConfigSpec.DoubleValue WALL_HOOK_FACING_RETARGET_SEARCH_STEP;
    private static final ForgeConfigSpec.DoubleValue WALL_HOOK_FACING_RETARGET_FORWARD_OFFSET;

    private static final ForgeConfigSpec.DoubleValue ENTITY_PULL_STRENGTH_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue ENTITY_PULL_UP_BOOST;
    private static final ForgeConfigSpec.BooleanValue RESPECT_KNOCKBACK_RESISTANCE;

    private static final ForgeConfigSpec.IntValue PHANTOM_SWING_HOLD_THRESHOLD_TICKS;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_MISSED_AIR_HORIZONTAL_PULL_STRENGTH;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_MISSED_AIR_MAX_HORIZONTAL_SPEED;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_MISSED_AIR_BASE_UP_SPEED;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_MISSED_AIR_HEIGHT_UP_SCALE;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_MISSED_AIR_MAX_UP_SPEED;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_MISSED_AIR_MAX_TOTAL_SPEED;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_REEL_IN_SPEED;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_INITIAL_BOOST;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_CONSTRAINT_STRENGTH;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_MAX_CORRECTION_SPEED;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_MAX_SPEED;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_PREHOLD_FALL_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_PREHOLD_MAX_FALL_SPEED;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_ENERGY_RETENTION;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_ENERGY_GAIN_PER_TICK;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_ENERGY_CAP_RATIO;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_MAX_ANGLE_DEGREES;
    private static final ForgeConfigSpec.DoubleValue PHANTOM_SWING_PER_SWING_ENERGY_MULTIPLIER;

    static {
        BUILDER.push("common");
        GRAPPLING_HOOK_COOLDOWN = BUILDER
                .comment("Cooldown in ticks after using the normal grappling hook. The Phantom Grappling Hook has no cooldown.")
                .translation("config.epicawaken_grappling_hook.grapplingHookCooldown")
                .defineInRange("grapplingHookCooldown", 30, 0, 1200);
        HOOK_PULL_ANIMATION_SPEED = BUILDER
                .comment("Main speed multiplier for the Curios grappling hook. This scales hook_pull animation speed, hook timing, and projectile speed.")
                .translation("config.epicawaken_grappling_hook.hookPullAnimationSpeed")
                .defineInRange("hookPullAnimationSpeed", 1.0D, 0.25D, 3.0D);
        PROJECTILE_INACCURACY = BUILDER
                .comment("Projectile inaccuracy for the grappling hook fired by hook_pull. Awaken default is 1.0. Lower values are more accurate.")
                .translation("config.epicawaken_grappling_hook.projectileInaccuracy")
                .defineInRange("projectileInaccuracy", 1.0D, 0.0D, 5.0D);
        if (GrapplingHookDebugMode.isEnabled()) {
            DEBUG_LOGGING = BUILDER
                    .comment("Enables verbose grappling hook debug logs in a Forge development environment.")
                    .translation("config.epicawaken_grappling_hook.debugLogging")
                    .define("debugLogging", false);
        } else {
            DEBUG_LOGGING = null;
        }
        DISABLE_PARCOOL_CRAWL_AND_SLIDE_DURING_HOOK = BUILDER
                .comment("Prevents Parcool crawl and slide while grappling hook animations, pulls, or ground slides are active.")
                .translation("config.epicawaken_grappling_hook.disableParcoolCrawlAndSlideDuringHook")
                .define("disableParcoolCrawlAndSlideDuringHook", true);
        MISSED_HOOK_GROUND_ANIMATION_DURATION_TICKS = BUILDER
                .comment("How many server ticks the missed-hook projectile stays around after an empty hook locks. No pull is applied.")
                .translation("config.epicawaken_grappling_hook.missedHookGroundAnimationDurationTicks")
                .defineInRange("missedHookGroundAnimationDurationTicks", 10, 1, 80);
        BUILDER.pop();

        BUILDER.push("ground_hook");
        GROUND_HOOK_PULL_STRENGTH = BUILDER
                .comment("Fallback one-shot pull strength for HOOK_GROUND when no safe terrain target is available.")
                .translation("config.epicawaken_grappling_hook.groundHookPullStrength")
                .defineInRange("groundHookPullStrength", 0.08D, 0.01D, 1.0D);
        GROUND_HOOK_SLIDE_ENABLED = BUILDER
                .comment("Whether HOOK_GROUND starts a short high-speed slide after the player reaches the hooked ground target.")
                .translation("config.epicawaken_grappling_hook.groundHookSlideEnabled")
                .define("groundHookSlideEnabled", true);
        GROUND_HOOK_SLIDE_DURATION_TICKS = BUILDER
                .comment("Maximum duration in ticks for the slide after HOOK_GROUND reaches its target.")
                .translation("config.epicawaken_grappling_hook.groundHookSlideDurationTicks")
                .defineInRange("groundHookSlideDurationTicks", 12, 1, 60);
        GROUND_HOOK_SLIDE_START_DISTANCE = BUILDER
                .comment("HOOK_GROUND starts sliding when the player is within this distance from the hooked ground target. This avoids braking against the exact target point.")
                .translation("config.epicawaken_grappling_hook.groundHookSlideStartDistance")
                .defineInRange("groundHookSlideStartDistance", 0.6D, 0.0D, 3.0D);
        GROUND_HOOK_SLIDE_INITIAL_SPEED = BUILDER
                .comment("Minimum horizontal speed used when the HOOK_GROUND slide starts.")
                .translation("config.epicawaken_grappling_hook.groundHookSlideInitialSpeed")
                .defineInRange("groundHookSlideInitialSpeed", 0.9D, 0.05D, 3.0D);
        GROUND_HOOK_SLIDE_FRICTION = BUILDER
                .comment("Per-tick horizontal speed multiplier during the HOOK_GROUND slide. Higher values slide farther.")
                .translation("config.epicawaken_grappling_hook.groundHookSlideFriction")
                .defineInRange("groundHookSlideFriction", 0.88D, 0.1D, 1.0D);
        GROUND_HOOK_SLIDE_MIN_SPEED = BUILDER
                .comment("The HOOK_GROUND slide ends early when horizontal speed drops below this value.")
                .translation("config.epicawaken_grappling_hook.groundHookSlideMinSpeed")
                .defineInRange("groundHookSlideMinSpeed", 0.15D, 0.0D, 1.0D);
        GROUND_HOOK_TARGET_PULL_DURATION_TICKS = BUILDER
                .comment("Maximum duration in ticks for HOOK_GROUND targeted terrain pulling.")
                .translation("config.epicawaken_grappling_hook.groundHookTargetPullDurationTicks")
                .defineInRange("groundHookTargetPullDurationTicks", 24, 1, 100);
        GROUND_HOOK_TARGET_PULL_MIN_SPEED = BUILDER
                .comment("Minimum velocity per tick while HOOK_GROUND pulls the player toward the hooked ground target.")
                .translation("config.epicawaken_grappling_hook.groundHookTargetPullMinSpeed")
                .defineInRange("groundHookTargetPullMinSpeed", 0.8D, 0.01D, 1.0D);
        GROUND_HOOK_TARGET_PULL_MAX_SPEED = BUILDER
                .comment("Maximum velocity per tick while HOOK_GROUND pulls the player toward the hooked ground target.")
                .translation("config.epicawaken_grappling_hook.groundHookTargetPullMaxSpeed")
                .defineInRange("groundHookTargetPullMaxSpeed", 1.2D, 0.05D, 5.0D);
        GROUND_HOOK_TARGET_PULL_ARRIVAL_DISTANCE = BUILDER
                .comment("Additional arrival radius for HOOK_GROUND. The built-in collision-safe contact tolerance still applies when this is lower.")
                .translation("config.epicawaken_grappling_hook.groundHookTargetPullArrivalDistance")
                .defineInRange("groundHookTargetPullArrivalDistance", 0.0D, 0.0D, 3.0D);
        GROUND_HOOK_TARGET_PULL_SNAP_TO_TARGET = BUILDER
                .comment("Whether the player is snapped to the safe ground target if the ground pull reaches its time limit.")
                .translation("config.epicawaken_grappling_hook.groundHookTargetPullSnapToTarget")
                .define("groundHookTargetPullSnapToTarget", true);
        BUILDER.pop();

        BUILDER.push("air_hook");
        AIR_PULL_STRENGTH = BUILDER
                .comment("Fallback pull strength applied to the player when HOOK_AIR has no fixed terrain target.")
                .translation("config.epicawaken_grappling_hook.airPullStrength")
                .defineInRange("airPullStrength", 0.2D, 0.01D, 1.5D);
        AIR_HOOK_TARGET_PULL_DURATION_TICKS = BUILDER
                .comment("Maximum duration in ticks for targeted high-terrain grappling after HOOK_AIR latches onto terrain.")
                .translation("config.epicawaken_grappling_hook.airHookTargetPullDurationTicks")
                .defineInRange("airHookTargetPullDurationTicks", 24, 1, 100);
        AIR_HOOK_TARGET_PULL_MIN_SPEED = BUILDER
                .comment("Minimum velocity per tick while HOOK_AIR pulls the player toward a hooked high-terrain target.")
                .translation("config.epicawaken_grappling_hook.airHookTargetPullMinSpeed")
                .defineInRange("airHookTargetPullMinSpeed", 0.08D, 0.01D, 1.0D);
        AIR_HOOK_TARGET_PULL_MAX_SPEED = BUILDER
                .comment("Maximum velocity per tick while HOOK_AIR pulls the player toward a hooked high-terrain target.")
                .translation("config.epicawaken_grappling_hook.airHookTargetPullMaxSpeed")
                .defineInRange("airHookTargetPullMaxSpeed", 1.2D, 0.05D, 5.0D);
        AIR_HOOK_TARGET_PULL_ARRIVAL_DISTANCE = BUILDER
                .comment("Additional arrival radius for HOOK_AIR terrain pulling. The built-in collision-safe contact tolerance still applies when this is lower.")
                .translation("config.epicawaken_grappling_hook.airHookTargetPullArrivalDistance")
                .defineInRange("airHookTargetPullArrivalDistance", 0.0D, 0.0D, 3.0D);
        AIR_HOOK_TARGET_PULL_SNAP_TO_TARGET = BUILDER
                .comment("Whether the player is snapped to the safe high-terrain target if the pull reaches its time limit.")
                .translation("config.epicawaken_grappling_hook.airHookTargetPullSnapToTarget")
                .define("airHookTargetPullSnapToTarget", true);
        AIR_HOOK_FOV_HOLD_TAIL_TICKS = BUILDER
                .comment("Client-side ticks to keep the captured pre-hook FOV after HOOK_AIR pull ends. This smooths transitions into wall-running.")
                .translation("config.epicawaken_grappling_hook.airHookFovHoldTailTicks")
                .defineInRange("airHookFovHoldTailTicks", 24, 0, 80);
        BUILDER.pop();

        BUILDER.push("wall_hook");
        WALL_HOOK_FACING_RETARGET_ENABLED = BUILDER
                .comment("Whether a wall hook retargets to a point above the impact when the player is facing the wall.")
                .translation("config.epicawaken_grappling_hook.wallHookFacingRetargetEnabled")
                .define("wallHookFacingRetargetEnabled", true);
        WALL_HOOK_FACING_RETARGET_ADAPTIVE_ENABLED = BUILDER
                .comment("Whether facing-wall hook retargeting searches for the nearest collision-free point between the minimum and maximum up offsets.")
                .translation("config.epicawaken_grappling_hook.wallHookFacingRetargetAdaptiveEnabled")
                .define("wallHookFacingRetargetAdaptiveEnabled", true);
        WALL_HOOK_FACING_RETARGET_ANGLE_WIDTH_DEGREES = BUILDER
                .comment("Total horizontal angle width in degrees for facing-wall retargeting. For example, 120 means 60 degrees left and 60 degrees right from directly facing the wall.")
                .translation("config.epicawaken_grappling_hook.wallHookFacingRetargetAngleWidthDegrees")
                .defineInRange("wallHookFacingRetargetAngleWidthDegrees", 132.0D, 0.0D, 180.0D);
        WALL_HOOK_FACING_RETARGET_MIN_UP_OFFSET = BUILDER
                .comment("Minimum vertical offset in blocks used by adaptive facing-wall hook retargeting.")
                .translation("config.epicawaken_grappling_hook.wallHookFacingRetargetMinUpOffset")
                .defineInRange("wallHookFacingRetargetMinUpOffset", 0.5D, 0.0D, 8.0D);
        WALL_HOOK_FACING_RETARGET_UP_OFFSET = BUILDER
                .comment("Maximum vertical offset in blocks for adaptive facing-wall hook retargeting. Also used as the fallback fixed offset.")
                .translation("config.epicawaken_grappling_hook.wallHookFacingRetargetUpOffset")
                .defineInRange("wallHookFacingRetargetUpOffset", 3.0D, 0.0D, 8.0D);
        WALL_HOOK_FACING_RETARGET_SEARCH_STEP = BUILDER
                .comment("Vertical step in blocks used while adaptive facing-wall hook retargeting searches for the nearest collision-free point.")
                .translation("config.epicawaken_grappling_hook.wallHookFacingRetargetSearchStep")
                .defineInRange("wallHookFacingRetargetSearchStep", 0.25D, 0.05D, 2.0D);
        WALL_HOOK_FACING_RETARGET_FORWARD_OFFSET = BUILDER
                .comment("Outward offset from the wall surface in blocks for the facing-wall retarget point.")
                .translation("config.epicawaken_grappling_hook.wallHookFacingRetargetForwardOffset")
                .defineInRange("wallHookFacingRetargetForwardOffset", 0.45D, 0.0D, 2.0D);
        BUILDER.pop();

        BUILDER.push("entity_hook");
        ENTITY_PULL_STRENGTH_MULTIPLIER = BUILDER
                .comment("Multiplier for pulling hooked entities toward the player. This is independent from hookPullAnimationSpeed.")
                .translation("config.epicawaken_grappling_hook.entityPullStrengthMultiplier")
                .defineInRange("entityPullStrengthMultiplier", 1.0D, 0.1D, 5.0D);
        ENTITY_PULL_UP_BOOST = BUILDER
                .comment("Extra upward velocity added to hooked entities when they are pulled.")
                .translation("config.epicawaken_grappling_hook.entityPullUpBoost")
                .defineInRange("entityPullUpBoost", 0.25D, 0.0D, 1.5D);
        RESPECT_KNOCKBACK_RESISTANCE = BUILDER
                .comment("Whether hooked entities reduce pull strength with their knockback resistance.")
                .translation("config.epicawaken_grappling_hook.respectKnockbackResistance")
                .define("respectKnockbackResistance", true);
        BUILDER.pop();

        BUILDER.push("phantom_swing");
        PHANTOM_SWING_HOLD_THRESHOLD_TICKS = BUILDER
                .comment("How many ticks the grappling hook key must remain held before a Phantom Grappling Hook switches from normal use to swinging.")
                .translation("config.epicawaken_grappling_hook.phantomSwingHoldThresholdTicks")
                .defineInRange("phantomSwingHoldThresholdTicks", 7, 2, 20);
        PHANTOM_MISSED_AIR_HORIZONTAL_PULL_STRENGTH = BUILDER
                .comment("Horizontal pull strength used when a short-pressed Phantom Grappling Hook anchors in empty air.")
                .translation("config.epicawaken_grappling_hook.phantomMissedAirHorizontalPullStrength")
                .defineInRange("phantomMissedAirHorizontalPullStrength", 0.24D, 0.0D, 2.0D);
        PHANTOM_MISSED_AIR_MAX_HORIZONTAL_SPEED = BUILDER
                .comment("Maximum horizontal speed after a Phantom Grappling Hook short-press air pull.")
                .translation("config.epicawaken_grappling_hook.phantomMissedAirMaxHorizontalSpeed")
                .defineInRange("phantomMissedAirMaxHorizontalSpeed", 2.2D, 0.1D, 8.0D);
        PHANTOM_MISSED_AIR_BASE_UP_SPEED = BUILDER
                .comment("Base upward speed for a Phantom Grappling Hook short-press air pull.")
                .translation("config.epicawaken_grappling_hook.phantomMissedAirBaseUpSpeed")
                .defineInRange("phantomMissedAirBaseUpSpeed", 0.9D, 0.0D, 4.0D);
        PHANTOM_MISSED_AIR_HEIGHT_UP_SCALE = BUILDER
                .comment("Additional upward speed per block that the empty-air anchor is above the player.")
                .translation("config.epicawaken_grappling_hook.phantomMissedAirHeightUpScale")
                .defineInRange("phantomMissedAirHeightUpScale", 0.08D, 0.0D, 1.0D);
        PHANTOM_MISSED_AIR_MAX_UP_SPEED = BUILDER
                .comment("Maximum upward speed after a Phantom Grappling Hook short-press air pull.")
                .translation("config.epicawaken_grappling_hook.phantomMissedAirMaxUpSpeed")
                .defineInRange("phantomMissedAirMaxUpSpeed", 1.65D, 0.1D, 6.0D);
        PHANTOM_MISSED_AIR_MAX_TOTAL_SPEED = BUILDER
                .comment("Maximum combined speed after a Phantom Grappling Hook short-press air pull.")
                .translation("config.epicawaken_grappling_hook.phantomMissedAirMaxTotalSpeed")
                .defineInRange("phantomMissedAirMaxTotalSpeed", 2.8D, 0.1D, 10.0D);
        PHANTOM_SWING_REEL_IN_SPEED = BUILDER
                .comment("Rope length removed per tick while a Phantom Grappling Hook reels in after entering swing mode.")
                .translation("config.epicawaken_grappling_hook.phantomSwingReelInSpeed")
                .defineInRange("phantomSwingReelInSpeed", 0.7D, 0.0D, 4.0D);
        PHANTOM_SWING_INITIAL_BOOST = BUILDER
                .comment("Initial tangential speed added when a Phantom Grappling Hook begins swinging.")
                .translation("config.epicawaken_grappling_hook.phantomSwingInitialBoost")
                .defineInRange("phantomSwingInitialBoost", 1.82D, 0.0D, 3.0D);
        PHANTOM_SWING_CONSTRAINT_STRENGTH = BUILDER
                .comment("Strength used to pull a swinging player back inside the rope radius.")
                .translation("config.epicawaken_grappling_hook.phantomSwingConstraintStrength")
                .defineInRange("phantomSwingConstraintStrength", 0.35D, 0.05D, 1.5D);
        PHANTOM_SWING_MAX_CORRECTION_SPEED = BUILDER
                .comment("Maximum inward correction speed applied by the Phantom Grappling Hook rope constraint.")
                .translation("config.epicawaken_grappling_hook.phantomSwingMaxCorrectionSpeed")
                .defineInRange("phantomSwingMaxCorrectionSpeed", 0.7D, 0.05D, 2.0D);
        PHANTOM_SWING_MAX_SPEED = BUILDER
                .comment("Maximum total player speed while swinging from a Phantom Grappling Hook.")
                .translation("config.epicawaken_grappling_hook.phantomSwingMaxSpeed")
                .defineInRange("phantomSwingMaxSpeed", 4.68D, 0.2D, 5.0D);
        PHANTOM_SWING_PREHOLD_FALL_MULTIPLIER = BUILDER
                .comment("Multiplier applied to downward velocity while holding a Phantom Grappling Hook before swing mode starts.")
                .translation("config.epicawaken_grappling_hook.phantomSwingPreholdFallMultiplier")
                .defineInRange("phantomSwingPreholdFallMultiplier", 0.65D, 0.1D, 1.0D);
        PHANTOM_SWING_PREHOLD_MAX_FALL_SPEED = BUILDER
                .comment("Maximum downward speed while holding a Phantom Grappling Hook before swing mode starts.")
                .translation("config.epicawaken_grappling_hook.phantomSwingPreholdMaxFallSpeed")
                .defineInRange("phantomSwingPreholdMaxFallSpeed", 0.12D, 0.01D, 1.0D);
        PHANTOM_SWING_ENERGY_RETENTION = BUILDER
                .comment("Fraction of Phantom Grappling Hook swing energy retained each tick. Lower values add gentle per-tick speed damping.")
                .translation("config.epicawaken_grappling_hook.phantomSwingEnergyRetention")
                .defineInRange("phantomSwingEnergyRetention", 0.995D, 0.8D, 1.0D);
        PHANTOM_SWING_ENERGY_GAIN_PER_TICK = BUILDER
                .comment("Maximum swing energy gained per tick while descending, scaled by how steeply the player is moving downward.")
                .translation("config.epicawaken_grappling_hook.phantomSwingEnergyGainPerTick")
                .defineInRange("phantomSwingEnergyGainPerTick", 0.052D, 0.0D, 0.25D);
        PHANTOM_SWING_ENERGY_CAP_RATIO = BUILDER
                .comment("Maximum fraction of the energy needed to loop over the top of the swing anchor.")
                .translation("config.epicawaken_grappling_hook.phantomSwingEnergyCapRatio")
                .defineInRange("phantomSwingEnergyCapRatio", 0.65D, 0.1D, 1.0D);
        PHANTOM_SWING_MAX_ANGLE_DEGREES = BUILDER
                .comment("Maximum swing angle measured from directly below the anchor. Values below 90 keep the player below the anchor height.")
                .translation("config.epicawaken_grappling_hook.phantomSwingMaxAngleDegrees")
                .defineInRange("phantomSwingMaxAngleDegrees", 95.0D, 45.0D, 120.0D);
        PHANTOM_SWING_PER_SWING_ENERGY_MULTIPLIER = BUILDER
                .comment("Energy multiplier applied each time the Phantom Grappling Hook swing reverses direction.")
                .translation("config.epicawaken_grappling_hook.phantomSwingPerSwingEnergyMultiplier")
                .defineInRange("phantomSwingPerSwingEnergyMultiplier", 0.95D, 0.5D, 1.0D);
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int grapplingHookCooldown;
    public static double hookPullAnimationSpeed;
    public static double projectileInaccuracy;
    public static boolean debugLogging;
    public static boolean disableParcoolCrawlAndSlideDuringHook;
    public static int missedHookGroundAnimationDurationTicks;

    public static double groundHookPullStrength;
    public static boolean groundHookSlideEnabled;
    public static int groundHookSlideDurationTicks;
    public static double groundHookSlideStartDistance;
    public static double groundHookSlideInitialSpeed;
    public static double groundHookSlideFriction;
    public static double groundHookSlideMinSpeed;
    public static int groundHookTargetPullDurationTicks;
    public static double groundHookTargetPullMinSpeed;
    public static double groundHookTargetPullMaxSpeed;
    public static double groundHookTargetPullArrivalDistance;
    public static boolean groundHookTargetPullSnapToTarget;

    public static double airPullStrength;
    public static int airHookTargetPullDurationTicks;
    public static double airHookTargetPullMinSpeed;
    public static double airHookTargetPullMaxSpeed;
    public static double airHookTargetPullArrivalDistance;
    public static boolean airHookTargetPullSnapToTarget;
    public static int airHookFovHoldTailTicks;

    public static boolean wallHookFacingRetargetEnabled;
    public static boolean wallHookFacingRetargetAdaptiveEnabled;
    public static double wallHookFacingRetargetAngleWidthDegrees;
    public static double wallHookFacingRetargetHalfAngleDegrees;
    public static double wallHookFacingRetargetDot;
    public static double wallHookFacingRetargetMinUpOffset;
    public static double wallHookFacingRetargetUpOffset;
    public static double wallHookFacingRetargetSearchStep;
    public static double wallHookFacingRetargetForwardOffset;

    // Hidden prototype module. Kept in code for later experiments, but not exposed in Forge config.
    public static boolean airHookArrivalJumpEnabled = false;
    public static int airHookArrivalJumpWindowTicks = 10;
    public static double airHookArrivalJumpInitialSpeed = 0.65D;
    public static int airHookArrivalJumpDurationTicks = 8;
    public static double airHookArrivalJumpFriction = 0.86D;
    public static double airHookArrivalJumpMinSpeed = 0.12D;

    public static double entityPullStrengthMultiplier;
    public static double entityPullUpBoost;
    public static boolean respectKnockbackResistance;

    public static int phantomSwingHoldThresholdTicks;
    public static double phantomMissedAirHorizontalPullStrength;
    public static double phantomMissedAirMaxHorizontalSpeed;
    public static double phantomMissedAirBaseUpSpeed;
    public static double phantomMissedAirHeightUpScale;
    public static double phantomMissedAirMaxUpSpeed;
    public static double phantomMissedAirMaxTotalSpeed;
    public static double phantomSwingReelInSpeed;
    public static double phantomSwingInitialBoost;
    public static double phantomSwingConstraintStrength;
    public static double phantomSwingMaxCorrectionSpeed;
    public static double phantomSwingMaxSpeed;
    public static double phantomSwingPreholdFallMultiplier;
    public static double phantomSwingPreholdMaxFallSpeed;
    public static double phantomSwingEnergyRetention;
    public static double phantomSwingEnergyGainPerTick;
    public static double phantomSwingEnergyCapRatio;
    public static double phantomSwingMaxAngleDegrees;
    public static double phantomSwingPerSwingEnergyMultiplier;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        grapplingHookCooldown = GRAPPLING_HOOK_COOLDOWN.get();
        hookPullAnimationSpeed = HOOK_PULL_ANIMATION_SPEED.get();
        projectileInaccuracy = PROJECTILE_INACCURACY.get();
        debugLogging = DEBUG_LOGGING != null && DEBUG_LOGGING.get();
        disableParcoolCrawlAndSlideDuringHook = DISABLE_PARCOOL_CRAWL_AND_SLIDE_DURING_HOOK.get();
        missedHookGroundAnimationDurationTicks = MISSED_HOOK_GROUND_ANIMATION_DURATION_TICKS.get();

        groundHookPullStrength = GROUND_HOOK_PULL_STRENGTH.get();
        groundHookSlideEnabled = GROUND_HOOK_SLIDE_ENABLED.get();
        groundHookSlideDurationTicks = GROUND_HOOK_SLIDE_DURATION_TICKS.get();
        groundHookSlideStartDistance = GROUND_HOOK_SLIDE_START_DISTANCE.get();
        groundHookSlideInitialSpeed = GROUND_HOOK_SLIDE_INITIAL_SPEED.get();
        groundHookSlideFriction = GROUND_HOOK_SLIDE_FRICTION.get();
        groundHookSlideMinSpeed = GROUND_HOOK_SLIDE_MIN_SPEED.get();
        groundHookTargetPullDurationTicks = GROUND_HOOK_TARGET_PULL_DURATION_TICKS.get();
        groundHookTargetPullMinSpeed = GROUND_HOOK_TARGET_PULL_MIN_SPEED.get();
        groundHookTargetPullMaxSpeed = GROUND_HOOK_TARGET_PULL_MAX_SPEED.get();
        groundHookTargetPullArrivalDistance = GROUND_HOOK_TARGET_PULL_ARRIVAL_DISTANCE.get();
        groundHookTargetPullSnapToTarget = GROUND_HOOK_TARGET_PULL_SNAP_TO_TARGET.get();

        airPullStrength = AIR_PULL_STRENGTH.get();
        airHookTargetPullDurationTicks = AIR_HOOK_TARGET_PULL_DURATION_TICKS.get();
        airHookTargetPullMinSpeed = AIR_HOOK_TARGET_PULL_MIN_SPEED.get();
        airHookTargetPullMaxSpeed = AIR_HOOK_TARGET_PULL_MAX_SPEED.get();
        airHookTargetPullArrivalDistance = AIR_HOOK_TARGET_PULL_ARRIVAL_DISTANCE.get();
        airHookTargetPullSnapToTarget = AIR_HOOK_TARGET_PULL_SNAP_TO_TARGET.get();
        airHookFovHoldTailTicks = AIR_HOOK_FOV_HOLD_TAIL_TICKS.get();

        wallHookFacingRetargetEnabled = WALL_HOOK_FACING_RETARGET_ENABLED.get();
        wallHookFacingRetargetAdaptiveEnabled = WALL_HOOK_FACING_RETARGET_ADAPTIVE_ENABLED.get();
        wallHookFacingRetargetAngleWidthDegrees = WALL_HOOK_FACING_RETARGET_ANGLE_WIDTH_DEGREES.get();
        wallHookFacingRetargetHalfAngleDegrees = wallHookFacingRetargetAngleWidthDegrees * 0.5D;
        wallHookFacingRetargetDot = Math.cos(Math.toRadians(wallHookFacingRetargetHalfAngleDegrees));
        wallHookFacingRetargetMinUpOffset = WALL_HOOK_FACING_RETARGET_MIN_UP_OFFSET.get();
        wallHookFacingRetargetUpOffset = WALL_HOOK_FACING_RETARGET_UP_OFFSET.get();
        wallHookFacingRetargetSearchStep = WALL_HOOK_FACING_RETARGET_SEARCH_STEP.get();
        wallHookFacingRetargetForwardOffset = WALL_HOOK_FACING_RETARGET_FORWARD_OFFSET.get();

        entityPullStrengthMultiplier = ENTITY_PULL_STRENGTH_MULTIPLIER.get();
        entityPullUpBoost = ENTITY_PULL_UP_BOOST.get();
        respectKnockbackResistance = RESPECT_KNOCKBACK_RESISTANCE.get();

        phantomSwingHoldThresholdTicks = PHANTOM_SWING_HOLD_THRESHOLD_TICKS.get();
        phantomMissedAirHorizontalPullStrength = PHANTOM_MISSED_AIR_HORIZONTAL_PULL_STRENGTH.get();
        phantomMissedAirMaxHorizontalSpeed = PHANTOM_MISSED_AIR_MAX_HORIZONTAL_SPEED.get();
        phantomMissedAirBaseUpSpeed = PHANTOM_MISSED_AIR_BASE_UP_SPEED.get();
        phantomMissedAirHeightUpScale = PHANTOM_MISSED_AIR_HEIGHT_UP_SCALE.get();
        phantomMissedAirMaxUpSpeed = PHANTOM_MISSED_AIR_MAX_UP_SPEED.get();
        phantomMissedAirMaxTotalSpeed = PHANTOM_MISSED_AIR_MAX_TOTAL_SPEED.get();
        phantomSwingReelInSpeed = PHANTOM_SWING_REEL_IN_SPEED.get();
        phantomSwingInitialBoost = PHANTOM_SWING_INITIAL_BOOST.get();
        phantomSwingConstraintStrength = PHANTOM_SWING_CONSTRAINT_STRENGTH.get();
        phantomSwingMaxCorrectionSpeed = PHANTOM_SWING_MAX_CORRECTION_SPEED.get();
        phantomSwingMaxSpeed = PHANTOM_SWING_MAX_SPEED.get();
        phantomSwingPreholdFallMultiplier = PHANTOM_SWING_PREHOLD_FALL_MULTIPLIER.get();
        phantomSwingPreholdMaxFallSpeed = PHANTOM_SWING_PREHOLD_MAX_FALL_SPEED.get();
        phantomSwingEnergyRetention = PHANTOM_SWING_ENERGY_RETENTION.get();
        phantomSwingEnergyGainPerTick = PHANTOM_SWING_ENERGY_GAIN_PER_TICK.get();
        phantomSwingEnergyCapRatio = PHANTOM_SWING_ENERGY_CAP_RATIO.get();
        phantomSwingMaxAngleDegrees = PHANTOM_SWING_MAX_ANGLE_DEGREES.get();
        phantomSwingPerSwingEnergyMultiplier = PHANTOM_SWING_PER_SWING_ENERGY_MULTIPLIER.get();
    }

    public static int getHookLockDelayTicks() {
        return Math.max(1, (int) Math.ceil(BASE_HOOK_LOCK_DELAY_TICKS / hookPullAnimationSpeed));
    }

    public static float getHookLockAnimationTimeSeconds() {
        return BASE_HOOK_LOCK_DELAY_TICKS / 20.0F;
    }

    public static double getProjectileSpeed() {
        return getProjectileSpeed(0.0D);
    }

    public static double getProjectileSpeed(double extraRange) {
        return BASE_PROJECTILE_SPEED * hookPullAnimationSpeed
                + Math.max(0.0D, extraRange) / getHookLockDelayTicks();
    }

    public static int getMissedHookVisualTicks() {
        return Math.max(missedHookGroundAnimationDurationTicks, MIN_MISSED_HOOK_VISUAL_TICKS);
    }

    public static int getNormalHookMaxLifeTicks() {
        int lockDelayTicks = getHookLockDelayTicks();
        return Math.max(
                lockDelayTicks + getMissedHookVisualTicks() + 2,
                Math.max(
                        lockDelayTicks + 8 + airHookTargetPullDurationTicks,
                        lockDelayTicks + 7 + groundHookTargetPullDurationTicks));
    }

    public static int getHookUseBlockDurationTicks() {
        return getNormalHookMaxLifeTicks() + 20;
    }

    public static double getPhantomProjectileSpeed() {
        return getPhantomProjectileSpeed(0.0D);
    }

    public static double getPhantomProjectileSpeed(double extraRange) {
        return (PHANTOM_HOOK_LAUNCH_RANGE + Math.max(0.0D, extraRange)) / getHookLockDelayTicks();
    }
}
