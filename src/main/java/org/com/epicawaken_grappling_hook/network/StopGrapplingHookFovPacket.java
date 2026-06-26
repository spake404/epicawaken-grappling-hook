package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class StopGrapplingHookFovPacket {
    private final GrapplingHookFovType type;

    public StopGrapplingHookFovPacket(GrapplingHookFovType type) {
        this.type = type;
    }

    public static void encode(StopGrapplingHookFovPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.type.ordinal());
    }

    public static StopGrapplingHookFovPacket decode(FriendlyByteBuf buffer) {
        return new StopGrapplingHookFovPacket(GrapplingHookFovType.fromOrdinal(buffer.readVarInt()));
    }

    public static void handle(StopGrapplingHookFovPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        org.com.epicawaken_grappling_hook.client.ClientNetworkPacketHandlers.handleStopFov(packet.type));
            }
        });
        context.setPacketHandled(true);
    }
}
