package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import org.com.epicawaken_grappling_hook.client.ClientGrapplingHookFovEffect;
import org.com.epicawaken_grappling_hook.client.ClientGrapplingHookSprintRestore;
import org.com.epicawaken_grappling_hook.util.GrapplingHookParcoolBlocker;

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
                ClientGrapplingHookSprintRestore.onFovStarted(packet.type);
                if (packet.type != GrapplingHookFovType.AIR) {
                    ClientGrapplingHookFovEffect.start();
                } else {
                    ClientGrapplingHookFovEffect.startAirFovHold();
                }
                GrapplingHookParcoolBlocker.block(Minecraft.getInstance().player, 80);
            }
        });
        context.setPacketHandled(true);
    }
}
