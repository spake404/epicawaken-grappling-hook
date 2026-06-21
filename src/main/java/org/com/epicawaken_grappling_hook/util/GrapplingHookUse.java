package org.com.epicawaken_grappling_hook.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.animation.ModHookAnimations;
import org.com.epicawaken_grappling_hook.client.ClientGrapplingHookUseTracker;
import org.com.epicawaken_grappling_hook.entity.ModEntities;
import org.com.epicawaken_grappling_hook.item.ModItems;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.SyncConfiguredUsePacket;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHook;
import top.theillusivec4.curios.api.CuriosApi;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public class GrapplingHookUse {
    private static final int CONFIGURED_USE_TTL_TICKS = 80;
    private static final Map<UUID, Long> CONFIGURED_USES = new ConcurrentHashMap<>();

    public static void tryUse(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        if (!hasEquippedGrapplingHook(player)) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(ModItems.GRAPPLING_HOOK.get())) {
            return;
        }

        GrapplingHookParcoolBlocker.block(player, Config.maxLifeTicks + Config.getHookLockDelayTicks() + 20);
        markConfiguredUse(player);
        ServerPlayerPatch playerPatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (playerPatch != null) {
            playerPatch.playAnimationSynchronized(ModHookAnimations.HOOK_PULL, 0.0F);
        } else {
            shootFallbackHook(player);
        }

        if (Config.grapplingHookCooldown > 0) {
            player.getCooldowns().addCooldown(ModItems.GRAPPLING_HOOK.get(), Config.grapplingHookCooldown);
        }
    }

    private static void shootFallbackHook(ServerPlayer player) {
        Level level = player.level();
        GrapplingHook hook = new GrapplingHook(ModEntities.GRAPPLING_HOOK.get(), level);
        hook.setOwner(player);
        hook.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        hook.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) Config.getProjectileSpeed(), (float) Config.projectileInaccuracy);
        level.addFreshEntity(hook);
    }

    public static boolean hasActiveConfiguredUse(Entity entity) {
        if (entity.level().isClientSide && FMLEnvironment.dist == Dist.CLIENT) {
            return ClientGrapplingHookUseTracker.hasActiveConfiguredUse(entity);
        }

        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }

        Long expiresAt = CONFIGURED_USES.get(player.getUUID());
        if (expiresAt == null) {
            return false;
        }

        if (player.serverLevel().getGameTime() > expiresAt) {
            CONFIGURED_USES.remove(player.getUUID(), expiresAt);
            return false;
        }

        return true;
    }

    private static void markConfiguredUse(ServerPlayer player) {
        CONFIGURED_USES.put(player.getUUID(), player.serverLevel().getGameTime() + CONFIGURED_USE_TTL_TICKS);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new SyncConfiguredUsePacket(player.getId()));
    }

    private static boolean hasEquippedGrapplingHook(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(handler -> handler.findFirstCurio(GrapplingHookUse::isGrapplingHookStack))
                .isPresent();
    }

    private static boolean isGrapplingHookStack(ItemStack stack) {
        return stack.is(ModItems.GRAPPLING_HOOK.get());
    }

    private GrapplingHookUse() {
    }
}
