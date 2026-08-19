package com.mo.economy_system.network;

import com.mo.economy_system.common.mail.MailAttachmentSnapshot;
import com.mo.economy_system.common.mail.MailSnapshot;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.MailboxClaimAllMessage;
import com.mo.economy_system.common.network.MailboxClaimAttachmentMessage;
import com.mo.economy_system.common.network.MailboxDataRequestMessage;
import com.mo.economy_system.common.network.MailboxDataResponseMessage;
import com.mo.economy_system.common.network.MailboxDeleteMessage;
import com.mo.economy_system.common.network.MailboxMarkReadMessage;
import com.mo.economy_system.common.network.MailboxNotificationMessage;
import com.mo.economy_system.common.network.MailboxResponseKind;
import com.mo.economy_system.common.network.MailboxSendPlayerMessage;
import com.mo.economy_system.common.network.MailboxSendResultMessage;
import com.mo.economy_system.common.network.MailboxSendStatus;
import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import com.mo.economy_system.platform.nbt.NbtData;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.platform.network.WireDecodeException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Shared defensive wire codec for the full mailbox protocol. */
public final class MailboxWireCodec {
  private MailboxWireCodec() {}

  public static void encodeRequest(MailboxDataRequestMessage message, WireBuffer buffer) {
    buffer.writeLong(message.requestId());
  }

  public static MailboxDataRequestMessage decodeRequest(WireBuffer buffer) {
    requireBytes(buffer, Long.BYTES);
    MailboxDataRequestMessage message = new MailboxDataRequestMessage(buffer.readLong());
    requireConsumed(buffer);
    return message;
  }

  public static void encodeResponse(MailboxDataResponseMessage message, WireBuffer buffer) {
    try (WireBuffer temporary = buffer.temporary()) {
      temporary.writeUtf(message.kind().id(), 16);
      temporary.writeLong(message.requestId());
      temporary.writeInt(message.mails().size());
      for (MailSnapshot mail : message.mails()) encodeMail(mail, temporary);
      if (temporary.readableBytes() > EconomyNetworkLimits.MAX_MAILBOX_RESPONSE_WIRE_BYTES) {
        throw new IllegalArgumentException("mailbox response exceeds wire budget");
      }
      buffer.writeRemaining(temporary);
    }
  }

  public static MailboxDataResponseMessage decodeResponse(WireBuffer buffer) {
    if (buffer.readableBytes() > EconomyNetworkLimits.MAX_MAILBOX_RESPONSE_WIRE_BYTES) {
      throw new WireDecodeException("mailbox response exceeds wire budget");
    }
    try {
      MailboxResponseKind kind = MailboxResponseKind.fromId(buffer.readUtf(16));
      long requestId = buffer.readLong();
      int count = buffer.readInt();
      int max = EconomyNetworkLimits.MAX_MAILS_PER_PLAYER + EconomyNetworkLimits.MAX_MAIL_ANNOUNCEMENTS;
      if (count < 0 || count > max) throw new WireDecodeException("invalid mailbox mail count: " + count);
      if (kind == MailboxResponseKind.ERROR && count != 0) {
        throw new WireDecodeException("error mailbox response cannot contain mail");
      }
      List<MailSnapshot> mails = new ArrayList<>(count);
      for (int i = 0; i < count; i++) mails.add(decodeMail(buffer));
      requireConsumed(buffer);
      return kind == MailboxResponseKind.DATA
          ? MailboxDataResponseMessage.data(requestId, mails)
          : MailboxDataResponseMessage.error(requestId);
    } catch (WireDecodeException failure) {
      throw failure;
    } catch (RuntimeException failure) {
      throw new WireDecodeException("invalid mailbox response", failure);
    }
  }

  public static void encodeMarkRead(MailboxMarkReadMessage message, WireBuffer buffer) {
    buffer.writeUuid(message.mailId());
    buffer.writeLong(message.requestId());
  }

  public static MailboxMarkReadMessage decodeMarkRead(WireBuffer buffer) {
    requireBytes(buffer, 24);
    MailboxMarkReadMessage message = new MailboxMarkReadMessage(buffer.readUuid(), buffer.readLong());
    requireConsumed(buffer);
    return message;
  }

