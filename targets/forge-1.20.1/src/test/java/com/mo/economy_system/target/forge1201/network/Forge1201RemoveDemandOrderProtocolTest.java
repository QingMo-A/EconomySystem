package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class Forge1201RemoveDemandOrderProtocolTest {
  @Test void codecRoundTripsCanonicalProtocol16() {
    RemoveDemandOrderMessage message = new RemoveDemandOrderMessage(UUID.randomUUID());
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    Forge1201RemoveDemandOrderCodec.encode(message, buffer);
    assertEquals(message, Forge1201RemoveDemandOrderCodec.decode(buffer));
    assertEquals(16, EconomyMessages.REMOVE_DEMAND_ORDER.discriminator());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER,
        EconomyMessages.REMOVE_DEMAND_ORDER.spec().direction());
  }

  @Test void nullAndTruncatedPayloadAreRejected() {
    assertThrows(NullPointerException.class, () -> new RemoveDemandOrderMessage(null));
    assertThrows(RuntimeException.class, () -> Forge1201RemoveDemandOrderCodec.decode(
        new FriendlyByteBuf(Unpooled.buffer())));
  }
}
