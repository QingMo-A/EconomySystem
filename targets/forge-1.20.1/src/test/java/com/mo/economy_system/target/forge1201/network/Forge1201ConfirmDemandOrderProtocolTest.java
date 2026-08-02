package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.ConfirmDemandOrderMessage;
import com.mo.economy_system.common.network.EconomyMessages;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class Forge1201ConfirmDemandOrderProtocolTest {
  @Test
  void uuidRoundTripsOnCanonicalDiscriminator() {
    var message = new ConfirmDemandOrderMessage(UUID.randomUUID());
    var buffer = new FriendlyByteBuf(Unpooled.buffer());
    Forge1201ConfirmDemandOrderCodec.encode(message, buffer);
    assertEquals(message, Forge1201ConfirmDemandOrderCodec.decode(buffer));
    assertEquals(13, EconomyMessages.CONFIRM_DEMAND_ORDER.discriminator());
  }

  @Test
  void nullTradeIdIsRejected() {
    assertThrows(NullPointerException.class, () -> new ConfirmDemandOrderMessage(null));
  }
}
