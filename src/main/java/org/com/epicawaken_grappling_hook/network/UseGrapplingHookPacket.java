package org.com.epicawaken_grappling_hook.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.com.epicawaken_grappling_hook.util.GrapplingHookUse;

import java.util.function.Supplier;

public class UseGrapplingHookPacket {
    private final Action action;
    private final int sequenceId;
    private final float aimYaw;
    private final float aimPitch;

    public UseGrapplingHookPacket(Action action, int sequenceId, float aimYaw, float aimPitch) {
        this.action = action;
        this.sequenceId = sequenceId;
        this.aimYaw = aimYaw;
        this.aimPitch = aimPitch;
    }

    public static void encode(UseGrapplingHookPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeVarInt(packet.sequenceId);
        buffer.writeFloat(packet.aimYaw);
        buffer.writeFloat(packet.aimPitch);
    }

    public static UseGrapplingHookPacket decode(FriendlyByteBuf buffer) {
        return new UseGrapplingHookPacket(
                buffer.readEnum(Action.class),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readFloat());
    }

    public static void handle(UseGrapplingHookPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            GrapplingHookUse.handleInput(sender, packet.action, packet.sequenceId, packet.aimYaw, packet.aimPitch);
        }
        context.setPacketHandled(true);
    }

    public enum Action {
        PRESS,
        RELEASE
    }
}
