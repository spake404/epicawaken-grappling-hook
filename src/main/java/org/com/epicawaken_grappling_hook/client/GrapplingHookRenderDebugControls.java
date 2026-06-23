package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.lwjgl.glfw.GLFW;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Locale;

public final class GrapplingHookRenderDebugControls {
    private static final float POSITION_STEP = 0.025F;
    private static final float ROTATION_STEP = 5.0F;
    private static final float SCALE_STEP = 0.01F;

    private static final Transform DEFAULT_NORMAL_TRANSFORM = new Transform(-0.60F, 0.50F, 0.250F, 0.0F, 0.0F, -180.0F, 0.50F);
    private static final Transform DEFAULT_PULL_TRANSFORM = new Transform(-0.60F, 0.50F, 0.250F, 0.0F, 0.0F, -180.0F, 0.50F);
    private static final Transform EPIC_FIGHT_NORMAL_TRANSFORM = new Transform(0.675F, 0.325F, -0.175F, 185.0F, 0.0F, 0.0F, 0.52F);
    private static final Transform EPIC_FIGHT_PULL_TRANSFORM = new Transform(0.675F, 0.325F, -0.175F, 185.0F, 0.0F, 0.0F, 0.52F);
    private static final Transform ROPE_HAND_TRANSFORM = new Transform(0.0F, 0.125F, -0.225F, 0.0F, 0.0F, 0.0F, 1.0F);
    private static final Transform ROPE_GROUND_AIR_TRANSFORM = new Transform(0.0F, 0.325F, -0.225F, 0.0F, 0.0F, 0.0F, 1.0F);
    private static final Transform PROJECTILE_ARROW_TRANSFORM = new Transform(-0.325F, 0.425F, -0.425F, -90.0F, -5.0F, 90.0F, 0.84F);

    private static final KeyMapping TOGGLE_MODEL = key("toggle_model", GLFW.GLFW_KEY_KP_ADD);
    private static final KeyMapping TOGGLE_TARGET = key("toggle_target", GLFW.GLFW_KEY_KP_SUBTRACT);
    private static final KeyMapping PRINT_VALUES = key("print_values", GLFW.GLFW_KEY_KP_0);
    private static final KeyMapping X_DOWN = key("x_down", GLFW.GLFW_KEY_KP_1);
    private static final KeyMapping X_UP = key("x_up", GLFW.GLFW_KEY_KP_3);
    private static final KeyMapping Y_DOWN = key("y_down", GLFW.GLFW_KEY_KP_4);
    private static final KeyMapping Y_UP = key("y_up", GLFW.GLFW_KEY_KP_6);
    private static final KeyMapping Z_DOWN = key("z_down", GLFW.GLFW_KEY_KP_7);
    private static final KeyMapping Z_UP = key("z_up", GLFW.GLFW_KEY_KP_9);
    private static final KeyMapping VALUE_DOWN = key("value_down", GLFW.GLFW_KEY_KP_2);
    private static final KeyMapping VALUE_UP = key("value_up", GLFW.GLFW_KEY_KP_8);
    private static final KeyMapping CYCLE_PARAMETER = key("cycle_parameter", GLFW.GLFW_KEY_KP_5);

    private static ModelMode modelMode = ModelMode.NORMAL;
    private static Target target = Target.EPIC_FIGHT;
    private static Parameter parameter = Parameter.ROT_Z;

