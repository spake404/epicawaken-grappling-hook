package org.com.epicawaken_grappling_hook.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.network.PacketDistributor;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.SyncGrapplingHookJumpInputPacket;

public final class ClientGrapplingHookJumpInputSync {
    private static boolean lastJumpDown;

    private ClientGrapplingHookJumpInputSync() {
    }

    public static void tick() {
        if (!Config.airHookArrivalJumpEnabled) {
            lastJumpDown = false;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            lastJumpDown = false;
            return;
        }

        boolean jumpDown = minecraft.options.keyJump.isDown();
        if (jumpDown && !lastJumpDown) {
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new SyncGrapplingHookJumpInputPacket());
        }
        lastJumpDown = jumpDown;
    }
}
