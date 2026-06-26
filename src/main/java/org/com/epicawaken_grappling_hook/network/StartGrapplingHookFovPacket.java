package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class StartGrapplingHookFovPacket {
    private final GrapplingHookFovType type;

    public StartGrapplingHookFovPacket(GrapplingHookFovType type) {
        this.type = type;
    }

    public static void encode(StartGrapplingHookFovPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.type.ordinal());
    }

    public static StartGrapplingHookFovPacket decode(FriendlyByteBuf buffer) {
        return new StartGrapplingHookFovPacket(GrapplingHookFovType.fromOrdinal(buffer.readVarInt()));
    }

    public static void handle(StartGrapplingHookFovPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        org.com.epicawaken_grappling_hook.client.ClientNetworkPacketHandlers.handleStartFov(packet.type));
            }
        });
        context.setPacketHandled(true);
    }
}
