package com.mo.economy_system.core.economy_system.mailbox;

import com.mo.economy_system.common.mail.MailboxCodec;
import com.mo.economy_system.common.mail.MailboxLedger;
import com.mo.economy_system.target.neoforge1211.item.NeoForge1211NbtAdapter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** NeoForge 1.21.1 persistence adapter for mailbox metadata and global announcements. */
public final class MailboxSavedData extends SavedData {
  private static final String DATA_NAME = "mailbox_data";
  private final MailboxLedger ledger = new MailboxLedger();

  public static MailboxSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
    MailboxSavedData data = new MailboxSavedData();
    if (!tag.isEmpty()) data.ledger.restore(MailboxCodec.decode(NeoForge1211NbtAdapter.fromNative(tag)));
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    CompoundTag encoded = NeoForge1211NbtAdapter.toNative(MailboxCodec.encode(ledger.snapshot()));
    for (String key : encoded.getAllKeys()) tag.put(key, encoded.get(key).copy());
    return tag;
  }

  public static MailboxSavedData getInstance(ServerLevel level) {
    ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
    return overworld.getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(MailboxSavedData::new, MailboxSavedData::load), DATA_NAME);
  }

  public MailboxLedger ledger() { return ledger; }
  public void markDirty() { setDirty(); }
}
