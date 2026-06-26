package org.com.epicawaken_grappling_hook.util;

import com.alrex.parcool.common.action.impl.Crawl;
import com.alrex.parcool.common.action.impl.Slide;
import com.alrex.parcool.common.capability.Parkourability;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;

@Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID)
public final class GrapplingHookParcoolBlocker {
    private static final Map<UUID, Long> BLOCKED_UNTIL = new ConcurrentHashMap<>();

    private GrapplingHookParcoolBlocker() {
    }

    public static void block(Entity entity, int ticks) {
        if (!ParcoolCompat.isLoaded()
                || !Config.disableParcoolCrawlAndSlideDuringHook
                || !(entity instanceof Player player)
                || player.level() == null) {
            return;
        }

        long until = player.level().getGameTime() + Math.max(1, ticks);
        BLOCKED_UNTIL.merge(player.getUUID(), until, Math::max);
    }

    public static void clear(Entity entity) {
        if (entity instanceof Player player) {
            BLOCKED_UNTIL.remove(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTickStart(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        handleBlockedPlayer(event.player, false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTickEnd(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        handleBlockedPlayer(event.player, true);
    }

    private static void handleBlockedPlayer(Player player, boolean repairPose) {
        if (BLOCKED_UNTIL.isEmpty() || !ParcoolCompat.isLoaded() || !Config.disableParcoolCrawlAndSlideDuringHook) {
            return;
        }

        if (!isBlocked(player)) {
            return;
        }

        cancelBlockedParcoolActions(player);
        if (repairPose) {
            repairCrawlPose(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity());
    }

    private static boolean isBlocked(Player player) {
        Long expiresAt = BLOCKED_UNTIL.get(player.getUUID());
        if (expiresAt == null) {
            return false;
        }

        if (player.level().getGameTime() > expiresAt) {
            BLOCKED_UNTIL.remove(player.getUUID(), expiresAt);
            return false;
        }

        return true;
    }

    private static void cancelBlockedParcoolActions(Player player) {
        Parkourability parkourability = Parkourability.get(player);
        if (parkourability == null) {
            return;
        }

        Slide slide = parkourability.get(Slide.class);
        if (slide != null && slide.isDoing()) {
            slide.finish(player);
        }

        Crawl crawl = parkourability.get(Crawl.class);
        if (crawl != null) {
            crawl.toggleStatus = false;
            if (crawl.isDoing()) {
                crawl.finish(player);
            }
        }
    }

    private static void repairCrawlPose(Player player) {
        if (player.getPose() != Pose.SWIMMING || player.isInWaterOrBubble() || player.isFallFlying()) {
            return;
        }

        player.setPose(player.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING);
    }
}
