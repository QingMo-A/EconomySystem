package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.client.ClientShopState;
import com.mo.economy_system.common.economy.ShopDataService;
import com.mo.economy_system.common.network.ShopDataRequestMessage;
import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.platform.EconomyServices;
import java.util.List;
import java.util.UUID;
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
            ShopDataService.sendCatalog(player.getUUID(), new ShopDataService.ShopDataPort() {
                @Override
                public List<com.mo.economy_system.common.network.ShopItemSnapshot> snapshot() {
                    return EconomyServices.platform().shopCatalog().snapshot();
                }

                @Override
                public void send(UUID playerId, ShopDataResponseMessage response) {
                    if (!player.getUUID().equals(playerId)) return;
                    EconomyServices.platform().network().sendToPlayer(playerId, response);
                }
            });
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
