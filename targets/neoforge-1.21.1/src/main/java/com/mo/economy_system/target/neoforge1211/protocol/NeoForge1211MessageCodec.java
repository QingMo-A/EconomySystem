package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** NeoForge-owned wire codec for one loader-neutral message type. */
public interface NeoForge1211MessageCodec<T extends EconomyNetworkMessage> {
    void encode(T message, RegistryFriendlyByteBuf buffer);

    T decode(RegistryFriendlyByteBuf buffer);
}
