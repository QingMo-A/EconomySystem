package com.mo.economy_system.target.neoforge1211.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class NeoForge1211RemoveDemandOrderProtocolTest {
  @Test void codecRoundTripsCanonicalProtocol16() {
    RemoveDemandOrderMessage message = new RemoveDemandOrderMessage(UUID.randomUUID());
    RegistryFriendlyByteBuf buffer =
        new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    var codec = NeoForge1211MessageCodecs.codec(EconomyMessages.REMOVE_DEMAND_ORDER);
    codec.encode(message, buffer);
    assertEquals(message, codec.decode(buffer));
    assertEquals(16, EconomyMessages.REMOVE_DEMAND_ORDER.discriminator());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER,
        EconomyMessages.REMOVE_DEMAND_ORDER.spec().direction());
  }

  @Test void nullAndTruncatedPayloadAreRejected() {
    assertThrows(NullPointerException.class, () -> new RemoveDemandOrderMessage(null));
    RegistryFriendlyByteBuf buffer =
        new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    assertThrows(RuntimeException.class,
        () -> NeoForge1211MessageCodecs.codec(
            EconomyMessages.REMOVE_DEMAND_ORDER).decode(buffer));
  }
}
