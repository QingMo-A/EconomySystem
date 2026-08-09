package com.mo.economy_system.target.neoforge1211;

import com.mo.economy_system.platform.EconomyPlatform;
import com.mo.economy_system.platform.network.EconomyNetworkBridge;
import com.mo.economy_system.target.neoforge1211.item.NeoForge1211ItemStackBridge;
import com.mo.economy_system.target.neoforge1211.network.NeoForge1211NetworkBridge;
import com.mo.economy_system.platform.shop.EconomyShopCatalogBridge;
import com.mo.economy_system.target.neoforge1211.shop.NeoForge1211ShopCatalogBridge;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.Path;

public final class NeoForge1211Platform implements EconomyPlatform {
    private static final NeoForge1211ItemStackBridge ITEM_STACKS = new NeoForge1211ItemStackBridge();
    private static final NeoForge1211ShopCatalogBridge SHOP_CATALOG = new NeoForge1211ShopCatalogBridge();
    private final EconomyNetworkBridge network = new NeoForge1211NetworkBridge();

    @Override
    public String targetName() {
        return "neoforge-1.21.1";
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static MinecraftServer activeServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    /** Target-native ItemStack adapter; shared code never receives this API. */
    public static NeoForge1211ItemStackBridge nativeItemStacks() {
        return ITEM_STACKS;
    }

    /** Target-native catalog operations such as ItemStack reconstruction. */
    public static NeoForge1211ShopCatalogBridge nativeShopCatalog() {
        return SHOP_CATALOG;
    }

    @Override
    public String modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public EconomyNetworkBridge network() {
        return network;
    }

    @Override
    public EconomyShopCatalogBridge shopCatalog() {
        return SHOP_CATALOG;
    }
}
