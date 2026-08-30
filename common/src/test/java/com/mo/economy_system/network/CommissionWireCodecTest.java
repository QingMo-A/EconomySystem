package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.common.commission.CommissionRewardSnapshot;
import com.mo.economy_system.common.commission.CommissionStatus;
import com.mo.economy_system.common.commission.CommissionType;
import com.mo.economy_system.common.network.CommissionActionResponseMessage;
import com.mo.economy_system.common.network.CommissionDataResponseMessage;
import com.mo.economy_system.common.network.CommissionSubmitMessage;
import com.mo.economy_system.common.network.CommissionSubmitStatus;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommissionWireCodecTest {
  private static final UUID OWNER = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");

  @Test
  void dataResponseRoundTripsFrozenCommission() {
    CommissionInstance commission = new CommissionInstance(
        UUID.fromString("11112222-3333-4444-5555-666677778888"), OWNER,
        "stone", CommissionType.ITEM_DELIVERY, "smith", "Town Smithy",
        "minecraft:iron_ingot", 64, 12, CommissionRewardSnapshot.coins(520),
        1_000L, 9_000L, CommissionStatus.ACTIVE, "Deliver iron");
    CommissionDataResponseMessage expected = CommissionDataResponseMessage.data(
        7L, 2_000L, 8_000L, 6, List.of(commission));
    TestWireBuffer buffer = new TestWireBuffer();

    CommissionWireCodec.encodeDataResponse(expected, buffer);

    assertEquals(expected, CommissionWireCodec.decodeDataResponse(buffer));
  }

  @Test
  void submitAndActionRoundTrip() {
    CommissionSubmitMessage submit = new CommissionSubmitMessage(4L, OWNER, UUID.randomUUID(), 32);
    TestWireBuffer submitBuffer = new TestWireBuffer();
    CommissionWireCodec.encodeSubmit(submit, submitBuffer);
    assertEquals(submit, CommissionWireCodec.decodeSubmit(submitBuffer));

    CommissionActionResponseMessage action = new CommissionActionResponseMessage(
        4L, CommissionSubmitStatus.COMPLETED, "奖励已发送至邮箱");
    TestWireBuffer actionBuffer = new TestWireBuffer();
    CommissionWireCodec.encodeActionResponse(action, actionBuffer);
    assertEquals(action, CommissionWireCodec.decodeActionResponse(actionBuffer));
  }

  @Test
  void responseRejectsInvalidEntryCount() {
    TestWireBuffer buffer = new TestWireBuffer();
    buffer.writeInt(0); // DATA
    buffer.writeLong(1L);
    buffer.writeLong(2L);
    buffer.writeLong(3L);
    buffer.writeInt(6);
    buffer.writeUtf("", 2_048);
    buffer.writeInt(25);
    assertThrows(RuntimeException.class, () -> CommissionWireCodec.decodeDataResponse(buffer));
  }
}
