package org.com.epicawaken_grappling_hook;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.com.epicawaken_grappling_hook.animation.ModHookAnimations;
import org.com.epicawaken_grappling_hook.entity.ModEntities;
import org.com.epicawaken_grappling_hook.item.ModItems;
import org.com.epicawaken_grappling_hook.network.ModNetwork;
import org.com.epicawaken_grappling_hook.util.AwakenAnimationHooks;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

@Mod(Epicawaken_grappling_hook.MODID)
public class Epicawaken_grappling_hook {
    public static final String MODID = "epicawaken_grappling_hook";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.GRAPPLING_HOOK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(ModItems.GRAPPLING_HOOK.get()))
                    .build());

    public Epicawaken_grappling_hook() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(ModHookAnimations::registerAnimations);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CuriosApi.registerCurio(ModItems.GRAPPLING_HOOK.get(), (ICurioItem) ModItems.GRAPPLING_HOOK.get());
            AwakenAnimationHooks.register();
            ModNetwork.register();
        });
    }
}
