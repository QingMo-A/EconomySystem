package com.mo.economy_system.target.neoforge1211.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.ConfirmDemandOrderMessage;
import com.mo.economy_system.common.network.EconomyMessages;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class NeoForge1211ConfirmDemandOrderProtocolTest {
  @Test
  void uuidRoundTripsOnCanonicalDiscriminator() {
    var message = new ConfirmDemandOrderMessage(UUID.randomUUID());
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    var codec = NeoForge1211MessageCodecs.codec(EconomyMessages.CONFIRM_DEMAND_ORDER);
    codec.encode(message, buffer);
    assertEquals(message, codec.decode(buffer));
    assertEquals(13, EconomyMessages.CONFIRM_DEMAND_ORDER.discriminator());
  }

  @Test
  void nullTradeIdIsRejected() {
    assertThrows(NullPointerException.class, () -> new ConfirmDemandOrderMessage(null));
  }
}
