package org.com.epicawaken_grappling_hook.client;

import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.network.GrapplingHookFovType;
import org.com.epicawaken_grappling_hook.util.ParcoolCompat;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

@OnlyIn(Dist.CLIENT)
public final class ClientGrapplingHookSprintRestore {
    private static final int START_WAIT_TICKS = 60;
    private static final int RESTORE_TAIL_TICKS = 8;

    private static boolean shouldRestoreSprint;
    private static boolean active;
    private static GrapplingHookFovType activeType = GrapplingHookFovType.UNKNOWN;
    private static int remainingStartWaitTicks;
    private static int remainingTailTicks;

    public static void recordUseAttempt() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            clear("record_no_player");
            return;
        }

        shouldRestoreSprint = minecraft.player.isSprinting() || isFastRunning(minecraft.player);
        active = false;
        remainingStartWaitTicks = shouldRestoreSprint ? START_WAIT_TICKS : 0;
        remainingTailTicks = 0;
        debug("record shouldRestoreSprint={} startWaitTicks={}", shouldRestoreSprint, remainingStartWaitTicks);
    }

    public static void onFovStarted(GrapplingHookFovType type) {
        if (!shouldRestoreSprint) {
            return;
        }

        if (type == GrapplingHookFovType.AIR) {
            clear("air_start");
            return;
        }

        active = true;
        activeType = type;
        remainingStartWaitTicks = 0;
        remainingTailTicks = RESTORE_TAIL_TICKS;
        debug("start type={} tailTicks={}", activeType, remainingTailTicks);
    }

    public static void onFovStopped(GrapplingHookFovType type) {
        if (type == GrapplingHookFovType.AIR) {
            clear("air_stop");
            return;
        }

        if (!active) {
            return;
        }

        active = false;
        remainingTailTicks = RESTORE_TAIL_TICKS;
        debug("stop type={} tailTicks={}", activeType, remainingTailTicks);
    }

    public static boolean hasWork() {
        return shouldRestoreSprint;
    }

    public static void tick() {
        if (!shouldRestoreSprint) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null) {
            clear("tick_no_player");
            return;
        }

        if (!active) {
            if (remainingStartWaitTicks > 0) {
                remainingStartWaitTicks--;
                return;
            }

            if (remainingTailTicks <= 0) {
                clear("tail_complete");
                return;
            }
            remainingTailTicks--;
        }

        if (!canKeepSprinting(minecraft, player)) {
            return;
        }

        player.setSprinting(true);
        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (playerPatch != null && !playerPatch.getEntityState().movementLocked()) {
            playerPatch.updateMotion(false);
        }
    }

    private static boolean isFastRunning(LocalPlayer player) {
        if (!ParcoolCompat.isLoaded()) {
            return false;
        }

        Parkourability parkourability = Parkourability.get(player);
        if (parkourability == null) {
            return false;
        }

        FastRun fastRun = parkourability.get(FastRun.class);
        return fastRun != null && fastRun.isDoing();
    }

    private static boolean canKeepSprinting(Minecraft minecraft, LocalPlayer player) {
        return (minecraft.options.keyUp.isDown()
                || minecraft.options.keyDown.isDown()
                || minecraft.options.keyLeft.isDown()
                || minecraft.options.keyRight.isDown())
                && !player.isShiftKeyDown()
                && !player.isInWaterOrBubble()
                && !player.isFallFlying()
                && !player.isPassenger();
    }

    private static void clear(String reason) {
        debug("clear reason={}", reason);
        shouldRestoreSprint = false;
        active = false;
        activeType = GrapplingHookFovType.UNKNOWN;
        remainingStartWaitTicks = 0;
        remainingTailTicks = 0;
    }

    private static void debug(String message, Object... args) {
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSprintRestore][CLIENT] " + message, args);
        }
    }

    private ClientGrapplingHookSprintRestore() {
    }
}
