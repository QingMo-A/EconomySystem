package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

/**
 * Client intent for protocol 9. The client supplies quantity + integer unitPrice only; identity,
 * item snapshot, time, order state and derived totalPrice remain server-owned.
 */
public record CreateDemandOrderMessage(String itemId, int quantity, int unitPrice)
    implements EconomyNetworkMessage {
}
