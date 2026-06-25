package org.com.epicawaken_grappling_hook.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;

public class GrapplingHookMissedTracker {
    private static final long MISSED_TTL_TICKS = 80L;
    private static final Map<UUID, Long> MISSED_HOOKS = new ConcurrentHashMap<>();

    public static void markMissed(Entity entity) {
        if (entity == null) {
            return;
        }

        long expiresAt = entity.level().getGameTime() + MISSED_TTL_TICKS;
        MISSED_HOOKS.put(entity.getUUID(), expiresAt);
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][{}] markMissed entity={} uuid={} gameTime={} expiresAt={}",
                    entity.level().isClientSide ? "CLIENT" : "SERVER",
                    entity.getId(),
                    entity.getUUID(),
                    entity.level().getGameTime(),
                    expiresAt);
        }
    }

    public static boolean hasMissed(Entity entity) {
        if (entity == null) {
            return false;
        }

        Long expiresAt = MISSED_HOOKS.get(entity.getUUID());
        if (expiresAt == null) {
            return false;
        }

        if (entity.level().getGameTime() > expiresAt) {
            MISSED_HOOKS.remove(entity.getUUID(), expiresAt);
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][{}] missed marker expired entity={} gameTime={} expiresAt={}",
                        entity.level().isClientSide ? "CLIENT" : "SERVER",
                        entity.getId(),
                        entity.level().getGameTime(),
                        expiresAt);
            }
            return false;
        }

        return true;
    }

    public static void clearMissed(Entity entity) {
        if (entity == null) {
            return;
        }

        MISSED_HOOKS.remove(entity.getUUID());
    }

    private GrapplingHookMissedTracker() {
    }
}
