package org.com.epicawaken_grappling_hook.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.network.GrapplingHookFovType;
import org.com.epicawaken_grappling_hook.util.GrapplingHookArrivalTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookMissedTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookParcoolBlocker;

@OnlyIn(Dist.CLIENT)
public final class ClientNetworkPacketHandlers {
    private ClientNetworkPacketHandlers() {
    }

    public static void handleConfiguredUse(int entityId) {
        ClientGrapplingHookUseTracker.markConfiguredUse(entityId);
        if (Minecraft.getInstance().level == null) {
            return;
        }

        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        GrapplingHookMissedTracker.clearMissed(entity);
        GrapplingHookParcoolBlocker.block(entity, Config.maxLifeTicks + Config.getHookLockDelayTicks() + 20);
    }

    public static void handleArrival(int entityId) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if (entity != null) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][CLIENT] received arrival packet entity={} gameTime={}", entityId, Minecraft.getInstance().level.getGameTime());
            }
            GrapplingHookArrivalTracker.markArrived(entity);
        } else if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][CLIENT] received arrival packet but entity not found entity={}", entityId);
        }
    }

    public static void handleMissed(int entityId) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if (entity != null) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][CLIENT] received missed packet entity={} gameTime={}",
                        entityId,
                        Minecraft.getInstance().level.getGameTime());
            }
            GrapplingHookMissedTracker.markMissed(entity);
            ClientMissedHookRopeRetractTracker.markNewMissed(entity);
        } else if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][CLIENT] received missed packet but entity not found entity={}", entityId);
        }
    }

    public static void handleStartFov(GrapplingHookFovType type) {
        ClientGrapplingHookSprintRestore.onFovStarted(type);
        if (type != GrapplingHookFovType.AIR) {
            ClientGrapplingHookFovEffect.start();
        } else {
            ClientGrapplingHookFovEffect.startAirFovHold();
        }
        GrapplingHookParcoolBlocker.block(Minecraft.getInstance().player, 80);
    }

    public static void handleStopFov(GrapplingHookFovType type) {
        ClientGrapplingHookFovEffect.stop();
        if (type == GrapplingHookFovType.AIR) {
            ClientGrapplingHookFovEffect.startAirFovHoldTail();
            ClientGrapplingHookWallRunBridge.openAirHookWindow();
        }
        ClientGrapplingHookSprintRestore.onFovStopped(type);
        GrapplingHookParcoolBlocker.clear(Minecraft.getInstance().player);
    }
}
