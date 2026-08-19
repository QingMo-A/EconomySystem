package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.network.MailboxNotificationMessage;
import com.mo.economy_system.testsupport.TestWireBuffer;
import org.junit.jupiter.api.Test;

class MailboxNotificationWireCodecTest {
  @Test
  void notificationRoundTripsTypeSenderAndSubject() {
    MailboxNotificationMessage message =
        new MailboxNotificationMessage(MailType.PLAYER, "Alice", "Hello");
    TestWireBuffer buffer = new TestWireBuffer();

    MailboxWireCodec.encodeNotification(message, buffer);

    assertEquals(message, MailboxWireCodec.decodeNotification(buffer));
  }

  @Test
  void systemNotificationCanUseTranslatedFallbacks() {
    MailboxNotificationMessage message =
        new MailboxNotificationMessage(MailType.COMPENSATION, "", "");
    TestWireBuffer buffer = new TestWireBuffer();

    MailboxWireCodec.encodeNotification(message, buffer);

    assertEquals(message, MailboxWireCodec.decodeNotification(buffer));
  }
}
