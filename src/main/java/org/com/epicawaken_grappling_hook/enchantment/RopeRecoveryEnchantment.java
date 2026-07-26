package org.com.epicawaken_grappling_hook.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.com.epicawaken_grappling_hook.item.ModItems;

public class RopeRecoveryEnchantment extends Enchantment {
    public RopeRecoveryEnchantment() {
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
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ROPE_RECOVERY.get(), stack);
    }

    public static float getChance(int level) {
        return switch (level) {
            case 1 -> 0.20F;
            case 2 -> 0.40F;
            case 3 -> 0.60F;
            default -> 0.0F;
        };
    }

    private static boolean isGrapplingHook(ItemStack stack) {
        return stack.is(ModItems.GRAPPLING_HOOK.get()) || stack.is(ModItems.PHANTOM_GRAPPLING_HOOK.get());
    }
}
