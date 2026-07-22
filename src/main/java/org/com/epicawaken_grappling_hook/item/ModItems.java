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
            new GrapplingHookCurioItem(new Item.Properties().durability(64).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> PHANTOM_GRAPPLING_HOOK = ITEMS.register("phantom_grappling_hook", () ->
            new PhantomGrapplingHookCurioItem(new Item.Properties().durability(64).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> ROPE = ITEMS.register("rope", () ->
            new Item(new Item.Properties()));

    private ModItems() {
    }
}
