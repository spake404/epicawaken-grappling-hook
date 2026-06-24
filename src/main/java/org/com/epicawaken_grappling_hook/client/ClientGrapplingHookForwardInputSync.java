package org.com.epicawaken_grappling_hook.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.SyncGrapplingHookForwardInputPacket;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHook;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class ClientGrapplingHookForwardInputSync {
    private static final int RESEND_INTERVAL_TICKS = 5;

    private static boolean hasLastState;
    private static boolean lastForwardDown;
    private static int resendCooldown;

    private ClientGrapplingHookForwardInputSync() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            reset();
            return;
        }

        boolean active = hasLocalGrapplingHook(minecraft, player) || isHookAnimation(player);
        if (!active) {
            if (hasLastState && lastForwardDown) {
                send(false);
            }
            reset();
            return;
        }

        boolean forwardDown = minecraft.options.keyUp.isDown();
        resendCooldown--;
        if (!hasLastState || forwardDown != lastForwardDown || resendCooldown <= 0) {
            send(forwardDown);
            hasLastState = true;
            lastForwardDown = forwardDown;
            resendCooldown = RESEND_INTERVAL_TICKS;
        }
    }

    private static boolean hasLocalGrapplingHook(Minecraft minecraft, LocalPlayer player) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof GrapplingHook grapplingHook && grapplingHook.getOwner() == player) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHookAnimation(LocalPlayer player) {
        LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
        if (entityPatch == null) {
            return false;
        }
        AnimationPlayer animationPlayer = entityPatch.getAnimator().getPlayerFor(null);
        if (animationPlayer == null) {
            return false;
        }
        return isHookAnimation(animationPlayer.getRealAnimation()) || isHookAnimation(animationPlayer.getAnimation());
    }

    private static boolean isHookAnimation(AssetAccessor<?> animation) {
        if (animation == null) {
            return false;
        }
        String name = String.valueOf(animation.registryName());
        return name.endsWith("/hook_pull") || name.endsWith("/hook_air");
    }

    private static void send(boolean forwardDown) {
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new SyncGrapplingHookForwardInputPacket(forwardDown));
    }

    private static void reset() {
        hasLastState = false;
        lastForwardDown = false;
        resendCooldown = 0;
    }
}
