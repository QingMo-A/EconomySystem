package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.common.network.InvitePlayerMessage;
import com.mo.economy_system.network.TerritoryInviteWireCodec;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import com.mo.economy_system.protocol.EconomyProtocol;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class Forge1201TerritoryInviteProtocolTest {
  @Test
  void uuidPairUsesCanonicalProtocol20() {
    InvitePlayerMessage message = new InvitePlayerMessage(
        UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"),
        UUID.fromString("ffeeddcc-bbaa-9988-7766-554433221100"));
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    TerritoryInviteWireCodec.encode(message, buffer);
    assertEquals(message, TerritoryInviteWireCodec.decode(buffer));
    assertEquals(20, EconomyProtocol.INVITE_PLAYER.discriminator());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER,
        EconomyProtocol.INVITE_PLAYER.direction());
  }

  @Test
  void malformedPayloadsAreRejected() {
    FriendlyByteBuf truncated = new FriendlyByteBuf(Unpooled.buffer());
    truncated.writeZero(31);
    assertThrows(DecoderException.class, () -> TerritoryInviteWireCodec.decode(truncated));
    FriendlyByteBuf trailing = new FriendlyByteBuf(Unpooled.buffer());
    TerritoryInviteWireCodec.encode(new InvitePlayerMessage(UUID.randomUUID(), UUID.randomUUID()), trailing);
    trailing.writeByte(1);
    assertThrows(DecoderException.class, () -> TerritoryInviteWireCodec.decode(trailing));
  }
}
