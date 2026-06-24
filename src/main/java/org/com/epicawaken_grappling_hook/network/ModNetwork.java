package org.com.epicawaken_grappling_hook.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId;

    public static void register() {
        CHANNEL.messageBuilder(UseGrapplingHookPacket.class, packetId++)
                .encoder(UseGrapplingHookPacket::encode)
                .decoder(UseGrapplingHookPacket::decode)
                .consumerMainThread(UseGrapplingHookPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncConfiguredUsePacket.class, packetId++)
                .encoder(SyncConfiguredUsePacket::encode)
                .decoder(SyncConfiguredUsePacket::decode)
                .consumerMainThread(SyncConfiguredUsePacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncGrapplingHookArrivalPacket.class, packetId++)
                .encoder(SyncGrapplingHookArrivalPacket::encode)
                .decoder(SyncGrapplingHookArrivalPacket::decode)
                .consumerMainThread(SyncGrapplingHookArrivalPacket::handle)
                .add();
        CHANNEL.messageBuilder(StartGrapplingHookFovPacket.class, packetId++)
                .encoder(StartGrapplingHookFovPacket::encode)
                .decoder(StartGrapplingHookFovPacket::decode)
                .consumerMainThread(StartGrapplingHookFovPacket::handle)
                .add();
        CHANNEL.messageBuilder(StopGrapplingHookFovPacket.class, packetId++)
                .encoder(StopGrapplingHookFovPacket::encode)
                .decoder(StopGrapplingHookFovPacket::decode)
                .consumerMainThread(StopGrapplingHookFovPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncGrapplingHookForwardInputPacket.class, packetId++)
                .encoder(SyncGrapplingHookForwardInputPacket::encode)
                .decoder(SyncGrapplingHookForwardInputPacket::decode)
                .consumerMainThread(SyncGrapplingHookForwardInputPacket::handle)
                .add();
    }

    private ModNetwork() {
    }
}
