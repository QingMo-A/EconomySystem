package com.mo.economy_system.api.mailbox;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stable server-side API for EconomySystem mailbox delivery. */
public interface EconomyMailboxApi {
  int MAX_SUBJECT_LENGTH = 96;
  int MAX_BODY_LENGTH = 2_048;
  int MAX_SOURCE_LENGTH = 128;
  int MAX_ATTACHMENTS = 27;
  Pattern SOURCE_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

  DeliveryStatus sendNotice(UUID recipientId, MailDraft draft);

  DeliveryStatus sendCompensation(UUID recipientId, MailDraft draft, List<MailItemGrant> items);

  /** Publishes one global text announcement. Zero expiry means no explicit expiry. */
  DeliveryStatus publishAnnouncement(MailDraft draft, long expiresAtEpochMillis);

  enum DeliveryStatus {
    SUCCESS,
    INVALID_INPUT,
    UNKNOWN_ITEM,
    MAILBOX_FULL,
    ATTACHMENT_STORAGE_FULL,
    TOO_MANY_ATTACHMENTS,
    BALANCE_LIMIT,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  /** Immutable text/source metadata for one API-produced mail. */
  record MailDraft(String source, String subject, String body, int moneyAmount) {
    /** Compatibility constructor for text/item-only messages. */
    public MailDraft(String source, String subject, String body) {
      this(source, subject, body, 0);
    }

    public MailDraft {
      source = Objects.requireNonNull(source, "source").trim();
      subject = Objects.requireNonNullElse(subject, "").trim();
      body = Objects.requireNonNullElse(body, "");
      if (source.isEmpty() || source.length() > MAX_SOURCE_LENGTH || !SOURCE_PATTERN.matcher(source).matches()) {
        throw new IllegalArgumentException("source must be a namespaced id such as mymod:daily_reward");
      }
      if (subject.isEmpty() || subject.length() > MAX_SUBJECT_LENGTH) {
        throw new IllegalArgumentException("subject must be 1.." + MAX_SUBJECT_LENGTH + " characters");
      }
      if (body.length() > MAX_BODY_LENGTH) {
        throw new IllegalArgumentException("body exceeds " + MAX_BODY_LENGTH + " characters");
      }
      if (moneyAmount < 0) {
        throw new IllegalArgumentException("moneyAmount must be non-negative");
      }
    }

    public static MailDraft of(String source, String subject, String body) {
      return new MailDraft(source, subject, body);
    }

    public static MailDraft of(String source, String subject, String body, int moneyAmount) {
      return new MailDraft(source, subject, body, moneyAmount);
    }
  }

  /** Simple registry-backed item grant. Target adapters preserve the registered item's native stack rules. */
  record MailItemGrant(String itemId, int count) {
    public MailItemGrant {
      itemId = Objects.requireNonNull(itemId, "itemId").trim();
      if (itemId.isEmpty() || itemId.length() > 256 || !SOURCE_PATTERN.matcher(itemId).matches()) {
        throw new IllegalArgumentException("itemId must be a namespaced registry id");
      }
      if (count <= 0) throw new IllegalArgumentException("count must be positive");
    }

    public static MailItemGrant of(String itemId, int count) {
      return new MailItemGrant(itemId, count);
    }
  }
}
