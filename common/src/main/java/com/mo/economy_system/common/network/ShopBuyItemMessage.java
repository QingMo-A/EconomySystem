package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

import java.util.Objects;

/** Client intent to buy a quantity of one server-owned shop entry. */
public record ShopBuyItemMessage(String shopItemId, int quantity)
        implements EconomyNetworkMessage {
    public ShopBuyItemMessage {
        shopItemId = Objects.requireNonNull(shopItemId, "shopItemId");
    }
}
