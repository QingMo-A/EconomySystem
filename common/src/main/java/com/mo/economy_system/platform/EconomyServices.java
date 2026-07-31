package com.mo.economy_system.platform;

/** Target-provided services available to shared EconomySystem code. */
public final class EconomyServices {
    private static volatile EconomyPlatform platform;

    private EconomyServices() {
    }

    public static synchronized void init(EconomyPlatform value) {
        if (value == null) {
            throw new IllegalArgumentException("EconomyPlatform cannot be null");
        }
        if (platform != null) {
            throw new IllegalStateException(
                    "EconomyServices is already initialized for " + platform.targetName()
            );
        }
        platform = value;
    }

    public static EconomyPlatform platform() {
        EconomyPlatform value = platform;
        if (value == null) {
            throw new IllegalStateException(
                    "EconomyServices has not been initialized by the loader target"
            );
        }
        return value;
    }
}
