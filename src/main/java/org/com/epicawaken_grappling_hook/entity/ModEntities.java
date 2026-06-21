package org.com.epicawaken_grappling_hook.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHook;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Epicawaken_grappling_hook.MODID);

    public static final RegistryObject<EntityType<GrapplingHook>> GRAPPLING_HOOK = ENTITY_TYPES.register("grappling_hook", () ->
            EntityType.Builder.<GrapplingHook>of(GrapplingHook::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .build("grappling_hook"));

    private ModEntities() {
    }
}
