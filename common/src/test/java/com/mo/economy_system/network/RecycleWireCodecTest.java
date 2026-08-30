package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.common.network.*;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecycleWireCodecTest {
  @Test
  void responseAndSubmitRoundTrip() {
    RecycleOfferSnapshot offer = new RecycleOfferSnapshot("minecraft:kelp", 1, 2, 1536, 40, 64, true);
    RecycleDataResponseMessage data = RecycleDataResponseMessage.data(8, 1000, 2000, List.of(offer));
    TestWireBuffer response = new TestWireBuffer(); RecycleWireCodec.encodeResponse(data, response);
    assertEquals(data, RecycleWireCodec.decodeResponse(response));
    RecycleSubmitMessage submit = new RecycleSubmitMessage(8, UUID.randomUUID(), "minecraft:kelp", 12);
    TestWireBuffer request = new TestWireBuffer(); RecycleWireCodec.encodeSubmit(submit, request);
    assertEquals(submit, RecycleWireCodec.decodeSubmit(request));
    RecycleActionResponseMessage action = new RecycleActionResponseMessage(8, RecycleActionStatus.SUCCESS, 12, 24, 1524, "ok");
    TestWireBuffer actionBuffer = new TestWireBuffer(); RecycleWireCodec.encodeAction(action, actionBuffer);
    assertEquals(action, RecycleWireCodec.decodeAction(actionBuffer));
  }

  @Test
  void responseRejectsTooManyOffers() {
    TestWireBuffer buffer = new TestWireBuffer(); buffer.writeInt(0); buffer.writeLong(1); buffer.writeLong(2); buffer.writeLong(3); buffer.writeUtf("", 512); buffer.writeInt(EconomyNetworkLimits.MAX_RECYCLE_OFFERS + 1);
    assertThrows(RuntimeException.class, () -> RecycleWireCodec.decodeResponse(buffer));
  }
}
