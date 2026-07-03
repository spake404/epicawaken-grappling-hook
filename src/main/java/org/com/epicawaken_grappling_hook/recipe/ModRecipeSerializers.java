package org.com.epicawaken_grappling_hook.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Epicawaken_grappling_hook.MODID);

    public static final RegistryObject<RecipeSerializer<GrapplingHookRepairRecipe>> GRAPPLING_HOOK_REPAIR =
            RECIPE_SERIALIZERS.register("grappling_hook_repair", () ->
                    new SimpleCraftingRecipeSerializer<>(GrapplingHookRepairRecipe::new));

    private ModRecipeSerializers() {
    }
}
