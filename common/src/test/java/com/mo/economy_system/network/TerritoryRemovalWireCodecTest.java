package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.RemoveTerritoryMessage;
import com.mo.economy_system.platform.network.WireDecodeException;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryRemovalWireCodecTest {
  @Test
  void exactGoldenWireAndProtocol() {
    UUID id = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
    TestWireBuffer buffer = new TestWireBuffer();
    TerritoryRemovalWireCodec.encode(new RemoveTerritoryMessage(id), buffer);
    assertEquals(16, buffer.readableBytes());
    assertArrayEquals(new byte[] {
        0, 17, 34, 51, 68, 85, 102, 119,
        (byte) 136, (byte) 153, (byte) 170, (byte) 187,
        (byte) 204, (byte) 221, (byte) 238, (byte) 255
    }, buffer.bytes());
    assertEquals(id, TerritoryRemovalWireCodec.decode(buffer).territoryId());
    assertEquals(21, EconomyMessages.REMOVE_TERRITORY.discriminator());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER, EconomyMessages.REMOVE_TERRITORY.direction());
  }

  @Test
  void rejectsTruncationAndTrailing() {
    TestWireBuffer truncated = new TestWireBuffer();
    truncated.writeLong(1);
    assertThrows(WireDecodeException.class, () -> TerritoryRemovalWireCodec.decode(truncated));

    TestWireBuffer trailing = new TestWireBuffer();
    trailing.writeUuid(UUID.randomUUID());
    trailing.writeByte(1);
    assertThrows(WireDecodeException.class, () -> TerritoryRemovalWireCodec.decode(trailing));
  }
}
