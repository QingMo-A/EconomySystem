package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.InvitePlayerMessage;
import com.mo.economy_system.platform.network.WireDecodeException;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryInviteWireCodecTest {
  @Test
  void goldenAndRoundTrip() {
    InvitePlayerMessage message = new InvitePlayerMessage(
        UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"),
        UUID.fromString("ffeeddcc-bbaa-9988-7766-554433221100"));
    TestWireBuffer buffer = new TestWireBuffer();
    TerritoryInviteWireCodec.encode(message, buffer);
    assertEquals(32, buffer.readableBytes());
    assertEquals(
        "00112233445566778899aabbccddeeffffeeddccbbaa99887766554433221100",
        HexFormat.of().formatHex(buffer.bytes()));
    assertEquals(message, TerritoryInviteWireCodec.decode(buffer));
  }

  @Test
  void rejectsTruncationAndTrailing() {
    for (int size : new int[] {0, 15, 16, 31}) {
      TestWireBuffer truncated = new TestWireBuffer();
      truncated.writeZero(size);
      assertThrows(WireDecodeException.class, () -> TerritoryInviteWireCodec.decode(truncated));
    }
    TestWireBuffer trailing = new TestWireBuffer();
    TerritoryInviteWireCodec.encode(
        new InvitePlayerMessage(UUID.randomUUID(), UUID.randomUUID()), trailing);
    trailing.writeByte(1);
    assertThrows(WireDecodeException.class, () -> TerritoryInviteWireCodec.decode(trailing));
  }
}
