package com.mo.economy_system.common.network;

public enum MailboxSendStatus {
  SUCCESS("success"),
  RECIPIENT_NOT_FOUND("recipient_not_found"),
  CANNOT_SEND_TO_SELF("cannot_send_to_self"),
  INVALID_CONTENT("invalid_content"),
  INVALID_ATTACHMENT("invalid_attachment"),
  INSUFFICIENT_FUNDS("insufficient_funds"),
  RECIPIENT_BALANCE_LIMIT("recipient_balance_limit"),
  RECIPIENT_MAILBOX_FULL("recipient_mailbox_full"),
  RATE_LIMITED("rate_limited"),
  FAILED("failed");

  private final String id;
  MailboxSendStatus(String id) { this.id = id; }
  public String id() { return id; }

  public static MailboxSendStatus fromId(String id) {
    for (MailboxSendStatus value : values()) if (value.id.equals(id)) return value;
    throw new IllegalArgumentException("unknown mailbox send status: " + id);
  }
}
