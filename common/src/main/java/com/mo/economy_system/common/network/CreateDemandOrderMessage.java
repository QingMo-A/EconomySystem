package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

/** Client intent for protocol 9. Identity, item data, time, and order state are server-owned. */
public record CreateDemandOrderMessage(String itemId, int quantity, int totalPrice)
        implements EconomyNetworkMessage {
}
