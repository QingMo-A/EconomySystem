package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.network.MailboxSendStatus;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable common state for player-to-player mail composition. */
public record MailboxComposeState(
    List<MailboxComposeInventoryItem> inventory,
    String recipient,
    String subject,
    String body,
    String moneyAmount,
    Set<Integer> selectedSlots,
    boolean sending,
    long requestId,
    long appliedRevision,
    MailboxSendStatus status) {

  /** Compatibility constructor for compose screens created before money input was added. */
  public MailboxComposeState(
      List<MailboxComposeInventoryItem> inventory,
      String recipient,
      String subject,
      String body,
      Set<Integer> selectedSlots,
      boolean sending,
      long requestId,
      long appliedRevision,
      MailboxSendStatus status) {
    this(inventory, recipient, subject, body, "0", selectedSlots, sending, requestId,
        appliedRevision, status);
  }

  public MailboxComposeState {
    inventory = List.copyOf(Objects.requireNonNull(inventory, "inventory"));
    recipient = Objects.requireNonNullElse(recipient, "");
    subject = Objects.requireNonNullElse(subject, "");
    body = Objects.requireNonNullElse(body, "");
    moneyAmount = Objects.requireNonNullElse(moneyAmount, "0");
    selectedSlots = Set.copyOf(Objects.requireNonNull(selectedSlots, "selectedSlots"));
    if (requestId < -1 || appliedRevision < -1) throw new IllegalArgumentException("invalid request state");
  }

  public MailboxComposeInventoryItem itemAt(int slot) {
    return inventory.stream().filter(item -> item.slot() == slot).findFirst().orElse(null);
  }
}
