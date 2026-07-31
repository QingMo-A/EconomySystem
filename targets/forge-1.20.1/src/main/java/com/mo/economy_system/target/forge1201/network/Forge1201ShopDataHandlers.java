package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.client.ClientShopState;
import com.mo.economy_system.common.economy.ShopDataService;
import com.mo.economy_system.common.network.ShopDataRequestMessage;
import com.mo.economy_system.common.network.ShopDataResponseMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Forge behavior adapter for common shop-catalog synchronization. */
final class Forge1201ShopDataHandlers {
    private Forge1201ShopDataHandlers() {
    }

    static void handleRequest(
            ShopDataRequestMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            ShopDataService.sendCatalog(player);
        }
        context.setPacketHandled(true);
    }

    static void handleResponse(
            ShopDataResponseMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ClientShopState.update(message);
        contextSupplier.get().setPacketHandled(true);
    }
}
