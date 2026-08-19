package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.mail.MailAttachmentSnapshot;
import com.mo.economy_system.common.mail.MailSnapshot;
import com.mo.economy_system.common.mail.MailType;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Mailbox list row. The legacy name is kept to avoid breaking the bridge UI surface. */
public record DeliveryRow(MailSnapshot mail, String displayName) {
  public DeliveryRow {
    Objects.requireNonNull(mail, "mail");
    displayName = Objects.requireNonNullElse(displayName, "").trim();
  }

  /** Compatibility constructor for legacy tests/callers during the mailbox migration. */
  public DeliveryRow(DeliveryBoxEntrySnapshot entry) { this(entry, ""); }

  public DeliveryRow(DeliveryBoxEntrySnapshot entry, String displayName) {
    this(new MailSnapshot(
        UUID.randomUUID(),
        entry.source().toLowerCase(java.util.Locale.ROOT).contains("market") ? MailType.MARKET : MailType.SYSTEM,
        null, "", "", "", entry.source(), 0, 0, false, false, true,
        List.of(new MailAttachmentSnapshot(entry.entryId(), entry.item()))), displayName);
  }

  public UUID entryId() { return mail.mailId(); }
  public UUID mailId() { return mail.mailId(); }
  public boolean matchesCategory(DeliveryCategory category) { return category.matches(this); }
  public MailAttachmentSnapshot firstAttachment() {
    return mail.attachments().isEmpty() ? null : mail.attachments().get(0);
  }

  public String senderKey() {
    return MailboxDisplayKeys.senderKey(mail.type());
  }

  public String subjectKey() {
    return MailboxDisplayKeys.subjectKey(mail.type());
  }

  public String bodyKey() {
    return switch (mail.type()) {
      case MARKET -> "screen.mailbox.body.market_return";
      case PLAYER -> "screen.mailbox.body.player_delivery";
      case COMPENSATION -> "screen.mailbox.body.compensation";
      case ANNOUNCEMENT -> "screen.mailbox.body.announcement";
      case SYSTEM -> "screen.mailbox.body.system_delivery";
    };
  }

  public String searchableText() {
    return (mail.senderName() + "\n" + mail.subject() + "\n" + mail.body() + "\n" + mail.source()
        + "\n" + displayName).toLowerCase(java.util.Locale.ROOT);
  }
}
