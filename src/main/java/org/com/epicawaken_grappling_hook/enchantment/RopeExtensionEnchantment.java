package org.com.epicawaken_grappling_hook.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.com.epicawaken_grappling_hook.item.ModItems;

public class RopeExtensionEnchantment extends Enchantment {
    public RopeExtensionEnchantment() {
        super(
                Rarity.COMMON,
                EnchantmentCategory.BREAKABLE,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 1 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 15;
    }

    @Override
    public int getMaxLevel() {
        return 3;
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
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ROPE_EXTENSION.get(), stack);
    }

    public static int getBonusBlocks(int level) {
        return switch (level) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            default -> 0;
        };
    }

    private static boolean isGrapplingHook(ItemStack stack) {
        return stack.is(ModItems.GRAPPLING_HOOK.get()) || stack.is(ModItems.PHANTOM_GRAPPLING_HOOK.get());
    }
}
