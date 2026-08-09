package com.mo.economy_system.core.economy_system.delivery_box;

import com.mo.economy_system.common.delivery.DeliveryBoxEntryCodec;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.delivery.DeliveryBoxLedger;
import com.mo.economy_system.common.delivery.DeliveryBoxRepository;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import com.mo.economy_system.target.neoforge1211.item.NeoForge1211NbtAdapter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** NeoForge 1.21.1 persistence adapter for the common delivery ledger. */
public final class DeliveryBoxSavedData extends SavedData {
  private static final String DATA_NAME = "delivery_box_data";
  private final DeliveryBoxLedger ledger = new DeliveryBoxLedger();
  private HolderLookup.Provider registries;

  public static DeliveryBoxSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
    DeliveryBoxSavedData data = new DeliveryBoxSavedData();
    data.registries = registries;
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
        entries.add(DeliveryBoxEntryCodec.decode(NeoForge1211NbtAdapter.fromNative(list.getCompound(index))));
      }
      boxes.put(owner, entries);
    }
    data.ledger.restore(boxes);
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    for (Map.Entry<UUID, List<DeliveryBoxEntrySnapshot>> box : ledger.snapshot().entrySet()) {
      ListTag entries = new ListTag();
      for (DeliveryBoxEntrySnapshot entry : box.getValue()) {
        entries.add(NeoForge1211NbtAdapter.toNative(DeliveryBoxEntryCodec.encode(entry)));
      }
      tag.put(box.getKey().toString(), entries);
    }
    return tag;
  }

  public static DeliveryBoxSavedData getInstance(ServerLevel level) {
    ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
    DeliveryBoxSavedData data = overworld.getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(DeliveryBoxSavedData::new, DeliveryBoxSavedData::load), DATA_NAME);
    data.registries = overworld.registryAccess();
    return data;
  }

  public DeliveryBoxLedger ledger() {
    return ledger;
  }

  public void markDirty() {
    setDirty();
  }

  /** Compatibility input for the still-running market expiration task. */
  public void addItem(UUID ownerId, DeliveryItem item) {
    ItemStack stack = item.getItemStack().copy();
    DeliveryBoxEntrySnapshot entry = new DeliveryBoxEntrySnapshot(
        item.getDataID(),
        NeoForge1211Platform.nativeItemStacks().captureSnapshot(stack, registries).orElseThrow(),
        item.getSource());
    if (!entry.item().itemId().equals(item.getItemID())) {
      throw new IllegalArgumentException("delivery item id mismatch");
    }
    ledger.add(ownerId, entry, this::setDirty);
  }

  /** Compatibility view; callers receive new objects and copied stacks. */
  public List<DeliveryItem> getItems(UUID ownerId) {
    List<DeliveryItem> result = new ArrayList<>();
    for (DeliveryBoxEntrySnapshot entry : ledger.list(ownerId)) {
      ItemStack stack = NeoForge1211Platform.nativeItemStacks()
          .restoreSnapshot(entry.item(), registries)
          .orElseThrow();
      result.add(new DeliveryItem(entry.entryId(), entry.item().itemId(), stack, entry.source()));
    }
    return List.copyOf(result);
  }

  public DeliveryItem getItem(UUID ownerId, UUID entryId) {
    return getItems(ownerId).stream()
        .filter(item -> item.getDataID().equals(entryId))
        .findFirst()
        .orElse(null);
  }

  public boolean removeItem(UUID ownerId, UUID entryId) {
    DeliveryBoxRepository.Reservation reservation = ledger.reserve(ownerId, entryId);
    if (reservation == null) return false;
    return reservation.commit(this::setDirty) == DeliveryBoxRepository.CommitResult.REMOVED;
  }
}
