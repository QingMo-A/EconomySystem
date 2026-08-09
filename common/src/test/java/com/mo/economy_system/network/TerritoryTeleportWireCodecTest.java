package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import com.mo.economy_system.platform.network.WireDecodeException;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryTeleportWireCodecTest {
  private static final UUID ID = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");

  @Test
  void goldenUuidBytesAndRoundTrip() {
    TestWireBuffer buffer = new TestWireBuffer();
    TerritoryTeleportWireCodec.encode(new TeleportToTerritoryMessage(ID), buffer);
    assertEquals("00112233445566778899aabbccddeeff", HexFormat.of().formatHex(buffer.bytes()));
    assertEquals(ID, TerritoryTeleportWireCodec.decode(buffer).territoryId());
  }

  @Test
  void rejectsTruncatedAndTrailing() {
    TestWireBuffer truncated = new TestWireBuffer();
    truncated.writeZero(15);
    assertThrows(WireDecodeException.class, () -> TerritoryTeleportWireCodec.decode(truncated));

    TestWireBuffer trailing = new TestWireBuffer();
    trailing.writeUuid(ID);
    trailing.writeByte(1);
    assertThrows(WireDecodeException.class, () -> TerritoryTeleportWireCodec.decode(trailing));
  }

  @Test
  void canonicalManifestIsUnchanged() {
    assertEquals(19, EconomyMessages.TELEPORT_TO_TERRITORY.discriminator());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER, EconomyMessages.TELEPORT_TO_TERRITORY.direction());
    assertEquals("economy_system:territory_system/packet_teleport_to_territory", EconomyMessages.TELEPORT_TO_TERRITORY.id());
  }

  @Test
  void rejectsNull() {
    assertThrows(NullPointerException.class, () -> new TeleportToTerritoryMessage(null));
  }
}
