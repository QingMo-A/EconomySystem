package com.mo.economy_system.common.delivery;

import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import com.mo.economy_system.platform.nbt.NbtData;
import java.util.HashSet;
import java.util.Set;

/** Stable storage schema with strict new-format reads and legacy compact input support. */
public final class DeliveryBoxEntryCodec {
  public static final int SCHEMA_VERSION = 1;
  private static final Set<String> KEYS = Set.of("schemaVersion", "entryId", "item", "source");
  private static final Set<String> LEGACY_KEYS = Set.of("dataID", "itemID", "itemStack", "source");

  private DeliveryBoxEntryCodec() {}

  public static NbtData.Compound encode(DeliveryBoxEntrySnapshot entry) {
    return NbtData.compoundBuilder()
        .putInt("schemaVersion", SCHEMA_VERSION)
        .putUuid("entryId", entry.entryId())
        .put("item", ItemStackSnapshotCodec.encode(entry.item()).orElseThrow())
        .putString("source", entry.source())
        .build();
  }

  public static DeliveryBoxEntrySnapshot decode(NbtData.Compound input) {
    if (input == null) throw new IllegalArgumentException("delivery entry is null");
    NbtData.Compound tag = input;
    return tag.contains("schemaVersion") ? decodeV1(tag) : decodeLegacy(tag);
  }

  private static DeliveryBoxEntrySnapshot decodeV1(NbtData.Compound tag) {
    requireOnly(tag, KEYS);
    if (!(tag.get("schemaVersion") instanceof NbtData.IntValue version)
        || version.value() != SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported delivery entry schema");
    }
    if (!(tag.get("entryId") instanceof NbtData.IntArrayValue)
        || !(tag.get("item") instanceof NbtData.Compound)
        || !(tag.get("source") instanceof NbtData.StringValue)) {
      throw new IllegalArgumentException("invalid delivery entry schema");
    }
    return new DeliveryBoxEntrySnapshot(
        NbtData.readUuid(tag.get("entryId")),
        ItemStackSnapshotCodec.decode((NbtData.Compound) tag.get("item")).orElseThrow(),
        ((NbtData.StringValue) tag.get("source")).value());
  }

  private static DeliveryBoxEntrySnapshot decodeLegacy(NbtData.Compound tag) {
    requireOnly(tag, LEGACY_KEYS);
    if (!(tag.get("dataID") instanceof NbtData.IntArrayValue)
        || !(tag.get("itemID") instanceof NbtData.StringValue)
        || !(tag.get("itemStack") instanceof NbtData.Compound)
        || !(tag.get("source") instanceof NbtData.StringValue)) {
      throw new IllegalArgumentException("invalid legacy delivery entry");
    }
    ItemStackSnapshot item = ItemStackSnapshotCodec.decode((NbtData.Compound) tag.get("itemStack")).orElseThrow();
    if (!((NbtData.StringValue) tag.get("itemID")).value().equals(item.itemId())) {
      throw new IllegalArgumentException("legacy delivery item id mismatch");
    }
    return new DeliveryBoxEntrySnapshot(NbtData.readUuid(tag.get("dataID")), item,
        ((NbtData.StringValue) tag.get("source")).value());
  }

  private static void requireOnly(NbtData.Compound tag, Set<String> allowed) {
    Set<String> unknown = new HashSet<>(tag.keys());
    unknown.removeAll(allowed);
    if (!unknown.isEmpty()) throw new IllegalArgumentException("unknown delivery field: " + unknown);
  }
}
