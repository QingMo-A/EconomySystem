package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.mail.MailType;

/** User-facing mailbox filters. */
public enum DeliveryCategory {
  ALL("screen.mailbox.category.all"),
  UNREAD("screen.mailbox.category.unread"),
  PLAYER("screen.mailbox.category.player"),
  SYSTEM("screen.mailbox.category.system"),
  MARKET("screen.mailbox.category.market"),
  ANNOUNCEMENT("screen.mailbox.category.announcement");

  private final String labelKey;

  DeliveryCategory(String labelKey) { this.labelKey = labelKey; }
  public String labelKey() { return labelKey; }

  public boolean matches(DeliveryRow row) {
    if (this == ALL) return true;
    if (this == UNREAD) return !row.mail().read();
    MailType type = row.mail().type();
    return switch (this) {
      case PLAYER -> type == MailType.PLAYER;
      case SYSTEM -> type == MailType.SYSTEM || type == MailType.COMPENSATION;
      case MARKET -> type == MailType.MARKET;
      case ANNOUNCEMENT -> type == MailType.ANNOUNCEMENT;
      case ALL, UNREAD -> true;
    };
  }
}
