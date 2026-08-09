package com.mo.economy_system.target.neoforge1211.redpacket;

import com.mo.economy_system.common.redpacket.RedPacket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** NeoForge NBT persistence adapter for common red-packet snapshots. */
public final class NeoForge1211RedPacketSavedData extends SavedData {
  private static final String DATA_NAME = "red_packet_data";
  private static final int SCHEMA_VERSION = 1;

  private List<RedPacket> packets = List.of();
  private boolean writable = true;

  public List<RedPacket> packets() {
    return List.copyOf(packets);
  }

  public void replacePackets(List<RedPacket> value) {
    if (!writable) throw new IllegalStateException("unsupported or invalid red-packet persistence");
    List<RedPacket> replacement = validated(value);
    setDirty();
    packets = replacement;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    tag.putInt("schemaVersion", SCHEMA_VERSION);
    ListTag encoded = new ListTag();
    for (RedPacket packet : packets) encoded.add(encode(packet));
    tag.put("packets", encoded);
    return tag;
  }

  public static NeoForge1211RedPacketSavedData load(
      CompoundTag tag, HolderLookup.Provider registries) {
    NeoForge1211RedPacketSavedData data = new NeoForge1211RedPacketSavedData();
    int schema = tag.contains("schemaVersion") ? tag.getInt("schemaVersion") : 1;
    if (schema != SCHEMA_VERSION) {
      data.writable = false;
      return data;
    }
    try {
      data.packets = decodeAll(tag);
    } catch (RuntimeException error) {
      data.writable = false;
      data.packets = List.of();
    }
    return data;
  }

  public static NeoForge1211RedPacketSavedData getInstance(ServerLevel level) {
    ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
    return overworld.getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(NeoForge1211RedPacketSavedData::new, NeoForge1211RedPacketSavedData::load),
        DATA_NAME);
  }

  private static List<RedPacket> decodeAll(CompoundTag tag) {
    if (!tag.contains("packets", Tag.TAG_LIST)) return List.of();
    List<RedPacket> decoded = new ArrayList<>();
    Set<UUID> senders = new HashSet<>();
    ListTag values = tag.getList("packets", Tag.TAG_COMPOUND);
    for (int index = 0; index < values.size(); index++) {
      RedPacket packet = decode(values.getCompound(index));
      if (!senders.add(packet.senderId())) throw new IllegalArgumentException("duplicate sender");
      decoded.add(packet);
    }
    return List.copyOf(decoded);
  }

  private static RedPacket decode(CompoundTag tag) {
    Set<UUID> claimed = new HashSet<>();
    ListTag players = tag.getList("claimedPlayers", Tag.TAG_STRING);
    for (int index = 0; index < players.size(); index++) {
      claimed.add(UUID.fromString(players.getString(index)));
    }
    return new RedPacket(
        UUID.fromString(tag.getString("senderId")),
        tag.getString("senderName"),
        tag.getInt("totalAmount"),
        tag.getInt("totalCount"),
        tag.getInt("claimedAmount"),
        tag.getBoolean("lucky") ? RedPacket.Mode.LUCKY : RedPacket.Mode.EVEN,
        tag.getLong("createdAtMillis"),
        tag.getLong("expirationTimeMillis"),
        claimed);
  }

  private static CompoundTag encode(RedPacket packet) {
    CompoundTag tag = new CompoundTag();
    tag.putString("senderId", packet.senderId().toString());
    tag.putString("senderName", packet.senderName());
    tag.putInt("totalAmount", packet.totalAmount());
    tag.putInt("totalCount", packet.totalCount());
    tag.putInt("claimedAmount", packet.claimedAmount());
    tag.putBoolean("lucky", packet.isLucky());
    tag.putLong("createdAtMillis", packet.createdAtMillis());
    tag.putLong("expirationTimeMillis", packet.expirationTimeMillis());
    ListTag claimed = new ListTag();
    for (UUID playerId : packet.claimedPlayers()) claimed.add(StringTag.valueOf(playerId.toString()));
    tag.put("claimedPlayers", claimed);
    return tag;
  }

  private static List<RedPacket> validated(List<RedPacket> value) {
    if (value == null) throw new IllegalArgumentException("packets");
    List<RedPacket> copied = List.copyOf(value);
    Set<UUID> senders = new HashSet<>();
    for (RedPacket packet : copied) {
      if (!senders.add(packet.senderId())) throw new IllegalArgumentException("duplicate sender");
    }
    return copied;
  }
}
