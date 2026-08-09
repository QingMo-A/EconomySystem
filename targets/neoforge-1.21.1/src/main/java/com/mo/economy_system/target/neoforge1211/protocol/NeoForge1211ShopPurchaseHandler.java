package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.target.neoforge1211.NeoForge1211ShopPurchaseAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** NeoForge entrypoint for the common server-authoritative purchase service. */
public final class NeoForge1211ShopPurchaseHandler {
    private NeoForge1211ShopPurchaseHandler() {
    }

    public static void handle(ShopBuyItemMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer
                    ? serverPlayer
                    : null;
            if (player != null) {
                NeoForge1211ShopPurchaseAdapter.execute(player, message);
            }
        });
    }
}
