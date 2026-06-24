package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.com.epicawaken_grappling_hook.util.GrapplingHookForwardInputTracker;

public class SyncGrapplingHookForwardInputPacket {
    private final boolean forwardDown;

    public SyncGrapplingHookForwardInputPacket(boolean forwardDown) {
        this.forwardDown = forwardDown;
    }

    public static void encode(SyncGrapplingHookForwardInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.forwardDown);
    }

    public static SyncGrapplingHookForwardInputPacket decode(FriendlyByteBuf buffer) {
        return new SyncGrapplingHookForwardInputPacket(buffer.readBoolean());
    }

    public static void handle(SyncGrapplingHookForwardInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            GrapplingHookForwardInputTracker.update(sender, packet.forwardDown);
        }
        context.setPacketHandled(true);
    }
}
