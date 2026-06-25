package org.com.epicawaken_grappling_hook.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.util.GrapplingHookMissedTracker;

public class SyncGrapplingHookMissedPacket {
    private final int entityId;

    public SyncGrapplingHookMissedPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(SyncGrapplingHookMissedPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static SyncGrapplingHookMissedPacket decode(FriendlyByteBuf buffer) {
        return new SyncGrapplingHookMissedPacket(buffer.readVarInt());
    }

    public static void handle(SyncGrapplingHookMissedPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                handleClient(packet.entityId);
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(int entityId) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if (entity != null) {
            if (Config.debugLogging) {
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][CLIENT] received missed packet entity={} gameTime={}",
                        entityId,
                        Minecraft.getInstance().level.getGameTime());
            }
            GrapplingHookMissedTracker.markMissed(entity);
        } else if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][CLIENT] received missed packet but entity not found entity={}", entityId);
        }
    }
}
