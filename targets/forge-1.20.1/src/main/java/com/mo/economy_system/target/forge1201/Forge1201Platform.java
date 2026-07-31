package com.mo.economy_system.target.forge1201;

import com.mo.economy_system.platform.EconomyPlatform;
import com.mo.economy_system.platform.network.EconomyNetworkBridge;
import com.mo.economy_system.platform.item.EconomyItemStackBridge;
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
    private final EconomyNetworkBridge network = new Forge1201NetworkBridge();
    private final EconomyItemStackBridge itemStacks = new Forge1201ItemStackBridge();
    private final EconomyShopCatalogBridge shopCatalog = new Forge1201ShopCatalogBridge();

    @Override
    public String targetName() {
        return "forge-1.20.1";
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public MinecraftServer currentServer() {
        return ServerLifecycleHooks.getCurrentServer();
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
    public EconomyItemStackBridge itemStacks() {
        return itemStacks;
    }

    @Override
    public EconomyShopCatalogBridge shopCatalog() {
        return shopCatalog;
    }
}
