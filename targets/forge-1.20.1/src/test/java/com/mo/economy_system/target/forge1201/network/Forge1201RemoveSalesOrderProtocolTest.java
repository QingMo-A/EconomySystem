package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.RemoveSalesOrderMessage;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class Forge1201RemoveSalesOrderProtocolTest {
  @Test
  void uuidRoundTripsOnCanonicalDiscriminator() {
    var message = new RemoveSalesOrderMessage(UUID.randomUUID());
    var buffer = new FriendlyByteBuf(Unpooled.buffer());
    Forge1201RemoveSalesOrderCodec.encode(message, buffer);
    assertEquals(message, Forge1201RemoveSalesOrderCodec.decode(buffer));
    assertEquals(15, EconomyMessages.REMOVE_SALES_ORDER.discriminator());
  }

  @Test
  void nullTradeIdIsRejected() {
    assertThrows(NullPointerException.class, () -> new RemoveSalesOrderMessage(null));
  }
}
