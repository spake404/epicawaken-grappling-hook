package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class SyncGrapplingPullVelocityPacket {
    private final int entityId;
    private final Vec3 velocity;

    public SyncGrapplingPullVelocityPacket(int entityId, Vec3 velocity) {
        this.entityId = entityId;
        this.velocity = velocity;
    }

    public static void encode(SyncGrapplingPullVelocityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeDouble(packet.velocity.x);
        buffer.writeDouble(packet.velocity.y);
        buffer.writeDouble(packet.velocity.z);
    }

    public static SyncGrapplingPullVelocityPacket decode(FriendlyByteBuf buffer) {
        return new SyncGrapplingPullVelocityPacket(
                buffer.readVarInt(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(SyncGrapplingPullVelocityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        org.com.epicawaken_grappling_hook.client.ClientNetworkPacketHandlers.handlePullVelocity(
                                packet.entityId,
                                packet.velocity));
            }
        });
        context.setPacketHandled(true);
    }
}
