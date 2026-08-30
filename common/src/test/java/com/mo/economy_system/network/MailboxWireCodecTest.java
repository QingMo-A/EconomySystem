package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.common.mail.MailSnapshot;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.network.MailboxDataResponseMessage;
import com.mo.economy_system.common.network.MailboxSendPlayerMessage;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MailboxWireCodecTest {
  @Test
  void playerMailMoneyAmountRoundTrips() {
    MailboxSendPlayerMessage message = new MailboxSendPlayerMessage(
        "Alice", "Payment", "Here are the coins", List.of(2, 7), 19L, 250);
    TestWireBuffer buffer = new TestWireBuffer();

    MailboxWireCodec.encodeSendPlayer(message, buffer);

    assertEquals(message, MailboxWireCodec.decodeSendPlayer(buffer));
  }

  @Test
  void mailboxResponseMoneyAmountRoundTrips() {
    MailSnapshot mail = new MailSnapshot(
        UUID.randomUUID(), MailType.SYSTEM, null, "", "Payment", "Body", "mail.system",
        10L, 0L, false, false, true, List.of(), 250);
    MailboxDataResponseMessage response = MailboxDataResponseMessage.data(4L, List.of(mail));
    TestWireBuffer buffer = new TestWireBuffer();

    MailboxWireCodec.encodeResponse(response, buffer);

    assertEquals(response, MailboxWireCodec.decodeResponse(buffer));
  }

  @Test
  void playerMailPayloadWithoutNewMoneyFieldIsRejected() {
    TestWireBuffer buffer = new TestWireBuffer();
    buffer.writeUtf("Alice", 64);
    buffer.writeUtf("Subject", 96);
    buffer.writeUtf("Body", 2_048);
    buffer.writeInt(0);
    buffer.writeLong(1L);

    assertThrows(RuntimeException.class, () -> MailboxWireCodec.decodeSendPlayer(buffer));
  }
}
