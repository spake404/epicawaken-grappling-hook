package org.com.epicawaken_grappling_hook.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.com.epicawaken_grappling_hook.item.ModItems;

public class ItemRetrievalEnchantment extends Enchantment {
    public ItemRetrievalEnchantment() {
        super(
                Rarity.RARE,
                EnchantmentCategory.BREAKABLE,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 15;
    }

    @Override
    public int getMaxCost(int level) {
        return 40;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    @Override
    public boolean isTradeable() {
        return true;
    }

    @Override
    public boolean isAllowedOnBooks() {
        return true;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return isGrapplingHook(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return isGrapplingHook(stack);
    }

    public static int getLevel(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ITEM_RETRIEVAL.get(), stack);
    }

    private static boolean isGrapplingHook(ItemStack stack) {
        return stack.is(ModItems.GRAPPLING_HOOK.get()) || stack.is(ModItems.PHANTOM_GRAPPLING_HOOK.get());
    }
}
