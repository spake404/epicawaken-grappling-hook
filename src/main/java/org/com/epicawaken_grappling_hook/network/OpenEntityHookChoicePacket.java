package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class OpenEntityHookChoicePacket {
    private final int sequenceId;
    private final int windowTicks;

    public OpenEntityHookChoicePacket(int sequenceId, int windowTicks) {
        this.sequenceId = sequenceId;
        this.windowTicks = windowTicks;
    }

    public static void encode(OpenEntityHookChoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sequenceId);
        buffer.writeVarInt(packet.windowTicks);
    }

    public static OpenEntityHookChoicePacket decode(FriendlyByteBuf buffer) {
        return new OpenEntityHookChoicePacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(OpenEntityHookChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        org.com.epicawaken_grappling_hook.client.ClientNetworkPacketHandlers.handleEntityHookChoice(
                                packet.sequenceId,
                                packet.windowTicks));
            }
        });
        context.setPacketHandled(true);
    }
}
