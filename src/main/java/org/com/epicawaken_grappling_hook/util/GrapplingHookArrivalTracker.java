package org.com.epicawaken_grappling_hook.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;

public class GrapplingHookArrivalTracker {
    private static final long ARRIVAL_TTL_TICKS = 20L;
    private static final Map<UUID, Long> ARRIVALS = new ConcurrentHashMap<>();

    public static void markArrived(Entity entity) {
        if (entity == null) {
            return;
        }

        ARRIVALS.put(entity.getUUID(), entity.level().getGameTime() + ARRIVAL_TTL_TICKS);
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][{}] markArrived entity={} uuid={} gameTime={} expiresAt={}",
                    entity.level().isClientSide ? "CLIENT" : "SERVER",
                    entity.getId(),
                    entity.getUUID(),
                    entity.level().getGameTime(),
                    entity.level().getGameTime() + ARRIVAL_TTL_TICKS);
        }
    }

    public static boolean hasArrived(Entity entity) {
        if (entity == null) {
            return false;
        }

        Long expiresAt = ARRIVALS.get(entity.getUUID());
        if (expiresAt == null) {
            return false;
        }

        if (entity.level().getGameTime() > expiresAt) {
            ARRIVALS.remove(entity.getUUID(), expiresAt);
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][{}] arrival expired entity={} gameTime={} expiresAt={}",
                        entity.level().isClientSide ? "CLIENT" : "SERVER",
                        entity.getId(),
                        entity.level().getGameTime(),
                        expiresAt);
            }
            return false;
        }

        return true;
    }

    private GrapplingHookArrivalTracker() {
    }
}
