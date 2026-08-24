package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.network.MailboxSendPlayerMessage;

/** Target adapter for request identity and network delivery. */
public interface MailboxComposePort {
  long nextRequestId();
  void send(MailboxSendPlayerMessage message);
}
