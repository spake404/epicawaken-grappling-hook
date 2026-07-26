package org.com.epicawaken_grappling_hook.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class GrapplingHookCurioItem extends Item implements ICurioItem {
    public GrapplingHookCurioItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.epicawaken_grappling_hook.usage").withStyle(ChatFormatting.AQUA));
        this.appendUsageTooltip(tooltip);
    }

    protected void appendUsageTooltip(List<Component> tooltip) {
        addUsageLine(tooltip, "tooltip.epicawaken_grappling_hook.grappling_hook.ground");
        addUsageLine(tooltip, "tooltip.epicawaken_grappling_hook.grappling_hook.wall");
        addUsageLine(tooltip, "tooltip.epicawaken_grappling_hook.grappling_hook.wall_safe");
        addUsageLine(tooltip, "tooltip.epicawaken_grappling_hook.grappling_hook.entity");
        addUsageLine(tooltip, "tooltip.epicawaken_grappling_hook.grappling_hook.entity_immovable");
        addUsageLine(tooltip, "tooltip.epicawaken_grappling_hook.grappling_hook.miss");
    }

    protected static void addUsageLine(List<Component> tooltip, String translationKey) {
        tooltip.add(Component.translatable(translationKey).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "glove".equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canEquip(slotContext, stack);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.ROPE.get()) || super.isValidRepairItem(stack, repairCandidate);
    }
}
