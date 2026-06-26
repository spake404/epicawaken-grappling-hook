package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class SyncGrapplingHookArrivalPacket {
    private final int entityId;

    public SyncGrapplingHookArrivalPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(SyncGrapplingHookArrivalPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static SyncGrapplingHookArrivalPacket decode(FriendlyByteBuf buffer) {
        return new SyncGrapplingHookArrivalPacket(buffer.readVarInt());
    }

    public static void handle(SyncGrapplingHookArrivalPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        org.com.epicawaken_grappling_hook.client.ClientNetworkPacketHandlers.handleArrival(packet.entityId));
            }
        });
        context.setPacketHandled(true);
    }
}
