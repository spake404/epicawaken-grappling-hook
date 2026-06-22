package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class ClientSlowMotionDebugControls {
    private static final float[] SPEEDS = {1.0F, 0.5F, 0.25F, 0.1F};

    private static final KeyMapping CYCLE_SPEED = key("slow_motion_cycle", GLFW.GLFW_KEY_KP_MULTIPLY);
    private static final KeyMapping RESET_SPEED = key("slow_motion_reset", GLFW.GLFW_KEY_KP_DIVIDE);

    private static int speedIndex;

    private ClientSlowMotionDebugControls() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_SPEED);
        event.register(RESET_SPEED);
    }

    public static void tick() {
        while (CYCLE_SPEED.consumeClick()) {
            speedIndex = (speedIndex + 1) % SPEEDS.length;
            announce();
        }
        while (RESET_SPEED.consumeClick()) {
            speedIndex = 0;
            announce();
        }
    }

    public static float speedMultiplier() {
        return SPEEDS[speedIndex];
    }

    public static boolean isSlowed() {
        return speedMultiplier() < 0.999F;
    }

    private static KeyMapping key(String name, int keyCode) {
        return new KeyMapping(
                "key.epicawaken_grappling_hook.debug." + name,
                InputConstants.Type.KEYSYM,
                keyCode,
                "key.categories.epicawaken_grappling_hook");
    }

    private static void announce() {
        String message = String.format(Locale.ROOT, "Client slow motion: %.2fx", speedMultiplier());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("[Grappling Hook Debug] " + message), true);
        }
        Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug] {}", message);
    }
}
