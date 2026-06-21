package org.com.epicawaken_grappling_hook.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;

@OnlyIn(Dist.CLIENT)
public final class ClientGrapplingHookFovEffect {
    private static final int CAPTURE_TTL_TICKS = 80;

    private static float lastObservedFovModifier = 1.0F;
    private static float capturedFovModifier = 1.0F;
    private static boolean hasCapturedFov;
    private static boolean active;
    private static boolean airFovHoldActive;
    private static long captureExpiresAt;
    private static int airFovHoldTailTicks;
    private static long lastAirFovHoldTailGameTime = Long.MIN_VALUE;

    public static void recordUseAttempt() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clearCapture();
            return;
        }

        capturedFovModifier = lastObservedFovModifier;
        hasCapturedFov = true;
        captureExpiresAt = minecraft.level.getGameTime() + CAPTURE_TTL_TICKS;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookFovDebug][CLIENT] captured fovModifier={} expiresAt={}",
                    capturedFovModifier,
                    captureExpiresAt);
        }
    }

    public static void start() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !hasCapturedFov || minecraft.level.getGameTime() > captureExpiresAt) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookFovDebug][CLIENT] start ignored hasCaptured={} gameTime={} expiresAt={}",
                        hasCapturedFov,
                        minecraft.level != null ? minecraft.level.getGameTime() : -1,
                        captureExpiresAt);
            }
            return;
        }

        active = true;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookFovDebug][CLIENT] started fovModifier={} gameTime={}",
                    capturedFovModifier,
                    minecraft.level.getGameTime());
        }
    }

    public static void startAirFovHold() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !hasCapturedFov || minecraft.level.getGameTime() > captureExpiresAt || capturedFovModifier <= 1.0F) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookFovDebug][CLIENT] air fov hold ignored hasCaptured={} captured={} gameTime={} expiresAt={}",
                        hasCapturedFov,
                        capturedFovModifier,
                        minecraft.level != null ? minecraft.level.getGameTime() : -1,
                        captureExpiresAt);
            }
            return;
        }

        airFovHoldActive = true;
        airFovHoldTailTicks = 0;
        lastAirFovHoldTailGameTime = Long.MIN_VALUE;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookFovDebug][CLIENT] air fov hold started fovModifier={} gameTime={}",
                    capturedFovModifier,
                    minecraft.level.getGameTime());
        }
    }

    public static void stop() {
        debugStop();
        active = false;
    }

    public static void startAirFovHoldTail() {
        if (!airFovHoldActive && capturedFovModifier <= 1.0F) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookFovDebug][CLIENT] air fov hold tail ignored active={} captured={}",
                        airFovHoldActive,
                        capturedFovModifier);
            }
            return;
        }

        airFovHoldActive = false;
        airFovHoldTailTicks = Config.airHookFovHoldTailTicks;
        lastAirFovHoldTailGameTime = Long.MIN_VALUE;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookFovDebug][CLIENT] air fov hold tail started fovModifier={} ticks={}",
                    capturedFovModifier,
                    airFovHoldTailTicks);
        }
    }

    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        if (airFovHoldActive && hasCapturedFov) {
            event.setNewFovModifier(capturedFovModifier);
            lastObservedFovModifier = capturedFovModifier;
            return;
        }

        if (airFovHoldTailTicks > 0 && hasCapturedFov) {
            Minecraft minecraft = Minecraft.getInstance();
            long gameTime = minecraft.level != null ? minecraft.level.getGameTime() : Long.MIN_VALUE;
            if (gameTime != lastAirFovHoldTailGameTime) {
                lastAirFovHoldTailGameTime = gameTime;
                airFovHoldTailTicks--;
            }
            event.setNewFovModifier(capturedFovModifier);
            lastObservedFovModifier = capturedFovModifier;
            return;
        }

        if (active && hasCapturedFov) {
            event.setNewFovModifier(capturedFovModifier);
            return;
        }

        lastObservedFovModifier = event.getNewFovModifier();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && hasCapturedFov && minecraft.level.getGameTime() > captureExpiresAt) {
            clearCapture();
        }
    }

    private static void debugStop() {
        if (!active || !Config.debugLogging) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookFovDebug][CLIENT] stopped gameTime={}",
                minecraft.level != null ? minecraft.level.getGameTime() : -1);
    }

    private static void clearCapture() {
        hasCapturedFov = false;
        active = false;
        airFovHoldActive = false;
        capturedFovModifier = 1.0F;
        captureExpiresAt = 0L;
        airFovHoldTailTicks = 0;
        lastAirFovHoldTailGameTime = Long.MIN_VALUE;
    }

    private ClientGrapplingHookFovEffect() {
    }
}
