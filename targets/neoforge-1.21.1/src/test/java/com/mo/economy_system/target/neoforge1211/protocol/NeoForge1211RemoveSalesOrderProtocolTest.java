package com.mo.economy_system.target.neoforge1211.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.RemoveSalesOrderMessage;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class NeoForge1211RemoveSalesOrderProtocolTest {
  @Test
  void uuidRoundTripsOnCanonicalDiscriminator() {
    var message = new RemoveSalesOrderMessage(UUID.randomUUID());
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    var codec = NeoForge1211MessageCodecs.codec(EconomyMessages.REMOVE_SALES_ORDER);
    codec.encode(message, buffer);
    assertEquals(message, codec.decode(buffer));
    assertEquals(15, EconomyMessages.REMOVE_SALES_ORDER.discriminator());
  }

  @Test
  void nullTradeIdIsRejected() {
    assertThrows(NullPointerException.class, () -> new RemoveSalesOrderMessage(null));
  }
}
