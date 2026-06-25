package org.com.epicawaken_grappling_hook.network;

import org.com.epicawaken_grappling_hook.projectile.hook.GrapplingHook;

public enum GrapplingHookFovType {
    UNKNOWN,
    AIR,
    GROUND,
    ENTITY;

    public static GrapplingHookFovType fromHookType(GrapplingHook.HookType hookType) {
        if (hookType == null) {
            return UNKNOWN;
        }

        return switch (hookType) {
            case AIR -> AIR;
            case GROUND -> GROUND;
            case ENTITY -> ENTITY;
            case MISSED -> UNKNOWN;
        };
    }

    public static GrapplingHookFovType fromOrdinal(int ordinal) {
        GrapplingHookFovType[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : UNKNOWN;
    }
}
