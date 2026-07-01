package org.com.epicawaken_grappling_hook.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.com.epicawaken_grappling_hook.item.ModItems;

import java.util.Optional;

final class GrapplingHookEquipmentLookup {
    private GrapplingHookEquipmentLookup() {
    }

    static Optional<Entry> findVisible(Player player) {
        Optional<GrapplingHookCurioLookup.Entry> curioEntry = GrapplingHookCurioLookup.findVisible(player);
        if (curioEntry.isPresent()) {
            return Optional.of(new Entry(curioEntry.get().stack(), Source.CURIOS));
        }

        ItemStack offhandStack = GrapplingHookOffhandRenderState.getRenderableOffhandStack(player);
        if (isGrapplingHookStack(offhandStack)) {
            return Optional.of(new Entry(offhandStack, Source.OFFHAND));
        }

        return Optional.empty();
    }

    static boolean hasVisibleCurio(Player player) {
        return GrapplingHookCurioLookup.findVisible(player).isPresent();
    }

    static boolean isGrapplingHookStack(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.GRAPPLING_HOOK.get());
    }

    enum Source {
        CURIOS,
        OFFHAND
    }

    record Entry(ItemStack stack, Source source) {
    }
}
