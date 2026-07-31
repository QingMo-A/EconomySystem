package com.mo.economy_system.platform;

import com.mo.economy_system.platform.network.EconomyNetworkBridge;
import com.mo.economy_system.platform.item.EconomyItemStackBridge;
import com.mo.economy_system.platform.shop.EconomyShopCatalogBridge;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

/**
 * Stable platform operations used by shared EconomySystem behavior.
 * Loader event buses and other Forge/NeoForge types must not appear here.
 */
public interface EconomyPlatform {
    String targetName();

    Path configDirectory();

    /** Returns the active logical server, or {@code null} when none is running. */
    MinecraftServer currentServer();

    String modVersion(String modId);

    boolean isModLoaded(String modId);

    EconomyNetworkBridge network();

    EconomyItemStackBridge itemStacks();

    EconomyShopCatalogBridge shopCatalog();
}
