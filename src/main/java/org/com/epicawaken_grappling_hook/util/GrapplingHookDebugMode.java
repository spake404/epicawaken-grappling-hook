package org.com.epicawaken_grappling_hook.util;

import net.minecraftforge.fml.loading.FMLEnvironment;

public final class GrapplingHookDebugMode {
    private GrapplingHookDebugMode() {
    }

    public static boolean isEnabled() {
        return !FMLEnvironment.production;
    }
}
