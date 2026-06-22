package org.com.epicawaken_grappling_hook.client;

import net.minecraftforge.fml.ModList;
import yesman.epicfight.config.ClientConfig;

public final class EpicFightCuriosFallbackGuard {
    private static final String EPIC_FIGHT_CURIOS_LAYER = "yesman.epicfight.compat.CuriosCompat$PatchedCuriosLayerRenderer";
    private static final String EPIC_FIGHT_CURIOS_COMPAT_LAYER = "com.oneworldstudio.epicfightcurioscompat.ClientCuriosCompat$PatchedCuriosLayerRenderer";

    private EpicFightCuriosFallbackGuard() {
    }

    public static boolean isSuppressedLayerCall() {
        if (!ClientConfig.enableAnimatedFirstPersonModel) {
            return false;
        }
        return (isModLoaded("epicfight") || isModLoaded("epicfight_curios_compat")) && isEpicFightCuriosFallback();
    }

    private static boolean isEpicFightCuriosFallback() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            if (EPIC_FIGHT_CURIOS_LAYER.equals(className) || EPIC_FIGHT_CURIOS_COMPAT_LAYER.equals(className)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isModLoaded(String modid) {
        return ModList.get() != null && ModList.get().isLoaded(modid);
    }
}
