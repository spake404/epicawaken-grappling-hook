package org.com.epicawaken_grappling_hook.client;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.Crawl;
import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.action.impl.HorizontalWallRun;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.utilities.BufferUtil;
import com.alrex.parcool.utilities.VectorUtil;
import com.alrex.parcool.utilities.WorldUtil;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.util.ParcoolCompat;

@OnlyIn(Dist.CLIENT)
public final class ClientGrapplingHookWallRunBridge {
    private static final int AIR_HOOK_WALL_RUN_WINDOW_TICKS = 8;
    private static final int WALL_RUN_BUFFER_BYTES = 128;

    private static int remainingAirHookWindowTicks;

    public static void openAirHookWindow() {
        if (!ParcoolCompat.isLoaded()) {
            return;
        }

        remainingAirHookWindowTicks = AIR_HOOK_WALL_RUN_WINDOW_TICKS;
        debug("open window ticks={}", remainingAirHookWindowTicks);
    }

    public static boolean hasOpenWindow() {
        return remainingAirHookWindowTicks > 0;
    }

    public static void tick() {
        if (!ParcoolCompat.isLoaded() || remainingAirHookWindowTicks <= 0) {
            return;
        }

        remainingAirHookWindowTicks--;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null) {
            clear("no_player");
            return;
        }

        if (!KeyBindings.getKeyHorizontalWallRun().isDown()) {
            return;
        }

        Parkourability parkourability = Parkourability.get(player);
        IStamina stamina = IStamina.get(player);
        if (parkourability == null || stamina == null) {
            clear("missing_capability");
            return;
        }

        HorizontalWallRun wallRun = parkourability.get(HorizontalWallRun.class);
        if (wallRun == null) {
            clear("missing_action");
            return;
        }

        if (wallRun.isDoing()) {
            clear("already_running");
            return;
        }

        ByteBuffer buffer = createHorizontalWallRunBuffer(player, parkourability, stamina);
        if (buffer == null) {
            return;
        }

        wallRun.start(player, parkourability, buffer, stamina);
        clear("started");
    }

    private static ByteBuffer createHorizontalWallRunBuffer(LocalPlayer player, Parkourability parkourability, IStamina stamina) {
        Vec3 wallDirection = WorldUtil.getRunnableWall(player, player.getBbWidth() * 0.65D);
        if (wallDirection == null) {
            debug("canStart=false reason=no_wall pos={} velocity={}", player.position(), player.getDeltaMovement());
            return null;
        }

        Vec3 wallNormal = wallDirection.normalize();
        Vec3 bodyDirection = VectorUtil.fromYawDegree(player.getYRot()).normalize();
        Vec3 localWallDirection = new Vec3(
                wallNormal.x * bodyDirection.x + wallNormal.z * bodyDirection.z,
                0.0D,
                -wallNormal.x * bodyDirection.z + wallNormal.z * bodyDirection.x).normalize();
        if (Math.abs(localWallDirection.z) < 0.9D) {
            debug("canStart=false reason=angle localWallDirection={} yaw={} pos={}", localWallDirection, player.getYRot(), player.position());
            return null;
        }

        if (!basicConditionsPass(player, parkourability, stamina)) {
            return null;
        }

        Vec3 runningDirection = wallNormal.yRot((float) (Math.PI * 0.5D));
        if (runningDirection.dot(bodyDirection) < 0.0D) {
            runningDirection = runningDirection.reverse();
        }

        ByteBuffer buffer = ByteBuffer.allocate(WALL_RUN_BUFFER_BYTES);
        BufferUtil.wrap(buffer).putBoolean(localWallDirection.z > 0.0D);
        buffer.putDouble(wallDirection.x);
        buffer.putDouble(wallDirection.z);
        buffer.putDouble(runningDirection.x);
        buffer.putDouble(runningDirection.z);
        return buffer;
    }

    private static boolean basicConditionsPass(LocalPlayer player, Parkourability parkourability, IStamina stamina) {
        WallJump wallJump = parkourability.get(WallJump.class);
        if (wallJump != null && wallJump.justJumped()) {
            debug("canStart=false reason=wall_jump_cooldown");
            return false;
        }

        Crawl crawl = parkourability.get(Crawl.class);
        Dodge dodge = parkourability.get(Dodge.class);
        Vault vault = parkourability.get(Vault.class);
        ClingToCliff cling = parkourability.get(ClingToCliff.class);
        if ((crawl != null && crawl.isDoing())
                || (dodge != null && dodge.isDoing())
                || (vault != null && vault.isDoing())
                || (cling != null && cling.isDoing())) {
            debug("canStart=false reason=conflicting_action");
            return false;
        }

        if (player.isInWaterOrBubble()
                || Math.abs(player.getDeltaMovement().y) >= 0.5D
                || player.onGround()
                || parkourability.getAdditionalProperties().getNotLandingTick() <= 5
                || stamina.isExhausted()) {
            debug("canStart=false reason=state onGround={} yVel={} notLandingTick={} exhausted={}",
                    player.onGround(),
                    player.getDeltaMovement().y,
                    parkourability.getAdditionalProperties().getNotLandingTick(),
                    stamina.isExhausted());
            return false;
        }

        return true;
    }

    private static void clear(String reason) {
        debug("clear reason={}", reason);
        remainingAirHookWindowTicks = 0;
    }

    private static void debug(String message, Object... args) {
        if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookWallRunBridge][CLIENT] " + message, args);
        }
    }

    private ClientGrapplingHookWallRunBridge() {
    }
}
