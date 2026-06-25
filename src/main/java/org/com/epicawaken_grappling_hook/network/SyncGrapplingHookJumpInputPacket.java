package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.com.epicawaken_grappling_hook.util.AirHookArrivalJumpTracker;

public class SyncGrapplingHookJumpInputPacket {
    public static void encode(SyncGrapplingHookJumpInputPacket packet, FriendlyByteBuf buffer) {
    }

    public static SyncGrapplingHookJumpInputPacket decode(FriendlyByteBuf buffer) {
        return new SyncGrapplingHookJumpInputPacket();
    }

    public static void handle(SyncGrapplingHookJumpInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            AirHookArrivalJumpTracker.tryStartFromJump(sender);
        }
        context.setPacketHandled(true);
    }
}
