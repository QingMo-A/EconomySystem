package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.mail.MailboxCodec;
import com.mo.economy_system.common.mail.MailboxLedger;
import com.mo.economy_system.target.forge1201.item.Forge1201NbtAdapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Forge 1.20.1 persistence adapter for mailbox metadata and global announcements. */
final class Forge1201MailboxSavedData extends SavedData {
  private static final String DATA_NAME = "mailbox_data";
  private final MailboxLedger ledger = new MailboxLedger();

  static Forge1201MailboxSavedData get(ServerLevel level) {
    ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
    return overworld.getDataStorage().computeIfAbsent(
        Forge1201MailboxSavedData::load, Forge1201MailboxSavedData::new, DATA_NAME);
  }

  static Forge1201MailboxSavedData load(CompoundTag tag) {
    Forge1201MailboxSavedData data = new Forge1201MailboxSavedData();
    if (!tag.isEmpty()) data.ledger.restore(MailboxCodec.decode(Forge1201NbtAdapter.fromNative(tag)));
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    CompoundTag encoded = Forge1201NbtAdapter.toNative(MailboxCodec.encode(ledger.snapshot()));
    for (String key : encoded.getAllKeys()) tag.put(key, encoded.get(key).copy());
    return tag;
  }

  MailboxLedger ledger() { return ledger; }
  void markDirty() { setDirty(); }
}
