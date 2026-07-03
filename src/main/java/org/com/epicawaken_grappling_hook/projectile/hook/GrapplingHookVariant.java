package org.com.epicawaken_grappling_hook.projectile.hook;

import net.minecraft.world.item.ItemStack;
import org.com.epicawaken_grappling_hook.item.ModItems;

public enum GrapplingHookVariant {
    NORMAL,
    PHANTOM;

    public static GrapplingHookVariant fromId(int id) {
        GrapplingHookVariant[] values = values();
        return id >= 0 && id < values.length ? values[id] : NORMAL;
    }

    public static GrapplingHookVariant fromStack(ItemStack stack) {
        return stack.is(ModItems.PHANTOM_GRAPPLING_HOOK.get()) ? PHANTOM : NORMAL;
    }

    public boolean isPhantom() {
        return this == PHANTOM;
    }
}
