package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.delivery.DeliveryBoxTestFixtures;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataResponseMessage;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryBoxWireCodecTest {
  @Test
  void requestAndClaimHaveStableGoldenLayout() {
    TestWireBuffer request = new TestWireBuffer();
    DeliveryBoxWireCodec.encodeRequest(new DeliveryBoxDataRequestMessage(7), request);
    assertEquals(8, request.readableBytes());
    assertEquals(7, request.readLong());

    UUID id = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
    TestWireBuffer claim = new TestWireBuffer();
    DeliveryBoxWireCodec.encodeClaim(new DeliveryBoxClaimMessage(id, 9), claim);
    assertEquals(24, claim.readableBytes());
    assertEquals(id, claim.readUUID());
    assertEquals(9, claim.readLong());
  }

  @Test
  void responseRoundTripsCombinedSnapshot() {
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 12);
    DeliveryBoxDataResponseMessage message = DeliveryBoxDataResponseMessage.data(11, List.of(entry));
    TestWireBuffer buffer = new TestWireBuffer();
    DeliveryBoxWireCodec.encodeResponse(message, buffer);
    assertEquals(message, DeliveryBoxWireCodec.decodeResponse(buffer));
  }

  @Test
  void rejectsTruncatedTrailingUnknownKindAndOversizedEntryCount() {
    for (int size = 0; size < 8; size++) {
      TestWireBuffer request = new TestWireBuffer();
      request.writeZero(size);
      assertThrows(RuntimeException.class, () -> DeliveryBoxWireCodec.decodeRequest(request));
    }
    TestWireBuffer trailing = new TestWireBuffer();
    DeliveryBoxWireCodec.encodeClaim(new DeliveryBoxClaimMessage(UUID.randomUUID(), 0), trailing);
    trailing.writeByte(1);
    assertThrows(RuntimeException.class, () -> DeliveryBoxWireCodec.decodeClaim(trailing));

    TestWireBuffer unknown = new TestWireBuffer();
    unknown.writeUtf("future");
    unknown.writeLong(0);
    unknown.writeInt(0);
    assertThrows(RuntimeException.class, () -> DeliveryBoxWireCodec.decodeResponse(unknown));

    TestWireBuffer count = new TestWireBuffer();
    count.writeUtf("data");
    count.writeLong(0);
    count.writeInt(Integer.MAX_VALUE);
    assertThrows(RuntimeException.class, () -> DeliveryBoxWireCodec.decodeResponse(count));
  }
}
