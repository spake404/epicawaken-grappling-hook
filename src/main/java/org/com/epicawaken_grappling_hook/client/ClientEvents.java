package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import org.com.epicawaken_grappling_hook.Config;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.entity.ModEntities;
import org.com.epicawaken_grappling_hook.item.ModItems;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.network.UseGrapplingHookPacket;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHookRenderer;
import org.com.epicawaken_grappling_hook.util.GrapplingHookParcoolBlocker;
import org.lwjgl.glfw.GLFW;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public class ClientEvents {
    public static final KeyMapping USE_GRAPPLING_HOOK = new KeyMapping(
            "key.epicawaken_grappling_hook.use_grappling_hook",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories.epicawaken_grappling_hook");

    private ClientEvents() {
    }

    @Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(USE_GRAPPLING_HOOK);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.GRAPPLING_HOOK.get(), GrapplingHookRenderer::new);
        }

        @SubscribeEvent
        public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
            event.register(GrapplingHookCurioRenderer.ARM_MODEL);
            event.register(GrapplingHookRenderer.PROJECTILE_MODEL);
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> CuriosRendererRegistry.register(ModItems.GRAPPLING_HOOK.get(), GrapplingHookCurioRenderer::new));
        }
    }

    @Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            while (USE_GRAPPLING_HOOK.consumeClick()) {
                GrapplingHookParcoolBlocker.block(net.minecraft.client.Minecraft.getInstance().player, 8);
                ClientGrapplingHookSprintRestore.recordUseAttempt();
                ClientGrapplingHookFovEffect.recordUseAttempt();
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new UseGrapplingHookPacket());
            }
            if (ClientGrapplingHookSprintRestore.hasWork()) {
                ClientGrapplingHookSprintRestore.tick();
            }
            if (ClientGrapplingHookWallRunBridge.hasOpenWindow()) {
                ClientGrapplingHookWallRunBridge.tick();
            }
            if (Config.debugLogging) {
                ClientGrapplingHookDebugLogger.tick();
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onComputeFovModifier(ComputeFovModifierEvent event) {
            ClientGrapplingHookFovEffect.onComputeFovModifier(event);
        }
    }
}
