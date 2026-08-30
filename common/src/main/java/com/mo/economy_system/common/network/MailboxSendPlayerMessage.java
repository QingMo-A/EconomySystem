package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record MailboxSendPlayerMessage(
    String recipientName,
    String subject,
    String body,
    List<Integer> inventorySlots,
    long requestId,
    int moneyAmount) implements EconomyNetworkMessage {

  /** Compatibility constructor for item/text-only player mail. */
  public MailboxSendPlayerMessage(
      String recipientName,
      String subject,
      String body,
      List<Integer> inventorySlots,
      long requestId) {
    this(recipientName, subject, body, inventorySlots, requestId, 0);
  }

  public MailboxSendPlayerMessage {
    recipientName = Objects.requireNonNull(recipientName, "recipientName").trim();
    subject = Objects.requireNonNullElse(subject, "");
    body = Objects.requireNonNullElse(body, "");
    inventorySlots = List.copyOf(Objects.requireNonNull(inventorySlots, "inventorySlots"));
    if (recipientName.isEmpty() || recipientName.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH) {
      throw new IllegalArgumentException("invalid recipient name");
    }
    if (subject.length() > EconomyNetworkLimits.MAX_MAIL_SUBJECT_LENGTH) {
      throw new IllegalArgumentException("mail subject too long");
    }
    if (body.length() > EconomyNetworkLimits.MAX_MAIL_BODY_LENGTH) {
      throw new IllegalArgumentException("mail body too long");
    }
    if (moneyAmount < 0) {
      throw new IllegalArgumentException("mail money amount must be non-negative");
    }
    if (inventorySlots.size() > EconomyNetworkLimits.MAX_PLAYER_MAIL_ATTACHMENTS) {
      throw new IllegalArgumentException("too many player mail attachments");
    }
    HashSet<Integer> unique = new HashSet<>();
    for (Integer slot : inventorySlots) {
      if (slot == null || slot < 0 || slot >= 36 || !unique.add(slot)) {
        throw new IllegalArgumentException("invalid inventory slot selection");
      }
    }
    if (subject.isBlank() && body.isBlank() && inventorySlots.isEmpty() && moneyAmount == 0) {
      throw new IllegalArgumentException("empty mail cannot be sent");
    }
    if (requestId < 0) throw new IllegalArgumentException("requestId must be non-negative");
  }
}
