package com.mo.economy_system.target.neoforge1211.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.territory.TerritoryTestFixtures;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class NeoForge1211TerritoryDataProtocolTest {
  private RegistryFriendlyByteBuf buffer() {
    return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
  }

  @Test void goldenRequestAndResponseRoundTrip() {
    RegistryFriendlyByteBuf request = buffer();
    var requestCodec = NeoForge1211MessageCodecs.codec(EconomyMessages.TERRITORY_DATA_REQUEST);
    requestCodec.encode(new TerritoryDataRequestMessage(7), request);
    assertEquals(new TerritoryDataRequestMessage(7), requestCodec.decode(request));
    var expected = TerritoryTestFixtures.response(7);
    RegistryFriendlyByteBuf response = buffer();
    var responseCodec = NeoForge1211MessageCodecs.codec(EconomyMessages.TERRITORY_DATA_RESPONSE);
    responseCodec.encode(expected, response);
    assertEquals(expected, responseCodec.decode(response));
  }

  @Test void canonicalIdsAndDirectionsRemainStable() {
    assertEquals(17, EconomyMessages.TERRITORY_DATA_REQUEST.discriminator());
    assertEquals(18, EconomyMessages.TERRITORY_DATA_RESPONSE.discriminator());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER, EconomyMessages.TERRITORY_DATA_REQUEST.spec().direction());
    assertEquals(EconomyMessageDirection.SERVER_TO_CLIENT, EconomyMessages.TERRITORY_DATA_RESPONSE.spec().direction());
  }

  @Test void truncatedAndNegativeCountAreRejected() {
    assertThrows(RuntimeException.class, () -> NeoForge1211MessageCodecs.codec(
        EconomyMessages.TERRITORY_DATA_REQUEST).decode(buffer()));
    RegistryFriendlyByteBuf negative = buffer();
    negative.writeUtf("data", 16); negative.writeLong(1); negative.writeInt(-1);
    assertThrows(RuntimeException.class, () -> NeoForge1211MessageCodecs.codec(
        EconomyMessages.TERRITORY_DATA_RESPONSE).decode(negative));
  }

  @Test void errorRoundTripAndUnknownKindAreRejected() {
    var codec = NeoForge1211MessageCodecs.codec(EconomyMessages.TERRITORY_DATA_RESPONSE);
    RegistryFriendlyByteBuf error = buffer();
    var expected = com.mo.economy_system.common.network.TerritoryDataResponseMessage.error(9);
    codec.encode(expected, error);
    assertEquals(expected, codec.decode(error));
    RegistryFriendlyByteBuf unknown = buffer();
    unknown.writeUtf("future", 16); unknown.writeLong(9);
    assertThrows(RuntimeException.class, () -> codec.decode(unknown));
  }
}
