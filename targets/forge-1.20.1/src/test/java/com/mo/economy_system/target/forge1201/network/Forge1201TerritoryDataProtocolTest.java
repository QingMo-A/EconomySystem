package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.territory.TerritoryTestFixtures;
import com.mo.economy_system.network.TerritoryTeleportWireCodec;
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
    assertEquals(19, EconomyMessages.TELEPORT_TO_TERRITORY.discriminator());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER,
        EconomyMessages.TELEPORT_TO_TERRITORY.spec().direction());
  }

  @Test void teleportUuidCodecRoundTripsAndRejectsMalformedPayloads() {
    var id = java.util.UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
    FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
    TerritoryTeleportWireCodec.encode(new TeleportToTerritoryMessage(id), encoded);
    assertEquals(new TeleportToTerritoryMessage(id), TerritoryTeleportWireCodec.decode(encoded));

    FriendlyByteBuf truncated = new FriendlyByteBuf(Unpooled.buffer());
    truncated.writeLong(1L);
    assertThrows(RuntimeException.class, () -> TerritoryTeleportWireCodec.decode(truncated));

    FriendlyByteBuf trailing = new FriendlyByteBuf(Unpooled.buffer());
    TerritoryTeleportWireCodec.encode(new TeleportToTerritoryMessage(id), trailing);
    trailing.writeByte(1);
    assertThrows(RuntimeException.class, () -> TerritoryTeleportWireCodec.decode(trailing));
  }

  @Test void truncatedNegativeCountAndTrailingDataAreRejected() {
    assertThrows(RuntimeException.class, () -> Forge1201TerritoryDataCodec.decodeRequest(
        new FriendlyByteBuf(Unpooled.buffer())));
    FriendlyByteBuf negative = new FriendlyByteBuf(Unpooled.buffer());
    negative.writeUtf("data", 16); negative.writeLong(1); negative.writeInt(-1);
    assertThrows(RuntimeException.class, () -> Forge1201TerritoryDataCodec.decodeResponse(negative));
    FriendlyByteBuf trailing = new FriendlyByteBuf(Unpooled.buffer());
    Forge1201TerritoryDataCodec.encodeRequest(new TerritoryDataRequestMessage(1), trailing);
    trailing.writeByte(1);
    assertThrows(RuntimeException.class, () -> Forge1201TerritoryDataCodec.decodeRequest(trailing));
  }

  @Test void errorRoundTripAndUnknownKindAreRejected() {
    FriendlyByteBuf error = new FriendlyByteBuf(Unpooled.buffer());
    var expected = com.mo.economy_system.common.network.TerritoryDataResponseMessage.error(9);
    Forge1201TerritoryDataCodec.encodeResponse(expected, error);
    assertEquals(expected, Forge1201TerritoryDataCodec.decodeResponse(error));
    FriendlyByteBuf unknown = new FriendlyByteBuf(Unpooled.buffer());
    unknown.writeUtf("future", 16); unknown.writeLong(9);
    assertThrows(RuntimeException.class, () -> Forge1201TerritoryDataCodec.decodeResponse(unknown));
  }
}
