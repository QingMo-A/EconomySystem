package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

import java.util.List;
import java.util.Objects;

/** Immutable snapshot of the current system-shop catalog. */
public record ShopDataResponseMessage(List<ShopItemSnapshot> items)
        implements EconomyNetworkMessage {
    public ShopDataResponseMessage {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
    }
}
