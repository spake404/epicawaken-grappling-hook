package org.com.epicawaken_grappling_hook.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class GrapplingHookForwardInputTracker {
    private static final Map<UUID, InputState> INPUTS = new HashMap<>();

    private GrapplingHookForwardInputTracker() {
    }

    public static void update(ServerPlayer player, boolean forwardDown) {
        INPUTS.put(player.getUUID(), new InputState(forwardDown, player.serverLevel().getGameTime()));
    }

    public static boolean isForwardDown(ServerPlayer player, int graceTicks) {
        InputState state = INPUTS.get(player.getUUID());
        if (state == null || !state.forwardDown()) {
            return false;
        }
        return player.serverLevel().getGameTime() - state.gameTime() <= graceTicks;
    }

    public static void clear(ServerPlayer player) {
        INPUTS.remove(player.getUUID());
    }

    private record InputState(boolean forwardDown, long gameTime) {
    }
}
