package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.client.ClientGrapplingHookUseTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookMissedTracker;
import org.com.epicawaken_grappling_hook.util.GrapplingHookParcoolBlocker;

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
                ClientGrapplingHookUseTracker.markConfiguredUse(packet.entityId);
                if (Minecraft.getInstance().level != null) {
                    Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId);
                    GrapplingHookMissedTracker.clearMissed(entity);
                    GrapplingHookParcoolBlocker.block(entity, Config.maxLifeTicks + Config.getHookLockDelayTicks() + 20);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
