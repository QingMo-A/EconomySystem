package com.mo.economy_system.target.forge1201;

import com.mo.economy_system.platform.EconomyPlatform;
import com.mo.economy_system.platform.network.EconomyNetworkBridge;
import com.mo.economy_system.target.forge1201.item.Forge1201ItemStackBridge;
import com.mo.economy_system.target.forge1201.network.Forge1201NetworkBridge;
import com.mo.economy_system.platform.shop.EconomyShopCatalogBridge;
import com.mo.economy_system.target.forge1201.shop.Forge1201ShopCatalogBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.file.Path;

public final class Forge1201Platform implements EconomyPlatform {
    private static final Forge1201ItemStackBridge ITEM_STACKS = new Forge1201ItemStackBridge();
    private static final Forge1201ShopCatalogBridge SHOP_CATALOG = new Forge1201ShopCatalogBridge();
    private final EconomyNetworkBridge network = new Forge1201NetworkBridge();

    @Override
    public String targetName() {
        return "forge-1.20.1";
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static MinecraftServer activeServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    /** Target-native ItemStack adapter; shared code never receives this API. */
    public static Forge1201ItemStackBridge nativeItemStacks() {
        return ITEM_STACKS;
    }

    /** Target-native catalog operations such as ItemStack reconstruction. */
    public static Forge1201ShopCatalogBridge nativeShopCatalog() {
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
