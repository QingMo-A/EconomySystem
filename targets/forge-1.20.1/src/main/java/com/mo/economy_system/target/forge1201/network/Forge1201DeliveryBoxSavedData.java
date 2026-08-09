package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.delivery.DeliveryBoxEntryCodec;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.delivery.DeliveryBoxLedger;
import com.mo.economy_system.target.forge1201.item.Forge1201NbtAdapter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Forge 1.20.1 persistence adapter for the common delivery ledger. */
final class Forge1201DeliveryBoxSavedData extends SavedData {
  private static final String DATA_NAME = "delivery_box_data";
  private final DeliveryBoxLedger ledger = new DeliveryBoxLedger();

  static Forge1201DeliveryBoxSavedData get(ServerLevel level) {
    ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
    return overworld.getDataStorage().computeIfAbsent(
        Forge1201DeliveryBoxSavedData::load, Forge1201DeliveryBoxSavedData::new, DATA_NAME);
  }

  static Forge1201DeliveryBoxSavedData load(CompoundTag tag) {
    Forge1201DeliveryBoxSavedData data = new Forge1201DeliveryBoxSavedData();
    Map<UUID, List<DeliveryBoxEntrySnapshot>> boxes = new LinkedHashMap<>();
    for (String key : tag.getAllKeys()) {
      UUID owner;
      try {
        owner = UUID.fromString(key);
      } catch (IllegalArgumentException failure) {
        throw new IllegalArgumentException("invalid delivery owner key: " + key, failure);
      }
      if (!tag.contains(key, Tag.TAG_LIST)) {
        throw new IllegalArgumentException("delivery owner value must be a list: " + key);
      }
      ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
      List<DeliveryBoxEntrySnapshot> entries = new ArrayList<>(list.size());
      for (int index = 0; index < list.size(); index++) {
        entries.add(DeliveryBoxEntryCodec.decode(Forge1201NbtAdapter.fromNative(list.getCompound(index))));
      }
      boxes.put(owner, entries);
    }
    data.ledger.restore(boxes);
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    for (Map.Entry<UUID, List<DeliveryBoxEntrySnapshot>> box : ledger.snapshot().entrySet()) {
      ListTag entries = new ListTag();
      for (DeliveryBoxEntrySnapshot entry : box.getValue()) {
        entries.add(Forge1201NbtAdapter.toNative(DeliveryBoxEntryCodec.encode(entry)));
      }
      tag.put(box.getKey().toString(), entries);
    }
    return tag;
  }

  DeliveryBoxLedger ledger() {
    return ledger;
  }

  void markDirty() {
    setDirty();
  }
}
