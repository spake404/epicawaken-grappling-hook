package org.com.epicawaken_grappling_hook.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class GrapplingHookCurioItem extends Item implements ICurioItem {
    public GrapplingHookCurioItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "charm".equals(slotContext.identifier()) || "curio".equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canEquip(slotContext, stack);
    }
}
