package com.mo.economy_system.common.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.MailboxDataResponseMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientMailboxStateTest {
  @Test
  void notificationInvalidationIsIndependentFromMailboxDataRevision() {
    long beforeInvalidation = ClientMailboxState.invalidationRevision();
    long beforeDataRevision = ClientMailboxState.snapshot().revision();

    long invalidated = ClientMailboxState.invalidate();

    assertEquals(beforeInvalidation + 1, invalidated);
    assertEquals(beforeDataRevision, ClientMailboxState.snapshot().revision());
  }

  @Test
  void repeatedSameRequestIdResponsesStillPublishNewClientRevisions() {
    long requestId = Math.max(1, ClientMailboxState.snapshot().requestId());
    long before = ClientMailboxState.snapshot().revision();

    assertTrue(ClientMailboxState.update(MailboxDataResponseMessage.data(requestId, List.of())));
    long first = ClientMailboxState.snapshot().revision();
    assertTrue(ClientMailboxState.update(MailboxDataResponseMessage.data(requestId, List.of())));
    long second = ClientMailboxState.snapshot().revision();

    assertTrue(first > before);
    assertTrue(second > first);
  }
}
