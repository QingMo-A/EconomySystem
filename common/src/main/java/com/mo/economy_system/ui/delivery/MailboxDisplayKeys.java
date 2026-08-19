package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.mail.MailType;
import java.util.Objects;

/** Shared translation-key mapping for mailbox rows, detail views and delivery toasts. */
public final class MailboxDisplayKeys {
  private MailboxDisplayKeys() {}

  public static String senderKey(MailType type) {
    return switch (Objects.requireNonNull(type, "type")) {
      case MARKET -> "screen.mailbox.sender.market";
      case PLAYER -> "screen.mailbox.sender.player";
      case COMPENSATION -> "screen.mailbox.sender.compensation";
      case ANNOUNCEMENT -> "screen.mailbox.sender.announcement";
      case SYSTEM -> "screen.mailbox.sender.system";
    };
  }

  public static String subjectKey(MailType type) {
    return switch (Objects.requireNonNull(type, "type")) {
      case MARKET -> "screen.mailbox.subject.market_return";
      case PLAYER -> "screen.mailbox.subject.player_delivery";
      case COMPENSATION -> "screen.mailbox.subject.compensation";
      case ANNOUNCEMENT -> "screen.mailbox.subject.announcement";
      case SYSTEM -> "screen.mailbox.subject.system_delivery";
    };
  }
}
