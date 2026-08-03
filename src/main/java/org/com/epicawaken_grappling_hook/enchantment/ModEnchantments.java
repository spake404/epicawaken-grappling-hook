package org.com.epicawaken_grappling_hook.enchantment;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Epicawaken_grappling_hook.MODID);

    public static final RegistryObject<Enchantment> ROPE_EXTENSION =
            ENCHANTMENTS.register("rope_extension", RopeExtensionEnchantment::new);

    public static final RegistryObject<Enchantment> ROPE_RECOVERY =
            ENCHANTMENTS.register("rope_recovery", RopeRecoveryEnchantment::new);

    public static final RegistryObject<Enchantment> ITEM_RETRIEVAL =
            ENCHANTMENTS.register("item_retrieval", ItemRetrievalEnchantment::new);

    public static final RegistryObject<Enchantment> GRAPPLING_SHIELD_DISARM =
            ENCHANTMENTS.register("grappling_shield_disarm", GrapplingShieldDisarmEnchantment::new);

    private ModEnchantments() {
    }
}
