package org.com.epicawaken_grappling_hook.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.network.PacketDistributor;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.SyncGrapplingHookHoldInputPacket;

public final class ClientGrapplingHookHoldInputSync {
    private static boolean lastUseDown;

    private ClientGrapplingHookHoldInputSync() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            lastUseDown = false;
            return;
        }

        boolean useDown = ClientEvents.USE_GRAPPLING_HOOK.isDown();
        if (useDown != lastUseDown) {
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new SyncGrapplingHookHoldInputPacket(useDown));
            lastUseDown = useDown;
        }
    }
}
