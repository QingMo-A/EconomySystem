package com.mo.economy_system.common.mail;

import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import com.mo.economy_system.platform.nbt.NbtData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Loader-neutral NBT codec for mailbox metadata. */
public final class MailboxCodec {
  /** Schema 4 adds an idempotent, claim-on-open currency reward, separate from {@code money}. */
  public static final int SCHEMA_VERSION = 4;

  private MailboxCodec() {}

  public static NbtData.Compound encode(MailboxLedger.State state) {
    List<NbtData> personal = new ArrayList<>();
    for (Map.Entry<UUID, List<MailRecord>> box : state.personal().entrySet()) {
      List<NbtData> mails = box.getValue().stream().map(MailboxCodec::encodeMail).map(v -> (NbtData) v).toList();
      personal.add(NbtData.compoundBuilder()
          .putUuid("owner", box.getKey())
          .put("mails", NbtData.list(mails))
          .build());
    }
    List<NbtData> announcements = state.announcements().stream()
        .map(MailboxCodec::encodeMail).map(v -> (NbtData) v).toList();
    return NbtData.compoundBuilder()
        .putInt("schema", SCHEMA_VERSION)
        .put("personal", NbtData.list(personal))
        .put("announcements", NbtData.list(announcements))
        .put("announcement_reads", encodeSetMap(state.announcementReads()))
        .put("announcement_dismissed", encodeSetMap(state.announcementDismissed()))
        .build();
  }

  public static MailboxLedger.State decode(NbtData.Compound root) {
    int schema = intValue(root, "schema", 1);
    if (schema < 1 || schema > SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported mailbox schema: " + schema);
    }

    Map<UUID, List<MailRecord>> personal = new LinkedHashMap<>();
    for (NbtData value : listValue(root, "personal")) {
      NbtData.Compound box = compound(value, "mailbox owner");
      UUID owner = uuidValue(box, "owner");
      List<MailRecord> mails = new ArrayList<>();
      for (NbtData mail : listValue(box, "mails")) mails.add(decodeMail(compound(mail, "mail")));
      if (personal.put(owner, List.copyOf(mails)) != null) {
        throw new IllegalArgumentException("duplicate mailbox owner");
      }
    }

    List<MailRecord> announcements = new ArrayList<>();
    for (NbtData value : listValue(root, "announcements")) {
      announcements.add(decodeMail(compound(value, "announcement")));
    }
    return new MailboxLedger.State(Map.copyOf(personal), List.copyOf(announcements),
        decodeSetMap(root, "announcement_reads"), decodeSetMap(root, "announcement_dismissed"));
  }

  private static NbtData.Compound encodeMail(MailRecord mail) {
    var builder = NbtData.compoundBuilder()
        .putUuid("id", mail.mailId())
        .putString("type", mail.type().id())
        .putString("sender_name", mail.senderName())
        .putString("subject", mail.subject())
        .putString("body", mail.body())
        .putString("source", mail.source())
        .putLong("created", mail.createdAtEpochMillis())
        .putLong("expires", mail.expiresAtEpochMillis())
        .putBoolean("read", mail.read())
        .putBoolean("protected", mail.protectedMail());
    if (mail.senderId() != null) builder.putUuid("sender_id", mail.senderId());
    if (mail.moneyAmount() > 0) builder.putInt("money", mail.moneyAmount());
    if (mail.rewardRecordId() != null) {
      builder.putUuid("reward_record_id", mail.rewardRecordId())
          .putInt("currency_reward_amount", mail.currencyRewardAmount())
          .putBoolean("currency_reward_claimed", mail.currencyRewardClaimed());
    }
    List<NbtData> attachments = mail.attachmentIds().stream().map(NbtData::uuid).map(v -> (NbtData) v).toList();
    List<NbtData> claimed = mail.claimedAttachments().stream().map(attachment -> (NbtData) NbtData.compoundBuilder()
        .putUuid("id", attachment.entryId())
        .put("item", ItemStackSnapshotCodec.encode(attachment.item()).orElseThrow())
        .build()).toList();
    return builder.put("attachments", NbtData.list(attachments))
        .put("claimed_attachments", NbtData.list(claimed))
        .build();
  }

