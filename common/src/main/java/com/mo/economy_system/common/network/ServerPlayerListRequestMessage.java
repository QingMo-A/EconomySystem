package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

/** Requests the known economy account player list. */
public record ServerPlayerListRequestMessage() implements EconomyNetworkMessage {
    public static final ServerPlayerListRequestMessage INSTANCE = new ServerPlayerListRequestMessage();
}
