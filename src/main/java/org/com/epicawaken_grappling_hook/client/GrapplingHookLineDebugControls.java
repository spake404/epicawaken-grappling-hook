package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.lwjgl.glfw.GLFW;

public final class GrapplingHookLineDebugControls {
    private static final KeyMapping CYCLE_COLOR = key("cycle_line_color", GLFW.GLFW_KEY_KP_DECIMAL);
    private static final KeyMapping TOGGLE_PREVIEW = key("toggle_line_preview", GLFW.GLFW_KEY_KP_ENTER);
    private static final int GRADIENT_SEGMENT_SPAN = 3;
    private static final float[][] STATIC_GRADIENT_COLORS = {
            {0.23921569F, 0.1882353F, 0.18039216F},
            {0.3764706F, 0.28235295F, 0.24313726F},
            {0.5176471F, 0.4392157F, 0.39215687F}
    };

    private static ColorMode colorMode = ColorMode.STATIC_GRADIENT;
    private static boolean previewEnabled;
    private static boolean previewKeyWasDown;
    private static boolean skipNextPreviewMappingClick;

    private GrapplingHookLineDebugControls() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_COLOR);
        event.register(TOGGLE_PREVIEW);
    }

    public static void tick() {
        while (CYCLE_COLOR.consumeClick()) {
            colorMode = colorMode.next();
            announce("Line color switched to " + colorMode.label);
        }

        boolean toggledByKeyMapping = false;
        while (TOGGLE_PREVIEW.consumeClick()) {
            if (skipNextPreviewMappingClick) {
                skipNextPreviewMappingClick = false;
            } else {
                togglePreview();
                toggledByKeyMapping = true;
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean previewKeyDown = minecraft.getWindow() != null
                && (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_KP_ENTER)
                || InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_ENTER));
        if (previewKeyDown && !previewKeyWasDown && !toggledByKeyMapping) {
            togglePreview();
        }
        previewKeyWasDown = previewKeyDown;
    }

    public static void onKeyInput(int key, int action) {
        if (action == GLFW.GLFW_PRESS && isPreviewToggleKey(key)) {
            togglePreview();
            previewKeyWasDown = true;
            skipNextPreviewMappingClick = true;
        }
    }

    public static boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public static float red(int step) {
        return colorMode.red(step);
    }

    public static float green(int step) {
        return colorMode.green(step);
    }

    public static float blue(int step) {
        return colorMode.blue(step);
    }

    private static KeyMapping key(String name, int keyCode) {
        return new KeyMapping(
                "key.epicawaken_grappling_hook.debug." + name,
                InputConstants.Type.KEYSYM,
                keyCode,
                "key.categories.epicawaken_grappling_hook");
    }

    private static void announce(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("[Grappling Hook Line Debug] " + message), true);
        }
        Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookLineDebug] {}", message);
    }

    private static void togglePreview() {
        previewEnabled = !previewEnabled;
        announce("Line preview " + (previewEnabled ? "enabled" : "disabled"));
    }

    private static boolean isPreviewToggleKey(int key) {
        return key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_ENTER;
    }

    private enum ColorMode {
        STATIC_GRADIENT("static_gradient", 0.0F, 0.0F, 0.0F, true),
        DARK_GRAY("dark_gray", 0.12F, 0.12F, 0.13F),
        CYAN("cyan", 0.0F, 0.8F, 1.0F),
        PURPLE("purple", 0.55F, 0.25F, 1.0F),
        WHITE("white", 0.9F, 0.9F, 0.9F);

        private final String label;
        private final float red;
        private final float green;
        private final float blue;
        private final boolean gradient;

        ColorMode(String label, float red, float green, float blue) {
            this(label, red, green, blue, false);
        }

        ColorMode(String label, float red, float green, float blue, boolean gradient) {
            this.label = label;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.gradient = gradient;
        }

        private ColorMode next() {
            ColorMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        private float red(int step) {
            return gradient ? gradientColor(step)[0] : red;
        }

        private float green(int step) {
            return gradient ? gradientColor(step)[1] : green;
        }

        private float blue(int step) {
            return gradient ? gradientColor(step)[2] : blue;
        }

        private static float[] gradientColor(int step) {
            int colorIndex = Math.floorMod(step / GRADIENT_SEGMENT_SPAN, STATIC_GRADIENT_COLORS.length);
            return STATIC_GRADIENT_COLORS[colorIndex];
        }
    }
}