  private static MailRecord decodeMail(NbtData.Compound tag) {
    UUID senderId = tag.contains("sender_id") ? uuidValue(tag, "sender_id") : null;
    List<UUID> attachments = new ArrayList<>();
    for (NbtData value : listValue(tag, "attachments")) attachments.add(NbtData.readUuid(value));
    List<MailAttachmentSnapshot> claimed = new ArrayList<>();
    for (NbtData value : listValue(tag, "claimed_attachments")) {
      NbtData.Compound row = compound(value, "claimed attachment");
      NbtData itemValue = row.get("item");
      if (!(itemValue instanceof NbtData.Compound itemTag)) {
        throw new IllegalArgumentException("claimed attachment item must be compound");
      }
      claimed.add(new MailAttachmentSnapshot(
          uuidValue(row, "id"), ItemStackSnapshotCodec.decode(itemTag).orElseThrow(), true));
    }
    return new MailRecord(
        uuidValue(tag, "id"),
        MailType.fromId(stringValue(tag, "type", "system")),
        senderId,
        stringValue(tag, "sender_name", ""),
        stringValue(tag, "subject", ""),
        stringValue(tag, "body", ""),
        stringValue(tag, "source", ""),
        longValue(tag, "created", 0),
        longValue(tag, "expires", 0),
        List.copyOf(attachments),
        List.copyOf(claimed),
        booleanValue(tag, "read", false),
        booleanValue(tag, "protected", false),
        intValue(tag, "money", 0),
        tag.contains("reward_record_id") ? uuidValue(tag, "reward_record_id") : null,
        intValue(tag, "currency_reward_amount", 0),
        booleanValue(tag, "currency_reward_claimed", false));
  }

  private static NbtData.ListValue encodeSetMap(Map<UUID, Set<UUID>> source) {
    List<NbtData> rows = new ArrayList<>();
    for (Map.Entry<UUID, Set<UUID>> entry : source.entrySet()) {
      List<NbtData> ids = entry.getValue().stream().map(NbtData::uuid).map(v -> (NbtData) v).toList();
      rows.add(NbtData.compoundBuilder().putUuid("owner", entry.getKey())
          .put("ids", NbtData.list(ids)).build());
    }
    return NbtData.list(rows);
  }

  private static Map<UUID, Set<UUID>> decodeSetMap(NbtData.Compound root, String key) {
    Map<UUID, Set<UUID>> result = new LinkedHashMap<>();
    for (NbtData value : listValue(root, key)) {
      NbtData.Compound row = compound(value, key);
      UUID owner = uuidValue(row, "owner");
      Set<UUID> ids = new LinkedHashSet<>();
      for (NbtData id : listValue(row, "ids")) ids.add(NbtData.readUuid(id));
      if (result.put(owner, Set.copyOf(ids)) != null) throw new IllegalArgumentException("duplicate mailbox state owner");
    }
    return Map.copyOf(result);
  }

  private static NbtData.Compound compound(NbtData value, String label) {
    if (!(value instanceof NbtData.Compound compound)) throw new IllegalArgumentException(label + " must be compound");
    return compound;
  }

  private static List<NbtData> listValue(NbtData.Compound tag, String key) {
    NbtData value = tag.get(key);
    if (value == null) return List.of();
    if (!(value instanceof NbtData.ListValue list)) throw new IllegalArgumentException(key + " must be list");
    return list.values();
  }

  private static UUID uuidValue(NbtData.Compound tag, String key) {
    NbtData value = tag.get(key);
    if (value == null) throw new IllegalArgumentException("missing " + key);
    return NbtData.readUuid(value);
  }

  private static String stringValue(NbtData.Compound tag, String key, String fallback) {
    NbtData value = tag.get(key);
    if (value == null) return fallback;
    if (!(value instanceof NbtData.StringValue string)) throw new IllegalArgumentException(key + " must be string");
    return string.value();
  }

  private static int intValue(NbtData.Compound tag, String key, int fallback) {
    NbtData value = tag.get(key);
    if (value == null) return fallback;
    if (!(value instanceof NbtData.IntValue integer)) throw new IllegalArgumentException(key + " must be int");
    return integer.value();
  }

  private static long longValue(NbtData.Compound tag, String key, long fallback) {
    NbtData value = tag.get(key);
    if (value == null) return fallback;
    if (!(value instanceof NbtData.LongValue longValue)) throw new IllegalArgumentException(key + " must be long");
    return longValue.value();
  }

  private static boolean booleanValue(NbtData.Compound tag, String key, boolean fallback) {
    NbtData value = tag.get(key);
    if (value == null) return fallback;
    if (!(value instanceof NbtData.ByteValue byteValue)) throw new IllegalArgumentException(key + " must be byte");
    return byteValue.value() != 0;
  }
}
