package org.com.epicawaken_grappling_hook.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.com.epicawaken_grappling_hook.item.ModItems;
import org.jetbrains.annotations.NotNull;

public class GrapplingHookRepairRecipe extends CustomRecipe {
    public GrapplingHookRepairRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, @NotNull Level level) {
        RepairInputs inputs = findInputs(container);
        return inputs.grapplingHookCount == 1
                && inputs.ropeCount >= inputs.grapplingHook.getDamageValue()
                && inputs.invalidCount == 0
                && inputs.grapplingHook.isDamaged();
    }

    @Override
    @NotNull
    public ItemStack assemble(CraftingContainer container, @NotNull RegistryAccess registryAccess) {
        RepairInputs inputs = findInputs(container);
        if (inputs.grapplingHookCount != 1
                || inputs.invalidCount > 0
                || !inputs.grapplingHook.isDamaged()
                || inputs.ropeCount < inputs.grapplingHook.getDamageValue()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = inputs.grapplingHook.copy();
        result.setCount(1);
        result.setDamageValue(0);
        return result;
    }

    @Override
    @NotNull
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        RepairInputs inputs = findInputs(container);
        if (inputs.grapplingHookCount != 1
                || inputs.invalidCount > 0
                || !inputs.grapplingHook.isDamaged()
                || inputs.ropeCount < inputs.grapplingHook.getDamageValue()) {
            return remainingItems;
        }

        int ropesToConsume = inputs.grapplingHook.getDamageValue();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.is(ModItems.ROPE.get())) {
                continue;
            }

            int consumedFromSlot = Math.min(ropesToConsume, stack.getCount());
            ropesToConsume -= consumedFromSlot;
            if (consumedFromSlot == 0) {
                remainingItems.set(i, stack.copyWithCount(1));
            } else if (consumedFromSlot > 1) {
                stack.shrink(consumedFromSlot - 1);
            }
        }

        return remainingItems;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    @NotNull
    public ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return new ItemStack(ModItems.GRAPPLING_HOOK.get());
    }

    @Override
    @NotNull
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.GRAPPLING_HOOK_REPAIR.get();
    }

    private static RepairInputs findInputs(CraftingContainer container) {
        ItemStack grapplingHook = ItemStack.EMPTY;
        int grapplingHookCount = 0;
        int ropeCount = 0;
        int invalidCount = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (isRepairableGrapplingHook(stack)) {
                grapplingHook = stack;
                grapplingHookCount++;
            } else if (stack.is(ModItems.ROPE.get())) {
                ropeCount += stack.getCount();
            } else {
                invalidCount++;
            }
        }

        return new RepairInputs(grapplingHook, grapplingHookCount, ropeCount, invalidCount);
    }

    private static boolean isRepairableGrapplingHook(ItemStack stack) {
        return stack.is(ModItems.GRAPPLING_HOOK.get()) || stack.is(ModItems.PHANTOM_GRAPPLING_HOOK.get());
    }

    private record RepairInputs(ItemStack grapplingHook, int grapplingHookCount, int ropeCount, int invalidCount) {
    }
}
