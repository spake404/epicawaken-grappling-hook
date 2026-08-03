package org.com.epicawaken_grappling_hook.util;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.animation.ModHookAnimations;
import org.com.epicawaken_grappling_hook.enchantment.GrapplingShieldDisarmEnchantment;
import org.com.epicawaken_grappling_hook.enchantment.ItemRetrievalEnchantment;
import org.com.epicawaken_grappling_hook.enchantment.RopeExtensionEnchantment;
import org.com.epicawaken_grappling_hook.enchantment.RopeRecoveryEnchantment;
import org.com.epicawaken_grappling_hook.client.ClientGrapplingHookUseTracker;
import org.com.epicawaken_grappling_hook.entity.ModEntities;
import org.com.epicawaken_grappling_hook.item.ModItems;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.SyncConfiguredUsePacket;
import org.com.epicawaken_grappling_hook.network.UseGrapplingHookPacket;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHook;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHookVariant;
import top.theillusivec4.curios.api.CuriosApi;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID)
public class GrapplingHookUse {
    private static final int CONFIGURED_USE_TTL_TICKS = 80;
    private static final int ITEM_RETRIEVAL_DOUBLE_CLICK_WINDOW_TICKS = 6;
    private static final Map<UUID, Long> CONFIGURED_USES = new ConcurrentHashMap<>();
    private static final Map<UUID, GrapplingHookVariant> CONFIGURED_USE_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, UseSession> USE_SESSIONS = new ConcurrentHashMap<>();

    public static void handleInput(ServerPlayer player, UseGrapplingHookPacket.Action action, int sequenceId, float aimYaw, float aimPitch) {
        if (sequenceId < 0) {
            return;
        }

        switch (action) {
            case PRESS -> tryUse(player, sequenceId, aimYaw, aimPitch);
            case RELEASE -> releaseUse(player, sequenceId);
            case ITEM_RETRIEVAL -> requestItemRetrieval(player, sequenceId);
            case ENTITY_PULL_TARGET -> requestEntityPullToOwner(player, sequenceId);
        }
    }

    private static void tryUse(ServerPlayer player, int sequenceId, float aimYaw, float aimPitch) {
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        ItemStack grapplingHook = findEquippedGrapplingHook(player);
        if (grapplingHook.isEmpty()) {
            return;
        }
        GrapplingHookVariant variant = GrapplingHookVariant.fromStack(grapplingHook);
        int ropeExtensionLevel = RopeExtensionEnchantment.getLevel(grapplingHook);
        int grapplingShieldDisarmLevel = GrapplingShieldDisarmEnchantment.getLevel(grapplingHook);
        if (hasActiveUseSession(player)) {
            return;
        }
        if (isOnGrapplingHookCooldown(player, variant)) {
            return;
        }
        if (!canReleaseGrapplingHook(grapplingHook)) {
            playFailedUse(player);
            addCooldown(player, variant);
            return;
        }

        GrapplingHookParcoolBlocker.block(player, Config.getHookUseBlockDurationTicks());
        GrapplingHookMissedTracker.clearMissed(player);
        float validatedAimYaw = Float.isFinite(aimYaw) ? Mth.wrapDegrees(aimYaw) : player.getYRot();
        float validatedAimPitch = Float.isFinite(aimPitch) ? Mth.clamp(aimPitch, -90.0F, 90.0F) : player.getXRot();
        facePlayerTowardAim(player, validatedAimYaw, validatedAimPitch);
        USE_SESSIONS.put(player.getUUID(), new UseSession(
                sequenceId,
                player.serverLevel().getGameTime(),
                variant,
                ropeExtensionLevel,
                grapplingShieldDisarmLevel,
                validatedAimYaw,
                validatedAimPitch,
                !player.onGround()));
        markConfiguredUse(player, variant);
        ServerPlayerPatch playerPatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (playerPatch != null) {
            playerPatch.playAnimationSynchronized(ModHookAnimations.HOOK_PULL, 0.0F);
        } else {
            shootFallbackHook(player, variant, sequenceId);
        }
        damageGrapplingHook(player, grapplingHook);

        addCooldown(player, variant);
    }

