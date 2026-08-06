package com.mo.economy_system.common.delivery;

import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Stable storage schema with strict new-format reads and legacy compact input support. */
public final class DeliveryBoxEntryCodec {
  public static final int SCHEMA_VERSION = 1;
  private static final Set<String> KEYS = Set.of("schemaVersion", "entryId", "item", "source");
  private static final Set<String> LEGACY_KEYS = Set.of("dataID", "itemID", "itemStack", "source");

  private DeliveryBoxEntryCodec() {}

  public static CompoundTag encode(DeliveryBoxEntrySnapshot entry) {
    CompoundTag result = new CompoundTag();
    result.putInt("schemaVersion", SCHEMA_VERSION);
    result.putUUID("entryId", entry.entryId());
    result.put("item", ItemStackSnapshotCodec.encode(entry.item()).orElseThrow());
    result.putString("source", entry.source());
    return result;
  }

  public static DeliveryBoxEntrySnapshot decode(CompoundTag input) {
    if (input == null) throw new IllegalArgumentException("delivery entry is null");
    CompoundTag tag = input.copy();
    return tag.contains("schemaVersion") ? decodeV1(tag) : decodeLegacy(tag);
  }

  private static DeliveryBoxEntrySnapshot decodeV1(CompoundTag tag) {
    requireOnly(tag, KEYS);
    if (!tag.contains("schemaVersion", Tag.TAG_INT)
        || tag.getInt("schemaVersion") != SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported delivery entry schema");
    }
    if (!tag.hasUUID("entryId") || !tag.contains("item", Tag.TAG_COMPOUND)
        || !tag.contains("source", Tag.TAG_STRING)) {
      throw new IllegalArgumentException("invalid delivery entry schema");
    }
    return new DeliveryBoxEntrySnapshot(
        tag.getUUID("entryId"),
        ItemStackSnapshotCodec.decode(tag.getCompound("item")).orElseThrow(),
        tag.getString("source"));
  }

  private static DeliveryBoxEntrySnapshot decodeLegacy(CompoundTag tag) {
    requireOnly(tag, LEGACY_KEYS);
    if (!tag.hasUUID("dataID") || !tag.contains("itemID", Tag.TAG_STRING)
        || !tag.contains("itemStack", Tag.TAG_COMPOUND)
        || !tag.contains("source", Tag.TAG_STRING)) {
      throw new IllegalArgumentException("invalid legacy delivery entry");
    }
    ItemStackSnapshot item = ItemStackSnapshotCodec.decode(tag.getCompound("itemStack")).orElseThrow();
    if (!tag.getString("itemID").equals(item.itemId())) {
      throw new IllegalArgumentException("legacy delivery item id mismatch");
    }
    return new DeliveryBoxEntrySnapshot(tag.getUUID("dataID"), item, tag.getString("source"));
  }

  private static void requireOnly(CompoundTag tag, Set<String> allowed) {
    Set<String> unknown = new HashSet<>(tag.getAllKeys());
    unknown.removeAll(allowed);
    if (!unknown.isEmpty()) throw new IllegalArgumentException("unknown delivery field: " + unknown);
  }
}
