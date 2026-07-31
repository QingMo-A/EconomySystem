package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.economy.ShopPurchaseService;
import com.mo.economy_system.common.network.ShopBuyItemMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Forge entrypoint for the common server-authoritative purchase service. */
final class Forge1201ShopPurchaseHandler {
    private Forge1201ShopPurchaseHandler() {
    }

    static void handle(
            ShopBuyItemMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            ShopPurchaseService.execute(player, message);
        }
        context.setPacketHandled(true);
    }
}
