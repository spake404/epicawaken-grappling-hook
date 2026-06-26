package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class SyncConfiguredUsePacket {
    private final int entityId;

    public SyncConfiguredUsePacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(SyncConfiguredUsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static SyncConfiguredUsePacket decode(FriendlyByteBuf buffer) {
        return new SyncConfiguredUsePacket(buffer.readVarInt());
    }

    public static void handle(SyncConfiguredUsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        org.com.epicawaken_grappling_hook.client.ClientNetworkPacketHandlers.handleConfiguredUse(packet.entityId));
            }
        });
        context.setPacketHandled(true);
    }
}
