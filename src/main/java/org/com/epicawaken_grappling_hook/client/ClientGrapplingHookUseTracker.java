package org.com.epicawaken_grappling_hook.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientGrapplingHookUseTracker {
    private static final int CONFIGURED_USE_TTL_TICKS = 80;
    private static final int IN_HAND_RENDER_TTL_TICKS = 50;
    private static final Map<Integer, Long> CONFIGURED_USES = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> IN_HAND_RENDER_USES = new ConcurrentHashMap<>();

    public static void markConfiguredUse(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            long gameTime = minecraft.level.getGameTime();
            CONFIGURED_USES.put(entityId, gameTime + CONFIGURED_USE_TTL_TICKS);
            IN_HAND_RENDER_USES.put(entityId, gameTime + IN_HAND_RENDER_TTL_TICKS);
        }
    }

    public static boolean hasActiveConfiguredUse(Entity entity) {
        return hasTrackedUse(entity, CONFIGURED_USES);
    }

    public static boolean shouldRenderInHand(Entity entity) {
        return hasTrackedUse(entity, IN_HAND_RENDER_USES);
    }

    private static boolean hasTrackedUse(Entity entity, Map<Integer, Long> trackedUses) {
        Long expiresAt = trackedUses.get(entity.getId());
        if (expiresAt == null) {
            return false;
        }

        if (entity.level().getGameTime() > expiresAt) {
            trackedUses.remove(entity.getId(), expiresAt);
            return false;
        }

        return true;
    }

    private ClientGrapplingHookUseTracker() {
    }
}
