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
import org.com.epicawaken_grappling_hook.util.GrapplingHookArrivalTracker;

public class SyncGrapplingHookArrivalPacket {
    private final int entityId;

    public SyncGrapplingHookArrivalPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(SyncGrapplingHookArrivalPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static SyncGrapplingHookArrivalPacket decode(FriendlyByteBuf buffer) {
        return new SyncGrapplingHookArrivalPacket(buffer.readVarInt());
    }

    public static void handle(SyncGrapplingHookArrivalPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
                Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][CLIENT] received arrival packet entity={} gameTime={}", entityId, Minecraft.getInstance().level.getGameTime());
            }
            GrapplingHookArrivalTracker.markArrived(entity);
        } else if (Config.debugLogging) {
            Epicawaken_grappling_hook.LOGGER.info("[GrapplingHookDebug][CLIENT] received arrival packet but entity not found entity={}", entityId);
        }
    }
}
