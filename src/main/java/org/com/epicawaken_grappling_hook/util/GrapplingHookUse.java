package org.com.epicawaken_grappling_hook.util;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHookVariant;
import top.theillusivec4.curios.api.CuriosApi;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public class GrapplingHookUse {
    private static final int CONFIGURED_USE_TTL_TICKS = 80;
    private static final Map<UUID, Long> CONFIGURED_USES = new ConcurrentHashMap<>();
    private static final Map<UUID, GrapplingHookVariant> CONFIGURED_USE_VARIANTS = new ConcurrentHashMap<>();

    public static void tryUse(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        ItemStack grapplingHook = findEquippedGrapplingHook(player);
        if (grapplingHook.isEmpty()) {
            return;
        }
        if (isOnGrapplingHookCooldown(player)) {
            return;
        }
        if (!canReleaseGrapplingHook(grapplingHook)) {
            playFailedUse(player);
            addCooldown(player);
            return;
        }

        GrapplingHookParcoolBlocker.block(player, Config.maxLifeTicks + Config.getHookLockDelayTicks() + 20);
        GrapplingHookMissedTracker.clearMissed(player);
        GrapplingHookVariant variant = GrapplingHookVariant.fromStack(grapplingHook);
        markConfiguredUse(player, variant);
        ServerPlayerPatch playerPatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (playerPatch != null) {
            playerPatch.playAnimationSynchronized(ModHookAnimations.HOOK_PULL, 0.0F);
        } else {
            shootFallbackHook(player, variant);
        }
        damageGrapplingHook(player, grapplingHook);

        addCooldown(player);
    }

    private static void shootFallbackHook(ServerPlayer player, GrapplingHookVariant variant) {
        Level level = player.level();
        GrapplingHook hook = new GrapplingHook(ModEntities.GRAPPLING_HOOK.get(), level);
        hook.setOwner(player);
        hook.setVariant(variant);
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
            CONFIGURED_USE_VARIANTS.remove(player.getUUID());
            return false;
        }

        return true;
    }

    public static GrapplingHookVariant getActiveConfiguredUseVariant(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return GrapplingHookVariant.NORMAL;
        }

        if (!hasActiveConfiguredUse(player)) {
            return GrapplingHookVariant.NORMAL;
        }

        return CONFIGURED_USE_VARIANTS.getOrDefault(player.getUUID(), GrapplingHookVariant.NORMAL);
    }

    private static void markConfiguredUse(ServerPlayer player, GrapplingHookVariant variant) {
        CONFIGURED_USES.put(player.getUUID(), player.serverLevel().getGameTime() + CONFIGURED_USE_TTL_TICKS);
        CONFIGURED_USE_VARIANTS.put(player.getUUID(), variant);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new SyncConfiguredUsePacket(player.getId()));
    }

    private static void playFailedUse(ServerPlayer player) {
        GrapplingHookParcoolBlocker.block(player, 8);
        ServerPlayerPatch playerPatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (playerPatch != null) {
            playerPatch.playAnimationSynchronized(ModHookAnimations.HOOK_PULL, 0.0F);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void damageGrapplingHook(ServerPlayer player, ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return;
        }

        stack.setDamageValue(Math.min(stack.getDamageValue() + 1, stack.getMaxDamage()));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static void addCooldown(ServerPlayer player) {
        if (Config.grapplingHookCooldown > 0) {
            player.getCooldowns().addCooldown(ModItems.GRAPPLING_HOOK.get(), Config.grapplingHookCooldown);
            player.getCooldowns().addCooldown(ModItems.PHANTOM_GRAPPLING_HOOK.get(), Config.grapplingHookCooldown);
        }
    }

    private static boolean isOnGrapplingHookCooldown(ServerPlayer player) {
        return player.getCooldowns().isOnCooldown(ModItems.GRAPPLING_HOOK.get())
                || player.getCooldowns().isOnCooldown(ModItems.PHANTOM_GRAPPLING_HOOK.get());
    }

    private static boolean canReleaseGrapplingHook(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() < stack.getMaxDamage();
    }

    private static ItemStack findEquippedGrapplingHook(ServerPlayer player) {
        if (isGrapplingHookStack(player.getOffhandItem())) {
            return player.getOffhandItem();
        }

        Optional<ItemStack> curioStack = CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(handler -> handler.findFirstCurio(GrapplingHookUse::isGrapplingHookStack))
                .map(slotResult -> slotResult.stack());
        return curioStack.orElse(ItemStack.EMPTY);
    }

    private static boolean isGrapplingHookStack(ItemStack stack) {
        return stack.is(ModItems.GRAPPLING_HOOK.get()) || stack.is(ModItems.PHANTOM_GRAPPLING_HOOK.get());
    }

    private GrapplingHookUse() {
    }
}
