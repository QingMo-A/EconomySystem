package com.mo.economy_system.common.economy;

import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Shared server behavior for a system-shop catalog request. */
public final class ShopDataService {
    private ShopDataService() {
    }

    public static void sendCatalog(UUID playerId, ShopDataPort port) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(port, "port");
        port.send(playerId, new ShopDataResponseMessage(port.snapshot()));
    }

    /** Target adapter for catalog access and version-specific network delivery. */
    public interface ShopDataPort {
        List<ShopItemSnapshot> snapshot();

        void send(UUID playerId, ShopDataResponseMessage message);
    }
}