  public static void encodeDelete(MailboxDeleteMessage message, WireBuffer buffer) {
    buffer.writeUuid(message.mailId());
    buffer.writeLong(message.requestId());
  }

  public static MailboxDeleteMessage decodeDelete(WireBuffer buffer) {
    requireBytes(buffer, 24);
    MailboxDeleteMessage message = new MailboxDeleteMessage(buffer.readUuid(), buffer.readLong());
    requireConsumed(buffer);
    return message;
  }

  public static void encodeClaimAttachment(MailboxClaimAttachmentMessage message, WireBuffer buffer) {
    buffer.writeUuid(message.mailId());
    buffer.writeUuid(message.entryId());
    buffer.writeLong(message.requestId());
  }

  public static MailboxClaimAttachmentMessage decodeClaimAttachment(WireBuffer buffer) {
    requireBytes(buffer, 40);
    MailboxClaimAttachmentMessage message =
        new MailboxClaimAttachmentMessage(buffer.readUuid(), buffer.readUuid(), buffer.readLong());
    requireConsumed(buffer);
    return message;
  }

  public static void encodeClaimAll(MailboxClaimAllMessage message, WireBuffer buffer) {
    buffer.writeUuid(message.mailId());
    buffer.writeLong(message.requestId());
  }

  public static MailboxClaimAllMessage decodeClaimAll(WireBuffer buffer) {
    requireBytes(buffer, 24);
    MailboxClaimAllMessage message = new MailboxClaimAllMessage(buffer.readUuid(), buffer.readLong());
    requireConsumed(buffer);
    return message;
  }

