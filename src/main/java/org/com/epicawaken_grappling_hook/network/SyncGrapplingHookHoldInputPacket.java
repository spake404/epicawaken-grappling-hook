package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.com.epicawaken_grappling_hook.util.GrapplingHookSwingTracker;

public class SyncGrapplingHookHoldInputPacket {
    private final boolean holding;

    public SyncGrapplingHookHoldInputPacket(boolean holding) {
        this.holding = holding;
    }

    public static void encode(SyncGrapplingHookHoldInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.holding);
    }

    public static SyncGrapplingHookHoldInputPacket decode(FriendlyByteBuf buffer) {
        return new SyncGrapplingHookHoldInputPacket(buffer.readBoolean());
    }

    public static void handle(SyncGrapplingHookHoldInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            GrapplingHookSwingTracker.setHolding(sender, packet.holding);
        }
        context.setPacketHandled(true);
    }
}
