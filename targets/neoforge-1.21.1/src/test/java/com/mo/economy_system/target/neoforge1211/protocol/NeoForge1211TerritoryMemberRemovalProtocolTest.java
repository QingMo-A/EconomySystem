package com.mo.economy_system.target.neoforge1211.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import com.mo.economy_system.network.TerritoryMemberRemovalWireCodec;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import com.mo.economy_system.protocol.EconomyProtocol;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class NeoForge1211TerritoryMemberRemovalProtocolTest {
  @Test
  void targetCodecUsesTheCanonicalSharedBytes() {
    assertEquals(22, EconomyProtocol.REMOVE_PLAYER.discriminator());
    assertEquals(
        EconomyMessageDirection.CLIENT_TO_SERVER, EconomyProtocol.REMOVE_PLAYER.direction());
    assertEquals(RemoveTerritoryMemberMessage.class, EconomyMessages.REMOVE_PLAYER.messageClass());
    var message =
        new RemoveTerritoryMemberMessage(
            UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"),
            UUID.fromString("ffeeddcc-bbaa-9988-7766-554433221100"));
    FriendlyByteBuf shared = new FriendlyByteBuf(Unpooled.buffer());
    RegistryFriendlyByteBuf target = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
    try {
      TerritoryMemberRemovalWireCodec.encode(message, NeoForge1211WireBuffer.wrap(shared));
      NeoForge1211MessageCodecs.codec(EconomyMessages.REMOVE_PLAYER).encode(message, target);
      assertEquals(32, target.readableBytes());
      assertEquals(shared, target);
      assertEquals(
          message, NeoForge1211MessageCodecs.codec(EconomyMessages.REMOVE_PLAYER).decode(target));
    } finally {
      shared.release();
      target.release();
    }
  }
}
