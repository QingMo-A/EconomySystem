package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.territory.TerritoryTestFixtures;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class Forge1201TerritoryDataProtocolTest {
  @Test void goldenRequestAndResponseRoundTrip() {
    FriendlyByteBuf request = new FriendlyByteBuf(Unpooled.buffer());
    Forge1201TerritoryDataCodec.encodeRequest(new TerritoryDataRequestMessage(7), request);
    assertEquals(new TerritoryDataRequestMessage(7), Forge1201TerritoryDataCodec.decodeRequest(request));
    var expected = TerritoryTestFixtures.response(7);
    FriendlyByteBuf response = new FriendlyByteBuf(Unpooled.buffer());
    Forge1201TerritoryDataCodec.encodeResponse(expected, response);
    assertEquals(expected, Forge1201TerritoryDataCodec.decodeResponse(response));
  }

  @Test void canonicalIdsAndDirectionsRemainStable() {
    assertEquals(17, EconomyMessages.TERRITORY_DATA_REQUEST.discriminator());
    assertEquals(18, EconomyMessages.TERRITORY_DATA_RESPONSE.discriminator());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER, EconomyMessages.TERRITORY_DATA_REQUEST.spec().direction());
    assertEquals(EconomyMessageDirection.SERVER_TO_CLIENT, EconomyMessages.TERRITORY_DATA_RESPONSE.spec().direction());
  }

  @Test void truncatedNegativeCountAndTrailingDataAreRejected() {
    assertThrows(RuntimeException.class, () -> Forge1201TerritoryDataCodec.decodeRequest(
        new FriendlyByteBuf(Unpooled.buffer())));
    FriendlyByteBuf negative = new FriendlyByteBuf(Unpooled.buffer());
    negative.writeLong(1); negative.writeInt(-1);
    assertThrows(RuntimeException.class, () -> Forge1201TerritoryDataCodec.decodeResponse(negative));
    FriendlyByteBuf trailing = new FriendlyByteBuf(Unpooled.buffer());
    Forge1201TerritoryDataCodec.encodeRequest(new TerritoryDataRequestMessage(1), trailing);
    trailing.writeByte(1);
    assertThrows(RuntimeException.class, () -> Forge1201TerritoryDataCodec.decodeRequest(trailing));
  }
}
