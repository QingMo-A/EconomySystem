package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.client.ClientShopState;
import com.mo.economy_system.common.economy.ShopDataService;
import com.mo.economy_system.common.network.ShopDataRequestMessage;
import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.core.economy_system.shop.ShopItem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** NeoForge behavior adapter for common shop-catalog synchronization. */
public final class NeoForge1211ShopDataHandlers {
    private NeoForge1211ShopDataHandlers() {
    }

    public static void handleRequest(ShopDataRequestMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer
                    ? serverPlayer
                    : null;
            if (player != null) {
                ShopDataService.sendCatalog(player);
            }
        });
    }

    public static void handleResponse(ShopDataResponseMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientShopState.update(message);
            ClientOnly.apply(message);
        });
    }

    /** Kept lazy so dedicated servers never resolve client screen classes. */
    private static final class ClientOnly {
        private ClientOnly() {
        }

        private static void apply(ShopDataResponseMessage message) {
            List<ShopItem> items = message.items().stream()
                    .map(ShopItem::fromBridgeSnapshot)
                    .toList();
            net.minecraft.client.gui.screens.Screen screen =
                    net.minecraft.client.Minecraft.getInstance().screen;
            if (screen instanceof com.mo.economy_system.screen.economy_system.shop.Screen_Shop shop) {
                shop.updateShopItems(items);
            }
        }
    }
}
