package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.client.ClientShopState;
import com.mo.economy_system.common.economy.ShopDataService;
import com.mo.economy_system.common.network.ShopDataRequestMessage;
import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.platform.EconomyServices;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


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
        });
    }

    public static void handleResponse(ShopDataResponseMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientShopState.update(message);
        });
    }
}
