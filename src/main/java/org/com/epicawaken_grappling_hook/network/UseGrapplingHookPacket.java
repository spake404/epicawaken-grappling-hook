package org.com.epicawaken_grappling_hook.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.com.epicawaken_grappling_hook.util.GrapplingHookUse;

import java.util.function.Supplier;

public class UseGrapplingHookPacket {
    public static void encode(UseGrapplingHookPacket packet, FriendlyByteBuf buffer) {
    }

    public static UseGrapplingHookPacket decode(FriendlyByteBuf buffer) {
        return new UseGrapplingHookPacket();
    }

    public static void handle(UseGrapplingHookPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            GrapplingHookUse.tryUse(sender);
        }
        context.setPacketHandled(true);
    }
}
