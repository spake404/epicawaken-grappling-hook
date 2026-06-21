package org.com.epicawaken_grappling_hook.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Epicawaken_grappling_hook.MODID);

    public static final RegistryObject<Item> GRAPPLING_HOOK = ITEMS.register("grappling_hook", () ->
            new GrapplingHookCurioItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    private ModItems() {
    }
}
