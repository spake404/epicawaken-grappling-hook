package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import org.com.epicawaken_grappling_hook.client.ClientGrapplingHookFovEffect;
import org.com.epicawaken_grappling_hook.client.ClientGrapplingHookSprintRestore;
import org.com.epicawaken_grappling_hook.client.ClientGrapplingHookWallRunBridge;
import org.com.epicawaken_grappling_hook.util.GrapplingHookParcoolBlocker;

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
                ClientGrapplingHookFovEffect.stop();
                if (packet.type == GrapplingHookFovType.AIR) {
                    ClientGrapplingHookFovEffect.startAirFovHoldTail();
                    ClientGrapplingHookWallRunBridge.openAirHookWindow();
                }
                ClientGrapplingHookSprintRestore.onFovStopped(packet.type);
                GrapplingHookParcoolBlocker.clear(Minecraft.getInstance().player);
            }
        });
        context.setPacketHandled(true);
    }
}
