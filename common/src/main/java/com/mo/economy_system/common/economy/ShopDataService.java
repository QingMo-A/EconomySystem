package com.mo.economy_system.common.economy;

import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/** Shared server behavior for a system-shop catalog request. */
public final class ShopDataService {
    private ShopDataService() {
    }

    public static void sendCatalog(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        EconomyServices.platform().network().sendToPlayer(
                player,
                new ShopDataResponseMessage(EconomyServices.platform().shopCatalog().snapshot())
        );
    }
}
