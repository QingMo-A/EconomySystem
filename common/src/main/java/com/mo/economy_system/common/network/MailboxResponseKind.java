package com.mo.economy_system.common.network;

public enum MailboxResponseKind {
  DATA("data"),
  ERROR("error");

  private final String id;

  MailboxResponseKind(String id) { this.id = id; }
  public String id() { return id; }

  public static MailboxResponseKind fromId(String id) {
    for (MailboxResponseKind value : values()) if (value.id.equals(id)) return value;
    throw new IllegalArgumentException("unknown mailbox response kind: " + id);
  }
}
