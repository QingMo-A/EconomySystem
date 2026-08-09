package com.mo.economy_system.platform;

import com.mo.economy_system.platform.network.EconomyNetworkBridge;
import com.mo.economy_system.platform.shop.EconomyShopCatalogBridge;

import java.nio.file.Path;

/**
 * Stable platform operations used by shared EconomySystem behavior.
 * Loader event buses and other Forge/NeoForge types must not appear here.
 */
public interface EconomyPlatform {
    String targetName();

    /** Target API facts used for explicit, tested fallbacks. */
    default PlatformCapabilities capabilities() {
        return PlatformCapabilities.full();
    }

    Path configDirectory();

    String modVersion(String modId);

    boolean isModLoaded(String modId);

    EconomyNetworkBridge network();

    EconomyShopCatalogBridge shopCatalog();
}
