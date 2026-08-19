package com.mo.economy_system.common.mail;

import com.mo.economy_system.platform.item.ItemStackSnapshot;
import java.util.Objects;
import java.util.UUID;

/** One mailbox attachment exposed to the client, including persistent claimed history. */
public record MailAttachmentSnapshot(UUID entryId, ItemStackSnapshot item, boolean claimed) {
  public MailAttachmentSnapshot(UUID entryId, ItemStackSnapshot item) {
    this(entryId, item, false);
  }

  public MailAttachmentSnapshot {
    Objects.requireNonNull(entryId, "entryId");
    Objects.requireNonNull(item, "item");
  }

  public MailAttachmentSnapshot asClaimed() {
    return claimed ? this : new MailAttachmentSnapshot(entryId, item, true);
  }
}