    private static void facePlayerTowardAim(ServerPlayer player, float aimYaw, float aimPitch) {
        player.setYRot(aimYaw);
        player.setXRot(aimPitch);
        player.setYHeadRot(aimYaw);
        player.yRotO = aimYaw;
        player.xRotO = aimPitch;
        player.yHeadRotO = aimYaw;
        player.yBodyRot = aimYaw;
        player.yBodyRotO = aimYaw;
    }

    private static void releaseUse(ServerPlayer player, int sequenceId) {
        UseSession session = USE_SESSIONS.get(player.getUUID());
        if (session == null || session.sequenceId != sequenceId || !session.keyDown) {
            return;
        }

        session.keyDown = false;
        session.releaseGameTime = player.serverLevel().getGameTime();
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info(
                    "[GrapplingHookUseDebug][SERVER] release owner={} sequence={} heldTicks={} swingAllowedAtPress={} onGround={} hookEntityId={}",
                    player.getId(),
                    sequenceId,
                    session.releaseGameTime - session.pressGameTime,
                    session.swingAllowedAtPress,
                    player.onGround(),
                    session.hookEntityId);
        }
    }

    private static void requestItemRetrieval(ServerPlayer player, int sequenceId) {
        UseSession session = USE_SESSIONS.get(player.getUUID());
        if (session == null
                || session.sequenceId != sequenceId
                || session.keyDown
                || session.itemRetrievalRequested) {
            return;
        }

        long currentGameTime = player.serverLevel().getGameTime();
        long heldTicks = Math.max(0L, session.releaseGameTime - session.pressGameTime);
        long releaseAge = currentGameTime - session.releaseGameTime;
        if (heldTicks >= Config.phantomSwingHoldThresholdTicks
                || releaseAge < 0L
                || releaseAge > ITEM_RETRIEVAL_DOUBLE_CLICK_WINDOW_TICKS) {
            return;
        }

        ItemStack grapplingHook = findEquippedGrapplingHook(player);
        if (ItemRetrievalEnchantment.getLevel(grapplingHook) <= 0) {
            return;
        }

        if (session.hookEntityId >= 0) {
            Entity entity = player.serverLevel().getEntity(session.hookEntityId);
            if (!(entity instanceof GrapplingHook hook)
                    || hook.getUseSequenceId() != sequenceId
                    || !hook.enableItemRetrieval()) {
                return;
            }
        }

        session.itemRetrievalRequested = true;
    }

    private static void requestEntityPullToOwner(ServerPlayer player, int sequenceId) {
        UseSession session = USE_SESSIONS.get(player.getUUID());
        if (session == null || session.sequenceId != sequenceId || session.keyDown || session.hookEntityId < 0) {
            return;
        }

        Entity entity = player.serverLevel().getEntity(session.hookEntityId);
        if (entity instanceof GrapplingHook hook && hook.getUseSequenceId() == sequenceId) {
            hook.requestEntityPullToOwner();
        }
    }

    private static void shootFallbackHook(ServerPlayer player, GrapplingHookVariant variant, int sequenceId) {
        Level level = player.level();
        GrapplingHook hook = new GrapplingHook(ModEntities.GRAPPLING_HOOK.get(), level);
        hook.setOwner(player);
        hook.setVariant(variant);
        configureSpawnedHook(player, hook);
        hook.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        shootConfiguredHook(player, hook);
        hook.captureSwingForwardDirectionFromVelocity();
        level.addFreshEntity(hook);
        registerSpawnedHook(player, hook, sequenceId);
    }

    public static void shootConfiguredHook(ServerPlayer player, GrapplingHook hook) {
        UseSession session = USE_SESSIONS.get(player.getUUID());
        GrapplingHookVariant variant = session == null ? hook.getVariant() : session.variant;
        float aimYaw = session == null ? player.getYRot() : session.aimYaw;
        float aimPitch = session == null ? player.getXRot() : session.aimPitch;
        double extraRange = hook.getRopeExtensionBonusBlocks();
        double projectileSpeed = variant.isPhantom()
                ? Config.getPhantomProjectileSpeed(extraRange)
                : Config.getProjectileSpeed(extraRange);
        if (variant.isPhantom()) {
            Vec3 aimDirection = Vec3.directionFromRotation(aimPitch, aimYaw);
            hook.shoot(
                    aimDirection.x,
                    aimDirection.y,
                    aimDirection.z,
                    (float) projectileSpeed,
                    (float) Config.projectileInaccuracy);
        } else {
            hook.shootFromRotation(
                    player,
                    aimPitch,
                    aimYaw,
                    0.0F,
                    (float) projectileSpeed,
                    (float) Config.projectileInaccuracy);
        }
        hook.setRetrievalReturnSpeed(hook.getDeltaMovement().length());
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info(
                    "[GrapplingHookAimDebug][SERVER] owner={} sequence={} packetYaw={} packetPitch={} serverYaw={} serverPitch={} velocity={}",
                    player.getId(),
                    session == null ? -1 : session.sequenceId,
                    aimYaw,
                    aimPitch,
                    player.getYRot(),
                    player.getXRot(),
                    hook.getDeltaMovement());
        }
    }

    public static void configureSpawnedHook(ServerPlayer player, GrapplingHook hook) {
        UseSession session = USE_SESSIONS.get(player.getUUID());
        if (session == null) {
            hook.configureUse(-1, false, 0);
            return;
        }

        hook.setVariant(session.variant);
        hook.configureUse(session.sequenceId, session.variant.isPhantom(), session.ropeExtensionLevel);
        hook.setGrapplingShieldDisarmEnabled(session.grapplingShieldDisarmLevel > 0);
        if (session.itemRetrievalRequested) {
            hook.enableItemRetrieval();
        }
    }

    public static void registerSpawnedHook(ServerPlayer player, GrapplingHook hook) {
        registerSpawnedHook(player, hook, hook.getUseSequenceId());
    }

    private static void registerSpawnedHook(ServerPlayer player, GrapplingHook hook, int sequenceId) {
        UseSession session = USE_SESSIONS.get(player.getUUID());
        if (session != null && session.sequenceId == sequenceId) {
            session.hookEntityId = hook.getId();
        }
    }

    public static HoldDecision getHoldDecision(ServerPlayer player, int sequenceId) {
        UseSession session = USE_SESSIONS.get(player.getUUID());
        if (session == null || session.sequenceId != sequenceId || !session.variant.isPhantom()) {
            return HoldDecision.NORMAL;
        }

        if (session.itemRetrievalRequested) {
            return HoldDecision.NORMAL;
        }

        if (!session.swingAllowedAtPress) {
            return HoldDecision.NORMAL;
        }

        long decisionTime = session.keyDown ? player.serverLevel().getGameTime() : session.releaseGameTime;
        long heldTicks = Math.max(0L, decisionTime - session.pressGameTime);
        if (heldTicks >= Config.phantomSwingHoldThresholdTicks) {
            if (!session.keyDown) {
                return HoldDecision.SWING_RELEASED;
            }
            return player.onGround() ? HoldDecision.PENDING : HoldDecision.SWING_HELD;
        }

        return session.keyDown ? HoldDecision.PENDING : HoldDecision.NORMAL;
    }

    public static boolean hasEquippedVariant(ServerPlayer player, GrapplingHookVariant variant) {
        ItemStack stack = findEquippedGrapplingHook(player);
        return !stack.isEmpty() && GrapplingHookVariant.fromStack(stack) == variant;
    }

    public static void finishUse(ServerPlayer player, int sequenceId, int hookEntityId, boolean refreshCooldown) {
        UseSession session = USE_SESSIONS.get(player.getUUID());
        if (session == null || session.sequenceId != sequenceId) {
            return;
        }
        if (session.hookEntityId >= 0 && hookEntityId >= 0 && session.hookEntityId != hookEntityId) {
            return;
        }

        USE_SESSIONS.remove(player.getUUID(), session);
        CONFIGURED_USES.remove(player.getUUID());
        CONFIGURED_USE_VARIANTS.remove(player.getUUID());
        if (refreshCooldown) {
            addCooldown(player, session.variant);
        }
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

    private static boolean hasActiveUseSession(ServerPlayer player) {
        UseSession session = USE_SESSIONS.get(player.getUUID());
        if (session == null) {
            return false;
        }

        if (session.hookEntityId >= 0 && player.serverLevel().getEntity(session.hookEntityId) == null) {
            finishUse(player, session.sequenceId, session.hookEntityId, false);
            return false;
        }

        long age = player.serverLevel().getGameTime() - session.pressGameTime;
        if (session.hookEntityId < 0 && age > CONFIGURED_USE_TTL_TICKS) {
            finishUse(player, session.sequenceId, session.hookEntityId, false);
            return false;
        }

        return true;
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
        if (player.getAbilities().instabuild || !stack.isDamageableItem()) {
            return;
        }

        int ropeRecoveryLevel = RopeRecoveryEnchantment.getLevel(stack);
        if (player.getRandom().nextFloat() < RopeRecoveryEnchantment.getChance(ropeRecoveryLevel)) {
            return;
        }

        stack.setDamageValue(Math.min(stack.getDamageValue() + 1, stack.getMaxDamage()));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static void addCooldown(ServerPlayer player, GrapplingHookVariant variant) {
        if (!variant.isPhantom() && Config.grapplingHookCooldown > 0) {
            player.getCooldowns().addCooldown(ModItems.GRAPPLING_HOOK.get(), Config.grapplingHookCooldown);
        }
    }

    private static boolean isOnGrapplingHookCooldown(ServerPlayer player, GrapplingHookVariant variant) {
        return !variant.isPhantom() && player.getCooldowns().isOnCooldown(ModItems.GRAPPLING_HOOK.get());
    }

    private static boolean canReleaseGrapplingHook(ItemStack stack) {
        return !stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage();
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

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        USE_SESSIONS.remove(playerId);
        CONFIGURED_USES.remove(playerId);
        CONFIGURED_USE_VARIANTS.remove(playerId);
    }

    private GrapplingHookUse() {
    }

    public enum HoldDecision {
        NORMAL,
        PENDING,
        SWING_HELD,
        SWING_RELEASED
    }

    private static final class UseSession {
        private final int sequenceId;
        private final long pressGameTime;
        private final GrapplingHookVariant variant;
        private final int ropeExtensionLevel;
        private final int grapplingShieldDisarmLevel;
        private final float aimYaw;
        private final float aimPitch;
        private final boolean swingAllowedAtPress;
        private boolean keyDown = true;
        private long releaseGameTime;
        private int hookEntityId = -1;
        private boolean itemRetrievalRequested;

        private UseSession(
                int sequenceId,
                long pressGameTime,
                GrapplingHookVariant variant,
                int ropeExtensionLevel,
                int grapplingShieldDisarmLevel,
                float aimYaw,
                float aimPitch,
                boolean swingAllowedAtPress) {
            this.sequenceId = sequenceId;
            this.pressGameTime = pressGameTime;
            this.releaseGameTime = pressGameTime;
            this.variant = variant;
            this.ropeExtensionLevel = ropeExtensionLevel;
            this.grapplingShieldDisarmLevel = grapplingShieldDisarmLevel;
            this.aimYaw = aimYaw;
            this.aimPitch = aimPitch;
            this.swingAllowedAtPress = swingAllowedAtPress;
        }
    }
}