    private GrapplingHookRenderDebugControls() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_MODEL);
        event.register(TOGGLE_TARGET);
        event.register(PRINT_VALUES);
        event.register(X_DOWN);
        event.register(X_UP);
        event.register(Y_DOWN);
        event.register(Y_UP);
        event.register(Z_DOWN);
        event.register(Z_UP);
        event.register(VALUE_DOWN);
        event.register(VALUE_UP);
        event.register(CYCLE_PARAMETER);
    }

    public static void tick() {
        while (TOGGLE_MODEL.consumeClick()) {
            modelMode = modelMode == ModelMode.NORMAL ? ModelMode.PULL : ModelMode.NORMAL;
            announce("Model switched to " + modelMode.label);
        }
        while (TOGGLE_TARGET.consumeClick()) {
            target = target.next();
            announce("Transform target switched to " + target.label);
        }
        while (PRINT_VALUES.consumeClick()) {
            announce(currentTransform().format(modelMode, target, parameter));
        }
        while (CYCLE_PARAMETER.consumeClick()) {
            parameter = parameter.next();
            announce("Selected parameter: " + parameter.label + " | " + currentTransform().format(modelMode, target, parameter));
        }
        adjustClicks(X_DOWN, Parameter.X, -POSITION_STEP);
        adjustClicks(X_UP, Parameter.X, POSITION_STEP);
        adjustClicks(Y_DOWN, Parameter.Y, -POSITION_STEP);
        adjustClicks(Y_UP, Parameter.Y, POSITION_STEP);
        adjustClicks(Z_DOWN, Parameter.Z, -POSITION_STEP);
        adjustClicks(Z_UP, Parameter.Z, POSITION_STEP);
        adjustClicks(VALUE_DOWN, parameter, -parameter.step());
        adjustClicks(VALUE_UP, parameter, parameter.step());
    }

    public static boolean isPullModelForced() {
        return modelMode == ModelMode.PULL;
    }

    public static void applyDefaultNormalTransform(PoseStack poseStack) {
        DEFAULT_NORMAL_TRANSFORM.apply(poseStack);
    }

    public static void applyDefaultPullTransform(PoseStack poseStack) {
        DEFAULT_PULL_TRANSFORM.apply(poseStack);
    }

    public static void applyEpicFightNormalTransform(PoseStack poseStack) {
        EPIC_FIGHT_NORMAL_TRANSFORM.apply(poseStack);
    }

    public static void applyEpicFightPullTransform(PoseStack poseStack) {
        EPIC_FIGHT_PULL_TRANSFORM.apply(poseStack);
    }

    public static void applyProjectileArrowTransform(PoseStack poseStack) {
        PROJECTILE_ARROW_TRANSFORM.apply(poseStack);
    }

    public static Vec3 ropeHandLocalOffset() {
        return new Vec3(ROPE_HAND_TRANSFORM.x, ROPE_HAND_TRANSFORM.y, ROPE_HAND_TRANSFORM.z);
    }

    public static Vec3 ropeHandLocalOffset(LivingEntityPatch<?> entityPatch) {
        if (isGroundOrAirHookAnimation(entityPatch)) {
            return new Vec3(ROPE_GROUND_AIR_TRANSFORM.x, ROPE_GROUND_AIR_TRANSFORM.y, ROPE_GROUND_AIR_TRANSFORM.z);
        }
        return ropeHandLocalOffset();
    }

    private static KeyMapping key(String name, int keyCode) {
        return new KeyMapping(
                "key.epicawaken_grappling_hook.debug." + name,
                InputConstants.Type.KEYSYM,
                keyCode,
                "key.categories.epicawaken_grappling_hook");
    }

    private static void adjustClicks(KeyMapping keyMapping, Parameter parameter, float delta) {
        while (keyMapping.consumeClick()) {
            Transform transform = currentTransform();
            transform.adjust(parameter, delta);
            announce(transform.format(modelMode, target, GrapplingHookRenderDebugControls.parameter));
        }
    }

    private static Transform currentTransform() {
        if (target == Target.ROPE_HAND) {
            return ROPE_HAND_TRANSFORM;
        }
        if (target == Target.ROPE_GROUND_AIR) {
            return ROPE_GROUND_AIR_TRANSFORM;
        }
        if (target == Target.PROJECTILE_ARROW) {
            return PROJECTILE_ARROW_TRANSFORM;
        }
        if (target == Target.DEFAULT) {
            return modelMode == ModelMode.NORMAL ? DEFAULT_NORMAL_TRANSFORM : DEFAULT_PULL_TRANSFORM;
        }
        return modelMode == ModelMode.NORMAL ? EPIC_FIGHT_NORMAL_TRANSFORM : EPIC_FIGHT_PULL_TRANSFORM;
    }

    private static void announce(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("[Grappling Hook Render Debug] " + message), true);
        }
        Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookRenderDebug] {}", message);
    }

    public static boolean isGroundOrAirHookAnimation(LivingEntityPatch<?> entityPatch) {
        if (entityPatch == null) {
            return false;
        }
        AnimationPlayer animationPlayer = entityPatch.getAnimator().getPlayerFor(null);
        if (animationPlayer == null) {
            return false;
        }
        return isGroundOrAirHookAnimation(animationPlayer.getRealAnimation())
                || isGroundOrAirHookAnimation(animationPlayer.getAnimation());
    }

    private static boolean isGroundOrAirHookAnimation(AssetAccessor<?> animation) {
        if (animation == null) {
            return false;
        }
        String name = String.valueOf(animation.registryName());
        return name.endsWith("/hook_ground") || name.endsWith("/hook_air");
    }

    private enum ModelMode {
        NORMAL("normal"),
        PULL("11tick_pull");

        private final String label;

        ModelMode(String label) {
            this.label = label;
        }
    }

    private enum Target {
        DEFAULT("default_curios"),
        EPIC_FIGHT("EpicFight"),
        ROPE_HAND("rope_hand"),
        ROPE_GROUND_AIR("rope_ground_air"),
        PROJECTILE_ARROW("projectile_arrow");

        private final String label;

        Target(String label) {
            this.label = label;
        }

        private Target next() {
            return switch (this) {
                case DEFAULT -> EPIC_FIGHT;
                case EPIC_FIGHT -> ROPE_HAND;
                case ROPE_HAND -> ROPE_GROUND_AIR;
                case ROPE_GROUND_AIR -> PROJECTILE_ARROW;
                case PROJECTILE_ARROW -> DEFAULT;
            };
        }
    }

    private enum Parameter {
        X("X"),
        Y("Y"),
        Z("Z"),
        ROT_X("rotX"),
        ROT_Y("rotY"),
        ROT_Z("rotZ"),
        SCALE("scale");

        private final String label;

        Parameter(String label) {
            this.label = label;
        }

        private Parameter next() {
            return switch (this) {
                case ROT_X -> ROT_Y;
                case ROT_Y -> ROT_Z;
                case ROT_Z -> SCALE;
                case SCALE, X, Y, Z -> ROT_X;
            };
        }

        private float step() {
            return this == SCALE ? SCALE_STEP : ROTATION_STEP;
        }
    }

    private static final class Transform {
        private float x;
        private float y;
        private float z;
        private float rotX;
        private float rotY;
        private float rotZ;
        private float scale;

        private Transform(float x, float y, float z, float rotX, float rotY, float rotZ, float scale) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.scale = scale;
        }

        private void apply(PoseStack poseStack) {
            poseStack.translate(x, y, z);
            applyRotation(poseStack, Axis.XP, rotX);
            applyRotation(poseStack, Axis.YP, rotY);
            applyRotation(poseStack, Axis.ZP, rotZ);
            poseStack.scale(scale, scale, scale);
        }

        private void adjust(Parameter parameter, float delta) {
            switch (parameter) {
                case X -> x += delta;
                case Y -> y += delta;
                case Z -> z += delta;
                case ROT_X -> rotX += delta;
                case ROT_Y -> rotY += delta;
                case ROT_Z -> rotZ += delta;
                case SCALE -> scale = Math.max(0.01F, scale + delta);
            }
        }

        private String format(ModelMode modelMode, Target target, Parameter selectedParameter) {
            return String.format(Locale.ROOT,
                    "model=%s target=%s selected=%s X=%.3f Y=%.3f Z=%.3f rotX=%.1f rotY=%.1f rotZ=%.1f scale=%.2f",
                    modelMode.label,
                    target.label,
                    selectedParameter.label,
                    x,
                    y,
                    z,
                    rotX,
                    rotY,
                    rotZ,
                    scale);
        }
    }

    private static void applyRotation(PoseStack poseStack, Axis axis, float degrees) {
        if (degrees != 0.0F) {
            poseStack.mulPose(axis.rotationDegrees(degrees));
        }
    }
}
