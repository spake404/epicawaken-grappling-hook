package org.com.epicawaken_grappling_hook.client;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.com.epicawaken_grappling_hook.item.ModItems;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Optional;

final class GrapplingHookCurioLookup {
    private GrapplingHookCurioLookup() {
    }

    static Optional<Entry> findVisible(Player player) {
        Entry[] result = new Entry[1];
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getCurios().forEach((slotId, stacksHandler) -> {
            if (result[0] != null) {
                return;
            }

            IDynamicStackHandler stackHandler = stacksHandler.getStacks();
            IDynamicStackHandler cosmeticStacksHandler = stacksHandler.getCosmeticStacks();
            NonNullList<Boolean> renderStates = stacksHandler.getRenders();

            for (int i = 0; i < stackHandler.getSlots(); i++) {
                boolean renderable = renderStates.size() > i && renderStates.get(i);
                ItemStack stack = cosmeticStacksHandler.getStackInSlot(i);
                boolean cosmetic = true;
                if (stack.isEmpty() && renderable) {
                    stack = stackHandler.getStackInSlot(i);
                    cosmetic = false;
                }

                if (!stack.isEmpty() && stack.is(ModItems.GRAPPLING_HOOK.get())) {
                    result[0] = new Entry(stack, new SlotContext(slotId, player, i, cosmetic, renderable));
                    return;
                }
            }
        }));
        return Optional.ofNullable(result[0]);
    }

    record Entry(ItemStack stack, SlotContext slotContext) {
    }
}
