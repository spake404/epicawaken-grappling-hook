package org.com.epicawaken_grappling_hook.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientEntityHookChoiceTracker {
    private static int activeSequenceId = -1;
    private static long expiresAtGameTime = Long.MIN_VALUE;

    public static void open(int sequenceId, int windowTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || sequenceId < 0) {
            clear();
            return;
        }
        activeSequenceId = sequenceId;
        expiresAtGameTime = minecraft.level.getGameTime() + Math.max(1, windowTicks);
    }

    public static int consumeActiveSequence() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || activeSequenceId < 0
                || minecraft.level.getGameTime() > expiresAtGameTime) {
            clear();
            return -1;
        }

        int sequenceId = activeSequenceId;
        clear();
        return sequenceId;
    }

    private static void clear() {
        activeSequenceId = -1;
        expiresAtGameTime = Long.MIN_VALUE;
    }

    private ClientEntityHookChoiceTracker() {
    }
}
