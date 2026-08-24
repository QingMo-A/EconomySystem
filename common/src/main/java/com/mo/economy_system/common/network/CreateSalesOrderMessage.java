package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

/**
 * Client intent for protocol 8. The third field is the integer unit price; the authoritative
 * server derives totalPrice = quantity * unitPrice with overflow checks.
 */
public record CreateSalesOrderMessage(int slot, int quantity, int unitPrice)
    implements EconomyNetworkMessage {
}
