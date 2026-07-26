package org.com.epicawaken_grappling_hook.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.com.epicawaken_grappling_hook.item.ModItems;
import org.jetbrains.annotations.NotNull;

public class PhantomGrapplingHookCraftRecipe extends ShapedRecipe {
    private PhantomGrapplingHookCraftRecipe(ResourceLocation id, ShapedRecipe shapedRecipe) {
        super(
                id,
                shapedRecipe.getGroup(),
                shapedRecipe.category(),
                shapedRecipe.getWidth(),
                shapedRecipe.getHeight(),
                shapedRecipe.getIngredients(),
                new ItemStack(ModItems.PHANTOM_GRAPPLING_HOOK.get()),
                shapedRecipe.showNotification());
    }

    @Override
    @NotNull
    public ItemStack assemble(CraftingContainer container, @NotNull RegistryAccess registryAccess) {
        ItemStack sourceHook = ItemStack.EMPTY;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(ModItems.GRAPPLING_HOOK.get())) {
                sourceHook = stack;
                break;
            }
        }

        if (sourceHook.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(ModItems.PHANTOM_GRAPPLING_HOOK.get());
        CompoundTag sourceTag = sourceHook.getTag();
        if (sourceTag != null) {
            result.setTag(sourceTag.copy());
        }
        result.setDamageValue(Math.min(sourceHook.getDamageValue(), result.getMaxDamage()));
        return result;
    }

    @Override
    @NotNull
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.PHANTOM_GRAPPLING_HOOK_CRAFT.get();
    }

    public static class Serializer implements RecipeSerializer<PhantomGrapplingHookCraftRecipe> {
        @Override
        @NotNull
        public PhantomGrapplingHookCraftRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            ShapedRecipe shapedRecipe = RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
            return new PhantomGrapplingHookCraftRecipe(id, shapedRecipe);
        }

        @Override
        @NotNull
        public PhantomGrapplingHookCraftRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buffer) {
            ShapedRecipe shapedRecipe = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer);
            return new PhantomGrapplingHookCraftRecipe(id, shapedRecipe);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull PhantomGrapplingHookCraftRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
        }
    }
}
