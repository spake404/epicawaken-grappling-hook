package org.com.epicawaken_grappling_hook.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

final class GrapplingHookOffhandRenderState {
    private static final Map<Player, ItemStack> HIDDEN_OFFHAND_STACKS = new IdentityHashMap<>();

    private GrapplingHookOffhandRenderState() {
    }

    static void hideForPlayerRender(Player player) {
        ItemStack offhandStack = player.getOffhandItem();
        if (!GrapplingHookEquipmentLookup.isGrapplingHookStack(offhandStack) || HIDDEN_OFFHAND_STACKS.containsKey(player)) {
            return;
        }

        HIDDEN_OFFHAND_STACKS.put(player, offhandStack);
        player.getInventory().offhand.set(0, ItemStack.EMPTY);
    }

    static void restoreAfterPlayerRender(Player player) {
        ItemStack hiddenStack = HIDDEN_OFFHAND_STACKS.remove(player);
        if (hiddenStack != null) {
            player.getInventory().offhand.set(0, hiddenStack);
        }
    }

    static void restoreAll() {
        HIDDEN_OFFHAND_STACKS.forEach((player, stack) -> player.getInventory().offhand.set(0, stack));
        HIDDEN_OFFHAND_STACKS.clear();
    }

    static ItemStack getRenderableOffhandStack(Player player) {
        ItemStack hiddenStack = HIDDEN_OFFHAND_STACKS.get(player);
        return hiddenStack != null ? hiddenStack : player.getOffhandItem();
    }
}