  public static void encodeSendPlayer(MailboxSendPlayerMessage message, WireBuffer buffer) {
    buffer.writeUtf(message.recipientName(), EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
    buffer.writeUtf(message.subject(), EconomyNetworkLimits.MAX_MAIL_SUBJECT_LENGTH);
    buffer.writeUtf(message.body(), EconomyNetworkLimits.MAX_MAIL_BODY_LENGTH);
    buffer.writeInt(message.inventorySlots().size());
    for (Integer slot : message.inventorySlots()) buffer.writeInt(slot);
    buffer.writeLong(message.requestId());
  }

  public static MailboxSendPlayerMessage decodeSendPlayer(WireBuffer buffer) {
    try {
      String recipient = buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
      String subject = buffer.readUtf(EconomyNetworkLimits.MAX_MAIL_SUBJECT_LENGTH);
      String body = buffer.readUtf(EconomyNetworkLimits.MAX_MAIL_BODY_LENGTH);
      int count = buffer.readInt();
      if (count < 0 || count > EconomyNetworkLimits.MAX_PLAYER_MAIL_ATTACHMENTS) {
        throw new WireDecodeException("invalid player mail attachment count: " + count);
      }
      List<Integer> slots = new ArrayList<>(count);
      for (int i = 0; i < count; i++) slots.add(buffer.readInt());
      long requestId = buffer.readLong();
      requireConsumed(buffer);
      return new MailboxSendPlayerMessage(recipient, subject, body, slots, requestId);
    } catch (WireDecodeException failure) {
      throw failure;
    } catch (RuntimeException failure) {
      throw new WireDecodeException("invalid player mail request", failure);
    }
  }

  public static void encodeSendResult(MailboxSendResultMessage message, WireBuffer buffer) {
    buffer.writeLong(message.requestId());
    buffer.writeUtf(message.status().id(), 32);
  }

  public static MailboxSendResultMessage decodeSendResult(WireBuffer buffer) {
    try {
      MailboxSendResultMessage message = new MailboxSendResultMessage(
          buffer.readLong(), MailboxSendStatus.fromId(buffer.readUtf(32)));
      requireConsumed(buffer);
      return message;
    } catch (RuntimeException failure) {
      throw new WireDecodeException("invalid mailbox send result", failure);
    }
  }

  public static void encodeNotification(MailboxNotificationMessage message, WireBuffer buffer) {
    buffer.writeUtf(message.type().id(), 32);
    buffer.writeUtf(message.senderName(), EconomyNetworkLimits.MAX_MAIL_SENDER_LENGTH);
    buffer.writeUtf(message.subject(), EconomyNetworkLimits.MAX_MAIL_SUBJECT_LENGTH);
  }

  public static MailboxNotificationMessage decodeNotification(WireBuffer buffer) {
    try {
      MailboxNotificationMessage message = new MailboxNotificationMessage(
          MailType.fromId(buffer.readUtf(32)),
          buffer.readUtf(EconomyNetworkLimits.MAX_MAIL_SENDER_LENGTH),
          buffer.readUtf(EconomyNetworkLimits.MAX_MAIL_SUBJECT_LENGTH));
      requireConsumed(buffer);
      return message;
    } catch (RuntimeException failure) {
      throw new WireDecodeException("invalid mailbox notification", failure);
    }
  }

  private static void encodeMail(MailSnapshot mail, WireBuffer buffer) {
    buffer.writeUuid(mail.mailId());
    buffer.writeUtf(mail.type().id(), 32);
    buffer.writeBoolean(mail.senderId() != null);
    if (mail.senderId() != null) buffer.writeUuid(mail.senderId());
    buffer.writeUtf(mail.senderName(), EconomyNetworkLimits.MAX_MAIL_SENDER_LENGTH);
    buffer.writeUtf(mail.subject(), EconomyNetworkLimits.MAX_MAIL_SUBJECT_LENGTH);
    buffer.writeUtf(mail.body(), EconomyNetworkLimits.MAX_MAIL_BODY_LENGTH);
    buffer.writeUtf(mail.source(), EconomyNetworkLimits.MAX_MAIL_SOURCE_LENGTH);
    buffer.writeLong(mail.createdAtEpochMillis());
    buffer.writeLong(mail.expiresAtEpochMillis());
    buffer.writeBoolean(mail.read());
    buffer.writeBoolean(mail.globalAnnouncement());
    buffer.writeBoolean(mail.protectedMail());
    buffer.writeInt(mail.attachments().size());
    for (MailAttachmentSnapshot attachment : mail.attachments()) {
      buffer.writeUuid(attachment.entryId());
      buffer.writeBoolean(attachment.claimed());
      buffer.writeNbt(ItemStackSnapshotCodec.encode(attachment.item()).orElseThrow());
    }
  }

  private static MailSnapshot decodeMail(WireBuffer buffer) {
    UUID mailId = buffer.readUuid();
    MailType type = MailType.fromId(buffer.readUtf(32));
    UUID senderId = buffer.readBoolean() ? buffer.readUuid() : null;
    String senderName = buffer.readUtf(EconomyNetworkLimits.MAX_MAIL_SENDER_LENGTH);
    String subject = buffer.readUtf(EconomyNetworkLimits.MAX_MAIL_SUBJECT_LENGTH);
    String body = buffer.readUtf(EconomyNetworkLimits.MAX_MAIL_BODY_LENGTH);
    String source = buffer.readUtf(EconomyNetworkLimits.MAX_MAIL_SOURCE_LENGTH);
    long created = buffer.readLong();
    long expires = buffer.readLong();
    boolean read = buffer.readBoolean();
    boolean global = buffer.readBoolean();
    boolean protectedMail = buffer.readBoolean();
    int count = buffer.readInt();
    if (count < 0 || count > EconomyNetworkLimits.MAX_MAIL_ATTACHMENTS) {
      throw new WireDecodeException("invalid mail attachment count: " + count);
    }
    List<MailAttachmentSnapshot> attachments = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      UUID entryId = buffer.readUuid();
      boolean claimed = buffer.readBoolean();
      NbtData.Compound nbt = buffer.readNbt();
      if (nbt == null) throw new WireDecodeException("missing mailbox attachment snapshot");
      attachments.add(new MailAttachmentSnapshot(
          entryId, ItemStackSnapshotCodec.decode(nbt).orElseThrow(), claimed));
    }
    return new MailSnapshot(mailId, type, senderId, senderName, subject, body, source,
        created, expires, read, global, protectedMail, attachments);
  }

  private static void requireBytes(WireBuffer buffer, int count) {
    if (buffer.readableBytes() < count) throw new WireDecodeException("truncated mailbox payload");
  }

  private static void requireConsumed(WireBuffer buffer) {
    if (buffer.isReadable()) throw new WireDecodeException("trailing mailbox payload data");
  }
}
