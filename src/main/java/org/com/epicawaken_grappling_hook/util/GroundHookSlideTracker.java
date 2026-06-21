package org.com.epicawaken_grappling_hook.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.network.GrapplingHookFovType;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.StartGrapplingHookFovPacket;
import org.com.epicawaken_grappling_hook.network.StopGrapplingHookFovPacket;

@Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID)
public final class GroundHookSlideTracker {
    private static final Map<UUID, SlideState> SLIDES = new HashMap<>();

    private GroundHookSlideTracker() {
    }

    public static boolean start(Entity entity, Vec3 preferredDirection) {
        if (!(entity instanceof ServerPlayer player) || !Config.groundHookSlideEnabled) {
            return false;
        }

        Vec3 currentHorizontalVelocity = horizontal(player.getDeltaMovement());
        Vec3 preferredHorizontalVelocity = horizontal(preferredDirection);
        Vec3 direction = preferredHorizontalVelocity.lengthSqr() > currentHorizontalVelocity.lengthSqr()
                ? preferredHorizontalVelocity
                : currentHorizontalVelocity;
        if (direction.lengthSqr() < 1.0E-6D) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] slide start skipped owner={} reason=no_direction ownerVelocity={}",
                        player.getId(),
                        player.getDeltaMovement());
            }
            return false;
        }

        double currentSpeed = currentHorizontalVelocity.length();
        double preferredSpeed = preferredHorizontalVelocity.length();
        double speed = Math.max(currentSpeed, preferredSpeed);
        if (speed <= 1.0E-6D) {
            speed = Config.groundHookSlideInitialSpeed;
        }

        SlideState state = new SlideState(direction.normalize(), speed, Config.groundHookSlideDurationTicks);
        SLIDES.put(player.getUUID(), state);
        GrapplingHookParcoolBlocker.block(player, Config.groundHookSlideDurationTicks + 2);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StartGrapplingHookFovPacket(GrapplingHookFovType.GROUND));
        applySlideVelocity(player, state);

        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] slide started owner={} direction={} currentSpeed={} preferredSpeed={} slideSpeed={} duration={} ownerPos={} ownerVelocity={}",
                    player.getId(),
                    state.direction,
                    currentSpeed,
                    preferredSpeed,
                    speed,
                    Config.groundHookSlideDurationTicks,
                    player.position(),
                    player.getDeltaMovement());
        }
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || SLIDES.isEmpty() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        SlideState state = SLIDES.get(player.getUUID());
        if (state != null) {
            tickSlide(player, state);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SLIDES.remove(event.getEntity().getUUID());
    }

    private static void tickSlide(ServerPlayer player, SlideState state) {
        if (state.remainingTicks-- <= 0 || state.speed < Config.groundHookSlideMinSpeed || player.isShiftKeyDown()) {
            SLIDES.remove(player.getUUID());
            GrapplingHookParcoolBlocker.clear(player);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StopGrapplingHookFovPacket(GrapplingHookFovType.GROUND));
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] slide stopped owner={} remainingTicks={} speed={} minSpeed={} ownerPos={} ownerVelocity={}",
                        player.getId(),
                        state.remainingTicks,
                        state.speed,
                        Config.groundHookSlideMinSpeed,
                        player.position(),
                        player.getDeltaMovement());
            }
            return;
        }

        applySlideVelocity(player, state);
        GrapplingHookParcoolBlocker.block(player, 2);
        state.speed *= Config.groundHookSlideFriction;
    }

    private static void applySlideVelocity(ServerPlayer player, SlideState state) {
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(state.direction.x * state.speed, current.y, state.direction.z * state.speed);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookSlideDebug][SERVER] slide tick owner={} remainingTicks={} speed={} direction={} oldVelocity={} newVelocity={} ownerPos={}",
                    player.getId(),
                    state.remainingTicks,
                    state.speed,
                    state.direction,
                    current,
                    player.getDeltaMovement(),
                    player.position());
        }
    }

    private static Vec3 horizontal(Vec3 vec3) {
        return new Vec3(vec3.x, 0.0D, vec3.z);
    }

    private static final class SlideState {
        private final Vec3 direction;
        private double speed;
        private int remainingTicks;

        private SlideState(Vec3 direction, double speed, int remainingTicks) {
            this.direction = direction;
            this.speed = speed;
            this.remainingTicks = remainingTicks;
        }
    }
}
