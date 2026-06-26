package org.com.epicawaken_grappling_hook.util;

import net.minecraftforge.fml.ModList;

public final class ParcoolCompat {
    private static final String PARCOOL_MODID = "parcool";

    private ParcoolCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(PARCOOL_MODID);
    }
}
