package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

/** Client intent for protocol 8. All item and ownership data is server-owned. */
public record CreateSalesOrderMessage(int slot, int quantity, int totalPrice)
        implements EconomyNetworkMessage {
}
