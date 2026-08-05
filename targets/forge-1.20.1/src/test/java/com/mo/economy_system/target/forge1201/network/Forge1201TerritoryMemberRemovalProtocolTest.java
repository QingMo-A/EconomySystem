package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import com.mo.economy_system.network.TerritoryMemberRemovalWireCodec;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import com.mo.economy_system.protocol.EconomyProtocol;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class Forge1201TerritoryMemberRemovalProtocolTest {
  @Test
  void protocolAndSharedCodecRemainCanonical() {
    assertEquals(22, EconomyProtocol.REMOVE_PLAYER.discriminator());
    assertEquals(
        EconomyMessageDirection.CLIENT_TO_SERVER, EconomyProtocol.REMOVE_PLAYER.direction());
    assertEquals(RemoveTerritoryMemberMessage.class, EconomyMessages.REMOVE_PLAYER.messageClass());
    UUID territory = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
    UUID target = UUID.fromString("ffeeddcc-bbaa-9988-7766-554433221100");
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    TerritoryMemberRemovalWireCodec.encode(
        new RemoveTerritoryMemberMessage(territory, target), buffer);
    assertEquals(32, buffer.readableBytes());
    assertEquals(territory, buffer.readUUID());
    assertEquals(target, buffer.readUUID());
  }
}
