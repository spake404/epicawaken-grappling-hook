package org.com.epicawaken_grappling_hook.util;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.item.ModItems;

@Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GrapplingHookRepairEvents {
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!isRepairableGrapplingHook(left) || !right.is(ModItems.ROPE.get()) || !left.isDamaged()) {
            return;
        }

        int repairAmount = Math.min(left.getDamageValue(), right.getCount());
        if (repairAmount <= 0) {
            return;
        }

        ItemStack result = left.copy();
        result.setCount(1);
        result.setDamageValue(left.getDamageValue() - repairAmount);
        event.setOutput(result);
        event.setMaterialCost(repairAmount);
        event.setCost(1);
    }

    private static boolean isRepairableGrapplingHook(ItemStack stack) {
        return stack.is(ModItems.GRAPPLING_HOOK.get()) || stack.is(ModItems.PHANTOM_GRAPPLING_HOOK.get());
    }

    private GrapplingHookRepairEvents() {
    }
}
