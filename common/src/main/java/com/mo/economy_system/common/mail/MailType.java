package com.mo.economy_system.common.mail;

import java.util.Locale;

/** Stable mailbox classification shared by persistence, wire and UI layers. */
public enum MailType {
  PLAYER("player"),
  SYSTEM("system"),
  COMPENSATION("compensation"),
  MARKET("market"),
  ANNOUNCEMENT("announcement");

  private final String id;

  MailType(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public static MailType fromId(String id) {
    if (id == null) throw new IllegalArgumentException("mail type is missing");
    String normalized = id.trim().toLowerCase(Locale.ROOT);
    for (MailType value : values()) if (value.id.equals(normalized)) return value;
    throw new IllegalArgumentException("unknown mail type: " + id);
  }
}
