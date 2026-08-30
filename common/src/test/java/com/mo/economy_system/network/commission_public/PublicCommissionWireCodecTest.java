package com.mo.economy_system.network.commission_public;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.network.commission_public.PublicCommissionActionResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitStatus;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicCommissionWireCodecTest {
  private static final UUID ID = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");

  @Test
  void snapshotAndActionsRoundTrip() {
    PublicCommission commission = PublicCommission.create(ID, "City expansion", "town_hall",
        "Town Hall", "minecraft:stone", 10_000, 3, 1_000L, 10_000L,
        "Build the central road");
    PublicCommissionDataResponseMessage response = PublicCommissionDataResponseMessage.data(
        4L, 2_000L, List.of(commission));
    TestWireBuffer responseBuffer = new TestWireBuffer();
    PublicCommissionWireCodec.encodeDataResponse(response, responseBuffer);
    assertEquals(response, PublicCommissionWireCodec.decodeDataResponse(responseBuffer));

    PublicCommissionSubmitMessage submit = new PublicCommissionSubmitMessage(5L, ID,
        UUID.randomUUID(), 64);
    TestWireBuffer submitBuffer = new TestWireBuffer();
    PublicCommissionWireCodec.encodeSubmit(submit, submitBuffer);
    assertEquals(submit, PublicCommissionWireCodec.decodeSubmit(submitBuffer));

    PublicCommissionActionResponseMessage action = new PublicCommissionActionResponseMessage(
        5L, PublicCommissionSubmitStatus.PARTIAL, 64, 192, "已发送奖励邮件");
    TestWireBuffer actionBuffer = new TestWireBuffer();
    PublicCommissionWireCodec.encodeActionResponse(action, actionBuffer);
    assertEquals(action, PublicCommissionWireCodec.decodeActionResponse(actionBuffer));
  }

  @Test
  void responseRejectsOversizedList() {
    TestWireBuffer buffer = new TestWireBuffer();
    buffer.writeInt(0);
    buffer.writeLong(1L);
    buffer.writeLong(2L);
    buffer.writeInt(25);
    assertThrows(RuntimeException.class, () -> PublicCommissionWireCodec.decodeDataResponse(buffer));
  }
}
